package dungcony.ds.mapper;

import dungcony.ds.dtos.OfflineMessage;
import dungcony.ds.model.Message;

public class Mes {
    /**
     * Tao offline message tu message P2P khi gui truc tiep that bai.
     */
    public static OfflineMessage fromMessage(Message message) {
        return new OfflineMessage(
                message.getId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getGroupId(),
                message.getContent(),
                message.getTimestamp(),
                false
        );
    }
}
