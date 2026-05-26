package dungcony.ds.config;

import lombok.extern.slf4j.Slf4j;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class Init {
    private final Conn conn;

    public Init(Conn conn) {
        this.conn = conn;
    }

    // Tạo các bảng nếu file database mới hoặc chưa có schema.
    public void initializeSchema() {
        try (Connection connection = conn.getConnection();
             Statement statement = connection.createStatement()) {
            log.info("Đang khởi tạo schema bootstrap nếu còn thiếu. url={}", conn.getJdbcUrl());
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        public_key TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS groups (
                        group_id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        created_by TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS group_members (
                        group_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        joined_at INTEGER NOT NULL,
                        PRIMARY KEY(group_id, user_id),
                        FOREIGN KEY(group_id) REFERENCES groups(group_id),
                        FOREIGN KEY(user_id) REFERENCES users(user_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS offline_messages (
                        message_id TEXT PRIMARY KEY,
                        sender_id TEXT NOT NULL,
                        receiver_id TEXT,
                        group_id TEXT,
                        content TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        delivered INTEGER NOT NULL DEFAULT 0,
                        encrypted INTEGER NOT NULL DEFAULT 0,
                        encryption_algorithm TEXT,
                        encrypted_for TEXT
                    )
                    """);
            ensureColumn(connection, "users", "public_key", "TEXT");
            ensureColumn(connection, "offline_messages", "encrypted", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "offline_messages", "encryption_algorithm", "TEXT");
            ensureColumn(connection, "offline_messages", "encrypted_for", "TEXT");
            log.info("Schema SQLite bootstrap đã sẵn sàng.");
        } catch (SQLException e) {
            log.error("Không thể khởi tạo schema database bootstrap.", e);
            throw new IllegalStateException("Không thể khởi tạo database bootstrap", e);
        }
    }

    private void ensureColumn(Connection connection, String table, String column, String definition) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
        log.info("ÄÃ£ migrate SQLite column {}.{}", table, column);
    }
}
