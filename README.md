# Product Requirements Document: Auth Service

**Author:** Gemini AI  
**Version:** 1.7  
**Status:** In Development

---

## 1. Overview

The Auth Service is a standalone, production-ready microservice responsible for handling all user authentication and authorization. It is designed to act as a centralized identity provider for a larger ecosystem of applications. The service provides a secure, robust, and scalable solution for managing user accounts, including local registration with email verification, social logins (OAuth2), comprehensive password management, social account linking, account deletion, and a full suite of administrative functions.

The primary goal of this service is to abstract away the complexities of authentication, providing a simple and secure API for frontend applications and other backend services.

---

## 2. Goals and Objectives

*   **Provide Secure Authentication:** Implement industry-standard security practices for user login and session management.
*   **Support Multiple Authentication Methods:** Offer both traditional email/password registration and modern social logins.
*   **Flexible Account Management:** Allow users to link, unlink, and delete their accounts.
*   **Ensure User Validity:** Enforce email verification for new accounts to ensure users are reachable and legitimate.
*   **Full Password Lifecycle Management:** Provide secure options for users to reset their current password or recover a forgotten password.
*   **Administrative Oversight:** Grant administrators the ability to view and manage user accounts and system roles.
*   **Developer-Friendly API:** Expose a clear, well-documented set of RESTful endpoints for easy integration.

---

## 3. Core Features

### 3.1. User Account Management

*   **Local User Registration:** Users can sign up with a username, email, and password. The account remains in an `UNVERIFIED` state until the user verifies their email address.
*   **Account Verification:** Upon registration, a unique verification link is sent to the user's email. Clicking this link activates the account, changing its status to `ACTIVE`. Users cannot log in until their account is verified.
*   **Get Current User:** An authenticated user can retrieve their own profile information.
*   **Account Deletion:** An authenticated user can permanently delete their own account.

### 3.2. Social Account Management

*   **Link Social Account:** A logged-in user can link their local account to an OAuth2 provider (Google, GitHub).
*   **View Linked Accounts:** A logged-in user can view a list of their currently linked social accounts.
*   **Unlink Social Account:** A logged-in user can remove a link to a social account from their profile.

### 3.3. Password Management

*   **Logged-In Password Reset:** An authenticated user can change their password by providing their current password and a new password.
*   **Forgot Password Flow:** An unauthenticated user can request a password reset by providing their email address.

### 3.4. Session Management & Social Login

*   **JWT-Based Sessions:** The service uses JSON Web Tokens (JWT) for session management.
*   **Secure Cookie Storage:** Tokens are stored in `HttpOnly`, `Secure` cookies.
*   **Provider Integration:** Supports direct user authentication via Google and GitHub.

### 3.5. Administrative Functions

*   **User Listing:** Users with `ROLE_ADMIN` can retrieve a complete list of all users in the system.
*   **Get User by ID:** An administrator can retrieve the full details of a single user by their ID.
*   **Role Listing:** Users with `ROLE_ADMIN` can retrieve a list of all available roles in the system.
*   **Disable User Account:** An administrator can disable any user's account (except their own).
*   **Enable User Account:** An administrator can re-enable a disabled user's account.
*   **Delete User Account:** An administrator can permanently delete any user's account (except their own).
*   **Update User Roles:** An administrator can update the roles assigned to any user (except their own).

### 3.6. Security Features

*   **Password Encryption:** Passwords are securely hashed using the **BCrypt** algorithm.
*   **CSRF Protection:** Implemented for browser-based flows, while being disabled for the stateless API endpoints.
*   **Rate Limiting:** Protects sensitive endpoints against brute-force attacks.

---

## 4. API Endpoint Definitions

The base URL for the service is `http://localhost:8080`.

### 4.1. Authentication Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/auth/signup` | `POST` | Registers a new user. The account will be `UNVERIFIED`. |
| `/api/auth/verify` | `GET` | Verifies a user's account using a code sent via email. |
| `/api/auth/login` | `POST` | Authenticates a user and returns secure session cookies. |
| `/api/auth/refresh` | `POST` | Uses the refresh token cookie to issue a new access token. |
| `/api/auth/logout` | `POST` | Invalidates the user's session and clears cookies. |

### 4.2. Password Management Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/auth/password/forgot` | `POST` | Triggers a 6-digit reset code to be sent to the user's email. |
| `/api/auth/password/reset` | `POST` | Resets the password using the 6-digit code and a new password. |

### 4.3. User Account Management Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/users/me` | `GET` | **(Authenticated)** Retrieves the profile of the currently logged-in user. |
| `/api/users/me` | `DELETE` | **(Authenticated)** Permanently deletes the currently logged-in user's account. |
| `/api/users/me/password` | `POST` | **(Authenticated)** Allows a logged-in user to change their password. |
| `/api/users/me/linked-accounts` | `GET` | **(Authenticated)** Retrieves a list of linked social accounts. |
| `/api/users/me/link-oauth` | `POST` | **(Authenticated)** Links a new social account (Google/GitHub). |
| `/api/users/me/unlink-oauth` | `POST` | **(Authenticated)** Unlinks a previously linked social account. |

