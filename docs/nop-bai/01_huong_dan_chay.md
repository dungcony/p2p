# Hướng dẫn chạy chương trình

## 1. Mục đích tài liệu

Tài liệu này hướng dẫn cách build, chạy thử và kiểm tra hệ thống **P2P Chat System**. Hệ thống gồm hai module:

- `bootstrap-server`: tracker TCP hỗ trợ peer discovery, quản lý online/offline, group metadata và offline message.
- `peer-node`: ứng dụng desktop Swing đại diện cho một peer. Mỗi peer vừa là client gửi tin, vừa là server TCP nhận tin trực tiếp từ peer khác.

## 2. Yêu cầu môi trường

| Thành phần | Phiên bản khuyến nghị | Ghi chú |
| --- | --- | --- |
| JDK | Java 21 | Cần cấu hình `JAVA_HOME` trỏ tới JDK 21. |
| Maven | 3.9+ | Dùng để build multi-module project. |
| Hệ điều hành | Windows | Project có sẵn script `.bat`; vẫn có thể chạy bằng Maven trên hệ điều hành khác. |
| Docker | Tùy chọn | Dùng nếu muốn chạy bootstrap server bằng container. |

Kiểm tra môi trường:

```bat
java -version
mvn -version
```

## 3. Cấu trúc mã nguồn

```text
p2p/
  pom.xml
  run.bat
  run-bootstrap.bat
  docker-compose.yml
  docs/
  bootstrap-server/
    pom.xml
    src/main/java/dungcony/ds/
    src/main/resources/config.properties
  peer-node/
    pom.xml
    src/main/java/dungcony/ds/
    src/main/resources/data/
```

Ý nghĩa các file chạy nhanh:

| File | Công dụng |
| --- | --- |
| `run-bootstrap.bat` | Chạy tracker server trên port mặc định `9000`. |
| `run.bat` | Build dependency cần thiết và chạy một instance `peer-node`. |
| `docker-compose.yml` | Chạy `bootstrap-server` bằng Docker Compose. |

## 4. Chạy nhanh trên Windows

Mở terminal tại thư mục gốc project `p2p/`.

### Bước 1: chạy bootstrap server

```bat
run-bootstrap.bat
```

Mặc định bootstrap server lắng nghe tại:

```text
127.0.0.1:9000
```

Bootstrap server dùng giao thức TCP text command và SQLite để lưu user, group metadata, group member và offline message.

### Bước 2: chạy peer thứ nhất

Mở terminal thứ hai:

```bat
run.bat --peer-port=5001 --data-dir=tmp/alice
```

### Bước 3: chạy peer thứ hai

Mở terminal thứ ba:

```bat
run.bat --peer-port=5002 --data-dir=tmp/bob
```

### Bước 4: chạy peer thứ ba, nếu muốn thử group chat/broadcast

```bat
run.bat --peer-port=5003 --data-dir=tmp/carol
```

Khi chạy lần đầu, ứng dụng hiển thị giao diện Swing để chọn hoặc tạo profile peer. Mỗi peer cần dùng một port TCP khác nhau.

## 5. Tham số runtime của peer

| Tham số | Ví dụ | Ý nghĩa |
| --- | --- | --- |
| `--peer-port` | `--peer-port=5001` | Port TCP mà peer dùng để lắng nghe tin nhắn đến. |
| `--port` | `--port=5001` | Alias của `--peer-port`. |
| `--data-dir` | `--data-dir=tmp/alice` | Thư mục lưu profile, `messages.json`, `groups.json`. |

Nếu không truyền `--peer-port`, ứng dụng dùng port đã lưu trong profile hoặc hỏi port khi tạo profile mới.

## 6. Cấu hình bootstrap server

File cấu hình mặc định:

```text
bootstrap-server/src/main/resources/config.properties
```

Nội dung chính:

```properties
server.port=9000
database.path=bootstrap-server/src/main/resources/database/bootstrap-server.db
```

Thứ tự ưu tiên khi đọc cấu hình:

1. JVM system property, ví dụ `-Dserver.port=9100`.
2. Environment variable, ví dụ `BOOTSTRAP_SERVER_PORT`.
3. File `config.properties`.
4. Giá trị fallback trong code.

## 7. Chạy bằng Maven

Compile toàn bộ project:

```bat
mvn -DskipTests compile
```

Chạy test:

```bat
mvn test
```

Chạy bootstrap server trực tiếp bằng Maven:

