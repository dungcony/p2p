package dungcony.ds.bootstrap;

import com.google.gson.Gson;
import dungcony.ds.model.PeerInfo;

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
    private final PeerRegistry registry = new PeerRegistry();
    private final Gson gson = new Gson();
    private volatile boolean running;

    public BootstrapServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        new BootstrapServer(port).start();
    }

    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[INFO] Bootstrap server listening on port " + port);
            while (running) {
                Socket socket = serverSocket.accept();
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

    public void stop() {
        running = false;
    }

    private void handle(Socket socket) {
        try (Socket accepted = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(accepted.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                return;
            }

            String[] parts = line.split(" ", 2);
            String command = parts[0].toUpperCase(Locale.ROOT);
            String payload = parts.length > 1 ? parts[1] : "";

            if ("JOIN".equals(command)) {
                PeerInfo peerInfo = gson.fromJson(payload, PeerInfo.class);
                registry.join(peerInfo);
                writer.println(gson.toJson(registry.list()));
                return;
            }

            if ("LEAVE".equals(command)) {
                registry.leave(payload.trim());
                writer.println("OK");
                return;
            }

            if ("LIST".equals(command)) {
                Collection<PeerInfo> peers = registry.list();
                writer.println(gson.toJson(peers));
                return;
            }

            writer.println("UNKNOWN_COMMAND");
        } catch (IOException e) {
            System.out.println("[WARN] Bootstrap request failed: " + e.getMessage());
        }
    }
}
