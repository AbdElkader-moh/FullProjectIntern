#!/bin/bash

set -e

DOCKER_USERNAME="abdelkader112002"

echo "Building backend..."
cd backend/user
mvn clean package -DskipTests
docker build -t backend-service:v1.0 .
docker tag backend-service:v1.0 $DOCKER_USERNAME/backend-service:v1.0
cd ../..

echo "Building frontend..."
cd Frontend
docker build -t frontend-service:v1.0 .
docker tag frontend-service:v1.0 $DOCKER_USERNAME/frontend-service:v1.0
cd ..

echo "Pushing images..."
docker push $DOCKER_USERNAME/backend-service:v1.0
docker push $DOCKER_USERNAME/frontend-service:v1.0

echo "Done."