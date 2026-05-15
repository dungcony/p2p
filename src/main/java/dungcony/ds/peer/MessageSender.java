package dungcony.ds.peer;

import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.network.TCPClient;

public class MessageSender {
    private static final int RETRY_COUNT = 3;
    private final TCPClient tcpClient;

    public MessageSender(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public boolean send(PeerInfo peerInfo, Message message) {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            if (tcpClient.send(peerInfo, message)) {
                return true;
            }
            sleepBeforeRetry();
        }
        return false;
    }

    public void broadcast(Group group, Message message) {
        for (PeerInfo member : group.getMembers()) {
            send(member, message);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
