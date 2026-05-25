# Xử lý lỗi và thử nghiệm hệ thống

## 1. Mục tiêu

Phần này trình bày cách hệ thống xử lý lỗi trong môi trường phân tán và các thử nghiệm đã/đề xuất thực hiện để đánh giá hoạt động của P2P Chat System.

Các nhóm lỗi chính:

- Lỗi kết nối TCP giữa peer.
- Peer nhận offline hoặc tắt đột ngột.
- Bootstrap server không khả dụng.
- Message không nhận được ACK.
- Dữ liệu local JSON hoặc SQLite gặp lỗi đọc/ghi.
- Lỗi nhập liệu từ người dùng.
- Lỗi đồng thời khi có nhiều kết nối cùng lúc.

## 2. Cơ chế xử lý lỗi mạng

### 2.1. Timeout khi kết nối peer-to-peer

`TCPClient` đặt timeout cho mỗi lần gửi:

| Loại timeout | Giá trị | Ý nghĩa |
| --- | --- | --- |
| Connect timeout | `2000 ms` | Không treo vô hạn khi peer đích không mở port hoặc mất mạng. |
| Read timeout | `3000 ms` | Không treo vô hạn khi peer nhận không trả response. |

Nếu có `IOException`, `TCPClient` trả `null` để tầng trên xử lý retry hoặc fallback.

### 2.2. Retry và ACK

`MessageSender` retry tối đa 3 lần. Một lần gửi được xem là thành công khi:

- Có response.
- Response có `type = ACK`.
- Response có `id` trùng với message gốc.

Nếu response sai type, sai id hoặc timeout, sender thử lại sau `300 ms`.

### 2.3. Trạng thái message

| Trạng thái | Khi nào xảy ra | Ý nghĩa với người dùng |
| --- | --- | --- |
| `SENDING` | Đang retry thủ công message cũ. | Tin đang được gửi lại. |
| `SENT` | Đã nhận ACK hợp lệ. | Tin đã được peer đích nhận ở tầng ứng dụng. |
| `PENDING` | Gửi trực tiếp thất bại nhưng bootstrap lưu offline thành công. | Tin sẽ được giao khi receiver join lại. |
| `FAILED` | Gửi trực tiếp thất bại và không lưu offline được. | Người dùng cần thử lại sau. |

## 3. Xử lý peer offline

### 3.1. Offline có chủ động

Khi peer stop bình thường:

1. `PeerNode.stop()` được gọi.
2. Peer gửi `LEAVE addressKey` lên bootstrap.
3. Bootstrap xóa peer khỏi `PeerRegistry`.
4. Peer dừng `TCPServer`.

### 3.2. Offline đột ngột

Nếu peer bị tắt đột ngột và không kịp gửi `LEAVE`:

1. Bootstrap không nhận `JOIN` refresh từ peer đó nữa.
2. `PeerRegistry` dùng TTL `15000 ms`.
3. Khi `LIST` hoặc `JOIN` tiếp theo chạy, `evictExpiredPeers()` loại peer quá hạn.

### 3.3. Gửi tin tới peer offline

Luồng xử lý:

1. Sender gửi trực tiếp qua TCP.
2. Nếu connect/read timeout hoặc không nhận ACK, retry tối đa 3 lần.
3. Nếu vẫn thất bại, sender gọi `STORE_OFFLINE` lên bootstrap.
4. Nếu bootstrap trả `OK`, message local lưu trạng thái `PENDING`.
5. Khi receiver `JOIN` lại, bootstrap trả offline message trong `JoinResponse`.
6. Receiver lưu tin vào `messages.json` và notify UI.

Nếu bootstrap cũng không khả dụng, message chuyển sang `FAILED`.

## 4. Xử lý bootstrap server không khả dụng

Bootstrap không phải server chat trung tâm, nên peer vẫn có thể hoạt động một phần khi bootstrap lỗi.

