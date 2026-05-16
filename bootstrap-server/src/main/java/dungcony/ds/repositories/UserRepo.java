package dungcony.ds.repositories;

import dungcony.ds.entities.UserEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public record UserRepo(Conn conn) {

    /**
     * Them moi hoac cap nhat user bang connection rieng khi xu ly REGISTER doc lap.
     */
    public void upsert(UserEntity userEntity) {
        try (Connection connection = conn.getConnection()) {
            upsert(userEntity, connection);
            System.out.println("[INFO] SQLite upserted user=" + userEntity.getUserId()
                    + ", displayName=" + userEntity.getDisplayName());
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to upsert user: " + e.getMessage());
        }
    }

    /**
     * Them moi hoac cap nhat user trong bang users.
     */
    public void upsert(UserEntity userEntity, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users(user_id, display_name, created_at, updated_at)
                VALUES(?, ?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    display_name = excluded.display_name,
                    updated_at = excluded.updated_at
                """)) {
            statement.setString(1, userEntity.getUserId());
            statement.setString(2, userEntity.getDisplayName());
            statement.setLong(3, userEntity.getCreatedAt());
            statement.setLong(4, userEntity.getUpdatedAt());
            statement.executeUpdate();
        }
    }
}
