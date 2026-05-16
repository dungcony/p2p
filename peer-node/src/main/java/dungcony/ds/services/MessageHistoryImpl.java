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
        if (peerKey == null || peerKey.isBlank() || message == null) {
            return;
        }
        List<Message> messages = messageHistory.computeIfAbsent(peerKey,
                ignored -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (messages) {
            if (messages.stream().noneMatch(existingMessage -> existingMessage.getId().equals(message.getId()))) {
                messages.add(message);
            }
            messages.sort(java.util.Comparator.comparingLong(Message::getTimestamp));
        }
        System.out.println("[DEBUG] Đã thêm message vào lịch sử. peerKey=" + peerKey
                + ", messageId=" + message.getId());
    }

    /**
     * Lay history voi peer, lazy load tu messages.json khi can.
     */
    @Override
    public List<Message> getMessages(PeerInfo peerInfo, String fallbackKey) {
        String key = peerInfo == null ? fallbackKey : peerInfo.addressKey();
        if (peerInfo != null) {
            List<Message> localMessages = localMessageRepo.findByConversationPeerId(peerInfo.getId());
            if (!localMessages.isEmpty()) {
                mergeLocalMessages(key, localMessages);
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

    /**
     * Lay cac peer da tung co tin nhan 1-1 trong messages.json.
     */
    @Override
    public List<PeerInfo> getDirectConversationPeers() {
        return localMessageRepo.findDirectConversationPeers();
    }

    /**
     * Merge messages.json vao cache runtime de UI khong mat tin cu khi cache da co tin moi.
     */
    private void mergeLocalMessages(String key, List<Message> localMessages) {
        List<Message> cachedMessages = messageHistory.computeIfAbsent(key,
                ignored -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (cachedMessages) {
            for (Message localMessage : localMessages) {
                boolean exists = cachedMessages.stream()
                        .anyMatch(existingMessage -> existingMessage.getId().equals(localMessage.getId()));
                if (!exists) {
                    cachedMessages.add(localMessage);
                }
            }
            cachedMessages.sort(java.util.Comparator.comparingLong(Message::getTimestamp));
        }
        System.out.println("[DEBUG] Đã merge tin nhắn local vào cache runtime. peerKey=" + key
                + ", localCount=" + localMessages.size()
                + ", cachedCount=" + cachedMessages.size());
    }
}
