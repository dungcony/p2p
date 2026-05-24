package dungcony.ds.services.impl.messaging;

import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.messaging.InboundMessageService;
import dungcony.ds.services.interfaces.messaging.MessageRouterService;
import dungcony.ds.services.interfaces.peer.PeerDiscoverService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageRouterImpl implements MessageRouterService {
    private final PeerInfo localPeer;
    private final InboundMessageService inboundMessageService;
    private final PeerDiscoverService peerDiscoverService;

    public MessageRouterImpl(PeerInfo localPeer,
                             InboundMessageService inboundMessageService,
                             PeerDiscoverService peerDiscoverService) {
        this.localPeer = localPeer;
        this.inboundMessageService = inboundMessageService;
        this.peerDiscoverService = peerDiscoverService;
    }

    @Override
    public Message receive(Message message) {
        if (message == null) {
            log.warn("Nhận message null. Sẽ không trả ACK.");
            return null;
        }
        log.info("Đã nhận {} message id={} từ={}", message.getType(), message.getId(), message.getSenderIp());

        if (message.getType() == MessageType.HEARTBEAT) {
            inboundMessageService.markPeerOnline(message);
            log.debug("Trả ACK cho HEARTBEAT id={}", message.getId());
            return Message.ack(message, localPeer);
        }

        if (message.getType() == MessageType.PEER_LIST_REQUEST) {
            inboundMessageService.markPeerOnline(message);
            log.debug("Trả PEER_LIST_RESPONSE cho request id={}", message.getId());
            return peerDiscoverService.buildPeerListResponse(message);
        }

        if (message.getType() == MessageType.PEER_LIST_RESPONSE) {
            peerDiscoverService.onPeerListResponse(message);
            log.debug("Trả ACK cho PEER_LIST_RESPONSE id={}", message.getId());
            return Message.ack(message, localPeer);
        }

        if (message.getType() == MessageType.GROUP_MEMBERS_SYNC) {
            inboundMessageService.onGroupMembersSync(message);
            log.debug("Trả ACK cho GROUP_MEMBERS_SYNC id={}", message.getId());
            return Message.ack(message, localPeer);
        }

        if (message.getType() == MessageType.CHAT
                || message.getType() == MessageType.GROUP_CHAT
                || message.getType() == MessageType.BROADCAST) {
            message.setFromCurrentUser(false);
            inboundMessageService.onInboundMessage(message);
            log.debug("Trả ACK cho tin nhắn chat id={}", message.getId());
            return Message.ack(message, localPeer);
        }

        if (message.getType() == MessageType.JOIN) {
            inboundMessageService.markPeerOnline(message);
            log.debug("Trả ACK cho JOIN id={}", message.getId());
            return Message.ack(message, localPeer);
        }

        log.debug("Trả ACK mặc định cho message id={}", message.getId());
        return Message.ack(message, localPeer);
    }
}
