# P2P Chat System

Project này triển khai hệ thống chat ngang hàng P2P bằng Java 21. Mỗi peer vừa gửi tin như một client, vừa lắng nghe TCP như một server để nhận tin trực tiếp từ peer khác. `bootstrap-server` chỉ đóng vai trò tracker hỗ trợ khám phá peer, trạng thái online/offline, group metadata và store-and-forward khi peer nhận đang offline.

## Modules

| Module | Vai trò |
| --- | --- |
| `peer-node` | Ứng dụng Swing của từng peer; gửi/nhận chat trực tiếp qua TCP, quản lý UI, lịch sử local và group chat. |
| `bootstrap-server` | Tracker TCP độc lập; lưu peer online trong RAM, lưu user/group/offline message bằng SQLite. |

Hai module chạy độc lập. `peer-node` vẫn mở được khi `bootstrap-server` chưa chạy, nhưng các chức năng discovery tự động, danh sách online từ tracker, group sync qua tracker và offline message cần bootstrap.

## Yêu cầu

- Java 21
- Maven 3.9+
- Windows batch scripts có sẵn cho luồng chạy nhanh

## Chạy nhanh

Chạy tracker trước nếu muốn dùng discovery và offline message:

```bat
run-bootstrap.bat
```

Mở peer ở terminal khác:

```bat
run.bat --peer-port=5001 --data-dir=tmp/alice
```

Mở thêm peer:

```bat
run.bat --peer-port=5002 --data-dir=tmp/bob
run.bat --peer-port=5003 --data-dir=tmp/carol
```

Nếu chạy `run.bat` không truyền tham số, app sẽ dùng profile đã lưu hoặc hỏi port khi tạo profile mới.

## Build Và Test

Chạy toàn bộ test:

```bat
mvn test
```

Compile toàn bộ project:

```bat
mvn -DskipTests compile
```

Build riêng bootstrap server:

```bat
mvn -pl bootstrap-server -am -DskipTests package
```

`peer-node` có test integration phụ thuộc artifact `bootstrap-server`. Script `run.bat` tự chuẩn bị artifact này bằng Maven, nhưng không khởi động bootstrap server.

## Docker Bootstrap Server

Build và chạy tracker bằng Docker:

```bat
docker build -f bootstrap-server/Dockerfile -t p2p-bootstrap-server .
docker run --rm -p 9000:9000 -v p2p-bootstrap-data:/data p2p-bootstrap-server
```

Hoặc dùng Compose:

```bat
docker compose up --build bootstrap-server
```

Có thể đổi port/database:

```bat
docker run --rm -p 9100:9100 ^
  -e BOOTSTRAP_SERVER_PORT=9100 ^
  -e BOOTSTRAP_DATABASE_PATH=/data/bootstrap-server.db ^
  -v p2p-bootstrap-data:/data ^
  p2p-bootstrap-server
```

## Kiến Trúc Tổng Quan

```text
peer-node A                                      peer-node B
  Swing UI                                         TCPServer
  PeerNode facade                                  MessageReceiver
  Chat/Group/Retry services          TCP JSON      MessageRouter
  MessageSender + TCPClient  ------------------>   InboundMessageService
       |
       | TCP text commands
       v
bootstrap-server
  REGISTER / JOIN / LEAVE / LIST
  CREATE_GROUP / ADD_GROUP_MEMBER / LIST_GROUPS
  STORE_OFFLINE
  SQLite persistence
```

`bootstrap-server` không chuyển tiếp tin nhắn chat trực tiếp. Tin 1-1 và group chat được gửi peer-to-peer bằng TCP. Tracker chỉ hỗ trợ tìm peer và lưu tin offline khi gửi trực tiếp thất bại.

## Chức Năng

- Peer discovery qua bootstrap `JOIN/LIST`.
- Fallback discovery qua peer đã biết bằng `PEER_LIST_REQUEST`.
- Chat 1-1 trực tiếp giữa peer qua TCP.
- Chat nhóm tới từng member.
- Broadcast toàn mạng tới các peer online đang biết.
- ACK, retry và timeout khi gửi TCP.
- Store-and-forward qua bootstrap khi peer nhận offline.
- Hiển thị online/offline và refresh trạng thái định kỳ.
- Lưu lịch sử message/group local bằng JSON theo từng profile peer.
- Đổi tên nhóm và thêm thành viên nhóm; sync trực tiếp chạy nền để UI không bị đứng.

## Đáp Ứng Yêu Cầu Đồ Án

| Yêu cầu | Triển khai |
| --- | --- |
| Mỗi peer gửi và nhận đồng thời | `peer-node` có `TCPServer` chạy nền và `TCPClient` gửi tin. |
| TCP socket hoặc tương đương | Peer-to-peer dùng TCP socket; bootstrap cũng dùng TCP text protocol. |
| Xử lý nhiều kết nối | `TCPServer` và `BootstrapServer` dùng cached thread pool. |
| Peer discovery | Bootstrap `JOIN/LIST`, cộng fallback peer-to-peer `PEER_LIST_REQUEST`. |
| Chat trực tiếp | `MessageType.CHAT` gửi trực tiếp tới peer đích. |
| Chat nhóm | `MessageType.GROUP_CHAT` gửi tới từng member. |
| Online/offline | Tracker giữ peer online runtime, peer refresh định kỳ. |
| Truyền tin đáng tin cậy | ACK, retry, socket timeout, offline fallback. |
| Broadcast | `NetworkBroadcastImpl` gửi tới toàn bộ peer online. |
| Store-and-forward | `STORE_OFFLINE`, peer nhận lại khi `JOIN`. |

Mã hóa tin nhắn là chức năng nâng cao khuyến khích, hiện chưa triển khai.

## Tài Liệu Module

- [peer-node/README.md](peer-node/README.md)
- [bootstrap-server/README.md](bootstrap-server/README.md)

## Cấu Trúc Thư Mục

```text
p2p/
  pom.xml
  run.bat
  run-bootstrap.bat
  docker-compose.yml
  docs/
    require.md
    system_design.md
  peer-node/
  bootstrap-server/
```

