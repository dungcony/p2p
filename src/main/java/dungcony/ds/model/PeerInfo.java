package dungcony.ds.model;

import java.util.Objects;

public class PeerInfo {
    private String id;
    private String host;
    private int port;
    private boolean online;

    public PeerInfo() {
    }

    public PeerInfo(String id, String host, int port) {
        this(id, host, port, true);
    }

    public PeerInfo(String id, String host, int port, boolean online) {
        this.id = id == null || id.isBlank() ? host + ":" + port : id.trim();
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.online = online;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PeerInfo peerInfo)) {
            return false;
        }
        return port == peerInfo.port && Objects.equals(host, peerInfo.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
