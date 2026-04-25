#!/bin/bash

set -e

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

echo "Waiting for MySQL..."
sleep 20

echo "Building backend..."
cd backend/user
mvn clean package -DskipTests
docker build --no-cache -t spring-user-app .

echo "Starting backend..."
docker run -d --name internship-backend \
  --network project-net \
  -p 8080:8080 \
  spring-user-app

cd ../..

echo "Building frontend..."
cd Frontend
docker build --no-cache -t angular-frontend .

echo "Starting frontend..."
docker run -d --name internship-frontend \
  --network project-net \
  -p 4200:80 \
  angular-frontend

cd ..

echo "Done."
echo "Open: http://localhost:4200"