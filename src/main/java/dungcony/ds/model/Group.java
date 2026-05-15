package dungcony.ds.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Group {
    private String groupId;
    private String name;
    private Set<PeerInfo> members = new LinkedHashSet<>();

    public Group() {
    }

    public Group(String name) {
        this.groupId = UUID.randomUUID().toString();
        this.name = name == null || name.isBlank() ? "Group" : name.trim();
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
