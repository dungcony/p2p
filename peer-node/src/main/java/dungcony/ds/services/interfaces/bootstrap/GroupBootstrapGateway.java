package dungcony.ds.services.interfaces.bootstrap;

import dungcony.ds.dtos.GroupMemberPayload;
import dungcony.ds.dtos.GroupPayload;
import dungcony.ds.model.Group;

import java.util.Collection;

/**
 * Giao tiếp với bootstrap-server cho quản lý group chat.
 * Tách ra từ BootstrapGateway theo ISP — chỉ dùng bởi
 * BootstrapGroupImpl và các service liên quan đến group sync.
 */
public interface GroupBootstrapGateway {

    boolean createGroup(Group group, String createdBy);

    boolean addGroupMember(String groupId, String userId);

    Collection<GroupPayload> listGroups();

    Collection<GroupMemberPayload> listGroupMembers(String groupId);
}
