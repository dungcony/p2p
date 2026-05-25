package dungcony.ds.dtos;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * DTO JSON lưu một message trong local history
 */
@Slf4j
public record MesRecord(
        String messageId,
        String conversationPeerId,
        String conversationPeerName,
        String conversationPeerKey,
        String senderId,
        String senderHost,
        int senderPort,
        String receiverId,
        String receiverHost,
        int receiverPort,
        String groupId,
        String groupName,
        String messageType,
        String status,
        String content,
        long timestamp,
        boolean fromCurrentUser
) {
    public static MesRecord from(PeerInfo conversationPeer, Message message) {
        return new MesRecord(
                message.getId(),
                conversationPeer.getId(),
                conversationPeer.getName(),
                conversationPeer.addressKey(),
                message.getSenderId(),
                message.getSenderHost(),
                message.getSenderPort(),
                message.getReceiverId(),
                message.getReceiverHost(),
                message.getReceiverPort(),
                message.getGroupId(),
                message.getGroupName(),
                message.getType().name(),
                message.getStatus().name(),
                message.getContent(),
                message.getTimestamp(),
                message.isFromCurrentUser()
        );
    }

    // Phục hồi Message runtime từ DTO JSON
    public Message toMessage() {
        return Message.restore(
                messageId,
                MessageType.valueOf(messageType),
                senderId,
                senderHost,
                senderPort,
                receiverId,
                receiverHost,
                receiverPort,
                groupId,
                groupName,
                content,
                timestamp,
                fromCurrentUser,
                parseStatus()
        );
    }

    // Đọc status từ JSON cũ/mới, mặc định SENT để tương thích với file cũ chưa có field status
    private MessageStatus parseStatus() {
        if (status == null || status.isBlank()) {
            return MessageStatus.SENT;
        }
        try {
            return MessageStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.warn("Trạng thái tin nhắn trong JSON không hợp lệ. messageId={}, status={}. Dùng SENT.", messageId, status);
            return MessageStatus.SENT;
        }
    }
}
