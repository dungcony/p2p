package dungcony.ds.model;

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

public class BootstrapClient {
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final String host;
    private final int port;
    private final Gson gson = new Gson();

    /**
     * Khoi tao client ket noi toi bootstrap-server/tracker.
     */
    public BootstrapClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Gui REGISTER de bootstrap-server luu user_id va display_name cua peer.
     */
    public boolean register(PeerInfo peerInfo) {
        String response = request("REGISTER", peerInfo);
        boolean success = "OK".equalsIgnoreCase(response);
        System.out.println("[INFO] Kết quả Bootstrap REGISTER=" + success
                + ", peerId=" + peerInfo.getId() + ", response=" + response);
        return success;
    }

    /**
     * Gui JOIN de danh dau peer online va nhan danh sach peer dang online.
     */
    public JoinResponse join(PeerInfo peerInfo) {
        JoinResponse joinResponse = joinOrNull(peerInfo);
        return joinResponse == null ? JoinResponse.empty() : joinResponse;
    }

    /**
     * Gui JOIN va tra ve null neu bootstrap khong phan hoi hop le.
     */
    public JoinResponse joinOrNull(PeerInfo peerInfo) {
        String response = request("JOIN", peerInfo);
        if (response == null || response.isBlank()) {
            System.out.println("[WARN] Bootstrap JOIN trả response rỗng.");
            return null;
        }
        try {
            JoinResponse joinResponse = gson.fromJson(response, JoinResponse.class);
            System.out.println("[INFO] Bootstrap JOIN nhận sốPeer="
                    + joinResponse.getOnlinePeers().size()
                    + ", tinOffline=" + joinResponse.getOfflineMessages().size());
            return joinResponse;
        } catch (RuntimeException e) {
            System.out.println("[ERROR] Không thể parse Bootstrap JOIN response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gui tin nhan offline len bootstrap-server khi receiver dang mat ket noi truc tiep.
     */
    public boolean storeOffline(OfflineMessage message) {
        String response = request("STORE_OFFLINE", message);
        boolean success = "OK".equalsIgnoreCase(response);
        System.out.println("[INFO] Kết quả Bootstrap STORE_OFFLINE=" + success
                + ", messageId=" + message.messageId()
                + ", receiverId=" + message.receiverId());
        return success;
    }

    /**
     * Tao/cap nhat group metadata tren bootstrap-server.
     */
    public boolean createGroup(Group group, String createdBy) {
        GroupPayload payload = new GroupPayload(
                group.getGroupId(),
                group.getName(),
                createdBy,
                System.currentTimeMillis()
        );
        String response = request("CREATE_GROUP", payload);
        boolean success = "OK".equalsIgnoreCase(response);
        System.out.println("[INFO] Kết quả Bootstrap CREATE_GROUP=" + success
                + ", groupId=" + group.getGroupId() + ", response=" + response);
        return success;
    }

    /**
     * Them user/peer vao group tren bootstrap-server.
     */
    public boolean addGroupMember(String groupId, String userId) {
        GroupMemberPayload payload = new GroupMemberPayload(groupId, userId, System.currentTimeMillis());
        String response = request("ADD_GROUP_MEMBER", payload);
        boolean success = "OK".equalsIgnoreCase(response);
        System.out.println("[INFO] Kết quả Bootstrap ADD_GROUP_MEMBER=" + success
                + ", groupId=" + groupId + ", userId=" + userId);
        return success;
    }

    /**
     * Lay danh sach group metadata tu bootstrap-server.
     */
    public Collection<GroupPayload> listGroups() {
        String response = requestRaw("LIST_GROUPS", "");
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        GroupPayload[] groups = gson.fromJson(response, GroupPayload[].class);
        List<GroupPayload> result = groups == null ? Collections.emptyList() : Arrays.asList(groups);
        System.out.println("[INFO] Bootstrap LIST_GROUPS sốLượng=" + result.size());
        return result;
    }

    /**
     * Lay danh sach member user_id cua mot group tu bootstrap-server.
     */
    public Collection<GroupMemberPayload> listGroupMembers(String groupId) {
        String response = requestRaw("LIST_GROUP_MEMBERS", groupId);
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        GroupMemberPayload[] members = gson.fromJson(response, GroupMemberPayload[].class);
        List<GroupMemberPayload> result = members == null ? Collections.emptyList() : Arrays.asList(members);
        System.out.println("[INFO] Bootstrap LIST_GROUP_MEMBERS groupId=" + groupId
                + ", sốLượng=" + result.size());
        return result;
    }

    /**
     * Gui LEAVE de bootstrap-server xoa dia chi online cua peer hien tai.
     */
    public void leave(String peerKey) {
        String response = requestRaw("LEAVE", peerKey);
        System.out.println("[INFO] Bootstrap LEAVE peerKey=" + peerKey + ", response=" + response);
    }

    /**
     * Lay danh sach peer online tu bootstrap-server khi can refresh thu cong.
     */
    public Collection<PeerInfo> list() {
        Collection<PeerInfo> peers = listOrNull();
        return peers == null ? Collections.emptyList() : peers;
    }

    /**
     * Lay danh sach peer online, tra null neu khong ket noi duoc bootstrap-server.
     */
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

    /**
     * Gui request co payload JSON toi bootstrap-server.
     */
    private String request(String command, Object payload) {
        return requestRaw(command, gson.toJson(payload));
    }

    /**
     * Gui mot dong command toi bootstrap-server va doc mot dong response.
     */
    private String requestRaw(String command, String payload) {
        String line = payload == null || payload.isBlank() ? command : command + " " + payload;
        try (Socket socket = new Socket()) {
            System.out.println("[DEBUG] Bắt đầu kết nối bootstrap " + host + ":" + port + ", command=" + command);
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                writer.println(line);
                String response = reader.readLine();
                System.out.println("[DEBUG] Bootstrap response command=" + command + ", response=" + response);
                return response;
            }
        } catch (IOException e) {
            System.out.println("[WARN] Request bootstrap thất bại. command=" + command
                    + ", địaChỉ=" + host + ":" + port + ", lỗi=" + e.getMessage());
            return null;
        }
    }

}
