package dungcony.ds.repositories;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.entities.GroupEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record GroupRepo(Conn conn) {

    
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupRepo.class);
// Tạo/cập nhật metadata group trong bảng groups.
    public void upsert(GroupEntity groupEntity) {
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO groups(group_id, name, created_by, created_at)
                     VALUES(?, ?, ?, ?)
                     ON CONFLICT(group_id) DO UPDATE SET
                         name = excluded.name
                     """)) {
            statement.setString(1, groupEntity.getGroupId());
            statement.setString(2, groupEntity.getName());
            statement.setString(3, groupEntity.getCreatedBy());
            statement.setLong(4, groupEntity.getCreatedAt());
            statement.executeUpdate();
            LOGGER.info("Đã lưu nhóm id=" + groupEntity.getGroupId());
        } catch (SQLException e) {
            LOGGER.error("Không thể lưu nhóm: " + e.getMessage());
        }
    }

    // Lấy toàn bộ group metadata bootstrap đang lưu.
    public Collection<GroupEntity> listAll() {
        List<GroupEntity> groups = new ArrayList<>();
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT group_id, name, created_by, created_at
                     FROM groups
                     ORDER BY created_at DESC
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                groups.add(new GroupEntity(
                        resultSet.getString("group_id"),
                        resultSet.getString("name"),
                        resultSet.getString("created_by"),
                        resultSet.getLong("created_at")
                ));
            }
        } catch (SQLException e) {
            LOGGER.error("Không thể liệt kê nhóm: " + e.getMessage());
        }
        return groups;
    }
}
