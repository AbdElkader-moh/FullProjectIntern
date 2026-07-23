# OpenShift manifests (Kustomize)..

Deploys frontend, user-service, sensor-service, mysql, and simulator to the
Developer Sandbox project. Both environments below run in the **same**
namespace (`beyond-code-dev`) -- the Sandbox only grants one -- so "test" and
"prod" are distinguished by resource naming, not by namespace boundary.

```
openshift/
├── base/                 # environment-agnostic: no namespace, no real image
│   │                       tags beyond the placeholder "latest", no per-env
│   │                       Service-name references baked in
│   └── kustomization.yaml
└── overlays/
    ├── prod/              # the long-lived stack (unsuffixed names, has Routes)
    │   ├── kustomization.yaml
    │   ├── configmaps.yaml   # backend-config/simulator-config/nginx config
    │   └── routes.yaml       # the only externally-reachable environment
    └── test/              # temporary stack, torn down after every pipeline run
        ├── kustomization.yaml
        └── configmaps.yaml
```

## Why prod and test each have their own `configmaps.yaml`

`backend-config`, `simulator-config`, and the nginx proxy config all hardcode
Service DNS names (`mysql`, `user-service`, `sensor-service`). Since the test
overlay renames those Services to `mysql-test`/`user-service-test`/
`sensor-service-test`, a single shared copy of these ConfigMaps would leave
the test pods silently talking to **production's** database and services.
So each overlay carries its own version, with every cross-reference pointing
at its own Services. `mysql-config` (just the DB name, no cross-references)
is the one ConfigMap that's safe to keep in `base`.

## Why the test overlay needs `patches:`, not just `nameSuffix`

`nameSuffix: -test` renames every object (`frontend` -> `frontend-test`,
etc.), and `commonLabels: {variant: test}` stamps a label on everything so
the pipeline's cleanup step can do one blanket `oc delete -l variant=test`.
Neither is enough on its own for isolation: Kubernetes Service selectors
match as a **subset** check, so prod's `frontend` Service (selector
`app: frontend`) would still match a test pod labeled `app: frontend,
variant: test` -- having extra labels doesn't disqualify a pod from an
existing, less specific selector. The `patches:` block in
`overlays/test/kustomization.yaml` gives every test Deployment/Service a
genuinely different `app` value (`frontend-test`, `mysql-test`, ...), so
prod's existing, unmodified selectors can never match a test pod -- without
needing to touch or restart anything already running in prod.

## Commands

```bash
# Preview what would be applied
oc kustomize openshift/overlays/prod
kubectl kustomize openshift/overlays/test
/
# Apply (Tekton does this itself -- see tekton/04-deploy-task.yaml -- but for
# a manual test, swap PLACEHOLDER for a real, already-pushed tag first)
oc apply -k openshift/overlays/prod
oc apply -k openshift/overlays/test

# Tear down the temporary test stack manually (Tekton's finally: block does
# this automatically at the end of every pipeline run)
oc delete deployment,service,configmap,secret,pvc -l variant=test -n beyond-code-dev
```

## Pipeline flow (see tekton/02-pipeline.yaml)

`clone -> build-* (parallel) -> deploy-test -> run-newman -> run-selenium ->
deploy-prod`, with `cleanup-test` as a `finally:` task that always runs last,
regardless of where the pipeline stopped. `deploy-prod` only runs if
`run-selenium` passed -- Tekton skips downstream `runAfter` tasks the moment
anything upstream fails, so that's the promotion gate; no extra `when:`
condition needed.
