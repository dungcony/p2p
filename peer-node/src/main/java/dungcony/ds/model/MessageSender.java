package dungcony.ds.model;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.enums.MessageType;
import dungcony.ds.network.TCPClient;

@Slf4j
public class MessageSender {
private static final int RETRY_COUNT = 3;
    private final TCPClient tcpClient;

    // Khởi tạo sender với TCPClient dùng để gửi dữ liệu qua mạng.
    public MessageSender(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    // Gửi message tới một peer, retry vài lần nếu chưa nhận ACK.
    public boolean send(PeerInfo peerInfo, Message message) {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            log.debug("Đang gửi " + message.getType() + " message id=" + message.getId()
                    + " tới=" + peerInfo.addressKey() + ", attempt=" + attempt + "/" + RETRY_COUNT);
            if (tcpClient.send(peerInfo, message)) {
                log.debug("Đã nhận ACK cho message id=" + message.getId()
                        + " từ=" + peerInfo.addressKey());
                return true;
            }
            log.warn("Không nhận được ACK cho message id=" + message.getId()
                    + " từ=" + peerInfo.addressKey() + ", attempt=" + attempt);
            sleepBeforeRetry();
        }
        return false;
    }

    // Gửi request và cho response có type cụ thể, dùng cho discovery peer-to-peer.
    public Message sendForResponse(PeerInfo peerInfo, Message message, MessageType expectedType) {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            log.debug("Đang gửi request " + message.getType() + " id=" + message.getId()
                    + " tới=" + peerInfo.addressKey() + ", attempt=" + attempt + "/" + RETRY_COUNT);
            Message response = tcpClient.sendForResponse(peerInfo, message);
            if (response != null && response.getType() == expectedType && message.getId().equals(response.getId())) {
                log.debug("Đã nhận response hợp lệ. requestId=" + message.getId()
                        + ", responseType=" + response.getType());
                return response;
            }
            log.warn("Response không hợp lệ hoặc timeout. requestId=" + message.getId()
                    + ", expected=" + expectedType
                    + ", actual=" + (response == null ? "null" : response.getType()));
            sleepBeforeRetry();
        }
        return null;
    }

    // Broadcast một message tới toàn bộ thành viên của group.
    public void broadcast(Group group, Message message) {
        for (PeerInfo member : group.getMembers()) {
            log.info("Đang broadcast message id=" + message.getId()
                    + " tới thành viên=" + member.addressKey());
            send(member, message);
        }
    }

    // Nghỉ ngắn giữa các lần retry để tránh gửi dồn dập khi peer chưa phản hồi.
    private void sleepBeforeRetry() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
