package dungcony.ds.dtos;

public record OfflineMessage(String messageId,
                             String senderId,
                             String receiverId,
                             String groupId,
                             String content,
                             long createdAt,
                             boolean delivered) {
}
