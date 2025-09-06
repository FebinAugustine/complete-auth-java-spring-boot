# Product Requirements Document: Auth Service

**Author:** Gemini AI  
**Version:** 1.0  
**Status:** In Development

---

## 1. Overview

The Auth Service is a standalone, production-ready microservice responsible for handling all user authentication and authorization. It is designed to act as a centralized identity provider for a larger ecosystem of applications. The service provides a secure, robust, and scalable solution for managing user accounts, including local (email/password) registration and social logins via OAuth2 providers (Google and GitHub).

The primary goal of this service is to abstract away the complexities of authentication, providing a simple and secure API for frontend applications and other backend services.

---

## 2. Goals and Objectives

*   **Provide Secure Authentication:** Implement industry-standard security practices for user login and session management.
*   **Support Multiple Authentication Methods:** Offer both traditional email/password registration and modern social logins.
*   **Centralized User Identity:** Act as a single source of truth for user identity across multiple client applications.
*   **Developer-Friendly API:** Expose a clear, well-documented set of RESTful endpoints for easy integration.
*   **Scalability and Reliability:** Built on a robust tech stack to handle high loads and ensure uptime.

---

## 3. Core Features

### 3.1. User Account Management

*   **Local User Registration:** Users can create a new account using a unique username, a valid email address, and a password. The service ensures that usernames and emails are unique.
*   **Local User Authentication:** Registered users can log in using their username/email and password. Upon successful authentication, the service issues secure session tokens.

### 3.2. Session Management

*   **JWT-Based Sessions:** The service uses JSON Web Tokens (JWT) for session management. Two tokens are issued upon login:
    *   **Access Token (ATK):** A short-lived token used to authenticate API requests.
    *   **Refresh Token (RTK):** A long-lived token used to obtain a new access token without requiring the user to log in again.
*   **Secure Cookie Storage:** Both tokens are stored in `HttpOnly`, `Secure` cookies, preventing access from client-side JavaScript and mitigating XSS attacks.
*   **Token Refresh:** A dedicated endpoint allows a client to refresh an expired access token using a valid refresh token.
*   **Logout:** A logout endpoint invalidates the user's session by revoking the refresh token and clearing the authentication cookies.

### 3.3. Social Login (OAuth2)

*   **Provider Integration:** Supports user authentication via third-party OAuth2 providers, initially configured for **Google** and **GitHub**.
*   **Seamless User Provisioning:** When a user logs in with an OAuth2 provider for the first time, a new user account is automatically created and linked to their social identity.

### 3.4. Security Features

*   **Password Encryption:** User passwords are never stored in plaintext. They are securely hashed using the **BCrypt** algorithm.
*   **CSRF Protection:** Implements Cross-Site Request Forgery protection using a synchronized token pattern (cookie-based).
*   **Rate Limiting:** Protects against brute-force attacks by enforcing rate limits on sensitive endpoints like login and signup.

---

## 4. API Endpoint Definitions

The base URL for the service is `http://localhost:8080`.

| Endpoint                      | Method | Description                                                                                             |
| ----------------------------- | ------ | ------------------------------------------------------------------------------------------------------- |
| `/`                           | `GET`  | Public health-check endpoint to confirm the service is running.                                         |
| `/api/auth/signup`            | `POST` | Registers a new user with a username, email, and password.                                                |
| `/api/auth/login`             | `POST` | Authenticates a user with credentials and returns secure `HttpOnly` cookies for the session.                |
| `/api/auth/refresh`           | `POST` | Uses the refresh token (sent via cookie) to issue a new access token.                                     |
| `/api/auth/logout`            | `POST` | Invalidates the user's session and clears authentication cookies.                                           |
| `/oauth2/authorization/{provider}` | `GET`  | **(Browser Only)** Initiates the OAuth2 login flow. Replace `{provider}` with `google` or `github`. |

### These are the URLs that Google and GitHub will redirect back to. You never need to interact with these directly.•
Google Callback: http://localhost:8080/login/oauth2/code/google•GitHub Callback: http://localhost:8080/login/oauth2/code/github
### Example Payloads

**POST `/api/auth/signup`**
```json
{
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "a-very-secure-password"
}
```

**POST `/api/auth/login`**
```json
{
    "usernameOrEmail": "johndoe",
    "password": "a-very-secure-password"
}
```

---

## 5. OAuth2 Integration Flow

The OAuth2 endpoints are not traditional REST endpoints and are designed for a browser-based redirect flow.

1.  **Initiation:** A frontend application directs the user's browser to `http://localhost:8080/oauth2/authorization/google` (or `github`).
2.  **Provider Authentication:** The user is redirected to the provider's login page (e.g., Google's sign-in form).
3.  **Callback:** After successful authentication, the provider redirects the user back to the service's callback URL (`/login/oauth2/code/{provider}`), which is handled automatically by Spring Security.
4.  **Session Creation:** The Auth Service processes the callback, creates a local user account if one doesn't exist, issues JWTs, and sets them as secure cookies.
5.  **Final Redirect:** The service redirects the user's browser to the success URL defined in `app.oauth2.success-redirect` in the configuration.

---

## 6. Technical Stack

*   **Framework:** Spring Boot 3.5.5
*   **Language:** Java 21
*   **Security:** Spring Security 6
*   **Database:** MySQL 8.0
*   **Data Access:** Spring Data JPA / Hibernate
*   **Database Migrations:** Flyway
*   **Build Tool:** Apache Maven
*   **JWT Library:** `jjwt`

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

---

## 8. Project Structure

The project follows a standard Maven directory layout. Key files and directories are organized as follows:

```
.
├── .mvn/                           // Maven Wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/febin/auth/
│   │   │   │   ├── AuthApplication.java      // Main Spring Boot application class
│   │   │   │   ├── config/                   // Spring Security and other configurations
│   │   │   │   ├── controller/               // REST API controllers
│   │   │   │   ├── dto/                      // Data Transfer Objects (for API requests/responses)
│   │   │   │   ├── entity/                   // JPA entity classes (database models)
│   │   │   │   ├── oauth/                    // OAuth2 login success/failure handlers
│   │   │   │   ├── ratelimit/                // Rate limiting filter implementation
│   │   │   │   ├── repository/               // Spring Data JPA repositories
│   │   │   │   ├── security/                 // Custom security components (e.g., JWT filter)
│   │   │   │   ├── service/                  // Business logic and services
│   │   │   │   └── util/                     // Utility classes (e.g., JWT, Cookies)
│   │   │   └── db/migration/               // Flyway database migration scripts (Java-based)
│   │   └── resources/
│   │       ├── application.properties      // Main application configuration
│   │       └── static/                     // Static assets (if any)
│   └── test/
│       └── java/                         // Test source files
├── .gitignore                        // Specifies intentionally untracked files to ignore
├── Dockerfile                        // Instructions for building a Docker image
├── mvnw                              // Maven Wrapper script for Unix-like systems
├── mvnw.cmd                          // Maven Wrapper script for Windows
└── pom.xml                           // Project Object Model: defines dependencies and build config
```
