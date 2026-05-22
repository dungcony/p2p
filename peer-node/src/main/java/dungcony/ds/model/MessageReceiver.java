package dungcony.ds.model;

import dungcony.ds.enums.MessageType;
import lombok.extern.slf4j.Slf4j;

// Xử lý message nhận được.
@Slf4j
public class MessageReceiver {
    private final PeerNode peerNode;

    // Khởi tạo receiver gắn với PeerNode để cập nhật trạng thái và lịch sử khi nhận tin.
    public MessageReceiver(PeerNode peerNode) {
        this.peerNode = peerNode;
    }

    // Phân loại message đến, cập nhật PeerNode tương ứng và trả ACK cho bên gửi.
    public Message receive(Message message) {
        if (message == null) {
            log.warn("Nhận message null. Sẽ không trả ACK.");
            return null;
        }

        log.info("Đã nhận {} message id={} từ={}", message.getType(), message.getId(), message.getSenderIp());
        if (message.getType() == MessageType.HEARTBEAT) {
            peerNode.markPeerOnline(message);
            log.debug("Trả ACK cho HEARTBEAT id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.PEER_LIST_REQUEST) {
            peerNode.markPeerOnline(message);
            log.debug("Trả PEER_LIST_RESPONSE cho request id={}", message.getId());
            return peerNode.buildPeerListResponse(message);
        }

        if (message.getType() == MessageType.PEER_LIST_RESPONSE) {
            peerNode.onPeerListResponse(message);
            log.debug("Trả ACK cho PEER_LIST_RESPONSE id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.GROUP_MEMBERS_SYNC) {
            peerNode.onGroupMembersSync(message);
            log.debug("Trả ACK cho GROUP_MEMBERS_SYNC id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.CHAT
                || message.getType() == MessageType.GROUP_CHAT
                || message.getType() == MessageType.BROADCAST) {
            message.setFromCurrentUser(false);
            peerNode.onInboundMessage(message);
            log.debug("Trả ACK cho tin nhắn chat id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.JOIN) {
            peerNode.markPeerOnline(message);
            log.debug("Trả ACK cho JOIN id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        log.debug("Trả ACK mặc định cho message id={}", message.getId());
        return Message.ack(message, peerNode.getLocalPeer());
    }
}
