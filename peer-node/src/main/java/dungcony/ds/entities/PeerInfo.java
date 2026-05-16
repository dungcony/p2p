package dungcony.ds.entities;

import java.util.Objects;

public class PeerInfo {
    private String id;
    private String name;
    private String host;
    private int port;
    private boolean online;

    public PeerInfo() {
    }

    /**
     * Tạo thông tin peer với trạng thái online mặc định.
     */
    public PeerInfo(String id, String host, int port) {
        this(id, id, host, port, true);
    }

    /**
     * Tạo thông tin peer với id ổn định và tên hiển thị riêng.
     */
    public PeerInfo(String id, String name, String host, int port) {
        this(id, name, host, port, true);
    }

    /**
     * Tạo thông tin peer đầy đủ gồm định danh, host, port và trạng thái online.
     */
    public PeerInfo(String id, String host, int port, boolean online) {
        this(id, id, host, port, online);
    }

    /**
     * Tạo thông tin peer đầy đủ gồm id, tên hiển thị, host, port và trạng thái online.
     */
    public PeerInfo(String id, String name, String host, int port, boolean online) {
        this.id = id == null || id.isBlank() ? host + ":" + port : id.trim();
        this.name = name == null || name.isBlank() ? this.id : name.trim();
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.online = online;
    }

    /**
     * Lấy tên/id hiển thị của peer.
     */
    public String getId() {
        return id;
    }

    /**
     * Lấy tên hiển thị của peer, tách riêng với id ổn định dùng để lưu DB.
     */
    public String getName() {
        return name == null || name.isBlank() ? id : name;
    }

    /**
     * Lấy host hoặc IP mà peer đang lắng nghe.
     */
    public String getHost() {
        return host;
    }

    /**
     * Lấy port TCP mà peer đang lắng nghe.
     */
    public int getPort() {
        return port;
    }

    /**
     * Cho biết peer đang được đánh dấu online hay offline.
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Cập nhật trạng thái online/offline sau heartbeat hoặc gửi tin.
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * Trả về key chuẩn host:port để lưu map peer và history.
     */
    public String addressKey() {
        return host + ":" + port;
    }

    /**
     * So sánh peer ưu tiên theo id ổn định, fallback về host/port khi chưa có id.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PeerInfo peerInfo)) {
            return false;
        }
        if (id != null && !id.isBlank() && peerInfo.id != null && !peerInfo.id.isBlank()) {
            return Objects.equals(id, peerInfo.id);
        }
        return port == peerInfo.port && Objects.equals(host, peerInfo.host);
    }

    /**
     * Tạo hash tương ứng với equals theo id hoặc host/port.
     */
    @Override
    public int hashCode() {
        if (id != null && !id.isBlank()) {
            return Objects.hash(id);
        }
        return Objects.hash(host, port);
    }
}
