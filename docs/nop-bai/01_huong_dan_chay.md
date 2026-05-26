# Hướng Dẫn Chạy Hệ Thống

Tài liệu này hướng dẫn build và chạy hệ thống P2P Chat theo luồng hiện tại.

## 1. Yêu Cầu Môi Trường

| Thành phần | Yêu cầu |
| --- | --- |
| JDK | Java 21 |
| Build tool | Maven 3.9+ |
| Hệ điều hành kiểm thử | Windows |
| Port mặc định bootstrap | `9000` |
| Port peer mặc định | Bắt đầu từ `5001`, tự chọn port trống khi tạo profile mới |

Kiểm tra môi trường:

```bat
java -version
mvn -version
```

## 2. Build Project

Từ thư mục gốc project:

```bat
mvn clean test
```

Nếu chỉ muốn chuẩn bị dependency của `bootstrap-server` cho `peer-node`:

```bat
mvn -pl bootstrap-server -am -DskipTests install
```

`run-peer.bat` đã tự chạy bước này trước khi mở app peer.

## 3. Chạy Bootstrap Server

Mở terminal thứ nhất:

```bat
run-bootstrap.bat
```

Script này chạy:

```bat
mvn -pl bootstrap-server exec:java -Dexec.mainClass="dungcony.ds.App" -Dexec.args="9000"
```

Bootstrap server là tracker. Nó hỗ trợ:

- `REGISTER`, `JOIN`, `LEAVE`, `LIST` cho peer discovery và online/offline.
- `STORE_OFFLINE` cho tin nhắn offline.
- `CREATE_GROUP`, `ADD_GROUP_MEMBER`, `LIST_GROUPS`, `LIST_GROUP_MEMBERS` cho metadata nhóm.

Bootstrap không chuyển tiếp tin chat online giữa peer.

## 4. Chạy Một Peer

Mở terminal khác:

```bat
run.bat
```

`run.bat` chỉ là wrapper gọi `run-peer.bat`. Khi không có tham số, app mở UI chọn/tạo profile:

- Có profile người dùng thật: vào thẳng màn chat.
- Chỉ có profile demo `alice`, `bob`, `carol` hoặc chưa có profile: mở dialog nhập tên peer.
- `peer.id` là UUID nội bộ, người dùng không nhập.
- `peer.port` được tự chọn tránh trùng bootstrap và tránh port đang bận.

Chạy bằng profile demo:

```bat
run-peer.bat --profile=alice
run-peer.bat --profile=bob
run-peer.bat --profile=carol
```

Chạy hoặc tạo profile theo tên:

```bat
run-peer.bat --peer-name=Dung
```

Nếu đã có profile tên `Dung`, app dùng lại profile đó. Nếu chưa có, app tạo profile mới.

## 5. Chạy Demo Nhanh

Chạy bootstrap và ba peer demo trong các cửa sổ riêng:

```bat
run-all-peers.bat
```

Demo tạo:

| Peer | Profile | Port thường dùng |
| --- | --- | --- |
| Alice | `alice` | `5001` |
| Bob | `bob` | `5002` |
| Carol | `carol` | `5003` |

Các port có thể thay đổi nếu port đang bị chiếm hoặc nếu truyền `--peer-port`.

## 6. Tham Số CLI Của Peer

| Tham số | Ví dụ | Ý nghĩa |
| --- | --- | --- |
| `--profile` | `--profile=alice` | Nạp profile theo folder/id trong data root. |
| `--peer-name` | `--peer-name=Dung` | Tìm profile theo tên hoặc tạo mới. |
| `--peer-port` / `--port` | `--peer-port=5010` | Override port lắng nghe của peer. |
| `--data-dir` | `--data-dir=tmp/demo-data` | Thư mục gốc chứa profiles và dữ liệu local. |

Ví dụ chạy hai peer riêng trong data root tạm:

```bat
run-peer.bat --data-dir=tmp/demo --peer-name=Alice --peer-port=5101
run-peer.bat --data-dir=tmp/demo --peer-name=Bob --peer-port=5102
```

## 7. Dữ Liệu Local

Mặc định:

```text
peer-node/src/main/resources/data/
```

