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
                             boolean delivered,
                             boolean encrypted,
                             String encryptionAlgorithm,
                             String encryptedFor) {
    public OfflineMessage(String messageId,
                          String senderId,
                          String receiverId,
                          String groupId,
                          String content,
                          long createdAt,
                          boolean delivered) {
        this(messageId, senderId, receiverId, groupId, content, createdAt, delivered, false, null, null);
    }
}
