package dungcony.ds.interfaces;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.List;

public interface MessageHistoryService {

    /**
     * Luu message vao memory va messages.json theo peer doi thoai.
     */
    void addAndSave(PeerInfo conversationPeer, Message message);
    
    /**
     * Luu message vao memory voi key tuy bien va persist theo peer doi thoai.
     */
    void addAndSave(String historyKey, PeerInfo conversationPeer, Message message);

    /**
     * Luu message vao history runtime.
     */
    void add(String peerKey, Message message);


    /**
     * Lay history voi peer, lazy load tu messages.json khi can.
     */
    List<Message> getMessages(PeerInfo peerInfo, String fallbackKey);

    /**
     * Lay message cuoi cung voi peer.
     */
    Message getLastMessage(PeerInfo peerInfo, String fallbackKey);

    /**
     * Lay cac peer da tung co tin nhan 1-1 trong messages.json.
     */
    List<PeerInfo> getDirectConversationPeers();
}
