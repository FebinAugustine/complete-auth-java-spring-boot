package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;
import java.sql.ResultSet;

/**
 * V1: Create initial schema: roles, users, user_roles, refresh_tokens
 * and seed default roles.
 */
public class V1__init extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement stmt = context.getConnection().createStatement()) {

            // roles table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS roles (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(50) NOT NULL UNIQUE
                    )
                    """);

            // users table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(100) NOT NULL UNIQUE,
                      email VARCHAR(255) NOT NULL UNIQUE,
                      password VARCHAR(255) NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // user_roles join table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS user_roles (
                      user_id BIGINT NOT NULL,
                      role_id BIGINT NOT NULL,
                      PRIMARY KEY (user_id, role_id),
                      CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                      CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
                    )
                    """);

            // refresh_tokens
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS refresh_tokens (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      token VARCHAR(255) NOT NULL UNIQUE,
                      user_id BIGINT NOT NULL,
                      expiry_date DATETIME NOT NULL,
                      revoked BOOLEAN NOT NULL DEFAULT false,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
                    )
                    """);

            // Seed roles (insert only if not present)
            // role: ROLE_USER
            try (ResultSet rs = context.getConnection().getMetaData()
                    .getTables(null, null, "roles", null)) {
                // no-op; the table was just created
            }
            // Insert roles defensively
            stmt.executeUpdate("""
                    INSERT INTO roles (name)
                      SELECT 'ROLE_USER' FROM (SELECT 1) AS tmp
                      WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER')
                    """);
            stmt.executeUpdate("""
                    INSERT INTO roles (name)
                      SELECT 'ROLE_ADMIN' FROM (SELECT 1) AS tmp
                      WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN')
                    """);
        }
    }
}
