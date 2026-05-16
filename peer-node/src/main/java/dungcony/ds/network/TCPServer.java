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

    /**
     * Khởi tạo TCPServer với port lắng nghe và MessageReceiver để xử lý message đến.
     */
    public TCPServer(int port, MessageReceiver receiver) {
        this.port = port;
        this.receiver = receiver;
    }

    /**
     * Mở ServerSocket, accept nhiều kết nối và giao từng kết nối cho ConnectionHandler.
     */
    public void listen() {
        running = true;
        try (ServerSocket openedSocket = new ServerSocket(port)) {
            serverSocket = openedSocket;
            System.out.println("[INFO] TCPServer đang lắng nghe trên cổng " + port);
            while (running) {
                Socket socket = openedSocket.accept();
                System.out.println("[DEBUG] TCPServer đã nhận kết nối từ "
                        + socket.getRemoteSocketAddress());
                connectionPool.submit(new ConnectionHandler(socket, receiver));
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[ERROR] TCP server đã dừng: " + e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    /**
     * Dừng server và đóng connection pool để peer thoát sạch.
     */
    public void stop() {
        running = false;
        connectionPool.shutdownNow();
        System.out.println("[INFO] TCPServer đang dừng trên cổng " + port);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
