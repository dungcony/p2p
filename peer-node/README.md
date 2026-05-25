# Peer Node

`peer-node` là ứng dụng desktop Swing đại diện cho một peer trong hệ thống P2P Chat. Mỗi peer tự lắng nghe TCP để nhận tin từ peer khác và tự mở TCP client khi gửi tin. Module này có thể chạy riêng, không bắt buộc bootstrap server phải đang mở.

Khi chạy cùng `bootstrap-server`, peer có thêm peer discovery, danh sách online, group metadata và offline message.

## Chạy Nhanh

Từ thư mục gốc project:

```bat
run.bat --profile=alice
```

Mở peer khác ở terminal mới:

```bat
run.bat --profile=bob
```

Nếu muốn dùng discovery/offline message, chạy bootstrap server ở terminal riêng:

```bat
run-bootstrap.bat
```

`run.bat` có bước chuẩn bị Maven artifact của `bootstrap-server` vì integration test của `peer-node` dùng module đó. Bước này không khởi động bootstrap server.

## Tham Số Runtime

| Tham số | Ý nghĩa |
| --- | --- |
| `--profile=alice` | Nạp profile có sẵn trong `<data-root>/alice/config.properties`. |
| `--peer-name=Alice` | Tên hiển thị của peer; nếu chưa có profile cùng tên thì app tự sinh `peer.id` và tự chọn port trống. |
| `--peer-port=5001` hoặc `--port=5001` | Port TCP mà peer lắng nghe. |
| `--data-dir=tmp/demo-data` | Thư mục gốc chứa các profile, message history và group cache. |

Nếu chạy `run.bat` không truyền tham số, script chỉ hỏi tên hiển thị trong terminal rồi truyền vào app. Luồng chọn/tạo profile bằng dialog đã được bỏ để mỗi tiến trình peer có cấu hình rõ ràng ngay từ lúc khởi động.
Khi tạo profile mới, người dùng không nhập `peer.id`; id là UUID nội bộ do app tự sinh.

## Cấu Hình

Bootstrap config dùng chung nằm trong:

```text
<data-root>/config.properties
```

Ví dụ mặc định:

```properties
bootstrap.host=localhost
bootstrap.port=9000
```

Mỗi profile peer có thư mục riêng:

```text
<data-root>/<peer-id-uuid>/
  config.properties
  messages.json
  groups.json
```

`config.properties` trong profile lưu `peer.id`, `peer.name`, `peer.port`. `messages.json` và `groups.json` là dữ liệu local của peer đó.

## Chức Năng

- Khởi động trực tiếp bằng profile hoặc tham số CLI.
- Gửi/nhận chat 1-1 trực tiếp qua TCP.
- Gửi/nhận group chat.
- Tạo nhóm, thêm thành viên, đổi tên nhóm.
- Sync membership trực tiếp chạy nền để UI không bị đứng khi peer chưa sẵn sàng ACK.
- Broadcast message tới các peer online đang biết.
- ACK, retry, timeout khi gửi TCP.
- Store offline message lên bootstrap khi gửi trực tiếp thất bại.
- Nhận offline message khi peer `JOIN` lại bootstrap.
- Lưu lịch sử chat và group local theo từng profile.
- Peer discovery qua bootstrap hoặc qua peer đã biết.

## Kiến Trúc

Package chính:

| Package | Trách nhiệm |
| --- | --- |
| `dungcony.ds.app` | `PeerNode` facade, factory dependency, runtime lifecycle. |
| `dungcony.ds.config` | Profile, runtime option, cấu hình peer. |
| `dungcony.ds.network` | TCP client/server, JSON message protocol, bootstrap client. |
| `dungcony.ds.services.interfaces` | Contract theo nghiệp vụ để giữ DIP/ISP. |
| `dungcony.ds.services.impl` | Chat, group, discovery, retry, bootstrap sync, presence. |
| `dungcony.ds.repositories` | Lưu message/group local bằng JSON. |
| `dungcony.ds.ui` | Swing UI. |
| `dungcony.ds.model`, `dtos`, `enums` | Dữ liệu runtime và payload. |

Luồng gửi chat 1-1:

```text
UI -> PeerNode -> ChatService -> MessageSender -> TCPClient
   -> peer đích TCPServer -> MessageReceiver -> MessageRouter
   -> InboundMessageService -> MessageHistoryService -> ACK
```

Luồng join bootstrap:

```text
PeerNodeRuntime -> BootstrapSyncService -> BootstrapClient
   -> REGISTER/JOIN -> online peers + groups + offline messages
   -> PeerDirectoryService + GroupRegistry + MessageHistoryService
```

## Chạy Bằng Maven

Chuẩn bị dependency test nếu chạy riêng module:

```bat
mvn -pl bootstrap-server -am -DskipTests install
```

Chạy app:

```bat
mvn -pl peer-node exec:java -Dexec.mainClass="dungcony.ds.App" -Dexec.args="--peer-port=5001 --data-dir=tmp/alice"
```

Compile:

```bat
mvn -pl peer-node -am -DskipTests compile
```

Test toàn bộ project từ root:

```bat
mvn test
```

## Ghi Chú Vận Hành

- Nếu bootstrap không chạy, peer vẫn có thể chat trực tiếp với peer đã biết địa chỉ `host:port`.
- Nếu bootstrap chạy, peer tự refresh danh sách online và group định kỳ.
- Group membership được lưu ở bootstrap để peer mới join sau vẫn đồng bộ được group.
- Direct sync group là best-effort chạy nền; thất bại không làm UI bị đứng.
