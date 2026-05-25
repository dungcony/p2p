package dungcony.ds.utils;

import dungcony.ds.model.PeerInfo;

/**
 * Helper tạo conversation ảo cho broadcast toàn mạng.
 */
public final class BroadcastConversation {
    public static final String ID = "__broadcast__";
    public static final String NAME = "Thế giới";
    public static final String HOST = "broadcast:world";
    public static final int PORT = 0;

    private BroadcastConversation() {
    }

    public static String historyKey() {
        return HOST + ":" + PORT;
    }

    public static PeerInfo conversationPeer() {
        return new PeerInfo(ID, NAME, HOST, PORT, true);
    }
}
