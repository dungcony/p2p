package dungcony.ds.peer;

import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.repositories.LocalGroupRepo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GroupManager {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final LocalGroupRepo localGroupRepo;
    private final Consumer<Group> groupCreatedPublisher;

    /**
     * Khoi tao GroupManager khong persist, giu tuong thich voi cac luong cu.
     */
    public GroupManager() {
        this(null, null);
    }

    /**
     * Khoi tao GroupManager co local repo de load/save groups.json cua profile.
     */
    public GroupManager(LocalGroupRepo localGroupRepo) {
        this(localGroupRepo, null);
    }

    /**
     * Khoi tao GroupManager co local repo va publisher de dong bo group moi len bootstrap.
     */
    public GroupManager(LocalGroupRepo localGroupRepo, Consumer<Group> groupCreatedPublisher) {
        this.localGroupRepo = localGroupRepo;
        this.groupCreatedPublisher = groupCreatedPublisher;
        if (localGroupRepo != null) {
            for (Group group : localGroupRepo.findAll()) {
                groups.put(group.getGroupId(), group);
            }
            System.out.println("[INFO] GroupManager đã nạp nhóm local. sốLượng=" + groups.size());
        }
    }

    /**
     * Tạo nhóm mới và thêm danh sách thành viên ban đầu nếu có.
     */
    public Group createGroup(String name, Collection<PeerInfo> members) {
        Group group = new Group(name);
        if (members != null) {
            members.forEach(group::addMember);
        }
        groups.put(group.getGroupId(), group);
        saveGroup(group);
        publishGroup(group);
        System.out.println("[INFO] Đã tạo nhóm id=" + group.getGroupId()
                + ", tên=" + group.getName() + ", sốThànhViên=" + group.getMembers().size());
        return group;
    }

    /**
     * Them thanh vien vao group da ton tai va luu lai local cache.
     */
    public Group addMembers(String groupId, Collection<PeerInfo> members) {
        Group group = groups.get(groupId);
        if (group == null) {
            System.out.println("[WARN] Không thể thêm thành viên vì không tìm thấy groupId=" + groupId);
            return null;
        }
        int before = group.getMembers().size();
        if (members != null) {
            members.forEach(group::addMember);
        }
        int added = group.getMembers().size() - before;
        saveGroup(group);
        System.out.println("[INFO] Đã thêm thành viên vào nhóm local. groupId=" + groupId
                + ", sốThànhViênThêm=" + added);
        return group;
    }

    /**
     * Tao hoac cap nhat group local khi nhan duoc GROUP_CHAT truc tiep tu peer khac.
     */
    public Group ensureLocalGroup(String groupId, String name, Collection<PeerInfo> members) {
        Group group = groups.get(groupId);
        if (group == null) {
            group = new Group(groupId, name, members);
            groups.put(group.getGroupId(), group);
            System.out.println("[INFO] Đã tạo nhóm local từ tin nhắn nhận vào. groupId=" + group.getGroupId()
                    + ", tên=" + group.getName());
        } else if (members != null) {
            members.forEach(group::addMember);
            System.out.println("[DEBUG] Đã refresh thành viên nhóm local từ tin nhắn nhận vào. groupId="
                    + group.getGroupId() + ", sốThànhViên=" + group.getMembers().size());
        }
        saveGroup(group);
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

    /**
     * Thay the local group cache bang danh sach group bootstrap tra ve.
     */
    public void replaceAll(Collection<Group> authoritativeGroups) {
        groups.clear();
        if (authoritativeGroups != null) {
            for (Group group : authoritativeGroups) {
                groups.put(group.getGroupId(), group);
            }
        }
        if (localGroupRepo != null) {
            localGroupRepo.saveAll(groups.values());
        }
        System.out.println("[INFO] GroupManager đã thay nhóm bằng dữ liệu bootstrap. sốLượng=" + groups.size());
    }

    /**
     * Luu group moi/cap nhat xuong groups.json neu local repo duoc cau hinh.
     */
    private void saveGroup(Group group) {
        if (localGroupRepo != null) {
            localGroupRepo.save(group);
        }
    }

    /**
     * Publish group moi len bootstrap neu PeerNode cau hinh publisher.
     */
    private void publishGroup(Group group) {
        if (groupCreatedPublisher != null) {
            groupCreatedPublisher.accept(group);
        }
    }
}