Cấu trúc:

```text
data/
├── config.properties          # bootstrap.host, bootstrap.port dùng chung
├── alice/
│   ├── config.properties      # peer.id, peer.name, peer.port
│   ├── messages.json
│   └── groups.json
└── <uuid-profile>/
    ├── config.properties
    ├── messages.json
    └── groups.json
```

| File | Vai trò |
| --- | --- |
| `config.properties` ở data root | Cấu hình bootstrap dùng chung. |
| `profile/config.properties` | Định danh peer và port lắng nghe. |
| `messages.json` | Lịch sử direct, group, broadcast và trạng thái gửi. |
| `groups.json` | Group local mà peer đang biết. |

Bootstrap dùng SQLite theo cấu hình module `bootstrap-server` để lưu users, groups, group members và offline messages.

## 8. Kịch Bản Kiểm Thử Thủ Công

### 8.1. Chat 1-1

1. Chạy `run-bootstrap.bat`.
2. Mở Alice và Bob.
3. Đợi hai peer thấy nhau online.
4. Alice chọn Bob, gửi tin.
5. Bob nhận tin, Alice thấy tin ở trạng thái `Đã gửi`.

### 8.2. Peer Offline Và Store-And-Forward

1. Chạy Alice và Bob cùng bootstrap.
2. Đóng Bob.
3. Alice gửi tin cho Bob.
4. Nếu bootstrap còn chạy, tin của Alice chuyển `PENDING`.
5. Mở lại Bob cùng `peer.id`; Bob nhận offline message khi `JOIN`.

### 8.3. Chat Nhóm

1. Chạy ít nhất ba peer.
2. Alice tạo nhóm với Bob và Carol.
3. Alice gửi tin trong nhóm.
4. Bob và Carol nhận `GROUP_CHAT` trực tiếp.
5. Nếu một member offline, tin nhóm có thể được lưu offline qua bootstrap theo `receiverId`.

### 8.4. Broadcast `[Thế giới]`

1. Chạy ít nhất ba peer.
2. Alice chọn conversation `[Thế giới]`.
3. Alice gửi tin như chat thường.
4. Các peer online nhận `BROADCAST`.
5. Peer offline không nhận lại broadcast khi online sau đó.

## 9. Test Tự Động

Chạy toàn bộ test:

```bat
mvn test
```

Chạy test peer-node:

```bat
mvn -pl peer-node test
```

Chạy test bootstrap:

```bat
mvn -pl bootstrap-server test
```

Các test quan trọng:

| Test | Ý nghĩa |
| --- | --- |
| `directMessageIsDeliveredWithAckThroughDiscoveredPeer` | Chat 1-1 qua discovery và ACK. |
| `failedDirectSendIsStoredOfflineAndDeliveredWhenReceiverJoinsAgain` | Store-and-forward khi receiver offline. |
| `groupMessageIsBroadcastToAllOnlineMembers` | Group chat tới các member online. |
| `networkBroadcastIsDeliveredToAllOnlinePeers` | Broadcast `[Thế giới]` tới peer online. |
| `broadcastHistoryIsSeparateFromDirectConversations` | Broadcast không lẫn vào chat riêng. |
| `ignoresDemoProfilesAndCreatesEditableUserProfile` | Không dùng demo profile làm profile người dùng thật. |

## 10. Lỗi Thường Gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
| --- | --- | --- |
| Peer không thấy peer khác | Bootstrap chưa chạy hoặc chưa refresh | Chạy `run-bootstrap.bat`, đợi khoảng 5 giây. |
| Không mở được port peer | Port trong profile đang bị process khác dùng | Dùng `--peer-port` khác hoặc tạo profile mới. |
| Tin chuyển `FAILED` | Peer đích offline và bootstrap không lưu được offline | Chạy lại bootstrap/peer nhận rồi thử lại. |
| Tin chuyển `PENDING` | Gửi trực tiếp thất bại nhưng bootstrap đã lưu offline | Mở lại receiver cùng `peer.id`. |
| Broadcast không tới peer offline | Thiết kế broadcast chỉ gửi realtime | Đây là hành vi đúng của `[Thế giới]`. |

