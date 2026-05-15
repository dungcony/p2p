# Cấu trúc dự án P2P Chat System

## Tổng quan

Hệ thống chat ngang hàng (P2P) viết bằng Java, giao diện Swing. Mỗi peer vừa đóng vai **client** (gửi tin) vừa đóng vai **server** (nhận tin) trong cùng một tiến trình.

```
p2p-chat/
├── src/
│   └── main/
│       ├── java/
│       │   └── dungcony/ds/
│       │       ├── App.java
│       │       ├── bootstrap/
│       │       ├── peer/
│       │       ├── network/
│       │       ├── model/
│       │       └── ui/
│       └── resources/
│           └── data/
│               ├── config.properties
│               ├── peer.json
│               └── group.json
├── docs/
└── pom.xml
```

---

## Chi tiết từng package

### `App.java` — Entry point

Nằm ở root package, chỉ chứa `main()`. Nhiệm vụ:
1. Hiện `LoginDialog` để người dùng nhập tên và port
2. Khởi động `PeerNode`
3. Mở `MainWindow`

```java
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog();
            login.setVisible(true);
        });
    }
}
```

---

### `bootstrap/` — Bootstrap Server (Tracker)

Chạy **độc lập** như một tiến trình riêng, không phải peer. Giúp peer mới tìm được các peer khác trong mạng.

| File | Nhiệm vụ |
|---|---|
| `BootstrapServer.java` | Lắng nghe kết nối, xử lý yêu cầu JOIN/LEAVE/LIST |
| `PeerRegistry.java` | Lưu danh sách peer đang online (`ConcurrentHashMap`) |
| `HeartbeatMonitor.java` | Định kỳ ping peer, xoá peer không phản hồi |

**Luồng hoạt động:**
```
Peer mới → gửi JOIN(id, host, port) → BootstrapServer
BootstrapServer → trả về danh sách peer đang online
BootstrapServer → broadcast PEER_JOINED tới các peer khác
```

---

### `peer/` — Logic chính của Peer Node

Trái tim của hệ thống P2P. `PeerNode` khởi động đồng thời hai vai trò: server (lắng nghe) và client (gửi tin).

| File | Nhiệm vụ |
|---|---|
| `PeerNode.java` | Class trung tâm, khởi động TCPServer và TCPClient song song |
| `MessageSender.java` | Gửi tin nhắn 1-1 và broadcast nhóm |
| `MessageReceiver.java` | Nhận và xử lý tin nhắn đến, gọi callback về UI |
| `GroupManager.java` | Quản lý nhóm chat: tạo nhóm, thêm/xoá thành viên |
| `MessageListener.java` | Interface callback để notify UI khi có tin đến |

**Cách PeerNode hoạt động song song:**
```java
public void start() {
    // Vai SERVER: lắng nghe mãi trên thread riêng
    new Thread(tcpServer::listen).start();

    // Vai CLIENT: gọi khi cần gửi tin, không cần start trước
    tcpClient = new TCPClient();
}
```

---

### `network/` — Tầng giao tiếp mạng (TCP Socket)

Xử lý thuần túy việc truyền/nhận dữ liệu qua mạng. Không biết gì về logic nghiệp vụ.

| File | Nhiệm vụ |
|---|---|
| `TCPServer.java` | Mở `ServerSocket`, dùng `ExecutorService` accept nhiều kết nối |
| `TCPClient.java` | Mở `Socket` kết nối tới peer khác để gửi tin |
| `ConnectionHandler.java` | Xử lý một kết nối đến, implements `Runnable` |
| `MessageProtocol.java` | Serialize/deserialize tin nhắn sang JSON (dùng Gson) |

**Xử lý nhiều kết nối đồng thời:**
```java
public void listen() {
    ExecutorService pool = Executors.newCachedThreadPool();
    while (true) {
        Socket conn = serverSocket.accept(); // blocking
        pool.submit(new ConnectionHandler(conn, receiver));
    }
}
```

---

### `model/` — Data class

Các class dữ liệu thuần túy, không chứa logic xử lý.

| File | Nội dung |
|---|---|
| `Message.java` | Nội dung tin nhắn: id, type, sender, receiver, content, timestamp |
| `PeerInfo.java` | Thông tin peer: id, host, port, status |
| `MessageType.java` | Enum: `CHAT`, `GROUP_CHAT`, `JOIN`, `LEAVE`, `ACK`, `HEARTBEAT` |
| `Group.java` | Thông tin nhóm: groupId, name, danh sách members |

**Ví dụ `MessageType`:**
```java
public enum MessageType {
    CHAT,        // tin nhắn 1-1
    GROUP_CHAT,  // tin nhắn nhóm
    JOIN,        // peer tham gia mạng
    LEAVE,       // peer rời mạng
    ACK,         // xác nhận đã nhận
    HEARTBEAT    // kiểm tra còn sống
}
```

---

### `ui/` — Giao diện người dùng (Swing)

## Dữ liệu lưu trữ

### RAM (mất khi tắt app)
- Danh sách peer đang online → `ConcurrentHashMap` trong `PeerRegistry`
- Kết nối TCP đang mở → `Map<PeerInfo, Socket>` trong `PeerNode`
- Tin nhắn đang hiển thị → `DefaultListModel` trong `ChatPanel`

### File (còn sau khi tắt app)

**`config.properties`** — cấu hình của peer:
```properties
peer.id=peer-alice
peer.port=5001
bootstrap.host=localhost
bootstrap.port=9000
```

**`peer.json`** — danh sách peer đã từng kết nối (dùng để reconnect):
```json
[
  { "id": "peer-bob", "host": "192.168.1.5", "port": 5002 },
  { "id": "peer-carol", "host": "192.168.1.8", "port": 5003 }
]
```

**`group.json`** — nhóm chat đã tạo:
```json
[
  { "groupId": "g1", "name": "Nhóm học", "members": ["peer-bob", "peer-carol"] }
]
```

---

## Luồng khởi động

```
App.main()
  └─ LoginDialog (nhập tên, port)
       └─ PeerNode.start()
            ├─ TCPServer.listen()        [thread riêng — vai SERVER]
            ├─ Kết nối Bootstrap Server  [lấy danh sách peer]
            └─ MainWindow.show()         [hiện giao diện Swing]
```

---