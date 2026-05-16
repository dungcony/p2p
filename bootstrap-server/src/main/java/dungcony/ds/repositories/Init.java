package dungcony.ds.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Init {
    private static final Logger LOGGER = LoggerFactory.getLogger(Init.class);
    private final Conn conn;

    public Init(Conn conn) {
        this.conn = conn;
    }

    /**
     * Tao cac bang neu file database moi hoac chua co schema.
     */
    public void initializeSchema() {
        try (Connection connection = conn.getConnection();
             Statement statement = connection.createStatement()) {
            LOGGER.info("Đang khởi tạo schema bootstrap nếu còn thiếu. url={}", conn.getJdbcUrl());
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
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
                        delivered INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            LOGGER.info("Schema SQLite bootstrap đã sẵn sàng.");
        } catch (SQLException e) {
            LOGGER.error("Không thể khởi tạo schema database bootstrap.", e);
            throw new IllegalStateException("Không thể khởi tạo database bootstrap", e);
        }
    }
}
