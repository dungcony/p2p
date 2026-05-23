package dungcony.ds.interfaces;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.List;

// Đại diện contract đọc ghi lịch sử message của peer-node
public interface MessageHistoryService {

    // Lưu message vào memory và messages.json theo peer đối thoại
    void addAndSave(PeerInfo conversationPeer, Message message);

    // Lưu message vào memory với key tùy biến và persist theo peer đối thoại
    void addAndSave(String historyKey, PeerInfo conversationPeer, Message message);

    // Lưu message vào history runtime
    void add(String peerKey, Message message);

    // Lưu lại message đã thay đổi trạng thái vào history và JSON
    void updateAndSave(PeerInfo conversationPeer, Message message);

    // Lấy history với peer, lazy load từ messages.json khi cần
    List<Message> getMessages(PeerInfo peerInfo, String fallbackKey);

    // Lấy message cuối cùng với peer
    Message getLastMessage(PeerInfo peerInfo, String fallbackKey);

    // Lấy các peer đã từng có tin nhắn 1-1 trong messages.json
    List<PeerInfo> getDirectConversationPeers();
}
