# Hướng Dẫn Chạy Hệ Thống

Tài liệu này mô tả cách build, chạy và kiểm thử thủ công hệ thống P2P Chat.

## 1. Yêu Cầu Môi Trường

| Thành phần | Yêu cầu |
| --- | --- |
| JDK | Java 21 |
| Maven | 3.9+ |
| Hệ điều hành đã kiểm thử | Windows |
| Port bootstrap mặc định | `9000` |
| Port peer demo | `5001`, `5002`, `5003` |

Kiểm tra:

```bat
java -version
mvn -version
```

## 2. Build Và Test

Từ thư mục gốc project:

```bat
mvn test
```

Nếu chỉ muốn compile/package nhanh:

```bat
mvn -DskipTests package
```

Chạy test từng module:

```bat
mvn -pl bootstrap-server test
mvn -pl peer-node test
```

`peer-node` phụ thuộc một số class model/DTO của `bootstrap-server`, vì vậy `run-peer.bat` tự chuẩn bị artifact bằng:

```bat
mvn -pl bootstrap-server -am -DskipTests install
```

Bước này chỉ phục vụ Maven dependency resolution, không khởi động tracker.

## 3. Chạy Bootstrap Server

Mở terminal thứ nhất:

```bat
run-bootstrap.bat
```

Script chạy module `bootstrap-server` ở port `9000`.

Bootstrap server hỗ trợ:

| Nhóm | Command chính |
| --- | --- |
| Peer lifecycle | `REGISTER`, `JOIN`, `LEAVE`, `LIST` |
| Offline message | `STORE_OFFLINE` |
| Group metadata | `CREATE_GROUP`, `ADD_GROUP_MEMBER`, `LIST_GROUPS`, `LIST_GROUP_MEMBERS` |

Bootstrap không relay tin online. Khi hai peer đều online, tin chat được gửi trực tiếp qua TCP giữa peer với peer.

## 4. Chạy Một Peer

Mở terminal khác:

```bat
run-peer.bat
```

Khi không truyền tham số:

- Nếu đã có profile người dùng thật, app vào thẳng màn chat.
- Nếu chưa có profile thật, app mở dialog nhập tên hiển thị.
- `peer.id` là UUID nội bộ, người dùng không cần nhập.
- `peer.port` được tự chọn để tránh trùng bootstrap và tránh port đang bận.

Chạy hoặc tạo profile theo tên:

```bat
run-peer.bat --peer-name=Dung
```

Nếu đã có profile tên `Dung`, app dùng lại profile đó. Nếu chưa có, app tạo profile mới.

Chạy profile cụ thể:

```bat
run-peer.bat --profile=alice
```

Override port:

```bat
run-peer.bat --peer-name=Alice --peer-port=5001
```

Đổi data root tạm để demo sạch:

```bat
run-peer.bat --data-dir=tmp/demo --peer-name=Alice --peer-port=5101
run-peer.bat --data-dir=tmp/demo --peer-name=Bob --peer-port=5102
```

## 5. Chạy Demo Ba Peer

Chạy bootstrap và ba peer trong các cửa sổ riêng:

```bat
run-all-peers.bat
```

Script mở:

| Cửa sổ | Cách chọn profile |
| --- | --- |
| Bootstrap | `run-bootstrap.bat` |
| Alice | Dùng `--profile=alice` nếu có, nếu không dùng `--peer-name=Alice --peer-port=5001`. |
| Bob | Dùng `--profile=bob` nếu có, nếu không dùng `--peer-name=Bob --peer-port=5002`. |
| Carol | Dùng `--profile=carol` nếu có, nếu không dùng `--peer-name=Carol --peer-port=5003`. |

Cách này giữ được dữ liệu demo cũ nếu máy đã có `runtime-data/peer-node/alice|bob|carol`, đồng thời vẫn chạy được trên máy sạch.

## 6. Tham Số CLI Của Peer

| Tham số | Ví dụ | Ý nghĩa |
| --- | --- | --- |
| `--profile` | `--profile=alice` | Nạp profile theo folder/id trong data root. |
| `--peer-name` | `--peer-name=Dung` | Tìm profile theo tên hoặc tạo mới. |
| `--peer-port` / `--port` | `--peer-port=5010` | Override port lắng nghe của peer. |
| `--data-dir` | `--data-dir=tmp/demo-data` | Thư mục gốc chứa profiles và dữ liệu local. |

