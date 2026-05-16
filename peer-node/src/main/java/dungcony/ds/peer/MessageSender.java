package dungcony.ds.peer;

import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.entities.PeerInfo;
import dungcony.ds.network.TCPClient;

public class MessageSender {
    private static final int RETRY_COUNT = 3;
    private final TCPClient tcpClient;

    /**
     * Khởi tạo sender với TCPClient dùng để gửi dữ liệu qua mạng.
     */
    public MessageSender(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    /**
     * Gửi message tới một peer, retry vài lần nếu chưa nhận ACK.
     */
    public boolean send(PeerInfo peerInfo, Message message) {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            System.out.println("[DEBUG] Sending " + message.getType() + " message id=" + message.getId()
                    + " to=" + peerInfo.addressKey() + ", attempt=" + attempt + "/" + RETRY_COUNT);
            if (tcpClient.send(peerInfo, message)) {
                System.out.println("[DEBUG] ACK received for message id=" + message.getId()
                        + " from=" + peerInfo.addressKey());
                return true;
            }
            System.out.println("[WARN] No ACK for message id=" + message.getId()
                    + " from=" + peerInfo.addressKey() + ", attempt=" + attempt);
            sleepBeforeRetry();
        }
        return false;
    }

    /**
     * Broadcast một message tới toàn bộ thành viên của group.
     */
    public void broadcast(Group group, Message message) {
        for (PeerInfo member : group.getMembers()) {
            System.out.println("[INFO] Broadcasting message id=" + message.getId()
                    + " to member=" + member.addressKey());
            send(member, message);
        }
    }

    /**
     * Nghỉ ngắn giữa các lần retry để tránh gửi dồn dập khi peer chưa phản hồi.
     */
    private void sleepBeforeRetry() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
