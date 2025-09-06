package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class V8__create_admin_user extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String adminPassword = encoder.encode("admin123");

        // Check if admin user already exists to make this migration repeatable
        boolean adminExists = false;
        try (PreparedStatement ps = context.getConnection().prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, "admin");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    adminExists = true;
                }
            }
        }

        if (!adminExists) {
            // Insert the admin user with the new account_status column
            try (PreparedStatement statement = context.getConnection().prepareStatement(
                    "INSERT INTO users (username, email, password, account_status, created_at) VALUES (?, ?, ?, ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, "admin");
                statement.setString(2, "admin@example.com");
                statement.setString(3, adminPassword);
                statement.setString(4, "ACTIVE"); // Set account status directly
                statement.executeUpdate();

                // Get the ID of the newly created admin user
                long adminUserId;
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        adminUserId = generatedKeys.getLong(1);
                    } else {
                        throw new IllegalStateException("Could not get id of inserted admin user.");
                    }
                }

                // Find the IDs of ROLE_USER and ROLE_ADMIN
                long userRoleId = -1, adminRoleId = -1;
                try (Statement selectStatement = context.getConnection().createStatement()) {
                    try (ResultSet rs = selectStatement.executeQuery("SELECT id FROM roles WHERE name = 'ROLE_USER'")) {
                        if (rs.next()) userRoleId = rs.getLong(1);
                    }
                    try (ResultSet rs = selectStatement.executeQuery("SELECT id FROM roles WHERE name = 'ROLE_ADMIN'")) {
                        if (rs.next()) adminRoleId = rs.getLong(1);
                    }
                }

                if (userRoleId == -1 || adminRoleId == -1) {
                    throw new IllegalStateException("Could not find default roles in the database.");
                }

                // Assign both roles to the admin user
                try (PreparedStatement linkStatement = context.getConnection().prepareStatement(
                        "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                    linkStatement.setLong(1, adminUserId);
                    linkStatement.setLong(2, userRoleId);
                    linkStatement.executeUpdate();

                    linkStatement.setLong(1, adminUserId);
                    linkStatement.setLong(2, adminRoleId);
                    linkStatement.executeUpdate();
                }
            }
        }
    }
}
