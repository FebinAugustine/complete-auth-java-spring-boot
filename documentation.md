# Technical Documentation: Auth Service

**Version:** 1.0  
**Last Updated:** September 7, 2025

---

## 1. Introduction

### 1.1. Purpose

This document provides a comprehensive technical overview of the Auth Service, a standalone microservice for user authentication and authorization. It is intended for developers, architects, and system administrators responsible for maintaining, extending, or integrating with this service.

The purpose of this documentation is to detail the internal architecture, design decisions, API specifications, and core functionalities to ensure the project is easy to understand, maintain, and scale.

### 1.2. Project Goals

The primary goal of the Auth Service is to centralize and secure all aspects of user identity management. It is designed to be a single source of truth for user data, providing a robust and secure foundation for a larger ecosystem of applications. Key goals include:

*   **Security:** To implement and enforce industry-standard security practices for authentication, session management, and data protection.
*   **Decoupling:** To abstract the complexities of user authentication away from other services, allowing them to focus on their core business logic.
*   **Scalability:** To be built on a reliable and scalable technology stack capable of handling high user loads.
*   **Maintainability:** To have a clean, well-organized, and thoroughly documented codebase that is easy for developers to work with.

---

## 2. System Architecture & Design

### 2.1. Architectural Style

The Auth Service is built as a **stateless RESTful API**. This architectural choice is crucial for scalability and decoupling. The server does not store any session state for authenticated users. Instead, the client is responsible for maintaining the session by sending a JSON Web Token (JWT) with each request.

### 2.2. Core Technologies

*   **Java 21 & Spring Boot 3.5.5:** The foundation of the application, providing a robust, modern, and highly configurable framework for building enterprise-grade applications.
*   **Spring Security 6:** The cornerstone of the security implementation. It is used to handle all aspects of authentication and authorization.
*   **Spring Data JPA & Hibernate:** Used for all database interactions, providing a powerful Object-Relational Mapping (ORM) layer that simplifies data access.
*   **MySQL 8.0:** The chosen relational database for storing all user and application data.
*   **Flyway:** The database migration tool. It ensures that the database schema evolves in a controlled, versioned, and repeatable manner.
*   **JWT (JSON Web Tokens):** Used for creating secure, self-contained session tokens.
*   **Thymeleaf:** A modern templating engine used to create rich, styled HTML emails for user communication.
*   **Maven:** The build automation and dependency management tool for the project.

### 2.3. Security Design

The security of the Auth Service is built on several key pillars:

*   **Dual Security Filter Chains:** The application uses two separate, ordered `SecurityFilterChain` beans to handle different types of traffic. This is the standard approach for applications that serve both a stateless REST API and stateful, browser-based web flows.
    *   **API Chain (`@Order(1)`):** This chain is configured to be completely stateless. It uses the `JwtAuthenticationFilter` to validate JWTs sent in cookies and is configured to return `401 Unauthorized` JSON errors instead of redirecting to a login page.
    *   **Web Chain (`@Order(2)`):** This chain handles all non-API traffic, including the browser-based OAuth2 login flow. It is stateful and uses traditional session management and CSRF protection.

*   **JWT-Based Authentication:** For the API, authentication is managed via JWTs. Upon successful login, two tokens are issued and stored in secure, `HttpOnly` cookies:
    *   **Access Token (ATK):** A short-lived token that grants access to protected resources.
    *   **Refresh Token (RTK):** A long-lived token that can be used to obtain a new access token without requiring the user to re-enter their credentials.

*   **Password Hashing:** User passwords are never stored in plaintext. They are securely hashed using the industry-standard **BCrypt** algorithm, which is integrated into Spring Security's `PasswordEncoder`.

*   **CSRF Protection:** Cross-Site Request Forgery protection is **enabled** for the web filter chain to protect against malicious cross-site attacks during browser-based flows like OAuth2. It is **disabled** for the stateless API endpoints, which are protected by the requirement of sending a JWT.

---

## 3. Project Structure & File Breakdown

The project follows a standard Maven directory layout, with a logical package structure designed to separate concerns.

```
.
├── .mvn/                           // Maven Wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/febin/auth/
│   │   │   │   ├── AuthApplication.java      // Main Spring Boot application class. Also contains the PasswordEncoder bean.
│   │   │   │   ├── config/                   // Contains the core SecurityConfig class.
│   │   │   │   ├── controller/               // REST API controllers (Auth, User, Admin, Password).
│   │   │   │   ├── dto/                      // Data Transfer Objects (DTOs) for API requests and responses.
│   │   │   │   ├── entity/                   // JPA entity classes that model the database tables.
│   │   │   │   ├── exception/                // Custom exception classes and the GlobalExceptionHandler.
│   │   │   │   ├── oauth/                    // Handlers for OAuth2 login success and failure.
│   │   │   │   ├── ratelimit/                // The IP-based rate limiting filter.
│   │   │   │   ├── repository/               // Spring Data JPA repository interfaces.
│   │   │   │   ├── security/                 // The custom JwtAuthenticationFilter.
│   │   │   │   ├── service/                  // The core business logic for the application.
│   │   │   │   └── util/                     // Utility classes for handling JWTs and cookies.
│   │   │   └── db/migration/               // Flyway database migration scripts (Java-based).
│   │   └── resources/
│   │       ├── application.properties      // Main application configuration file.
│   │       └── templates/                  // HTML email templates processed by Thymeleaf.
│   └── test/
│       └── java/                         // Test source files
├── .gitignore                        // Specifies files and directories to be ignored by Git.
├── Dockerfile                        // Instructions for building a Docker image of the application.
├── mvnw & mvnw.cmd                   // Maven Wrapper scripts for consistent builds.
└── pom.xml                           // The Project Object Model, defining all dependencies and build config.
```

