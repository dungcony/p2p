package dungcony.ds.network;

import dungcony.ds.model.Message;
import dungcony.ds.enums.MessageType;
import dungcony.ds.entities.PeerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPClient {
    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 3000;
    private final MessageProtocol protocol = new MessageProtocol();

    /**
     * Mở kết nối TCP tới peer đích, gửi một message và trả về true khi nhận ACK hợp lệ.
     */
    public boolean send(PeerInfo peerInfo, Message message) {
        try (Socket socket = new Socket()) {
            System.out.println("[DEBUG] TCP connect start: " + peerInfo.addressKey()
                    + ", messageId=" + message.getId());
            socket.connect(new InetSocketAddress(peerInfo.getHost(), peerInfo.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            System.out.println("[DEBUG] TCP connected: " + peerInfo.addressKey());

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            writer.println(protocol.serialize(message));
            System.out.println("[DEBUG] TCP payload sent. messageId=" + message.getId());
            String response = reader.readLine();
            if (response == null || response.isBlank()) {
                System.out.println("[WARN] TCP response empty. peer=" + peerInfo.addressKey()
                        + ", messageId=" + message.getId());
                return false;
            }

            Message ack = protocol.deserialize(response);
            boolean validAck = ack != null && ack.getType() == MessageType.ACK && message.getId().equals(ack.getId());
            System.out.println("[DEBUG] TCP response received. peer=" + peerInfo.addressKey()
                    + ", messageId=" + message.getId() + ", validAck=" + validAck);
            return validAck;
        } catch (IOException e) {
            System.out.println("[WARN] TCP send failed. peer=" + peerInfo.addressKey()
                    + ", messageId=" + message.getId() + ", error=" + e.getMessage());
            return false;
        }
    }
}
