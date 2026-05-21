package dungcony.ds.peer;

import dungcony.ds.model.Message;
import dungcony.ds.enums.MessageType;

public class MessageReceiver {
    private final PeerNode peerNode;

    /**
     * Khởi tạo receiver gắn với PeerNode để cập nhật trạng thái và lịch sử khi nhận tin.
     */
    public MessageReceiver(PeerNode peerNode) {
        this.peerNode = peerNode;
    }

    /**
     * Phân loại message đến, cập nhật PeerNode tương ứng và trả ACK cho bên gửi.
     */
    public Message receive(Message message) {
        if (message == null) {
            System.out.println("[WARN] Nhận message null. Sẽ không trả ACK.");
            return null;
        }

        System.out.println("[INFO] Đã nhận " + message.getType() + " message id=" + message.getId()
                + " từ=" + message.getSenderIp());
        if (message.getType() == MessageType.HEARTBEAT) {
            peerNode.markPeerOnline(message);
            System.out.println("[DEBUG] Trả ACK cho HEARTBEAT id=" + message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.PEER_LIST_REQUEST) {
            peerNode.markPeerOnline(message);
            System.out.println("[DEBUG] Trả PEER_LIST_RESPONSE cho request id=" + message.getId());
            return peerNode.buildPeerListResponse(message);
        }

        if (message.getType() == MessageType.PEER_LIST_RESPONSE) {
            peerNode.onPeerListResponse(message);
            System.out.println("[DEBUG] Trả ACK cho PEER_LIST_RESPONSE id=" + message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.GROUP_MEMBERS_SYNC) {
            peerNode.onGroupMembersSync(message);
            System.out.println("[DEBUG] Trả ACK cho GROUP_MEMBERS_SYNC id=" + message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.CHAT
                || message.getType() == MessageType.GROUP_CHAT
                || message.getType() == MessageType.BROADCAST) {
            message.setFromCurrentUser(false);
            peerNode.onInboundMessage(message);
            System.out.println("[DEBUG] Trả ACK cho tin nhắn chat id=" + message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        if (message.getType() == MessageType.JOIN) {
            peerNode.markPeerOnline(message);
            System.out.println("[DEBUG] Trả ACK cho JOIN id=" + message.getId());
            return Message.ack(message, peerNode.getLocalPeer());
        }

        System.out.println("[DEBUG] Trả ACK mặc định cho message id=" + message.getId());
        return Message.ack(message, peerNode.getLocalPeer());
    }
}