---

## 4. Core Functionality Deep Dive

This section details the implementation of the service's key features.

### 4.1. User Registration & Verification

1.  **Request:** A `POST` request is made to `/api/auth/signup` with a username, email, and password.
2.  **Controller:** The `AuthController` receives the request and calls the `UserService.registerUser()` method.
3.  **Service Logic (`UserService`):
    *   It first checks if the username or email already exists in the database to prevent duplicates.
    *   It creates a new `User` entity.
    *   It securely hashes the provided password using the `PasswordEncoder`.
    *   It generates a unique, non-guessable `verificationCode` (a UUID).
    *   It sets the user's `accountStatus` to `UNVERIFIED`.
    *   It assigns the default `ROLE_USER`.
    *   It saves the new `User` to the database.
    *   It calls the `EmailService` to send the verification email.
4.  **Email Service (`EmailService`):
    *   It uses the Thymeleaf `TemplateEngine` to process the `account-verification-email.html` template.
    *   It injects the user's name and the full verification URL (e.g., `http://localhost:8080/api/auth/verify?code=...`) into the template.
    *   It sends the generated HTML email using `JavaMailSender`.
5.  **Verification:**
    *   The user clicks the link in their email, which makes a `GET` request to `/api/auth/verify`.
    *   The `AuthController` calls `UserService.verifyUser()`.
    *   The service finds the user by the verification code, sets their `accountStatus` to `ACTIVE`, clears the code, and saves the user.
    *   The user is now able to log in.

### 4.2. Authentication & Session Management

1.  **Login Request:** A user sends a `POST` request to `/api/auth/login` with their credentials.
2.  **Controller & Service:** The `AuthController` calls `AuthService.login()`. The `AuthService` uses Spring Security's `AuthenticationManager` to validate the credentials. If the user is not `ACTIVE`, a `DisabledException` is thrown and handled by the controller to provide a specific error message.
3.  **Token Generation:** Upon successful authentication, the `AuthService` calls `JwtUtil` to generate a short-lived Access Token and a long-lived Refresh Token.
4.  **Cookie Creation:** The `AuthService` uses `CookieUtil` to create two secure, `HttpOnly` cookies (`ATK` and `RTK`) to store the tokens.
5.  **API Communication:** For subsequent requests to protected endpoints (e.g., `/api/users/me`), the browser automatically sends the cookies.
6.  **JWT Filter:** The `JwtAuthenticationFilter` intercepts the request, extracts the Access Token from the `ATK` cookie, validates it using `JwtUtil`, and then uses the `UserDetailsService` (our `UserService`) to load the full `User` object and set it as the principal in the `SecurityContext`. This makes the user available to the controllers.

### 4.3. Password Management

*   **Forgot Password:**
    1.  A user submits their email to `POST /api/auth/password/forgot`.
    2.  The `UserService` generates a random 6-digit code, sets a 10-minute expiry on the user's record, and calls the `EmailService`.
    3.  The `EmailService` sends the `password-reset-email.html` template with the code.
    4.  The user submits the code and a new password to `POST /api/auth/password/reset`.
    5.  The `UserService` validates the code and its expiry time, then updates the password.
*   **Logged-In Reset:** A logged-in user sends a `POST` to `/api/users/me/password` with their current and new password. The `UserService` verifies the current password before setting the new one.

### 4.4. Administrative Functions

*   **Security:** All endpoints under `/api/admin/**` are protected by a rule in the `SecurityConfig` that requires the `ROLE_ADMIN` authority.
*   **Logic:** The `AdminController` receives requests and calls the appropriate methods in the `UserService`.
*   **Safety Checks:** The `UserService` includes critical safety checks to prevent an administrator from deleting or changing the roles of their own account, which prevents accidental self-lockout.

---

## 5. Database Schema

The database schema is managed by Flyway. The migration scripts in `src/main/java/db/migration/` define the following core tables:

*   **`users`**: Stores the core user information, including username, email, hashed password, and account status.
*   **`roles`**: Stores the available roles in the system (e.g., `ROLE_USER`, `ROLE_ADMIN`).
*   **`user_roles`**: A join table that links users to their roles, creating a many-to-many relationship.
*   **`refresh_tokens`**: Stores the refresh tokens issued to users, allowing for persistent sessions.
*   **`user_providers`**: A table that links a user to their social media accounts, enabling them to log in with multiple providers.

---

## 6. Setup and Configuration

### 6.1. Prerequisites
*   Java 21 SDK
*   Apache Maven 3.8+
*   A running MySQL 8.0 instance

### 6.2. Configuration
The primary configuration is managed in `src/main/resources/application.properties`. Key values to configure include:
*   `spring.datasource.*`: Database connection details.
*   `spring.mail.*`: SMTP server settings for sending emails.
*   `jwt.secret`: A long, random string for signing JWTs.
*   `spring.security.oauth2.client.registration.*`: Client ID and Client Secret for each OAuth2 provider.

### 6.3. Running the Application
1.  Create the MySQL database: `CREATE DATABASE authdb;`
2.  Update the credentials in `application.properties` and any `.env` files.
3.  Run the application using the main class `com.febin.auth.AuthApplication` or via the Maven command: `mvn spring-boot:run`.

