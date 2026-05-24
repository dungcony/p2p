package dungcony.ds.services.interfaces.messaging;

import dungcony.ds.model.Message;

// Đại diện use case gửi lại tin nhắn 1-1 đang FAILED hoặc PENDING
public interface MessageRetryService {

    // Retry một message trực tiếp và cập nhật lại trạng thái trong history local
    boolean retryMessage(Message message);
}
