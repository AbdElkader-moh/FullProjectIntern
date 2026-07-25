# Tekton + OpenShift — Step-by-Step Guide

This document explains, from zero, everything that was built in this project around **Tekton**
(the pipeline engine), **OpenShift** (where the app runs), and **Pipelines-as-Code** (how the
pipeline gets triggered automatically). It's written for someone who has never touched any of
these tools, and it's ordered the way the project was actually built, step by step, so you can
follow along and know exactly what to do to continue the work.

> Companion reading: [`devops-sprint-documentation.md`](./devops-sprint-documentation.md) covers
> the earlier, simpler Kubernetes/Minikube deployment (`k8s/` folder). This guide covers the newer,
> OpenShift-specific setup (`openshift/` + `tekton/` + `.tekton/`), which is the real deployment
> target this project is moving toward.

---

## 0. Concepts, in plain language..

Read this once before diving into the steps — everything below assumes you know what these words mean.

| Term | What it actually means here |
|---|---|
| **OpenShift** | Red Hat's distribution of Kubernetes. If you know `kubectl`, you already know 90% of `oc` (OpenShift's CLI) — it's a superset. It adds a few of its own object types, like `Route` (see below). |
| **Developer Sandbox** | The specific free OpenShift cluster this project deploys to (`rm3.7wse.p1.openshiftapps.com`). It only grants **one namespace per person** and restricts some cluster-wide permissions — this single fact shapes a lot of design decisions below. |
| **Namespace / Project** | An isolated area of the cluster. This project uses exactly one: `beyond-code-dev`. |
| **Route** | OpenShift's equivalent of a Kubernetes `Ingress` — what makes a Service reachable from outside the cluster over a public hostname. |
| **Kustomize** | A templating-free way to customize YAML. You write one reusable "base" set of manifests, then small "overlays" that patch/rename/relabel them per environment, without duplicating the whole file. Used here to derive a "test" and "prod" variant of the same app from one base. |
| **Tekton** | A Kubernetes-native CI/CD engine. Pipelines, tasks, and runs are just Kubernetes objects (`kind: Pipeline`, `kind: Task`, `kind: PipelineRun`) executed by a controller already installed in the cluster — not an external Jenkins-style server. |
| **Task** | One reusable unit of work in a pipeline (e.g. "clone the repo", "build an image", "run tests"), made of one or more `steps`, each a container. |
| **Pipeline** | An ordered graph of Tasks (`runAfter` controls ordering; tasks with no shared dependency run in parallel). |
| **PipelineRun** | One actual *execution* of a Pipeline with concrete parameter values. Creating a `PipelineRun` is what actually starts a pipeline. |
| **Pipelines-as-Code (PAC)** | A Tekton add-on that watches your Git provider (GitHub here) and automatically creates a `PipelineRun` when something happens (a push, a PR), based on YAML files kept inside your own repo under `.tekton/`. This is the only trigger mechanism this project uses. |
| **kaniko** | A tool that builds container images without needing Docker or root privileges — necessary because Tekton steps run as regular unprivileged pods. Used here to build and push every service's image. |
| **Workspace** | A shared directory (backed by a PVC or `emptyDir`) that multiple Tasks in the same Pipeline can read/write to, e.g. so `build-frontend` can see the files `clone` just checked out. |

---

## Step 1 — Design the OpenShift manifests (`openshift/`)

The first thing built was *where the app runs*, independent of any pipeline. This lives entirely
in `openshift/`, using Kustomize's base + overlays pattern.

### 1.1 `openshift/base/` — the environment-agnostic manifests

Plain manifests for the 5 services: `mysql`, `user-service`, `sensor-service`, `simulator`,
`frontend`. No namespace, no real image tags (just the placeholder `latest`), no per-environment
Service-name references. Listed together in `openshift/base/kustomization.yaml`.

