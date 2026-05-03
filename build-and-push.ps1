$DockerUsername = "abdelkader112002"

Write-Host "Building backend..."
cd backend/user
docker build -t backend-service:v1.0 .
docker tag backend-service:v1.0 "$DockerUsername/backend-service:v1.0"
cd ../..

Write-Host "Building frontend..."
cd Frontend
docker build -t frontend-service:v1.0 .
docker tag frontend-service:v1.0 "$DockerUsername/frontend-service:v1.0"
cd ..

Write-Host "Pushing images..."
docker push "$DockerUsername/backend-service:v1.0"
docker push "$DockerUsername/frontend-service:v1.0"

Write-Host "Done."