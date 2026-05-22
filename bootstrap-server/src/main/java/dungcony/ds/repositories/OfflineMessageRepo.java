package dungcony.ds.repositories;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.entities.OfflineMessageEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record OfflineMessageRepo(Conn conn) {

    
    private static final Logger LOGGER = LoggerFactory.getLogger(OfflineMessageRepo.class);
// Lưu message vào bảng offline_messages khi receiver đang offline.
    public void save(OfflineMessageEntity message) {
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO offline_messages(
                         message_id, sender_id, receiver_id, group_id, content, created_at, delivered
                     )
                     VALUES(?, ?, ?, ?, ?, ?, ?)
                     ON CONFLICT(message_id) DO UPDATE SET
                         content = excluded.content,
                         delivered = excluded.delivered
                     """)) {
            statement.setString(1, message.getMessageId());
            statement.setString(2, message.getSenderId());
            statement.setString(3, message.getReceiverId());
            statement.setString(4, message.getGroupId());
            statement.setString(5, message.getContent());
            statement.setLong(6, message.getCreatedAt());
            statement.setInt(7, message.isDelivered() ? 1 : 0);
            statement.executeUpdate();
            LOGGER.info("Đã lưu tin nhắn offline id=" + message.getMessageId()
                    + ", receiver=" + message.getReceiverId());
        } catch (SQLException e) {
            LOGGER.error("Không thể lưu tin nhắn offline: " + e.getMessage());
        }
    }

    // Lấy các offline message chưa delivered của receiver.
    public Collection<OfflineMessageEntity> findPendingByReceiver(String receiverId) {
        List<OfflineMessageEntity> messages = new ArrayList<>();
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT message_id, sender_id, receiver_id, group_id, content, created_at, delivered
                     FROM offline_messages
                     WHERE receiver_id = ? AND delivered = 0
                     ORDER BY created_at ASC
                     """)) {
            statement.setString(1, receiverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(new OfflineMessageEntity(
                            resultSet.getString("message_id"),
                            resultSet.getString("sender_id"),
                            resultSet.getString("receiver_id"),
                            resultSet.getString("group_id"),
                            resultSet.getString("content"),
                            resultSet.getLong("created_at"),
                            resultSet.getInt("delivered") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Không thể nạp tin nhắn offline: " + e.getMessage());
        }
        return messages;
    }

    // Đánh dấu các message đã được giao cho receiver khi peer JOIN lại.
    public void markDelivered(Collection<OfflineMessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        try (Connection connection = conn.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE offline_messages
                     SET delivered = 1
                     WHERE message_id = ?
                     """)) {
            for (OfflineMessageEntity message : messages) {
                statement.setString(1, message.getMessageId());
                statement.addBatch();
            }
            int[] updated = statement.executeBatch();
            LOGGER.info("Đã đánh dấu tin nhắn offline đã giao. sốLượng=" + updated.length);
        } catch (SQLException e) {
            LOGGER.error("Không thể đánh dấu tin nhắn offline đã giao: " + e.getMessage());
        }
    }
}
