# 🌆 Beyond Code — Smart City IoT Monitoring Platform

> A full-stack, containerized Smart City platform that collects, processes, and visualizes real-time IoT sensor data for **Traffic**, **Air Quality**, and **Street Lighting** management. The system provides live dashboards, configurable alerting, rich analytics, and an end-to-end automated testing suite — all orchestrated through Docker Compose and a Jenkins CI/CD pipeline.

---

## 📑 Table of Contents

- [Project Overview](#-project-overview)
- [Architecture Overview](#-architecture-overview)
- [Tech Stack Summary](#-tech-stack-summary)
  - [Frontend](#frontend)
  - [Backend — User Service](#backend--user-service)
  - [Backend — Sensor Data Service](#backend--sensor-data-service)
  - [Sensor Simulator](#sensor-simulator)
  - [DevOps & Infrastructure](#devops--infrastructure)
- [Testing Strategy](#-testing-strategy)
  - [Frontend (UI) Testing](#frontend-ui-testing)
  - [Backend (API) Testing](#backend-api-testing)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Setup](#environment-setup)
  - [Spin Up the Full Stack](#spin-up-the-full-stack)
  - [Tear Down](#tear-down)
  - [Manual Service Execution](#manual-service-execution)
- [Running the Test Suites](#-running-the-test-suites)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Kubernetes Deployment](#-kubernetes-deployment)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

---

## 🌟 Project Overview

Bayond Code is an enterprise-grade **IoT integration layer and monitoring platform** built for smart city infrastructure. It provides city operators and analysts with a unified interface to:

- **Monitor** real-time sensor feeds for traffic flow, air pollution levels, and street-light health.
- **Alert** operators instantly via configurable threshold-based rules, with real-time WebSocket toast notifications pushed directly to the browser.
- **Analyze** historical sensor data with advanced filtering, sorting, and date-range querying.
- **Manage** user accounts, profiles, and per-sensor alert thresholds from a centralized settings panel.

The system is built as a **microservices-first** architecture, where each domain concern is isolated into its own independently deployable Spring Boot service, fronted by a modern Angular SPA, all wired together via Docker Compose for local development and Kubernetes manifests for cloud deployment.

---

## 🏛️ Architecture Overview

```
┌────────────────────────────────────────────────────────────────────┐
│                        Browser Client                              │
│               Angular 21 SPA  (port 4200 / nginx)                 │
└─────────────────────────┬──────────────────────────────────────────┘
                          │  REST + WebSocket (STOMP over SockJS)
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
  ┌───────────────┐ ┌───────────────┐    │
  │  User Service │ │ Sensor Service│    │  (JWT Auth Filter on every request)
  │  Spring Boot  │ │  Spring Boot  │    │
  │  port 8080    │ │  port 8081    │    │
  └───────┬───────┘ └───────┬───────┘    │
          │                 │            │
          └────────┬────────┘            │
                   ▼                     │
          ┌────────────────┐             │
          │   MySQL 8.x    │◄────────────┘
          │  (port 3307)   │
          └────────────────┘
                   ▲
          ┌────────────────┐
          │ Sensor Simulator│
          │  (Python / cron)│
          └────────────────┘
```

All services communicate over an isolated **Docker bridge network** (`project-net`), meaning no service port is exposed to external traffic except through the explicitly mapped host ports.

---

## 🛠️ Tech Stack Summary

### Frontend

**Technology:** Angular 21 · TypeScript 5.9 · Bootstrap 5 · STOMP/SockJS · Vitest · Nginx

The frontend is a **Single-Page Application (SPA)** built with Angular 21, leveraging its standalone component architecture for maximum modularity and lazy loading. Rather than loading all application code upfront, each route lazily imports its own self-contained component bundle. This keeps the initial load payload minimal and improves Time-To-Interactive significantly.

**Component Architecture:**
The application is divided into three domain verticals — Traffic, Air Pollution, and Street Lighting — each with its own set of **Dashboard**, **Analytics**, and **Alerts** components. Cross-cutting concerns like authentication (`signin`, `signup`) and user management (`profile`, `notifications`, `settings`) are housed in shared component groups.

Route-level **Auth Guards** (`authGuard`) enforce that all dashboard, analytics, and settings routes require a valid JWT before the component is even loaded — unauthenticated requests are immediately redirected to `/signin`.

**Real-Time Communication:**
The application connects to the Sensor Service via **WebSocket (STOMP over SockJS)**, enabling the server to push new alert notifications to the browser in real time without polling. Toast notifications appear automatically in a `.toast-stack` overlay when sensor readings cross configured thresholds.

**HTTP Layer:**
An `HttpInterceptor` injects the JWT Bearer token on every outgoing API call, and a `proxy.conf.json` is configured for the development server to forward `/api` calls to the correct backend ports, avoiding CORS issues locally.

**Build & Serve:**
In production (and inside Docker), the Angular app is built with `ng build` into a static asset bundle and served by **Nginx** using a custom `nginx.conf` that handles Angular's `PathLocationStrategy` by redirecting all 404s back to `index.html`.

---

### Backend — User Service

**Technology:** Spring Boot 4 · Java 17 · Spring Data JPA · Spring Security (JWT) · Cloudinary · SpringDoc OpenAPI 3 · Maven

The User Service is a dedicated **RESTful microservice** responsible for all user identity and profile operations. It is built on the latest Spring Boot 4 and uses **Spring MVC** for request routing, with all REST endpoints documented automatically via **SpringDoc OpenAPI** (accessible at `/swagger-ui.html` when running).

**API Routing & Data Flow:**
HTTP requests arrive at the embedded **Tomcat** server (Spring Boot's default embedded container), pass through a custom **JWT authentication filter** that validates the `Authorization: Bearer <token>` header on every protected route, and are dispatched to the appropriate `@RestController`. Controllers delegate to a service layer, which in turn uses **Spring Data JPA** repositories to interact with the MySQL database via **Hibernate ORM**.

**Authentication:**
User passwords are hashed using **Spring Security Crypto** (BCrypt). On a successful login, a **JJWT**-signed token is returned to the client. This token is a self-contained claim carrying the user's identity, validated on every subsequent request without a database round-trip.

**Media Uploads:**
Profile image uploads are handled via the **Cloudinary** SDK. Binary image data is streamed from the API layer directly to Cloudinary's CDN, and only the returned public URL is persisted in the database — keeping the database lean.

**Secrets Management:**
Database credentials, JWT secret, and Cloudinary API keys are **never embedded in environment variables as plain text**. Instead, Docker Secrets are used: each secret is mounted as a file inside the container (e.g., `/run/secrets/jwt_secret`), and the application reads the file at startup.

---

### Backend — Sensor Data Service

**Technology:** Spring Boot 3.4 · Java 17 · Spring Data JPA · Spring WebSocket (STOMP) · JJWT · SpringDoc OpenAPI · Maven

The Sensor Service is the **core data ingestion and event distribution hub** of the platform. It exposes REST endpoints for receiving sensor readings (from the Simulator or any external source), querying historical data with rich filtering, and pushing real-time alerts to connected browser clients via WebSocket.

**Data Ingestion:**
The simulator and any external agent `POST` raw sensor readings to dedicated endpoints (e.g., `/api/sensor/traffic`, `/api/sensor/air`, `/api/sensor/lights`). The service validates incoming payloads, persists them to MySQL, and synchronously evaluates them against all configured user thresholds.

**Threshold Evaluation & Alert Generation:**
If a reading crosses any threshold, an `Alert` entity is created and persisted. The same event is immediately broadcast to all WebSocket subscribers on the relevant STOMP topic, delivering sub-second notification delivery to the Angular frontend.

**Query Layer:**
Historical data endpoints support **server-side filtering** (by status, location, date range, metric type), **sorting** (by any field, ascending or descending), and **pagination** using Spring's `Pageable` abstraction — ensuring the API remains performant regardless of the number of stored readings.

**Session Management:**
`spring-session-jdbc` backs HTTP session state into the database, enabling stateless horizontal scaling of the service behind a load balancer without sticky sessions.

---

### Sensor Simulator

**Technology:** Python 3 · `requests` library

The Simulator is a lightweight **Python script** (`simulator.py`) packaged in its own Docker container. It runs on a configurable schedule (defaulting to every 120 seconds per sensor type) and continuously `POST`s synthetically generated sensor readings to the Sensor Service, simulating a real IoT sensor network. This ensures the dashboards always have live data without requiring physical hardware.

The simulator respects the internal Docker network hostname (`http://sensor-service:8081`) for communication, demonstrating proper service-discovery within the Docker Compose network.

---

### DevOps & Infrastructure

**Technologies:** Docker · Docker Compose · Jenkins · Kubernetes (K8s) · DockerHub

#### Docker & Docker Compose

Every service in the system — MySQL, User Service, Sensor Service, Simulator, and the Angular Frontend — is **containerized with its own Dockerfile** following a multi-stage build pattern where applicable. This guarantees environment parity between a developer's laptop and the production server.

The `docker-compose.yml` defines the **entire application graph** as a single declarative manifest:

- **`internship-mysql`**: A custom MySQL image initialized from `./db/Dockerfile`, with a persistent named volume (`mysql_data`) to survive container restarts.
- **`internship-backend` (User Service)**: Waits for MySQL to pass its health check before starting, using Docker Compose's `depends_on: condition: service_healthy` — preventing race-condition startup failures.
- **`internship-sensor` (Sensor Service)**: Similarly health-check gated behind MySQL.
- **`internship-simulator`**: Starts after the Sensor Service and continuously feeds data.
- **`internship-frontend`**: Nginx-served Angular SPA, depends on both backend services being available.

**Secrets** are managed via Docker Compose's native `secrets` block: each secret is read from a local `.txt` file at container start and mounted as an in-memory `tmpfs` file at `/run/secrets/<name>` inside each container that needs it — keeping secrets out of environment variables, image layers, and version control.

#### Jenkins CI/CD Pipeline

The `Jenkinsfile` defines a declarative **multi-stage CI/CD pipeline**:

| Stage | Description |
|---|---|
| **Checkout** | Pulls the latest code from the SCM (GitHub) |
| **Prepare Secrets** | Writes Jenkins-managed credentials into the `secrets/` directory |
| **Verify Secrets** | Confirms all secret files are present before building |
| **Build Images** | Runs `docker compose build` to build all service images |
| **Docker Login** | Authenticates to DockerHub using stored Jenkins credentials |
| **Push Images** | Tags and pushes all four service images to DockerHub |
| **Deploy** | Tears down the existing stack with `docker compose down` and relaunches with `docker compose up -d` |

#### Kubernetes

The `k8s/` directory contains full **Kubernetes manifests** for production-grade cloud deployment:

- `Deployment` manifests for all five services, referencing DockerHub images.
- `Service` manifests (ClusterIP/NodePort) to enable inter-pod communication.
- A `PersistentVolumeClaim` for MySQL data durability across pod restarts.
- An `Ingress` resource to expose the frontend and API routes through a single external load balancer entry point — compatible with AWS EKS, GKE, or any standard Ingress controller.

For **AWS cloud deployment**, the recommended architecture is:
- **Amazon EKS** to run the Kubernetes cluster.
- **Amazon RDS (MySQL)** to replace the containerized MySQL for production-grade managed database with automatic backups and failover.
- **Amazon ECR** (or DockerHub) as the container image registry.
- **AWS ALB Ingress Controller** to map the K8s `Ingress` resource to an Application Load Balancer.
- **AWS Secrets Manager** integrated with EKS via the Secrets Store CSI Driver, replacing Docker Secrets for cloud-native secret delivery.

---

## 🧪 Testing Strategy

### Frontend (UI) Testing

**Technology:** Java 11 · Selenium WebDriver 4.18 · TestNG 7.9 · Allure Reports 2.27 · ExtentReports 5 · WebDriverManager · Apache POI · Maven Surefire Plugin

The frontend is covered by a dedicated **Selenium-based end-to-end (E2E) test automation framework** located in `FrontendTestingFramework/`. This is a completely separate Maven project that treats the running application as a black box, driving a real Chrome browser through all user-facing workflows.

**Framework Design:**
The framework is architected around the **Page Object Model (POM)** design pattern. Every screen in the application (`SignInPage`, `ProfilePage`, `AirPollutionAlertsPage`, `StreetLightAnalyticsPage`, etc.) has a dedicated Java class in `src/test/java/pages/` that encapsulates all element locators and interaction methods. Test classes in `src/test/java/tests/` remain free of Selenium boilerplate, only calling high-level page methods and making TestNG assertions.

**Test Lifecycle & Data Seeding:**
Each test class uses `@BeforeClass` to perform one-time setup (e.g., logging in, configuring alert thresholds via the Settings page) and `@BeforeMethod` to seed precise test data before each individual test via `SensorApiClient` — a utility class that makes direct REST calls to the Sensor Service API (`http://localhost:8081`) to `POST` readings with known values, guaranteeing deterministic test conditions.

**What Is Tested:**
- Authentication flows: Sign up, sign in, auth guard redirects for unauthenticated access.
- Profile management: Avatar upload, field editing, data persistence.
- Home & notifications: Bell badge, mark-as-read flows, notification list.
- Settings: Creating and deleting sensor alert thresholds.
- Traffic, Air Pollution & Street Lighting dashboards, analytics tables, and alert pages — including filtering, sorting, pagination, date-range validation, severity badge content, and real-time WebSocket toast notifications.

**Reporting:**
Tests produce two parallel reports:
1. **ExtentReports** HTML report (in `reports/`): A browser-viewable, color-coded execution summary with screenshots on failure.
2. **Allure Report** (in `target/allure-results`): A rich, interactive report with Epic/Feature/Story hierarchy, timeline view, and trend graphs. Annotations like `@Epic`, `@Feature`, `@Story`, `@Severity`, and `@Description` are applied to every test method.

**Suite Configuration:**
Test execution is controlled by `testng.xml` (full suite) or `sanity.xml` (smoke/sanity subset), both of which are consumed by the Maven Surefire Plugin, enabling targeted execution from the command line.

---

### Backend (API) Testing

**Technology:** Postman · Newman (CLI runner)

The backend API layer is validated through a comprehensive **Postman collection** suite located in `Backend Testing/`. The collections are organized by sprint and domain, covering the full lifecycle of every API endpoint.

**Collection Structure:**

| Collection | Coverage |
|---|---|
| `Project — Sprint 1, 2 and 3 BE Tests` | User registration, login, JWT token flow, profile CRUD, sensor data ingestion endpoints |
| `Sprint 4 and Sprint 5` | Analytics query endpoints, threshold management, alert retrieval with pagination and filtering |
| `🩺 Sanity Checks — Full System` | A curated smoke-test collection covering critical happy paths across all services for rapid post-deployment verification |

**API Testing Approach:**
Each Postman request carries **pre-request scripts** to generate unique test data (e.g., timestamp-suffixed usernames) and **test scripts** with `pm.test()` assertions that validate:
- HTTP status codes (200, 201, 400, 401, 403, 404).
- Response body schema correctness (field presence, data types, non-empty arrays).
- Business logic (e.g., a threshold-crossing reading creates a corresponding alert record).
- JWT token chaining: the token returned from the login request is automatically stored in a collection variable and injected as the `Authorization` header in all subsequent protected requests.

**Execution:**
Collections can be run locally via the Postman desktop GUI against a `New Environment.postman_environment.json` file that sets `baseUrl` to `http://localhost:8088` (User Service) and `http://localhost:8081` (Sensor Service). They can also be executed headlessly in CI via **Newman**:

```bash
newman run "Backend Testing/🩺 Sanity Checks — Full System.postman_collection.json" \
  --environment "Backend Testing/New Environment.postman_environment.json" \
  --reporters cli,json \
  --reporter-json-export results/sanity-report.json
```

---

## 🚀 Getting Started

### Prerequisites

Ensure the following tools are installed on your machine before proceeding:

| Tool | Version | Purpose |
|---|---|---|
| **Docker Desktop** | ≥ 24.x | Container runtime & Compose |
| **Docker Compose** | ≥ 2.x (included with Desktop) | Multi-container orchestration |
| **Git** | Any recent | Cloning the repository |
| **Java JDK** | 17+ | Manual backend execution / test runner |
| **Maven** | 3.9+ | Backend builds & test execution |
| **Node.js + npm** | Node 22 / npm 11 | Manual frontend development |
| **Angular CLI** | 21.x | Running the dev server locally |
| **Chrome Browser** | Latest stable | Selenium E2E tests |

---

### Environment Setup

1. **Clone the repository:**

```bash
git clone git@github.com:AbdElkader-moh/FullProjectIntern.git
cd FullProjectIntern
```

2. **Create the secrets directory and populate secret files.**

Docker Compose reads credentials from plain-text files in the `secrets/` directory. **These files must never be committed to version control** — they are listed in `.gitignore`.

```bash
mkdir secrets
```

Create each file with the appropriate value:

```bash
# On Linux/macOS:
echo "your_mysql_root_password"   > secrets/mysql_root_password.txt
echo "your_mysql_username"        > secrets/mysql_user.txt
echo "your_mysql_password"        > secrets/mysql_password.txt
echo "your_cloudinary_cloud_name" > secrets/cloudinary_cloud_name.txt
echo "your_cloudinary_api_key"    > secrets/cloudinary_api_key.txt
echo "your_cloudinary_api_secret" > secrets/cloudinary_api_secret.txt
echo "your_jwt_secret_key"        > secrets/jwt_secret.txt
```

```powershell
# On Windows PowerShell:
"your_mysql_root_password"   | Out-File -Encoding ascii secrets/mysql_root_password.txt
"your_mysql_username"        | Out-File -Encoding ascii secrets/mysql_user.txt
"your_mysql_password"        | Out-File -Encoding ascii secrets/mysql_password.txt
"your_cloudinary_cloud_name" | Out-File -Encoding ascii secrets/cloudinary_cloud_name.txt
"your_cloudinary_api_key"    | Out-File -Encoding ascii secrets/cloudinary_api_key.txt
"your_cloudinary_api_secret" | Out-File -Encoding ascii secrets/cloudinary_api_secret.txt
"your_jwt_secret_key"        | Out-File -Encoding ascii secrets/jwt_secret.txt
```

3. **Set the required `.env` variables** (used by `docker-compose.yml` for image tagging):

```bash
# .env file in the project root
DOCKERHUB_USERNAME=your_dockerhub_username
IMAGE_TAG=latest
```

---

### Spin Up the Full Stack

With secrets and the `.env` file in place, a **single command** builds all Docker images and starts the entire application stack:

```bash
docker-compose up --build
```

> **Tip:** Omit `--build` on subsequent runs to reuse cached images and start faster:
> ```bash
> docker-compose up
> ```

> **Tip:** To run in the background (detached mode), add the `-d` flag:
> ```bash
> docker-compose up -d
> ```

Once all containers are healthy, the application is accessible at:

| Service | URL | Notes |
|---|---|---|
| **Frontend** | http://localhost:4200 | Angular SPA via Nginx |
| **User Service API** | http://localhost:8088 | Spring Boot REST API |
| **User Service Swagger** | http://localhost:8088/swagger-ui.html | OpenAPI 3 docs |
| **Sensor Service API** | http://localhost:8081 | Spring Boot REST API |
| **Sensor Service Swagger** | http://localhost:8081/swagger-ui.html | OpenAPI 3 docs |
| **MySQL** | localhost:3307 | Accessible via any DB client |

**Startup order is guaranteed by health checks.** MySQL must pass `mysqladmin ping` before the backend services start, and both backend services must be available before the frontend container completes its startup dependency chain.

---

### Tear Down

To **stop** all running containers and remove them along with the Docker Compose-defined network:

```bash
docker-compose down
```

To also **remove the persistent MySQL data volume** (⚠️ this deletes all database data):

```bash
docker-compose down -v
```

---

### Manual Service Execution

For local development, you can run individual services outside of Docker.

#### ▶ Run the User Service (Spring Boot)

```bash
cd backend/user

# Build the project (skip tests for speed):
./mvnw clean package -DskipTests

# Run the compiled JAR directly:
java -jar target/user-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3307/project_db \
  --spring.datasource.username=your_db_user \
  --spring.datasource.password=your_db_password

# Or use the Spring Boot Maven plugin for hot-reload:
./mvnw spring-boot:run
```

#### ▶ Run the Sensor Data Service (Spring Boot)

```bash
cd backend/sensor_data

# Build and run:
./mvnw clean package -DskipTests
java -jar target/sensor_data-0.0.1-SNAPSHOT.jar

# Or with the Maven plugin:
./mvnw spring-boot:run
```

#### ▶ Run the Angular Frontend (Development Server)

The development server proxies API calls to the backends via `proxy.conf.json`:

```bash
cd Frontend

# Install dependencies (first time only):
npm install

# Start the dev server with hot module replacement:
npm start
# Equivalent: ng serve --proxy-config proxy.conf.json
```

The dev server will be available at **http://localhost:4200** with live reload on file changes.

#### ▶ Run the Sensor Simulator Manually

```bash
cd simulator

# Install Python dependency:
pip install requests

# Run the simulator (targets localhost:8081):
python simulator.py
```

---

## 🧪 Running the Test Suites

### Frontend E2E Tests (Selenium / TestNG)

> **Prerequisite:** The full Docker Compose stack must be running (`docker-compose up -d`) before executing E2E tests, as the tests drive the live application.

```bash
cd FrontendTestingFramework

# Run the full test suite (defined in testng.xml):
mvn clean test

# Run only the sanity/smoke suite (defined in sanity.xml):
mvn clean test -DsuiteXmlFile=sanity.xml

# Run a specific test class:
mvn clean test -Dtest=AirPollutionAlertsTest

# Run tests with a custom application URL:
mvn clean test -DAPP_URL=http://localhost:4200

# Generate and open the Allure report after a test run:
mvn allure:report
# Then open: target/site/allure-maven-plugin/index.html

# Or serve the Allure report live:
mvn allure:serve
```

### Backend API Tests (Postman / Newman)

> **Prerequisite:** Node.js must be installed. Install Newman globally:

```bash
npm install -g newman
```

```bash
# Run the full Sprint 1-3 backend test collection:
newman run "Backend Testing/Project — Sprint 1 , 2 and 3  BE Tests.postman_collection.json" \
  --environment "Backend Testing/New Environment.postman_environment.json"

# Run the Sprint 4-5 collection:
newman run "Backend Testing/Sprint 4 and Sprint 5.postman_collection.json" \
  --environment "Backend Testing/New Environment.postman_environment.json"

# Run the Sanity Check collection (recommended for post-deployment verification):
newman run "Backend Testing/🩺 Sanity Checks — Full System.postman_collection.json" \
  --environment "Backend Testing/New Environment.postman_environment.json" \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export results/sanity-report.html
```

---

## ⚙️ CI/CD Pipeline

The project ships with a **self-contained Jenkins server** defined in [`jenkins-compose.yml`](./jenkins-compose.yml). The custom [`jenkins/Dockerfile`](./jenkins/Dockerfile) extends the official Jenkins LTS image with Docker, Docker Compose, and Git pre-installed — so the Jenkins container can build and push Docker images directly from the pipeline without any additional setup on the host.

---

### 🚀 Running Jenkins

#### Step 1 — Start the Jenkins Container

From the project root, spin up Jenkins using its dedicated Compose file:

```bash
docker-compose -f jenkins-compose.yml up -d --build
```

Jenkins will be available at **http://localhost:8080**.

> **Note:** Jenkins data is persisted in `C:\jenkins_home` on Windows. This directory is bind-mounted into the container so your jobs, credentials, and plugin state survive container restarts.

---

#### Step 2 — Unlock Jenkins (First Time Only)

On first startup, Jenkins generates a one-time **admin unlock password**. Retrieve it by reading the log:

```bash
docker logs internship-jenkins
```

Look for a block like this in the output:

```
*************************************************************
*************************************************************

Jenkins initial setup is required. An admin user has been created
and a password generated.
Please use the following password to proceed to installation:

  a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6

This may also be found at:
/var/jenkins_home/secrets/initialAdminPassword
*************************************************************
```

Copy the password, open **http://localhost:8080**, paste it into the **Unlock Jenkins** screen, and click **Continue**.

Alternatively, read the password directly from the container:

```bash
docker exec internship-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

---

#### Step 3 — Install Plugins

When prompted, choose **"Install suggested plugins"**. Jenkins will automatically install the most commonly used plugins (Git, Pipeline, Docker Pipeline, Credentials Binding, etc.). Wait for the installation to complete before proceeding.

---

#### Step 4 — Create the Admin User

Fill in your preferred username, password, full name, and email address, then click **Save and Continue** → **Save and Finish** → **Start using Jenkins**.

---

#### Step 5 — Configure Required Credentials

Navigate to **Dashboard → Manage Jenkins → Credentials → System → Global credentials → Add Credentials** and create the following entries exactly as named (the `Jenkinsfile` references these IDs):

| Credential ID | Kind | Value |
|---|---|---|
| `dockerhub-creds` | Username & Password | Your DockerHub username + password |
| `MYSQL_PASSWORD` | Secret Text | MySQL user password |
| `MYSQL_USER` | Secret Text | MySQL username |
| `MYSQL_ROOT_PASSWORD` | Secret Text | MySQL root password |
| `CLOUDINARY_CLOUD_NAME` | Secret Text | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Secret Text | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Secret Text | Cloudinary API secret |

---

#### Step 6 — Create the Pipeline Job

1. From the Jenkins Dashboard click **"New Item"**.
2. Enter a name (e.g., `bayond-code-pipeline`) and select **"Pipeline"**, then click **OK**.
3. Scroll to the **Pipeline** section and set **Definition** to `Pipeline script from SCM`.
4. Set **SCM** to `Git` and enter the repository URL:
   ```
   https://github.com/AbdElkader-moh/FullProjectIntern.git
   ```
5. Set **Branch Specifier** to `*/main`.
6. Set **Script Path** to `Jenkinsfile`.
7. Click **Save**.

---

#### Step 7 — Trigger a Build

Click **"Build Now"** on the pipeline job page. Jenkins will execute all stages in order:

```
Checkout → Prepare Secrets → Verify Secrets → Build Docker Images → Docker Login → Push to DockerHub → Deploy
```

Monitor live progress in **Console Output**. A green ✅ indicates the pipeline completed successfully.

---

#### Stopping Jenkins

```bash
# Stop the Jenkins container (preserves all data in C:\jenkins_home):
docker-compose -f jenkins-compose.yml down
```

---

### Pipeline Stage Reference

| Stage | Description |
|---|---|
| **Checkout** | Pulls the latest code from the SCM (GitHub) |
| **Prepare Secrets** | Writes Jenkins-managed credentials into the `secrets/` directory |
| **Verify Secrets** | Confirms all secret files are present before building |
| **Build Images** | Runs `docker compose build` to build all service images |
| **Docker Login** | Authenticates to DockerHub using stored Jenkins credentials |
| **Push Images** | Tags and pushes all four service images to DockerHub |
| **Deploy** | Tears down the existing stack and relaunches with `docker compose up -d` |

---

## ☸️ Kubernetes Deployment

Production-grade manifests are provided in the `k8s/` directory:

```bash
# Apply all Kubernetes manifests to your cluster:
kubectl apply -f k8s/

# Check deployment status:
kubectl get pods
kubectl get services
kubectl get ingress

# View logs for a specific service:
kubectl logs -l app=user-service --tail=100
kubectl logs -l app=sensor-service --tail=100
```

**Manifest inventory:**

| File | Description |
|---|---|
| `mysql-deployment.yaml` | MySQL StatefulSet with PVC |
| `mysql-pvc.yaml` | Persistent Volume Claim for MySQL data |
| `mysql-service.yaml` | ClusterIP service for internal DB access |
| `user-service-deployment.yaml` | User Service Deployment + env config |
| `user-service-service.yaml` | ClusterIP service for User Service |
| `sensor-service-deployment.yaml` | Sensor Service Deployment |
| `sensor-service-service.yaml` | ClusterIP service for Sensor Service |
| `simulator-deployment.yaml` | Simulator Deployment |
| `frontend-deployment.yaml` | Nginx Frontend Deployment |
| `frontend-service.yaml` | NodePort/LoadBalancer service for Frontend |
| `ingress.yaml` | Ingress rules routing traffic to frontend and APIs |

---

## 📁 Project Structure

```
FullProjectIntern/
├── Frontend/                        # Angular 21 SPA
│   ├── src/app/
│   │   ├── components/              # 18 feature components (dashboards, analytics, alerts)
│   │   ├── guards/                  # JWT auth guard
│   │   ├── interceptors/            # HTTP JWT injection interceptor
│   │   ├── models/                  # TypeScript data models
│   │   └── services/                # API service layer (HttpClient wrappers)
│   ├── Dockerfile                   # Multi-stage: Node build → Nginx serve
│   └── nginx.conf                   # SPA-aware Nginx configuration
│
├── backend/
│   ├── user/                        # User Service (Spring Boot 4, Java 17)
│   │   ├── src/main/java/           # Controllers, Services, Repos, Security, JPA Entities
│   │   └── Dockerfile
│   └── sensor_data/                 # Sensor Data Service (Spring Boot 3.4, Java 17)
│       ├── src/main/java/           # Sensor ingestion, threshold eval, WebSocket, alert APIs
│       └── Dockerfile
│
├── FrontendTestingFramework/        # Selenium E2E Test Automation (Java 11, TestNG)
│   ├── src/test/java/
│   │   ├── base/                    # BaseTest setup (WebDriver init, login helpers)
│   │   ├── pages/                   # Page Object Model classes
│   │   ├── tests/                   # TestNG test classes (TC-xxx)
│   │   ├── utils/                   # SensorApiClient, RetryHelper, Excel reader
│   │   ├── listeners/               # Allure, ExtentReports, TestNG listeners
│   │   └── data/                    # Test data providers
│   ├── testng.xml                   # Full regression suite
│   ├── sanity.xml                   # Smoke/sanity suite
│   └── pom.xml
│
├── Backend Testing/                 # Postman API test collections
│   ├── Project — Sprint 1,2,3.postman_collection.json
│   ├── Sprint 4 and Sprint 5.postman_collection.json
│   └── 🩺 Sanity Checks — Full System.postman_collection.json
│
├── simulator/                       # Python IoT data simulator
│   └── simulator.py
│
├── db/                              # MySQL Docker init scripts
├── k8s/                             # Kubernetes deployment manifests
├── jenkins/                         # Jenkins configuration
├── secrets/                         # ⚠️ Git-ignored — runtime secrets only
├── docker-compose.yml               # Full local environment orchestration
├── jenkins-compose.yml              # Jenkins server Docker Compose
├── Jenkinsfile                      # Declarative CI/CD pipeline definition
└── .env                             # DockerHub username & image tag (git-ignored)
```

---

## 🤝 Contributing

1. Fork the repository and create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes, following the existing code style (Prettier for frontend, standard Java formatting for backend).

3. Add or update relevant tests in `FrontendTestingFramework/` (for UI changes) or `Backend Testing/` (for API changes).

4. Ensure all tests pass:
   ```bash
   # Backend tests
   cd FrontendTestingFramework && mvn clean test

   # API tests
   newman run "Backend Testing/🩺 Sanity Checks — Full System.postman_collection.json" \
     --environment "Backend Testing/New Environment.postman_environment.json"
   ```

5. Commit with a descriptive message and open a pull request against `main`.

---

<div align="center">

**Built with ❤️ by BeyondCode**

</div>
