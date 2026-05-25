package dungcony.ds.repositories;

import dungcony.ds.config.Conn;
import dungcony.ds.entities.GroupMemberEntity;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public record GroupMemberRepo(Conn conn) {
    // Thêm thành viên vào group_members.
    public void add(GroupMemberEntity memberEntity) {
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO group_members(group_id, user_id, joined_at)
                     VALUES(?, ?, ?)
                     ON CONFLICT(group_id, user_id) DO UPDATE SET
                         joined_at = excluded.joined_at
                     """)) {
            statement.setString(1, memberEntity.getGroupId());
            statement.setString(2, memberEntity.getUserId());
            statement.setLong(3, memberEntity.getJoinedAt());
            statement.executeUpdate();
            log.info("Đã thêm thành viên nhóm groupId={}, userId={}", memberEntity.getGroupId(), memberEntity.getUserId());
        } catch (SQLException e) {
            log.error("Không thể thêm thành viên nhóm: {}", e.getMessage());
        }
    }

    // Xóa thành viên khỏi group_members.
    public void remove(String groupId, String userId) {
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM group_members
                     WHERE group_id = ? AND user_id = ?
                     """)) {
            statement.setString(1, groupId);
            statement.setString(2, userId);
            int deleted = statement.executeUpdate();
            log.info("Đã xóa thành viên nhóm groupId={}, userId={}, đãXóa={}", groupId, userId, deleted);
        } catch (SQLException e) {
            log.error("Không thể xóa thành viên nhóm: {}", e.getMessage());
        }
    }

    // Lấy danh sách thành viên của một group.
    public Collection<GroupMemberEntity> listByGroup(String groupId) {
        List<GroupMemberEntity> members = new ArrayList<>();
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT group_id, user_id, joined_at
                     FROM group_members
                     WHERE group_id = ?
                     ORDER BY joined_at ASC
                     """)) {
            statement.setString(1, groupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    members.add(new GroupMemberEntity(
                            resultSet.getString("group_id"),
                            resultSet.getString("user_id"),
                            resultSet.getLong("joined_at")
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Không thể liệt kê thành viên nhóm: {}", e.getMessage());
        }
        return members;
    }
}
