$ErrorActionPreference = "Stop"

Write-Host "Creating Docker network..."
docker network create project-net
Write-Host "Removing old containers..."
docker rm -f internship-frontend internship-backend internship-mysql 2>$null

Write-Host "Starting MySQL..."
docker run -d --name internship-mysql `
  --network project-net `
  -p 3306:3306 `
  -v mysql_data:/var/lib/mysql `
  -e MYSQL_ROOT_PASSWORD=root123 `
  -e MYSQL_PASSWORD=app123 `
  mysql:8.0

Write-Host "Waiting for MySQL..."
Start-Sleep -Seconds 20

Write-Host "Building backend..."
Set-Location backend/user
mvn clean package -DskipTests
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
