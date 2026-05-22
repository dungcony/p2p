package dungcony.ds.repositories;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.entities.UserEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public record UserRepo(Conn conn) {

    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepo.class);
// Thêm mới hoặc cập nhật user bằng connection riêng khi xử lý REGISTER độc lập.
    public void upsert(UserEntity userEntity) {
        try (Connection connection = conn.getConnection()) {
            upsert(userEntity, connection);
            LOGGER.info("SQLite đã upsert user=" + userEntity.getUserId()
                    + ", tênHiểnThị=" + userEntity.getDisplayName());
        } catch (SQLException e) {
            LOGGER.error("Không thể upsert user: " + e.getMessage());
        }
    }

    // Thêm mới hoặc cập nhật user trong bảng users.
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
