package dungcony.ds.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.repositories.LocalGroupRepo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GroupManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupManager.class);
private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final LocalGroupRepo localGroupRepo;
    private final Consumer<Group> groupCreatedPublisher;


    // Khởi tạo GroupManager có local repo và publisher để đồng bộ group mới lên bootstrap.
    public GroupManager(LocalGroupRepo localGroupRepo, Consumer<Group> groupCreatedPublisher) {
        this.localGroupRepo = localGroupRepo;
        this.groupCreatedPublisher = groupCreatedPublisher;
        if (localGroupRepo != null) {
            for (Group group : localGroupRepo.findAll()) {
                groups.put(group.getGroupId(), group);
            }
            LOGGER.info("GroupManager đã nạp nhóm local. sốLượng=" + groups.size());
        }
    }

    // Tạo nhóm mới và thêm danh sách thành viên ban đầu nếu có.
    public Group createGroup(String name, Collection<PeerInfo> members) {
        Group group = new Group(name);
        if (members != null) {
            members.forEach(group::addMember);
        }
        groups.put(group.getGroupId(), group);
        saveGroup(group);
        publishGroup(group);
        LOGGER.info("Đã tạo nhóm id=" + group.getGroupId()
                + ", tên=" + group.getName() + ", sốThànhViên=" + group.getMembers().size());
        return group;
    }

    // Thêm thành viên vào group đã tồn tại và lưu lại local cache.
    public Group addMembers(String groupId, Collection<PeerInfo> members) {
        Group group = groups.get(groupId);
        if (group == null) {
            LOGGER.warn("Không thể thêm thành viên vì không tìm thấy groupId=" + groupId);
            return null;
        }
        int before = group.getMembers().size();
        if (members != null) {
            members.forEach(group::addMember);
        }
        int added = group.getMembers().size() - before;
        saveGroup(group);
        LOGGER.info("Đã thêm thành viên vào nhóm local. groupId=" + groupId
                + ", sốThànhViênThêm=" + added);
        return group;
    }

    // Tạo hoặc cập nhật group local khi nhận được GROUP_CHAT trực tiếp từ peer khác.
    public Group ensureLocalGroup(String groupId, String name, Collection<PeerInfo> members) {
        Group group = groups.get(groupId);
        if (group == null) {
            group = new Group(groupId, name, members);
            groups.put(group.getGroupId(), group);
            LOGGER.info("Đã tạo nhóm local từ tin nhắn nhận vào. groupId=" + group.getGroupId()
                    + ", tên=" + group.getName());
        } else if (members != null) {
            members.forEach(group::addMember);
            LOGGER.debug("Đã refresh thành viên nhóm local từ tin nhắn nhận vào. groupId="
                    + group.getGroupId() + ", sốThànhViên=" + group.getMembers().size());
        }
        saveGroup(group);
        return group;
    }

    // Đồng bộ group local bằng snapshot membership đầy đủ từ peer khác.
    public Group syncMembers(String groupId, String name, Collection<PeerInfo> members) {
        Group group = groups.get(groupId);
        if (group == null) {
            group = new Group(groupId, name, members);
            groups.put(group.getGroupId(), group);
            LOGGER.info("Đã tạo nhóm local từ GROUP_MEMBERS_SYNC. groupId=" + group.getGroupId()
                    + ", tên=" + group.getName());
        } else {
            group.replaceMembers(members);
            LOGGER.info("Đã cập nhật membership nhóm từ GROUP_MEMBERS_SYNC. groupId=" + groupId
                    + ", sốThànhViên=" + group.getMembers().size());
        }
        saveGroup(group);
        return group;
    }

    // Tìm group theo groupId để gửi tin hoặc hiển thị thông tin nhóm.
    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    // Lấy toàn bộ nhóm hiện có ở runtime.
    public Collection<Group> getAllGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    // Thay thế local group cache bằng danh sách group bootstrap trả về.
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
        LOGGER.info("GroupManager đã thay nhóm bằng dữ liệu bootstrap. sốLượng=" + groups.size());
    }

    // Lưu group mới/cập nhật xuống groups.json nếu local repo được cấu hình.
    private void saveGroup(Group group) {
        if (localGroupRepo != null) {
            localGroupRepo.save(group);
        }
    }

    // Publish group mới lên bootstrap nếu PeerNode cấu hình publisher.
    private void publishGroup(Group group) {
        if (groupCreatedPublisher != null) {
            groupCreatedPublisher.accept(group);
        }
    }
}
