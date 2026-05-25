package dungcony.ds.services.interfaces.messaging;

import dungcony.ds.model.Message;

/**
 * Service điều phối message inbound và tạo response gửi ngược
 */
public interface MessageRouterService {
    Message receive(Message message);
}
