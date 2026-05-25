package dungcony.ds.dtos;

/**
 * DTO lưu tin nhắn offline trên bootstrap để giao lại khi peer online
 */
public record OfflineMessage(String messageId,
                             String senderId,
                             String receiverId,
                             String groupId,
                             String content,
                             long createdAt,
                             boolean delivered) {
}
