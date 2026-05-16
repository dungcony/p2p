package dungcony.ds.services;

import dungcony.ds.interfaces.ChatService;
import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.mapper.Mes;
import dungcony.ds.model.BootstrapClient;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.peer.MessageSender;

import java.util.function.Consumer;

public class ChatImpl implements ChatService {
    private final PeerInfo localPeer;
    private final MessageSender messageSender;
    private final BootstrapClient bootstrapClient;
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final Consumer<Message> messageNotifier;
    private final Runnable peerChangeNotifier;

    /**
     * Khoi tao service xu ly heartbeat va gui chat 1-1.
     */
    public ChatImpl(PeerInfo localPeer, MessageSender messageSender, BootstrapClient bootstrapClient,
                    PeerDirectoryService peerDirectoryService, MessageHistoryService messageHistoryService,
                    Consumer<Message> messageNotifier, Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.bootstrapClient = bootstrapClient;
        this.peerDirectoryService = peerDirectoryService;
        this.messageHistoryService = messageHistoryService;
        this.messageNotifier = messageNotifier;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    /**
     * Gửi tin nhắn 1-1 trực tiếp tới peer đích, lưu lịch sử nếu gửi được hoặc store offline thành công.
     */
    @Override
    public boolean sendMessage(String content, String hostAndMaybePort) {
        if (content == null || content.isBlank()) {
            System.out.println("[WARN] Refusing to send blank message.");
            return false;
        }
        PeerInfo receiver = peerDirectoryService.resolvePeer(hostAndMaybePort);
        if (receiver == null) {
            System.out.println("[WARN] Refusing to send message because target peer is blank.");
            return false;
        }
        if (peerDirectoryService.isSelfPeer(receiver)) {
            System.out.println("[WARN] Refusing to send message to local peer: " + receiver.addressKey());
            return false;
        }

        Message message = Message.chat(localPeer, receiver, content);
        System.out.println("[INFO] Sending CHAT message id=" + message.getId()
                + " to=" + receiver.addressKey());
        boolean sent = messageSender.send(receiver, message);
        receiver.setOnline(sent);
        if (sent || storeOfflineIfPossible(message)) {
            messageHistoryService.addAndSave(receiver, message);
            messageNotifier.accept(message);
        }
        if (sent) {
            System.out.println("[INFO] CHAT message delivered and stored. id=" + message.getId());
        } else {
            System.out.println("[WARN] CHAT message failed after retries. id=" + message.getId()
                    + ", to=" + receiver.addressKey());
        }
        peerChangeNotifier.run();
        return sent;
    }

    /**
     * Luu tin offline len bootstrap-server de receiver nhan lai khi JOIN.
     */
    private boolean storeOfflineIfPossible(Message message) {
        if (bootstrapClient == null) {
            System.out.println("[WARN] Cannot store offline message because bootstrap is disabled. messageId="
                    + message.getId());
            return false;
        }
        boolean stored = bootstrapClient.storeOffline(Mes.fromMessage(message));
        System.out.println("[INFO] Offline fallback stored=" + stored + ", messageId=" + message.getId());
        return stored;
    }
}
