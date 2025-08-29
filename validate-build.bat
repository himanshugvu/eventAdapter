@echo off
echo Validating Event Orchestrator Framework Build...

echo.
echo 1. Testing Maven build...
call mvn clean compile -q
if %ERRORLEVEL% neq 0 (
    echo FAILED: Maven compilation failed!
    exit /b 1
)
echo PASSED: Maven compilation successful

echo.
echo 2. Testing Maven package...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo FAILED: Maven packaging failed!
    exit /b 1
)
echo PASSED: Maven packaging successful

echo.
echo 3. Checking JAR files...
if not exist "payments-orchestrator-example\target\payments-orchestrator-example-1.0.0.jar" (
    echo FAILED: PostgreSQL example JAR not found!
    exit /b 1
)
echo PASSED: PostgreSQL JAR exists

if not exist "payments-orchestrator-mongo-example\target\payments-orchestrator-mongo-example-1.0.0.jar" (
    echo FAILED: MongoDB example JAR not found!
    exit /b 1
)
echo PASSED: MongoDB JAR exists

echo.
echo 4. Validating Docker build context...
if not exist "Dockerfile.postgres" (
    echo FAILED: PostgreSQL Dockerfile missing!
    exit /b 1
)
echo PASSED: PostgreSQL Dockerfile exists

if not exist "Dockerfile.mongo" (
    echo FAILED: MongoDB Dockerfile missing!
    exit /b 1
)
echo PASSED: MongoDB Dockerfile exists

echo.
echo 5. Checking deployment configurations...
if not exist "docker-compose.postgres.yml" (
    echo FAILED: PostgreSQL Docker Compose missing!
    exit /b 1
)
echo PASSED: PostgreSQL Docker Compose exists

if not exist "docker-compose.mongo.yml" (
    echo FAILED: MongoDB Docker Compose missing!
    exit /b 1
)
echo PASSED: MongoDB Docker Compose exists

if not exist "k8s\deployment.yaml" (
    echo FAILED: Kubernetes deployment missing!
    exit /b 1
)
echo PASSED: Kubernetes manifests exist

if not exist "helm\orchestrator\Chart.yaml" (
    echo FAILED: Helm chart missing!
    exit /b 1
)
echo PASSED: Helm chart exists

echo.
echo ✅ All validations passed! The Event Orchestrator Framework is ready.
echo.
echo Next steps:
echo   PostgreSQL variant: build-postgres.bat ^&^& docker-compose -f docker-compose.postgres.yml up
echo   MongoDB variant:    build-mongo.bat ^&^& docker-compose -f docker-compose.mongo.yml up