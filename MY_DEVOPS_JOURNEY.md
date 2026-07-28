# How We Built the Tekton + OpenShift Pipeline — Step by Step

A chronological account of how the CI/CD setup in this repo came together, reconstructed from
the git history (`a1c758e` → `bb867db` → `02ece76` → `441b913`) and the files themselves. For the
pure technical reference (what each file does, why), see
[`TEKTON_OPENSHIFT_GUIDE.md`](./TEKTON_OPENSHIFT_GUIDE.md) — this document is the narrative of
*how we got there*, in the order we actually did it, with the commands we ran.

---

## 1. Logged in to the Red Hat Developer Sandbox

```bash
oc login --token=<your-token> --server=https://api.rm3.7wse.p1.openshiftapps.com:6443
oc project beyond-code-dev
```

The Sandbox only grants **one namespace per tenant** (`beyond-code-dev`), and restricts some
cluster-scoped permissions. That single constraint ends up shaping almost every design decision
below — no separate test namespace, no cluster-side EventListener, etc.

## 2. Designed the OpenShift manifests, before writing any pipeline

Built [`openshift/base/`](openshift/base/) first: plain, environment-agnostic Kubernetes
manifests for the 5 services — `mysql`, `user-service`, `sensor-service`, `simulator`,
`frontend` — with no namespace, no real image tags (just the `latest` placeholder), and no
per-environment Service-name references baked in. Tied together with
[`openshift/base/kustomization.yaml`](openshift/base/kustomization.yaml).

Deliberate details we baked in here, all driven by OpenShift's security model:
- Every pod runs `runAsNonRoot: true` and drops all Linux capabilities, since OpenShift's default
  `restricted-v2` Security Context Constraint assigns each pod an arbitrary non-root UID at
  admission time. MySQL's data/run dirs and nginx's cache/run dirs are all writable `emptyDir`s
  instead of assuming root ownership.
- `mysql` uses `strategy: Recreate`, not `RollingUpdate` — its `ReadWriteOnce` PVC can only be
  mounted by one pod at a time, so the old pod has to fully terminate before a replacement starts.
- Only `mysql-config` stayed in `base` as a shared ConfigMap. `backend-config`,
  `simulator-config`, and `frontend-nginx-config` all hardcode Service DNS names
  (`mysql`, `user-service`, `sensor-service`) that differ between test and prod, so each overlay
  needed its own copy instead.

## 3. Split "prod" and "test" into Kustomize overlays

Since there's no separate test namespace to isolate into, we had to make isolation happen through
Kustomize features alone:

- **[`overlays/prod/`](openshift/overlays/prod/)** — the long-lived, public stack. Adds
  `namespace: beyond-code-dev`, its own unsuffixed `configmaps.yaml`, and `routes.yaml` (3
  OpenShift `Route` objects with TLS edge termination — prod is the *only* stack reachable from
  outside the cluster).
- **[`overlays/test/`](openshift/overlays/test/)** — the temporary, per-pipeline-run stack,
  deployed into the *same* namespace. Three layers of isolation:
  1. `nameSuffix: -test` renames every object (`frontend` → `frontend-test`, etc).
  2. `commonLabels: {variant: test}` stamps a label on everything so a single
     `oc delete ... -l variant=test` can clean it all up later.
  3. Hand-written `patches:` — the part that actually matters for correctness. Kubernetes Service
     selectors match as a **subset** check, so a pod labeled `app: frontend, variant: test` would
     still satisfy prod's `frontend` Service selector (`app: frontend`) even with the extra label.
     Without these patches, prod's real Service would silently start routing live traffic into a
     throwaway test pod. The patches give every test Deployment/Service a genuinely different
     `app` value (`frontend-test`, `mysql-test`, ...) so prod's unmodified selectors can never
     match a test pod.
  - Its own `configmaps.yaml` re-points every cross-service URL at the `-test` counterparts, so
    test pods can never read or write prod's database.
  - No Route here — Newman/Selenium hit these Services directly over in-cluster DNS
    (`http://frontend-test`, `http://user-service-test:8080`, ...).

## 4. Created the real secrets imperatively (never committed to git)

`openshift/base/01-secrets.yaml` only ever existed as a **template** with dummy values — real
secrets were always meant to be created by hand, out-of-band:

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

