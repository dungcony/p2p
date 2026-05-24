package dungcony.ds.utils;

import dungcony.ds.model.Group;
import dungcony.ds.model.PeerInfo;

public final class GroupConversationHelper {

    /**
     * Tiền tố dùng để phân biệt group key khỏi conversation 1-1.
     */
    public static final String GROUP_PREFIX = "group:";

    private GroupConversationHelper() {
        // utility class — không khởi tạo
    }

    /**
     * Tạo history key cho group, đảm bảo không trùng với addressKey của peer 1-1.
     *
     * @param groupId ID của group
     * @return key dạng "group:<groupId>"
     */
    public static String historyKey(String groupId) {
        return GROUP_PREFIX + groupId;
    }

    /**
     * Tạo PeerInfo ảo đại diện group để lưu conversation trong MessageHistoryService.
     * Dùng host ảo "group:<groupId>" để phân biệt với peer thật.
     *
     * @param group group cần tạo conversation peer
     * @return PeerInfo ảo với host = "group:<groupId>", port = 0
     */
    public static PeerInfo conversationPeer(Group group) {
        return new PeerInfo(
                group.getGroupId(),
                group.getName(),
                GROUP_PREFIX + group.getGroupId(),
                0,
                true
        );
    }

    /**
     * Kiểm tra host có phải là host ảo của group không.
     * Dùng để lọc group pseudo-peer khỏi danh sách peer thật.
     *
     * @param host host cần kiểm tra
     * @return true nếu là host ảo dạng "group:..."
     */
    public static boolean isGroupHost(String host) {
        return host != null && host.startsWith(GROUP_PREFIX);
    }
}
