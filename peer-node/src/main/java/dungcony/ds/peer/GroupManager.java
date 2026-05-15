package dungcony.ds.peer;

import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    /**
     * Tạo nhóm mới và thêm danh sách thành viên ban đầu nếu có.
     */
    public Group createGroup(String name, Collection<PeerInfo> members) {
        Group group = new Group(name);
        if (members != null) {
            members.forEach(group::addMember);
        }
        groups.put(group.getGroupId(), group);
        System.out.println("[INFO] Created group id=" + group.getGroupId()
                + ", name=" + group.getName() + ", members=" + group.getMembers().size());
        return group;
    }

    /**
     * Tìm group theo groupId để gửi tin hoặc hiển thị thông tin nhóm.
     */
    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    /**
     * Lấy toàn bộ nhóm hiện có ở runtime.
     */
    public Collection<Group> getAllGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }
}
