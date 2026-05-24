package dungcony.ds.services.interfaces.messaging;

import dungcony.ds.model.Message;

public interface MessageListener {
    void onMessageReceived(Message message);
}
