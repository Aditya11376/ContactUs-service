# ContactUs Microservice 📩🛡️

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Port](https://img.shields.io/badge/Port-8083-blue.svg)](#)
[![Database](https://img.shields.io/badge/PostgreSQL-15-316192.svg)](https://www.postgresql.org/)

---

## 📌 Overview

The **ContactUs Service** is a decoupled Spring Boot microservice responsible for receiving, validating, moderating, and persisting user inquiries, bug reports, and feedback across client applications (`appName`).

Before storing any submission in PostgreSQL, the service performs **AI-powered Spam Detection and Automatic Categorization** via the internal **Gemini AI Service**, ensuring clean and categorized support tickets.

### Key Capabilities
- **Internal Gateway Security:** Protected by constant-time cryptographic comparison (`MessageDigest.isEqual`) against the `X-Internal-Secret` header so only authorized gateways can invoke submission endpoints.
- **AI Spam Filtering & Classification:** Automatically checks incoming messages via the Gemini Gateway and categorizes valid requests into `SUPPORT`, `FEEDBACK`, `GENERAL_INQUIRY`, or `BUG_REPORT`.
- **Resilient Fallback & Warm-up:** Configured with strict `RestClient` timeouts (5s connect / 8s read), automatic local rule-based classification fallback if the LLM gateway is unreachable, and a non-blocking startup warm-up ping.
- **Neon / PostgreSQL Persistence:** Archives legitimate, categorized messages using Spring Data JPA (`contact_us_messages`) with HikariCP connection tuning and SSL support (`?sslmode=require`).
- **Cloud-Optimized Fast Boot:** Tuned for constrained 512MB containers (`-XX:+UseSerialGC`, `-XX:TieredStopAtLevel=1`, lazy initialization, JMX disabled) to minimize cold-start times.
- **Centralized Exception Handling:** Uses `@RestControllerAdvice` (`ContactUsExceptionHandler`) for standardized validation errors, spam rejection (`400 Bad Request`), and runtime exceptions.
- **Container & Cloud Ready:** Ships with a multi-stage `Dockerfile` and local `compose.yaml` for seamless local and cloud deployment.

---

## 🏗️ Architecture & Request Flow

```text
+------------------+     POST /api/contact-us     +--------------------+     POST /gemini/generate     +--------------------+
|   API Gateway    |  (Header: X-Internal-Secret) |   ContactUs Svc    |  (Spam Check & Categorize)  |  Gemini AI Gateway |
|   (Port 8080)    | ---------------------------->|    (Port 8083)     | ----------------------------> |    (Port 8092)     |
+------------------+                              +---------+----------+                               +--------------------+
                                                            |
                                              If NOT Spam   |   If Spam -> 400 Bad Request
                                                            v
                                                  +---------+----------+
                                                  |     PostgreSQL     |
                                                  | (contact_us_msgs)  |
                                                  +--------------------+
```

---

## 🚀 API Specification

### 1. Health Check
Verifies service availability and port status.

- **Endpoint:** `GET /health` (Aliases: `/`, `/api/health`, `/api/contactus/health`)
- **Response (`200 OK`):**
```json
{
  "status": "UP",
  "service": "contact-us-service",
  "port": 8083,
  "timestamp": "2026-11-10T20:00:00.123456"
}
```

---

### 2. Submit Contact Message
Accepts user messages, verifies internal authorization, runs AI spam filtering, categorizes the request, and saves it to the database.

- **Endpoint:** `POST /api/contact-us`
- **Required Headers:**
  - `Content-Type: application/json`
  - `X-Internal-Secret: <YOUR_INTERNAL_SECRET>`
- **Request Body (`ContactUsDto`):**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "message": "I need assistance accessing the clinical dashboard reports.",
  "appName": "COSMOS-Web"
}
```

- **Field Validations:**
  - `name`: Required, non-blank string.
  - `email`: Required, valid email format.
  - `message`: Required, length strictly between **10 and 1000 characters**.
  - `appName`: Required, non-blank identifier of the client application.

- **Responses:**
  - **`201 Created`:**
    ```json
    {
      "message": "Message submitted successfully!"
    }
    ```
  - **`400 Bad Request` (Validation or Spam Detected):**
    ```json
    {
      "apiPath": "uri=/api/contact-us",
      "errorCode": "BAD_REQUEST",
      "errorMessage": "Message flagged as spam.",
      "errorTime": "2026-11-10T20:05:00"
    }
    ```
  - **`401 Unauthorized`:** Missing or invalid `X-Internal-Secret` header.

---

## ⚙️ Configuration & Environment Variables

Create a `.env` file in the root directory for local development (never commit `.env` to version control):

```env
SERVER_PORT=8083
GEMINI_SERVICE_URL=http://localhost:8092
APP_INTERNAL_SECRET=<YOUR_INTERNAL_SECRET>

DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=contact_us
DATABASE_USERNAME=<YOUR_DB_USERNAME>
DATABASE_PASSWORD=<YOUR_DB_PASSWORD>
DATABASE_PARAMS=
```

| Variable | Description | Default / Example |
|---|---|---|
| `PORT` / `SERVER_PORT` | HTTP server port | `8083` |
| `SPRING_DOCKER_COMPOSE_ENABLED` | Auto-start local `compose.yaml` DB | `true` (set `false` on Cloud) |
| `GEMINI_SERVICE_URL` | Base URL of the Gemini AI Gateway (no trailing slash) | — |
| `APP_INTERNAL_SECRET` | Shared secret key for gateway auth | — |
| `DATABASE_HOST` | PostgreSQL / Neon hostname | `localhost` |
| `DATABASE_PORT` | PostgreSQL port | `5432` |
| `DATABASE_NAME` | Target database name | `contact_us` |
| `DATABASE_USERNAME` | Database username | — |
| `DATABASE_PASSWORD` | Database password | — |
| `DATABASE_PARAMS` | Optional JDBC query parameters (e.g., `?sslmode=require` for Neon) | *(empty)* |
| `HIBERNATE_DDL_AUTO` | JPA schema mode (`update` for dev, `none` for fast prod boot) | `update` |

---

## 💻 Local Setup & Execution

### Prerequisites
- Java 21 (JDK)
- Docker Desktop (for local PostgreSQL container via `compose.yaml`)
- Running instance of the Gemini AI Gateway

### Run Locally
1. Configure your local `.env` file using the template above.
2. Start the application via Maven Wrapper (Spring Boot Docker Compose will automatically boot the Postgres container):
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 🐳 Docker & Cloud Deployment (Render + Neon)

The repository includes a multi-stage `Dockerfile` tuned for 512MB cloud containers (using SerialGC, Tiered Compilation Level 1, and lazy bean initialization for fast cold starts):

1. **Provision a Serverless PostgreSQL Database** (e.g., Neon or Render PostgreSQL).
2. **Create a Web Service** connected to your repository using the `Docker` runtime.
3. **Configure Health Check Path:** `/health`
4. **Set Environment Variables** in your cloud dashboard:
   - `SPRING_DOCKER_COMPOSE_ENABLED=false`
   - `GEMINI_SERVICE_URL=<DEPLOYED_GEMINI_URL>`
   - `APP_INTERNAL_SECRET=<YOUR_INTERNAL_SECRET>`
   - `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
   - `DATABASE_PARAMS=?sslmode=require` *(required for Neon SSL connections)*
   - `HIBERNATE_DDL_AUTO=none` *(optional, once tables are initialized for faster startup)*