`jwt-secret` is load-bearing — both backend services crash on startup without it.

## 5. Created the Docker Hub credentials for kaniko

```bash
oc create secret docker-registry dockerhub-creds \
  --docker-server=docker.io \
  --docker-username=<your-dockerhub-username> \
  --docker-password=<your-dockerhub-token>
```

Referenced by [`tekton/03-buildah-task.yaml`](tekton/03-buildah-task.yaml) (mounted at
`/kaniko/.docker/config.json`) so kaniko can push the built images to `docker.io/nadinahmed/...`.

## 6. Generated the 3 ConfigMaps the deploy task reads from

`deploy-overlay` (Step 7 below) deliberately does **not** depend on the git-cloned workspace —
it reads the manifests from ConfigMaps instead, generated from our local `openshift/` folder:

```bash
oc create configmap openshift-base-manifests \
  --from-file=openshift/base/ --dry-run=client -o yaml | oc apply -f -

oc create configmap openshift-overlay-test \
  --from-file=openshift/overlays/test/ --dry-run=client -o yaml | oc apply -f -

oc create configmap openshift-overlay-prod \
  --from-file=openshift/overlays/prod/ --dry-run=client -o yaml | oc apply -f -
```

Kept as 3 separate ConfigMaps, not 1: `base/kustomization.yaml`,
`overlays/test/kustomization.yaml`, and `overlays/prod/kustomization.yaml` all share the same
filename, and `--from-file=DIR` keys entries by basename — combining them would let the later
ones silently overwrite the earlier ones. This is also why we have to remember to re-run these 3
commands every time we touch anything under `openshift/base/` or `openshift/overlays/*/` — the
pipeline never sees our local files directly.

## 7. Wrote the Tekton Tasks, one at a time, and `oc apply -f`'d each

1. [`01-hello-task.yaml`](tekton/01-hello-task.yaml) — a throwaway smoke test (`echo "Hello from
   Tekton!"`) just to confirm Tekton was actually working on the cluster before building anything
   real. It's still in the repo but was never wired into the real pipeline graph.
2. [`03-buildah-task.yaml`](tekton/03-buildah-task.yaml) — defines `kaniko-build-push` (misnamed
   after an earlier tool choice — the real content builds with **kaniko**, not buildah). Builds
   `/workspace/source/<CONTEXT>/Dockerfile` and pushes to `docker.io/nadinahmed/...`.
3. [`04-deploy-task.yaml`](tekton/04-deploy-task.yaml) — defines `deploy-overlay`: copies the
   read-only ConfigMap-mounted manifests into a writable scratch dir, `sed`s the real image tag
   into `kustomization.yaml` in place of `PLACEHOLDER`, runs `oc apply -k .`, then blocks on
   `oc rollout status` for every Deployment (using the `-test` suffix when `OVERLAY=test`).
4. [`05-newman-task.yaml`](tekton/05-newman-task.yaml) — defines `run-newman`: runs the "Sanity
   Checks" Postman collection against the just-deployed test Services.
5. [`06-selenium-task.yaml`](tekton/06-selenium-task.yaml) — defines `run-selenium`: runs the
   Selenium/TestNG suite from `FrontendTestingFramework/` against the test frontend, installing
   Chrome headless inside a Maven image at runtime.
6. [`08-cleanup-task.yaml`](tekton/08-cleanup-task.yaml) — defines `cleanup-test-resources`:
   `oc delete deployment,service,configmap,secret,pvc -l variant=test`.

We also pulled the standard `git-clone` Task straight from the public Tekton Hub catalog rather
than writing our own:

```bash
oc apply -f https://raw.githubusercontent.com/tektoncd/catalog/main/task/git-clone/0.9/git-clone.yaml
```

## 8. Wired everything into one Pipeline

[`02-pipeline.yaml`](tekton/02-pipeline.yaml) ties every Task above into one ordered graph:

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

`clone` exposes `$(tasks.clone.results.commit)` (the git commit SHA), threaded through as the
image tag for every build task and as the `TAG` param for both deploy tasks — every image and
every deployed manifest in a run traces back to one commit.