### 4.4. Admin Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/admin/users` | `GET` | **(Admin Only)** Retrieves a list of all users. |
| `/api/admin/users/{id}` | `GET` | **(Admin Only)** Retrieves the details of a specific user by their ID. |
| `/api/admin/roles` | `GET` | **(Admin Only)** Retrieves a list of all available roles. |
| `/api/admin/users/{id}/roles` | `PUT` | **(Admin Only)** Updates the roles for a specific user. |
| `/api/admin/users/{id}/disable` | `POST` | **(Admin Only)** Disables a user's account by their ID. |
| `/api/admin/users/{id}/enable` | `POST` | **(Admin Only)** Re-enables a disabled user's account by their ID. |
| `/api/admin/users/{id}` | `DELETE` | **(Admin Only)** Permanently deletes a user's account by their ID. |

### 4.5. Browser-Only Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/` | `GET` | Public health-check endpoint. |
| `/oauth2/authorization/{provider}` | `GET` | Initiates the OAuth2 login flow for `google` or `github`. |

---

## 5. User Flows

### 5.1. Account Verification Flow
1.  A user signs up via `POST /api/auth/signup`.
2.  The service creates the user with `accountStatus = UNVERIFIED` and sends an email containing a verification link.
3.  The user clicks the link, which directs them to `GET /api/auth/verify?code=...`.
4.  The service validates the code, sets the user's `accountStatus` to `ACTIVE`, and the user can now log in.

### 5.2. Forgot Password Flow
1.  A user submits their email to `POST /api/auth/password/forgot`.
2.  The service sends an email containing a 6-digit reset code.
3.  The user submits the code and their new password to `POST /api/auth/password/reset`.
4.  The service validates the code and updates the user's password.

### 5.3. Social Account Linking Flow
1.  The user logs into the application with their local username and password.
2.  The frontend application initiates an OAuth2 flow with the desired provider (e.g., Google) to obtain a one-time `authorization_code`.
3.  The frontend makes a `POST` request to `/api/users/me/link-oauth`, sending the `provider` name and the `authorization_code`.
4.  The backend service securely exchanges the code for an access token, fetches the user's profile from the provider, performs security checks, and creates the link in the database.

---

## 6. Technical Stack & Project Structure

*   **Framework:** Spring Boot 3.5.5
*   **Language:** Java 21
*   **Security:** Spring Security 6
*   **Database:** MySQL 8.0
*   **Data Access:** Spring Data JPA / Hibernate
*   **Database Migrations:** Flyway
*   **Build Tool:** Apache Maven
*   **Email Templating:** Thymeleaf
*   **JWT Library:** `jjwt`

```
.
├── .mvn/                           // Maven Wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/febin/auth/
│   │   │   │   ├── AuthApplication.java      // Main Spring Boot application class
│   │   │   │   ├── config/                   // Spring Security configuration
│   │   │   │   ├── controller/               // REST API controllers (Auth, User, Admin, Password)
│   │   │   │   ├── dto/                      // Data Transfer Objects for API requests/responses
│   │   │   │   ├── entity/                   // JPA entity classes (database models)
│   │   │   │   ├── exception/                // Custom exception classes and global handler
│   │   │   │   ├── oauth/                    // OAuth2 login success/failure handlers
│   │   │   │   ├── ratelimit/                // Rate limiting filter implementation
│   │   │   │   ├── repository/               // Spring Data JPA repositories
│   │   │   │   ├── security/                 // Custom security components (e.g., JWT filter)
│   │   │   │   ├── service/                  // Business logic and services
│   │   │   │   └── util/                     // Utility classes (e.g., JWT, Cookies)
│   │   │   └── db/migration/               // Flyway database migration scripts (Java-based)
│   │   └── resources/
│   │       ├── application.properties      // Main application configuration
│   │       ├── static/
│   │       └── templates/                  // HTML email templates (Thymeleaf)
│   └── test/
│       └── java/                         // Test source files
├── .gitignore                        // Specifies intentionally untracked files to ignore
├── Dockerfile                        // Instructions for building a Docker image
├── mvnw & mvnw.cmd                   // Maven Wrapper scripts
└── pom.xml                           // Project Object Model: defines dependencies and build config
```

---

## 7. Setup and Configuration

### Prerequisites
*   Java 21 SDK
*   Apache Maven 3.8+
*   A running MySQL 8.0 instance

### Configuration
The primary configuration is managed in `src/main/resources/application.properties`. Key values to configure include:
*   `spring.datasource.*`: Database connection details.
*   `jwt.secret`: A long, random string for signing JWTs.
*   `spring.security.oauth2.client.registration.*`: Client ID and Client Secret for each OAuth2 provider.

### Running the Application
1.  Create the MySQL database: `CREATE DATABASE authdb;`
2.  Update the credentials in `application.properties`.
3.  Run the application using the main class `com.febin.auth.AuthApplication` or via the Maven command: `mvn spring-boot:run`.

