# DevOps Sprint Documentation: Migrating FullProjectIntern from Docker Compose to Kubernetes

**Project:** FullProjectIntern — IoT System Tracking Application
**Role:** DevOps
**Objective:** Migrate a five-service application (MySQL, user-service, sensor-service, simulator, Angular frontend) from Docker Compose to a Kubernetes deployment on Minikube, with proper networking and secrets handling.

---

## 1. Application Overview

The application consists of five services, originally orchestrated with Docker Compose:

| Service | Role | Original Port |
|---|---|---|
| `mysql` | Database (MySQL 8.0) | 4306 → 3306 |
| `user-service` | Spring Boot user management API | 8088 → 8080 |
| `sensor-service` | Spring Boot sensor data API | 8081 |
| `simulator` | Generates simulated sensor data | internal only |
| `frontend` | Angular UI served via nginx | 4200 → 80 |

In Docker Compose, services communicated using container names (e.g. `internship-mysql`, `internship-backend`) over a shared bridge network, and secrets were mounted as files via Docker secrets (`*_FILE` environment variables).

---

## 2. Environment Setup

### 2.1 Tooling Installation

Minikube and kubectl were installed via Chocolatey on Windows 11 Pro:

```powershell
choco install minikube -y
minikube version
kubectl version --client
```

**Output:**
```
minikube version: v1.38.1
Client Version: v1.34.1
```

### 2.2 Cluster Initialization

The local cluster was started using the Docker driver:

```powershell
minikube start --driver=docker
minikube status
kubectl get nodes
```

**Output:**
```
NAME       STATUS   ROLES           AGE   VERSION
minikube   Ready    control-plane   Xm    v1.35.1
```

Minikube provisions a single-node cluster that serves as both control plane and worker node, which is sufficient for local development and sprint demonstration purposes.

---

## 3. Containerization Strategy

### 3.1 Image Build and Registry

Each application component was built as a standalone Docker image and published to a private Docker Hub registry (`nadinahmed`), so that Kubernetes could pull images independently of the local build context:

```powershell
docker login

docker build -t nadinahmed/user-service:latest ./backend/user
docker push nadinahmed/user-service:latest

docker build -t nadinahmed/sensor-service:latest ./backend/sensor_data
docker push nadinahmed/sensor-service:latest

docker build -t nadinahmed/simulator:latest ./simulator
docker push nadinahmed/simulator:latest

docker build -t nadinahmed/frontend-app:latest ./Frontend
docker push nadinahmed/frontend-app:latest
```

MySQL uses the official `mysql:8.0` image directly from Docker Hub, so no custom build was required for the database tier.

As backend code evolved during the sprint (teammate contributions to `user-service` and `sensor-service`), images were rebuilt and re-pushed under the same tags, and a rolling restart was triggered against the cluster to pick up the new versions:

```powershell
kubectl rollout restart deployment user-service
kubectl rollout restart deployment sensor-service
```

---

## 4. Kubernetes Manifest Design

All manifests are organized under a single `k8s/` directory for clarity and to mirror the `docker-compose.yml` structure:

```
k8s/
├── secrets.yaml
├── mysql-pvc.yaml
├── mysql-deployment.yaml
├── mysql-service.yaml
├── user-service-deployment.yaml
├── user-service-service.yaml
├── sensor-service-deployment.yaml
├── sensor-service-service.yaml
├── simulator-deployment.yaml
├── frontend-deployment.yaml
├── frontend-service.yaml
├── alias-services.yaml
└── ingress.yaml
```

### 4.1 Secrets Management

Docker Compose used file-based secrets (`*_FILE` environment variables pointing at mounted files). To translate this pattern to Kubernetes, a native `Secret` object was used, and each secret value is mounted into containers as an individual file under `/run/secrets/`, preserving the same `_FILE`-style consumption pattern the Spring Boot services already expected:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysql-secret
type: Opaque
stringData:
  mysql_root_password: "..."
  mysql_user: "..."
  mysql_password: "..."
---
apiVersion: v1
kind: Secret
metadata:
  name: cloudinary-secret
