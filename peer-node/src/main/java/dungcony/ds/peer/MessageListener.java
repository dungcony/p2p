package dungcony.ds.peer;

import dungcony.ds.model.Message;

public interface MessageListener {
    void onMessageReceived(Message message);
}
