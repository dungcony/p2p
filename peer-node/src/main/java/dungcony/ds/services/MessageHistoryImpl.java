package dungcony.ds.services;

import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.repositories.LocalMessageRepo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageHistoryImpl implements MessageHistoryService {
    private final Map<String, List<Message>> messageHistory = new ConcurrentHashMap<>();
    private final LocalMessageRepo localMessageRepo;

    /**
     * Khoi tao service quan ly message history runtime va JSON local.
     */
    public MessageHistoryImpl(LocalMessageRepo localMessageRepo) {
        this.localMessageRepo = localMessageRepo;
    }

    /**
     * Luu message vao memory va messages.json theo peer doi thoai.
     */
    @Override
    public void addAndSave(PeerInfo conversationPeer, Message message) {
        add(conversationPeer.addressKey(), message);
        localMessageRepo.save(conversationPeer, message);
    }

    /**
     * Luu message vao memory voi key tuy bien va persist theo peer doi thoai.
     */
    @Override
    public void addAndSave(String historyKey, PeerInfo conversationPeer, Message message) {
        add(historyKey, message);
        localMessageRepo.save(conversationPeer, message);
    }

    /**
     * Luu message vao history runtime.
     */
    @Override
    public void add(String peerKey, Message message) {
        messageHistory.computeIfAbsent(peerKey, ignored -> Collections.synchronizedList(new ArrayList<>())).add(message);
        System.out.println("[DEBUG] Message appended to history. peerKey=" + peerKey
                + ", messageId=" + message.getId());
    }

    /**
     * Lay history voi peer, lazy load tu messages.json khi can.
     */
    @Override
    public List<Message> getMessages(PeerInfo peerInfo, String fallbackKey) {
        String key = peerInfo == null ? fallbackKey : peerInfo.addressKey();
        if (peerInfo != null && !messageHistory.containsKey(key)) {
            List<Message> localMessages = localMessageRepo.findByConversationPeerId(peerInfo.getId());
            if (!localMessages.isEmpty()) {
                messageHistory.put(key, Collections.synchronizedList(new ArrayList<>(localMessages)));
            }
        }
        return new ArrayList<>(messageHistory.getOrDefault(key, Collections.emptyList()));
    }

    /**
     * Lay message cuoi cung voi peer.
     */
    @Override
    public Message getLastMessage(PeerInfo peerInfo, String fallbackKey) {
        List<Message> messages = getMessages(peerInfo, fallbackKey);
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
}
