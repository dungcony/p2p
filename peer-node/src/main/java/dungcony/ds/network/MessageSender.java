package dungcony.ds.network;

import dungcony.ds.config.TCPConfig;
import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.messaging.PeerMessageSender;
import lombok.extern.slf4j.Slf4j;

/**
 * Đóng gói logic gửi message qua TCP với retry và ACK validation
 *
 * <p>Luồng gửi:</p>
 * <pre>
 *   send()            → TCPClient.sendForResponse() → validate ACK → retry nếu thất bại
 *   sendForResponse() → TCPClient.sendForResponse() → validate response type + id → retry
 *   broadcast()       → gọi send() cho từng member trong group
 * </pre>
 */
@Slf4j
public class MessageSender implements PeerMessageSender {

    private final TCPClient tcpClient;

    // Khởi tạo sender với TCPClient dùng để gửi dữ liệu qua mạng
    public MessageSender(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    // Gửi message tới một peer, retry vài lần nếu chưa nhận ACK
    @Override
    public boolean send(PeerInfo peerInfo, Message message) {
        for (int attempt = 1; attempt <= TCPConfig.retry_mes_count; attempt++) {
            log.debug("Đang gửi {} message id={} tới={}, attempt={}/{}",
                    message.getType(), message.getId(), peerInfo.addressKey(), attempt, TCPConfig.retry_mes_count);
            Message response = tcpClient.transmit(peerInfo, message);
            if (isValidResponse(response, message.getId(), MessageType.ACK)) {
                log.debug("Đã nhận ACK cho message id={} từ={}", message.getId(), peerInfo.addressKey());
                return true;
            }
            log.warn("Không nhận được ACK cho message id={} từ={}, attempt={}",
                    message.getId(), peerInfo.addressKey(), attempt);
            sleepBeforeRetry();
        }
        return false;
    }

    // Gửi request và chờ response có type cụ thể, dùng cho peer discovery
    @Override
    public Message sendForResponse(PeerInfo peerInfo, Message message, MessageType expectedType) {
        for (int attempt = 1; attempt <= TCPConfig.retry_mes_count; attempt++) {
            log.debug("Đang gửi request {} id={} tới={}, attempt={}/{}",
                    message.getType(), message.getId(), peerInfo.addressKey(), attempt, TCPConfig.retry_mes_count);
            Message response = tcpClient.transmit(peerInfo, message);
            if (isValidResponse(response, message.getId(), expectedType)) {
                log.debug("Đã nhận response hợp lệ. requestId={}, responseType={}",
                        message.getId(), response.getType());
                return response;
            }
            log.warn("Response không hợp lệ hoặc timeout. requestId={}, expected={}, actual={}",
                    message.getId(), expectedType, response == null ? "null" : response.getType());
            sleepBeforeRetry();
        }
        return null;
    }

    // Broadcast một message tới toàn bộ thành viên của group
    public void broadcast(Group group, Message message) {
        for (PeerInfo member : group.getMembers()) {
            log.info("Đang broadcast message id={} tới thành viên={}", message.getId(), member.addressKey());
            send(member, message);
        }
    }

    // Kiểm tra response có hợp lệ: không null, đúng type, đúng message id
    private boolean isValidResponse(Message response, String expectedId, MessageType expectedType) {
        return response != null
                && response.getType() == expectedType
                && expectedId.equals(response.getId());
    }

    // Nghỉ ngắn giữa các lần retry để tránh gửi dồn dập khi peer chưa phản hồi
    private void sleepBeforeRetry() {
        try {
            Thread.sleep(TCPConfig.retry_mes_delay_ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

