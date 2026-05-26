# Giao Thức Trao Đổi Thông Điệp

Hệ thống có hai giao thức TCP:

| Giao thức | Bên tham gia | Dạng payload | Mục đích |
| --- | --- | --- | --- |
| Peer-to-peer protocol | `peer-node` với `peer-node` | Một dòng JSON `Message` | Chat 1-1, group, broadcast, ACK, heartbeat, fallback discovery. |
| Bootstrap protocol | `peer-node` với `bootstrap-server` | Một dòng text `COMMAND [JSON_PAYLOAD]` | Register, join, list peer, group metadata, offline message. |

## 1. Peer-To-Peer Protocol

### 1.1. Định dạng truyền

Mỗi lần gửi, `TCPClient` mở một socket tới peer đích, gửi một dòng JSON và chờ một dòng response.

```text
<Message JSON>\n
<Response Message JSON>\n
```

Các lớp liên quan:

| Lớp | Vai trò |
| --- | --- |
| `MessageProtocol` | Serialize/deserialize `Message` bằng Gson. |
| `TCPClient` | Mở socket, gửi payload, đọc response. |
| `TCPServer` | Lắng nghe port local. |
| `ConnectionHandler` | Đọc một dòng request, route, ghi một dòng response. |
| `MessageReceiver` | Chuyển message vào `MessageRouterService`. |
| `MessageRouterImpl` | Phân loại message theo `MessageType`. |

### 1.2. Message schema

Model chính là `dungcony.ds.model.Message`.

| Field | Kiểu | Ý nghĩa |
| --- | --- | --- |
| `id` | `String` | UUID của message. ACK phải dùng cùng id. |
| `type` | `MessageType` | Loại message. |
| `senderId` | `String` | Định danh ổn định của sender. |
| `senderHost`, `senderPort` | `String`, `int` | Địa chỉ runtime của sender. |
| `receiverId` | `String` | Định danh receiver. |
| `receiverHost`, `receiverPort` | `String`, `int` | Địa chỉ runtime của receiver. |
| `groupId`, `groupName` | `String` | Metadata cho group chat. |
| `content` | `String` | Nội dung tin nhắn. |
| `timestamp` | `long` | Thời điểm tạo message. |
| `status` | `MessageStatus` | Trạng thái local: `SENT`, `PENDING`, `FAILED`. |
| `peers` | `List<PeerInfo>` | Dùng trong `PEER_LIST_RESPONSE`. |
| `groupMembers` | `List<PeerInfo>` | Dùng trong `GROUP_MEMBERS_SYNC`. |

Ví dụ `CHAT`:

```json
{
  "id": "8fb2f5c0-2b2f-4f44-a84f-76a5f80bde11",
  "type": "CHAT",
  "senderId": "alice",
  "senderHost": "127.0.0.1",
  "senderPort": 5001,
  "receiverId": "bob",
  "receiverHost": "127.0.0.1",
  "receiverPort": 5002,
  "content": "hello bob",
  "timestamp": 1716200000000,
  "status": "SENT"
}
```

Ví dụ `ACK`:

```json
{
  "id": "8fb2f5c0-2b2f-4f44-a84f-76a5f80bde11",
  "type": "ACK",
  "senderId": "bob",
  "senderHost": "127.0.0.1",
  "senderPort": 5002,
  "receiverId": "alice",
  "receiverHost": "127.0.0.1",
  "receiverPort": 5001,
  "timestamp": 1716200000100,
  "status": "SENT"
}
```

### 1.3. MessageType

