# Thiết Kế Hệ Thống Chat P2P

## 1. Tổng Quan Kiến Trúc

```
[Peer A] ←──── TCP trực tiếp ────→ [Peer B]
    │                                   │
    └──────────→ [Bootstrap Server] ←───┘
                 (Tracker)
```

Hệ thống gồm 2 thành phần chính:

- **Bootstrap Server (Tracker):** Đóng vai trò **danh bạ** — lưu danh sách peer và IP:Port của từng người. Tin nhắn **không đi qua Tracker**, chỉ có thông tin địa chỉ mới đi qua. Ngoại lệ duy nhất: buffer tin nhắn tạm thời khi peer offline (store-and-forward).
- **Peer Node:** Mỗi peer vừa là client (gửi) vừa là server (nhận), kết nối **trực tiếp** với peer khác qua TCP sau khi tra địa chỉ từ Tracker.

---

## 2. Bootstrap Server

### 2.1. Chức năng

- Lưu danh sách peer đang online
- Cung cấp IP:Port của peer khi được hỏi
- Buffer tin nhắn cho peer offline (store-and-forward)

### 2.2. Cơ sở dữ liệu

```sql
-- Danh sách peer
CREATE TABLE peers (
    peer_id       TEXT PRIMARY KEY,
    display_name  TEXT NOT NULL,
    ip            TEXT NOT NULL,
    port          INTEGER NOT NULL,
    public_key    TEXT,
    last_seen     TIMESTAMP
);

-- Buffer tin nhắn offline
CREATE TABLE pending_messages (
    message_id        TEXT PRIMARY KEY,
    from_peer         TEXT NOT NULL,
    to_peer           TEXT NOT NULL,
    encrypted_content TEXT NOT NULL,
    created_at        TIMESTAMP,
    expires_at        TIMESTAMP  -- tự xóa sau 7 ngày
);
```

### 2.3. API (TCP Socket)

| Lệnh | Mô tả |
|---|---|
| `REGISTER <peer_id> <ip> <port> <public_key>` | Đăng ký tham gia mạng |
| `GET_PEERS` | Lấy danh sách peer online |
| `GET_PEER <peer_id>` | Lấy IP:Port của một peer |
| `STORE_MSG <message_json>` | Lưu tin nhắn khi peer offline |
| `GET_PENDING <peer_id>` | Lấy tin nhắn đang chờ |
| `HEARTBEAT <peer_id>` | Cập nhật trạng thái online |
| `UNREGISTER <peer_id>` | Rời mạng |

---

## 3. Peer Node

### 3.1. Kiến trúc đa luồng

```
PeerNode
├── Thread: ServerListener      ← lắng nghe TCP, nhận tin từ peer khác
├── Thread: MessageSender       ← gửi tin đến peer khác
├── Thread: HeartbeatSender     ← ping Bootstrap mỗi 30 giây
└── Thread: PendingFetcher      ← lấy tin nhắn offline khi vừa online
```

### 3.2. Luồng khởi động

```
1. Peer khởi động
2. Nhập username + password
3. Đăng ký lên Bootstrap Server (IP + Port + public_key)
4. Lấy danh sách peer online từ Bootstrap
5. Lấy pending messages (tin nhắn nhận được lúc offline)
6. Bắt đầu lắng nghe kết nối TCP đến
```

### 3.3. Luồng gửi tin 1-1

```
A muốn nhắn B:
1. Có IP:Port của B trong local cache?
   ├── Có → kết nối thẳng tới B
   └── Không → hỏi Tracker (GET_PEER B) → lưu cache → kết nối

2. Gửi message (kèm message_id)
3. Chờ ACK từ B (timeout 5 giây)
4. Không nhận ACK → retry tối đa 3 lần

5. Vẫn thất bại?
   ├── IP có thể cũ → hỏi lại Tracker → thử lại 1 lần nữa
   └── Vẫn thất bại → B offline → STORE_MSG lên Bootstrap
```

### 3.4. Luồng gửi tin nhóm

```
A gửi nhóm [B, C, D]:
1. Với từng thành viên → áp dụng luồng gửi 1-1 (cache trước, hỏi Tracker nếu cần)
2. Gửi song song đến B, C, D
3. Chờ ACK từng người
4. Ai offline → STORE_MSG lên Bootstrap cho người đó
```

### 3.5. Cơ sở dữ liệu local (SQLite)

```sql
-- Lịch sử tin nhắn
CREATE TABLE messages (
    message_id  TEXT PRIMARY KEY,
    from_peer   TEXT NOT NULL,
    to_peer     TEXT,
    group_id    TEXT,
    content     TEXT NOT NULL,
    timestamp   TIMESTAMP,
    status      TEXT  -- sent / received / pending
);

-- Danh bạ
CREATE TABLE contacts (
    peer_id       TEXT PRIMARY KEY,
    display_name  TEXT NOT NULL,
    public_key    TEXT
);

-- Nhóm chat
CREATE TABLE groups (
    group_id    TEXT PRIMARY KEY,
    group_name  TEXT NOT NULL,
    members     TEXT  -- JSON array of peer_ids
);
```

