package dungcony.ds.dtos;

import dungcony.ds.model.Message;

public record OfflineMessage(String messageId,
                             String senderId,
                             String receiverId,
                             String groupId,
                             String content,
                             long createdAt,
                             boolean delivered) {

//    private String messageId;
//    private String senderId;
//    private String receiverId;
//    private String groupId;
//    private String content;
//    private long createdAt;
//    private boolean delivered;
//
//    public OfflineMessage() {
//    }
//
//    public OfflineMessage(String messageId, String senderId, String receiverId, String groupId,
//                          String content, long createdAt, boolean delivered) {
//        this.messageId = messageId;
//        this.senderId = senderId;
//        this.receiverId = receiverId;
//        this.groupId = groupId;
//        this.content = content;
//        this.createdAt = createdAt;
//        this.delivered = delivered;
//    }



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
}