Deliberate details baked in here (all driven by OpenShift's security model):
- Every pod sets `runAsNonRoot: true` and drops all Linux capabilities, because OpenShift's default
  `restricted-v2` Security Context Constraint assigns each pod an arbitrary non-root UID at
  admission time. MySQL's data/run directories and nginx's cache/run directories are all writable
  `emptyDir`s instead of assuming root ownership.
- `mysql` uses `strategy: Recreate` (not `RollingUpdate`) because its `ReadWriteOnce` PVC can only
  be mounted by one pod at a time — the old pod must fully terminate before a replacement starts.
- `backend-config`, `simulator-config`, and `frontend-nginx-config` are **not** in `base` — they
  hardcode Service DNS names (`mysql`, `user-service`, `sensor-service`) that differ between test
  and prod (see 1.2/1.3), so each overlay carries its own copy. Only `mysql-config` (just the DB
  name, no cross-references) is safe to share, so it's the one ConfigMap left in `base`.
- Secrets (`openshift/base/01-secrets.yaml`) are a **template only**, with dummy values — the file
  says so at the top. The real secrets are meant to be created imperatively instead (Step 4).
  `jwt-secret` is required: both backend services crash on startup without it.

### 1.2 `openshift/overlays/prod/` — the long-lived, public stack

Adds on top of `base`:
- `namespace: beyond-code-dev`
- Its own `configmaps.yaml`, using unsuffixed Service names (`mysql`, `user-service`,
  `sensor-service`)
- `routes.yaml` — 3 OpenShift `Route` objects (frontend, user-service, sensor-service) with TLS
  edge termination. **Prod is the only environment reachable from outside the cluster.**
- An `images:` block with `newTag: PLACEHOLDER` for the 4 built images — the pipeline overwrites
  `PLACEHOLDER` with the real git-commit SHA at deploy time (Step 2.3).

### 1.3 `openshift/overlays/test/` — the temporary, per-run stack

Because the Developer Sandbox only grants one namespace, there's no separate "test" namespace —
this overlay deploys into the exact same `beyond-code-dev` namespace as prod, and isolation is
achieved through Kustomize features instead of a namespace boundary:

1. **`nameSuffix: -test`** renames every object (`frontend` → `frontend-test`, etc).
2. **`commonLabels: {variant: test}`** stamps a label on every object, so a single
   `oc delete ... -l variant=test` can clean up everything later (Step 2.7).
3. **`patches:`** — the part that actually matters for *correctness*. Kubernetes Service
   selectors match as a **subset** check: a pod labeled `app: frontend, variant: test` would still
   satisfy prod's `frontend` Service selector (`app: frontend`) even with the extra label, since
   "has at least these labels" is still true. Without these patches, prod's real Service would
   silently start routing live traffic into a throwaway test pod. The patches give every test
   Deployment/Service a genuinely different `app` value (`frontend-test`, `mysql-test`, ...), so
   prod's unmodified selectors can never match a test pod.

Its own `configmaps.yaml` re-points every cross-service URL at the `-test` counterparts, so test
pods can never read or write prod's database. There is **no Route** here — tests reach the test
Services directly over in-cluster DNS (`http://frontend-test`, `http://user-service-test:8080`).

---

## Step 2 — Build the Tekton pipeline (`tekton/`)

With the target manifests in place, the next step was building the pipeline that builds, deploys,
and tests them. Each piece is its own file, applied once to the cluster as a Kubernetes object.

### 2.1 `01-hello-task.yaml` — smoke test

A minimal Task that just echoes text, used to confirm Tekton/OpenShift worked at all before
building anything real. **Not** part of the real pipeline graph — don't look for it being called
from `02-pipeline.yaml`.

### 2.2 `03-buildah-task.yaml` — the image build Task

Defines a Task named `kaniko-build-push` (the filename says "buildah", but the actual tool used is
**kaniko** — a naming leftover from an earlier attempt; see Step 7). It builds an image from
`/workspace/source/<CONTEXT>/Dockerfile` and pushes it to Docker Hub
(`docker.io/nadinahmed/...`), authenticating via a `dockerhub-creds` Secret mounted at
`/kaniko/.docker/config.json`. The pipeline runs this Task 4 times in parallel — once per service
— by passing a different `CONTEXT` each time.

### 2.3 `04-deploy-task.yaml` — the deploy Task

Defines `deploy-overlay`, which runs `oc apply -k` against one overlay. Deliberately **does not**
depend on the git-cloned workspace — instead it reads 3 ConfigMaps
(`openshift-base-manifests`, `openshift-overlay-test` / `openshift-overlay-prod`) that must be
generated ahead of time from the `openshift/` folder (Step 4). It copies those read-only
ConfigMap-mounted files into a writable scratch directory, `sed`s the real image tag into
`kustomization.yaml` in place of `PLACEHOLDER`, runs `oc apply -k .`, then waits
(`oc rollout status`) for every Deployment to become ready — using the `-test` suffix on those
names when `OVERLAY=test`.

### 2.4 `05-newman-task.yaml` — API sanity tests

Defines `run-newman`, which runs the "Sanity Checks" Postman collection
(`Backend Testing/*Sanity*.postman_collection.json`) against the just-deployed test Services,
passing their in-cluster URLs as `--env-var baseUrl=...` / `sensorBaseUrl=...`.

### 2.5 `06-selenium-task.yaml` — UI tests

Defines `run-selenium`, which runs the Selenium/TestNG suite in `FrontendTestingFramework/`
against the test frontend, installing Chrome on the fly inside a Maven image, headless.

### 2.6 `02-pipeline.yaml` — tying it all together

Defines the `build-project` Pipeline: the ordered graph connecting every Task above.

```
clone
  └─► build-frontend, build-user-service, build-sensor-service, build-simulator  (parallel)
        └─► deploy-test
              └─► run-newman
                    └─► run-selenium
                          └─► deploy-prod   (only reached if run-selenium passed)
finally:
  └─► cleanup-test   (always runs, no matter what happened above)
```

Key parameter flow: `clone` exposes `$(tasks.clone.results.commit)` (the git commit SHA), which is
threaded through as the image tag for every build task and as the `TAG` param for both deploy
tasks — so every image built in a run, and the manifests deployed from it, are tied to one
traceable commit.

`deploy-prod` has only `run-selenium` as its dependency — there's no separate `when:` condition
gating it. Tekton already skips every downstream `runAfter` task the instant anything upstream
fails, so "tests passed" and "safe to promote to prod" are the same condition for free.

The **`finally:`** block (`cleanup-test`) is different from a normal `runAfter` step: `finally`
tasks always execute after every regular task finishes, whether the pipeline succeeded, failed
partway, or was cancelled. This is what guarantees the `-test` stack never gets left behind,
regardless of where the pipeline stopped.

### 2.7 `08-cleanup-task.yaml` — the teardown Task

Defines `cleanup-test-resources`: `oc delete deployment,service,configmap,secret,pvc -l
variant=test`. One label selector catches everything the test overlay created, since
`commonLabels` stamped it on every object regardless of source file.

### 2.8 `07-pipelinerun.yaml` — a manual run template

A `PipelineRun` that starts `build-project` with a fixed `repo-url` param. Its `shared-workspace`
uses an **`emptyDir`-backed `volumeClaimTemplate`**, meaning a new PVC is minted per run and only
needs to live for that run's duration. This was a deliberate fix: earlier, a PVC minted per run
that was never automatically deleted had exhausted the project's PVC quota on the Developer
Sandbox.

At this point the pipeline could already be run manually with `oc create -f
tekton/07-pipelinerun.yaml` — the next step made it start automatically.

---

## Step 3 — Wire up automatic triggering with Pipelines-as-Code

The last piece built was automatic triggering: making a `PipelineRun` start by itself whenever
someone pushes to `main`, using **Pipelines-as-Code (PAC)** — a Tekton add-on already running in
the cluster.

### 3.1 `tekton/10-pac-repository.yaml`

Registers this GitHub repo with the cluster's **shared** PAC controller (a fixed service in the
`openshift-pipelines` namespace, shared by every Developer Sandbox tenant). This `Repository`
object only tells PAC *which* repo to watch and which secret to verify the GitHub webhook payload
against (`pac-github-webhook-config`) — the actual trigger rules live in `.tekton/push.yaml`,
inside this repo, not in this file.

