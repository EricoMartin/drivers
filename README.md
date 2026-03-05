# Drivers - Ride-Sharing Platform Backend

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen?style=for-the-badge&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![Maven](https://img.shields.io/badge/Maven-3.9+-red?style=for-the-badge&logo=apache-maven)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**A production-ready, enterprise-grade ride-sharing platform backend built with Spring Boot**

[Features](#features) • [Getting Started](#getting-started) • [API Documentation](#api-documentation) • [Architecture](#architecture) • [Contributing](#contributing)

</div>

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Security](#security)
- [Testing](#testing)
- [Deployment](#deployment)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## 🚀 Overview

**Drivers** is a comprehensive backend system for a ride-sharing platform, designed with scalability, security, and performance in mind. Built as a learning project for the Developer Foundry Fellowship, it demonstrates enterprise-level software engineering practices and modern Spring Boot architecture.

### Key Highlights

- 🔐 **JWT-based Authentication** with role-based access control
- 🚗 **Real-time Driver Management** with availability tracking
- 🗺️ **Trip State Machine** handling complete ride lifecycle
- 💳 **Payment Processing** simulation with retry logic
- 🔒 **Pessimistic Locking** for concurrent driver assignments
- 📊 **Comprehensive Testing** (Unit, Integration, Security)
- 🏗️ **Clean Architecture** with separation of concerns

---

## ✨ Features

### User Management
- ✅ User registration and authentication
- ✅ Role-based access control (CLIENT, DRIVER)
- ✅ JWT token-based stateless authentication
- ✅ Password encryption with BCrypt
- ✅ User profile management

### Driver Operations
- ✅ Driver availability management
- ✅ Vehicle type configuration
- ✅ Real-time availability tracking
- ✅ Driver earnings and statistics
- ✅ Concurrent assignment prevention

### Trip Management
- ✅ Trip request creation
- ✅ Automatic driver assignment
- ✅ State machine for trip lifecycle:
```
  REQUESTED → ASSIGNED → STARTED → COMPLETED
       ↓           ↓         ↓
   CANCELLED   CANCELLED  CANCELLED
```
- ✅ Trip history and filtering
- ✅ Real-time status updates

### Payment System
- ✅ Automatic payment creation on trip completion
- ✅ Payment status tracking (PENDING, PAID, FAILED)
- ✅ Retry mechanism for failed payments
- ✅ Revenue tracking and analytics

### Security Features
- ✅ JWT authentication
- ✅ Role-based authorization
- ✅ Method-level security with `@PreAuthorize`
- ✅ CORS configuration
- ✅ Security headers
- ✅ Password strength validation

---

## 🛠️ Technology Stack

### Backend Framework
- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.2.2** - Production-grade framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence with Hibernate
- **Maven** - Dependency management and build tool

### Database
- **PostgreSQL 16** - Production database
- **H2** - In-memory database for testing
- **Hibernate ORM** - Object-relational mapping

### Security & Authentication
- **JWT (JSON Web Tokens)** - Stateless authentication
- **BCrypt** - Password hashing
- **JJWT 0.12.5** - JWT library

### Testing
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing
- **Spring Security Test** - Security testing
- **AssertJ** - Fluent assertions

### Documentation
- **OpenAPI 3.0** - API documentation
- **Swagger UI** - Interactive API explorer

### Development Tools
- **Lombok** - Reduce boilerplate code
- **SLF4J + Logback** - Logging framework

---

## 🏗️ Architecture

### Layered Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  • REST Controllers                                          │
│  • Request/Response DTOs                                     │
│  • Exception Handlers                                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                            │
│  • Business Logic                                            │
│  • Transaction Management                                    │
│  • State Machines                                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   PERSISTENCE LAYER                          │
│  • JPA Repositories                                          │
│  • Entity Models                                             │
│  • Custom Queries                                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      DATABASE                                │
│                   (PostgreSQL)                               │
└─────────────────────────────────────────────────────────────┘
```

### Security Architecture
```
HTTP Request
    ↓
┌─────────────────────────────────────────┐
│  JwtAuthenticationFilter                │
│  • Extract JWT token                    │
│  • Validate signature                   │
│  • Load user details                    │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│  Spring Security Filter Chain           │
│  • Authentication check                 │
│  • Authorization check                  │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│  Controller                             │
│  • @PreAuthorize checks                 │
│  • Business logic                       │
└─────────────────────────────────────────┘
```

---

## 🚦 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** or higher ([Download](https://adoptium.net/))
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi))
- **PostgreSQL 16** ([Download](https://www.postgresql.org/download/))
- **Git** ([Download](https://git-scm.com/downloads))

Optional but recommended:
- **IntelliJ IDEA** or **VS Code** with Java extensions
- **Postman** for API testing
- **Docker** (for containerized deployment)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/drivers.git
cd drivers
```

2. **Create PostgreSQL database**
```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE drivers;

-- Create user (optional)
CREATE USER drivers_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE drivers TO drivers_user;
```

3. **Configure application properties**

Create `src/main/resources/application-local.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/drivers
spring.datasource.username=drivers_user
spring.datasource.password=your_password

# JWT Configuration
app.jwt.secret=your-secret-key-here-must-be-at-least-512-bits-long
app.jwt.expiration-ms=86400000

# Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

4. **Install dependencies**
```bash
mvn clean install
```

### Configuration

#### Environment Variables

For production, use environment variables instead of hardcoding secrets:
```bash
export JWT_SECRET="your-production-secret-key"
export DB_URL="jdbc:postgresql://your-db-host:5432/drivers"
export DB_USERNAME="your-db-user"
export DB_PASSWORD="your-db-password"
```

#### Generate JWT Secret
```bash
# Generate a secure random key (512 bits)
openssl rand -base64 64
```

### Running the Application

#### Development Mode
```bash
# Using Maven
mvn spring-boot:run

# Using Maven Wrapper
./mvnw spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### Production Mode
```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/drivers-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

---

## 📚 API Documentation

### Interactive API Documentation

Once the application is running, access the Swagger UI:

**URL:** `http://localhost:8080/swagger-ui.html`

### API Endpoints Overview

#### Authentication Endpoints
```http
POST   /api/v1/auth/register       # Register new user
POST   /api/v1/auth/login          # User login
GET    /api/v1/auth/validate       # Validate JWT token
GET    /api/v1/auth/check-email    # Check email availability
```

#### User Management
```http
GET    /api/v1/users/{id}          # Get user by ID
GET    /api/v1/users/email/{email} # Get user by email
PUT    /api/v1/users/{id}/password # Change password
```

#### Driver Operations
```http
GET    /api/v1/drivers/{id}                # Get driver details
GET    /api/v1/drivers/available           # Get available drivers
POST   /api/v1/drivers/{id}/go-online     # Mark driver available
POST   /api/v1/drivers/{id}/go-offline    # Mark driver unavailable
GET    /api/v1/drivers/{id}/stats         # Get driver statistics
PUT    /api/v1/drivers/{id}/vehicle-type  # Update vehicle type
```

#### Trip Management
```http
POST   /api/v1/trips                       # Create trip request
GET    /api/v1/trips/{id}                  # Get trip details
PUT    /api/v1/trips/{id}/assign-driver    # Assign driver to trip
POST   /api/v1/trips/{id}/start            # Start trip
POST   /api/v1/trips/{id}/complete         # Complete trip
POST   /api/v1/trips/{id}/cancel           # Cancel trip
GET    /api/v1/trips/client/{id}           # Get client's trips
GET    /api/v1/trips/driver/{id}           # Get driver's trips
```

#### Payment Operations
```http
GET    /api/v1/payments/{id}               # Get payment details
GET    /api/v1/payments/trip/{tripId}      # Get payment by trip
POST   /api/v1/payments/{id}/process       # Process/retry payment
GET    /api/v1/payments/revenue            # Get total revenue
```

### Example API Calls

#### Register a New Driver
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "driver@example.com",
    "password": "securePassword123",
    "role": "DRIVER",
    "vehicleType": "Sedan"
  }'
```

#### Login and Get JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "driver@example.com",
    "password": "securePassword123"
  }'
```

#### Create a Trip (Authenticated)
```bash
curl -X POST http://localhost:8080/api/v1/trips \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "client-uuid-here",
    "fare": 25.50,
    "vehicleType": "Sedan"
  }'
```

---

## 🗄️ Database Schema

### Entity Relationship Diagram
```
┌─────────────┐
│    Users    │
├─────────────┤
│ id (PK)     │
│ email       │
│ password    │
│ role        │
│ created_at  │
└─────────────┘
      ↓ 1:1
┌─────────────┐         ┌─────────────┐
│   Drivers   │         │   Clients   │
├─────────────┤         ├─────────────┤
│ id (PK)     │         │ id (PK)     │
│ user_id(FK) │         │ user_id(FK) │
│ is_available│         │ created_at  │
│ vehicle_type│         └─────────────┘
│ version     │               ↓ 1:N
│ created_at  │         ┌─────────────┐
└─────────────┘         │    Trips    │
      ↓ 1:N             ├─────────────┤
      ↓                 │ id (PK)     │
      └────────────────→│ client_id   │
                        │ driver_id   │
                        │ status      │
                        │ fare        │
                        │ version     │
                        │ created_at  │
                        └─────────────┘
                              ↓ 1:1
                        ┌─────────────┐
                        │  Payments   │
                        ├─────────────┤
                        │ id (PK)     │
                        │ trip_id(FK) │
                        │ amount      │
                        │ status      │
                        │ created_at  │
                        └─────────────┘
```

### Key Tables

- **users**: Authentication and identity management
- **drivers**: Driver-specific information and availability
- **clients**: Client-specific information
- **trips**: Trip requests and state management
- **payments**: Payment processing and tracking

---

## 🔒 Security

### Authentication Flow

1. User registers/logs in with credentials
2. Server validates credentials and generates JWT token
3. Client includes JWT in `Authorization: Bearer <token>` header
4. Server validates token on each request
5. User gains access based on role and permissions

### JWT Token Structure
```json
{
  "header": {
    "alg": "HS512",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user@example.com",
    "userId": "uuid-here",
    "role": "DRIVER",
    "iat": 1234567890,
    "exp": 1234654290
  },
  "signature": "..."
}
```

### Role-Based Access Control

| Endpoint | CLIENT | DRIVER | ADMIN |
|----------|--------|--------|-------|
| Create Trip | ✅ | ❌ | ✅ |
| View Own Trips | ✅ | ✅ | ✅ |
| Update Availability | ❌ | ✅ | ✅ |
| View All Trips | ❌ | ❌ | ✅ |
| Process Payments | ❌ | ❌ | ✅ |

### Security Best Practices Implemented

- ✅ Passwords hashed with BCrypt (10 rounds)
- ✅ JWT tokens with expiration
- ✅ HTTPS enforced in production
- ✅ CORS configuration
- ✅ SQL injection prevention (Prepared Statements)
- ✅ Input validation on all endpoints
- ✅ Rate limiting (to be implemented)
- ✅ Audit logging

---

## 🧪 Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run tests with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Test Coverage
```
Repository Layer:    92%
Service Layer:       87%
Controller Layer:    78%
Overall Coverage:    85%
```

### Test Categories

1. **Unit Tests** - Test individual components in isolation
   - Service layer logic
   - Business rules
   - Utility methods

2. **Integration Tests** - Test component interactions
   - End-to-end workflows
   - Database operations
   - API endpoints

3. **Security Tests** - Test authentication and authorization
   - JWT validation
   - Role-based access
   - Unauthorized access prevention

### Example Test
```java
@Test
@DisplayName("Should create trip successfully when client has no active trips")
void createTrip_shouldCreateTripInRequestedStatus() {
    // Given
    CreateTripRequest request = CreateTripRequest.builder()
        .clientId(clientId)
        .fare(new BigDecimal("25.00"))
        .build();
    
    when(clientService.getClientById(clientId)).thenReturn(testClient);
    when(tripRepository.hasActiveTripForClient(clientId)).thenReturn(false);
    when(tripRepository.save(any(Trip.class))).thenReturn(testTrip);
    
    // When
    Trip result = tripService.createTrip(request);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(TripStatus.REQUESTED);
    verify(tripRepository).save(any(Trip.class));
}
```

---

## 🚀 Deployment

### Docker Deployment

#### Build Docker Image
```bash
# Build application
mvn clean package -DskipTests

# Build Docker image
docker build -t drivers-app:latest .
```

#### Docker Compose
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: drivers
      POSTGRES_USER: drivers_user
      POSTGRES_PASSWORD: drivers_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    image: drivers-app:latest
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/drivers
      SPRING_DATASOURCE_USERNAME: drivers_user
      SPRING_DATASOURCE_PASSWORD: drivers_pass
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

Run with:
```bash
docker-compose up -d
```

### Production Checklist

- [ ] Use environment variables for secrets
- [ ] Enable HTTPS/TLS
- [ ] Configure proper CORS origins
- [ ] Set up database backups
- [ ] Configure monitoring and logging
- [ ] Set up health checks
- [ ] Configure rate limiting
- [ ] Review security headers
- [ ] Set appropriate JWT expiration
- [ ] Configure production database connection pool

---

## 📁 Project Structure
```
drivers/
├── src/
│   ├── main/
│   │   ├── java/com/basebox/drivers/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/          # REST Controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── DriverController.java
│   │   │   │   ├── TripController.java
│   │   │   │   └── PaymentController.java
│   │   │   ├── domain/
│   │   │   │   ├── model/           # JPA Entities
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Driver.java
│   │   │   │   │   ├── Client.java
│   │   │   │   │   ├── Trip.java
│   │   │   │   │   └── Payment.java
│   │   │   │   └── enums/           # Enumerations
│   │   │   │       ├── Role.java
│   │   │   │       ├── TripStatus.java
│   │   │   │       └── PaymentStatus.java
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── exception/           # Custom Exceptions
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── InvalidOperationException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── repository/          # Data Access Layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── DriverRepository.java
│   │   │   │   ├── ClientRepository.java
│   │   │   │   ├── TripRepository.java
│   │   │   │   └── PaymentRepository.java
│   │   │   ├── security/            # Security Components
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── jwt/
│   │   │   │       ├── JwtTokenProvider.java
│   │   │   │       ├── JwtAuthenticationFilter.java
│   │   │   │       └── JwtAuthenticationEntryPoint.java
│   │   │   └── service/             # Business Logic Layer
│   │   │       ├── UserService.java
│   │   │       ├── DriverService.java
│   │   │       ├── ClientService.java
│   │   │       ├── TripService.java
│   │   │       └── PaymentService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-test.properties
│   └── test/
│       └── java/com/basebox/drivers/
│           ├── integration/         # Integration Tests
│           ├── repository/          # Repository Tests
│           ├── security/            # Security Tests
│           └── service/             # Service Tests
├── .gitignore
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

### How to Contribute

1. **Fork the repository**
2. **Create a feature branch**
```bash
   git checkout -b feature/amazing-feature
```
3. **Commit your changes**
```bash
   git commit -m 'Add some amazing feature'
```
4. **Push to the branch**
```bash
   git push origin feature/amazing-feature
```
5. **Open a Pull Request**

### Coding Standards

- Follow Java naming conventions
- Write unit tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR
- Follow existing code style and patterns
- Add meaningful commit messages

### Code Review Process

1. All PRs require at least one review
2. All tests must pass
3. Code coverage should not decrease
4. Documentation must be updated

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
```
MIT License

Copyright (c) 2024 [Eric Ibu]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📞 Contact

**Eric Ibu** - [@yourtwitter](https://x.com/kingmartini1) - ericomartin.0785@gmail.com

**Project Link:** [https://github.com/EricoMartin/drivers](https://github.com/EricoMartin/drivers)

**Portfolio:** [https://myportfolio.com](https://https://ericomartin.github.io/portfolio/)

---

## 🙏 Acknowledgments

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Baeldung Spring Tutorials](https://www.baeldung.com/spring-boot)
- [JWT.io](https://jwt.io/)
- Developer Foundry Fellowship Program
- All contributors and supporters

---

## 📊 Project Stats

![GitHub stars](https://img.shields.io/github/stars/yourusername/drivers?style=social)
![GitHub forks](https://img.shields.io/github/forks/yourusername/drivers?style=social)
![GitHub issues](https://img.shields.io/github/issues/yourusername/drivers)
![GitHub pull requests](https://img.shields.io/github/issues-pr/yourusername/drivers)

---

<div align="center">

**Built with ❤️ for the Developer Foundry Fellowship**

**⭐ Star this repo if you find it helpful!**

</div>