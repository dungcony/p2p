# Mục Lục Báo Cáo

Đây là bộ tài liệu nộp bài cho đồ án **P2P Chat System**. Nội dung được viết theo `docs/require.md` và trạng thái code hiện tại của project.

## 1. Thông Tin Project

| Mục | Giá trị |
| --- | --- |
| Tên hệ thống | P2P Chat System |
| Ngôn ngữ | Java 21 |
| UI | Java Swing |
| Giao tiếp mạng | TCP socket |
| Build tool | Maven multi-module |
| Module chính | `peer-node`, `bootstrap-server` |
| Lưu trữ peer | JSON local theo profile |
| Lưu trữ tracker | SQLite |
| Dữ liệu runtime | `runtime-data/` |

## 2. Năm Phần Báo Cáo Chính

| STT | File | Nội dung |
| --- | --- | --- |
| 1 | [01_huong_dan_chay.md](01_huong_dan_chay.md) | Cách build, chạy bootstrap, chạy peer, chạy demo, tham số CLI, dữ liệu runtime và kịch bản kiểm thử thủ công. |
| 2 | [02_bao_cao_kien_truc_he_thong.md](02_bao_cao_kien_truc_he_thong.md) | Kiến trúc tổng quan, module, luồng runtime, luồng chat, group, broadcast, mã hóa và lưu trữ. |
| 3 | [03_giao_thuc_trao_doi_thong_diep.md](03_giao_thuc_trao_doi_thong_diep.md) | Giao thức TCP peer-to-peer, giao thức command với bootstrap, ACK, retry, schema message và mã hóa payload. |
| 4 | [04_co_che_peer_discovery.md](04_co_che_peer_discovery.md) | Cơ chế khám phá peer, online/offline, fallback discovery, resolve `peer.id` sang IP:port và public key. |
| 5 | [05_xu_ly_loi_va_thu_nghiem_he_thong.md](05_xu_ly_loi_va_thu_nghiem_he_thong.md) | Xử lý lỗi, store-and-forward, kiểm thử tự động, checklist đối chiếu yêu cầu và hạn chế. |

Tài liệu tham khảo thêm:

- [../require.md](../require.md): đề bài/yêu cầu gốc.
- [../design.md](../design.md): bản thiết kế chốt trong quá trình phát triển.
- [../../README.md](../../README.md): README tổng quan ở thư mục gốc project.

## 3. Cấu Trúc Source

```text
p2p/
├── bootstrap-server/          # Tracker TCP, SQLite, offline message, group metadata
├── peer-node/                 # Swing app, PeerNode facade, TCP client/server, local JSON
├── docs/
│   ├── require.md             # Đề bài / yêu cầu
│   ├── design.md              # Thiết kế chốt
│   └── nop-bai/               # Bộ báo cáo nộp bài gồm 5 phần chính
├── runtime-data/              # Dữ liệu runtime local, không nộp kèm git
├── run-bootstrap.bat          # Chạy tracker
├── run-peer.bat               # Chạy một peer
└── run-all-peers.bat          # Chạy bootstrap + Alice/Bob/Carol demo
```

## 4. Luồng Chạy Nhanh

```bat
mvn test
run-bootstrap.bat
run-peer.bat
```

Chạy demo ba peer:

```bat
run-all-peers.bat
```

`run-all-peers.bat` ưu tiên dùng lại profile `alice`, `bob`, `carol` nếu đã có trong `runtime-data/peer-node`. Nếu máy sạch chưa có profile demo, script tạo/chạy theo tên `Alice`, `Bob`, `Carol` với port `5001`, `5002`, `5003`.

## 5. Yêu cầu đã thực hiện

| Yêu cầu | Trạng thái | Bằng chứng chính |
| --- | --- | --- |
| Peer vừa gửi vừa nhận | Đạt | `peer-node` có `TCPClient` và `TCPServer` trong mỗi process peer. |
| Bootstrap/tracker | Đạt | Module `bootstrap-server`, command `REGISTER`, `JOIN`, `LEAVE`, `LIST`. |
| Peer discovery | Đạt | Bootstrap discovery và fallback `PEER_LIST_REQUEST`. |
| Danh sách online/offline | Đạt | `PeerRegistry` TTL, peer refresh `JOIN` định kỳ. |
| Chat 1-1 trực tiếp | Đạt | `ChatImpl` gửi `MessageType.CHAT` qua TCP peer-to-peer. |
| Chat nhóm | Đạt | `GroupChatImpl` gửi `GROUP_CHAT` tới từng member. |
| ACK/retry/timeout | Đạt | `MessageSender`, `TCPClient`. |
| Xử lý nhiều kết nối | Đạt | Peer và bootstrap dùng thread pool. |
| Broadcast toàn mạng | Đạt | Conversation `[Thế giới]`, `NetworkBroadcastImpl`. |
| Store-and-forward | Đạt | `STORE_OFFLINE`, `OfflineMessageRepo`, drain khi receiver `JOIN`. |
| Mã hóa tin nhắn | Đạt | RSA-OAEP SHA-256, public/private key theo profile. |

## 6. Điểm cần chú ý

- Bootstrap không phải chat server trung tâm. Tin online vẫn đi trực tiếp giữa các peer.
- `peer.id` là định danh ổn định; IP:port là địa chỉ runtime dùng để mở socket.
- Group member lưu theo `peer.id`, khi gửi sẽ resolve sang IP:port mới nhất từ discovery.
- Direct/group message có ACK, retry, timeout và fallback offline.
- Broadcast `[Thế giới]` là realtime, chỉ peer online nhận và không lưu offline.
- Dữ liệu runtime đã tách khỏi `src/main/resources`, nằm trong `runtime-data/` và bị `.gitignore`.
