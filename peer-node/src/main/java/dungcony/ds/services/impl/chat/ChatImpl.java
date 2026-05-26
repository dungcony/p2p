package dungcony.ds.services.impl.chat;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.bootstrap.OfflineMessageGateway;
import dungcony.ds.services.interfaces.chat.ChatService;
import dungcony.ds.services.interfaces.messaging.MessageHistoryService;
import dungcony.ds.services.interfaces.messaging.PeerMessageSender;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.services.interfaces.security.MessageEncryptionService;
import dungcony.ds.utils.Mes;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
// Service xử lý gửi chat 1-1 và fallback lưu offline qua bootstrap
public class ChatImpl implements ChatService {

    private final PeerInfo localPeer;
    private final PeerMessageSender messageSender;
    private final OfflineMessageGateway bootstrapGateway;
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final MessageEncryptionService encryptionService;
    private final Consumer<Message> messageNotifier;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service xử lý heartbeat và gửi chat 1-1
    public ChatImpl(PeerInfo localPeer, PeerMessageSender messageSender, OfflineMessageGateway bootstrapGateway,
            PeerDirectoryService peerDirectoryService, MessageHistoryService messageHistoryService,
            MessageEncryptionService encryptionService,
            Consumer<Message> messageNotifier, Runnable peerChangeNotifier) {
        this.localPeer = localPeer;
        this.messageSender = messageSender;
        this.bootstrapGateway = bootstrapGateway;
        this.peerDirectoryService = peerDirectoryService;
        this.messageHistoryService = messageHistoryService;
        this.encryptionService = encryptionService;
        this.messageNotifier = messageNotifier;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    // Gửi tin nhắn 1-1 trực tiếp tới peer đích, lưu lịch sử nếu gửi được hoặc store
    // offline thành công
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

        // Mã hóa message cho receiver, nếu thất bại thì đánh dấu message lỗi
        Optional<Message> outboundMessage = encryptionService.encryptForReceiver(message, receiver);

        if (outboundMessage.isEmpty()) {
            message.setStatus(MessageStatus.FAILED);
            messageHistoryService.addAndSave(receiver, message);
            messageNotifier.accept(message);
            peerChangeNotifier.run();
            return false;
        }
        log.info("Đang gửi tin nhắn CHAT id={} tới={}", message.getId(), receiver.addressKey());
        boolean sent = messageSender.send(receiver, outboundMessage.get());
        receiver.setOnline(sent);
        if (sent) {
            message.setStatus(MessageStatus.SENT);
        } else if (storeOfflineIfPossible(outboundMessage.get())) {
            message.setStatus(MessageStatus.PENDING);
        } else {
            message.setStatus(MessageStatus.FAILED);
        }
        messageHistoryService.addAndSave(receiver, message);
        messageNotifier.accept(message);
        if (sent) {
            log.info("Tin nhắn CHAT đã được giao và lưu. id={}", message.getId());
        } else {
            log.warn("Tin nhắn CHAT thất bại sau khi retry. id={}, tới={}, status={}",
                    message.getId(), receiver.addressKey(), message.getStatus());
        }
        peerChangeNotifier.run();
        return sent;
    }

    // Lưu tin offline lên bootstrap-server để receiver nhận lại khi JOIN
    private boolean storeOfflineIfPossible(Message message) {
        if (bootstrapGateway == null) {
            log.warn("Không thể lưu tin nhắn offline vì bootstrap đang tắt. messageId={}", message.getId());
            return false;
        }
        boolean stored = bootstrapGateway.storeOffline(Mes.fromMessage(message));
        log.info("Đã lưu fallback offline={}, messageId={}", stored, message.getId());
        return stored;
    }
}
