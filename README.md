# Attendance & Access Control API

Enterprise-grade REST API for IoT-based attendance and access control system built with Java 21, Spring Boot 3.5.x, PostgreSQL 17, and Firebase Realtime Database integration.

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Core Features](#core-features)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Business Logic](#business-logic)
- [Database Schema](#database-schema)
- [Firebase Integration](#firebase-integration)
- [Deployment](#deployment)
- [Security](#security)

---

## Overview

This system provides a comprehensive solution for managing worker attendance and restricted area access control through IoT devices. The API serves as the central coordination layer between ESP32 microcontrollers equipped with RFID readers and fingerprint sensors, Firebase Realtime Database as the communication layer, and PostgreSQL for persistent data storage.

### Key Capabilities

- **Worker Management**: Complete CRUD operations with biometric and RFID registration
- **Attendance Tracking**: Automatic check-in/check-out detection via RFID tags
- **Access Control Auditing**: Fingerprint-based authentication logging for restricted areas
- **Real-time Event Processing**: Firebase polling service for hardware event detection
- **Simulation Mode**: Time manipulation for testing different scenarios
- **Night Shift Support**: Proper handling of work schedules crossing midnight

### Design Philosophy

The system follows a **separation of concerns** principle:

- **Hardware (ESP32)** handles access control decisions autonomously
- **API** focuses on attendance tracking, audit logging, and system management
- **Firebase RTDB** acts as a message broker between hardware and software
- **PostgreSQL** provides reliable persistent storage

---

## System Architecture

### Architectural Pattern: Hexagonal Architecture (Ports & Adapters)
```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│              (REST Controllers, DTOs)                        │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                         │
│         (Services, Use Cases, Mappers)                       │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│     (Entities, Value Objects, Business Logic)                │
├─────────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                        │
│   (Repositories, Firebase, External Services)                │
└─────────────────────────────────────────────────────────────┘
```

### Component Diagram
```
┌──────────────┐      REST API       ┌──────────────┐
│              │◄───────────────────►│              │
│  REST Client │                     │  Spring Boot │
│              │                     │     API      │
└──────────────┘                     └───────┬──────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
              ┌─────▼─────┐          ┌──────▼──────┐          ┌──────▼──────┐
              │           │          │             │          │             │
              │ PostgreSQL│          │  Firebase   │          │   ESP32     │
              │  Database │          │    RTDB     │          │  Hardware   │
              │           │          │             │          │             │
              └───────────┘          └──────▲──────┘          └──────┬──────┘
                                            │                        │
                                            └────────────────────────┘
                                                 REST Polling
```

### Key Architectural Decisions

1. **Hardware Autonomy**: ESP32 devices make access control decisions independently
2. **Event-Driven Processing**: Firebase polling service detects hardware events asynchronously
3. **Fixed RFID Pool**: System initializes with 5 preconfigured RFID tags
4. **Synchronous Biometric Registration**: Worker creation blocks until fingerprint capture completes
5. **Audit-Only Access Logs**: Access control decisions are logged for audit purposes

---

## Technology Stack

### Backend Framework
- **Java 21**: Latest LTS with modern language features
- **Spring Boot 3.5.8**: Enterprise application framework
- **Spring Data JPA**: Database abstraction layer
- **Hibernate**: ORM implementation with PostgreSQL dialect

### Database
- **PostgreSQL 17**: Primary relational database with advanced indexing
- **HikariCP**: High-performance JDBC connection pool

### Integration Layer
- **Firebase Admin SDK 9.4.2**: Realtime Database integration
- **Firebase REST API**: Polling-based event detection

### Hardware Platform
- **ESP32 Microcontrollers**: IoT device firmware
- **RFID-RC522**: RFID card reader modules
- **Adafruit Fingerprint Sensor**: Biometric authentication

### API Documentation
- **SpringDoc OpenAPI 2.8.3**: Swagger/OpenAPI 3.0 documentation
- **Swagger UI**: Interactive API exploration

---

## Project Structure
```
src/main/java/com/iot/attendance/
├── config/                          # Configuration classes
│   ├── FirebaseConfig.java         # Firebase initialization
│   ├── OpenApiConfig.java          # Swagger documentation
│   ├── PostgresConfig.java         # JPA configuration
│   └── SecurityConfig.java         # CORS & security
│
├── domain/                          # Core domain models
│   ├── model/                      # Aggregate roots & entities
│   ├── enums/                      # Domain enumerations
│   └── valueobjects/               # Value objects (RfidTag, FingerprintId)
│
├── application/                     # Application services
│   ├── service/                    # Service interfaces
│   ├── service/impl/               # Service implementations
│   ├── dto/                        # Request/Response DTOs
│   └── mapper/                     # Entity-DTO mappers
│
├── infrastructure/                  # External integrations
│   ├── persistence/
│   │   ├── entity/                 # JPA entities
│   │   └── repository/             # Spring Data repositories
│   ├── firebase/
│   │   ├── FirebaseRealtimeService.java    # Admin commands
│   │   └── FirebasePollingService.java     # Event listener
│   ├── config/                     # Infrastructure config
│   └── exception/                  # Custom exceptions
│
└── presentation/                    # REST controllers
    └── controller/                 # HTTP endpoints
```

---

## Core Features

### 1. Worker Management
- Create workers with automatic fingerprint registration
- Update worker information
- Assign/unassign RFID tags from fixed pool
- Grant/revoke restricted area access
- Delete workers with hardware cleanup

### 2. Attendance Tracking
- Automatic check-in/check-out detection
- Lateness calculation with configurable threshold
- Night shift support (schedules crossing midnight)
- Duration calculation with second-level precision

### 3. Access Control Auditing
- Access granted/denied event logging
- Worker identification via fingerprint
- Security alert generation

### 4. System Configuration
- Work schedule configuration
- Simulation mode for testing
- Real-time configuration updates

---

## Installation

### Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL 17
- Firebase Project with Realtime Database

### Quick Start

1. **Clone Repository**
```bash
git clone https://github.com/yourusername/attendance-access-control-api.git
cd attendance-access-control-api
```

2. **Database Setup**
```bash
docker-compose up -d postgres
```

3. **Firebase Configuration**
    - Download service account key from Firebase Console
    - Save as `firebase-credentials.json` in `src/main/resources/`

4. **Configure Application**

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

5. **Build and Run**
```bash
mvn clean install
mvn spring-boot:run
```

Access Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Configuration

### Application Profiles

**Development** (`application-dev.yml`)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

logging:
  level:
    com.iot.attendance: DEBUG
```

**Production** (create `application-prod.yml`)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate

logging:
  level:
    com.iot.attendance: INFO
```

### RFID Card Pool

System initializes with 5 fixed RFID tags on startup:
```java
private static final List<String> SYSTEM_RFID_UIDS = Arrays.asList(
    "3513B5B1",
    "85DB6DB1",
    "BA910FB1",
    "40C86F61",
    "FD5FC801"
);
```

### Night Shift Configuration

System automatically detects night shifts when `workStartTime > workEndTime`:
```json
{
  "workStartTime": "22:50:00",
  "workEndTime": "10:00:00",
  "lateThresholdMinutes": 10
}
```

---

## API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Worker Endpoints

#### Create Worker with Biometric Registration
```http
POST /api/v1/workers
Content-Type: application/json

{
  "firstName": "Juan",
  "lastName": "Pérez",
  "documentNumber": "12345678",
  "email": "juan.perez@company.com",
  "phoneNumber": "987654321",
  "hasRestrictedAreaAccess": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Worker created and fingerprint registered successfully",
  "data": {
    "id": 1,
    "fingerprintId": 15,
    "rfidTags": [],
    "status": "ACTIVE"
  }
}
```

#### List Available RFID Cards
```http
GET /api/v1/workers/rfid/unassigned
```

#### Assign RFID Tag to Worker
```http
POST /api/v1/workers/1/rfid-tags
Content-Type: application/json

{
  "rfidTag": "3513B5B1"
}
```

#### Get Worker by ID
```http
GET /api/v1/workers/1
```

#### Update Worker
```http
PUT /api/v1/workers/1
Content-Type: application/json

{
  "firstName": "Juan Carlos",
  "hasRestrictedAreaAccess": false
}
```

#### Delete Worker
```http
DELETE /api/v1/workers/1
```

---

### Attendance Endpoints

#### Get Latest Attendance
```http
GET /api/v1/attendance/worker/1/latest
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "workerId": 1,
    "workerFullName": "Juan Pérez",
    "rfidTag": "3513B5B1",
    "attendanceDate": "2025-12-03",
    "checkInTime": "2025-12-03T08:05:30",
    "checkOutTime": "2025-12-03T17:02:15",
    "workedDuration": "8h 56m 45s",
    "isLate": true,
    "latenessDuration": "5m 30s",
    "status": "CHECKED_OUT"
  }
}
```

#### Get Recent Attendances
```http
GET /api/v1/attendance/worker/1/recent?limit=10
```

#### Get Attendances by Date
```http
GET /api/v1/attendance/date/2025-12-03
```

#### Get Attendances by Date Range
```http
GET /api/v1/attendance/worker/1?startDate=2025-12-01&endDate=2025-12-31
```

#### Get Late Attendances
```http
GET /api/v1/attendance/date/2025-12-03/late
```

#### Count Late Attendances
```http
GET /api/v1/attendance/worker/1/late-count?startDate=2025-12-01&endDate=2025-12-31
```

---

### Access Audit Endpoints

#### Get Access History by Worker
```http
GET /api/v1/access-audit/worker/1
```

#### Get Access History by Time Range
```http
GET /api/v1/access-audit/time-range?startTime=2025-12-01T00:00:00&endTime=2025-12-31T23:59:59
```

#### Get Recent Denied Accesses
```http
GET /api/v1/access-audit/denied?hours=24
```

#### Get Recent Granted Accesses
```http
GET /api/v1/access-audit/granted?hours=24
```

---

### System Configuration Endpoints

#### Get Current Configuration
```http
GET /api/v1/system/config
```

#### Update Configuration
```http
PUT /api/v1/system/config
Content-Type: application/json

{
  "workStartTime": "22:50:00",
  "workEndTime": "10:00:00",
  "lateThresholdMinutes": 10
}
```

#### Enable Simulation Mode
```http
POST /api/v1/system/config/simulation/enable
Content-Type: application/json

{
  "simulatedDateTime": "2025-12-15T07:45:00"
}
```

#### Disable Simulation Mode
```http
POST /api/v1/system/config/simulation/disable
```

---

### Firebase Admin Endpoints

#### Send Register Command
```http
POST /api/v1/firebase/admin/command/register
```

#### Send Delete Command
```http
POST /api/v1/firebase/admin/command/delete?fingerprintId=15
```

#### Get Last Fingerprint ID
```http
GET /api/v1/firebase/admin/last-fingerprint-id
```

---

## Business Logic

### Attendance Check-In/Check-Out Flow

**Smart Detection Algorithm:**

The system automatically determines whether an RFID scan represents check-in or check-out:
```java
public AttendanceResponse processRfidEvent(RfidAttendanceRequest request) {
    // Normalize RFID
    String normalizedRfid = request.getRfidUid().toUpperCase().replace(" ", "").trim();
    
    // Find card and worker
    RfidCardEntity card = rfidCardRepository.findById(normalizedRfid)
            .orElseThrow(() -> new ResourceNotFoundException("RFID not registered"));
    
    WorkerEntity worker = card.getWorker();
    
    // Check for active attendance
    Optional<AttendanceEntity> activeAttendance = 
            attendanceRepository.findActiveAttendanceByWorkerId(worker.getId());
    
    if (activeAttendance.isPresent()) {
        // Has active check-in → Process CHECK-OUT
        return attendanceService.recordCheckOut(request);
    } else {
        // No active check-in → Process CHECK-IN
        return attendanceService.recordCheckIn(request);
    }
}
```

### Lateness Calculation

**Day Shift Example:**
```
Configuration: workStartTime=08:00, lateThreshold=15 minutes
Check-in: 08:20
Result: Late by 20 minutes
```

**Night Shift Example:**
```
Configuration: workStartTime=22:50, workEndTime=10:00
Check-in: 23:05 (same day)
Result: Late by 15 minutes

Check-in: 09:00 (next day)
Result: On time
```

**Implementation:**
```java
private void calculateLateness(AttendanceEntity entity, SystemConfigurationEntity config) {
    LocalDateTime checkInTime = entity.getCheckInTime();
    LocalTime workStartTime = config.getWorkStartTime();
    LocalTime workEndTime = config.getWorkEndTime();
    
    // Detect night shift
    boolean isNightShift = workStartTime.isAfter(workEndTime);
    
    LocalDateTime workStartDateTime;
    
    if (isNightShift) {
        LocalTime checkInOnlyTime = checkInTime.toLocalTime();
        
        if (checkInOnlyTime.isBefore(workEndTime)) {
            // Check-in after midnight - shift started previous day
            workStartDateTime = attendanceDate.minusDays(1).atTime(workStartTime);
        } else {
            // Check-in before midnight - shift starts today
            workStartDateTime = attendanceDate.atTime(workStartTime);
        }
    } else {
        // Normal day shift
        workStartDateTime = attendanceDate.atTime(workStartTime);
    }
    
    LocalDateTime lateThresholdTime = workStartDateTime.plusMinutes(toleranceMinutes);
    
    if (checkInTime.isAfter(lateThresholdTime)) {
        entity.setLate(true);
        entity.setLatenessDuration(Duration.between(workStartDateTime, checkInTime));
    }
}
```

### Duration Formatting

Adaptive format based on magnitude:
```java
private String formatDuration(Duration duration) {
    if (duration == null || duration.isZero()) return "0s";
    
    long hours = duration.toHours();
    long minutes = duration.toMinutes() % 60;
    long seconds = duration.getSeconds() % 60;
    
    if (hours == 0 && minutes == 0) {
        return String.format("%ds", seconds);           // "48s"
    } else if (hours == 0) {
        return String.format("%dm %ds", minutes, seconds); // "5m 30s"
    } else {
        return String.format("%dh %dm %ds", hours, minutes, seconds); // "8h 45m 30s"
    }
}
```

---

## Database Schema

### Workers Table
```sql
CREATE TABLE workers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_number VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(150),
    phone_number VARCHAR(20),
    fingerprint_id INTEGER UNIQUE,
    has_restricted_area_access BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);
```

### RFID Cards Table
```sql
CREATE TABLE rfid_cards (
    rfid_uid VARCHAR(20) PRIMARY KEY,
    worker_id BIGINT REFERENCES workers(id) ON DELETE SET NULL,
    last_seen TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Attendances Table
```sql
CREATE TABLE attendances (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT NOT NULL REFERENCES workers(id) ON DELETE CASCADE,
    rfid_tag VARCHAR(50) NOT NULL,
    attendance_date DATE NOT NULL,
    check_in_time TIMESTAMP NOT NULL,
    check_out_time TIMESTAMP,
    worked_duration_seconds BIGINT,
    is_late BOOLEAN NOT NULL DEFAULT false,
    lateness_duration_seconds BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);
```

### Access Logs Table
```sql
CREATE TABLE access_logs (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT REFERENCES workers(id),
    fingerprint_id INTEGER,
    access_granted BOOLEAN NOT NULL,
    location VARCHAR(100),
    access_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

---

## Firebase Integration

### Realtime Database Structure
```json
{
  "admin": {
    "comando": "NADA",
    "estado": "LISTO",
    "id_target": null,
    "ultimo_id_creado": 15
  },
  "logs": {
    "asistencia": {
      "-N8x9YzABC123": "Marcaje RFID: 35 13 B5 B1"
    },
    "accesos": {
      "-N8x9YzGHI789": "Puerta abierta ID: 15"
    },
    "seguridad": {
      "-N8x9YzMNO345": "Intento fallido huella: 99"
    }
  }
}
```

### Polling Configuration

Events are polled every 3 seconds:
```java
@Scheduled(fixedRate = 3000)
public void pollRecentAttendance() {
    String url = String.format("%s/logs/asistencia.json?orderBy=\"$key\"&limitToLast=5",
                                databaseUrl);
    // Process events
}
```

### Synchronous Command Flow

**Worker Creation:**
1. API sends `REGISTRAR` command to Firebase
2. ESP32 enters fingerprint registration mode
3. API waits for confirmation (40-second timeout)
4. Worker saved with fingerprint ID

**Worker Deletion:**
1. API sends `BORRAR` command with fingerprint ID
2. API waits for hardware confirmation (10-second timeout)
3. Cleans up `id_target` in Firebase
4. Unassigns RFID cards
5. Deletes worker from database

---

## Deployment

### Docker Deployment
```bash
docker build -t attendance-api:1.0.0 .
docker-compose up -d
```

### Environment Variables
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/attendance_db
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
  FIREBASE_DATABASE_URL: ${FIREBASE_URL}
  TZ: America/Lima
```

### Production Checklist

- Change `ddl-auto` to `validate`
- Configure proper connection pool size
- Set up database backups
- Configure Firebase security rules
- Implement authentication (JWT/OAuth2)
- Enable HTTPS/TLS
- Configure rate limiting
- Set up monitoring (Prometheus/Grafana)
- Configure centralized logging

---

## Security

### Current Security Model

- CORS enabled for development
- No authentication required
- All endpoints publicly accessible

### Recommended Production Security

**1. Implement JWT Authentication**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

**2. Secure Firebase Credentials**

Store credentials outside codebase:
```yaml
firebase:
  credentials-path: file:///etc/attendance-api/firebase-credentials.json
```

**3. Configure Firebase Security Rules**
```json
{
  "rules": {
    "admin": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "logs": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

---

## Performance Optimization

### Database Indexes

All foreign keys and frequently queried columns are indexed:
```sql
CREATE INDEX idx_worker_document ON workers(document_number);
CREATE INDEX idx_attendance_worker_date ON attendances(worker_id, attendance_date);
CREATE INDEX idx_access_time ON access_logs(access_time);
```

### Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
```

### Firebase Polling Optimization

- Deduplication with synchronized sets
- Fetch only last 5 events per poll
- Periodic cleanup to prevent memory growth

---

## Monitoring

### Spring Boot Actuator
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Endpoints:
- `/actuator/health` - Application health
- `/actuator/metrics` - JVM metrics
- `/actuator/prometheus` - Prometheus metrics

### Logging
```yaml
logging:
  level:
    com.iot.attendance: INFO
  file:
    name: /var/log/attendance-api/application.log
```

---

## License

MIT License - Copyright (c) 2025 IoT Attendance Team

---

## Support

- Email: nicolocisneros@gmail.com
- GitHub: https://github.com/Tunkifloo/attendance-access-control-api

---

## Acknowledgments

Built with Spring Boot, Firebase, PostgreSQL, Project Lombok, and MapStruct.

```text
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣀⡤⡦⣖⣖⢶⢴⣄⣄⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡠⣴⣺⢽⣺⢽⣝⣗⡯⡯⣗⣗⡷⡽⣝⣗⢦⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⢴⢽⢽⡳⣝⢷⣝⣗⣗⣗⢯⢯⣗⣗⡯⡯⣗⡯⡷⣽⣳⣆⠄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⢮⢯⢯⢯⢯⢯⣟⣞⣞⣞⡮⣯⣳⣳⡳⡽⣝⢷⢽⢽⣳⣳⢯⢷⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⢯⢯⢯⢯⢯⢯⡻⡺⡺⡪⡪⡫⡣⢣⠫⡪⡊⡎⡮⡻⡺⡼⡽⣝⣞⢮⢯⣳⠅⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣰⢯⢯⢯⢯⢯⢯⡻⡺⡺⡪⡪⡫⡣⢣⠫⡪⡊⡎⡮⡻⡺⡼⡽⣝⣞⢮⢯⣳⠅⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠠⡯⡯⡯⡯⡯⡯⡳⡹⡸⡘⢌⢂⠪⠨⡂⡑⠔⡡⢊⠜⠌⢎⢎⢞⢞⢮⢯⡳⡽⡅⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠨⡯⡯⡯⡯⣮⣺⣺⣺⡺⡮⣮⣢⡱⡁⢆⠢⡑⣬⡢⡧⡧⡵⡔⡅⡅⠝⡳⡽⣝⠆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡯⡯⣏⢯⡪⡪⣪⡲⣕⣝⢮⣺⡪⡪⠐⠠⢑⢜⢜⣜⢌⢌⢌⢊⠢⢁⠸⣝⣞⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⢯⣟⢎⢎⢮⢺⢮⢯⡺⡪⡳⡳⣝⢌⠂⢁⠂⢱⢱⢹⢹⢑⠇⠣⠨⠀⠌⣞⢎⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⢳⢽⡱⡱⡱⡑⠕⢅⠣⡣⡣⡣⡣⡣⠐⠀⠌⠠⢈⠂⠅⡐⢈⠐⡀⠅⠂⢧⠑⠅⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⣏⣗⢕⢕⢕⢜⢌⠢⡑⢌⢎⢎⢎⢆⠡⢀⠡⠈⢔⢈⠐⡀⠂⠠⢀⠐⠈⠌⠌⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢳⡳⡕⡕⡕⡕⡌⡪⡸⡸⡸⡪⡳⣕⢕⢐⠹⢑⠱⡑⠔⡀⡁⢂⠐⢈⠨⠨⠐⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⢪⡫⡪⡪⡪⡪⡪⡪⡪⡪⡪⡪⡂⡱⢐⠐⡀⠢⡑⢕⢐⢀⠂⡈⠄⢂⠑⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡯⡪⡪⡪⡪⡪⡪⡪⡺⡺⡪⣙⢘⢌⢑⢑⠱⣕⢕⢐⠐⡐⢀⠂⡂⠂⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠹⡪⡪⡪⡪⡪⡪⡪⡪⡪⡪⡪⡪⠪⡂⠢⠐⡈⠪⡢⠨⢐⢀⠂⡂⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢝⢜⢜⢜⢜⢜⢜⢜⢌⢆⢂⠢⠡⡀⠅⠂⠄⠡⢂⢑⢐⢐⠨⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡀⡢⢑⢕⢕⢕⢕⢕⢕⢕⢕⢕⠔⡅⡣⢂⠂⠅⠌⢌⢂⢂⠂⡂⠀⠀⠀⠠⠀⠀⠄⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⡀⠠⢀⠂⠅⢂⢂⠡⠐⡑⢕⢕⢕⢕⢕⢕⢕⢕⢕⢕⢅⢇⠅⢕⢐⠐⠄⠁⠀⠀⠀⠀⠠⠐⠀⠀⡀⠈⠀⠠⠀⠀⠀⠀
⠀⠄⡀⡂⠌⡐⠠⢁⠂⠌⠨⠐⡀⢂⠁⠄⢂⠈⡊⠎⡎⣎⢎⢎⢎⢎⢎⢆⣇⢕⠆⠁⠈⠀⠀⠀⠀⠀⠀⠀⠀⠂⠀⡀⠈⠀⢀⠈⠀⠠
⠌⡐⡀⡂⠂⡂⠡⠐⡈⠨⠠⠡⠐⠠⢈⠐⡀⡢⠲⢵⣰⣨⣩⣉⡫⡯⣏⣃⢈⠀⠀⡔⡄⠀⠀⠀⠀⠀⠀⠈⠀⠄⠂⠀⡀⠄⠀⠀⠐⠀
⠂⢀⠂⠄⠡⠐⠈⠄⢂⠡⠁⠅⠌⢐⢀⠢⠑⠨⠨⢐⢀⠓⣗⢷⢽⣝⢮⢪⠲⢘⠨⠐⠡⠑⢅⠢⢀⠀⠠⠐⠀⠀⡀⠄⠀⠄⠀⠂⠀⠄
⠄⠂⠀⠄⠁⢀⠡⠈⠄⢂⠡⠁⠌⢐⠐⡀⠁⢁⠈⢀⠐⡐⠨⣫⣳⡳⡳⣕⠁⠀⠀⡈⠀⠁⠂⠨⠐⢈⠀⠀⠀⠀⡀⠀⡀⠀⠁⢐⠀⠀
⢀⠐⠀⠠⠐⠀⢀⠈⠀⠂⢀⠁⠈⠀⠠⠀⡈⠀⢀⠠⠐⠠⠁⢮⣺⣺⡪⡪⡊⠀⠀⠀⠈⠀⠄⠀⢀⠐⠈⡀⠂⡀⠀⠀⠀⠀⠂⠀⢀⢁
⠀⢀⠠⠀⠠⠀⠄⠀⠁⠐⠀⢀⠈⠀⠄⠁⠀⠠⠀⢀⠠⠈⠌⢮⣺⡺⡪⡪⡪⡀⠀⠁⠀⠁⠀⠐⠀⠀⠀⠀⠁⠄⠂⡀⠂⠀⢀⠀⠀⠀
⠈⠀⠀⠀⠄⠐⠀⠈⠀⠂⠈⠀⠀⠂⠀⠂⠁⢀⠐⠀⠀⠄⢅⣗⣗⢝⢮⢪⢪⡂⠀⠈⠀⠈⠀⠐⠀⠐⠀⠐⠀⠀⠁⠠⠐⠀⡀⠀⠀⠂
⢂⠈⠄⠁⠄⠂⠈⡀⠁⡀⠂⠁⠠⠈⠀⠐⠀⠠⠀⡀⠁⡐⢰⣳⡳⡕⡕⡕⡕⣕⠀⠁⠀⠁⠀⠁⠀⠂⠀⠄⠠⠀⠂⠀⠀⠁⠀⠄⢀⠀
⠠⠀⠐⠀⠐⠀⠁⠀⡀⠠⠀⠂⠠⠀⢁⠀⢁⠠⠀⠠⠐⠠⢱⡳⣝⢎⢎⢎⢎⢎⠎⠀⠈⠀⠈⠀⠈⠀⠐⠀⠠⠀⠀⠐⢀⠐⢀⠀⠠⠀

   PROJECT BUILD POWERED BY: RUM-AI
```

Developed by Tunkifloo and RUM-AI Team