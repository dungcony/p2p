package dungcony.ds.services.interfaces.security;

import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.Optional;

public interface MessageEncryptionService {
    Optional<Message> encryptForReceiver(Message message, PeerInfo receiver);

    Message decrypt(Message message);
}
