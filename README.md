# 💳 Credit Card Issuing and Management System

## 📌 Overview

The **Credit Card Issuing and Management System** is a backend application built using **Spring Boot** that simulates a **real-world credit card issuing and processing platform**.

The system models core financial workflows including:

- Credit card application & underwriting  
- Card issuance & lifecycle management  
- Transaction processing with authorization lifecycle  
- Ledger-based accounting (source of truth)  
- Billing cycle & statement generation  
- Interest calculation on revolving balances  
- Payment processing with allocation logic  
- Scheduled jobs for automated billing & due evaluation  

---

## 🛠 Tech Stack

- Java 17  
- Spring Boot  
- Spring Security + JWT Authentication  
- Spring Data JPA  
- PostgreSQL  
- Flyway (Database Migration)  
- Swagger / OpenAPI  
- Maven  
- JUnit & Mockito  

---

## 🚀 Key Features

### 🔐 Authentication & Security

- User registration and login  
- JWT-based authentication  
- Role-based access control (Customer / Admin)  

---

### 👤 Customer & KYC Management

- Create and update customer profiles  
- Manage customer addresses  
- KYC submission and verification  
- Enforced KYC validation before credit application  

---

### 💳 Credit Card Application & Underwriting

- Credit card application submission  
- Validation rules:
  - KYC must be approved  
  - Credit score validation (300–900)  
  - Duplicate application prevention  
  - Active application limit  
  - Rejection cooldown enforcement  
- Integration with underwriting engine  
- Auto account creation on approval  

---

### 🏦 Credit Account Management

- Credit account creation from approved applications  
- Account lifecycle:
  - ACTIVE → SUSPENDED → BLOCKED → CLOSED  
- Ownership validation for customers  
- Admin-level account management  
- Credit limit enforcement  

---

### 💳 Card Issuance & Lifecycle

- Issue credit cards (customer & admin flows)  
- Supports:
  - Virtual cards  
  - Physical cards (address required)  
- Constraints:
  - Max cards per account  
  - Virtual card uniqueness  
- Card lifecycle:
  - PENDING → ACTIVE → BLOCKED / EXPIRED / CANCELLED  
- Secure ownership validation  

---

### 💸 Transaction Processing Engine

- Supports:
  - Purchases  
  - Refunds  
- Validation pipeline:
  - Card status & expiry  
  - Channel validation (ATM / POS / ONLINE)  
  - Daily spend limits  
  - Credit limit validation using ledger  
- Authorization-based processing (HOLD → CAPTURE)  
- Declined transactions are persisted (audit compliance)  
- Pagination & filtering support  

---

### 🏦 Authorization Lifecycle

- AUTHORIZE → Reserve credit (hold)  
- CAPTURE → Finalize transaction  
- EXPIRE / REVERSE → Release unused holds  

Key behaviors:

- Holds reduce available credit  
- Ledger updated only after capture  
- Idempotency via network reference  

---

### 📒 Ledger System (Financial Core)

- Append-only ledger (immutable entries)  
- Entry types:
  - DEBIT → increases outstanding balance  
  - CREDIT → reduces outstanding balance  

**Balance Formula:**

Balance = Credits - Debits


- Acts as the **single source of truth**  

---

### 📊 Billing & Statement Engine

- Automatic & manual statement generation  
- Timezone-aware billing cycles  
- Prevents duplicate statements  
- Calculates:
  - Opening balance  
  - Transactions  
  - Interest  
  - Minimum due  
- Statement statuses:
  - PAID  
  - REVOLVING  
  - OVERDUE  
- Late fee handling  

---

### 📈 Interest Calculation Engine

- Daily interest calculation (APR → Daily Rate)  
- Time-segmented based on ledger entries  
- Grace period support  
- Accurate rounding using BigDecimal  

---

### 💰 Payment Processing System

- Idempotent payment handling  
- FIFO allocation across statements  
- Supports:
  - Partial payments  
  - Multi-statement payments  
  - Overpayments  
- Updates:
  - Ledger  
  - Statements  
  - Account balance  

---

### ⏰ Scheduled Billing & Automation

#### Daily Jobs (UTC)

**1. Statement Generation (00:00)**  
- Generates statements based on billing cycle day  
- Handles duplicate prevention  

**2. Due Date Evaluation (00:15)**  
- Evaluates:
  - PAID  
  - REVOLVING  
  - OVERDUE  
- Applies late fees  

---

## 🧱 Project Structure


src/main/java/com/example
│
├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── security
├── underwriting
├── scheduler
└── config


---

## 🗄 Database Migrations

Managed using Flyway


src/main/resources/db/migration


