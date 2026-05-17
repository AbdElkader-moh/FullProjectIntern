$DockerUsername = "abdelkader112002"

Write-Host "Building backend..."
cd backend/user
docker build -t user-service:v2.0 .
docker tag user-service:v2.0 "$DockerUsername/user-service:v2.0"
cd ../..

Write-Host "Building backend..."
cd backend/sensor_data
docker build -t sensor-service:v2.0 .
docker tag sensor-service:v2.0 "$DockerUsername/sensor-service:v2.0"
cd ../..



Write-Host "Building frontend..."
cd Frontend
docker build -t frontend-service:v2.0 .
docker tag frontend-service:v2.0 "$DockerUsername/frontend-service:v2.0"
cd ..

Write-Host "Pushing images..."
docker push "$DockerUsername/user-service:v2.0"
docker push "$DockerUsername/sensor-service:v2.0"
docker push "$DockerUsername/frontend-service:v2.0"

Write-Host "Done."