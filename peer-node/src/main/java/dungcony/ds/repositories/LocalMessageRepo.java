package dungcony.ds.repositories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dungcony.ds.model.Message;
import dungcony.ds.enums.MessageType;
import dungcony.ds.entities.PeerInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LocalMessageRepo {
    private static final Type MESSAGE_RECORD_LIST_TYPE = new TypeToken<List<MessageRecord>>() {
    }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path messageFilePath;

    /**
     * Khoi tao local JSON store rieng cho peerId hien tai.
     */
    public LocalMessageRepo(String peerId) {
        String safePeerId = peerId == null || peerId.isBlank()
                ? "peer-local"
                : peerId.replaceAll("[^a-zA-Z0-9._-]", "_");
        this.messageFilePath = Path.of("peer-node", "src", "main", "resources", "data", "messages", safePeerId + ".json");
        initializeStorage();
    }

    /**
     * Tao folder/file JSON neu local history chua ton tai.
     */
    private void initializeStorage() {
        try {
            Files.createDirectories(messageFilePath.getParent());
            if (!Files.exists(messageFilePath)) {
                Files.writeString(messageFilePath, "[]", StandardCharsets.UTF_8);
            }
            System.out.println("[INFO] Local message JSON store ready. path=" + messageFilePath.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to initialize local message JSON store: " + e.getMessage());
        }
    }

    /**
     * Luu mot message vao history local theo peer doi thoai.
     */
    public synchronized void save(PeerInfo conversationPeer, Message message) {
        if (conversationPeer == null || message == null) {
            System.out.println("[WARN] LocalMessageRepo save ignored null conversation/message.");
            return;
        }

        List<MessageRecord> records = readAllRecords();
        MessageRecord newRecord = MessageRecord.from(conversationPeer, message);
        Optional<MessageRecord> existingRecord = records.stream()
                .filter(record -> newRecord.messageId.equals(record.messageId))
                .findFirst();

        if (existingRecord.isPresent()) {
            records.set(records.indexOf(existingRecord.get()), newRecord);
            System.out.println("[DEBUG] Local JSON message updated. messageId=" + message.getId());
        } else {
            records.add(newRecord);
            System.out.println("[DEBUG] Local JSON message appended. messageId=" + message.getId());
        }

        records.sort(Comparator.comparingLong(record -> record.timestamp));
        writeAllRecords(records);
        System.out.println("[INFO] Local message saved. conversationPeerId=" + conversationPeer.getId()
                + ", messageId=" + message.getId());
    }

    /**
     * Doc history local voi mot peer theo peer.id on dinh.
     */
    public synchronized List<Message> findByConversationPeerId(String conversationPeerId) {
        List<Message> messages = new ArrayList<>();
        if (conversationPeerId == null || conversationPeerId.isBlank()) {
            return messages;
        }

        for (MessageRecord record : readAllRecords()) {
            if (!conversationPeerId.equals(record.conversationPeerId)) {
                continue;
            }
            try {
                messages.add(record.toMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("[WARN] Ignored invalid local message record. messageId="
                        + record.messageId + ", error=" + e.getMessage());
            }
        }

        messages.sort(Comparator.comparingLong(Message::getTimestamp));
        System.out.println("[DEBUG] Local messages loaded. conversationPeerId="
                + conversationPeerId + ", count=" + messages.size());
        return messages;
    }

    /**
     * Doc toan bo record tu file JSON local.
     */
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
            System.out.println("[ERROR] Failed to read local message JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Ghi toan bo record vao file JSON local.
     */
    private void writeAllRecords(List<MessageRecord> records) {
        try {
            Files.writeString(messageFilePath, gson.toJson(records), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to write local message JSON: " + e.getMessage());
        }
    }

    private static class MessageRecord {
        private String messageId;
        private String conversationPeerId;
        private String conversationPeerName;
        private String conversationPeerKey;
        private String senderId;
        private String senderHost;
        private int senderPort;
        private String receiverId;
        private String receiverHost;
        private int receiverPort;
        private String groupId;
        private String messageType;
        private String content;
        private long timestamp;
        private boolean fromCurrentUser;

        /**
         * Chuyen Message runtime thanh record phang de ghi JSON.
         */
        private static MessageRecord from(PeerInfo conversationPeer, Message message) {
            MessageRecord record = new MessageRecord();
            record.messageId = message.getId();
            record.conversationPeerId = conversationPeer.getId();
            record.conversationPeerName = conversationPeer.getName();
            record.conversationPeerKey = conversationPeer.addressKey();
            record.senderId = message.getSenderId();
            record.senderHost = message.getSenderHost();
            record.senderPort = message.getSenderPort();
            record.receiverId = message.getReceiverId();
            record.receiverHost = message.getReceiverHost();
            record.receiverPort = message.getReceiverPort();
            record.groupId = message.getGroupId();
            record.messageType = message.getType().name();
            record.content = message.getContent();
            record.timestamp = message.getTimestamp();
            record.fromCurrentUser = message.isFromCurrentUser();
            return record;
        }

        /**
         * Phuc hoi Message runtime tu record JSON.
         */
        private Message toMessage() {
            return Message.restore(
                    messageId,
                    MessageType.valueOf(messageType),
                    senderId,
                    senderHost,
                    senderPort,
                    receiverId,
                    receiverHost,
                    receiverPort,
                    groupId,
                    content,
                    timestamp,
                    fromCurrentUser
            );
        }
    }
}
