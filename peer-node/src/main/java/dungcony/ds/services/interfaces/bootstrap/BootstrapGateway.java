package dungcony.ds.services.interfaces.bootstrap;

import dungcony.ds.dtos.GroupMemberPayload;
import dungcony.ds.dtos.GroupPayload;
import dungcony.ds.dtos.JoinResponse;
import dungcony.ds.dtos.OfflineMessage;
import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;

public interface BootstrapGateway {
    boolean register(PeerInfo peerInfo);

    JoinResponse join(PeerInfo peerInfo);

    JoinResponse joinOrNull(PeerInfo peerInfo);

    boolean storeOffline(OfflineMessage message);

    boolean createGroup(Group group, String createdBy);

    boolean addGroupMember(String groupId, String userId);

    Collection<GroupPayload> listGroups();

    Collection<GroupMemberPayload> listGroupMembers(String groupId);

    void leave(String peerKey);

    Collection<PeerInfo> list();

    Collection<PeerInfo> listOrNull();
}
