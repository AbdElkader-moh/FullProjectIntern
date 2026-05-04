$ErrorActionPreference = "Stop"

Write-Host "Creating Docker network..."
if (-not (docker network ls --filter name=project-net -q)) {
  docker network create project-net
}

Write-Host "Removing old containers..."
# Attempt to remove containers; ignore errors if they do not exist
try {
  docker rm -f internship-frontend internship-backend internship-mysql 2>$null
} catch {}

Write-Host "Starting MySQL..."
docker run -d --name internship-mysql `
  --network project-net `
  -p 3306:3306 `
  -v mysql_data:/var/lib/mysql `
  -e MYSQL_DATABASE=project_db `
  -e MYSQL_ROOT_PASSWORD=root123 `
  -e MYSQL_USER=appuser `
  -e MYSQL_PASSWORD=app123 `
  mysql:8.0

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

docker build --no-cache -t spring-user-app .

Write-Host "Starting backend..."
docker run -d --name internship-backend `
  --network project-net `
  -p 8080:8080 `
  spring-user-app

Set-Location ../..

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