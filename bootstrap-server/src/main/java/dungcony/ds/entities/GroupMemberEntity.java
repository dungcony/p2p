package dungcony.ds.entities;

/**
 * Entity anh xa 1-1 voi bang group_members.
 */
public class GroupMemberEntity {
    private final String groupId;
    private final String userId;
    private final long joinedAt;

    public GroupMemberEntity(String groupId, String userId, long joinedAt) {
        this.groupId = groupId;
        this.userId = userId;
        this.joinedAt = joinedAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getUserId() {
        return userId;
    }

    public long getJoinedAt() {
        return joinedAt;
    }
}
