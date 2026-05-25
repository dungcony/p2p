package dungcony.ds.services.interfaces.bootstrap;

import dungcony.ds.dtos.OfflineMessage;

/**
 * Giao tiếp với bootstrap-server để lưu tin nhắn offline.
 * Tách ra từ BootstrapGateway theo ISP — chỉ dùng bởi các service
 * cần fallback lưu offline (ChatImpl, GroupChatImpl, MessageRetryImpl).
 */
public interface OfflineMessageGateway {

    boolean storeOffline(OfflineMessage message);
}
