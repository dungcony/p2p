package dungcony.ds.model;

import dungcony.ds.entities.PeerInfo;
import dungcony.ds.enums.MessageType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private String content;
    private long timestamp;
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
        message.fromCurrentUser = true;
        return message;
    }

    /**
     * Tạo message chat nhóm gắn với groupId.
     */
    public static Message groupChat(PeerInfo sender, String groupId, String content) {
        Message message = new Message();
        message.id = UUID.randomUUID().toString();
        message.type = MessageType.GROUP_CHAT;
        message.senderId = sender.getId();
        message.senderHost = sender.getHost();
        message.senderPort = sender.getPort();
        message.groupId = groupId;
        message.content = content == null ? "" : content;
        message.timestamp = Instant.now().toEpochMilli();
        message.fromCurrentUser = true;
        return message;
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
        return message;
    }

    /**
     * Phục hồi message từ local JSON store để UI hiển thị lại lịch sử chat.
     */
    public static Message restore(String id, MessageType type, String senderId, String senderHost, int senderPort,
                                  String receiverId, String receiverHost, int receiverPort, String groupId,
                                  String content, long timestamp, boolean fromCurrentUser) {
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
        message.content = content == null ? "" : content;
        message.timestamp = timestamp;
        message.fromCurrentUser = fromCurrentUser;
        return message;
    }

    /**
     * Lấy id duy nhất của message, dùng để so khớp với ACK.
     */
    public String getId() {
        return id;
    }

    /**
     * Lấy loại message như CHAT, GROUP_CHAT, ACK hoặc HEARTBEAT.
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Lấy id của peer gửi.
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * Lấy host/IP của peer gửi.
     */
    public String getSenderHost() {
        return senderHost;
    }

    /**
     * Lấy port của peer gửi.
     */
    public int getSenderPort() {
        return senderPort;
    }

    /**
     * Lấy địa chỉ peer gửi theo dạng host:port để khớp với key trong UI/history.
     */
    public String getSenderIp() {
        if (senderPort > 0) {
            return senderHost + ":" + senderPort;
        }
        return senderHost;
    }

    /**
     * Lấy id của peer nhận.
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * Lấy host/IP của peer nhận.
     */
    public String getReceiverHost() {
        return receiverHost;
    }

    /**
     * Lấy port của peer nhận.
     */
    public int getReceiverPort() {
        return receiverPort;
    }

    /**
     * Lấy id nhóm nếu message là GROUP_CHAT.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Lấy nội dung text của tin nhắn.
     */
    public String getContent() {
        return content;
    }

    /**
     * Lấy timestamp dạng epoch milliseconds.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Cho biết message đang hiển thị ở phía người gửi hiện tại hay peer khác.
     */
    public boolean isFromCurrentUser() {
        return fromCurrentUser;
    }

    /**
     * Đặt hướng hiển thị của message trong UI sau khi deserialize từ network.
     */
    public void setFromCurrentUser(boolean fromCurrentUser) {
        this.fromCurrentUser = fromCurrentUser;
    }

    /**
     * Format timestamp thành giờ/phút để hiển thị trong bong bóng chat.
     */
    public String getFormattedTime() {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}
