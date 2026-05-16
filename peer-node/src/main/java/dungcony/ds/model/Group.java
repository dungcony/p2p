package dungcony.ds.model;

import dungcony.ds.entities.PeerInfo;

import java.util.LinkedHashSet;
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
     * Lấy id duy nhất của nhóm.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Lấy tên hiển thị của nhóm.
     */
    public String getName() {
        return name;
    }

    /**
     * Lấy danh sách thành viên hiện có trong nhóm.
     */
    public Set<PeerInfo> getMembers() {
        return members;
    }

    /**
     * Thêm một peer vào nhóm nếu peer hợp lệ.
     */
    public void addMember(PeerInfo peerInfo) {
        if (peerInfo != null) {
            members.add(peerInfo);
        }
    }
}
