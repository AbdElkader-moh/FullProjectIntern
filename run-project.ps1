$ErrorActionPreference = "Stop"

Write-Host "Checking if Docker is running..."
$oldErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$dockerStatus = docker info 2>&1
$exitCode = $LASTEXITCODE
$ErrorActionPreference = $oldErrorActionPreference

if ($exitCode -ne 0) {
  Write-Host "Error: Docker daemon is not running! Please start Docker Desktop and try again." -ForegroundColor Red
  exit 1
}

Write-Host "Creating Docker network..."
try {
  # Attempt to create network; if it already exists, it will fail, which we can ignore.
  docker network create project-net 2>$null
} catch {}

Write-Host "Removing old containers..."
# Attempt to remove containers; ignore errors if they do not exist
try {
  docker rm -f internship-frontend internship-backend internship-sensor internship-simulator internship-mysql 2>$null
} catch {}

Write-Host "Starting MySQL..."
if (Test-Path ".env") {
  docker run -d --name internship-mysql `
    --network project-net `
    -p 3306:3306 `
    -v mysql_data:/var/lib/mysql `
    --env-file .env `
    mysql:8.0
} else {
  Write-Host "Error: .env file missing! Cannot start database securely."
  exit 1
}

Write-Host "Waiting for MySQL..."
Start-Sleep -Seconds 20

Write-Host "Building backend..."
Set-Location backend/user
# Ensure Maven is available or use wrapper
if (Get-Command mvn -ErrorAction SilentlyContinue) {
  mvn clean package -DskipTests
} elseif (Test-Path ".\mvnw.cmd") {
  .\mvnw.cmd clean package -DskipTests
} else {
  Write-Host "Error: Maven (mvn) not found and mvnw wrapper missing. Install Maven or ensure mvnw.cmd is present in backend/user."
  exit 1
}

if (-not (Test-Path "target/user*.jar" -PathType Leaf)) {
  Write-Host "Error: User JAR not found in target directory after build!"
  exit 1
}

docker build --no-cache -t spring-user-app .

Write-Host "Starting backend..."
if (Test-Path "..\..\.env") {
  Write-Host "Found .env file, injecting secrets..."
  docker run -d --name internship-backend `
    --network project-net `
    -p 8080:8080 `
    --env-file ..\..\.env `
    spring-user-app
} else {
  Write-Host "No .env file found. Running without secrets..."
  docker run -d --name internship-backend `
    --network project-net `
    -p 8080:8080 `
    spring-user-app
}

Set-Location ../sensor_data

Write-Host "Building sensor backend..."
if (Get-Command mvn -ErrorAction SilentlyContinue) {
  mvn clean package -DskipTests
} elseif (Test-Path "../user/mvnw.cmd") {
  $mvnwPath = Resolve-Path "../user/mvnw.cmd"
  & $mvnwPath clean package -DskipTests
} else {
  Write-Host "Error: Maven not found and wrapper missing. Cannot build sensor service."
  exit 1
}

if (-not (Test-Path "target/sensor_data*.jar" -PathType Leaf)) {
  Write-Host "Error: Sensor JAR not found in target directory after build!"
  exit 1
}

docker build --no-cache -t spring-sensor-app .

Write-Host "Starting sensor backend..."
if (Test-Path "..\..\.env") {
  docker run -d --name internship-sensor `
    --network project-net `
    -p 8081:8081 `
    --env-file ..\..\.env `
    spring-sensor-app
} else {
  docker run -d --name internship-sensor `
    --network project-net `
    -p 8081:8081 `
    spring-sensor-app
}

Set-Location ..

Write-Host "Building Python Simulator..."
docker build -f Dockerfile.sim -t sensor-simulator .

Write-Host "Starting Python Simulator..."
if (Test-Path ".env") {
  docker run -d --name internship-simulator `
    --network project-net `
    -e SENSOR_HOST=internship-sensor `
    --env-file .env `
    sensor-simulator
} else {
  docker run -d --name internship-simulator `
    --network project-net `
    -e SENSOR_HOST=internship-sensor `
    sensor-simulator
}

Set-Location ..

Write-Host "Building frontend..."
Set-Location Frontend
docker build --no-cache -t angular-frontend .

Write-Host "Starting frontend..."
docker run -d --name internship-frontend `
  --network project-net `
  -p 4200:80 `
  angular-frontend

Set-Location ..

Write-Host "Done."
Write-Host "Open: http://localhost:4200"