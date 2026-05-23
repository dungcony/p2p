package dungcony.ds.interfaces;

import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

import java.util.Collection;
import java.util.List;

// Đại diện use case quản lý group chat, gửi tin nhóm và đọc lịch sử nhóm
public interface GroupChatService {

    // Gửi một tin nhắn tới các thành viên hiện có của group
    void sendGroupMessage(String groupId, String content);

    // Tạo group mới từ danh sách peer được chọn và đồng bộ membership ban đầu
    Group createGroup(String name, Collection<PeerInfo> members);

    // Thêm thành viên vào group và đồng bộ membership mới
    Group addMembersToGroup(String groupId, Collection<PeerInfo> members);

    // Lấy toàn bộ group hiện có của peer local
    Collection<Group> getGroups();

    // Lấy lịch sử tin nhắn của một group theo groupId
    List<Message> getMessagesWithGroup(String groupId);

    // Lấy tin nhắn cuối cùng của group để UI hiển thị preview
    Message getLastGroupMessage(String groupId);
}
