package dungcony.ds.services.interfaces.messaging;

import dungcony.ds.model.Message;

/**
 * Listener nhận message mới để UI hoặc service cập nhật trạng thái
 */
public interface MessageListener {
    void onMessageReceived(Message message);
}
