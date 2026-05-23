package dungcony.ds.interfaces;

// Đại diện contract kiểm tra trạng thái hiện diện của peer
public interface PeerPresenceService {
    // Kiểm tra trạng thái online
    boolean checkUserIsOnline(String hostAndMaybePort);
}