| Chức năng | Khi bootstrap lỗi |
| --- | --- |
| Khởi động peer | Vẫn chạy được. |
| Chat trực tiếp tới peer đã biết `host:port` | Vẫn dùng được nếu peer đích reachable. |
| Discovery tự động | Không dùng được. |
| Fallback discovery qua peer đã biết | Vẫn dùng được bằng `PEER_LIST_REQUEST`. |
| Offline message | Không dùng được. |
| Group metadata sync qua tracker | Không dùng được. |
| Group chat tới member đã biết | Vẫn gửi trực tiếp được nếu member reachable. |

`BootstrapSyncImpl` xử lý trường hợp `REGISTER` hoặc `JOIN` thất bại bằng cách log warning và giữ peer chạy ở chế độ TCP trực tiếp.

## 5. Xử lý lỗi dữ liệu local

### 5.1. Message history

`LocalMessageRepo`:

- Tạo thư mục và file `messages.json` nếu chưa tồn tại.
- Dùng `synchronized` khi đọc/ghi.
- Nếu đọc JSON lỗi, trả danh sách rỗng và ghi log.
- Khi lưu message cùng `messageId`, cập nhật record cũ thay vì thêm bản trùng.

### 5.2. Group local

`LocalGroupRepo`:

- Tạo `groups.json` nếu chưa tồn tại.
- Dùng `synchronized` khi đọc/ghi.
- Nếu đọc lỗi, trả danh sách rỗng để ứng dụng không crash.
- Khi ghi group, sort theo tên và `groupId` để file ổn định hơn.

### 5.3. SQLite bootstrap

Bootstrap dùng `Init` để khởi tạo schema khi server start. Các repository tách riêng cho user, group, member và offline message để giảm rủi ro lẫn logic.

## 6. Xử lý lỗi nhập liệu

Các service kiểm tra input trước khi gửi:

| Trường hợp | Cách xử lý |
| --- | --- |
| Nội dung chat rỗng | `ChatImpl` từ chối gửi và trả `false`. |
| Broadcast rỗng | `NetworkBroadcastImpl` trả `BroadcastResult(0, 0, 0)`. |
| Địa chỉ peer rỗng | `PeerDirectoryImpl.resolvePeer()` trả `null`. |
| Gửi tới chính mình | `ChatImpl` và `PeerPresenceImpl` từ chối. |
| Port nhập sai trong `host:port` | Parser fallback về port local và log warning. |
| Group không tồn tại | `GroupChatImpl` bỏ qua gửi và log warning. |

## 7. Xử lý đồng thời

| Vấn đề | Cơ chế |
| --- | --- |
| Nhiều peer kết nối đồng thời tới một peer | `TCPServer` dùng `ExecutorService.newCachedThreadPool()`. |
| Nhiều peer gửi command tới bootstrap | `BootstrapServer` dùng thread pool riêng. |
| Cập nhật danh bạ peer runtime | `ConcurrentHashMap` trong `PeerDirectoryImpl`. |
| Cập nhật registry online bootstrap | `ConcurrentHashMap` trong `PeerRegistry`. |
| Nhiều listener UI | `CopyOnWriteArrayList` trong `PeerNode`. |
| Ghi file JSON local | Method repository dùng `synchronized`. |
| Sync membership group không làm đứng UI | `GroupChatImpl` dùng `CompletableFuture.runAsync()`. |

## 8. Kiểm thử tự động

Project có test bằng JUnit 5. Chạy toàn bộ:

```bat
mvn test
```

Kết quả kiểm tra gần nhất trên môi trường hiện tại:

| Thời điểm | Lệnh | Kết quả |
| --- | --- | --- |
| 25/05/2026 16:32:24 GMT+7 | `mvn test` | `BUILD SUCCESS`, 9 tests, 0 failures, 0 errors, 0 skipped. |

### 8.1. Test bootstrap server

