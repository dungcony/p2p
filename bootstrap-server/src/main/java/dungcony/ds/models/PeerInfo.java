package dungcony.ds.models;

/**
 * DTO riêng của bootstrap-server để module tracker không phụ thuộc vào peer-node.
 */
public class PeerInfo {
    private String id;
    private String host;
    private int port;
    private boolean online;

    public PeerInfo() {
    }

    public PeerInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.online = true;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String addressKey() {
        return host + ":" + port;
    }
}
