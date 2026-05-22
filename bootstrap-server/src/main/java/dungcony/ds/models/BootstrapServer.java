package dungcony.ds.models;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapServer.class);
private final int port;
    private final PeerRegistry registry;
    private final Gson gson = new Gson();
    private volatile boolean running;

    // Khởi tạo tracker lắng nghe trên port được truyền vào.
    public BootstrapServer(int port) {
        this(port, Config.load().getDatabasePath());
    }

    // Khởi tạo tracker với port và đường dẫn SQLite database.
    public BootstrapServer(int port, java.nio.file.Path databasePath) {
        this.port = port;
        Conn conn = new Conn(databasePath);
        new Init(conn).initializeSchema();
        this.registry = new PeerRegistry(conn);
    }

    // Cho phép chạy BootstrapServer độc lập từ command line.
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        Config config = Config.load();
        new BootstrapServer(port, config.getDatabasePath()).start();
    }

    // Bắt đầu vòng lặp accept request REGISTER/JOIN/LEAVE/LIST từ các peer.
    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Bootstrap server đang lắng nghe trên cổng " + port);
            while (running) {
                Socket socket = serverSocket.accept();
                LOGGER.debug("Bootstrap đã nhận kết nối từ "
                        + socket.getRemoteSocketAddress());
                Thread handler = new Thread(() -> handle(socket), "BootstrapHandler");
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.error("Bootstrap server đã dừng: " + e.getMessage());
            }
        }
    }

    // Đánh dấu server dừng nhận request mới.
    public void stop() {
        running = false;
        LOGGER.info("Bootstrap server đã được đánh dấu dừng trên cổng " + port);
    }

    // Xử lý một request tracker: REGISTER để lưu user, JOIN để online, LEAVE để rời mạng, LIST để lấy danh sách peer.
    private void handle(Socket socket) {
        try (Socket accepted = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(accepted.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                LOGGER.warn("Bootstrap nhận request rỗng.");
                return;
            }

            String[] parts = line.split(" ", 2);
            String command = parts[0].toUpperCase(Locale.ROOT);
            String payload = parts.length > 1 ? parts[1] : "";
            LOGGER.info("Bootstrap nhận command=" + command);

            switch (command) {
                case "REGISTER" -> {
                    PeerInfo peerInfo = gson.fromJson(payload, PeerInfo.class);
                    registry.register(peerInfo);
                    LOGGER.info("Bootstrap REGISTER userId="
                            + (peerInfo == null ? "null" : peerInfo.getId())
                            + ", tênHiểnThị=" + (peerInfo == null ? "null" : peerInfo.getName()));
                    writer.println("OK");
                    return;
                }
                case "JOIN" -> {
                    PeerInfo peerInfo = gson.fromJson(payload, PeerInfo.class);
                    registry.join(peerInfo);
                    String receiverId = peerInfo == null ? "" : peerInfo.getId();
                    Collection<OfflineMessageEntity> offlineMessages = registry.drainOfflineMessages(receiverId);
                    LOGGER.info("Bootstrap JOIN peer="
                            + (peerInfo == null ? "null" : peerInfo.addressKey())
                            + ", tổngPeer=" + registry.list().size());
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
                    LOGGER.info("Bootstrap LEAVE peerKey=" + payload.trim()
                            + ", tổngPeer=" + registry.list().size());
                    writer.println("OK");
                    return;
                }
                case "LIST" -> {
                    Collection<PeerInfo> peers = registry.list();
                    LOGGER.info("Bootstrap LIST tổngPeer=" + peers.size());
                    writer.println(gson.toJson(peers));
                    return;
                }
            }

            LOGGER.warn("Bootstrap nhận command không hỗ trợ=" + command);
            writer.println("UNKNOWN_COMMAND");
        } catch (IOException e) {
            LOGGER.warn("Bootstrap xử lý request thất bại: " + e.getMessage());
        }
    }
}
