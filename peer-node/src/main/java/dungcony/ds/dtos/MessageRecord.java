package dungcony.ds.dtos;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.enums.MessageType;
import dungcony.ds.enums.MessageStatus;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

public class MessageRecord {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageRecord.class);
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

    // Chuyển Message runtime thành DTO phẳng để ghi JSON.
    public static MessageRecord from(PeerInfo conversationPeer, Message message) {
        MessageRecord record = new MessageRecord();
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

    // Phục hồi Message runtime từ DTO JSON.
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

    // Đọc status từ JSON cũ/mới, mặc định SENT để tương thích với file cũ chưa có field status.
    private MessageStatus parseStatus() {
        if (status == null || status.isBlank()) {
            return MessageStatus.SENT;
        }
        try {
            return MessageStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Trạng thái tin nhắn trong JSON không hợp lệ. messageId="
                    + messageId + ", status=" + status + ". Dùng SENT.");
            return MessageStatus.SENT;
        }
    }

    public String getMessageId() {
        return messageId;
    }

    public String getConversationPeerId() {
        return conversationPeerId;
    }

    public String getConversationPeerName() {
        return conversationPeerName;
    }

    public String getConversationPeerKey() {
        return conversationPeerKey;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getMessageType() {
        return messageType;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
