package dungcony.ds.repositories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dungcony.ds.dtos.MesRecord;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.persistence.MessageRepository;
import dungcony.ds.utils.BroadcastConversation;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Repository JSON local lưu lịch sử message theo conversation
 */
@Slf4j
public class LocalMessageRepo implements MessageRepository {
    private static final Type MESSAGE_RECORD_LIST_TYPE = new TypeToken<List<MesRecord>>() {
    }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path messageFilePath;

    // Khởi tạo local JSON store trong dataDir riêng của instance hiện tại
    public LocalMessageRepo(Path dataDir) {
        Path resolvedDataDir = dataDir == null
                ? Path.of("peer-node", "src", "main", "resources", "data")
                : dataDir.normalize();
        this.messageFilePath = resolvedDataDir.resolve("messages.json");
        initializeStorage();
    }

    // Tạo folder/file JSON nếu local history chua tồn tại
    private void initializeStorage() {
        try {
            Files.createDirectories(messageFilePath.getParent());
            if (!Files.exists(messageFilePath)) {
                Files.writeString(messageFilePath, "[]", StandardCharsets.UTF_8);
            }
            log.info("Kho JSON tin nhắn local đã sẵn sàng. path={}", messageFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Không thể khởi tạo kho JSON tin nhắn local: {}", e.getMessage());
        }
    }

    // Lưu một message vào history local theo peer đối thoại
    @Override
    public synchronized void save(PeerInfo conversationPeer, Message message) {
        if (conversationPeer == null || message == null) {
            log.warn("LocalMessageRepo bỏ qua lưu conversation/message null.");
            return;
        }

        List<MesRecord> records = readAllRecords();
        MesRecord newRecord = MesRecord.from(conversationPeer, message);
        Optional<MesRecord> existingRecord = records.stream()
                .filter(record -> newRecord.messageId().equals(record.messageId()))
                .findFirst();

        if (existingRecord.isPresent()) {
            records.set(records.indexOf(existingRecord.get()), newRecord);
            log.debug("Đã cập nhật tin nhắn trong JSON local. messageId={}", message.getId());
        } else {
            records.add(newRecord);
            log.debug("Đã thêm tin nhắn vào JSON local. messageId={}", message.getId());
        }

        records.sort(Comparator.comparingLong(MesRecord::timestamp));
        writeAllRecords(records);
        log.info("Đã lưu tin nhắn local. conversationPeerId={}, messageId={}", conversationPeer.getId(), message.getId());
    }

    // Đọc history local với một peer theo peer.id ổn định
    @Override
    public synchronized List<Message> findByConversationPeerId(String conversationPeerId) {
        List<Message> messages = new ArrayList<>();
        if (conversationPeerId == null || conversationPeerId.isBlank()) {
            return messages;
        }

        for (MesRecord record : readAllRecords()) {
            if (!conversationPeerId.equals(record.conversationPeerId())) {
                continue;
            }
            if (BroadcastConversation.ID.equals(conversationPeerId)) {
                if (!"BROADCAST".equals(record.messageType())) {
                    continue;
                }
            } else if ("BROADCAST".equals(record.messageType())) {
                continue;
            }
            try {
                messages.add(record.toMessage());
            } catch (IllegalArgumentException e) {
                log.warn("Đã bỏ qua record tin nhắn local không hợp lệ. messageId={}, lỗi={}", record.messageId(), e.getMessage());
            }
        }

        messages.sort(Comparator.comparingLong(Message::getTimestamp));
        log.debug("Đã nạp tin nhắn local. conversationPeerId={}, sốLượng={}", conversationPeerId, messages.size());
        return messages;
    }

    // Đọc danh sách peer đã từng có tin nhắn 1-1 để hiện lại conversation khi peer offline
    @Override
    public synchronized List<PeerInfo> findDirectConversationPeers() {
        Map<String, PeerInfo> peersById = new LinkedHashMap<>();
        for (MesRecord record : readAllRecords()) {
            if (record.conversationPeerId() == null || record.conversationPeerId().isBlank()) {
                continue;
            }
            if (record.groupId() != null && !record.groupId().isBlank()) {
                continue;
            }
            if (!"CHAT".equals(record.messageType())) {
                continue;
            }
            PeerInfo peerInfo = toPeerInfo(record);
            if (peerInfo != null) {
                peersById.put(peerInfo.getId(), peerInfo);
            }
        }
        log.debug("Đã nạp danh sách conversation trực tiếp local. sốLượng={}", peersById.size());
        return new ArrayList<>(peersById.values());
    }

    // Đọc toàn bộ record từ file JSON local
    private List<MesRecord> readAllRecords() {
        try {
            if (!Files.exists(messageFilePath)) {
                return new ArrayList<>();
            }
            String json = Files.readString(messageFilePath, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return new ArrayList<>();
            }
            List<MesRecord> records = gson.fromJson(json, MESSAGE_RECORD_LIST_TYPE);
            return records == null ? new ArrayList<>() : new ArrayList<>(records);
        } catch (IOException | RuntimeException e) {
            log.error("Không thể đọc JSON tin nhắn local: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Chuyển metadata conversation trong JSON thành PeerInfo để UI có thể hiển thị history offline
    private PeerInfo toPeerInfo(MesRecord record) {
        String peerKey = record.conversationPeerKey();
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
                    record.conversationPeerId(),
                    record.conversationPeerName(),
                    host,
                    port,
                    false
            );
        } catch (NumberFormatException e) {
            log.warn("Đã bỏ qua khóa conversation peer không hợp lệ: {}", peerKey);
            return null;
        }
    }

    // Ghi toàn bộ record vào file JSON local
    private void writeAllRecords(List<MesRecord> records) {
        try {
            Files.writeString(messageFilePath, gson.toJson(records), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Không thể ghi JSON tin nhắn local: {}", e.getMessage());
        }
    }

}
