package dungcony.ds.model;

import lombok.extern.slf4j.Slf4j;


import com.google.gson.Gson;
import dungcony.ds.dtos.GroupMemberPayload;
import dungcony.ds.dtos.GroupPayload;
import dungcony.ds.dtos.JoinResponse;
import dungcony.ds.dtos.OfflineMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
public class BootstrapClient {
private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final String host;
    private final int port;
    private final Gson gson = new Gson();

    // Khởi tạo client kết nối tới bootstrap-server/tracker.
    public BootstrapClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // Gửi REGISTER để bootstrap-server lưu user_id và display_name của peer.
    public boolean register(PeerInfo peerInfo) {
        String response = request("REGISTER", peerInfo);
        boolean success = "OK".equalsIgnoreCase(response);
        log.info("Kết quả Bootstrap REGISTER={}, peerId={}, response={}", success, peerInfo.getId(), response);
        return success;
    }

    // Gửi JOIN để đánh dấu peer online và nhận danh sách peer đang online.
    public JoinResponse join(PeerInfo peerInfo) {
        JoinResponse joinResponse = joinOrNull(peerInfo);
        return joinResponse == null ? JoinResponse.empty() : joinResponse;
    }

    // Gửi JOIN và trả về null nếu bootstrap không phản hồi hợp lệ.
    public JoinResponse joinOrNull(PeerInfo peerInfo) {
        String response = request("JOIN", peerInfo);
        if (response == null || response.isBlank()) {
            log.warn("Bootstrap JOIN trả response rỗng.");
            return null;
        }
        try {
            JoinResponse joinResponse = gson.fromJson(response, JoinResponse.class);
            log.info("Bootstrap JOIN nhận sốPeer={}, tinOffline={}", joinResponse.getOnlinePeers().size(), joinResponse.getOfflineMessages().size());
            return joinResponse;
        } catch (RuntimeException e) {
            log.error("Không thể parse Bootstrap JOIN response: {}", e.getMessage());
            return null;
        }
    }

    // Gửi tin nhắn offline lên bootstrap-server khi receiver đang mất kết nối trực tiếp.
    public boolean storeOffline(OfflineMessage message) {
        String response = request("STORE_OFFLINE", message);
        boolean success = "OK".equalsIgnoreCase(response);
        log.info("Kết quả Bootstrap STORE_OFFLINE={}, messageId={}, receiverId={}", success, message.messageId(), message.receiverId());
        return success;
    }

    // Tạo/cập nhật group metadata trên bootstrap-server.
    public boolean createGroup(Group group, String createdBy) {
        GroupPayload payload = new GroupPayload(
                group.getGroupId(),
                group.getName(),
                createdBy,
                System.currentTimeMillis()
        );
        String response = request("CREATE_GROUP", payload);
        boolean success = "OK".equalsIgnoreCase(response);
        log.info("Kết quả Bootstrap CREATE_GROUP={}, groupId={}, response={}", success, group.getGroupId(), response);
        return success;
    }

    // Thêm user/peer vào group trên bootstrap-server.
    public boolean addGroupMember(String groupId, String userId) {
        GroupMemberPayload payload = new GroupMemberPayload(groupId, userId, System.currentTimeMillis());
        String response = request("ADD_GROUP_MEMBER", payload);
        boolean success = "OK".equalsIgnoreCase(response);
        log.info("Kết quả Bootstrap ADD_GROUP_MEMBER={}, groupId={}, userId={}", success, groupId, userId);
        return success;
    }

    // Lấy danh sách group metadata từ bootstrap-server.
    public Collection<GroupPayload> listGroups() {
        String response = requestRaw("LIST_GROUPS", "");
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        GroupPayload[] groups = gson.fromJson(response, GroupPayload[].class);
        List<GroupPayload> result = groups == null ? Collections.emptyList() : Arrays.asList(groups);
        log.info("Bootstrap LIST_GROUPS sốLượng={}", result.size());
        return result;
    }

    // Lấy danh sách member user_id của một group từ bootstrap-server.
    public Collection<GroupMemberPayload> listGroupMembers(String groupId) {
        String response = requestRaw("LIST_GROUP_MEMBERS", groupId);
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        GroupMemberPayload[] members = gson.fromJson(response, GroupMemberPayload[].class);
        List<GroupMemberPayload> result = members == null ? Collections.emptyList() : Arrays.asList(members);
        log.info("Bootstrap LIST_GROUP_MEMBERS groupId={}, sốLượng={}", groupId, result.size());
        return result;
    }

    // Gửi LEAVE để bootstrap-server xóa địa chỉ online của peer hiện tại.
    public void leave(String peerKey) {
        String response = requestRaw("LEAVE", peerKey);
        log.info("Bootstrap LEAVE peerKey={}, response={}", peerKey, response);
    }

    // Lấy danh sách peer online từ bootstrap-server khi cần refresh thủ công.
    public Collection<PeerInfo> list() {
        Collection<PeerInfo> peers = listOrNull();
        return peers == null ? Collections.emptyList() : peers;
    }

    // Lấy danh sách peer online, trả null nếu không kết nối được bootstrap-server.
    public Collection<PeerInfo> listOrNull() {
        String response = requestRaw("LIST", "");
        if (response == null) {
            return null;
        }
        if (response.isBlank()) {
            return Collections.emptyList();
        }
        PeerInfo[] peers = gson.fromJson(response, PeerInfo[].class);
        return peers == null ? Collections.emptyList() : java.util.Arrays.asList(peers);
    }

    // Gửi request có payload JSON tới bootstrap-server.
    private String request(String command, Object payload) {
        return requestRaw(command, gson.toJson(payload));
    }

    // Gửi một dòng command tới bootstrap-server và đọc một dòng response.
    private String requestRaw(String command, String payload) {
        String line = payload == null || payload.isBlank() ? command : command + " " + payload;
        try (Socket socket = new Socket()) {
            log.debug("Bắt đầu kết nối bootstrap {}:{}, command={}", host, port, command);
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                writer.println(line);
                String response = reader.readLine();
                log.debug("Bootstrap response command={}, response={}", command, response);
                return response;
            }
        } catch (IOException e) {
            log.warn("Request bootstrap thất bại. command={}, địaChỉ={}:{}, lỗi={}", command, host, port, e.getMessage());
            return null;
        }
    }

}