`deploy-prod` has only `run-selenium` as its dependency, with no extra `when:` condition — Tekton
already skips every downstream `runAfter` task the instant anything upstream fails, so "tests
passed" and "safe to promote to prod" fall out for free. `finally: cleanup-test` is different
from a normal `runAfter` step: it always executes, whether the pipeline succeeded, failed
partway, or was cancelled — guaranteeing the `-test` stack never gets left behind.

```bash
oc apply -f tekton/03-buildah-task.yaml
oc apply -f tekton/04-deploy-task.yaml
oc apply -f tekton/05-newman-task.yaml
oc apply -f tekton/06-selenium-task.yaml
oc apply -f tekton/08-cleanup-task.yaml
oc apply -f tekton/02-pipeline.yaml
```

## 9. Could already run it manually at this point

[`07-pipelinerun.yaml`](tekton/07-pipelinerun.yaml) is a `PipelineRun` template with a fixed
`repo-url` param:

```bash
oc create -f tekton/07-pipelinerun.yaml
```

Its `shared-workspace` uses a `volumeClaimTemplate` (later we'd learn this needed to be
`emptyDir`-backed to avoid minting a permanent PVC per run — see Step 12) — a deliberate fix
after an earlier version exhausted the project's PVC quota on the Sandbox by never cleaning up
PVCs from old runs.

## 10. First trigger attempt: GitHub Actions (commit `a1c758e`, 2026-07-20 02:48)

Our first idea for automatic triggering didn't use Pipelines-as-Code at all. We added
`.github/workflows/trigger-pipeline.yml`:

```yaml
on:
  push:
    branches: [main]
jobs:
  trigger-pipeline:
    runs-on: ubuntu-latest
    steps:
      - uses: redhat-actions/oc-login@v1
        with:
          openshift_server_url: https://api.rm3.7wse.p1.openshiftapps.com:6443
          openshift_token: ${{ secrets.OPENSHIFT_TOKEN }}
          namespace: beyond-code-dev
      - run: oc create -f tekton/07-pipelinerun.yaml
```

authenticating as a narrowly-scoped `github-actions-trigger` ServiceAccount
(`tekton/09-github-actions-sa.yaml`) with a Role limited to
`create/get/list` on `pipelineruns` only. We deliberately avoided a cluster-side Tekton
`EventListener` (the "classic" Tekton Triggers approach) here — it needs cluster-scoped RBAC
(watching `ClusterInterceptor` objects cluster-wide on boot) that the Developer Sandbox doesn't
grant tenants, so a GitHub-side workflow using a narrowly-scoped in-cluster identity was the way
around that restriction.

## 11. Replaced it with Pipelines-as-Code (commit `bb867db`, same day, 04:47)

A couple hours later we found a cleaner way that didn't need a `secrets.OPENSHIFT_TOKEN` sitting
in GitHub at all: **Pipelines-as-Code (PAC)**, a Tekton add-on already running in the cluster as a
shared service for every Sandbox tenant.

- [`tekton/10-pac-repository.yaml`](tekton/10-pac-repository.yaml) — a `Repository` CR
  registering this GitHub repo with the shared PAC controller, and pointing it at a
  `pac-github-webhook-config` secret (GitHub provider token + webhook secret) to verify incoming
  webhook payloads against.
- [`.tekton/push.yaml`](.tekton/push.yaml) — the actual trigger rule, read directly out of the
  repo by PAC on every webhook event (no `oc apply` needed for this file):
  - `pipelinesascode.tekton.dev/on-event: "[push]"` + `on-target-branch: "[main]"` — only fires
    on pushes to `main`.
  - `max-keep-runs: "3"` — caps how many finished PipelineRuns PAC leaves behind, so pods from old
    runs don't quietly pile up and exhaust the namespace's pod quota again.
  - `pipeline:` / `task:` annotations listing raw GitHub URLs for every Pipeline/Task the run
    needs — PAC resolves and inlines these itself at run time rather than reading whatever is
    already applied in-cluster, so every `taskRef` used inside `02-pipeline.yaml` had to be listed
    here too (except `git-clone`, which PAC resolves against the public Tekton Hub by name).

Setup steps for this:
```bash
# create the webhook-verification secret (GitHub token + webhook secret)
oc apply -f tekton/10-pac-repository.yaml
# then configure a GitHub webhook on the repo pointing at the shared PAC controller's URL,
# using that same webhook secret
```

