@echo off
echo Building Orchestrator Framework with MongoDB support...

echo.
echo Building Maven modules...
call mvn clean install -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Maven build failed!
    exit /b 1
)

echo.
echo Building MongoDB Docker image...
docker build -f Dockerfile.mongo -t payments-orchestrator-mongo:latest .

if %ERRORLEVEL% neq 0 (
    echo Docker build failed!
    exit /b 1
)

echo.
echo Build completed successfully!
echo Run: docker-compose -f docker-compose.mongo.yml up