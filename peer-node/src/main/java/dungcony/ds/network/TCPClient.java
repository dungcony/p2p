package dungcony.ds.network;

import lombok.extern.slf4j.Slf4j;


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

/**
 * Client TCP gửi message tới peer khác và đọc ACK hoặc response
 */
@Slf4j
public class TCPClient {
private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 3000;
    private final MessageProtocol protocol = new MessageProtocol();

    // Mở kết nối TCP tới peer đích, gửi một message và trả về true khi nhận ACK hợp lệ
    public boolean send(PeerInfo peerInfo, Message message) {
        Message response = sendForResponse(peerInfo, message);
        boolean validAck = response != null && response.getType() == MessageType.ACK && message.getId().equals(response.getId());
        log.debug("Đã nhận phản hồi TCP. peer={}, messageId={}, validAck={}", peerInfo.addressKey(), message.getId(), validAck);
        return validAck;
    }

    // Mở kết nối TCP tới peer đích, gửi một message và trả về response raw để xử lý các request không phải ACK
    public Message sendForResponse(PeerInfo peerInfo, Message message) {
        try (Socket socket = new Socket()) {
            log.debug("Bắt đầu kết nối TCP: {}, messageId={}", peerInfo.addressKey(), message.getId());
            socket.connect(new InetSocketAddress(peerInfo.getHost(), peerInfo.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            log.debug("TCP đã kết nối: {}", peerInfo.addressKey());

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            writer.println(protocol.serialize(message));
            log.debug("Đã gửi payload TCP. messageId={}", message.getId());
            String response = reader.readLine();
            if (response == null || response.isBlank()) {
                log.warn("Phản hồi TCP rỗng. peer={}, messageId={}", peerInfo.addressKey(), message.getId());
                return null;
            }

            Message decoded = protocol.deserialize(response);
            log.debug("Đã nhận phản hồi TCP. peer={}, messageId={}, responseType={}", peerInfo.addressKey(), message.getId(), (decoded == null ? "null" : decoded.getType()));
            return decoded;
        } catch (IOException e) {
            log.warn("Gửi TCP thất bại. peer={}, messageId={}, lỗi={}", peerInfo.addressKey(), message.getId(), e.getMessage());
            return null;
        }
    }
}
