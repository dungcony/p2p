package dungcony.ds.repositories;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dungcony.ds.dtos.MessageRecord;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LocalMessageRepo {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalMessageRepo.class);
private static final Type MESSAGE_RECORD_LIST_TYPE = new TypeToken<List<MessageRecord>>() {
    }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path messageFilePath;

    // Khởi tạo local JSON store riêng cho peerId hiện tại.
    public LocalMessageRepo(String peerId) {
        this(Path.of("peer-node", "src", "main", "resources", "data"));
    }

    // Khởi tạo local JSON store trong dataDir riêng của instance hiện tại.
    public LocalMessageRepo(Path dataDir) {
        Path resolvedDataDir = dataDir == null
                ? Path.of("peer-node", "src", "main", "resources", "data")
                : dataDir.normalize();
        this.messageFilePath = resolvedDataDir.resolve("messages.json");
        initializeStorage();
    }

    // Tạo folder/file JSON nếu local history chua tồn tại.
    private void initializeStorage() {
        try {
            Files.createDirectories(messageFilePath.getParent());
            if (!Files.exists(messageFilePath)) {
                Files.writeString(messageFilePath, "[]", StandardCharsets.UTF_8);
            }
            LOGGER.info("Kho JSON tin nhắn local đã sẵn sàng. path=" + messageFilePath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Không thể khởi tạo kho JSON tin nhắn local: " + e.getMessage());
        }
    }

    // Lưu một message vào history local theo peer đối thoại.
    public synchronized void save(PeerInfo conversationPeer, Message message) {
        if (conversationPeer == null || message == null) {
            LOGGER.warn("LocalMessageRepo bỏ qua lưu conversation/message null.");
            return;
        }

        List<MessageRecord> records = readAllRecords();
        MessageRecord newRecord = MessageRecord.from(conversationPeer, message);
        Optional<MessageRecord> existingRecord = records.stream()
                .filter(record -> newRecord.getMessageId().equals(record.getMessageId()))
                .findFirst();

        if (existingRecord.isPresent()) {
            records.set(records.indexOf(existingRecord.get()), newRecord);
            LOGGER.debug("Đã cập nhật tin nhắn trong JSON local. messageId=" + message.getId());
        } else {
            records.add(newRecord);
            LOGGER.debug("Đã thêm tin nhắn vào JSON local. messageId=" + message.getId());
        }

        records.sort(Comparator.comparingLong(MessageRecord::getTimestamp));
        writeAllRecords(records);
        LOGGER.info("Đã lưu tin nhắn local. conversationPeerId=" + conversationPeer.getId()
                + ", messageId=" + message.getId());
    }

    // Đọc history local với một peer theo peer.id ổn định.
    public synchronized List<Message> findByConversationPeerId(String conversationPeerId) {
        List<Message> messages = new ArrayList<>();
        if (conversationPeerId == null || conversationPeerId.isBlank()) {
            return messages;
        }

        for (MessageRecord record : readAllRecords()) {
            if (!conversationPeerId.equals(record.getConversationPeerId())) {
                continue;
            }
            try {
                messages.add(record.toMessage());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Đã bỏ qua record tin nhắn local không hợp lệ. messageId="
                        + record.getMessageId() + ", lỗi=" + e.getMessage());
            }
        }

        messages.sort(Comparator.comparingLong(Message::getTimestamp));
        LOGGER.debug("Đã nạp tin nhắn local. conversationPeerId="
                + conversationPeerId + ", sốLượng=" + messages.size());
        return messages;
    }

    // Đọc danh sách peer đã từng có tin nhắn 1-1 để hiện lại conversation khi peer offline.
    public synchronized List<PeerInfo> findDirectConversationPeers() {
        Map<String, PeerInfo> peersById = new LinkedHashMap<>();
        for (MessageRecord record : readAllRecords()) {
            if (record.getConversationPeerId() == null || record.getConversationPeerId().isBlank()) {
                continue;
            }
            if (record.getGroupId() != null && !record.getGroupId().isBlank()) {
                continue;
            }
            if (!"CHAT".equals(record.getMessageType()) && !"BROADCAST".equals(record.getMessageType())) {
                continue;
            }
            PeerInfo peerInfo = toPeerInfo(record);
            if (peerInfo != null) {
                peersById.put(peerInfo.getId(), peerInfo);
            }
        }
        LOGGER.debug("Đã nạp danh sách conversation trực tiếp local. sốLượng=" + peersById.size());
        return new ArrayList<>(peersById.values());
    }

    // Đọc toàn bộ record từ file JSON local.
    private List<MessageRecord> readAllRecords() {
        try {
            if (!Files.exists(messageFilePath)) {
                return new ArrayList<>();
            }
            String json = Files.readString(messageFilePath, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            List<MessageRecord> records = gson.fromJson(json, MESSAGE_RECORD_LIST_TYPE);
            return records == null ? new ArrayList<>() : new ArrayList<>(records);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Không thể đọc JSON tin nhắn local: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Chuyển metadata conversation trong JSON thành PeerInfo để UI có thể hiển thị history offline.
    private PeerInfo toPeerInfo(MessageRecord record) {
        String peerKey = record.getConversationPeerKey();
        if (peerKey == null || peerKey.isBlank()) {
            return null;
        }
        int colonIndex = peerKey.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex >= peerKey.length() - 1) {
            return null;
        }
        try {
            String host = peerKey.substring(0, colonIndex);
            int port = Integer.parseInt(peerKey.substring(colonIndex + 1));
            return new PeerInfo(
                    record.getConversationPeerId(),
                    record.getConversationPeerName(),
                    host,
                    port,
                    false
            );
        } catch (NumberFormatException e) {
            LOGGER.warn("Đã bỏ qua khóa conversation peer không hợp lệ: " + peerKey);
            return null;
        }
    }

    // Ghi toàn bộ record vào file JSON local.
    private void writeAllRecords(List<MessageRecord> records) {
        try {
            Files.writeString(messageFilePath, gson.toJson(records), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Không thể ghi JSON tin nhắn local: " + e.getMessage());
        }
    }

}
