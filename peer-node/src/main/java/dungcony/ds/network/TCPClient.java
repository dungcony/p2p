package dungcony.ds.network;

import dungcony.ds.enums.MessageType;
import dungcony.ds.model.Message;
import dungcony.ds.model.PeerInfo;

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
            System.out.println("[DEBUG] Bắt đầu kết nối TCP: " + peerInfo.addressKey()
                    + ", messageId=" + message.getId());
            socket.connect(new InetSocketAddress(peerInfo.getHost(), peerInfo.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            System.out.println("[DEBUG] TCP đã kết nối: " + peerInfo.addressKey());

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            writer.println(protocol.serialize(message));
            System.out.println("[DEBUG] Đã gửi payload TCP. messageId=" + message.getId());
            String response = reader.readLine();
            if (response == null || response.isBlank()) {
                System.out.println("[WARN] Phản hồi TCP rỗng. peer=" + peerInfo.addressKey()
                        + ", messageId=" + message.getId());
                return false;
            }

            Message ack = protocol.deserialize(response);
            boolean validAck = ack != null && ack.getType() == MessageType.ACK && message.getId().equals(ack.getId());
            System.out.println("[DEBUG] Đã nhận phản hồi TCP. peer=" + peerInfo.addressKey()
                    + ", messageId=" + message.getId() + ", validAck=" + validAck);
            return validAck;
        } catch (IOException e) {
            System.out.println("[WARN] Gửi TCP thất bại. peer=" + peerInfo.addressKey()
                    + ", messageId=" + message.getId() + ", lỗi=" + e.getMessage());
            return false;
        }
    }
}
