package dungcony.ds.services.impl.group;

import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.services.interfaces.persistence.GroupRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Quản lý vòng đời của các group chat: tạo, cập nhật membership, đồng bộ từ bootstrap
 * Cache group vào memory (ConcurrentHashMap) và persist xuống groups.json qua GroupRepository
 *
 * <p>Luồng group:</p>
 * <pre>
 *   createGroup()      → tạo Group mới → saveGroup() → publishGroup() (lên bootstrap)
 *   addMembers()       → thêm member → saveGroup()
 *   ensureLocalGroup() → tạo nếu chưa có, hoặc thêm member mới từ tin nhận vào
 *   syncMembers()      → thay thế toàn bộ membership theo snapshot từ GROUP_MEMBERS_SYNC
 *   replaceAll()       → reset cache theo dữ liệu authoritative từ bootstrap
 * </pre>
 */
@Slf4j
public class GroupManager {

    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final GroupRepository groupRepository;
    private final Consumer<Group> groupCreatedPublisher;

    // Khởi tạo GroupManager có local repo và publisher để đồng bộ group mới lên bootstrap
    public GroupManager(GroupRepository groupRepository, Consumer<Group> groupCreatedPublisher) {
        this.groupRepository = groupRepository;
        this.groupCreatedPublisher = groupCreatedPublisher;
        if (groupRepository != null) {
            for (Group group : groupRepository.findAll()) {
                groups.put(group.getGroupId(), group);
            }
            log.info("GroupManager đã nạp nhóm local. sốLượng={}", groups.size());
        }
    }

    // Tạo nhóm mới và thêm danh sách thành viên ban đầu nếu có
    public Group createGroup(String name, Collection<PeerInfo> members) {
        Group group = new Group(name);
        if (members != null) {
            members.forEach(group::addMember);
        }
        groups.put(group.getGroupId(), group);
        saveGroup(group);
        publishGroup(group);
        log.info("Đã tạo nhóm id={}, tên={}, sốThànhViên={}",
                group.getGroupId(), group.getName(), group.getMembers().size());
        return group;
    }

    // Thêm thành viên vào group đã tồn tại và lưu lại local cache
    public Group addMembers(String groupId, Collection<PeerInfo> members) {
        Group group = groups.get(groupId);
        if (group == null) {
            log.warn("Không thể thêm thành viên vì không tìm thấy groupId={}", groupId);
            return null;
        }
        int before = group.getMembers().size();
        if (members != null) {
            members.forEach(group::addMember);
        }
        int added = group.getMembers().size() - before;
        saveGroup(group);
        log.info("Đã thêm thành viên vào nhóm local. groupId={}, sốThànhViênThêm={}", groupId, added);
        return group;
    }

    // Tạo hoặc cập nhật group local khi nhận được GROUP_CHAT trực tiếp từ peer khác
    public Group ensureLocalGroup(String groupId, String name, Collection<PeerInfo> members) {
        Group group = groups.get(groupId);
        if (group == null) {
            group = new Group(groupId, name, members);
            groups.put(group.getGroupId(), group);
            log.info("Đã tạo nhóm local từ tin nhắn nhận vào. groupId={}, tên={}",
                    group.getGroupId(), group.getName());
        } else if (members != null) {
            members.forEach(group::addMember);
            log.debug("Đã refresh thành viên nhóm local từ tin nhắn nhận vào. groupId={}, sốThànhViên={}",
                    group.getGroupId(), group.getMembers().size());
        }
        saveGroup(group);
        return group;
    }

    // Đồng bộ group local bằng snapshot membership đầy đủ từ peer khác (GROUP_MEMBERS_SYNC)
    public Group syncMembers(String groupId, String name, Collection<PeerInfo> members) {
        Group group = groups.get(groupId);
        if (group == null) {
            group = new Group(groupId, name, members);
            groups.put(group.getGroupId(), group);
            log.info("Đã tạo nhóm local từ GROUP_MEMBERS_SYNC. groupId={}, tên={}",
                    group.getGroupId(), group.getName());
        } else {
            group.replaceMembers(members);
            log.info("Đã cập nhật membership nhóm từ GROUP_MEMBERS_SYNC. groupId={}, sốThànhViên={}",
                    groupId, group.getMembers().size());
        }
        saveGroup(group);
        return group;
    }

    // Tìm group theo groupId để gửi tin hoặc hiển thị thông tin nhóm
    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    // Lấy toàn bộ nhóm hiện có ở runtime
    public Collection<Group> getAllGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    // Thay thế local group cache bằng danh sách group bootstrap trả về
    public void replaceAll(Collection<Group> authoritativeGroups) {
        groups.clear();
        if (authoritativeGroups != null) {
            for (Group group : authoritativeGroups) {
                groups.put(group.getGroupId(), group);
            }
        }
        if (groupRepository != null) {
            groupRepository.saveAll(groups.values());
        }
        log.info("GroupManager đã thay nhóm bằng dữ liệu bootstrap. sốLượng={}", groups.size());
    }

    // Lưu group mới/cập nhật xuống groups.json nếu local repo được cấu hình
    private void saveGroup(Group group) {
        if (groupRepository != null) {
            groupRepository.save(group);
        }
    }

    // Publish group mới lên bootstrap nếu PeerNode cấu hình publisher
    private void publishGroup(Group group) {
        if (groupCreatedPublisher != null) {
            groupCreatedPublisher.accept(group);
        }
    }
}
