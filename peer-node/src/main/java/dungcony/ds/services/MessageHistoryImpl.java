package dungcony.ds.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHistoryImpl.class);
private final Map<String, List<Message>> messageHistory = new ConcurrentHashMap<>();
    private final LocalMessageRepo localMessageRepo;

    // Khởi tạo service quản lý message history runtime và JSON local.
    public MessageHistoryImpl(LocalMessageRepo localMessageRepo) {
        this.localMessageRepo = localMessageRepo;
    }

    // Lưu message vào memory và messages.json theo peer đối thoại.
    @Override
    public void addAndSave(PeerInfo conversationPeer, Message message) {
        add(conversationPeer.addressKey(), message);
        localMessageRepo.save(conversationPeer, message);
    }

    // Lưu message vào memory với key tùy biến và persist theo peer đối thoại.
    @Override
    public void addAndSave(String historyKey, PeerInfo conversationPeer, Message message) {
        add(historyKey, message);
        localMessageRepo.save(conversationPeer, message);
    }

    // Lưu message vào history runtime.
    @Override
    public void add(String peerKey, Message message) {
        if (peerKey == null || peerKey.isBlank() || message == null) {
            return;
        }
        List<Message> messages = messageHistory.computeIfAbsent(peerKey,
                ignored -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (messages) {
            int existingIndex = findMessageIndex(messages, message.getId());
            if (existingIndex >= 0) {
                messages.set(existingIndex, message);
            } else {
                messages.add(message);
            }
            messages.sort(java.util.Comparator.comparingLong(Message::getTimestamp));
        }
        LOGGER.debug("Đã cập nhật message trong lịch sử. peerKey=" + peerKey
                + ", messageId=" + message.getId() + ", status=" + message.getStatus());
    }

    // Lưu lại message đã thay đổi trạng thái vào cache runtime và JSON local.
    @Override
    public void updateAndSave(PeerInfo conversationPeer, Message message) {
        addAndSave(conversationPeer, message);
    }

    // Lấy history với peer, lazy load từ messages.json khi cần.
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

    // Lấy message cuối cùng với peer.
    @Override
    public Message getLastMessage(PeerInfo peerInfo, String fallbackKey) {
        List<Message> messages = getMessages(peerInfo, fallbackKey);
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    // Lấy các peer đã từng có tin nhắn 1-1 trong messages.json.
    @Override
    public List<PeerInfo> getDirectConversationPeers() {
        return localMessageRepo.findDirectConversationPeers();
    }

    // Merge messages.json vào cache runtime để UI không mất tin cũ khi cache đã có tin mới.
    private void mergeLocalMessages(String key, List<Message> localMessages) {
        List<Message> cachedMessages = messageHistory.computeIfAbsent(key,
                ignored -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (cachedMessages) {
            for (Message localMessage : localMessages) {
                int existingIndex = findMessageIndex(cachedMessages, localMessage.getId());
                if (existingIndex >= 0) {
                    cachedMessages.set(existingIndex, localMessage);
                } else {
                    cachedMessages.add(localMessage);
                }
            }
            cachedMessages.sort(java.util.Comparator.comparingLong(Message::getTimestamp));
        }
        LOGGER.debug("Đã merge tin nhắn local vào cache runtime. peerKey=" + key
                + ", localCount=" + localMessages.size()
                + ", cachedCount=" + cachedMessages.size());
    }

    private int findMessageIndex(List<Message> messages, String messageId) {
        if (messageId == null) {
            return -1;
        }
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
}
