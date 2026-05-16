package dungcony.ds.dtos;

import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

public class MessageRecord {
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
    private String messageType;
    private String content;
    private long timestamp;
    private boolean fromCurrentUser;

    /**
     * Chuyen Message runtime thanh DTO phang de ghi JSON.
     */
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
        record.messageType = message.getType().name();
        record.content = message.getContent();
        record.timestamp = message.getTimestamp();
        record.fromCurrentUser = message.isFromCurrentUser();
        return record;
    }

    /**
     * Phuc hoi Message runtime tu DTO JSON.
     */
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
                content,
                timestamp,
                fromCurrentUser
        );
    }

    public String getMessageId() {
        return messageId;
    }

    public String getConversationPeerId() {
        return conversationPeerId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
