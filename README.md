# P2P Chat System

Đồ án xây dựng hệ thống chat ngang hàng bằng Java 21. Mỗi peer vừa là client gửi tin, vừa là TCP server nhận tin trực tiếp từ peer khác. `bootstrap-server` chỉ đóng vai trò tracker: hỗ trợ peer discovery, trạng thái online/offline, metadata nhóm và store-and-forward khi gửi trực tiếp thất bại.

## Tính Năng Chính

- Peer discovery qua bootstrap `REGISTER`, `JOIN`, `LIST` và fallback qua `PEER_LIST_REQUEST`.
- Chat 1-1 trực tiếp peer-to-peer bằng TCP socket.
- Chat nhóm: sender gửi `GROUP_CHAT` trực tiếp tới từng thành viên.
- Broadcast toàn mạng qua conversation `[Thế giới]` cho các peer đang online.
- ACK, retry và timeout để phát hiện gửi thất bại.
- Store-and-forward cho direct/group message khi receiver offline.
- Mã hóa payload bằng RSA-OAEP SHA-256 theo từng receiver khi có public key.
- Local history theo profile bằng JSON: `messages.json`, `groups.json`.
- Bootstrap lưu user, group, member và offline message bằng SQLite.

## Module

| Module | Vai trò |
| --- | --- |
| `peer-node` | Ứng dụng Swing của mỗi peer; chứa UI, TCP client/server, nghiệp vụ chat, group, broadcast, local JSON repository. |
| `bootstrap-server` | Tracker TCP độc lập; quản lý peer online trong RAM và lưu dữ liệu bền vững bằng SQLite. |

## Cấu Trúc Thư Mục

```text
p2p/
├── bootstrap-server/          # Tracker TCP + SQLite
├── peer-node/                 # Swing app + P2P node
├── docs/
│   ├── require.md             # Đề bài
│   ├── design.md              # Thiết kế chốt
│   └── nop-bai/               # Bộ báo cáo nộp bài
├── run-bootstrap.bat          # Chạy bootstrap server
├── run-peer.bat               # Chạy một peer
└── run-all-peers.bat          # Chạy bootstrap + Alice/Bob/Carol demo
```

## Yêu Cầu Môi Trường

| Thành phần | Yêu cầu |
| --- | --- |
| JDK | Java 21 |
| Maven | 3.9+ |
| Hệ điều hành đã kiểm thử | Windows |
| Bootstrap port mặc định | `9000` |
| Peer port mặc định | Từ `5001`, tự chọn port trống khi tạo profile mới |

Kiểm tra:

```bat
java -version
mvn -version
```

## Chạy Nhanh

Build và chạy test:

```bat
mvn test
```

Chạy bootstrap server:

```bat
run-bootstrap.bat
```

Chạy một peer:

```bat
run-peer.bat
```

Khi chạy `run-peer.bat` không tham số, app mở UI chọn/tạo profile. Người dùng chỉ nhập tên hiển thị trong dialog, không nhập `peer.id` trong terminal. `peer.id` là UUID nội bộ và port được tự chọn nếu chưa có profile.

Chạy demo ba peer:

```bat
run-all-peers.bat
```

`run-all-peers.bat` ưu tiên dùng lại profile `alice`, `bob`, `carol` nếu đã có trong `runtime-data/peer-node`. Nếu máy sạch chưa có dữ liệu demo, script sẽ tạo/chạy theo tên `Alice`, `Bob`, `Carol` với port `5001`, `5002`, `5003`.

## Chạy Theo Tham Số

```bat
run-peer.bat --peer-name=Dung
run-peer.bat --peer-name=Alice --peer-port=5001
run-peer.bat --profile=alice
run-peer.bat --data-dir=tmp/demo --peer-name=Bob --peer-port=5102
```

| Tham số | Ý nghĩa |
| --- | --- |
| `--profile=<id>` | Nạp profile theo folder/id trong data root. |
| `--peer-name=<name>` | Tìm profile theo tên hoặc tạo mới nếu chưa có. |
| `--peer-port=<port>` / `--port=<port>` | Override port lắng nghe của peer. |
| `--data-dir=<path>` | Đổi thư mục dữ liệu runtime của peer. |

## Kiến Trúc Runtime

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

- `CHAT`, `GROUP_CHAT`, `BROADCAST` được gửi trực tiếp giữa các peer bằng TCP JSON một dòng.
- Receiver xử lý message và trả `ACK` cùng `message.id`.
- Sender retry tối đa 3 lần nếu timeout hoặc ACK không hợp lệ.
- Direct/group message thất bại có thể được lưu offline lên bootstrap.
- Broadcast `[Thế giới]` chỉ gửi realtime tới peer online, không lưu offline.

## Dữ Liệu Runtime

Dữ liệu chạy thật không lưu trong `src/main/resources`. Mặc định:

```text
runtime-data/
├── peer-node/
│   ├── config.properties
│   └── <profile-id>/
│       ├── config.properties
│       ├── messages.json
│       └── groups.json
└── bootstrap-server/
    └── bootstrap-server.db
```

`runtime-data/` đã được đưa vào `.gitignore` để tránh nộp kèm history, private key và database runtime. Profile peer lưu `peer.id`, `peer.name`, `peer.port`, public key và private key cục bộ. Bootstrap database lưu user public key, group metadata, group members và offline messages.

## Kiểm Thử

Chạy toàn bộ:

```bat
mvn test
```

Chạy từng module:

```bat
mvn -pl bootstrap-server test
mvn -pl peer-node test
```

Các nhóm test chính:

| Nhóm | Nội dung |
| --- | --- |
| Bootstrap | `JOIN`, `LIST`, `LEAVE`, offline message drain. |
| Peer integration | Chat 1-1, offline delivery, group chat, broadcast. |
| Profile/runtime | Parse CLI, tạo/tìm profile, bỏ qua demo profile khi chạy không args. |
| Local history/group | Không duplicate message, tách broadcast, giữ group local khi bootstrap rỗng. |

## Tài Liệu Nộp Bài

Bộ báo cáo nằm trong [docs/nop-bai](docs/nop-bai/00_muc_luc.md), chia đúng 5 phần chính:

1. [Hướng dẫn chạy hệ thống](docs/nop-bai/01_huong_dan_chay.md)
2. [Báo cáo kiến trúc hệ thống](docs/nop-bai/02_bao_cao_kien_truc_he_thong.md)
3. [Giao thức trao đổi thông điệp](docs/nop-bai/03_giao_thuc_trao_doi_thong_diep.md)
4. [Cơ chế peer discovery](docs/nop-bai/04_co_che_peer_discovery.md)
5. [Xử lý lỗi và thử nghiệm hệ thống](docs/nop-bai/05_xu_ly_loi_va_thu_nghiem_he_thong.md)

## Hạn Chế

- Chưa hỗ trợ NAT traversal, nên demo phù hợp nhất trên cùng máy hoặc cùng LAN có thể kết nối trực tiếp IP:port.
- Bootstrap vẫn là điểm phụ thuộc cho discovery tự động và offline store.
- Broadcast không có offline delivery.
- Offline message được drain khi receiver `JOIN`, chưa có cơ chế delivered/read receipt đầy đủ sau khi peer đọc tin.