A cluster-side `EventListener` (the "classic" Tekton Triggers approach) was **not** used here,
because it needs cluster-scoped RBAC (it watches `ClusterInterceptor` objects cluster-wide) that
the Developer Sandbox doesn't grant to tenants. PAC avoids that entirely, since the shared
controller — not anything this project's own service account owns — is what holds the
cluster-scoped access.

### 3.2 `.tekton/push.yaml`

Picked up directly by the shared PAC controller on every push GitHub forwards via the webhook —
no `oc apply` needed for this file, PAC reads it straight out of the repo on each event.

- `pipelinesascode.tekton.dev/on-event: "[push]"` and `on-target-branch: "[main]"` — only fires
  for pushes to `main`. This is the *only* branch gate in the whole system: nothing downstream
  double-checks the branch, so `run-selenium` succeeding always leads straight to `deploy-prod`.
- `max-keep-runs: "3"` — PAC only keeps the last 3 finished PipelineRuns for this file and garbage
  collects the rest. Without this, every push would leave another full set of pods behind forever
  — exactly what had previously exhausted the namespace's pod quota.
- `pipelinesascode.tekton.dev/pipeline` and `.../task` annotations — PAC does **not** resolve
  `pipelineRef`/`taskRef` against objects already sitting in the cluster the way a normal
  `PipelineRun` would. It only knows about Pipelines/Tasks it can fetch and inline itself at
  resolve time. So every Task the Pipeline references — not just the Pipeline — is listed here as
  a fetchable URL (raw GitHub URLs into this repo's own `tekton/*.yaml` files, using the same
  `provider.token` since this is a private repo), except `git-clone`, which is a bare name PAC
  resolves against the public Tekton Hub catalog instead.