type: Opaque
stringData:
  cloudinary_cloud_name: "..."
  cloudinary_api_key: "..."
  cloudinary_api_secret: "..."
```

`secrets.yaml` is excluded from version control via `.gitignore`, since it contains live credential values. Only `secrets.yaml` itself needs to be ignored — all other manifests are safe to commit and share with the team.

### 4.2 Persistent Storage

MySQL data is backed by a `PersistentVolumeClaim`, decoupling storage lifecycle from pod lifecycle (the volume survives pod restarts and recreation):

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

Because the claim uses `ReadWriteOnce` (single-node mount), the MySQL `Deployment` explicitly sets its update strategy to `Recreate` rather than the Kubernetes default `RollingUpdate`. This guarantees the old pod is fully terminated — and releases its volume mount — before a replacement pod is created, avoiding any scenario where two pods try to mount the same `ReadWriteOnce` volume at once.

```yaml
spec:
  strategy:
    type: Recreate
```

The four stateless services (`user-service`, `sensor-service`, `simulator`, `frontend`) instead use `RollingUpdate` with `maxUnavailable: 1` and `maxSurge: 1`, since they hold no exclusive state and benefit from zero-downtime updates:

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
```

### 4.3 Deployments and Label-Selector Design

Every `Deployment` defines pod template labels, and its corresponding `Service` targets pods purely through a label selector rather than any direct pod reference:

```yaml
# Deployment
metadata:
  labels:
    app: user-service

# Service
spec:
  selector:
    app: user-service
```

This indirection is the core of Kubernetes' service discovery model: it allows pods to be replaced, scaled, or rolled out without ever needing to reconfigure the services or consumers pointing at them.

### 4.4 Internal Service Naming and DNS

Docker Compose containers reference each other by container name (e.g. `internship-mysql`, `internship-backend`). Kubernetes instead resolves services by their `Service` name through internal cluster DNS. Application configuration (Spring Boot datasource URLs, nginx upstream targets) was updated to use Kubernetes service names:

```yaml
- name: SPRING_DATASOURCE_URL
  value: "jdbc:mysql://mysql:3306/project_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

To preserve full compatibility with the frontend's existing nginx configuration — which was built to reference the original Docker Compose container names `internship-backend` and `internship-sensor` — a small `alias-services.yaml` manifest defines additional `ClusterIP` services under those exact names, pointing at the real `user-service` and `sensor-service` pods via label selectors:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: internship-backend
spec:
  selector:
    app: user-service
  ports:
    - port: 8080
      targetPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: internship-sensor
spec:
  selector:
    app: sensor-service
  ports:
    - port: 8081
      targetPort: 8081
```

This means the frontend image did not need to be rebuilt with new nginx configuration — the naming layer was solved entirely at the Kubernetes networking level.

### 4.5 Exposing Services

Each backend and the frontend are exposed internally as `NodePort` services during development:

| Service | Type | Port |
|---|---|---|
| mysql | ClusterIP | 3306 |
| user-service | NodePort | 30088 |
| sensor-service | NodePort | 30081 |
| frontend | NodePort | 30200 |

### 4.6 Unified External Access via Ingress

