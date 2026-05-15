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

    public ConnectionHandler(Socket socket, MessageReceiver receiver) {
        this.socket = socket;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        try (Socket acceptedSocket = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(acceptedSocket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(acceptedSocket.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String payload = reader.readLine();
            if (payload == null || payload.isBlank()) {
                return;
            }

            Message incoming = protocol.deserialize(payload);
            Message response = receiver.receive(incoming);
            if (response != null) {
                writer.println(protocol.serialize(response));
            }
        } catch (IOException e) {
            System.out.println("[WARN] Failed to handle incoming connection: " + e.getMessage());
        }
    }
}
