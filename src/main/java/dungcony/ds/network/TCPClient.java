package dungcony.ds.network;

import dungcony.ds.model.Message;
import dungcony.ds.model.MessageType;
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

    public boolean send(PeerInfo peerInfo, Message message) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(peerInfo.getHost(), peerInfo.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            writer.println(protocol.serialize(message));
            String response = reader.readLine();
            if (response == null || response.isBlank()) {
                return false;
            }

            Message ack = protocol.deserialize(response);
            return ack != null && ack.getType() == MessageType.ACK && message.getId().equals(ack.getId());
        } catch (IOException e) {
            return false;
        }
    }
}
