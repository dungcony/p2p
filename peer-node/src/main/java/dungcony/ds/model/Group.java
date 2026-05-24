package dungcony.ds.model;

import lombok.Getter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class Group {
    private String groupId;
    private String name;
    private final Set<PeerInfo> members = new LinkedHashSet<>();

    public Group() {
    }

    // Tạo group mới với id tự sinh và tên hiển thị
    public Group(String name) {
        this.groupId = UUID.randomUUID().toString();
        this.name = name == null || name.isBlank() ? "Group" : name.trim();
    }

    // Phục hồi group có sẵn từ bootstrap/local cache với groupId đã tồn tại
    public Group(String groupId, String name, Collection<PeerInfo> members) {
        this.groupId = groupId == null || groupId.isBlank() ? UUID.randomUUID().toString() : groupId;
        this.name = name == null || name.isBlank() ? "Group" : name.trim();
        if (members != null) {
            members.forEach(this::addMember);
        }
    }


    public void addMember(PeerInfo peerInfo) {
        if (peerInfo != null) {
            members.add(peerInfo);
        }
    }

    // Thay danh sách thành viên bằng snapshot mới nhận từ bootstrap/peer sync
    public void replaceMembers(Collection<PeerInfo> newMembers) {
        members.clear();
        if (newMembers != null) {
            newMembers.forEach(this::addMember);
        }
    }
}
