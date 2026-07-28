# Sensor Data Service

A Spring Boot microservice for ingesting, storing, and querying IoT sensor telemetry across three domains: **traffic**, **air pollution**, and **street lighting**.

## Overview

This service exposes REST endpoints for:

- **Ingesting** sensor readings (POST) from physical/simulated IoT devices
- **Querying** historical readings with filtering, sorting, and pagination (GET)
- **Aggregated statistics** for dashboard summary cards
- **Trend data** for dashboard charts (last 50 readings per domain)
- **Real-time alerts** broadcast over WebSocket when a reading crosses a user-configured threshold

## Tech Stack

- **Java 17**, **Spring Boot 3.4**
- **Spring Data JPA** (Hibernate) + **MySQL**
- **Spring WebSocket** (STOMP) for live alert broadcasting
- **Spring Security** JWT-based auth on GET endpoints (ingestion endpoints are open for device write access)
- **springdoc-openapi** (Swagger UI) for API documentation
- **SonarCloud** for static analysis and quality gating

## Project Structure

```
src/main/java/com/backend/sensor_data/
├── controller/          # REST endpoints (SensorController)
├── service/              # Business logic (SensorDataService)
│   ├── processor/        # Per-domain ingestion pipelines (validate → map → save → alert → broadcast)
│   ├── strategy/          # Per-domain threshold-checking logic
│   └── factory/           # Routes DTOs to the correct processor
├── entity/                # JPA entities (TrafficData, AirPollutionData, StreetLightData, Settings, Notification)
├── dto/                    # Request/response payloads
├── repository/           # Spring Data JPA repositories
├── exception/             # Centralized exception handling (@ControllerAdvice)
├── filter/                 # JWT authentication filter
├── config/                 # DataSource, OpenAPI, WebSocket configuration
└── util/                    # JWT and secret-reading utilities
```

## Domains

| Domain | Entity | Key fields |
|---|---|---|
| Traffic | `TrafficData` | `location`, `trafficDensity` (0–500), `avgSpeed` (0–120), `congestionLevel` ('Low', 'Moderate', 'High', 'Severe')|
| Air Pollution | `AirPollutionData` | `location`, `co` (0–50 ppm), `ozone` (0–300 ppb), `pm25`, `pm10`, `no2`, `so2`, `pollutionLevel` ('Good', 'Moderate', 'Unhealthy', 'Very_Unhealthy')|
| Street Lighting | `StreetLightData` | `location`, `brightnessLevel` (0–100), `powerConsumption` (0–5000), `status` ('ON', 'OFF')|

Each domain follows the same **processor pipeline**: `validate → mapToEntity → save → checkThreshold → broadcast`, implemented via a shared `AbstractSensorProcessor` template method and routed through `SensorProcessorFactory`.

## API Endpoints

Base path: `/api/sensors`

### Ingestion (no auth required)
- `POST /traffic`: Submit a traffic reading
- `POST /air`: Submit an air quality reading
- `POST /light`: Submit a street light reading

### Querying (JWT required)
- `GET /traffic`, `GET /air`, `GET /light`: Paginated, Filterable, Sortable list of readings
- `GET /traffic/stats`, `GET /air/stats`, `GET /light/stats`: Aggregated dashboard statistics
- `GET /traffic/trends`, `GET /air/trends`, `GET /light/trends`: Last 50 readings, Most recent first
- `GET /traffic/congestion-summary`: Reading counts grouped by congestion level

### Query parameters (list endpoints)
- `page`, `size`: Standard pagination
- `sort`: `field,direction` (e.g. `sort=timestamp,desc`)
- Domain-specific filters: `location`, Date Range (`from`/`to`), `congestionLevel` / `pollutionLevel` / `status`

**Note on air pollution sorting:** the public API accepts `sort=pm2_5,desc` (matching the JSON field name in request/response bodies) even though the underlying Java property is `pm25`. This is resolved via an internal field-alias map in `SensorController`, keeping the public contract stable regardless of internal naming.

Full interactive documentation is available via Swagger UI once the app is running (see below).

## Alerts

Each domain has a `ThresholdStrategy` that checks incoming readings against user-defined `Settings` (per-user, per-metric thresholds with `above`/`below` alert types). A crossed threshold:
1. Logs a warning
2. Persists a `Notification`
3. Broadcasts a WebSocket message to `/topic/alerts/{userId}`

Every saved reading is also broadcast to a domain-wide topic (`/topic/traffic`, `/topic/air`, `/topic/light`) for live dashboard updates.

## Running Locally

### Prerequisites
- JDK 17+
- MySQL instance running and reachable
- Maven

### Configuration
Set your database connection and any required secrets via environment variables or `application.properties` (see `DataSourceConfig` / `SecretReader`).

### Build & run

```bash
# Windows
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw clean install
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8081` (adjust per your configured port), with Swagger UI at `/swagger-ui.html`.

### Running tests

```bash
.\mvnw.cmd test        # Windows
./mvnw test             # macOS/Linux
```

Coverage reports are generated under `target/site/jacoco/index.html` after a test run.

## Database

Uses MySQL with native `ENUM` columns for `congestionLevel`, `pollutionLevel`, and `status`: Java enum constant casing (`Low`, `Moderate`, `High`, `Severe`, etc.) intentionally mirrors the DB `ENUM` values rather than following standard Java UPPER_SNAKE_CASE convention, since changing one without the other would require a coordinated migration and API-compatibility layer.
