# Bootstrap Server

`bootstrap-server` là tracker TCP của hệ thống P2P Chat. Module này không chuyển tiếp tin nhắn chat online. Nhiệm vụ chính là giúp peer tham gia mạng, khám phá peer online, lưu group metadata và lưu offline message để peer nhận lấy lại khi online.

## Chạy Nhanh

Từ thư mục gốc project:

```bat
run-bootstrap.bat
```

Mặc định server lắng nghe port `9000`.

Chạy trực tiếp bằng Maven:

```bat
mvn -pl bootstrap-server exec:java -Dexec.mainClass="dungcony.ds.App" -Dexec.args="9000"
```

Build jar:

```bat
mvn -pl bootstrap-server -am -DskipTests package
java -jar bootstrap-server/target/bootstrap-server-1.0-SNAPSHOT.jar
```

## Cấu Hình

File mặc định:

```text
bootstrap-server/src/main/resources/config.properties
```

Ví dụ:

```properties
server.port=9000
database.path=runtime-data/bootstrap-server/bootstrap-server.db
```

Thứ tự ưu tiên cấu hình:

| Nguồn | Port | Database |
| --- | --- | --- |
| JVM system property | `-Dserver.port=9000` | `-Ddatabase.path=...` |
| Environment variable | `BOOTSTRAP_SERVER_PORT` hoặc `PORT` | `BOOTSTRAP_DATABASE_PATH` |
| Resource file | `server.port` | `database.path` |
| Fallback | `9000` | `runtime-data/bootstrap-server/bootstrap-server.db` |

## Docker

Build image:

```bat
docker build -f bootstrap-server/Dockerfile -t p2p-bootstrap-server .
```

Run:

```bat
docker run --rm -p 9000:9000 -v p2p-bootstrap-data:/data p2p-bootstrap-server
```

Run với port/database tùy chỉnh:

```bat
docker run --rm -p 9100:9100 ^
  -e BOOTSTRAP_SERVER_PORT=9100 ^
  -e BOOTSTRAP_DATABASE_PATH=/data/bootstrap-server.db ^
  -v p2p-bootstrap-data:/data ^
  p2p-bootstrap-server
```

## TCP Protocol

Mỗi request là một dòng text:

```text
COMMAND [JSON_PAYLOAD]
```

| Command | Mục đích | Response |
| --- | --- | --- |
| `REGISTER` | Lưu/cập nhật user vào SQLite. | `OK` |
| `JOIN` | Đánh dấu peer online, trả peer online và offline messages. | JSON `JoinResponse` |
| `LEAVE` | Xóa peer khỏi registry online runtime. | `OK` |
| `LIST` | Trả danh sách peer online. | JSON array `PeerInfo` |
| `STORE_OFFLINE` | Lưu message cho receiver offline. | `OK` |
| `CREATE_GROUP` | Tạo/cập nhật group metadata. | `OK` |
| `ADD_GROUP_MEMBER` | Thêm user vào group. | `OK` |
| `REMOVE_GROUP_MEMBER` | Xóa user khỏi group. | `OK` |
| `LIST_GROUPS` | Liệt kê group metadata. | JSON array |
| `LIST_GROUP_MEMBERS` | Liệt kê member của group. | JSON array |

## Persistence

Bootstrap dùng SQLite để lưu:

- Users.
- Group metadata.
- Group members.
- Offline messages.

Danh sách peer online không lưu DB; nó nằm trong RAM của `PeerRegistry` và được cập nhật qua `JOIN`/`LEAVE`. Peer quá hạn TTL sẽ bị loại khỏi danh sách online runtime.

## Kiến Trúc

| Package | Trách nhiệm |
| --- | --- |
| `dungcony.ds.config` | Load cấu hình, kết nối DB, init schema. |
| `dungcony.ds.models` | TCP server, registry online, DTO nội bộ. |
| `dungcony.ds.entities` | Entity ánh xạ SQLite. |
| `dungcony.ds.repositories` | CRUD SQLite cho user/group/offline message. |

Luồng `JOIN`:

```text
peer-node -> BootstrapClient -> BootstrapServer
  -> PeerRegistry.join()
  -> drainOfflineMessages(receiverId)
  -> response JoinResponse(onlinePeers, offlineMessages)
```

## Test

```bat
mvn -pl bootstrap-server test
mvn test
```
