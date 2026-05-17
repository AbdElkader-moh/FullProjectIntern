Write-Host "Stopping containers..."
docker compose down

Write-Host "Building and starting containers..."
docker compose up --build