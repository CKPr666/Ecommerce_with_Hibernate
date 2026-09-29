# Spring Boot E-Commerce Hibernate ORM Implementation

A comprehensive, enterprise-grade Java backend application managing an e-commerce ecosystem using **Spring Boot**, **Hibernate 6 ORM**, and the **JPA Specification**. This project demonstrates advanced database persistence, complex bidirectional entity relationships, transactional integrity, and data security.

## 📌 Project Overview
This system implements complete relational database persistence with an in-memory **H2 Database**. It showcases modern data patterns including custom soft deletion mechanics, type-safe dynamic queries using JPA CriteriaBuilder, declarative Named Queries, efficient result pagination, and cryptographically secure user authentication data.

---

## 🏗️ Entities & Relational Schema

The core domain architecture manages five highly decoupled entities linked via optimized database constraints and clean mapping cascades.

| Entity | Fields | Key Constraints & Implementation Features | Relationships |
| :--- | :--- | :--- | :--- |
| **Category** | `id`, `name`, `description` | `name` is UNIQUE & NOT NULL | One-to-Many with `Product` |
| **Product** | `id`, `name`, `price`, `stockQuantity`, `deleted` | `name` & `price` NOT NULL. Implements custom Soft Delete. | Many-to-One with `Category` |
| **Users** | `id`, `username`, `password`, `email`, `role` | `username` & `email` UNIQUE & NOT NULL. Role Enum (`ADMIN`, `CUSTOMER`). | One-to-Many with `Orders` |
| **Orders** | `id`, `orderDate`, `totalAmount` | `orderDate` & `totalAmount` NOT NULL | Many-to-One with `Users`, One-to-Many with `OrderDetails` |
| **OrderDetails** | `id`, `quantity`, `unitPrice` | `quantity` & `unitPrice` NOT NULL | Many-to-One with `Orders`, Many-to-One with `Product` |

### 🛠️ Entity Relationship Diagram
```text
[Category] 1 ────< * [Product] <──── * [OrderDetails] * >──── 1 [Orders] * >──── 1 [Users]
```

---

## 🚀 Key Features & Architectural Enhancements

*   **Advanced JPA & Hibernate Mapping:** Fully annotated domain models using `@Entity`, `@Table`, `@Id`, `@GeneratedValue(IDENTITY)`, and collection mapping types ensuring precise DDL generation.
*   **Bidirectional Synchronization:** Engineered helper methods (`addProduct`/`removeProduct`, `addOrder`/`removeOrder`, `addDetail`/`removeDetail`) to manage memory-state parity on both sides of relationships automatically.
*   **Native Soft Deletion:** Implemented seamless logical deletes using `@SQLDelete(sql = "UPDATE product SET deleted = true WHERE id = ?")` paired with `@SQLRestriction("deleted = false")` to filter out inactive records from query results transparently.
*   **Type-Safe Dynamic Queries:** Constructed clean database access APIs using the JPA `CriteriaBuilder` API to enable modular runtime query generation without risking SQL injection vectors.
*   **Declarative Named Queries:** Optimized read frequency on heavy relations via predefined, structured static queries such as `Product.findByCategory`.
*   **Result Pagination:** Built high-performance collection processing utilizing stateful chunking with `.setFirstResult()` and `.setMaxResults()`.
*   **Password Cryptography:** Security layer leveraging **BCrypt** hashing (`jbcrypt`) for salt-generation and verification of raw credentials.

---

## 🛠️ Tech Stack & Dependencies

*   **Language & Runtime:** Java JDK 17+
*   **Framework Layer:** Spring Boot 3.x
*   **Data Persistence Layer:** Hibernate Core 6.4.4.Final / Jakarta Persistence API (JPA)
*   **Database Engine:** H2 Database (In-Memory Engine: `jdbc:h2:mem:ecommerce;DB_CLOSE_DELAY=-1`)
*   **Build Pipeline:** Apache Maven 3.8+
*   **Testing Harness:** JUnit Jupiter 5.10.2

---

## ⚙️ Configuration & Execution Guide

### 1. Build and Compile Architecture
Download project dependencies and execute code compilation blocks via Maven:
```bash
mvn clean compile
```

### 2. Launch the Application Runner
Executes the main runtime bootstrap routine covering end-to-end CRUD operations, fetch joins, criteria evaluations, and soft-delete states:
```bash
mvn compile exec:java "-Dexec.mainClass=com.example.Main"
```

### 3. Run the Automated Test Suite
Run full target regression validation pipelines wrapped inside integration lifecycle setups:
```bash
mvn test
```

---

## 🧪 Automated Integration Tests Matrix

The test framework situated in `com.example.CrudTest` provides exhaustive functional coverage for runtime data operations:

*   `insertAndFindCategoryWithProduct`: Asserts multi-tier cascading persistence hooks down to deep graph nodes.
*   `createOrderWithMultipleDetails`: Validates multi-item transaction graphs mapping across accounts and lines.
*   `fetchOrderAlongWithAssociatedUsersAndProducts`: Confirms optimized single-query execution patterns using Fetch Joins to completely eliminate the N+1 select problem.
*   `updateOperationsForCategoryAndProduct`: Verifies update cascades when mutating item properties.
*   `softDeleteHidesProduct`: Confirms soft-deletion flags prevent read exposure via entity filters while persistent records maintain internal flag values.
*   `namedQueryFindByCategory`: Tests indexed static execution targets.
*   `criteriaQueryByPrice`: Tests programmatic context filtering on dynamically adjusted variables.
*   `paginationForProductListings`: Asserts offset navigation limits data bounds correctly.
*   `userPasswordHashingAndVerification`: Asserts hashing integrity and secure validation controls.
