package dungcony.ds.models;

import com.google.gson.Gson;
import dungcony.ds.entities.OfflineMessageEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BootstrapServerTest {
    private final Gson gson = new Gson();
    private final List<RunningBootstrap> runningServers = new ArrayList<>();

    @TempDir
    Path tempDir;

    @AfterEach
    void stopBootstrapServers() {
        for (RunningBootstrap runningServer : runningServers) {
            runningServer.close();
        }
        runningServers.clear();
    }

    @Test
    void joinListAndLeaveTrackOnlinePeers() throws Exception {
        RunningBootstrap bootstrap = startBootstrap();
        PeerInfo alice = new PeerInfo("alice", "Alice", "127.0.0.1", freePort(), true);
        PeerInfo bob = new PeerInfo("bob", "Bob", "127.0.0.1", freePort(), true);

        JoinResponse aliceJoin = join(bootstrap.port(), alice);
        assertPeerPresent(aliceJoin.getOnlinePeers(), "alice");

        JoinResponse bobJoin = join(bootstrap.port(), bob);
        assertPeerPresent(bobJoin.getOnlinePeers(), "alice");
        assertPeerPresent(bobJoin.getOnlinePeers(), "bob");

        assertEquals("OK", requestRaw(bootstrap.port(), "LEAVE", alice.addressKey()));
        PeerInfo[] listedPeers = gson.fromJson(requestRaw(bootstrap.port(), "LIST", ""), PeerInfo[].class);

        assertFalse(containsPeer(listedPeers, "alice"));
        assertTrue(containsPeer(listedPeers, "bob"));
    }

    @Test
    void offlineMessagesAreDrainedOnFirstJoinOnly() throws Exception {
        RunningBootstrap bootstrap = startBootstrap();
        PeerInfo bob = new PeerInfo("bob", "Bob", "127.0.0.1", freePort(), true);
        OfflineMessageEntity offlineMessage = new OfflineMessageEntity(
                "message-1",
                "alice",
                "bob",
                null,
                "hello while offline",
                System.currentTimeMillis(),
                false
        );

        assertEquals("OK", request(bootstrap.port(), "STORE_OFFLINE", offlineMessage));

        JoinResponse firstJoin = join(bootstrap.port(), bob);
        List<OfflineMessageEntity> firstMessages = new ArrayList<>(firstJoin.getOfflineMessages());
        assertEquals(1, firstMessages.size());
        assertEquals("message-1", firstMessages.getFirst().getMessageId());
        assertEquals("hello while offline", firstMessages.getFirst().getContent());

        JoinResponse secondJoin = join(bootstrap.port(), bob);
        assertTrue(secondJoin.getOfflineMessages().isEmpty());
    }

    private RunningBootstrap startBootstrap() throws Exception {
        int port = freePort();
        BootstrapServer server = new BootstrapServer(port, tempDir.resolve("bootstrap-" + port + ".db"));
        Thread thread = new Thread(server::start, "BootstrapServerTest-" + port);
        thread.setDaemon(true);
        thread.start();

        await("bootstrap server to listen", () -> requestRaw(port, "LIST", "") != null);
        RunningBootstrap runningBootstrap = new RunningBootstrap(server, thread, port);
        runningServers.add(runningBootstrap);
        return runningBootstrap;
    }

    private JoinResponse join(int port, PeerInfo peerInfo) throws IOException {
        String response = request(port, "JOIN", peerInfo);
        assertNotNull(response);
        return gson.fromJson(response, JoinResponse.class);
    }

    private String request(int port, String command, Object payload) throws IOException {
        return requestRaw(port, command, gson.toJson(payload));
    }

    private String requestRaw(int port, String command, String payload) {
        String line = payload == null || payload.isBlank() ? command : command + " " + payload;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            socket.setSoTimeout(1000);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                writer.println(line);
                return reader.readLine();
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void assertPeerPresent(Collection<PeerInfo> peers, String peerId) {
        assertTrue(peers.stream().anyMatch(peer -> peerId.equals(peer.getId())),
                "Expected peer " + peerId + " to be present");
    }

    private boolean containsPeer(PeerInfo[] peers, String peerId) {
        if (peers == null) {
            return false;
        }
        for (PeerInfo peer : peers) {
            if (peer != null && peerId.equals(peer.getId())) {
                return true;
            }
        }
        return false;
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static void await(String description, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
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
