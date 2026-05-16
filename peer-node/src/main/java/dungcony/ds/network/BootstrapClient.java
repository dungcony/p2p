package dungcony.ds.network;

import com.google.gson.Gson;
import dungcony.ds.model.JoinResponse;
import dungcony.ds.model.OfflineMessage;
import dungcony.ds.model.PeerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

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
        System.out.println("[INFO] Bootstrap REGISTER result=" + success
                + ", peerId=" + peerInfo.getId() + ", response=" + response);
        return success;
    }

    /**
     * Gui JOIN de danh dau peer online va nhan danh sach peer dang online.
     */
    public JoinResponse join(PeerInfo peerInfo) {
        String response = request("JOIN", peerInfo);
        if (response == null || response.isBlank()) {
            System.out.println("[WARN] Bootstrap JOIN returned empty response.");
            return new JoinResponse();
        }
        try {
            JoinResponse joinResponse = gson.fromJson(response, JoinResponse.class);
            System.out.println("[INFO] Bootstrap JOIN received peers="
                    + joinResponse.getOnlinePeers().size()
                    + ", offlineMessages=" + joinResponse.getOfflineMessages().size());
            return joinResponse;
        } catch (RuntimeException e) {
            System.out.println("[ERROR] Failed to parse Bootstrap JOIN response: " + e.getMessage());
            return new JoinResponse();
        }
    }

    /**
     * Gui tin nhan offline len bootstrap-server khi receiver dang mat ket noi truc tiep.
     */
    public boolean storeOffline(OfflineMessage message) {
        String response = request("STORE_OFFLINE", message);
        boolean success = "OK".equalsIgnoreCase(response);
        System.out.println("[INFO] Bootstrap STORE_OFFLINE result=" + success
                + ", messageId=" + message.getMessageId()
                + ", receiverId=" + message.getReceiverId());
        return success;
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
        String response = requestRaw("LIST", "");
        if (response == null || response.isBlank()) {
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
            System.out.println("[DEBUG] Bootstrap connect start " + host + ":" + port + ", command=" + command);
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
            System.out.println("[WARN] Bootstrap request failed. command=" + command
                    + ", address=" + host + ":" + port + ", error=" + e.getMessage());
            return null;
        }
    }
}
