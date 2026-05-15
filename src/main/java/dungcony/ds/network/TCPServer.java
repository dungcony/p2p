package dungcony.ds.network;

import dungcony.ds.peer.MessageReceiver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {
    private final int port;
    private final MessageReceiver receiver;
    private final ExecutorService connectionPool = Executors.newCachedThreadPool();
    private volatile boolean running;
    private ServerSocket serverSocket;

    public TCPServer(int port, MessageReceiver receiver) {
        this.port = port;
        this.receiver = receiver;
    }

    public void listen() {
        running = true;
        try (ServerSocket openedSocket = new ServerSocket(port)) {
            serverSocket = openedSocket;
            while (running) {
                Socket socket = openedSocket.accept();
                connectionPool.submit(new ConnectionHandler(socket, receiver));
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[ERROR] TCP server stopped: " + e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    public void stop() {
        running = false;
        connectionPool.shutdownNow();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
