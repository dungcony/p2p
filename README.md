# P2P Chat System

Hệ thống chat ngang hàng bằng Java 21. Mỗi peer vừa là client gửi tin, vừa là TCP server nhận tin trực tiếp từ peer khác. `bootstrap-server` chỉ là tracker hỗ trợ peer discovery, online/offline, group metadata và store-and-forward khi gửi trực tiếp thất bại.

## Modules

| Module | Vai trò |
| --- | --- |
| `peer-node` | Ứng dụng Swing của mỗi peer; chat 1-1, group, broadcast `[Thế giới]`, local JSON history. |
| `bootstrap-server` | Tracker TCP độc lập; online registry trong RAM, user/group/offline message bằng SQLite. |

## Chạy Nhanh

Chạy tracker:

```bat
run-bootstrap.bat
```

Chạy một peer:

```bat
run-peer.bat
```

Khi không truyền tham số, app mở UI profile:

- Có profile người dùng thật thì vào thẳng màn chat.
- Nếu chưa có profile thì app mở dialog nhập tên.
- Không nhập tên trong terminal; `peer.id` và port được app tự sinh/tự chọn.

Chạy demo:

```bat
run-all-peers.bat
```

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

## Build Và Test

```bat
mvn test
mvn -DskipTests compile
```

Chạy từng module:

```bat
mvn -pl bootstrap-server test
mvn -pl peer-node test
```

`run-peer.bat` tự chuẩn bị artifact `bootstrap-server` bằng:

```bat
mvn -pl bootstrap-server -am -DskipTests install
```

Bước này chỉ phục vụ Maven resolution, không khởi động bootstrap server.

## Kiến Trúc

```text
peer-node A                              peer-node B
  Swing UI                                 Swing UI
  PeerNode facade                          PeerNode facade
  Chat/Group/Broadcast services            Inbound services
  TCPClient + TCPServer   <------------>   TCPServer + TCPClient
          \                                  /
           \ REGISTER/JOIN/LIST/STORE_OFFLINE
            v
        bootstrap-server
        Tracker + SQLite
```

Luồng chính:

- `CHAT`, `GROUP_CHAT`, `BROADCAST` gửi trực tiếp peer-to-peer bằng TCP JSON.
- Receiver trả `ACK` cùng `message.id`.
- Sender retry tối đa 3 lần nếu timeout hoặc ACK không hợp lệ.
- Direct/group message có thể store offline qua bootstrap.
- Broadcast `[Thế giới]` chỉ gửi tới peer online, không store offline.

## Chức Năng

- Peer discovery qua bootstrap `JOIN/LIST`.
- Fallback discovery qua peer đã biết bằng `PEER_LIST_REQUEST`.
- Chat 1-1 trực tiếp.
- Chat nhóm tới từng member.
- Broadcast toàn mạng qua conversation `[Thế giới]`.
- ACK, retry, timeout.
- Store-and-forward cho direct/group message.
- Online/offline refresh định kỳ.
- Local history theo profile bằng `messages.json`, `groups.json`.
- Tạo nhóm, thêm thành viên, đổi tên nhóm, sync membership nền.

## Dữ Liệu

Mặc định:

```text
runtime-data/peer-node/
```

Cấu trúc:

```text
runtime-data/peer-node/
├── config.properties
└── <peer-id-or-profile-id>/
    ├── config.properties
    ├── messages.json
    └── groups.json
```

## Tài Liệu

- [Yêu cầu](docs/require.md)
- [Thiết kế chốt](docs/design.md)
- [Bộ báo cáo nộp bài](docs/nop-bai/00_muc_luc.md)
- [Peer node README](peer-node/README.md)
- [Bootstrap server README](bootstrap-server/README.md)
