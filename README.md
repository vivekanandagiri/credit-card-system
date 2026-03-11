# Credit Card Issuing and Management System

## Overview

The **Credit Card Issuing and Management System** is a backend application built using **Spring Boot** that simulates the core functionality of a credit card issuing platform.
It allows customers to apply for credit cards, manage their profiles, submit KYC details, and enables administrators to manage credit card products and underwriting decisions.

---

## Tech Stack

* **Java 17**
* **Spring Boot**
* **Spring Security + JWT Authentication**
* **Spring Data JPA**
* **PostgreSQL**
* **Flyway (Database Migration)**
* **Swagger / OpenAPI**
* **Maven**
* **JUnit & Mockito for Testing**

---

## Key Features

### Authentication & Security

* User registration and login
* JWT-based authentication
* Role-based access control

### Customer Management

* Create and update customer profiles
* Manage customer addresses
* KYC submission and verification

### Credit Card Management

* Credit card product creation
* Credit card application processing
* Underwriting decision engine
* Risk scoring and rule evaluation

### API Documentation

* Integrated **Swagger UI** for exploring APIs

---

## Project Structure

```
src/main/java/com/example
│
├── controller        # REST Controllers
├── service           # Business logic
├── repository        # Database access layer
├── entity            # JPA entities
├── dto               # Request & Response DTOs
├── mapper            # Entity-DTO mapping
├── security          # JWT & Spring Security config
├── underwriting      # Decision engine & rule evaluation
└── config            # Application configuration
```

---

## Database Migrations

Database schema is managed using **Flyway**.

Migration scripts are located in:

```
src/main/resources/db/migration
```

Example migrations include:

* Customer and authentication tables
* Address and residency tables
* KYC records
* Credit product definitions
* Credit card applications
* Underwriting rules

---

## Running the Project

### 1. Clone the Repository

```
git clone https://github.com/your-username/credit-card-system.git
```

### 2. Configure the Database

Create a PostgreSQL database:

```
credit_card_system
```

Update configuration in `application.properties`.

---

### 3. Run the Application

```
mvn spring-boot:run
```

Application runs on:

```
http://localhost:8082
```

---

## API Documentation

Swagger UI:

```
http://localhost:8082/swagger-ui/index.html
```

---

## Testing

Run tests using:

```
mvn test
```

---

## API Documentation

Swagger UI is available at:

http://localhost:8082/swagger-ui/index.html

Example API endpoints:

* POST /api/auth/register
* POST /api/auth/login
* GET /api/credit-products
* POST /api/credit-card-applications

Swagger Screenshot:

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/9388ea22-9e30-4f62-b283-87b21faae3e3" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/4c7476a6-d0be-4bb9-a7ce-69fe3e1be3fa" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/6f944ca2-be2f-42b2-9634-b7db3e2232d9" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/413b42ed-52a8-4822-8a37-125c031fa685" />
## Architecture

The application follows a layered architecture:

Controller Layer
Handles HTTP requests and responses.

Service Layer
Contains business logic.

Repository Layer
Handles database interaction using Spring Data JPA.

Security Layer
Manages authentication and authorization using JWT.

Underwriting Engine
Evaluates credit card applications using configurable rules and risk scoring.
## Features Implemented

* JWT Authentication
* Role-Based Access Control
* Credit Card Application Workflow
* KYC Verification
* Customer Profile Management
* Flyway Database Migration
* Risk Scoring Engine
* Underwriting Decision System
* RESTful API Design
* Unit Testing with JUnit



**Vivekananda Giri**

Software Engineer | Backend Developer | Java & Spring Boot
