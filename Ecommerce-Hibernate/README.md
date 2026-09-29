# Ecommerce Hibernate

A small Java + Hibernate + H2 demo project that models an e-commerce domain using JPA annotations, Hibernate sessions, and repository-style CRUD examples.

## Features

- Hibernate ORM configuration with H2 in-memory database
- Entity mapping for categories, products, users, orders, and order details
- One-to-many and many-to-one relationships
- Named query and Criteria API examples
- Pagination example
- Soft-delete behavior using Hibernate SQL annotations
- BCrypt password hashing for users
- JUnit 5 tests covering main persistence behaviors

## Tech Stack

- Java 17
- Maven 3.9+
- Hibernate ORM 6.4.4.Final
- H2 Database 2.2.224
- JUnit 5.10.2
- jBCrypt 0.4

## Project Structure

```text
Ecommerce-Hibernate/
├── pom.xml
├── schema.sql
├── README.md
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── Main.java
│   │   │       ├── entity/
│   │   │       └── util/
│   │   └── resources/
│   │       └── hibernate.cfg.xml
│   └── test/
│       └── java/
│           └── com/example/CrudTest.java
└── target/
```

## Prerequisites

- Java 17 or newer
- Maven 3.9 or newer

## Run the tests

```bash
mvn test
```

## Run the demo

The demo application is driven from `Main` and can be executed from your IDE or by using a Maven execution plugin.

```bash
mvn test
```

The `Main` class demonstrates:

- inserting category and product data
- creating orders and order details
- fetching joins with `JOIN FETCH`
- named queries
- criteria queries
- pagination
- updating products
- soft-deleting products

## Notes

- The app uses an in-memory H2 database and is intended for development/demo usage.
- `hibernate.hbm2ddl.auto` is set to `create-drop`, so schema tables are recreated when the session factory starts.
- Passwords are stored as BCrypt hashes, not plain text.
