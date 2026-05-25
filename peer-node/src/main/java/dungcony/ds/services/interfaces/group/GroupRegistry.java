package dungcony.ds.services.interfaces.group;

import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;

/**
 * Contract quản lý vòng đời các group chat runtime.
 * Tạo ra để tuân thủ DIP — các service phụ thuộc vào abstraction
 * này thay vì concrete class GroupManager.
 */
public interface GroupRegistry {

    // Tạo nhóm mới với danh sách thành viên ban đầu
    Group createGroup(String name, Collection<PeerInfo> members);

    // Thêm thành viên vào nhóm đã tồn tại
    Group addMembers(String groupId, Collection<PeerInfo> members);

    // Tạo hoặc cập nhật nhóm local khi nhận được GROUP_CHAT trực tiếp
    Group ensureLocalGroup(String groupId, String name, Collection<PeerInfo> members);

    // Đồng bộ nhóm bằng snapshot membership đầy đủ từ GROUP_MEMBERS_SYNC
    Group syncMembers(String groupId, String name, Collection<PeerInfo> members);

    // Tìm nhóm theo groupId
    Group getGroup(String groupId);

    // Lấy toàn bộ nhóm hiện có
    Collection<Group> getAllGroups();

    // Thay thế toàn bộ cache nhóm bằng danh sách authoritative từ bootstrap
    void replaceAll(Collection<Group> groups);
}
