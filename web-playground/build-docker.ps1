# Build script for multi-module Docker deployment
# This script:
# 1. Builds both Gradle modules (main-app and third-party-service)
# 2. Creates Docker images for each module
# 3. Ready to be deployed via docker-compose

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building Multi-Module Application" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Clean and build both modules
Write-Host "Step 1: Building Gradle modules..." -ForegroundColor Yellow
.\gradlew.bat clean :main-app:shadowJar :third-party-service:shadowJar --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Host "Gradle build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Gradle build completed successfully!" -ForegroundColor Green
Write-Host ""

# Step 2: Build Docker image for main-app
Write-Host "Step 2: Building Docker image for main-app..." -ForegroundColor Yellow
docker build -f main-app/Dockerfile -t main-app:latest ./main-app

if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker build failed for main-app!" -ForegroundColor Red
    exit 1
}

Write-Host "main-app Docker image built successfully!" -ForegroundColor Green
Write-Host ""

# Step 3: Build Docker image for third-party-service
Write-Host "Step 3: Building Docker image for third-party-service..." -ForegroundColor Yellow
docker build -f third-party-service/Dockerfile -t third-party-service:latest ./third-party-service

if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker build failed for third-party-service!" -ForegroundColor Red
    exit 1
}

Write-Host "third-party-service Docker image built successfully!" -ForegroundColor Green
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Docker images created:" -ForegroundColor Green
Write-Host "  - main-app:latest" -ForegroundColor White
Write-Host "  - third-party-service:latest" -ForegroundColor White
Write-Host ""
Write-Host "To start the application, run:" -ForegroundColor Yellow
Write-Host "  docker-compose up -d" -ForegroundColor White
Write-Host ""
Write-Host "To view logs:" -ForegroundColor Yellow
Write-Host "  docker-compose logs -f" -ForegroundColor White
Write-Host ""
Write-Host "To stop the application:" -ForegroundColor Yellow
Write-Host "  docker-compose down" -ForegroundColor White

