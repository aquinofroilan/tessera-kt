# CI/CD Pipeline

This repository uses GitHub Actions for continuous integration and continuous deployment.

## Pipeline Overview

The CI pipeline runs on every push to `main` or `develop` branches and on all pull requests. It performs the following steps:

### 1. **Kotlin Linting**
- Runs `ktlint` to check code style and formatting
- Ensures consistent Kotlin code style across the project
- Fails the build if linting errors are found

### 2. **Build Application**
- Compiles the Kotlin/Java code using Gradle
- Generates the JAR artifact
- Validates that the code compiles successfully

### 3. **Run Tests**
- Executes all unit and integration tests
- Uses MongoDB service container for database tests
- Uploads test results as artifacts

### 4. **Health Check**
- Starts the Spring Boot application in background
- Waits for application to be ready
- Pings the `/api/health` endpoint
- Verifies the application is running correctly
- Checks that MongoDB connection is working

### 5. **Upload Artifacts**
- Stores test results for 7 days
- Stores the built JAR file for 7 days

## Required Environment Variables

The pipeline uses the following environment variables:

- `MONGODB_URI`: MongoDB connection string (auto-configured in CI)
- `TEST_MONGODB_URI`: MongoDB connection for tests (auto-configured in CI)
- `JWT_SECRET`: Secret key for JWT tokens (auto-configured in CI)

## MongoDB Service

The pipeline spins up a MongoDB 7.0 container with:
- **Username**: `testuser`
- **Password**: `testpass`
- **Port**: `27017`
- **Health checks**: Ensures MongoDB is ready before running tests

## Running Locally

To run the same checks locally:

```bash
# Run linting
./gradlew ktlintCheck

# Auto-fix linting issues
./gradlew ktlintFormat

# Build the application
./gradlew build

# Run tests
./gradlew test

# Start the application
./gradlew bootRun
```

## Pipeline Status

Check the **Actions** tab in GitHub to see the status of recent pipeline runs.
