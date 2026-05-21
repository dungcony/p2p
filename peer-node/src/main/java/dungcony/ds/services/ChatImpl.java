package dungcony.ds.services;

import dungcony.ds.enums.MessageStatus;
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
            System.out.println("[WARN] Từ chối gửi tin nhắn rỗng.");
            return false;
        }
        PeerInfo receiver = peerDirectoryService.resolvePeer(hostAndMaybePort);
        if (receiver == null) {
            System.out.println("[WARN] Từ chối gửi tin vì peer đích rỗng.");
            return false;
        }
        if (peerDirectoryService.isSelfPeer(receiver)) {
            System.out.println("[WARN] Từ chối gửi tin tới peer hiện tại: " + receiver.addressKey());
            return false;
        }

        Message message = Message.chat(localPeer, receiver, content);
        System.out.println("[INFO] Đang gửi tin nhắn CHAT id=" + message.getId()
                + " tới=" + receiver.addressKey());
        boolean sent = messageSender.send(receiver, message);
        receiver.setOnline(sent);
        if (sent) {
            message.setStatus(MessageStatus.SENT);
        } else if (storeOfflineIfPossible(message)) {
            message.setStatus(MessageStatus.PENDING);
        } else {
            message.setStatus(MessageStatus.FAILED);
        }
        messageHistoryService.addAndSave(receiver, message);
        messageNotifier.accept(message);
        if (sent) {
            System.out.println("[INFO] Tin nhắn CHAT đã được giao và lưu. id=" + message.getId());
        } else {
            System.out.println("[WARN] Tin nhắn CHAT thất bại sau khi retry. id=" + message.getId()
                    + ", tới=" + receiver.addressKey() + ", status=" + message.getStatus());
        }
        peerChangeNotifier.run();
        return sent;
    }

    /**
     * Luu tin offline len bootstrap-server de receiver nhan lai khi JOIN.
     */
    private boolean storeOfflineIfPossible(Message message) {
        if (bootstrapClient == null) {
            System.out.println("[WARN] Không thể lưu tin nhắn offline vì bootstrap đang tắt. messageId="
                    + message.getId());
            return false;
        }
        boolean stored = bootstrapClient.storeOffline(Mes.fromMessage(message));
        System.out.println("[INFO] Đã lưu fallback offline=" + stored + ", messageId=" + message.getId());
        return stored;
    }
}