With this in place, the full flow is: **push to `main` → GitHub webhook → shared PAC controller →
new `PipelineRun` in `beyond-code-dev`, running the exact graph from Step 2.6.**

---

## Step 4 — One-time cluster setup (do this before any of the above will work)

The YAML files in this repo are necessary but not sufficient — the following must be created
imperatively too (none of it is safe to keep as committed YAML with real values):

1. **Log in and target the namespace:**
   ```bash
   oc login --token=<your-token> --server=https://api.rm3.7wse.p1.openshiftapps.com:6443
   oc project beyond-code-dev
   ```

2. **Real secrets** (do *not* apply `openshift/base/01-secrets.yaml` as-is — it's a template with
   placeholder/dummy values):
   ```bash
   oc create secret generic mysql-secret \
     --from-literal=mysql_root_password='...' \
     --from-literal=mysql_user='...' \
     --from-literal=mysql_password='...'

   oc create secret generic cloudinary-secret \
     --from-literal=cloudinary_cloud_name='...' \
     --from-literal=cloudinary_api_key='...' \
     --from-literal=cloudinary_api_secret='...'

   oc create secret generic jwt-secret \
     --from-literal=jwt_secret='...'
   ```

3. **Docker Hub credentials for kaniko** (referenced by `tekton/03-buildah-task.yaml`):
   ```bash
   oc create secret docker-registry dockerhub-creds \
     --docker-server=docker.io \
     --docker-username=<your-dockerhub-username> \
     --docker-password=<your-dockerhub-token>
   ```

3b. **SonarCloud token** (referenced by `tekton/11-sonar-task.yaml`). Generate one at
    sonarcloud.io → My Account → Security, then:
    ```bash
    oc create secret generic sonarcloud-creds \
      --from-literal=SONAR_TOKEN='...'
    ```
    Also make sure a project exists on SonarCloud (under your organization) for each key used in
    `tekton/02-pipeline.yaml`'s `sonar-user-service` / `sonar-sensor-service` tasks
    (`beyond-code-user-service`, `beyond-code-sensor-service`) — SonarCloud can auto-create them
    on first analysis if "Auto-provisioning" is enabled for the org, otherwise create them by hand
    first. And set the real org key in `tekton/07-pipelinerun.yaml`'s `sonar-organization` param
    (currently a `REPLACE_ME_SONAR_ORG` placeholder).

4. **The 3 ConfigMaps `deploy-overlay` reads from** — regenerate these every time anything under
   `openshift/base/` or `openshift/overlays/*/` changes:
   ```bash
   oc create configmap openshift-base-manifests \
     --from-file=openshift/base/ --dry-run=client -o yaml | oc apply -f -

   oc create configmap openshift-overlay-test \
     --from-file=openshift/overlays/test/ --dry-run=client -o yaml | oc apply -f -

   oc create configmap openshift-overlay-prod \
     --from-file=openshift/overlays/prod/ --dry-run=client -o yaml | oc apply -f -
   ```
   (Kept as 3 separate ConfigMaps because `base/kustomization.yaml` and both overlays'
   `kustomization.yaml` share the same filename — combining them into one ConfigMap would let the
   later ones silently overwrite the earlier ones, since `--from-file=DIR` keys entries by
   basename.)

5. **Apply the Tekton Tasks and Pipeline:**
   ```bash
   oc apply -f tekton/03-buildah-task.yaml
   oc apply -f tekton/04-deploy-task.yaml
   oc apply -f tekton/05-newman-task.yaml
   oc apply -f tekton/06-selenium-task.yaml
   oc apply -f tekton/08-cleanup-task.yaml
   oc apply -f tekton/09-unit-test-task.yaml
   oc apply -f tekton/11-sonar-task.yaml
   oc apply -f tekton/02-pipeline.yaml
   # git-clone is a standard Tekton Hub task, not defined in this repo:
   oc apply -f https://raw.githubusercontent.com/tektoncd/catalog/main/task/git-clone/0.9/git-clone.yaml
   ```