Includes:

- Customer & authentication  
- KYC  
- Credit products  
- Accounts  
- Cards  
- Transactions  
- Ledger  
- Billing  
- Payments  

---

## ⚙️ Running the Project

### 1. Clone


git clone https://github.com/your-username/credit-card-system.git

cd credit-card-system


---

### 2. Configure Database

Create database:


credit_card_system


Update:


application.properties


---

### 3. Run


mvn spring-boot:run


App runs at:


http://localhost:8082


---

## 📘 API Documentation

Swagger UI:


http://localhost:8082/swagger-ui/index.html


---

## 🧪 Testing


mvn test


---

## 📸 Screenshots

(Add your Swagger screenshots here)

---

## 🧱 Architecture

Layered architecture:

- Controller Layer  
- Service Layer  
- Repository Layer  
- Security Layer  

### Core Modules

- Authorization  
- Transaction  
- Ledger  
- Billing  
- Payment  
- Underwriting  

---

## 🔄 Core Financial Flows

### Transaction Flow

Request → Authorization (HOLD)
→ Transaction Creation
→ Ledger Update
→ Capture


### Billing Flow

Ledger → Statement → Interest → Due Calculation


### Payment Flow

Payment → Ledger → Allocation → Account Update


---

## ⚠️ Important Notes

- Ledger is the **source of truth**  
- All operations are **transactional & consistent**  
- Idempotency is enforced for critical flows  
- Designed for scalability and extensibility  

---

## ✅ Features Implemented

- JWT Authentication  
- Role-Based Access Control  
- Credit Card Application Workflow  
- Underwriting System  
- Credit Account Management  
- Card Issuance & Lifecycle  
- Transaction Processing Engine  
- Authorization (Hold/Capture)  
- Ledger-Based Accounting  
- Billing & Statement Engine  
- Interest Calculation  
- Payment Processing & Allocation  
- Scheduled Jobs  
- RESTful APIs  
- Unit Testing  

---

## 👨‍💻 Author

**Vivekananda Giri**  
Software Engineer | Backend Developer | Java & Spring Boot  

---

## ⭐ Future Improvements

- Multi-currency support  
- Event-driven architecture (Kafka)  
- Fraud detection  
- Notification system  
- Microservices architecture  

If you want, I can also:

Add badges (GitHub, build, Java version)
Create a diagram (very strong for interviews)
Or shorten this into a perfect resume project description
give the full readme.md so that i can copy and paste

Here’s your clean, copy-paste ready README.md (no extra formatting issues, no IDs, just pure Markdown):

# 💳 Credit Card Issuing and Management System

## 📌 Overview

The **Credit Card Issuing and Management System** is a backend application built using **Spring Boot** that simulates a **real-world credit card issuing and processing platform**.

The system models core financial workflows including:

- Credit card application & underwriting  
- Card issuance & lifecycle management  
- Transaction processing with authorization lifecycle  
- Ledger-based accounting (source of truth)  
- Billing cycle & statement generation  
- Interest calculation on revolving balances  
- Payment processing with allocation logic  
- Scheduled jobs for automated billing & due evaluation  

---

## 🛠 Tech Stack

- Java 17  
- Spring Boot  
- Spring Security + JWT Authentication  
- Spring Data JPA  
- PostgreSQL  
- Flyway (Database Migration)  
- Swagger / OpenAPI  
- Maven  
- JUnit & Mockito  

---

## 🚀 Key Features

### 🔐 Authentication & Security

- User registration and login  
- JWT-based authentication  
- Role-based access control (Customer / Admin)  

---

### 👤 Customer & KYC Management

- Create and update customer profiles  
- Manage customer addresses  
- KYC submission and verification  
- Enforced KYC validation before credit application  

---

### 💳 Credit Card Application & Underwriting

- Credit card application submission  
- Validation rules:
  - KYC must be approved  
  - Credit score validation (300–900)  
  - Duplicate application prevention  
  - Active application limit  
  - Rejection cooldown enforcement  
- Integration with underwriting engine  
- Auto account creation on approval  

---

### 🏦 Credit Account Management

- Credit account creation from approved applications  
- Account lifecycle:
  - ACTIVE → SUSPENDED → BLOCKED → CLOSED  
- Ownership validation for customers  
- Admin-level account management  
- Credit limit enforcement  

---

### 💳 Card Issuance & Lifecycle

- Issue credit cards (customer & admin flows)  
- Supports:
  - Virtual cards  
  - Physical cards (address required)  
- Constraints:
  - Max cards per account  
  - Virtual card uniqueness  
