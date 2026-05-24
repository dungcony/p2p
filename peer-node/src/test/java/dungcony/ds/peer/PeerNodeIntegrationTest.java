package dungcony.ds.peer;

import dungcony.ds.dtos.BroadcastResult;
import dungcony.ds.enums.MessageStatus;
import dungcony.ds.enums.MessageType;
import dungcony.ds.model.*;
import dungcony.ds.models.BootstrapServer;
import dungcony.ds.network.BootstrapClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class PeerNodeIntegrationTest {
    private final List<PeerNode> peers = new ArrayList<>();
    private final List<RunningBootstrap> bootstraps = new ArrayList<>();

    @TempDir
    Path tempDir;

    @AfterEach
    void stopNodes() {
        for (int index = peers.size() - 1; index >= 0; index--) {
            try {
                peers.get(index).stop();
            } catch (RuntimeException ignored) {
            }
        }
        peers.clear();

        for (RunningBootstrap bootstrap : bootstraps) {
            bootstrap.close();
        }
        bootstraps.clear();
    }

    @Test
    void directMessageIsDeliveredWithAckThroughDiscoveredPeer() throws Exception {
        RunningBootstrap bootstrap = startBootstrap();
        PeerNode bob = startPeer("bob", "Bob", freePort(), bootstrap.port(), tempDir.resolve("bob"));
        await("bob to join bootstrap", () -> bootstrapHasPeer(bootstrap.port(), "bob"));

        CountDownLatch bobReceived = new CountDownLatch(1);
        List<Message> bobMessages = new CopyOnWriteArrayList<>();
        bob.addMessageListener(message -> {
            bobMessages.add(message);
            if (message.getType() == MessageType.CHAT && "hello bob".equals(message.getContent())) {
                bobReceived.countDown();
            }
        });

        PeerNode alice = startPeer("alice", "Alice", freePort(), bootstrap.port(), tempDir.resolve("alice"));
        PeerInfo bobFromAlice = awaitKnownPeer(alice, "bob");

        assertTrue(bobFromAlice.isOnline());
        assertTrue(alice.sendMessage("hello bob", bobFromAlice.addressKey()));
        assertTrue(bobReceived.await(5, TimeUnit.SECONDS));
        assertTrue(bobMessages.stream().anyMatch(message -> "hello bob".equals(message.getContent())));

        Message sentMessage = alice.getLastMessage(bobFromAlice.addressKey());
        assertNotNull(sentMessage);
        assertEquals(MessageStatus.SENT, sentMessage.getStatus());
    }

    @Test
    void failedDirectSendIsStoredOfflineAndDeliveredWhenReceiverJoinsAgain() throws Exception {
        RunningBootstrap bootstrap = startBootstrap();
        PeerNode alice = startPeer("alice", "Alice", freePort(), bootstrap.port(), tempDir.resolve("alice"));
        PeerNode bob = startPeer("bob", "Bob", freePort(), bootstrap.port(), tempDir.resolve("bob"));
        await("bob to join bootstrap", () -> bootstrapHasPeer(bootstrap.port(), "bob"));

        PeerInfo bobFromAlice = awaitKnownPeer(alice, "bob");
        bob.stop();
        await("bob TCP server to stop", () -> !canConnect("127.0.0.1", bob.getLocalPeer().getPort()));

        assertFalse(alice.sendMessage("offline hello", bobFromAlice.addressKey()));
        Message pendingMessage = alice.getLastMessage(bobFromAlice.addressKey());
        assertNotNull(pendingMessage);
        assertEquals(MessageStatus.PENDING, pendingMessage.getStatus());

        PeerNode bobAgain = new PeerNode(
                "bob",
                "Bob",
                bob.getLocalPeer().getPort(),
                "127.0.0.1",
                bootstrap.port(),
                tempDir.resolve("bob-rejoined")
        );
        peers.add(bobAgain);
        CountDownLatch offlineDelivered = new CountDownLatch(1);
        List<Message> bobAgainMessages = new CopyOnWriteArrayList<>();
        bobAgain.addMessageListener(message -> {
            bobAgainMessages.add(message);
            if (message.getType() == MessageType.CHAT && "offline hello".equals(message.getContent())) {
                offlineDelivered.countDown();
            }
        });
        bobAgain.start();
        await("bob rejoined TCP server to listen", () -> canConnect("127.0.0.1", bobAgain.getLocalPeer().getPort()));

        assertTrue(offlineDelivered.await(8, TimeUnit.SECONDS));
        assertTrue(bobAgainMessages.stream().anyMatch(message -> "offline hello".equals(message.getContent())));
    }

    @Test
    void groupMessageIsBroadcastToAllOnlineMembers() throws Exception {
        RunningBootstrap bootstrap = startBootstrap();
        PeerNode bob = startPeer("bob", "Bob", freePort(), bootstrap.port(), tempDir.resolve("bob"));
        PeerNode carol = startPeer("carol", "Carol", freePort(), bootstrap.port(), tempDir.resolve("carol"));
        await("bob and carol to join bootstrap",
                () -> bootstrapHasPeer(bootstrap.port(), "bob") && bootstrapHasPeer(bootstrap.port(), "carol"));

        CountDownLatch bobReceived = new CountDownLatch(1);
        CountDownLatch carolReceived = new CountDownLatch(1);
        bob.addMessageListener(message -> {
            if (message.getType() == MessageType.GROUP_CHAT && "hello group".equals(message.getContent())) {
                bobReceived.countDown();
            }
        });
        carol.addMessageListener(message -> {
            if (message.getType() == MessageType.GROUP_CHAT && "hello group".equals(message.getContent())) {
                carolReceived.countDown();
            }
        });

        PeerNode alice = startPeer("alice", "Alice", freePort(), bootstrap.port(), tempDir.resolve("alice"));
        PeerInfo bobFromAlice = awaitKnownPeer(alice, "bob");
        PeerInfo carolFromAlice = awaitKnownPeer(alice, "carol");
        Group group = alice.createGroup("Study", List.of(bobFromAlice, carolFromAlice));

        alice.sendGroupMessage(group.getGroupId(), "hello group");

        assertTrue(bobReceived.await(5, TimeUnit.SECONDS));
        assertTrue(carolReceived.await(5, TimeUnit.SECONDS));
    }

    @Test
    void networkBroadcastIsDeliveredToAllOnlinePeers() throws Exception {
        RunningBootstrap bootstrap = startBootstrap();
        PeerNode bob = startPeer("bob", "Bob", freePort(), bootstrap.port(), tempDir.resolve("bob"));
        PeerNode carol = startPeer("carol", "Carol", freePort(), bootstrap.port(), tempDir.resolve("carol"));
        await("bob and carol to join bootstrap",
                () -> bootstrapHasPeer(bootstrap.port(), "bob") && bootstrapHasPeer(bootstrap.port(), "carol"));

        CountDownLatch bobReceived = new CountDownLatch(1);
        CountDownLatch carolReceived = new CountDownLatch(1);
        bob.addMessageListener(message -> {
            if (message.getType() == MessageType.BROADCAST && "hello network".equals(message.getContent())) {
                bobReceived.countDown();
            }
        });
        carol.addMessageListener(message -> {
            if (message.getType() == MessageType.BROADCAST && "hello network".equals(message.getContent())) {
                carolReceived.countDown();
            }
        });

        PeerNode alice = startPeer("alice", "Alice", freePort(), bootstrap.port(), tempDir.resolve("alice"));
        awaitKnownPeer(alice, "bob");
        awaitKnownPeer(alice, "carol");

        BroadcastResult result = alice.broadcastToNetwork("hello network");

        assertEquals(2, result.totalTargets());
        assertEquals(2, result.delivered());
        assertEquals(0, result.failed());
        assertTrue(bobReceived.await(5, TimeUnit.SECONDS));
        assertTrue(carolReceived.await(5, TimeUnit.SECONDS));
    }

    private RunningBootstrap startBootstrap() throws Exception {
        int port = freePort();
        BootstrapServer server = new BootstrapServer(port, tempDir.resolve("bootstrap-" + port + ".db"));
        Thread thread = new Thread(server::start, "PeerNodeIntegrationTest-Bootstrap-" + port);
        thread.setDaemon(true);
        thread.start();
        await("bootstrap server to listen", () -> new BootstrapClient("127.0.0.1", port).listOrNull() != null);

        RunningBootstrap bootstrap = new RunningBootstrap(server, thread, port);
        bootstraps.add(bootstrap);
        return bootstrap;
    }

    private PeerNode startPeer(String id, String name, int port, int bootstrapPort, Path dataDir) throws Exception {
        PeerNode peerNode = new PeerNode(id, name, port, "127.0.0.1", bootstrapPort, dataDir);
        peers.add(peerNode);
        peerNode.start();
        await(id + " TCP server to listen", () -> canConnect("127.0.0.1", port));
        return peerNode;
    }

    private PeerInfo awaitKnownPeer(PeerNode peerNode, String peerId) throws InterruptedException {
        List<PeerInfo> match = new ArrayList<>();
        await("peer " + peerId + " to be discovered", () -> {
            match.clear();
            for (PeerInfo peerInfo : peerNode.getKnownPeers()) {
                if (peerId.equals(peerInfo.getId())) {
                    match.add(peerInfo);
                    return true;
                }
            }
            return false;
        });
        return match.getFirst();
    }

    private boolean bootstrapHasPeer(int bootstrapPort, String peerId) {
        Collection<PeerInfo> peers = new BootstrapClient("127.0.0.1", bootstrapPort).listOrNull();
        return peers != null && peers.stream().anyMatch(peer -> peerId.equals(peer.getId()));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static boolean canConnect(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 300);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void await(String description, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Timed out waiting for " + description);
    }

    private record RunningBootstrap(BootstrapServer server, Thread thread, int port) implements AutoCloseable {
        @Override
        public void close() {
            server.stop();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
                socket.getOutputStream().write('\n');
            } catch (IOException ignored) {
            }
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