| Type | Vai trò | Response |
| --- | --- | --- |
| `CHAT` | Tin nhắn trực tiếp 1-1. | `ACK` cùng id. |
| `GROUP_CHAT` | Tin nhắn nhóm gửi tới một member. | `ACK` cùng id. |
| `BROADCAST` | Tin `[Thế giới]` gửi tới peer online. | `ACK` cùng id. |
| `PEER_LIST_REQUEST` | Hỏi danh sách peer mà peer đích biết. | `PEER_LIST_RESPONSE` cùng id. |
| `PEER_LIST_RESPONSE` | Trả danh sách peer đã biết. | `ACK` cùng id. |
| `GROUP_MEMBERS_SYNC` | Đồng bộ snapshot thành viên nhóm. | `ACK` cùng id. |
| `JOIN` | Control message trực tiếp, đánh dấu sender online. | `ACK` cùng id. |
| `LEAVE` | Control message trực tiếp. | `ACK` mặc định. |
| `HEARTBEAT` | Kiểm tra peer còn reachable không. | `ACK` cùng id. |
| `ACK` | Xác nhận đã xử lý message. | Không dùng như request nghiệp vụ. |

### 1.4. ACK, timeout và retry

`TCPClient` cấu hình:

| Giá trị | Mặc định |
| --- | --- |
| Connect timeout | `2000 ms` |
| Read timeout | `3000 ms` |

`MessageSender` cấu hình:

| Giá trị | Mặc định |
| --- | --- |
| Số lần retry | `3` |
| Delay giữa retry | `300 ms` |

ACK hợp lệ khi:

```text
response != null
response.type == ACK
response.id == message.id
```

Nếu response null, sai type hoặc sai id, sender retry. Sau khi retry hết, service nghiệp vụ quyết định:

- Chat 1-1: store offline nếu bootstrap khả dụng.
- Group chat: store offline cho member thất bại nếu bootstrap khả dụng.
- Broadcast: không store offline, chỉ ghi kết quả delivered/failed.

## 2. Luồng Xử Lý Message Đến

`MessageRouterImpl` route theo bảng handler:

| Type | Xử lý |
| --- | --- |
| `HEARTBEAT` | `InboundMessageImpl.markPeerOnline`, trả ACK. |
| `PEER_LIST_REQUEST` | Mark sender online, trả `PEER_LIST_RESPONSE`. |
| `PEER_LIST_RESPONSE` | Merge peer list vào directory, trả ACK. |
| `GROUP_MEMBERS_SYNC` | Sync group members local, trả ACK. |
| `CHAT` | Lưu direct history, notify UI, trả ACK. |
| `GROUP_CHAT` | Lưu group history, tạo/sync group local nếu cần, trả ACK. |
| `BROADCAST` | Lưu vào history `[Thế giới]`, notify UI, trả ACK. |
| `JOIN` | Mark sender online, trả ACK. |
| Loại khác | Trả ACK mặc định. |

## 3. Các Luồng P2P Chính

### 3.1. Chat 1-1 online

```mermaid
sequenceDiagram
    participant A as Alice
    participant B as Bob
    A->>B: Message(CHAT, id=m1)
    B->>B: save direct history
    B-->>A: Message(ACK, id=m1)
    A->>A: save status SENT
```

### 3.2. Chat 1-1 offline

```mermaid
sequenceDiagram
    participant A as Alice
    participant B as Bob offline
    participant BS as Bootstrap
    A-xB: CHAT timeout/retry hết
    A->>BS: STORE_OFFLINE offlineMessage(m1, receiverId=bob)
    BS-->>A: OK
    A->>A: save status PENDING
```

### 3.3. Nhận offline message

```mermaid
sequenceDiagram
    participant B as Bob
    participant BS as Bootstrap
    B->>BS: JOIN PeerInfo(bob)
    BS->>BS: drain offline messages for bob
    BS-->>B: JoinResponse(onlinePeers, offlineMessages)
    B->>B: save messages.json + notify UI
```

### 3.4. Group chat

```mermaid
sequenceDiagram
    participant A as Alice
    participant B as Bob
    participant C as Carol
    participant BS as Bootstrap
    A->>B: GROUP_CHAT groupId=g1
    B-->>A: ACK
    A-xC: GROUP_CHAT timeout/retry hết
    A->>BS: STORE_OFFLINE receiverId=carol, groupId=g1
```

