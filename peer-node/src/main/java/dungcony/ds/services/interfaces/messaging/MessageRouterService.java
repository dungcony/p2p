package dungcony.ds.services.interfaces.messaging;

import dungcony.ds.model.Message;

public interface MessageRouterService {
    Message receive(Message message);
}
