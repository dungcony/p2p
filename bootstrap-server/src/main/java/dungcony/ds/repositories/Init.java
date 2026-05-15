package dungcony.ds.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Init {
    private final Conn conn;

    public Init(Conn conn) {
        this.conn = conn;
    }

    /**
     * Tao schema SQLite cho cac bang bootstrap-server can quan ly.
     */
    public void initializeSchema() {
        try (Connection connection = conn.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        user_id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS peers_online (
                        peer_key TEXT PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        host TEXT NOT NULL,
                        port INTEGER NOT NULL,
                        online INTEGER NOT NULL,
                        last_seen INTEGER NOT NULL,
                        FOREIGN KEY(user_id) REFERENCES users(user_id)
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
            System.out.println("[INFO] Bootstrap SQLite schema ready. url=" + conn.getJdbcUrl());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize bootstrap database", e);
        }
    }
}
