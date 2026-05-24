package dungcony.ds.network;

import dungcony.ds.model.Message;
import dungcony.ds.services.interfaces.messaging.MessageRouterService;

/**
 * Xử lý message nhận được từ TCP socket và điều phối tới router nghiệp vụ
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
public class MessageReceiver {

    private final MessageRouterService messageRouterService;

    // Khởi tạo receiver với router nghiệp vụ để network layer không phụ thuộc PeerNode
    public MessageReceiver(MessageRouterService messageRouterService) {
        this.messageRouterService = messageRouterService;
    }

    /**
     * Chuyển message đến cho router nghiệp vụ và trả response cho bên gửi
     *
     * @param message message nhận được từ TCP socket (có thể null)
     * @return ACK hoặc response tương ứng, null nếu message không hợp lệ
     */
    public Message receive(Message message) {
        return messageRouterService.receive(message);
    }
}