- Card lifecycle:
  - PENDING → ACTIVE → BLOCKED / EXPIRED / CANCELLED  
- Secure ownership validation  

---

### 💸 Transaction Processing Engine

- Supports:
  - Purchases  
  - Refunds  
- Validation pipeline:
  - Card status & expiry  
  - Channel validation (ATM / POS / ONLINE)  
  - Daily spend limits  
  - Credit limit validation using ledger  
- Authorization-based processing (HOLD → CAPTURE)  
- Declined transactions are persisted (audit compliance)  
- Pagination & filtering support  

---

### 🏦 Authorization Lifecycle

- AUTHORIZE → Reserve credit (hold)  
- CAPTURE → Finalize transaction  
- EXPIRE / REVERSE → Release unused holds  

Key behaviors:

- Holds reduce available credit  
- Ledger updated only after capture  
- Idempotency via network reference  

---

### 📒 Ledger System (Financial Core)

- Append-only ledger (immutable entries)  
- Entry types:
  - DEBIT → increases outstanding balance  
  - CREDIT → reduces outstanding balance  

**Balance Formula:**


Balance = Credits - Debits


- Acts as the **single source of truth**  

---

### 📊 Billing & Statement Engine

- Automatic & manual statement generation  
- Timezone-aware billing cycles  
- Prevents duplicate statements  
- Calculates:
  - Opening balance  
  - Transactions  
  - Interest  
  - Minimum due  
- Statement statuses:
  - PAID  
  - REVOLVING  
  - OVERDUE  
- Late fee handling  

---

### 📈 Interest Calculation Engine

- Daily interest calculation (APR → Daily Rate)  
- Time-segmented based on ledger entries  
- Grace period support  
- Accurate rounding using BigDecimal  

---

### 💰 Payment Processing System

- Idempotent payment handling  
- FIFO allocation across statements  
- Supports:
  - Partial payments  
  - Multi-statement payments  
  - Overpayments  
- Updates:
  - Ledger  
  - Statements  
  - Account balance  

---

### ⏰ Scheduled Billing & Automation

#### Daily Jobs (UTC)

**1. Statement Generation (00:00)**  
- Generates statements based on billing cycle day  
- Handles duplicate prevention  

**2. Due Date Evaluation (00:15)**  
- Evaluates:
  - PAID  
  - REVOLVING  
  - OVERDUE  
- Applies late fees  

---

## 🧱 Project Structure


src/main/java/com/example
│
├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── security
├── underwriting
├── scheduler
└── config


---

## 🗄 Database Migrations

Managed using Flyway


src/main/resources/db/migration


Includes:

- Customer & authentication  
- KYC  
- Credit products  
- Accounts  
- Cards  
- Transactions  
- Ledger  
- Billing  
- Payments  

---

## ⚙️ Running the Project

### 1. Clone


git clone https://github.com/your-username/credit-card-system.git

cd credit-card-system


---

### 2. Configure Database

Create database:


credit_card_system


Update:


application.properties


---

### 3. Run


mvn spring-boot:run


App runs at:


http://localhost:8082


---

## 📘 API Documentation

Swagger UI:


http://localhost:8082/swagger-ui/index.html


---

## 🧪 Testing


mvn test


---

## 📸 Screenshots

(Add your Swagger screenshots here)

---

## 🧱 Architecture

Layered architecture:

- Controller Layer  
- Service Layer  
- Repository Layer  
- Security Layer  

### Core Modules

- Authorization  
- Transaction  
- Ledger  
- Billing  
- Payment  
- Underwriting  

---

## 🔄 Core Financial Flows

### Transaction Flow

Request → Authorization (HOLD)
→ Transaction Creation
→ Ledger Update
→ Capture


### Billing Flow

Ledger → Statement → Interest → Due Calculation


### Payment Flow

Payment → Ledger → Allocation → Account Update


---

## ⚠️ Important Notes

- Ledger is the **source of truth**  
- All operations are **transactional & consistent**  
- Idempotency is enforced for critical flows  
- Designed for scalability and extensibility  

---

## ✅ Features Implemented

- JWT Authentication  
- Role-Based Access Control  
- Credit Card Application Workflow  
- Underwriting System  
- Credit Account Management  
- Card Issuance & Lifecycle  
- Transaction Processing Engine  
- Authorization (Hold/Capture)  
- Ledger-Based Accounting  
- Billing & Statement Engine  
- Interest Calculation  
- Payment Processing & Allocation  
- Scheduled Jobs  
- RESTful APIs  
- Unit Testing  

---

## 👨‍💻 Author

**Vivekananda Giri**  
Software Engineer | Backend Developer | Java & Spring Boot  

---