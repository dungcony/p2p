package dungcony.ds.repositories;

import dungcony.ds.config.Conn;
import dungcony.ds.entities.UserEntity;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public record UserRepo(Conn conn) {
    // Thêm mới hoặc cập nhật user bằng connection riêng khi xử lý REGISTER độc lập.
    public void upsert(UserEntity userEntity) {
        try (Connection connection = conn.getConnection()) {
            upsert(userEntity, connection);
            log.info("SQLite đã upsert user={}, tênHiểnThị={}", userEntity.getUserId(), userEntity.getDisplayName());
        } catch (SQLException e) {
            log.error("Không thể upsert user: {}", e.getMessage());
        }
    }

    // Thêm mới hoặc cập nhật user trong bảng users.
    public void upsert(UserEntity userEntity, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO users(user_id, display_name, public_key, created_at, updated_at)
                VALUES(?, ?, ?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                    display_name = excluded.display_name,
                    public_key = COALESCE(excluded.public_key, users.public_key),
                    updated_at = excluded.updated_at
                """)) {
            statement.setString(1, userEntity.getUserId());
            statement.setString(2, userEntity.getDisplayName());
            statement.setString(3, userEntity.getPublicKey());
            statement.setLong(4, userEntity.getCreatedAt());
            statement.setLong(5, userEntity.getUpdatedAt());
            statement.executeUpdate();
        }
    }
}
