package dungcony.ds.entities;

// Entity ánh xạ 1-1 với bảng offline_messages.
public class OfflineMessageEntity {
    private final String messageId;
    private final String senderId;
    private final String receiverId;
    private final String groupId;
    private final String content;
    private final long createdAt;
    private final boolean delivered;
    private final boolean encrypted;
    private final String encryptionAlgorithm;
    private final String encryptedFor;

    public OfflineMessageEntity(String messageId, String senderId, String receiverId, String groupId,
                                String content, long createdAt, boolean delivered) {
        this(messageId, senderId, receiverId, groupId, content, createdAt, delivered, false, null, null);
    }

    public OfflineMessageEntity(String messageId, String senderId, String receiverId, String groupId,
                                String content, long createdAt, boolean delivered,
                                boolean encrypted, String encryptionAlgorithm, String encryptedFor) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.content = content;
        this.createdAt = createdAt;
        this.delivered = delivered;
        this.encrypted = encrypted;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.encryptedFor = encryptedFor;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public String getEncryptedFor() {
        return encryptedFor;
    }
}
