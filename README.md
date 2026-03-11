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

## 

**Vivekananda Giri**

Software Engineer | Backend Developer | Java & Spring Boot
