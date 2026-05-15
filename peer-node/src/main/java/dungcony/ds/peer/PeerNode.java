package dungcony.ds.peer;

import dungcony.ds.model.Group;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.network.TCPClient;
import dungcony.ds.network.TCPServer;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PeerNode {
    public static final int DEFAULT_PORT = 5001;

    private final PeerInfo localPeer;
    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> messageHistory = new ConcurrentHashMap<>();
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> peerChangeListeners = new CopyOnWriteArrayList<>();
    private final GroupManager groupManager = new GroupManager();
    private final MessageSender messageSender;
    private final TCPServer tcpServer;

    /**
     * Khởi tạo một peer cục bộ với tên định danh và port lắng nghe do người dùng nhập.
     */
    public PeerNode(String peerId, int port) {
        this.localPeer = new PeerInfo(peerId, resolveLocalHost(), port);
        TCPClient tcpClient = new TCPClient();
        this.messageSender = new MessageSender(tcpClient);
        this.tcpServer = new TCPServer(port, new MessageReceiver(this));
        System.out.println("[INFO] PeerNode initialized: id=" + localPeer.getId()
                + ", address=" + localPeer.addressKey());
    }

    /**
     * Khởi động vai trò nhận tin của peer bằng TCPServer trên một thread riêng.
     */
    public void start() {
        System.out.println("[INFO] Starting TCP listener for local peer " + localPeer.addressKey());
        Thread serverThread = new Thread(tcpServer::listen, "PeerNode-TCPServer-" + localPeer.getPort());
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Dừng TCPServer để peer ngừng nhận kết nối mới.
     */
    public void stop() {
        System.out.println("[INFO] Stopping PeerNode " + localPeer.addressKey());
        tcpServer.stop();
    }

    /**
     * Trả về thông tin định danh, host và port của peer hiện tại.
     */
    public PeerInfo getLocalPeer() {
        return localPeer;
    }

    /**
     * Đăng ký callback để UI được thông báo khi có tin nhắn mới.
     */
    public void addMessageListener(MessageListener listener) {
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    /**
     * Đăng ký callback để UI refresh danh sách peer khi trạng thái peer thay đổi.
     */
    public void addPeerChangeListener(Runnable listener) {
        if (listener != null) {
            peerChangeListeners.add(listener);
        }
    }

    /**
     * Thêm một peer đã biết vào bộ nhớ runtime, thường được gọi từ màn Add Friend hoặc scanner.
     */
    public PeerInfo addKnownPeer(String name, String hostAndMaybePort) {
        PeerInfo peerInfo = parsePeer(name, hostAndMaybePort);
        if (peerInfo == null) {
            System.out.println("[WARN] Ignored addKnownPeer because address is blank.");
            return null;
        }
        if (isSelfPeer(peerInfo)) {
            System.out.println("[WARN] Ignored addKnownPeer because target is local peer: "
                    + peerInfo.addressKey());
            return null;
        }
        peers.put(peerInfo.addressKey(), peerInfo);
        System.out.println("[INFO] Added known peer: id=" + peerInfo.getId()
                + ", address=" + peerInfo.addressKey());
        notifyPeersChanged();
        return peerInfo;
    }

    /**
     * Lấy danh sách peer mà node hiện đang biết để UI hiển thị trong ChatList.
     */
    public Collection<PeerInfo> getKnownPeers() {
        return Collections.unmodifiableCollection(peers.values());
    }

    /**
     * Gửi heartbeat tới một peer để kiểm tra peer đó còn online hay không.
     */
    public boolean checkUserIsOnline(String hostAndMaybePort) {
        PeerInfo peerInfo = resolvePeer(hostAndMaybePort);
        if (peerInfo == null) {
            System.out.println("[WARN] Cannot check online status. Peer address is empty.");
            return false;
        }
        if (isSelfPeer(peerInfo)) {
            System.out.println("[WARN] Refusing heartbeat to local peer: " + peerInfo.addressKey());
            return false;
        }
        System.out.println("[DEBUG] Sending heartbeat to " + peerInfo.addressKey());
        boolean online = messageSender.send(peerInfo, Message.heartbeat(localPeer));
        peerInfo.setOnline(online);
        System.out.println("[INFO] Peer " + peerInfo.addressKey() + " online=" + online);
        notifyPeersChanged();
        return online;
    }

    /**
     * Gửi tin nhắn 1-1 trực tiếp tới peer đích, lưu lịch sử nếu nhận được ACK.
     */
    public boolean sendMessage(String content, String hostAndMaybePort) {
        if (content == null || content.isBlank()) {
            System.out.println("[WARN] Refusing to send blank message.");
            return false;
        }
        PeerInfo receiver = resolvePeer(hostAndMaybePort);
        if (receiver == null) {
            System.out.println("[WARN] Refusing to send message because target peer is blank.");
            return false;
        }
        if (isSelfPeer(receiver)) {
            System.out.println("[WARN] Refusing to send message to local peer: " + receiver.addressKey());
            return false;
        }

        Message message = Message.chat(localPeer, receiver, content);
        System.out.println("[INFO] Sending CHAT message id=" + message.getId()
                + " to=" + receiver.addressKey());
        boolean sent = messageSender.send(receiver, message);
        receiver.setOnline(sent);
        if (sent) {
            addMessage(receiver.addressKey(), message);
            notifyMessage(message);
            System.out.println("[INFO] CHAT message delivered and stored. id=" + message.getId());
        } else {
            System.out.println("[WARN] CHAT message failed after retries. id=" + message.getId()
                    + ", to=" + receiver.addressKey());
        }
        notifyPeersChanged();
        return sent;
    }

    /**
     * Gửi một tin nhắn tới tất cả thành viên của nhóm đã tạo.
     */
    public void sendGroupMessage(String groupId, String content) {
        Group group = groupManager.getGroup(groupId);
        if (group == null) {
            System.out.println("[WARN] Cannot send group message. Group not found: " + groupId);
            return;
        }
        Message message = Message.groupChat(localPeer, groupId, content);
        System.out.println("[INFO] Broadcasting GROUP_CHAT message id=" + message.getId()
                + " to group=" + groupId + ", members=" + group.getMembers().size());
        messageSender.broadcast(group, message);
    }

    /**
     * Cung cấp GroupManager để tầng UI hoặc service khác quản lý nhóm chat.
     */
    public GroupManager getGroupManager() {
        return groupManager;
    }

    /**
     * Lấy lịch sử tin nhắn với một peer cụ thể theo địa chỉ host hoặc host:port.
     */
    public List<Message> getMessagesWithPeer(String hostAndMaybePort) {
        PeerInfo peerInfo = resolvePeer(hostAndMaybePort);
        String key = peerInfo == null ? hostAndMaybePort : peerInfo.addressKey();
        return new ArrayList<>(messageHistory.getOrDefault(key, Collections.emptyList()));
    }

    /**
     * Lấy tin nhắn cuối cùng với một peer để hiển thị preview trong danh sách chat.
     */
    public Message getLastMessage(String hostAndMaybePort) {
        List<Message> messages = getMessagesWithPeer(hostAndMaybePort);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    /**
     * Quét subnet LAN hiện tại bằng heartbeat để tìm các peer đang chạy cùng port.
     */
    public List<PeerInfo> discoverPeersOnLocalNetwork() {
        List<PeerInfo> discovered = new ArrayList<>();
        String localHost = localPeer.getHost();
        int lastDot = localHost.lastIndexOf('.');
        if (lastDot < 0) {
            System.out.println("[WARN] Cannot discover peers. Local host is not an IPv4 LAN address: " + localHost);
            return discovered;
        }

        String prefix = localHost.substring(0, lastDot + 1);
        System.out.println("[INFO] Starting LAN discovery on subnet " + prefix + "0/24 using port " + localPeer.getPort());
        List<Thread> probes = new ArrayList<>();
        for (int i = 1; i <= 254; i++) {
            String host = prefix + i;
            if (host.equals(localHost)) {
                continue;
            }
            Thread probe = new Thread(() -> {
                PeerInfo peerInfo = new PeerInfo(host, host, localPeer.getPort());
                if (messageSender.send(peerInfo, Message.heartbeat(localPeer))) {
                    peers.put(peerInfo.addressKey(), peerInfo);
                    System.out.println("[INFO] Discovered peer " + peerInfo.addressKey());
                    synchronized (discovered) {
                        discovered.add(peerInfo);
                    }
                }
            }, "PeerProbe-" + host);
            probe.setDaemon(true);
            probes.add(probe);
            probe.start();
        }

        for (Thread probe : probes) {
            try {
                probe.join(3500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        notifyPeersChanged();
        System.out.println("[INFO] LAN discovery completed. Found peers=" + discovered.size());
        return discovered;
    }

    /**
     * Xử lý tin nhắn đến từ network: cập nhật peer, lưu lịch sử và notify UI.
     */
    void onInboundMessage(Message message) {
        PeerInfo sender = new PeerInfo(message.getSenderId(), message.getSenderHost(), message.getSenderPort());
        peers.put(sender.addressKey(), sender);
        addMessage(sender.addressKey(), message);
        System.out.println("[INFO] Inbound " + message.getType() + " message stored. id=" + message.getId()
                + ", from=" + sender.addressKey());
        notifyPeersChanged();
        notifyMessage(message);
    }

    /**
     * Đánh dấu peer gửi heartbeat/JOIN là online trong danh sách peer đã biết.
     */
    void markPeerOnline(Message message) {
        PeerInfo sender = new PeerInfo(message.getSenderId(), message.getSenderHost(), message.getSenderPort());
        peers.put(sender.addressKey(), sender);
        System.out.println("[DEBUG] Marked peer online from " + message.getType()
                + ": " + sender.addressKey());
        notifyPeersChanged();
    }

    /**
     * Lưu tin nhắn vào lịch sử theo key của peer đối thoại.
     */
    private void addMessage(String peerKey, Message message) {
        messageHistory.computeIfAbsent(peerKey, ignored -> Collections.synchronizedList(new ArrayList<>())).add(message);
        System.out.println("[DEBUG] Message appended to history. peerKey=" + peerKey
                + ", messageId=" + message.getId());
    }

    /**
     * Tìm peer đã biết hoặc phân tích địa chỉ đầu vào thành PeerInfo tạm thời.
     */
    private PeerInfo resolvePeer(String hostAndMaybePort) {
        if (hostAndMaybePort == null || hostAndMaybePort.isBlank()) {
            System.out.println("[WARN] resolvePeer called with blank address.");
            return null;
        }
        PeerInfo existing = peers.get(hostAndMaybePort.trim());
        if (existing != null) {
            return existing;
        }
        PeerInfo parsed = parsePeer(hostAndMaybePort, hostAndMaybePort);
        return peers.getOrDefault(parsed.addressKey(), parsed);
    }

    /**
     * Chuyển chuỗi host hoặc host:port thành PeerInfo với port mặc định nếu không nhập port.
     */
    private PeerInfo parsePeer(String name, String hostAndMaybePort) {
        String value = hostAndMaybePort == null ? "" : hostAndMaybePort.trim();
        if (value.isBlank()) {
            return null;
        }
        String host = value;
        int port = localPeer.getPort();
        int colonIndex = value.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < value.length() - 1) {
            host = value.substring(0, colonIndex);
            try {
                port = Integer.parseInt(value.substring(colonIndex + 1));
            } catch (NumberFormatException ignored) {
                System.out.println("[WARN] Invalid peer port in address '" + value
                        + "'. Falling back to local port " + localPeer.getPort());
                port = localPeer.getPort();
            }
        }
        return new PeerInfo(name, host, port);
    }

    /**
     * Kiểm tra địa chỉ người dùng nhập có trỏ về chính peer hiện tại hay không.
     */
    public boolean isSelfAddress(String hostAndMaybePort) {
        PeerInfo peerInfo = parsePeer(hostAndMaybePort, hostAndMaybePort);
        return isSelfPeer(peerInfo);
    }

    /**
     * So sánh PeerInfo với localPeer để chặn self-chat trong mọi luồng logic.
     */
    private boolean isSelfPeer(PeerInfo peerInfo) {
        if (peerInfo == null || peerInfo.getPort() != localPeer.getPort()) {
            return false;
        }
        return isSameHost(peerInfo.getHost(), localPeer.getHost());
    }

    /**
     * So sánh host theo literal, localhost/loopback và địa chỉ IP resolve được.
     */
    private boolean isSameHost(String candidateHost, String localHost) {
        if (candidateHost == null || candidateHost.isBlank()) {
            return false;
        }
        if (candidateHost.equalsIgnoreCase(localHost)
                || "localhost".equalsIgnoreCase(candidateHost)
                || "127.0.0.1".equals(candidateHost)) {
            return true;
        }
        try {
            InetAddress candidateAddress = InetAddress.getByName(candidateHost);
            InetAddress localAddress = InetAddress.getByName(localHost);
            return candidateAddress.isLoopbackAddress() || candidateAddress.equals(localAddress);
        } catch (UnknownHostException e) {
            System.out.println("[WARN] Failed to compare host with local peer. host="
                    + candidateHost + ", error=" + e.getMessage());
            return false;
        }
    }

    /**
     * Notify các MessageListener, bảo đảm callback chạy trên Swing EDT khi cần cập nhật UI.
     */
    private void notifyMessage(Message message) {
        Runnable notifier = () -> messageListeners.forEach(listener -> listener.onMessageReceived(message));
        if (SwingUtilities.isEventDispatchThread()) {
            notifier.run();
        } else {
            SwingUtilities.invokeLater(notifier);
        }
    }

    /**
     * Notify các listener đang quan sát thay đổi danh sách/trạng thái peer.
     */
    private void notifyPeersChanged() {
        Runnable notifier = () -> peerChangeListeners.forEach(Runnable::run);
        if (SwingUtilities.isEventDispatchThread()) {
            notifier.run();
        } else {
            SwingUtilities.invokeLater(notifier);
        }
    }

    /**
     * Tìm địa chỉ IPv4 LAN phù hợp nhất của máy hiện tại để peer khác có thể kết nối.
     */
    private static String resolveLocalHost() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        System.out.println("[INFO] Resolved local IPv4 address: " + address.getHostAddress());
                        return address.getHostAddress();
                    }
                }
            }
            String fallback = InetAddress.getLocalHost().getHostAddress();
            System.out.println("[WARN] Falling back to InetAddress.getLocalHost(): " + fallback);
            return fallback;
        } catch (IOException e) {
            System.out.println("[WARN] Failed to resolve local host. Falling back to 127.0.0.1. Error=" + e.getMessage());
            return "127.0.0.1";
        }
    }
}
