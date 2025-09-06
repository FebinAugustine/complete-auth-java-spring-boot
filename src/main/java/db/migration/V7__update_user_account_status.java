package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V7__update_user_account_status extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            // Add the new account_status column, defaulting to 'UNVERIFIED' for safety.
            statement.execute("ALTER TABLE users ADD COLUMN account_status VARCHAR(255) NOT NULL DEFAULT 'UNVERIFIED'");

            // This check makes the migration runnable on a clean database or an existing one.
            // It looks for the 'enabled' column before trying to migrate data from it.
            if (columnExists(context, "users", "enabled")) {
                statement.execute("UPDATE users SET account_status = 'ACTIVE' WHERE enabled = TRUE");
                statement.execute("ALTER TABLE users DROP COLUMN enabled");
            }
        }
    }

    private boolean columnExists(Context context, String tableName, String columnName) throws Exception {
        try (var rs = context.getConnection().getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }
}
