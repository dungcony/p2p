package dungcony.ds.utils;

import dungcony.ds.dtos.OfflineMessage;
import dungcony.ds.enums.MessageStatus;
import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.time.Instant;
import java.util.UUID;

public class Mes {
    // Tạo offline message từ message P2P khi gửi trực tiếp thất bại
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

    public static Message getMessage(PeerInfo sender, PeerInfo receiver, String content, Message message) {
        message.setSenderId(sender.getId());
        message.setSenderHost(sender.getHost());
        message.setSenderPort(sender.getPort());

        message.setReceiverId(receiver.getId());
        message.setReceiverHost(receiver.getHost());
        message.setReceiverPort(receiver.getPort());

        message.setContent(content == null ? "" : content);
        message.setTimestamp(Instant.now().toEpochMilli());
        message.setStatus(MessageStatus.SENDING);
        message.setFromCurrentUser(true);

        return message;
    }

    public static void getMessage(Message source, PeerInfo sender, Message message) {
        message.setSenderId(sender.getId());
        message.setSenderHost(sender.getHost());
        message.setSenderPort(sender.getPort());

        message.setReceiverId(source.getSenderId());
        message.setReceiverHost(source.getSenderHost());
        message.setReceiverPort(source.getSenderPort());

        message.setTimestamp(Instant.now().toEpochMilli());
        message.setStatus(MessageStatus.SENT);
    }

    // Phục hồi message từ local JSON, bao gồm metadata group nếu có
    public static Message restore(String id, MessageType type, String senderId, String senderHost, int senderPort,
                                  String receiverId, String receiverHost, int receiverPort, String groupId,
                                  String groupName, String content, long timestamp, boolean fromCurrentUser) {
        Message message = new Message();

        message.setId(id);
        message.setType(type);

        message.setSenderId(senderId);
        message.setSenderHost(senderHost);
        message.setSenderPort(senderPort);

        message.setReceiverId(receiverId);
        message.setReceiverHost(receiverHost);
        message.setReceiverPort(receiverPort);

        message.setGroupId(groupId);
        message.setGroupName(groupName);

        message.setContent(content == null ? "" : content);
        message.setTimestamp(timestamp);
        message.setStatus(MessageStatus.SENT);
        message.setFromCurrentUser(fromCurrentUser);

        return message;
    }


    // Tạo message chat nhóm có tên group để peer nhận có thể tạo group local khi bootstrap không sẵn sàng
    public static Message groupChat(PeerInfo sender, String groupId, String groupName, String content) {
        Message message = new Message();

        message.setId(UUID.randomUUID().toString());
        message.setType(MessageType.GROUP_CHAT);

        message.setSenderId(sender.getId());
        message.setSenderHost(sender.getHost());
        message.setSenderPort(sender.getPort());

        message.setGroupId(groupId);
        message.setGroupName(groupName);

        message.setContent(content == null ? "" : content);
        message.setTimestamp(Instant.now().toEpochMilli());
        message.setStatus(MessageStatus.SENDING);
        message.setFromCurrentUser(true);

        return message;
    }

}