To give the whole application a single, consistent entry point instead of separate host:port combinations per service, the Minikube Ingress addon was enabled and a single `Ingress` resource was configured to route all external traffic through one hostname, `myapp.local`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
spec:
  rules:
  - host: myapp.local
    http:
      paths:
      - path: /api/sensors
        pathType: Prefix
        backend:
          service:
            name: sensor-service
            port:
              number: 8081
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8080
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 80
```

Two design details are worth calling out:

- **Single origin:** Routing the frontend, user-service, and sensor-service through one shared host (`myapp.local`) means the browser sees every request as coming from the same origin, which sidesteps any cross-origin (CORS) complications that arise from calling multiple NodePort addresses directly.
- **Path specificity ordering:** Ingress paths are evaluated in the order they're declared, so the more specific `/api/sensors` rule is placed *before* the broader `/api` rule. This ensures sensor-service traffic is matched correctly rather than being absorbed by the general `/api` rule intended for user-service.

```powershell
minikube addons enable ingress
kubectl get pods -n ingress-nginx
```

---

## 5. Deployment Execution

All manifests are applied in dependency order — storage and secrets first, then the database, then dependent services:

```powershell
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/mysql-pvc.yaml
kubectl apply -f k8s/mysql-deployment.yaml
kubectl apply -f k8s/mysql-service.yaml
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/user-service-service.yaml
kubectl apply -f k8s/sensor-service-deployment.yaml
kubectl apply -f k8s/sensor-service-service.yaml
kubectl apply -f k8s/simulator-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/frontend-service.yaml
kubectl apply -f k8s/alias-services.yaml
kubectl apply -f k8s/ingress.yaml
```

**Output:**

```powershell
kubectl get pods
```
```
NAME                              READY   STATUS    RESTARTS   AGE
frontend-576c9c7b47-2r7qn         1/1     Running   0          Xm
mysql-59cb59b5-r9rmc              1/1     Running   0          Xm
sensor-service-7d598b9f4c-jtqmx   1/1     Running   0          Xm
simulator-57fd944b44-pjxwm        1/1     Running   0          Xm
user-service-fb5585cfd-zrc5m      1/1     Running   0          Xm
```

All five pods reach `Running` status with `1/1` readiness.

---

## 6. Making the Application Reachable

`minikube tunnel` is run in its own elevated PowerShell window to bind the Ingress controller's LoadBalancer IP to `localhost`:

```powershell
minikube tunnel
```

`myapp.local` is mapped to `127.0.0.1` in the Windows hosts file:

```
127.0.0.1    myapp.local
```

*(Windows hosts file editing requires an elevated `notepad C:\Windows\System32\drivers\etc\hosts` session.)*

The application is then reachable end-to-end at:

```
http://myapp.local
```

---

## 7. Architecture Summary

```
Browser (http://myapp.local)
         │
      Ingress
   ┌─────┼──────┐
/api/sensors  /api   /
   │           │      │
sensor-svc  user-svc  frontend
   │           │
       mysql
         ▲
     simulator
```

| Service | Type | Port | Purpose |
|---|---|---|---|
| mysql | ClusterIP | 3306 | Internal database |
| user-service | NodePort | 30088 | User management API |
| sensor-service | NodePort | 30081 | Sensor data API |
| frontend | NodePort | 30200 | Angular UI |
| Ingress | — | 80 | Unified routing via myapp.local |

---

## 8. Docker Compose → Kubernetes Concept Mapping

| Concept | Docker Compose | Kubernetes |
|---|---|---|
| Run containers | `services` | `Deployment` |
| Expose ports | `ports` | `Service` (NodePort/ClusterIP) |
| Passwords | `secrets` (files) | `Secret` (base64-encoded) |
| Storage | `volumes` | `PersistentVolumeClaim` |
| Internal DNS | container name | Service name |
| Start everything | `docker-compose up` | `kubectl apply -f k8s/` |
| Stop everything | `docker-compose down` | `kubectl delete -f k8s/` |

---

## 9. Operational Reference Commands

```powershell
# Check pod status
kubectl get pods

# Check services
kubectl get services

# View logs of a pod
kubectl logs <pod-name>

# Describe a pod (for detailed status/events)
kubectl describe pod <pod-name>

# Restart a deployment (e.g. after a new image is pushed)
kubectl rollout restart deployment <name>

# Delete everything
kubectl delete -f k8s/
```

---

## 10. Key Takeaways

- Kubernetes replaces Docker Compose's container-name-based networking with **label-selector-based service discovery**, decoupling Services from specific Pods and enabling rolling updates and scaling without reconfiguration.
- **PersistentVolumeClaims** separate storage lifecycle from pod lifecycle, and the deployment `strategy` (Recreate vs. RollingUpdate) must match the access mode of the underlying volume.
- A single **Ingress** with one hostname simplifies external access and avoids cross-origin complications that come from exposing each service on its own port.
- **Path ordering in Ingress rules matters** — more specific paths must be declared before broader ones to route correctly.
- Kubernetes `Secret` objects, mounted as files, provide a direct analogue to Docker Compose's file-based secrets, requiring minimal change to application-level configuration.