6. **Set up the Pipelines-as-Code trigger:**
   - Create the `pac-github-webhook-config` secret (GitHub provider token + webhook secret) that
     `tekton/10-pac-repository.yaml` references.
   - `oc apply -f tekton/10-pac-repository.yaml`
   - Configure a GitHub webhook on this repo pointing at the shared PAC controller's URL (see the
     comment in `tekton/10-pac-repository.yaml` for the reachable endpoint), using the same
     webhook secret.
   - `.tekton/push.yaml` needs no `oc apply` — PAC reads it directly out of the repo on each event.

7. **Deploy prod once, manually, to bootstrap it** (afterward, the pipeline keeps it updated):
   ```bash
   cd openshift/overlays/prod
   sed -i "s/PLACEHOLDER/<a-real-already-pushed-tag>/g" kustomization.yaml
   oc apply -k .
   ```

---

## Step 5 — Day-to-day workflow (once everything above is set up)

1. Make a code change, commit, push to `main`.
2. The GitHub webhook notifies the shared PAC controller, which creates a `PipelineRun` from
   `.tekton/push.yaml`.
3. Watch it:
   ```bash
   oc get pipelinerun -n beyond-code-dev --sort-by=.metadata.creationTimestamp
   tkn pipelinerun logs <name> -f -n beyond-code-dev    # if you have the `tkn` CLI
   # or, without tkn:
   oc get pods -n beyond-code-dev -l tekton.dev/pipelineRun=<name>
   oc logs <pod-name> -c step-<step-name> -n beyond-code-dev
   ```
4. If `run-newman` or `run-selenium` fails, the pipeline stops there — `deploy-prod` never runs,
   and `cleanup-test` (in `finally:`) removes the test stack regardless.
5. If everything passes, `deploy-prod` re-applies the **whole** prod overlay (not just a bare
   `oc set image`), so any manifest drift (new env vars, resource limits, etc.) gets corrected too,
   not just the image tag.
6. If you changed anything under `openshift/base/` or `openshift/overlays/*/`, remember: the
   pipeline's `deploy-overlay` task reads from ConfigMaps, **not** from the files in the git clone
   — you must regenerate those 3 ConfigMaps (Step 4.4) before the next run will pick it up.

---

## Step 6 — Manual / debugging commands cheat-sheet

```bash
# Preview what an overlay would produce, without applying anything
oc kustomize openshift/overlays/prod
oc kustomize openshift/overlays/test

# Manually start a pipeline run (useful for testing without a push)
oc create -f tekton/07-pipelinerun.yaml

# List recent runs / inspect one
oc get pipelinerun -n beyond-code-dev
oc describe pipelinerun <name> -n beyond-code-dev

# See what's currently deployed
oc get deployment,service,route -n beyond-code-dev

# Manually tear down a stuck/leftover test stack
oc delete deployment,service,configmap,secret,pvc -l variant=test -n beyond-code-dev

# Get prod's public URLs
oc get routes -n beyond-code-dev
```

---

## Step 7 — Known quirks (things that look wrong but are intentional, and one that isn't)

- **`tekton/03-buildah-task.yaml` is misnamed.** The file is named after *buildah*, but its actual
  content defines a Task called `kaniko-build-push` that builds images with **kaniko**, not
  buildah — a leftover from an earlier design that switched tools without renaming the file.
  Functionally harmless (the pipeline references the Task by its `taskRef.name`, not the filename),
  but worth renaming the next time someone touches this area, to stop new contributors from
  assuming buildah is in use.
- **`tekton/01-hello-task.yaml` is not part of the real pipeline.** It's a standalone smoke test
  from early setup, not a bug — don't look for where it's wired into `02-pipeline.yaml`.
- **`test` and `prod` share one namespace, on purpose.** This is a Developer Sandbox limitation
  (one namespace per tenant), not a free design choice — see Step 1.3 for how isolation is
  achieved without a namespace boundary.

---

## Step 8 — Suggested next steps for whoever continues this

- Rename `tekton/03-buildah-task.yaml` to match its actual content (`kaniko-build-push`), to stop
  it misleading future readers.
- Consider whether `tekton/01-hello-task.yaml` is still needed, or was only ever a bring-up smoke
  test.
- The dummy values inside `openshift/base/01-secrets.yaml` (a Cloudinary key/secret, a JWT signing
  key) are real-looking strings committed to git — even though the file is documented as a
  template, double-check whether any of those values are/were live before treating this repo as
  fully public-safe.
