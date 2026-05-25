package dungcony.ds.services.interfaces.bootstrap;

/**
 * Cổng giao tiếp đầy đủ với bootstrap-server.
 * Kết hợp peer lifecycle, offline message và group management.
 *
 * <p>Được tách thành 3 sub-interface theo ISP:
 * <ul>
 *   <li>{@link PeerBootstrapGateway} — register/join/leave/list peer</li>
 *   <li>{@link OfflineMessageGateway} — lưu tin nhắn offline</li>
 *   <li>{@link GroupBootstrapGateway} — quản lý group chat</li>
 * </ul>
 * Các service chỉ nên inject sub-interface phù hợp thay vì toàn bộ BootstrapGateway.
 * </p>
 */
public interface BootstrapGateway extends PeerBootstrapGateway, OfflineMessageGateway, GroupBootstrapGateway {
}

