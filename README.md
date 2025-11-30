# Attendance & Access Control API

Enterprise-grade REST API for IoT-based attendance and access control system built with Java 21, Spring Boot 3.5.x, PostgreSQL 17, and Firebase RTDB integration.

## Features

- **Worker Management**: Complete CRUD operations for workers
- **Biometric Authentication**: Fingerprint-based access control
- **RFID Attendance**: Automatic attendance tracking via RFID tags
- **Real-time Sync**: Firebase Realtime Database integration
- **Security Logging**: Comprehensive audit trail
- **Simulation Mode**: Test different scenarios with time manipulation

## Tech Stack

- Java 21
- Spring Boot 3.5.0
- PostgreSQL 17
- Firebase Realtime Database
- Maven
- Swagger/OpenAPI 3.0
- Lombok
- MapStruct

## Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL 17
- Firebase Account
- IntelliJ IDEA Ultimate (recommended)

## Quick Start

### 1. Clone the repository
```bash
git clone https://github.com/yourusername/attendance-access-control-api.git
cd attendance-access-control-api
```

### 2. Configure PostgreSQL
```bash
# Using Docker Compose
docker-compose up -d postgres

# Or install PostgreSQL 17 manually and create database
psql -U postgres
CREATE DATABASE attendance_db;
```

### 3. Configure Firebase

1. Download Firebase credentials from Firebase Console
2. Place `firebase-credentials.json` in `src/main/resources/`
3. Update `application.yml` with your Firebase database URL

### 4. Run the application
```bash
mvn clean install
mvn spring-boot:run
```

### 5. Access Swagger UI
```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

### Workers
- `POST /api/v1/workers` - Create worker
- `GET /api/v1/workers` - List all workers
- `GET /api/v1/workers/{id}` - Get worker by ID
- `PUT /api/v1/workers/{id}` - Update worker
- `DELETE /api/v1/workers/{id}` - Delete worker

### Attendance
- `POST /api/v1/attendance/check-in` - Record check-in
- `POST /api/v1/attendance/check-out` - Record check-out
- `GET /api/v1/attendance/date/{date}` - Get attendance by date

### Access Control
- `POST /api/v1/access/verify` - Verify fingerprint access
- `GET /api/v1/access/worker/{workerId}` - Get access logs

### Firebase Admin
- `POST /api/v1/firebase/admin/command/register` - Register fingerprint
- `POST /api/v1/firebase/admin/command/delete` - Delete fingerprint

## Project Structure
```
src/main/java/com/iot/attendance/
├── config/                 # Configuration classes
├── domain/                 # Domain models and value objects
├── application/            # DTOs, services, mappers
├── infrastructure/         # Repositories, Firebase, exceptions
└── presentation/           # REST Controllers
```

## Configuration

Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/attendance_db
    username: postgres
    password: your_password

firebase:
  database-url: https://your-project.firebaseio.com/
  credentials-path: firebase-credentials.json
```

## License

MIT License

## Support

For support, email support@attendance-iot.com