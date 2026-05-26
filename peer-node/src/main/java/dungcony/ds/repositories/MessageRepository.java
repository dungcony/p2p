package dungcony.ds.repositories;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.List;

/**
 * Repository lưu và đọc lịch sử message local
 */
public interface MessageRepository {
    void save(PeerInfo conversationPeer, Message message);

    List<Message> findByConversationPeerId(String conversationPeerId);

    List<PeerInfo> findDirectConversationPeers();
}
