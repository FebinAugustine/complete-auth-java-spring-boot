package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * V2: Add provider and provider_id columns to users table if they do not exist.
 * This migration is optional and kept for backward compatibility if code expects columns.
 */
public class V2__add_oauth_columns extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        DatabaseMetaData meta = context.getConnection().getMetaData();

        boolean hasProvider = false;
        boolean hasProviderId = false;

        try (ResultSet rs = meta.getColumns(null, null, "users", "provider")) {
            hasProvider = rs.next();
        }
        try (ResultSet rs2 = meta.getColumns(null, null, "users", "provider_id")) {
            hasProviderId = rs2.next();
        }

        try (Statement stmt = context.getConnection().createStatement()) {
            if (!hasProvider) {
                stmt.execute("ALTER TABLE users ADD COLUMN provider VARCHAR(50) DEFAULT 'LOCAL'");
            }
            if (!hasProviderId) {
                stmt.execute("ALTER TABLE users ADD COLUMN provider_id VARCHAR(255)");
            }
        }
    }
}
