package dungcony.ds.interfaces;

public interface PeerPresenceService {
    // kiếm tra trạng thái onl
    boolean checkUserIsOnline(String hostAndMaybePort);
}
