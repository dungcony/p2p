package dungcony.ds.services.impl.messaging;

import dungcony.ds.enums.MessageStatus;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.bootstrap.BootstrapGateway;
import dungcony.ds.services.interfaces.messaging.MessageHistoryService;
import dungcony.ds.services.interfaces.messaging.MessageRetryService;
import dungcony.ds.services.interfaces.messaging.PeerMessageSender;
import dungcony.ds.services.interfaces.peer.PeerDirectoryService;
import dungcony.ds.utils.Mes;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
// Service xử lý retry thủ công cho tin nhắn 1-1 đã FAILED hoặc PENDING
public class MessageRetryImpl implements MessageRetryService {

    private final PeerMessageSender messageSender;
    private final BootstrapGateway bootstrapGateway;
    private final PeerDirectoryService peerDirectoryService;
    private final MessageHistoryService messageHistoryService;
    private final Consumer<Message> messageNotifier;
    private final Runnable peerChangeNotifier;

    // Khởi tạo service retry với sender, bootstrap fallback, danh bạ và history
    public MessageRetryImpl(PeerMessageSender messageSender,
                            BootstrapGateway bootstrapGateway,
                            PeerDirectoryService peerDirectoryService,
                            MessageHistoryService messageHistoryService,
                            Consumer<Message> messageNotifier,
                            Runnable peerChangeNotifier) {
        this.messageSender = messageSender;
        this.bootstrapGateway = bootstrapGateway;
        this.peerDirectoryService = peerDirectoryService;
        this.messageHistoryService = messageHistoryService;
        this.messageNotifier = messageNotifier;
        this.peerChangeNotifier = peerChangeNotifier;
    }

    // Retry một tin 1-1 và cập nhật status SENDING → SENT / PENDING / FAILED
    @Override
    public boolean retryMessage(Message message) {
        if (message == null) {
            return false;
        }
        if (message.getGroupId() != null && !message.getGroupId().isBlank()) {
            log.warn("Chưa hỗ trợ retry thủ công cho tin nhắn nhóm. messageId={}", message.getId());
            return false;
        }
        if (message.getStatus() != MessageStatus.FAILED && message.getStatus() != MessageStatus.PENDING) {
            log.warn("Bỏ qua retry vì trạng thái hiện tại không cần retry. messageId={}, status={}",
                    message.getId(), message.getStatus());
            return false;
        }
        PeerInfo receiver = resolveMessageReceiver(message);
        if (receiver == null || peerDirectoryService.isSelfPeer(receiver)) {
            log.warn("Không thể retry vì không xác định được receiver. messageId={}", message.getId());
            return false;
        }
        message.setStatus(MessageStatus.SENDING);
        messageHistoryService.updateAndSave(receiver, message);
        messageNotifier.accept(message);

        log.info("Đang retry tin nhắn. messageId={}, receiver={}", message.getId(), receiver.addressKey());
        boolean sent = messageSender.send(receiver, message);
        receiver.setOnline(sent);
        if (sent) {
            message.setStatus(MessageStatus.SENT);
        } else if (storeDirectOfflineIfPossible(message)) {
            message.setStatus(MessageStatus.PENDING);
        } else {
            message.setStatus(MessageStatus.FAILED);
        }
        messageHistoryService.updateAndSave(receiver, message);
        messageNotifier.accept(message);
        peerChangeNotifier.run();
        log.info("Retry tin nhắn kết thúc. messageId={}, status={}", message.getId(), message.getStatus());
        return sent;
    }

    // Lưu fallback offline cho retry tin 1-1 nếu bootstrap đang sẵn sàng
    private boolean storeDirectOfflineIfPossible(Message message) {
        if (bootstrapGateway == null) {
            log.warn("Không thể lưu fallback retry vì bootstrap đang tắt. messageId={}", message.getId());
            return false;
        }
        boolean stored = bootstrapGateway.storeOffline(Mes.fromMessage(message));
        log.info("Đã lưu fallback retry offline={}, messageId={}", stored, message.getId());
        return stored;
    }

    // Xác định receiver từ danh bạ runtime hoặc từ host port đã lưu trong message
    private PeerInfo resolveMessageReceiver(Message message) {
        PeerInfo receiver = peerDirectoryService.findKnownPeerById(message.getReceiverId());
        if (receiver != null) {
            return receiver;
        }
        if (message.getReceiverHost() == null || message.getReceiverHost().isBlank()
                || message.getReceiverPort() <= 0) {
            return null;
        }
        return new PeerInfo(
                message.getReceiverId(),
                message.getReceiverId(),
                message.getReceiverHost(),
                message.getReceiverPort(),
                false);
    }
}
