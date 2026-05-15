package dungcony.ds.repositories;

import dungcony.ds.entities.GroupMemberEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record GroupMemberRepo(Conn conn) {

    /**
     * Them thanh vien vao group_members.
     */
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
            System.out.println("[INFO] Added group member groupId=" + memberEntity.getGroupId()
                    + ", userId=" + memberEntity.getUserId());
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to add group member: " + e.getMessage());
        }
    }

    /**
     * Xoa thanh vien khoi group_members.
     */
    public void remove(String groupId, String userId) {
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM group_members
                     WHERE group_id = ? AND user_id = ?
                     """)) {
            statement.setString(1, groupId);
            statement.setString(2, userId);
            int deleted = statement.executeUpdate();
            System.out.println("[INFO] Removed group member groupId=" + groupId
                    + ", userId=" + userId + ", deleted=" + deleted);
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to remove group member: " + e.getMessage());
        }
    }

    /**
     * Lay danh sach thanh vien cua mot group.
     */
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
            System.out.println("[ERROR] Failed to list group members: " + e.getMessage());
        }
        return members;
    }
}