---

## 4. Cấu Trúc Message

```json
{
    "message_id": "uuid-v4",
    "type": "CHAT | ACK | GROUP | JOIN | LEAVE",
    "from": "peer_id_A",
    "to": "peer_id_B",
    "group_id": null,
    "content": "Nội dung tin nhắn (đã mã hóa nếu bật)",
    "timestamp": 1700000000000
}
```

### Các loại message

| Type | Mô tả |
|---|---|
| `CHAT` | Tin nhắn 1-1 |
| `ACK` | Xác nhận đã nhận |
| `GROUP` | Tin nhắn nhóm |
| `JOIN` | Thông báo tham gia mạng |
| `LEAVE` | Thông báo rời mạng |

---

## 5. Cơ Chế Truyền Tin Đáng Tin Cậy

```
Gửi message
    │
    ▼
Chờ ACK (timeout 5s)
    │
    ├── Nhận ACK → ✅ Thành công
    │
    └── Timeout → Retry (tối đa 3 lần)
                    │
                    ├── ACK nhận được → ✅ Thành công
                    │
                    └── Vẫn thất bại → Peer offline
                                        → Store trên Bootstrap
                                        → Notify khi peer online lại
```

---

## 6. Mã Hóa (Nâng Cao)

Sử dụng **RSA-2048** cho trao đổi key, **AES-256** cho nội dung:

```
Khởi động lần đầu:
→ Tạo cặp RSA keypair (public + private)
→ Upload public key lên Bootstrap Server

Gửi tin cho B:
1. Lấy public key của B từ Bootstrap
2. Tạo AES session key ngẫu nhiên
3. Mã hóa content bằng AES session key
4. Mã hóa AES key bằng RSA public key của B
5. Gửi (encrypted_content + encrypted_aes_key)

Nhận tin:
1. Giải mã AES key bằng RSA private key của mình
2. Giải mã content bằng AES key
```

---

## 7. Cấu Trúc Module Java

```
src/
├── server/
│   ├── BootstrapServer.java        ← main server
│   ├── PeerRegistry.java           ← quản lý danh sách peer
│   └── PendingMessageStore.java    ← buffer tin nhắn offline
│
├── peer/
│   ├── PeerNode.java               ← khởi động peer, điều phối
│   ├── ServerListener.java         ← lắng nghe kết nối đến (thread)
│   ├── MessageSender.java          ← gửi tin, retry, ACK
│   ├── HeartbeatSender.java        ← ping Bootstrap định kỳ
│   └── PeerDiscovery.java          ← tìm kiếm peer qua Bootstrap
│
├── model/
│   ├── Message.java                ← cấu trúc message
│   ├── Peer.java                   ← thông tin peer
│   └── Group.java                  ← thông tin nhóm
│
├── storage/
│   └── LocalDatabase.java          ← SQLite local (messages, contacts)
│
├── crypto/
│   ├── KeyManager.java             ← tạo và lưu RSA keypair
│   └── Encryption.java             ← mã hóa/giải mã (nâng cao)
│
└── ui/
    └── ChatUI.java                 ← giao diện người dùng
```

---

## 8. Stack Công Nghệ

| Mục đích | Thư viện |
|---|---|
| TCP Socket | `java.net.Socket` (built-in) |
| Đa luồng | `ExecutorService` (built-in) |
| SQLite local | `sqlite-jdbc` |
| JSON | `Gson` hoặc `Jackson` |
| Mã hóa | `javax.crypto` (built-in) hoặc `Bouncy Castle` |
| Bootstrap Server | Socket thuần hoặc `Spring Boot` |

---

## 9. Đáp Ứng Yêu Cầu Đồ Án

| Yêu cầu | Giải pháp |
|---|---|
| Tham gia mạng P2P | Đăng ký với Bootstrap Server |
| Chat 1-1 | TCP trực tiếp giữa 2 peer |
| Chat nhóm | Broadcast đến từng thành viên |
| Peer Discovery | Bootstrap trả về danh sách peer online |
| Trạng thái online/offline | Heartbeat + last_seen |
| Truyền tin đáng tin cậy | ACK + Retry + Timeout |
| Giao tiếp TCP | `java.net.Socket` |
| Đa luồng | `ExecutorService` |
| Broadcast (nâng cao) | Gửi đồng thời đến nhiều peer |
| Store-and-forward (nâng cao) | Buffer trên Bootstrap khi offline |
| Mã hóa (nâng cao) | RSA + AES |
