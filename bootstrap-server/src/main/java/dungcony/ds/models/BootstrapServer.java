package dungcony.ds.models;

import com.google.gson.Gson;
import dungcony.ds.config.Config;
import dungcony.ds.entities.GroupEntity;
import dungcony.ds.entities.GroupMemberEntity;
import dungcony.ds.entities.OfflineMessageEntity;
import dungcony.ds.repositories.Conn;
import dungcony.ds.repositories.Init;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bootstrap server (tracker) lắng nghe kết nối TCP từ các peer.
 * Mỗi request là một dòng text: {@code COMMAND [JSON_PAYLOAD]}.
 *
 * <p>Luồng xử lý command:</p>
 * <pre>
 *   ── Quản lý peer & trạng thái online ──
 *      REGISTER  → lưu user vào DB (chưa online)
 *      JOIN      → đánh dấu online, trả danh sách peer + offline messages
 *      LEAVE     → đánh dấu offline
 *      LIST      → trả danh sách peer đang online
 *
 *   ── Offline messaging ──
 *      STORE_OFFLINE → lưu tin nhắn khi receiver offline
 *
 *   ── Quản lý group ──
 *      CREATE_GROUP       → tạo group metadata
 *      LIST_GROUPS        → lấy danh sách group
 *      ADD_GROUP_MEMBER   → thêm member vào group
 *      REMOVE_GROUP_MEMBER → xóa member khỏi group
 *      LIST_GROUP_MEMBERS  → lấy danh sách member của group
 * </pre>
 */
@Slf4j
public class BootstrapServer {

    private final int port;
    private final PeerRegistry registry;
    private final Gson gson = new Gson();
    private final ExecutorService connectionPool = Executors.newCachedThreadPool();
    private volatile boolean running;
    private ServerSocket serverSocket;

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

    // Bắt đầu vòng lặp accept request từ các peer, mỗi kết nối xử lý trên thread pool.
    public void start() {
        running = true;
        try (ServerSocket openedSocket = new ServerSocket(port)) {
            serverSocket = openedSocket;
            log.info("Bootstrap server đang lắng nghe trên cổng {}", port);
            while (running) {
                Socket socket = openedSocket.accept();
                if (!running || connectionPool.isShutdown()) {
                    socket.close();
                    break;
                }
                log.debug("Bootstrap đã nhận kết nối từ {}", socket.getRemoteSocketAddress());
                connectionPool.submit(() -> handle(socket));
            }
        } catch (IOException e) {
            if (running) {
                log.error("Bootstrap server đã dừng: {}", e.getMessage());
            }
        }
    }

    // Đánh dấu server dừng nhận request mới và tắt thread pool.
    public void stop() {
        running = false;
        log.info("Bootstrap server đã được đánh dấu dừng trên cổng {}", port);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        connectionPool.shutdownNow();
    }

    // Đọc một dòng command từ socket, phân loại và xử lý tương ứng.
    private void handle(Socket socket) {
        try (Socket accepted = socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(
                     accepted.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                log.warn("Bootstrap nhận request rỗng.");
                return;
            }
            String[] parts   = line.split(" ", 2);
            String command   = parts[0].toUpperCase(Locale.ROOT);
            String payload   = parts.length > 1 ? parts[1] : "";
            log.info("Bootstrap nhận command={}", command);

            switch (command) {

                // ── Quản lý peer & trạng thái online ─────────────────────────────
                case "REGISTER" -> {
                    PeerInfo peerInfo = gson.fromJson(payload, PeerInfo.class);
                    registry.register(peerInfo);
                    log.info("Bootstrap REGISTER userId={}, tênHiểnThị={}",
                            peerInfo == null ? "null" : peerInfo.getId(),
                            peerInfo == null ? "null" : peerInfo.getName());
                    writer.println("OK");
                }
                case "JOIN" -> {
                    PeerInfo peerInfo = gson.fromJson(payload, PeerInfo.class);
                    registry.join(peerInfo);
                    String receiverId = peerInfo == null ? "" : peerInfo.getId();
                    Collection<OfflineMessageEntity> offlineMessages = registry.drainOfflineMessages(receiverId);
                    log.info("Bootstrap JOIN peer={}, tổngPeer={}",
                            peerInfo == null ? "null" : peerInfo.addressKey(), registry.list().size());
                    writer.println(gson.toJson(new JoinResponse(registry.list(), offlineMessages)));
                }
                case "LEAVE" -> {
                    registry.leave(payload.trim());
                    log.info("Bootstrap LEAVE peerKey={}, tổngPeer={}", payload.trim(), registry.list().size());
                    writer.println("OK");
                }
                case "LIST" -> {
                    Collection<PeerInfo> peers = registry.list();
                    log.info("Bootstrap LIST tổngPeer={}", peers.size());
                    writer.println(gson.toJson(peers));
                }

                // ── Offline messaging ─────────────────────────────────────────────
                case "STORE_OFFLINE" -> {
                    OfflineMessageEntity message = gson.fromJson(payload, OfflineMessageEntity.class);
                    registry.storeOfflineMessage(message);
                    writer.println("OK");
                }

                // ── Quản lý group ─────────────────────────────────────────────────
                case "CREATE_GROUP" -> {
                    GroupEntity groupEntity = gson.fromJson(payload, GroupEntity.class);
                    registry.createGroup(groupEntity);
                    writer.println("OK");
                }
                case "LIST_GROUPS" -> {
                    writer.println(gson.toJson(registry.listGroups()));
                }
                case "ADD_GROUP_MEMBER" -> {
                    GroupMemberEntity memberEntity = gson.fromJson(payload, GroupMemberEntity.class);
                    registry.addGroupMember(memberEntity);
                    writer.println("OK");
                }
                case "REMOVE_GROUP_MEMBER" -> {
                    GroupMemberEntity memberEntity = gson.fromJson(payload, GroupMemberEntity.class);
                    registry.removeGroupMember(memberEntity.getGroupId(), memberEntity.getUserId());
                    writer.println("OK");
                }
                case "LIST_GROUP_MEMBERS" -> {
                    writer.println(gson.toJson(registry.listGroupMembers(payload.trim())));
                }

                default -> {
                    log.warn("Bootstrap nhận command không hỗ trợ={}", command);
                    writer.println("UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            log.warn("Bootstrap xử lý request thất bại: {}", e.getMessage());
        }
    }
}
