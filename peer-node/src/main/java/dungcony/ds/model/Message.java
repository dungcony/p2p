package dungcony.ds.model;

import dungcony.ds.enums.MessageType;
import dungcony.ds.enums.MessageStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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
    private transient boolean fromCurrentUser;

    public Message() {
    }

    /**
     * Constructor đơn giản phục vụ UI khi cần tạo message hiển thị cục bộ.
     */
    public Message(String senderHost, String content, boolean fromCurrentUser) {
        this.id = UUID.randomUUID().toString();
        this.type = MessageType.CHAT;
        this.senderHost = senderHost == null ? "" : senderHost.trim();
        this.content = content == null ? "" : content;
        this.timestamp = Instant.now().toEpochMilli();
        this.status = MessageStatus.SENT;
        this.fromCurrentUser = fromCurrentUser;
    }

    /**
     * Tạo message chat 1-1 có đầy đủ sender, receiver và timestamp.
     */
    public static Message chat(PeerInfo sender, PeerInfo receiver, String content) {
        Message message = new Message();
        message.id = UUID.randomUUID().toString();
        message.type = MessageType.CHAT;
        message.senderId = sender.getId();
        message.senderHost = sender.getHost();
        message.senderPort = sender.getPort();
        message.receiverId = receiver.getId();
        message.receiverHost = receiver.getHost();
        message.receiverPort = receiver.getPort();
        message.content = content == null ? "" : content;
        message.timestamp = Instant.now().toEpochMilli();
        message.status = MessageStatus.SENDING;
        message.fromCurrentUser = true;
        return message;
    }

    /**
     * Tạo message chat nhóm gắn với groupId.
     */
    public static Message groupChat(PeerInfo sender, String groupId, String content) {
        return groupChat(sender, groupId, null, content);
    }

    /**
     * Tao message chat nhom co ten group de peer nhan co the tao group local khi bootstrap khong san sang.
     */
    public static Message groupChat(PeerInfo sender, String groupId, String groupName, String content) {
        Message message = new Message();
        message.id = UUID.randomUUID().toString();
        message.type = MessageType.GROUP_CHAT;
        message.senderId = sender.getId();
        message.senderHost = sender.getHost();
        message.senderPort = sender.getPort();
        message.groupId = groupId;
        message.groupName = groupName;
        message.content = content == null ? "" : content;
        message.timestamp = Instant.now().toEpochMilli();
        message.status = MessageStatus.SENDING;
        message.fromCurrentUser = true;
        return message;
    }

    /**
     * Tao message chat nhom cho mot receiver cu the de bootstrap co the luu offline theo user_id.
     */
    public static Message groupChat(PeerInfo sender, PeerInfo receiver, String groupId, String groupName, String content) {
        Message message = groupChat(sender, groupId, groupName, content);
        message.receiverId = receiver.getId();
        message.receiverHost = receiver.getHost();
        message.receiverPort = receiver.getPort();
        return message;
    }

    /**
     * Tao message chat nhom cho mot receiver cu the de bootstrap co the luu offline theo user_id.
     */
    public static Message groupChat(PeerInfo sender, PeerInfo receiver, String groupId, String content) {
        return groupChat(sender, receiver, groupId, null, content);
    }

    /**
     * Tạo ACK phản hồi cho message nguồn để bên gửi biết tin đã được nhận.
     */
    public static Message ack(Message source, PeerInfo sender) {
        Message message = new Message();
        message.id = source.getId();
        message.type = MessageType.ACK;
        message.senderId = sender.getId();
        message.senderHost = sender.getHost();
        message.senderPort = sender.getPort();
        message.receiverId = source.getSenderId();
        message.receiverHost = source.getSenderHost();
        message.receiverPort = source.getSenderPort();
        message.timestamp = Instant.now().toEpochMilli();
        message.status = MessageStatus.SENT;
        return message;
    }

    /**
     * Tạo heartbeat message dùng để kiểm tra peer còn online hay không.
     */
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

    /**
     * Tao request hoi peer dich danh sach peer ma no dang biet.
     */
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

    /**
     * Tao response tra danh sach peer da biet cho requester.
     */
    public static Message peerListResponse(PeerInfo sender, Message request, Collection<PeerInfo> peers) {
        Message message = new Message();
        message.id = request.getId();
        message.type = MessageType.PEER_LIST_RESPONSE;
        message.senderId = sender.getId();
        message.senderHost = sender.getHost();
        message.senderPort = sender.getPort();
        message.receiverId = request.getSenderId();
        message.receiverHost = request.getSenderHost();
        message.receiverPort = request.getSenderPort();
        message.timestamp = Instant.now().toEpochMilli();
        message.status = MessageStatus.SENT;
        message.peers = peers == null ? new ArrayList<>() : new ArrayList<>(peers);
        return message;
    }

    /**
     * Tao message dong bo membership cua group toi mot member dang online.
     */
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

    /**
     * Phục hồi message từ local JSON store để UI hiển thị lại lịch sử chat.
     */
    public static Message restore(String id, MessageType type, String senderId, String senderHost, int senderPort,
                                  String receiverId, String receiverHost, int receiverPort, String groupId,
                                  String content, long timestamp, boolean fromCurrentUser) {
        return restore(id, type, senderId, senderHost, senderPort, receiverId, receiverHost, receiverPort,
                groupId, null, content, timestamp, fromCurrentUser);
    }

    /**
     * Phuc hoi message tu local JSON, bao gom metadata group neu co.
     */
    public static Message restore(String id, MessageType type, String senderId, String senderHost, int senderPort,
                                  String receiverId, String receiverHost, int receiverPort, String groupId,
                                  String groupName, String content, long timestamp, boolean fromCurrentUser) {
        Message message = new Message();
        message.id = id;
        message.type = type;
        message.senderId = senderId;
        message.senderHost = senderHost;
        message.senderPort = senderPort;
        message.receiverId = receiverId;
        message.receiverHost = receiverHost;
        message.receiverPort = receiverPort;
        message.groupId = groupId;
        message.groupName = groupName;
        message.content = content == null ? "" : content;
        message.timestamp = timestamp;
        message.status = MessageStatus.SENT;
        message.fromCurrentUser = fromCurrentUser;
        return message;
    }

    /**
     * Phuc hoi message tu local JSON voi trang thai gui da luu.
     */
    public static Message restore(String id, MessageType type, String senderId, String senderHost, int senderPort,
                                  String receiverId, String receiverHost, int receiverPort, String groupId,
                                  String groupName, String content, long timestamp, boolean fromCurrentUser,
                                  MessageStatus status) {
        Message message = restore(id, type, senderId, senderHost, senderPort, receiverId, receiverHost,
                receiverPort, groupId, groupName, content, timestamp, fromCurrentUser);
        message.status = status == null ? MessageStatus.SENT : status;
        return message;
    }

    public String getId() {
        return id;
    }

    public MessageType getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderHost() {
        return senderHost;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public String getSenderIp() {
        if (senderPort > 0) {
            return senderHost + ":" + senderPort;
        }
        return senderHost;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getReceiverHost() {
        return receiverHost;
    }

    public int getReceiverPort() {
        return receiverPort;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isFromCurrentUser() {
        return fromCurrentUser;
    }

    public void setFromCurrentUser(boolean fromCurrentUser) {
        this.fromCurrentUser = fromCurrentUser;
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
