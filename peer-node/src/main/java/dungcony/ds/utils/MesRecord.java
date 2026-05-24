package dungcony.ds.utils;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class MesRecord {
    private String messageId;
    private String conversationPeerId;
    private String conversationPeerName;
    private String conversationPeerKey;
    private String senderId;
    private String senderHost;
    private int senderPort;
    private String receiverId;
    private String receiverHost;
    private int receiverPort;
    private String groupId;
    private String groupName;
    private String messageType;
    private String status;
    private String content;
    private long timestamp;
    private boolean fromCurrentUser;

    public static MesRecord from(PeerInfo conversationPeer, Message message) {
        MesRecord record = new MesRecord();
        record.messageId = message.getId();
        record.conversationPeerId = conversationPeer.getId();
        record.conversationPeerName = conversationPeer.getName();
        record.conversationPeerKey = conversationPeer.addressKey();
        record.senderId = message.getSenderId();
        record.senderHost = message.getSenderHost();
        record.senderPort = message.getSenderPort();
        record.receiverId = message.getReceiverId();
        record.receiverHost = message.getReceiverHost();
        record.receiverPort = message.getReceiverPort();
        record.groupId = message.getGroupId();
        record.groupName = message.getGroupName();
        record.messageType = message.getType().name();
        record.status = message.getStatus().name();
        record.content = message.getContent();
        record.timestamp = message.getTimestamp();
        record.fromCurrentUser = message.isFromCurrentUser();
        return record;
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
