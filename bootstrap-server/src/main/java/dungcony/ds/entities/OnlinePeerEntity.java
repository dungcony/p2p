package dungcony.ds.entities;

import dungcony.ds.models.PeerInfo;

/**
 * Entity anh xa 1-1 voi bang peers_online.
 */
public class OnlinePeerEntity {
    private final String peerKey;
    private final String userId;
    private final String host;
    private final int port;
    private final boolean online;
    private final long lastSeen;

    public OnlinePeerEntity(String peerKey, String userId, String host, int port, boolean online, long lastSeen) {
        this.peerKey = peerKey;
        this.userId = userId;
        this.host = host;
        this.port = port;
        this.online = online;
        this.lastSeen = lastSeen;
    }

    public static OnlinePeerEntity fromPeerInfo(PeerInfo peerInfo, String userId, long now) {
        return new OnlinePeerEntity(
                peerInfo.addressKey(),
                userId,
                peerInfo.getHost(),
                peerInfo.getPort(),
                true,
                now
        );
    }

    public PeerInfo toPeerInfo() {
        PeerInfo peerInfo = new PeerInfo(userId, host, port);
        peerInfo.setOnline(online);
        return peerInfo;
    }

    public String getPeerKey() {
        return peerKey;
    }

    public String getUserId() {
        return userId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public long getLastSeen() {
        return lastSeen;
    }
}