File:

```text
bootstrap-server/src/test/java/dungcony/ds/models/BootstrapServerTest.java
```

| Test | Mục tiêu |
| --- | --- |
| `joinListAndLeaveTrackOnlinePeers` | Kiểm tra `JOIN`, `LIST`, `LEAVE` cập nhật peer online đúng. |
| `offlineMessagesAreDrainedOnFirstJoinOnly` | Kiểm tra offline message chỉ được trả ở lần `JOIN` đầu tiên rồi đánh dấu delivered. |

### 8.2. Test peer node integration

File:

```text
peer-node/src/test/java/dungcony/ds/peer/PeerNodeIntegrationTest.java
```

| Test | Mục tiêu |
| --- | --- |
| `directMessageIsDeliveredWithAckThroughDiscoveredPeer` | Alice discover Bob qua bootstrap, gửi chat trực tiếp và nhận ACK. |
| `failedDirectSendIsStoredOfflineAndDeliveredWhenReceiverJoinsAgain` | Bob offline, Alice gửi tin, bootstrap lưu offline, Bob join lại và nhận tin. |
| `groupMessageIsBroadcastToAllOnlineMembers` | Alice tạo group và gửi message tới Bob, Carol. |
| `networkBroadcastIsDeliveredToAllOnlinePeers` | Broadcast tới toàn bộ peer online và kiểm tra kết quả delivered/failed. |

### 8.3. Test message history

File:

```text
peer-node/src/test/java/dungcony/ds/services/MessageHistoryImplTest.java
```

| Test | Mục tiêu |
| --- | --- |
| `updatingSameMessageIdDoesNotDuplicateLocalHistory` | Khi cập nhật trạng thái cùng `messageId`, local history không bị nhân đôi record. |

### 8.4. Test runtime option

File:

```text
peer-node/src/test/java/dungcony/ds/AppRuntimeOptionsTest.java
```

| Test | Mục tiêu |
| --- | --- |
| `resolvesInlineRuntimeOptions` | Parse `--data-dir=...` và `--peer-port=...`. |
| `resolvesSeparatedRuntimeOptions` | Parse dạng `--data-dir value --port value`. |
| `keepsPreviousValuesWhenLaterValuesAreInvalid` | Bỏ qua giá trị invalid và giữ giá trị hợp lệ trước đó. |
| `missingDataDirValueDoesNotConsumeNextOption` | Không ăn nhầm option tiếp theo làm value của `--data-dir`. |

## 9. Kịch bản thử nghiệm thủ công

### 9.1. Chat trực tiếp 1-1

| Bước | Thao tác | Kết quả kỳ vọng |
| --- | --- | --- |
| 1 | Chạy bootstrap server | Server lắng nghe port `9000`. |
| 2 | Chạy Alice port `5001` | Alice join bootstrap. |
| 3 | Chạy Bob port `5002` | Bob join bootstrap. |
| 4 | Alice gửi tin cho Bob | Bob nhận tin, Alice thấy trạng thái gửi thành công. |
| 5 | Kiểm tra `messages.json` | Có record message của Alice/Bob. |

### 9.2. Peer offline và store-and-forward

| Bước | Thao tác | Kết quả kỳ vọng |
| --- | --- | --- |
| 1 | Chạy Alice, Bob và bootstrap | Cả hai online. |
| 2 | Tắt Bob | Bob không còn reachable qua TCP. |
| 3 | Alice gửi tin cho Bob | Gửi trực tiếp retry rồi thất bại. |
| 4 | Bootstrap còn chạy | Message được lưu offline, Alice lưu `PENDING`. |
| 5 | Chạy lại Bob cùng `peerId` | Bob nhận offline message khi `JOIN`. |

### 9.3. Bootstrap server tắt

