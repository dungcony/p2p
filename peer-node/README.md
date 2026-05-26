# Peer Node

`peer-node` là ứng dụng desktop Swing đại diện cho một peer trong hệ thống P2P Chat. Mỗi peer tự lắng nghe TCP để nhận tin và tự mở TCP client khi gửi tin.

Khi chạy cùng `bootstrap-server`, peer có thêm discovery tự động, trạng thái online/offline, group metadata và offline message.

## Chạy Nhanh

Từ thư mục gốc project:

```bat
run-peer.bat
```

Không truyền tham số thì app tự chọn luồng profile:

- Có profile người dùng thật: vào thẳng màn chat.
- Chưa có profile: mở dialog nhập tên peer.
- Không hỏi tên trong terminal.

Chạy profile demo:

```bat
run-peer.bat --profile=alice
run-peer.bat --profile=bob
run-peer.bat --profile=carol
```

Tạo/chạy theo tên:

```bat
run-peer.bat --peer-name=Dung
```

Nếu muốn discovery/offline message, chạy bootstrap ở terminal riêng:

```bat
run-bootstrap.bat
```

## Tham Số Runtime

| Tham số | Ý nghĩa |
| --- | --- |
| `--profile=<id>` | Nạp profile có sẵn trong `<data-root>/<id>/config.properties`. |
| `--peer-name=Dung` | Tìm profile theo tên hoặc tạo profile mới. |
| `--peer-port=5001` / `--port=5001` | Override port TCP peer lắng nghe. |
| `--data-dir=tmp/demo-data` | Thư mục gốc chứa profile, message history và group cache. |

Khi tạo profile mới, `peer.id` là UUID nội bộ do app tự sinh. Người dùng chỉ nhập tên hiển thị.

## Cấu Hình Và Dữ Liệu

Data root mặc định là `runtime-data/peer-node`.

Bootstrap config dùng chung:

```text
runtime-data/peer-node/config.properties
```

Ví dụ:

```properties
bootstrap.host=localhost
bootstrap.port=9000
```

Mỗi profile peer có thư mục riêng:

```text
<data-root>/<peer-id-or-profile-id>/
  config.properties
  messages.json
  groups.json
```

| File | Nội dung |
| --- | --- |
| `config.properties` | `peer.id`, `peer.name`, `peer.port`. |
| `messages.json` | Direct/group/broadcast history và status. |
| `groups.json` | Group local cache. |

## Chức Năng

- Gửi/nhận chat 1-1 trực tiếp qua TCP.
- Gửi/nhận group chat tới từng member.
- Conversation `[Thế giới]` cho broadcast tới peer online.
- Tạo nhóm, thêm thành viên, đổi tên nhóm.
- Sync membership nhóm qua bootstrap và trực tiếp bằng `GROUP_MEMBERS_SYNC`.
- ACK, retry, timeout.
- Store offline message lên bootstrap khi direct/group send thất bại.
- Nhận offline message khi peer `JOIN` lại bootstrap.
- Peer discovery qua bootstrap hoặc qua peer đã biết.
- Local history tách riêng direct, group và broadcast.

## Kiến Trúc Package

| Package | Trách nhiệm |
| --- | --- |
| `dungcony.ds.app` | `PeerNode` facade, factory dependency, runtime lifecycle. |
| `dungcony.ds.config` | Runtime option và value object profile. |
| `dungcony.ds.repositories` | JSON local repository và profile repository. |
| `dungcony.ds.network` | TCP client/server, JSON message protocol, bootstrap client. |
| `dungcony.ds.services.interfaces` | Contract nghiệp vụ. |
| `dungcony.ds.services.impl` | Chat, group, broadcast, discovery, retry, bootstrap sync, presence. |
| `dungcony.ds.ui` | Swing UI. |
| `dungcony.ds.model`, `dtos`, `enums` | Runtime model và payload. |

## Luồng Chính

Chat 1-1:

```text
UI -> PeerNode -> ChatImpl -> MessageSender -> TCPClient
   -> peer đích TCPServer -> MessageReceiver -> MessageRouter
   -> InboundMessageImpl -> MessageHistoryImpl -> ACK
```

Bootstrap sync:

```text
PeerNodeRuntime -> BootstrapSyncImpl -> BootstrapClient
   -> REGISTER/JOIN -> online peers + groups + offline messages
   -> PeerDirectory + GroupRegistry + MessageHistory
```

Broadcast:

```text
ChatScreen([Thế giới]) -> SendMessageBox -> PeerNode.broadcastToNetwork
   -> NetworkBroadcastImpl -> MessageSender -> peer online
   -> history __broadcast__
```

## Maven

Chuẩn bị dependency test nếu chạy riêng module:

```bat
mvn -pl bootstrap-server -am -DskipTests install
```

Chạy app:

```bat
mvn -pl peer-node -DskipTests compile exec:java -Dexec.mainClass="dungcony.ds.App"
```

Chạy với args:

```bat
mvn -pl peer-node -DskipTests compile exec:java -Dexec.mainClass="dungcony.ds.App" "-Dexec.args=--peer-name=Dung"
```

Test:

```bat
mvn -pl peer-node test
```

## Ghi Chú Vận Hành

- Nếu bootstrap không chạy, peer vẫn mở TCP listener và có thể chat tới peer đã biết IP:port.
- Nếu bootstrap chạy, peer tự refresh online peers, group và offline messages mỗi 5 giây.
- Broadcast `[Thế giới]` là realtime: peer offline không nhận lại tin cũ.
- Group member lưu theo `peer.id`, nhưng gửi TCP cần IP:port runtime từ discovery.
