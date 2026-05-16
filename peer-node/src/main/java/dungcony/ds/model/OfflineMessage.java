package dungcony.ds.model;

public class OfflineMessage {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String groupId;
    private String content;
    private long createdAt;
    private boolean delivered;

    public OfflineMessage() {
    }

    public OfflineMessage(String messageId, String senderId, String receiverId, String groupId,
                          String content, long createdAt, boolean delivered) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.content = content;
        this.createdAt = createdAt;
        this.delivered = delivered;
    }

    /**
     * Tao offline message tu message P2P khi gui truc tiep that bai.
     */
    public static OfflineMessage fromMessage(Message message) {
        return new OfflineMessage(
                message.getId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getGroupId(),
                message.getContent(),
                message.getTimestamp(),
                false
        );
    }

    /**
     * Lay id duy nhat cua tin offline do bootstrap-server luu.
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Lay id peer da gui tin offline.
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Lay id peer nhan tin offline.
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * Lay id group neu day la tin nhan nhom offline.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Lay noi dung tin nhan offline.
     */
    public String getContent() {
        return content;
    }

    /**
     * Lay thoi diem bootstrap-server ghi nhan tin offline.
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Cho biet bootstrap-server da danh dau tin nay la da giao hay chua.
     */
    public boolean isDelivered() {
        return delivered;
    }
}
