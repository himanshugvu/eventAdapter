@echo off
echo Building Orchestrator Framework with PostgreSQL support...

echo.
echo Building Maven modules...
call mvn clean install -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Maven build failed!
    exit /b 1
)

echo.
echo Building PostgreSQL Docker image...
docker build -f Dockerfile.postgres -t payments-orchestrator-postgres:latest .

if %ERRORLEVEL% neq 0 (
    echo Docker build failed!
    exit /b 1
)

echo.
echo Build completed successfully!
echo Run: docker-compose -f docker-compose.postgres.yml up