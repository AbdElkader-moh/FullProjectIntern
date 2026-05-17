#!/bin/bash

echo "Stopping containers..."
docker compose down

echo "Building and starting containers..."
docker compose up --build