Nếu gọi Maven trực tiếp, phải đặt tham số của app trong `-Dexec.args`. Không truyền `--peer-port` trực tiếp cho Maven.

Đúng:

```bat
mvn -pl peer-node -DskipTests compile exec:java -Dexec.mainClass=dungcony.ds.App "-Dexec.args=--peer-name=Alice --peer-port=5001"
```

Sai:

```bat
mvn -pl peer-node exec:java -Dexec.mainClass=dungcony.ds.App --peer-port=5001
```

Lệnh sai làm Maven báo `Unrecognized option: --peer-port=5001` vì Maven hiểu nhầm đó là option của Maven.

## 7. Dữ Liệu Runtime

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

| File | Vai trò |
| --- | --- |
| `runtime-data/peer-node/config.properties` | Cấu hình bootstrap dùng chung cho peer-node. |
| `runtime-data/peer-node/<profile>/config.properties` | `peer.id`, `peer.name`, `peer.port`, public key, private key. |
| `runtime-data/peer-node/<profile>/messages.json` | Lịch sử direct, group, broadcast và trạng thái gửi. |
| `runtime-data/peer-node/<profile>/groups.json` | Group local cache của profile. |
| `runtime-data/bootstrap-server/bootstrap-server.db` | SQLite database của tracker. |

`runtime-data/` nằm trong `.gitignore` để không nộp kèm history chat, private key và database runtime. Code vẫn có nhánh migrate dữ liệu cũ nếu thư mục legacy `peer-node/src/main/resources/data` còn tồn tại trên máy cũ, nhưng repo sạch không cần dùng thư mục đó nữa.

## 8. Kịch Bản Kiểm Thử Thủ Công

### 8.1. Chat 1-1

1. Chạy `run-bootstrap.bat`.
2. Mở Alice và Bob.
3. Đợi hai peer thấy nhau online.
4. Alice chọn Bob và gửi tin.
5. Bob nhận tin, Alice lưu message ở trạng thái `SENT`.

### 8.2. Store-And-Forward Khi Peer Offline

1. Chạy Alice, Bob và bootstrap.
2. Đóng Bob.
3. Alice gửi tin cho Bob.
4. Nếu bootstrap còn chạy, tin của Alice chuyển `PENDING`.
5. Mở lại Bob cùng `peer.id`; Bob nhận offline message khi `JOIN`.

### 8.3. Chat Nhóm

1. Chạy ít nhất ba peer.
2. Alice tạo nhóm với Bob và Carol.
3. Alice gửi tin trong nhóm.
4. Bob và Carol nhận `GROUP_CHAT` trực tiếp nếu online.
5. Member offline có thể nhận lại tin nhóm qua bootstrap khi online lại.

### 8.4. Broadcast `[Thế giới]`

1. Chạy ít nhất ba peer.
2. Alice chọn conversation `[Thế giới]`.
3. Alice gửi tin.
4. Peer online nhận `BROADCAST`.
5. Peer offline không nhận lại broadcast sau khi online, vì broadcast là realtime.

### 8.5. Mã Hóa Payload

1. Chạy hai peer đã đăng ký với bootstrap.
2. Gửi direct/group/broadcast message.
3. Sender mã hóa content bằng public key của receiver.
4. Receiver giải mã bằng private key trong profile local rồi lưu vào history.
5. Offline message lưu trên bootstrap vẫn giữ metadata `encrypted`, `encryptionAlgorithm`, `encryptedFor`.

## 9. Lỗi Thường Gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
| --- | --- | --- |
| Maven báo `Unrecognized option: --peer-port` | Truyền arg peer trực tiếp cho Maven | Dùng `run-peer.bat` hoặc đặt trong `"-Dexec.args=..."`. |
| Peer không thấy peer khác | Bootstrap chưa chạy hoặc peer chưa refresh | Chạy `run-bootstrap.bat`, đợi vài giây. |
| Không mở được port peer | Port đang bị process khác dùng | Dùng `--peer-port` khác hoặc tạo profile mới. |
| Tin chuyển `FAILED` | Không ACK và không store offline được | Kiểm tra peer đích, bootstrap và public key receiver. |
| Tin chuyển `PENDING` | Gửi trực tiếp thất bại nhưng bootstrap đã lưu offline | Mở lại receiver cùng `peer.id`. |
| Broadcast không tới peer offline | Thiết kế broadcast chỉ realtime | Đây là hành vi đúng của `[Thế giới]`. |
