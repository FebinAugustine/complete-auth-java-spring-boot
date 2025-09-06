package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.*;

/**
 * V3: Create user_providers table to support multiple OAuth providers per user.
 * Also drops provider/provider_id columns from users if they exist (done safely).
 */
public class V3__create_user_providers_and_drop_user_provider_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement stmt = context.getConnection().createStatement()) {

            // Create table user_providers if not exists
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS user_providers (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      user_id BIGINT NOT NULL,
                      provider VARCHAR(50) NOT NULL,
                      provider_id VARCHAR(255) NOT NULL,
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      CONSTRAINT fk_user_providers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
                    )
                    """);

            // Create unique index for provider+provider_id if not exists
            // MySQL doesn't support IF NOT EXISTS for CREATE INDEX in older versions, so check metadata
            DatabaseMetaData meta = context.getConnection().getMetaData();
            boolean indexExists = false;
            try (ResultSet rs = meta.getIndexInfo(null, null, "user_providers", false, false)) {
                while (rs.next()) {
                    String idxName = rs.getString("INDEX_NAME");
                    if (idxName != null && idxName.equalsIgnoreCase("uk_user_providers_provider_providerid")) {
                        indexExists = true;
                        break;
                    }
                }
            }
            if (!indexExists) {
                try {
                    stmt.execute("CREATE UNIQUE INDEX uk_user_providers_provider_providerid ON user_providers (provider, provider_id)");
                } catch (SQLException e) {
                    // ignore if concurrent or unsupported, but log in real app
                }
            }

            // Create index for user_id if not exists
            boolean userIdxExists = false;
            try (ResultSet rs2 = meta.getIndexInfo(null, null, "user_providers", false, false)) {
                while (rs2.next()) {
                    String idxName = rs2.getString("INDEX_NAME");
                    if (idxName != null && idxName.equalsIgnoreCase("idx_user_providers_user_id")) {
                        userIdxExists = true;
                        break;
                    }
                }
            }
            if (!userIdxExists) {
                try {
                    stmt.execute("CREATE INDEX idx_user_providers_user_id ON user_providers (user_id)");
                } catch (SQLException e) {
                    // ignore
                }
            }

            // Drop provider and provider_id columns from users if present (safe check)
            try (ResultSet rsProv = meta.getColumns(null, null, "users", "provider")) {
                if (rsProv.next()) {
                    try {
                        stmt.execute("ALTER TABLE users DROP COLUMN provider");
                    } catch (SQLException e) {
                        // ignore or log
                    }
                }
            }

            try (ResultSet rsProvId = meta.getColumns(null, null, "users", "provider_id")) {
                if (rsProvId.next()) {
                    try {
                        stmt.execute("ALTER TABLE users DROP COLUMN provider_id");
                    } catch (SQLException e) {
                        // ignore or log
                    }
                }
            }
        }
    }
}
