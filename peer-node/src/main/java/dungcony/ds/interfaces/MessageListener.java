package dungcony.ds.interfaces;

import dungcony.ds.model.Message;

public interface MessageListener {
    void onMessageReceived(Message message);
}