## 12. Deleted the GitHub Actions trigger entirely (commit `02ece76`, 2026-07-21 01:01)

The next day, once PAC was confirmed working, we removed `.github/workflows/trigger-pipeline.yml`
and `tekton/09-github-actions-sa.yaml` — PAC had fully replaced that path, so keeping both around
would just mean two competing triggers on every push.

## 13. Bootstrapped prod once, manually

```bash
cd openshift/overlays/prod
sed -i "s/PLACEHOLDER/<a-real-already-pushed-tag>/g" kustomization.yaml
oc apply -k .
```

After this one manual seed, the pipeline's `deploy-prod` task keeps prod updated on every
subsequent successful run.

## 14. Added real tests and a quality gate (commit `441b913`, 2026-07-25 13:16)

Five days later we noticed both backend Dockerfiles built with `mvn clean package -DskipTests` —
meaning nothing in CI had ever actually run the JUnit suites. We closed that gap:

- Wrote/extended JUnit tests for `backend/user` and `backend/sensor_data`
  (`SensorDataServiceTest.java`, `AirThresholdStrategyTest.java`).
- [`09-unit-test-task.yaml`](tekton/09-unit-test-task.yaml) — a `run-maven-tests` Task
  (`mvn -B test`) — the real gate the Docker build was skipping.
- [`11-sonar-task.yaml`](tekton/11-sonar-task.yaml) — a `sonarcloud-scan` Task running the
  SonarCloud Maven plugin with `-Dsonar.qualitygate.wait=true`, which polls SonarCloud until the
  Quality Gate result comes back and **fails the task** if the gate doesn't pass — not just if the
  upload itself errors.
- Rewired [`02-pipeline.yaml`](tekton/02-pipeline.yaml) so `test-user-service` /
  `test-sensor-service` run right after `clone`, `sonar-user-service` / `sonar-sensor-service` run
  after their respective tests pass, and `build-user-service` / `build-sensor-service` only run
  after their Sonar gate passes — so a failing test or a failed quality gate now stops the backend
  build (and therefore deploy-test, newman, selenium, deploy-prod) instead of silently shipping
  broken or low-quality code.

```bash
oc create secret generic sonarcloud-creds --from-literal=SONAR_TOKEN='...'
oc apply -f tekton/09-unit-test-task.yaml
oc apply -f tekton/11-sonar-task.yaml
oc apply -f tekton/02-pipeline.yaml   # re-apply, since the graph changed
```

(Also needed a SonarCloud project created ahead of time for each project key —
`beyond-code-user-service`, `beyond-code-sensor-service` — under the `abdelkader-moh`
organization, unless auto-provisioning is enabled.)

---

## End state: the day-to-day workflow

1. We make a code change, commit, and push to `main`.
2. GitHub's webhook notifies the shared PAC controller, which reads `.tekton/push.yaml` and
   creates a new `PipelineRun` in `beyond-code-dev`.
3. `clone` → tests + Sonar gates → parallel image builds → `deploy-test` → `run-newman` →
   `run-selenium` → `deploy-prod` (only if everything upstream passed) → `cleanup-test` always
   runs last via `finally:`.
4. I watch it with:
   ```bash
   oc get pipelinerun -n beyond-code-dev --sort-by=.metadata.creationTimestamp
   tkn pipelinerun logs <name> -f -n beyond-code-dev
   ```
5. If I changed anything under `openshift/base/` or `openshift/overlays/*/`, I have to remember
   to regenerate the 3 ConfigMaps (Step 6) — the pipeline reads from those, not from the git
   clone, so a local-only manifest change is otherwise silently ignored by the next run.

## Known quirks, carried over from the build process

- `tekton/03-buildah-task.yaml` is misnamed — it defines a kaniko-based Task, a leftover from
  switching tools without renaming the file.
- `tekton/01-hello-task.yaml` isn't part of the real pipeline graph — it's just the original
  smoke test.
- Test and prod deliberately share one namespace — a Sandbox limitation, not a design preference.
- The dummy values inside `openshift/base/01-secrets.yaml` are real-looking placeholder strings
  committed to git (a template, but worth double-checking none of them were ever live before
  treating this repo as fully public-safe).
