package dungcony.ds.model;

import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class Group {
    private String groupId;
    private String name;
    private Set<PeerInfo> members = new LinkedHashSet<>();

    public Group() {
    }

    /**
     * Tạo group mới với id tự sinh và tên hiển thị.
     */
    public Group(String name) {
        this.groupId = UUID.randomUUID().toString();
        this.name = name == null || name.isBlank() ? "Group" : name.trim();
    }

    /**
     * Phuc hoi group co san tu bootstrap/local cache voi groupId da ton tai.
     */
    public Group(String groupId, String name, Collection<PeerInfo> members) {
        this.groupId = groupId == null || groupId.isBlank() ? UUID.randomUUID().toString() : groupId;
        this.name = name == null || name.isBlank() ? "Group" : name.trim();
        if (members != null) {
            members.forEach(this::addMember);
        }
    }

    public String getGroupId() {
        return groupId;
    }


    public String getName() {
        return name;
    }

    public Set<PeerInfo> getMembers() {
        return members;
    }

    public void addMember(PeerInfo peerInfo) {
        if (peerInfo != null) {
            members.add(peerInfo);
        }
    }
}
