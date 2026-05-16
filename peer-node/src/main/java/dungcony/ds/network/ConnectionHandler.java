package dungcony.ds.network;

import dungcony.ds.model.Message;
import dungcony.ds.peer.MessageReceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final MessageReceiver receiver;
    private final MessageProtocol protocol = new MessageProtocol();

    /**
     * Khởi tạo handler cho một socket đã accept từ TCPServer.
     */
    public ConnectionHandler(Socket socket, MessageReceiver receiver) {
        this.socket = socket;
        this.receiver = receiver;
    }

    /**
     * Đọc một message JSON từ socket, chuyển cho MessageReceiver xử lý và trả ACK/response.
     */
    @Override
    public void run() {
        try (Socket acceptedSocket = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(acceptedSocket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(acceptedSocket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            System.out.println("[DEBUG] Đang xử lý kết nối TCP từ "
                    + acceptedSocket.getRemoteSocketAddress());
            String payload = reader.readLine();
            if (payload == null || payload.isBlank()) {
                System.out.println("[WARN] Payload TCP rỗng từ "
                        + acceptedSocket.getRemoteSocketAddress());
                return;
            }

            Message incoming = protocol.deserialize(payload);
            System.out.println("[DEBUG] Đã deserialize payload đến. messageId="
                    + (incoming == null ? "null" : incoming.getId()));
            Message response = receiver.receive(incoming);
            if (response != null) {
                writer.println(protocol.serialize(response));
                System.out.println("[DEBUG] Đã gửi phản hồi. messageId=" + response.getId()
                        + ", type=" + response.getType());
            } else {
                System.out.println("[WARN] Receiver trả về phản hồi null.");
            }
        } catch (IOException e) {
            System.out.println("[WARN] Không thể xử lý kết nối đến: " + e.getMessage());
        }
    }
}
