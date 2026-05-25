package dungcony.ds.services.impl.messaging;

import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.messaging.InboundMessageService;
import dungcony.ds.services.interfaces.messaging.MessageRouterService;
import dungcony.ds.services.interfaces.peer.PeerDiscoverService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Router phân loại message đến và gọi service xử lý phù hợp.
 *
 * <p>Tuân thủ OCP: thêm MessageType mới chỉ cần thêm entry vào map trong constructor,
 * không sửa logic của {@link #receive(Message)}.</p>
 */
@Slf4j
public class MessageRouterImpl implements MessageRouterService {

    private final PeerInfo localPeer;
    private final InboundMessageService inboundMessageService;
    private final PeerDiscoverService peerDiscoverService;
    private final Map<MessageType, UnaryOperator<Message>> handlers;

    public MessageRouterImpl(PeerInfo localPeer,
                             InboundMessageService inboundMessageService,
                             PeerDiscoverService peerDiscoverService) {
        this.localPeer = localPeer;
        this.inboundMessageService = inboundMessageService;
        this.peerDiscoverService = peerDiscoverService;
        this.handlers = buildHandlers();
    }

    @Override
    public Message receive(Message message) {
        if (message == null) {
            log.warn("Nhận message null. Sẽ không trả ACK.");
            return null;
        }
        log.info("Đã nhận {} message id={} từ={}", message.getType(), message.getId(), message.getSenderIp());
        UnaryOperator<Message> handler = handlers.getOrDefault(message.getType(), this::defaultAck);
        return handler.apply(message);
    }

    // Khởi tạo bảng ánh xạ MessageType → handler một lần duy nhất trong constructor
    private Map<MessageType, UnaryOperator<Message>> buildHandlers() {
        Map<MessageType, UnaryOperator<Message>> map = new EnumMap<>(MessageType.class);

        map.put(MessageType.HEARTBEAT, msg -> {
            inboundMessageService.markPeerOnline(msg);
            log.debug("Trả ACK cho HEARTBEAT id={}", msg.getId());
            return Message.ack(msg, localPeer);
        });

        map.put(MessageType.PEER_LIST_REQUEST, msg -> {
            inboundMessageService.markPeerOnline(msg);
            log.debug("Trả PEER_LIST_RESPONSE cho request id={}", msg.getId());
            return peerDiscoverService.buildPeerListResponse(msg);
        });

        map.put(MessageType.PEER_LIST_RESPONSE, msg -> {
            peerDiscoverService.onPeerListResponse(msg);
            log.debug("Trả ACK cho PEER_LIST_RESPONSE id={}", msg.getId());
            return Message.ack(msg, localPeer);
        });

        map.put(MessageType.GROUP_MEMBERS_SYNC, msg -> {
            inboundMessageService.onGroupMembersSync(msg);
            log.debug("Trả ACK cho GROUP_MEMBERS_SYNC id={}", msg.getId());
            return Message.ack(msg, localPeer);
        });

        map.put(MessageType.CHAT, this::handleInboundChat);
        map.put(MessageType.GROUP_CHAT, this::handleInboundChat);
        map.put(MessageType.BROADCAST, this::handleInboundChat);

        map.put(MessageType.JOIN, msg -> {
            inboundMessageService.markPeerOnline(msg);
            log.debug("Trả ACK cho JOIN id={}", msg.getId());
            return Message.ack(msg, localPeer);
        });

        return Collections.unmodifiableMap(map);
    }

    // Xử lý chung cho CHAT, GROUP_CHAT, BROADCAST
    private Message handleInboundChat(Message message) {
        message.setFromCurrentUser(false);
        inboundMessageService.onInboundMessage(message);
        log.debug("Trả ACK cho tin nhắn chat id={}", message.getId());
        return Message.ack(message, localPeer);
    }

    // Handler mặc định cho các MessageType chưa đăng ký
    private Message defaultAck(Message message) {
        log.debug("Trả ACK mặc định cho message id={}", message.getId());
        return Message.ack(message, localPeer);
    }
}
