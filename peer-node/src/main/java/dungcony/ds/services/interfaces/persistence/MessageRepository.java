package dungcony.ds.services.interfaces.persistence;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.List;

public interface MessageRepository {
    void save(PeerInfo conversationPeer, Message message);

    List<Message> findByConversationPeerId(String conversationPeerId);

    List<PeerInfo> findDirectConversationPeers();
}
