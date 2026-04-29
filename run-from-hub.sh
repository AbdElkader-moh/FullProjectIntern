#!/bin/bash

set -e

echo "==============================="
echo "   DXC Internship Project"
echo "==============================="

echo "Creating Docker network..."
docker network create project-net 2>/dev/null || true

echo "Removing old containers..."
docker rm -f internship-frontend internship-backend internship-mysql 2>/dev/null || true

echo "Starting MySQL..."
docker run -d --name internship-mysql \
  --network project-net \
  -p 3306:3306 \
  -v mysql_data:/var/lib/mysql \
  -e MYSQL_DATABASE=project_db \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_USER=appuser \
  -e MYSQL_PASSWORD=app123 \
  mysql:8.0

echo "Waiting for MySQL to initialize..."
sleep 20

echo "Pulling and starting backend..."
docker pull abdelkader112002/backend-service:v1.0
docker run -d --name internship-backend \
  --network project-net \
  -p 8080:8080 \
  abdelkader112002/backend-service:v1.0

echo "Pulling and starting frontend..."
docker pull abdelkader112002/frontend-service:v1.0
docker run -d --name internship-frontend \
  --network project-net \
  -p 4200:80 \
  abdelkader112002/frontend-service:v1.0

echo "==============================="
echo "Done! Open: http://localhost:4200"
echo "==============================="
