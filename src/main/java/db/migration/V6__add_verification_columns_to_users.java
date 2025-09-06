package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V6__add_verification_columns_to_users extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("ALTER TABLE users ADD COLUMN verification_code VARCHAR(255) NULL");
            statement.execute("ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT FALSE");
        }
    }
}