| Bước | Thao tác | Kết quả kỳ vọng |
| --- | --- | --- |
| 1 | Tắt bootstrap | Peer không refresh được tracker. |
| 2 | Chạy Alice và Bob bằng địa chỉ thủ công | Peer vẫn có thể chat trực tiếp nếu biết `host:port`. |
| 3 | Gửi tin tới peer offline | Message chuyển `FAILED` vì không store offline được. |

### 9.4. Group chat

| Bước | Thao tác | Kết quả kỳ vọng |
| --- | --- | --- |
| 1 | Chạy Alice, Bob, Carol | Cả ba online. |
| 2 | Alice tạo group gồm Bob và Carol | Group xuất hiện trong UI, metadata lưu local và bootstrap. |
| 3 | Alice gửi tin nhóm | Bob và Carol nhận `GROUP_CHAT`. |
| 4 | Tắt Carol rồi gửi tiếp | Bob nhận trực tiếp, Carol nhận offline khi join lại nếu bootstrap còn chạy. |

### 9.5. Broadcast toàn mạng

| Bước | Thao tác | Kết quả kỳ vọng |
| --- | --- | --- |
| 1 | Chạy ít nhất ba peer | Bootstrap trả danh sách online. |
| 2 | Alice broadcast | Bob và Carol nhận `BROADCAST`. |
| 3 | Tắt một peer và broadcast lại | `BroadcastResult.failed` tăng tương ứng. |

## 10. Ma trận đánh giá yêu cầu

| Yêu cầu đồ án | Cách kiểm chứng | Kết quả kỳ vọng |
| --- | --- | --- |
| Mỗi peer gửi và nhận đồng thời | Chạy hai peer, gửi tin qua lại cùng lúc | Không bị block UI; peer nhận được tin. |
| Giao tiếp bằng TCP socket | Kiểm tra `TCPClient`, `TCPServer`, test integration | Message đi qua socket TCP. |
| Xử lý nhiều kết nối | Chạy nhiều peer gửi tới một peer | Server accept bằng thread pool. |
| Peer discovery | Chạy bootstrap, peer join | Peer mới thấy peer online. |
| Chat trực tiếp | Test `directMessageIsDeliveredWithAckThroughDiscoveredPeer` | Message có ACK và `SENT`. |
| Chat nhóm | Test `groupMessageIsBroadcastToAllOnlineMembers` | Tất cả member online nhận tin. |
| Broadcast | Test `networkBroadcastIsDeliveredToAllOnlinePeers` | Delivered bằng số peer online. |
| Store-and-forward | Test offline message | Receiver nhận lại khi join. |
| Xử lý lỗi | Tắt peer/bootstrap trong lúc gửi | Message chuyển `PENDING` hoặc `FAILED`, app không crash. |

## 11. Kết luận thử nghiệm

Các test tự động hiện có bao phủ những luồng quan trọng nhất của hệ thống:

- Bootstrap quản lý online/offline và offline message.
- Peer discover nhau qua bootstrap.
- Chat 1-1 có ACK.
- Store-and-forward khi receiver offline.
- Group chat tới nhiều member.
- Broadcast toàn mạng.
- Lưu history không bị trùng khi retry/cập nhật trạng thái.

Hệ thống đáp ứng yêu cầu đồ án ở mức chức năng cốt lõi. Các hạn chế còn lại chủ yếu thuộc nhóm bảo mật, NAT traversal, tối ưu hiệu năng và khả năng chịu lỗi của bootstrap server.

## 12. Hướng cải tiến kiểm thử

- Thêm test bootstrap TTL để kiểm tra peer bị loại sau `ONLINE_TTL_MS`.
- Thêm test bootstrap bị tắt giữa lúc peer đang refresh.
- Thêm test JSON local bị hỏng để xác nhận app không crash.
- Thêm test stress nhiều peer gửi đồng thời.
- Thêm test group member offline nhận lại group message.
- Thêm test giao diện bằng công cụ UI automation nếu cần nghiệm thu UI.