### 3.5. Broadcast `[Thế giới]`

```mermaid
sequenceDiagram
    participant A as Alice
    participant B as Bob online
    participant C as Carol online
    A->>B: BROADCAST
    B-->>A: ACK
    A->>C: BROADCAST
    C-->>A: ACK
    A->>A: save __broadcast__
```

Broadcast không lưu offline. Peer offline tại thời điểm gửi sẽ bỏ lỡ tin đó.

## 4. Bootstrap Protocol

### 4.1. Định dạng

Mỗi request tới bootstrap là một dòng text:

```text
COMMAND [JSON_PAYLOAD]\n
```

Response là một dòng text:

- `"OK"` nếu command cập nhật thành công.
- JSON nếu command trả dữ liệu.
- `"UNKNOWN_COMMAND"` nếu command không hỗ trợ.
- `null` ở phía client nếu không kết nối được.

### 4.2. Command peer discovery

| Command | Payload | Response | Ý nghĩa |
| --- | --- | --- | --- |
| `REGISTER` | `PeerInfo` JSON | `OK` | Lưu/cập nhật user, chưa bắt buộc online. |
| `JOIN` | `PeerInfo` JSON | `JoinResponse` JSON | Đánh dấu online, trả online peers + offline messages. |
| `LEAVE` | `addressKey` text | `OK` | Xóa peer khỏi online registry. |
| `LIST` | Rỗng | `PeerInfo[]` JSON | Lấy danh sách peer online. |

`JoinResponse`:

```json
{
  "onlinePeers": [
    {
      "id": "bob",
      "name": "Bob",
      "host": "127.0.0.1",
      "port": 5002,
      "online": true
    }
  ],
  "offlineMessages": []
}
```

### 4.3. Command offline message

| Command | Payload | Response | Ý nghĩa |
| --- | --- | --- | --- |
| `STORE_OFFLINE` | `OfflineMessage` JSON | `OK` | Lưu tin chờ giao theo `receiverId`. |

Ví dụ:

```text
STORE_OFFLINE {"messageId":"m1","senderId":"alice","receiverId":"bob","groupId":null,"content":"offline hello","createdAt":1716200000000,"delivered":false}
```

Bootstrap trả offline message trong lần `JOIN` tiếp theo của receiver và đánh dấu delivered.

### 4.4. Command group metadata

| Command | Payload | Response | Ý nghĩa |
| --- | --- | --- | --- |
| `CREATE_GROUP` | `GroupPayload` JSON | `OK` | Tạo/cập nhật group metadata. |
| `ADD_GROUP_MEMBER` | `GroupMemberPayload` JSON | `OK` | Thêm user vào group. |
| `LIST_GROUPS` | Rỗng | `GroupPayload[]` JSON | Lấy danh sách group metadata. |
| `LIST_GROUP_MEMBERS` | `groupId` text | `GroupMemberPayload[]` JSON | Lấy member của group. |
| `REMOVE_GROUP_MEMBER` | `GroupMemberPayload` JSON | `OK` | Có support ở bootstrap, UI hiện tại chưa phải luồng chính. |

## 5. Tính Đúng Đắn Của Giao Thức

| Vấn đề | Cách xử lý |
| --- | --- |
| Tránh nhầm ACK | ACK phải giữ nguyên `message.id`. |
| Peer tắt đột ngột | Socket timeout, retry, fallback offline. |
| Nhiều kết nối đồng thời | Server dùng thread pool. |
| Group chỉ lưu id nhưng gửi cần IP:port | Runtime resolve id sang `PeerInfo` từ bootstrap/danh bạ trước khi gửi. |
| Broadcast lẫn chat riêng | `LocalMessageRepo` lọc `BROADCAST` ra khỏi direct conversation và lưu riêng `__broadcast__`. |
| Bootstrap lỗi | Peer vẫn chạy TCP trực tiếp với peer đã biết; không có discovery tự động/offline store mới. |

