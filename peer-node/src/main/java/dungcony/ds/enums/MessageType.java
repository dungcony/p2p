package dungcony.ds.enums;

/**
 * Các loại message điều phối qua giao thức P2P
 */
public enum MessageType {
    CHAT,
    GROUP_CHAT,
    BROADCAST,
    PEER_LIST_REQUEST,
    PEER_LIST_RESPONSE,
    GROUP_MEMBERS_SYNC,
    JOIN,
    LEAVE,
    ACK,
    HEARTBEAT
}
