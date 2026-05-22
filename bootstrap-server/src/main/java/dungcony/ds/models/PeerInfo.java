package dungcony.ds.models;

// DTO riêng của bootstrap-server để module tracker không phụ thuộc vào peer-node.
public class PeerInfo {
    private String id;
    private String name;
    private String host;
    private int port;
    private boolean online;

    public PeerInfo() {
    }

    public PeerInfo(String id, String host, int port) {
        this(id, id, host, port, true);
    }

    public PeerInfo(String id, String name, String host, int port) {
        this(id, name, host, port, true);
    }

    public PeerInfo(String id, String name, String host, int port, boolean online) {
        this.id = id;
        this.name = name == null || name.isBlank() ? id : name.trim();
        this.host = host;
        this.port = port;
        this.online = online;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name == null || name.isBlank() ? id : name;
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
