# Peer Node

`peer-node` là ứng dụng desktop Swing đại diện cho một node trong hệ thống chat P2P. Mỗi peer vừa là client gửi tin nhắn, vừa là server TCP nhận tin từ peer khác. Khi chạy cùng `bootstrap-server`, peer có thể đăng ký tham gia mạng, lấy danh sách peer online, đồng bộ group và nhận lại tin nhắn offline.

## Yêu cầu

- Java 21
- Maven 3.9+
- `bootstrap-server` đang chạy nếu muốn dùng peer discovery, online/offline và offline message

## Chạy nhanh

Từ thư mục gốc project:

```bat
run-bootstrap.bat
```

Mở một terminal khác cho peer đầu tiên:

```bat
run.bat --peer-port=5001
```

Mở thêm terminal cho các peer tiếp theo, mỗi peer dùng một port khác:

```bat
run.bat --peer-port=5002
run.bat --peer-port=5003
```

Có thể chỉ định thư mục dữ liệu riêng cho từng peer:

```bat
run.bat --peer-port=5001 --data-dir=tmp/alice
run.bat --peer-port=5002 --data-dir=tmp/bob
```

Chạy trực tiếp bằng Maven:

```bat
mvn -pl peer-node exec:java -Dexec.mainClass="dungcony.ds.App" -Dexec.args="--peer-port=5001 --data-dir=tmp/alice"
```

## Cấu hình

File cấu hình bootstrap mặc định:

```text
peer-node/src/main/resources/data/config.properties
```

Các khóa chính:

```properties
bootstrap.host=localhost
bootstrap.port=9000
```

Mỗi profile peer có thư mục dữ liệu riêng, gồm:

- `config.properties`: thông tin peer id, tên, port và bootstrap.
- `messages.json`: lịch sử tin nhắn local.
- `groups.json`: danh sách group local.

## Chức năng chính

- Tham gia mạng qua `bootstrap-server` bằng `REGISTER` và `JOIN`.
- Nhận danh sách peer online từ tracker và cập nhật định kỳ.
- Gửi chat 1-1 trực tiếp qua TCP socket giữa các peer.
- Nhận tin song song bằng `TCPServer` và thread pool cho từng kết nối.
- ACK cho message đến; sender retry khi timeout hoặc không nhận ACK.
- Lưu tin offline lên bootstrap khi gửi trực tiếp thất bại.
- Nhận lại offline message khi peer đăng nhập lại.
- Tạo group, đồng bộ membership và broadcast message đến các thành viên.
- Broadcast message đến toàn bộ peer online đang biết.
- Fallback peer discovery qua `PEER_LIST_REQUEST` nếu đã biết một peer khác.

## Kiến trúc

Các package chính:

- `dungcony.ds.app`: facade `PeerNode`, factory lắp dependency và runtime lifecycle.
- `dungcony.ds.network`: TCP client/server, protocol JSON, bootstrap client.
- `dungcony.ds.services.interfaces`: contract nghiệp vụ theo từng nhóm trách nhiệm.
- `dungcony.ds.services.impl`: implementation cho chat, group, discovery, retry, bootstrap sync.
- `dungcony.ds.repositories`: lưu message/group local bằng JSON.
- `dungcony.ds.ui`: giao diện Swing.
- `dungcony.ds.model`, `dtos`, `enums`: dữ liệu trao đổi trong runtime và qua mạng.

Luồng gửi tin 1-1:

```text
UI -> PeerNode -> ChatService -> MessageSender -> TCPClient
   -> peer đích TCPServer -> MessageReceiver -> MessageRouter
   -> InboundMessageService -> MessageHistoryService -> ACK
```

Luồng join bootstrap:

```text
PeerNodeRuntime -> BootstrapSyncService -> BootstrapClient
   -> REGISTER/JOIN -> nhận online peers, groups, offline messages
   -> PeerDirectoryService + GroupRegistry + MessageHistoryService
```

## Đáp ứng yêu cầu đồ án

| Yêu cầu | Hiện trạng |
| --- | --- |
| Peer vừa gửi vừa nhận | `PeerNode` gửi qua service, nhận qua `TCPServer` chạy nền. |
| Giao tiếp mạng TCP hoặc tương đương | Peer-to-peer dùng TCP socket; bootstrap cũng dùng TCP text protocol. |
| Peer discovery | Qua `JOIN/LIST` của bootstrap và fallback `PEER_LIST_REQUEST`. |
| Chat trực tiếp 1-1 | `ChatImpl` gửi `MessageType.CHAT` trực tiếp tới peer đích. |
| Chat nhóm | `GroupChatImpl` gửi `GROUP_CHAT` tới từng member và sync membership. |
| Online/offline | Bootstrap giữ danh sách online, peer refresh định kỳ và cập nhật local directory. |
| Tin đáng tin cậy | `MessageSender` kiểm tra ACK, retry 3 lần, TCP timeout; thất bại thì lưu offline nếu có bootstrap. |
| Xử lý nhiều kết nối | `TCPServer` dùng cached thread pool cho các socket đến. |
| Store-and-forward | `STORE_OFFLINE` trên bootstrap, peer nhận lại qua `JOIN`. |
| Broadcast toàn mạng | `NetworkBroadcastImpl` gửi tới toàn bộ peer online thu thập từ bootstrap và danh bạ local. |

## SOLID

- Single Responsibility: network, routing, chat, group, discovery, retry, persistence và UI được tách thành class/service riêng.
- Open/Closed: `MessageRouterImpl` dùng map `MessageType -> handler`, thêm loại message mới không cần sửa luồng receive chính.
- Liskov Substitution: các implementation được dùng qua interface service/gateway tương ứng.
- Interface Segregation: interface được chia nhỏ theo nghiệp vụ như `ChatService`, `PeerDiscoverService`, `MessageHistoryService`, `PeerBootstrapGateway`.
- Dependency Inversion: `PeerNode` và service phụ thuộc vào abstraction; `PeerNodeFactory` là nơi lắp implementation cụ thể.

## Kiểm thử

Chạy toàn bộ test từ root project:

```bat
mvn test
```

Bộ test hiện kiểm tra:

- Runtime option parser.
- Bootstrap tracker: `JOIN`, `LEAVE`, `LIST`, offline message.
- Peer integration: gửi trực tiếp có ACK, offline delivery khi peer join lại, group chat và network broadcast.
- Lưu/cập nhật lịch sử message local không bị duplicate khi retry.

