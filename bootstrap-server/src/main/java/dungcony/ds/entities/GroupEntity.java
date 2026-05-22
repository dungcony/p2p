package dungcony.ds.entities;

// Entity ánh xạ 1-1 với bảng groups.
public class GroupEntity {
    private final String groupId;
    private final String name;
    private final String createdBy;
    private final long createdAt;

    public GroupEntity(String groupId, String name, String createdBy, long createdAt) {
        this.groupId = groupId;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
