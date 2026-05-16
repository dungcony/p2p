package dungcony.ds.models;

import com.google.gson.Gson;
import dungcony.ds.config.Config;
import dungcony.ds.entities.GroupEntity;
import dungcony.ds.entities.GroupMemberEntity;
import dungcony.ds.entities.OfflineMessageEntity;
import dungcony.ds.repositories.Conn;
import dungcony.ds.repositories.Init;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;

public class BootstrapServer {
    private final int port;
    private final PeerRegistry registry;
    private final Gson gson = new Gson();
    private volatile boolean running;

    /**
     * Khởi tạo tracker lắng nghe trên port được truyền vào.
     */
    public BootstrapServer(int port) {
        this(port, Config.load().getDatabasePath());
    }

    /**
     * Khởi tạo tracker với port và đường dẫn SQLite database.
     */
    public BootstrapServer(int port, java.nio.file.Path databasePath) {
        this.port = port;
        Conn conn = new Conn(databasePath);
        new Init(conn).initializeSchema();
        this.registry = new PeerRegistry(conn);
    }

    /**
     * Cho phép chạy BootstrapServer độc lập từ command line.
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        Config config = Config.load();
        new BootstrapServer(port, config.getDatabasePath()).start();
    }

    /**
     * Bắt đầu vòng lặp accept request REGISTER/JOIN/LEAVE/LIST từ các peer.
     */
    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[INFO] Bootstrap server listening on port " + port);
            while (running) {
                Socket socket = serverSocket.accept();
                System.out.println("[DEBUG] Bootstrap accepted connection from "
                        + socket.getRemoteSocketAddress());
                Thread handler = new Thread(() -> handle(socket), "BootstrapHandler");
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[ERROR] Bootstrap server stopped: " + e.getMessage());
            }
        }
    }

    /**
     * Đánh dấu server dừng nhận request mới.
     */
    public void stop() {
        running = false;
        System.out.println("[INFO] Bootstrap server marked as stopped on port " + port);
    }

    /**
     * Xử lý một request tracker: REGISTER để lưu user, JOIN để online, LEAVE để rời mạng, LIST để lấy danh sách peer.
     */
    private void handle(Socket socket) {
        try (Socket accepted = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(accepted.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                System.out.println("[WARN] Bootstrap received empty request.");
                return;
            }

            String[] parts = line.split(" ", 2);
            String command = parts[0].toUpperCase(Locale.ROOT);
            String payload = parts.length > 1 ? parts[1] : "";
            System.out.println("[INFO] Bootstrap request command=" + command);

            switch (command) {
                case "REGISTER" -> {
                    PeerInfo peerInfo = gson.fromJson(payload, PeerInfo.class);
                    registry.register(peerInfo);
                    System.out.println("[INFO] Bootstrap REGISTER userId="
                            + (peerInfo == null ? "null" : peerInfo.getId())
                            + ", displayName=" + (peerInfo == null ? "null" : peerInfo.getName()));
                    writer.println("OK");
                    return;
                }
                case "JOIN" -> {
                    PeerInfo peerInfo = gson.fromJson(payload, PeerInfo.class);
                    registry.join(peerInfo);
                    String receiverId = peerInfo == null ? "" : peerInfo.getId();
                    Collection<OfflineMessageEntity> offlineMessages = registry.drainOfflineMessages(receiverId);
                    System.out.println("[INFO] Bootstrap JOIN peer="
                            + (peerInfo == null ? "null" : peerInfo.addressKey())
                            + ", totalPeers=" + registry.list().size());
                    writer.println(gson.toJson(new JoinResponse(registry.list(), offlineMessages)));
                    return;
                }
                case "STORE_OFFLINE" -> {
                    OfflineMessageEntity message = gson.fromJson(payload, OfflineMessageEntity.class);
                    registry.storeOfflineMessage(message);
                    writer.println("OK");
                    return;
                }
                case "CREATE_GROUP" -> {
                    GroupEntity groupEntity = gson.fromJson(payload, GroupEntity.class);
                    registry.createGroup(groupEntity);
                    writer.println("OK");
                    return;
                }
                case "LIST_GROUPS" -> {
                    writer.println(gson.toJson(registry.listGroups()));
                    return;
                }
                case "ADD_GROUP_MEMBER" -> {
                    GroupMemberEntity memberEntity = gson.fromJson(payload, GroupMemberEntity.class);
                    registry.addGroupMember(memberEntity);
                    writer.println("OK");
                    return;
                }
                case "REMOVE_GROUP_MEMBER" -> {
                    GroupMemberEntity memberEntity = gson.fromJson(payload, GroupMemberEntity.class);
                    registry.removeGroupMember(memberEntity.getGroupId(), memberEntity.getUserId());
                    writer.println("OK");
                    return;
                }
                case "LIST_GROUP_MEMBERS" -> {
                    writer.println(gson.toJson(registry.listGroupMembers(payload.trim())));
                    return;
                }
                case "LEAVE" -> {
                    registry.leave(payload.trim());
                    System.out.println("[INFO] Bootstrap LEAVE peerKey=" + payload.trim()
                            + ", totalPeers=" + registry.list().size());
                    writer.println("OK");
                    return;
                }
                case "LIST" -> {
                    Collection<PeerInfo> peers = registry.list();
                    System.out.println("[INFO] Bootstrap LIST totalPeers=" + peers.size());
                    writer.println(gson.toJson(peers));
                    return;
                }
            }

            System.out.println("[WARN] Bootstrap unknown command=" + command);
            writer.println("UNKNOWN_COMMAND");
        } catch (IOException e) {
            System.out.println("[WARN] Bootstrap request failed: " + e.getMessage());
        }
    }
}
