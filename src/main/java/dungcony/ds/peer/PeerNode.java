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

    public PeerNode(String peerId, int port) {
        this.localPeer = new PeerInfo(peerId, resolveLocalHost(), port);
        TCPClient tcpClient = new TCPClient();
        this.messageSender = new MessageSender(tcpClient);
        this.tcpServer = new TCPServer(port, new MessageReceiver(this));
    }

    public void start() {
        Thread serverThread = new Thread(tcpServer::listen, "PeerNode-TCPServer-" + localPeer.getPort());
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void stop() {
        tcpServer.stop();
    }

    public PeerInfo getLocalPeer() {
        return localPeer;
    }

    public void addMessageListener(MessageListener listener) {
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    public void addPeerChangeListener(Runnable listener) {
        if (listener != null) {
            peerChangeListeners.add(listener);
        }
    }

    public PeerInfo addKnownPeer(String name, String hostAndMaybePort) {
        PeerInfo peerInfo = parsePeer(name, hostAndMaybePort);
        peers.put(peerInfo.addressKey(), peerInfo);
        notifyPeersChanged();
        return peerInfo;
    }

    public Collection<PeerInfo> getKnownPeers() {
        return Collections.unmodifiableCollection(peers.values());
    }

    public boolean checkUserIsOnline(String hostAndMaybePort) {
        PeerInfo peerInfo = resolvePeer(hostAndMaybePort);
        if (peerInfo == null) {
            return false;
        }
        boolean online = messageSender.send(peerInfo, Message.heartbeat(localPeer));
        peerInfo.setOnline(online);
        notifyPeersChanged();
        return online;
    }

    public boolean sendMessage(String content, String hostAndMaybePort) {
        PeerInfo receiver = resolvePeer(hostAndMaybePort);
        if (receiver == null) {
            receiver = addKnownPeer(hostAndMaybePort, hostAndMaybePort);
        }

        Message message = Message.chat(localPeer, receiver, content);
        boolean sent = messageSender.send(receiver, message);
        receiver.setOnline(sent);
        if (sent) {
            addMessage(receiver.addressKey(), message);
            notifyMessage(message);
        }
        notifyPeersChanged();
        return sent;
    }

    public void sendGroupMessage(String groupId, String content) {
        Group group = groupManager.getGroup(groupId);
        if (group == null) {
            return;
        }
        Message message = Message.groupChat(localPeer, groupId, content);
        messageSender.broadcast(group, message);
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public List<Message> getMessagesWithPeer(String hostAndMaybePort) {
        PeerInfo peerInfo = resolvePeer(hostAndMaybePort);
        String key = peerInfo == null ? hostAndMaybePort : peerInfo.addressKey();
        return new ArrayList<>(messageHistory.getOrDefault(key, Collections.emptyList()));
    }

    public Message getLastMessage(String hostAndMaybePort) {
        List<Message> messages = getMessagesWithPeer(hostAndMaybePort);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    public List<PeerInfo> discoverPeersOnLocalNetwork() {
        List<PeerInfo> discovered = new ArrayList<>();
        String localHost = localPeer.getHost();
        int lastDot = localHost.lastIndexOf('.');
        if (lastDot < 0) {
            return discovered;
        }

        String prefix = localHost.substring(0, lastDot + 1);
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
        return discovered;
    }

    void onInboundMessage(Message message) {
        PeerInfo sender = new PeerInfo(message.getSenderId(), message.getSenderHost(), message.getSenderPort());
        peers.put(sender.addressKey(), sender);
        addMessage(sender.addressKey(), message);
        notifyPeersChanged();
        notifyMessage(message);
    }

    void markPeerOnline(Message message) {
        PeerInfo sender = new PeerInfo(message.getSenderId(), message.getSenderHost(), message.getSenderPort());
        peers.put(sender.addressKey(), sender);
        notifyPeersChanged();
    }

    private void addMessage(String peerKey, Message message) {
        messageHistory.computeIfAbsent(peerKey, ignored -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }

    private PeerInfo resolvePeer(String hostAndMaybePort) {
        if (hostAndMaybePort == null || hostAndMaybePort.isBlank()) {
            return null;
        }
        PeerInfo existing = peers.get(hostAndMaybePort.trim());
        if (existing != null) {
            return existing;
        }
        PeerInfo parsed = parsePeer(hostAndMaybePort, hostAndMaybePort);
        return peers.getOrDefault(parsed.addressKey(), parsed);
    }

    private PeerInfo parsePeer(String name, String hostAndMaybePort) {
        String value = hostAndMaybePort == null ? "" : hostAndMaybePort.trim();
        String host = value;
        int port = localPeer.getPort();
        int colonIndex = value.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < value.length() - 1) {
            host = value.substring(0, colonIndex);
            try {
                port = Integer.parseInt(value.substring(colonIndex + 1));
            } catch (NumberFormatException ignored) {
                port = localPeer.getPort();
            }
        }
        return new PeerInfo(name, host, port);
    }

    private void notifyMessage(Message message) {
        Runnable notifier = () -> messageListeners.forEach(listener -> listener.onMessageReceived(message));
        if (SwingUtilities.isEventDispatchThread()) {
            notifier.run();
        } else {
            SwingUtilities.invokeLater(notifier);
        }
    }

    private void notifyPeersChanged() {
        Runnable notifier = () -> peerChangeListeners.forEach(Runnable::run);
        if (SwingUtilities.isEventDispatchThread()) {
            notifier.run();
        } else {
            SwingUtilities.invokeLater(notifier);
        }
    }

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
                        return address.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            return "127.0.0.1";
        }
    }
}
