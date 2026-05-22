package dungcony.ds.services;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.enums.MessageStatus;
import dungcony.ds.interfaces.ChatService;
import dungcony.ds.interfaces.MessageHistoryService;
import dungcony.ds.interfaces.PeerDirectoryService;
import dungcony.ds.model.BootstrapClient;
import dungcony.ds.model.Message;
import dungcony.ds.model.MessageSender;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.utils.Mes;

import java.util.function.Consumer;

@Slf4j
public class ChatImpl implements ChatService {
private final PeerInfo localPeer;
    private final MessageSender messageSender;
    private final BootstrapClient bootstrapClient;
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final Consumer<Message> messageNotifier;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service xử lý heartbeat và gửi chat 1-1.
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

    // Gửi tin nhắn 1-1 trực tiếp tới peer đích, lưu lịch sử nếu gửi được hoặc store offline thành công.
    @Override
    public boolean sendMessage(String content, String hostAndMaybePort) {
        if (content == null || content.isBlank()) {
            log.warn("Từ chối gửi tin nhắn rỗng.");
            return false;
        }
        PeerInfo receiver = peerDirectoryService.resolvePeer(hostAndMaybePort);
        if (receiver == null) {
            log.warn("Từ chối gửi tin vì peer đích rỗng.");
            return false;
        }
        if (peerDirectoryService.isSelfPeer(receiver)) {
            log.warn("Từ chối gửi tin tới peer hiện tại: {}", receiver.addressKey());
            return false;
        }

        Message message = Message.chat(localPeer, receiver, content);
        log.info("Đang gửi tin nhắn CHAT id={} tới={}", message.getId(), receiver.addressKey());
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
            log.info("Tin nhắn CHAT đã được giao và lưu. id={}", message.getId());
        } else {
            log.warn("Tin nhắn CHAT thất bại sau khi retry. id={}, tới={}, status={}", message.getId(), receiver.addressKey(), message.getStatus());
        }
        peerChangeNotifier.run();
        return sent;
    }

    // Lưu tin offline lên bootstrap-server để receiver nhận lại khi JOIN.
    private boolean storeOfflineIfPossible(Message message) {
        if (bootstrapClient == null) {
            log.warn("Không thể lưu tin nhắn offline vì bootstrap đang tắt. messageId={}", message.getId());
            return false;
        }
        boolean stored = bootstrapClient.storeOffline(Mes.fromMessage(message));
        log.info("Đã lưu fallback offline={}, messageId={}", stored, message.getId());
        return stored;
    }
}
