package dungcony.ds.entities;

import dungcony.ds.models.PeerInfo;

// Entity ánh xạ 1-1 với bảng users.
public class UserEntity {
    private final String userId;
    private final String displayName;
    private final long createdAt;
    private final long updatedAt;

    public UserEntity(String userId, String displayName, long createdAt, long updatedAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserEntity fromPeerInfo(PeerInfo peerInfo, long now) {
        String userId = peerInfo.getId() == null || peerInfo.getId().isBlank()
                ? peerInfo.addressKey()
                : peerInfo.getId();
        String displayName = peerInfo.getName() == null || peerInfo.getName().isBlank()
                ? userId
                : peerInfo.getName();
        return new UserEntity(userId, displayName, now, now);
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
