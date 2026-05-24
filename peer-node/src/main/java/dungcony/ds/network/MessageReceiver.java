package dungcony.ds.network;

import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerNode;
import lombok.extern.slf4j.Slf4j;

/**
 * Xử lý message nhận được từ TCP socket và điều phối tới PeerNode.
 *
 * <p>Luồng xử lý mỗi message đến:</p>
 * <pre>
 *   1. Guard: reject null message
 *   2. Route theo MessageType:
 *      HEARTBEAT          → markPeerOnline() → trả ACK
 *      PEER_LIST_REQUEST  → buildPeerListResponse() → trả PEER_LIST_RESPONSE
 *      PEER_LIST_RESPONSE → onPeerListResponse() → trả ACK
 *      GROUP_MEMBERS_SYNC → onGroupMembersSync() → trả ACK
 *      CHAT / GROUP_CHAT / BROADCAST → onInboundMessage() → trả ACK
 *      JOIN               → markPeerOnline() → trả ACK
 *      default            → trả ACK
 * </pre>
 */
@Slf4j
public class MessageReceiver {

    private final PeerNode peerNode;

    // Khởi tạo receiver gắn với PeerNode để cập nhật trạng thái và lịch sử khi nhận tin.
    public MessageReceiver(PeerNode peerNode) {
        this.peerNode = peerNode;
    }

    /**
     * Phân loại message đến, cập nhật PeerNode tương ứng và trả response cho bên gửi.
     *
     * @param message message nhận được từ TCP socket (có thể null)
     * @return ACK hoặc response tương ứng, null nếu message không hợp lệ
     */
    public Message receive(Message message) {

        // ── 1. Guard: reject null ──────────────────────────────────────────
        if (message == null) {
            log.warn("Nhận message null. Sẽ không trả ACK.");
            return null;
        }
        log.info("Đã nhận {} message id={} từ={}", message.getType(), message.getId(), message.getSenderIp());

        // ── 2. Route theo MessageType ──────────────────────────────────────

        // HEARTBEAT → cập nhật peer online → ACK
        if (message.getType() == MessageType.HEARTBEAT) {
            peerNode.markPeerOnline(message);
            log.debug("Trả ACK cho HEARTBEAT id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        // PEER_LIST_REQUEST → trả danh sách peer thay vì ACK
        if (message.getType() == MessageType.PEER_LIST_REQUEST) {
            peerNode.markPeerOnline(message);
            log.debug("Trả PEER_LIST_RESPONSE cho request id={}", message.getId());
            return peerNode.buildPeerListResponse(message);
        }

        // PEER_LIST_RESPONSE → merge peer vào danh bạ → ACK
        if (message.getType() == MessageType.PEER_LIST_RESPONSE) {
            peerNode.onPeerListResponse(message);
            log.debug("Trả ACK cho PEER_LIST_RESPONSE id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        // GROUP_MEMBERS_SYNC → đồng bộ membership nhóm → ACK
        if (message.getType() == MessageType.GROUP_MEMBERS_SYNC) {
            peerNode.onGroupMembersSync(message);
            log.debug("Trả ACK cho GROUP_MEMBERS_SYNC id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        // CHAT / GROUP_CHAT / BROADCAST → lưu history, notify UI → ACK
        if (message.getType() == MessageType.CHAT
                || message.getType() == MessageType.GROUP_CHAT
                || message.getType() == MessageType.BROADCAST) {
            message.setFromCurrentUser(false);
            peerNode.onInboundMessage(message);
            log.debug("Trả ACK cho tin nhắn chat id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        // JOIN → cập nhật peer online → ACK
        if (message.getType() == MessageType.JOIN) {
            peerNode.markPeerOnline(message);
            log.debug("Trả ACK cho JOIN id={}", message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        // default → ACK
        log.debug("Trả ACK mặc định cho message id={}", message.getId());
        return Message.ack(message, peerNode.getLocalPeer());
    }
}
