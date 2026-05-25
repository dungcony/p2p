package dungcony.ds.model;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.enums.MessageType;
import dungcony.ds.utils.Mes;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static dungcony.ds.utils.Mes.getMessage;

/**
 * Model message dùng chung cho chat, group, broadcast và control packet
 */
@Getter
@Setter
public class Message {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private String id;
    private MessageType type;
    private String senderId;
    private String senderHost;
    private int senderPort;
    private String receiverId;
    private String receiverHost;
    private int receiverPort;
    private String groupId;
    private String groupName;
    private String content;
    private long timestamp;
    private MessageStatus status;
    private List<PeerInfo> peers;
    private List<PeerInfo> groupMembers;
    private transient boolean fromCurrentUser; // đánh dấu là không lưu field này transient nghĩa là đây là trạng thái local

    public Message() {
    }

    // Constructor đơn giản phục vụ UI khi cần tạo message hiển thị cục bộ
    public Message(String senderHost, String content, boolean fromCurrentUser) {
        this.id = UUID.randomUUID().toString();
        this.type = MessageType.CHAT;
        this.senderHost = senderHost == null ? "" : senderHost.trim();
        this.content = content == null ? "" : content;
        this.timestamp = Instant.now().toEpochMilli();
        this.status = MessageStatus.SENT;
        this.fromCurrentUser = fromCurrentUser;
    }

    // Tạo message chat 1-1 có đầy đủ sender, receiver và timestamp
    public static Message chat(PeerInfo sender, PeerInfo receiver, String content) {
        Message message = new Message();
        message.id = UUID.randomUUID().toString();
        message.type = MessageType.CHAT;
        return getMessage(sender, receiver, content, message);
    }


    // Tạo message chat nhóm cho một receiver cụ thể để bootstrap có thể lưu offline theo user_id
    public static Message groupChat(PeerInfo sender, PeerInfo receiver, String groupId, String groupName, String content) {
        Message message = Mes.groupChat(sender, groupId, groupName, content);
        message.receiverId = receiver.getId();
        message.receiverHost = receiver.getHost();
        message.receiverPort = receiver.getPort();
        return message;
    }

    // Tạo message broadcast toàn mạng cho một receiver cụ thể
    public static Message broadcast(PeerInfo sender, PeerInfo receiver, String content) {
        Message message = new Message();
        message.id = UUID.randomUUID().toString();
        message.type = MessageType.BROADCAST;
        return getMessage(sender, receiver, content, message);
    }

    // Tạo ACK phản hồi cho message nguồn để bên gửi biết tin đã được nhận
    public static Message ack(Message source, PeerInfo sender) {
        Message message = new Message();
        message.id = source.getId();
        message.type = MessageType.ACK;
        getMessage(source, sender, message);
        return message;
    }


    // Tạo heartbeat message dùng để kiểm tra peer còn online hay không
    public static Message heartbeat(PeerInfo sender) {
        Message message = new Message();
        message.id = UUID.randomUUID().toString();
        message.type = MessageType.HEARTBEAT;
        message.senderId = sender.getId();
        message.senderHost = sender.getHost();
        message.senderPort = sender.getPort();
        message.timestamp = Instant.now().toEpochMilli();
        message.status = MessageStatus.SENT;
        return message;
    }

    // Tạo request hỏi peer đích danh sách peer mà nó đang biết
    public static Message peerListRequest(PeerInfo sender, PeerInfo receiver) {
        Message message = new Message();
        message.id = UUID.randomUUID().toString();
        message.type = MessageType.PEER_LIST_REQUEST;
        message.senderId = sender.getId();
        message.senderHost = sender.getHost();
        message.senderPort = sender.getPort();
        message.receiverId = receiver.getId();
        message.receiverHost = receiver.getHost();
        message.receiverPort = receiver.getPort();
        message.timestamp = Instant.now().toEpochMilli();
        message.status = MessageStatus.SENT;
        return message;
    }

    // Tạo response trả danh sách peer đã biết cho requester
    public static Message peerListResponse(PeerInfo sender, Message request, Collection<PeerInfo> peers) {
        Message message = new Message();
        message.id = request.getId();
        message.type = MessageType.PEER_LIST_RESPONSE;
        getMessage(request, sender, message);
        message.peers = peers == null ? new ArrayList<>() : new ArrayList<>(peers);
        return message;
    }

    // Tạo message đồng bộ membership của group tới một member đang online
    public static Message groupMembersSync(PeerInfo sender, PeerInfo receiver, Group group) {
        Message message = new Message();
        message.id = UUID.randomUUID().toString();
        message.type = MessageType.GROUP_MEMBERS_SYNC;
        message.senderId = sender.getId();
        message.senderHost = sender.getHost();
        message.senderPort = sender.getPort();
        message.receiverId = receiver.getId();
        message.receiverHost = receiver.getHost();
        message.receiverPort = receiver.getPort();
        message.groupId = group.getGroupId();
        message.groupName = group.getName();
        message.timestamp = Instant.now().toEpochMilli();
        message.status = MessageStatus.SENT;
        message.groupMembers = new ArrayList<>(group.getMembers());
        return message;
    }

    // Phục hồi message từ local JSON store để UI hiển thị lại lịch sử chat
    public static Message restore(String id, MessageType type, String senderId, String senderHost, int senderPort,
                                  String receiverId, String receiverHost, int receiverPort, String groupId,
                                  String content, long timestamp, boolean fromCurrentUser) {
        return Mes.restore(id, type, senderId, senderHost, senderPort, receiverId, receiverHost, receiverPort,
                groupId, null, content, timestamp, fromCurrentUser);
    }


    // Phục hồi message từ local JSON với trạng thái gửi đã lưu
    public static Message restore(String id, MessageType type, String senderId, String senderHost, int senderPort,
                                  String receiverId, String receiverHost, int receiverPort, String groupId,
                                  String groupName, String content, long timestamp, boolean fromCurrentUser,
                                  MessageStatus status) {
        Message message = Mes.restore(id, type, senderId, senderHost, senderPort, receiverId, receiverHost,
                receiverPort, groupId, groupName, content, timestamp, fromCurrentUser);
        message.status = status == null ? MessageStatus.SENT : status;
        return message;
    }


    public String getSenderIp() {
        if (senderPort > 0) {
            return senderHost + ":" + senderPort;
        }
        return senderHost;
    }

    public MessageStatus getStatus() {
        return status == null ? MessageStatus.SENT : status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status == null ? MessageStatus.SENT : status;
    }

    public List<PeerInfo> getPeers() {
        return peers == null ? List.of() : peers;
    }

    public List<PeerInfo> getGroupMembers() {
        return groupMembers == null ? List.of() : groupMembers;
    }

    public String getFormattedTime() {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}