```bat
mvn -pl bootstrap-server exec:java -Dexec.mainClass="dungcony.ds.App" -Dexec.args="9000"
```

Chạy một peer trực tiếp bằng Maven:

```bat
mvn -pl peer-node exec:java -Dexec.mainClass="dungcony.ds.App" -Dexec.args="--peer-port=5001 --data-dir=tmp/alice"
```

Build riêng bootstrap server:

```bat
mvn -pl bootstrap-server -am -DskipTests package
```

Sau khi package, có thể chạy jar:

```bat
java -jar bootstrap-server/target/bootstrap-server-1.0-SNAPSHOT.jar
```

## 8. Chạy bootstrap bằng Docker

Build image:

```bat
docker build -f bootstrap-server/Dockerfile -t p2p-bootstrap-server .
```

Run container:

```bat
docker run --rm -p 9000:9000 -v p2p-bootstrap-data:/data p2p-bootstrap-server
```

Hoặc dùng Docker Compose:

```bat
docker compose up --build bootstrap-server
```

## 9. Kịch bản demo đề xuất

### 9.1. Chat trực tiếp 1-1

1. Chạy bootstrap server.
2. Chạy Alice ở port `5001`.
3. Chạy Bob ở port `5002`.
4. Đợi danh sách peer của Alice hiển thị Bob online.
5. Alice gửi tin nhắn tới Bob.
6. Bob nhận tin gần như tức thời; Alice lưu trạng thái `SENT`.

### 9.2. Offline message

1. Chạy Alice và Bob cùng bootstrap.
2. Tắt Bob.
3. Alice gửi tin nhắn tới Bob.
4. Gửi trực tiếp thất bại sau retry; Alice lưu tin ở trạng thái `PENDING` vì bootstrap đã lưu offline message.
5. Chạy lại Bob cùng `peerId`.
6. Khi Bob `JOIN`, bootstrap trả offline message cho Bob.

### 9.3. Group chat

1. Chạy Alice, Bob, Carol.
2. Alice tạo group gồm Alice, Bob, Carol.
3. Alice gửi tin nhắn nhóm.
4. Tin nhóm được gửi tới từng member bằng TCP trực tiếp.
5. Nếu member offline, tin được lưu fallback qua bootstrap theo `receiverId`.

### 9.4. Broadcast toàn mạng

1. Chạy ít nhất ba peer.
2. Từ một peer, chọn chức năng broadcast.
3. Peer gửi message `BROADCAST` tới toàn bộ peer online mà nó biết.

## 10. Dữ liệu sinh ra khi chạy

Mỗi peer có thư mục dữ liệu riêng:

```text
tmp/alice/
  config.properties
  messages.json
  groups.json
```

Ý nghĩa:

| File | Nội dung |
| --- | --- |
| `config.properties` | `peer.id`, `peer.name`, `peer.port` của profile. |
| `messages.json` | Lịch sử chat 1-1, group, broadcast và trạng thái gửi. |
| `groups.json` | Danh sách group mà peer đang tham gia. |

Bootstrap server dùng SQLite để lưu:

- User/profile đã đăng ký.
- Group metadata.
- Group member.
- Offline message chờ giao.

## 11. Lỗi thường gặp và cách xử lý

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
| --- | --- | --- |
| Peer không mở được port | Port đang bị process khác dùng | Đổi `--peer-port` hoặc tắt process đang chiếm port. |
| Peer không thấy peer khác | Bootstrap chưa chạy hoặc chưa refresh | Kiểm tra `run-bootstrap.bat`, đợi chu kỳ refresh khoảng 5 giây. |
| Gửi tin bị `FAILED` | Peer đích offline và bootstrap không khả dụng | Chạy lại bootstrap hoặc gửi lại khi peer online. |
| Gửi tin bị `PENDING` | Gửi trực tiếp thất bại nhưng đã lưu offline | Chạy lại peer nhận để bootstrap giao lại tin. |
| Không đồng bộ group | Peer mới chưa join bootstrap hoặc bootstrap mất kết nối | Kiểm tra bootstrap, đợi refresh hoặc tạo lại group. |

## 12. Gợi ý đóng gói mã nguồn khi nộp

Nên nộp toàn bộ thư mục `p2p/`, nhưng có thể bỏ các thư mục build tạm:

```text
target/
tmp/
.idea/
.vscode/
```

Không nên xóa:

- `pom.xml`
- `run.bat`
- `run-bootstrap.bat`
- `bootstrap-server/`
- `peer-node/`
- `docs/`
