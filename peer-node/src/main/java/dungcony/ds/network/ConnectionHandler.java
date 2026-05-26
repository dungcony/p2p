package dungcony.ds.network;

import dungcony.ds.model.Message;
import dungcony.ds.utils.Mes;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// MessageReceiver is in the same package (dungcony.ds.network) — no import needed

/**
 * Xử lý một kết nối TCP đến và chuyển payload vào router nhận message
 */
@Slf4j
public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final MessageReceiver receiver;

    // Khởi tạo handler cho một socket đã accept từ TCPServer
    public ConnectionHandler(Socket socket, MessageReceiver receiver) {
        this.socket = socket;
        this.receiver = receiver;
    }

    // Đọc một message JSON từ socket, chuyển cho MessageReceiver xử lý và trả ACK/response
    @Override
    public void run() {
        try (Socket acceptedSocket = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(acceptedSocket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(acceptedSocket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            log.debug("Đang xử lý kết nối TCP từ {}", acceptedSocket.getRemoteSocketAddress());
            String payload = reader.readLine();
            if (payload == null || payload.isBlank()) {
                log.warn("Payload TCP rỗng từ {}", acceptedSocket.getRemoteSocketAddress());
                return;
            }

            Message incoming = Mes.deserialize(payload);
            log.debug("Đã deserialize payload đến. messageId={}", (incoming == null ? "null" : incoming.getId()));
            Message response = receiver.receive(incoming);
            if (response != null) {
                writer.println(Mes.serialize(response));
                log.debug("Đã gửi phản hồi. messageId={}, type={}", response.getId(), response.getType());
            } else {
                log.warn("Receiver trả về phản hồi null.");
            }
        } catch (IOException e) {
            log.warn("Không thể xử lý kết nối đến: {}", e.getMessage());
        }
    }
}
