package dungcony.ds.model;

import java.util.Objects;

public class PeerInfo {
    private String id;
    private String host;
    private int port;
    private boolean online;

    public PeerInfo() {
    }

    /**
     * Tạo thông tin peer với trạng thái online mặc định.
     */
    public PeerInfo(String id, String host, int port) {
        this(id, host, port, true);
    }

    /**
     * Tạo thông tin peer đầy đủ gồm định danh, host, port và trạng thái online.
     */
    public PeerInfo(String id, String host, int port, boolean online) {
        this.id = id == null || id.isBlank() ? host + ":" + port : id.trim();
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
     * So sánh peer theo host và port vì đây là địa chỉ kết nối thật.
     */
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

    /**
     * Tạo hash tương ứng với equals theo host và port.
     */
    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
