# Mục Lục Báo Cáo

Đây là bộ tài liệu nộp bài cho đồ án **P2P Chat System**. Nội dung đã được chốt theo `docs/require.md` và theo trạng thái code hiện tại.

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

## 2. Cấu Trúc Tài Liệu

| STT | File | Nội dung |
| --- | --- | --- |
| 1 | [01_huong_dan_chay.md](01_huong_dan_chay.md) | Cách build, chạy bootstrap, chạy peer, chạy demo, dữ liệu local và kịch bản kiểm thử thủ công. |
| 2 | [02_bao_cao_kien_truc_he_thong.md](02_bao_cao_kien_truc_he_thong.md) | Kiến trúc tổng quan, module, luồng runtime, luồng chat, lưu trữ và quyết định thiết kế. |
| 3 | [03_giao_thuc_trao_doi_thong_diep.md](03_giao_thuc_trao_doi_thong_diep.md) | Giao thức TCP giữa peer, giao thức command với bootstrap, ACK, retry và message schema. |
| 4 | [04_co_che_peer_discovery.md](04_co_che_peer_discovery.md) | Cơ chế khám phá peer, online/offline, fallback discovery và cách resolve địa chỉ IP:port. |
| 5 | [05_xu_ly_loi_va_thu_nghiem_he_thong.md](05_xu_ly_loi_va_thu_nghiem_he_thong.md) | Xử lý lỗi, store-and-forward, kiểm thử tự động, checklist đối chiếu yêu cầu. |
| 6 | [../design.md](../design.md) | Bản thiết kế chốt theo yêu cầu và code hiện tại. |

## 3. Cấu Trúc Source

```text
p2p/
├── bootstrap-server/          # Tracker TCP, SQLite, offline message, group metadata
├── peer-node/                 # Swing app, PeerNode, TCP client/server, local JSON
├── docs/
│   ├── require.md             # Đề bài / yêu cầu
│   ├── design.md              # Thiết kế chốt
│   └── nop-bai/               # Bộ báo cáo nộp bài
├── run-bootstrap.bat          # Chạy tracker
├── run-peer.bat               # Chạy một peer
├── run.bat                    # Wrapper tương thích ngược cho run-peer.bat
└── run-all-peers.bat          # Chạy bootstrap + Alice/Bob/Carol demo
```

## 4. Luồng Chạy Nhanh

```bat
run-bootstrap.bat
run.bat
```

Khi chạy `run.bat` không tham số:

- Nếu đã có profile người dùng thật, app vào thẳng màn chat.
- Nếu chỉ có profile demo `alice`, `bob`, `carol` hoặc chưa có profile, app mở dialog nhập tên.
- Người dùng không phải nhập tên trong terminal.

Chạy demo ba peer:

```bat
run-all-peers.bat
```

## 5. Các Điểm Cần Nhấn Mạnh Khi Báo Cáo

- Mỗi peer vừa là client gửi tin, vừa là server TCP nhận tin.
- Bootstrap không phải chat server trung tâm; nó chỉ là tracker và nơi lưu tin offline khi gửi trực tiếp thất bại.
- Chat 1-1 và group chat có ACK, retry, timeout.
- Broadcast toàn mạng nằm trong conversation `[Thế giới]`; chỉ peer online nhận, không lưu offline.
- Group member được lưu theo `peer.id`, còn việc ACK/gửi TCP dùng IP:port runtime lấy từ discovery.
- Hệ thống có test tự động cho bootstrap, chat trực tiếp, offline message, group, broadcast, profile và local history.

