package dungcony.ds.interfaces;

public interface PeerPresenceService {
    // Kiểm tra trạng thái online
    boolean checkUserIsOnline(String hostAndMaybePort);
}
