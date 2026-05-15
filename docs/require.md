# 📌 Đồ án: Phát triển hệ thống Chat ngang hàng P2P (Peer-to-Peer Chat System)

## 1. Mục tiêu

Xây dựng một hệ thống chat ngang hàng (P2P) cho phép nhiều người dùng trao đổi tin nhắn trực tiếp mà không phụ thuộc hoàn toàn vào server trung tâm.

### Áp dụng kiến thức:
- Mô hình Peer-to-Peer trong hệ thống phân tán
- Giao tiếp giữa các tiến trình qua mạng
- Peer Discovery (khám phá nút mạng)
- Đồng bộ và quản lý trạng thái phân tán
- Xử lý lỗi và tính chịu lỗi cơ bản

---

## 2. Mô tả hệ thống

Hệ thống gồm nhiều **peer**, mỗi peer đóng vai trò:
- Client (gửi tin nhắn)
- Server (nhận và chuyển tiếp tin nhắn)

### Thành phần:
- **Peer Node**:
  - Gửi / nhận tin nhắn
  - Kết nối tới các peer khác
- **Bootstrap Server (Tracker)**:
  - Lưu danh sách peer
  - Hỗ trợ peer mới tham gia mạng

---

## 3. Yêu cầu chức năng

### 3.1. Tham gia mạng P2P
- Peer có thể đăng ký với bootstrap server hoặc một peer đã biết để tham gia mạng
- Hệ thống cung cấp danh sách các peer đang online

---

### 3.2. Chat trực tiếp
- Gửi tin nhắn 1-1 giữa các peer
- Tin nhắn được truyền trực tiếp giữa các peer qua mạng

---

### 3.3. Chat nhóm
- Gửi tin nhắn đến nhiều peer
- Tin nhắn được broadcast tới các thành viên trong nhóm

---

### 3.4. Peer Discovery
- Peer mới tham gia phải có khả năng tìm các peer khác trong mạng

---

### 3.5. Trạng thái Online/Offline
- Hiển thị peer đang online
- Cập nhật trạng thái khi peer tham gia hoặc rời mạng

---

### 3.6. Truyền tin đáng tin cậy
- Đảm bảo message được gửi thành công
- Cơ chế:
  - ACK (xác nhận)
  - Retry khi mất kết nối
  - Timeout

---

## 4. Yêu cầu kỹ thuật

### Cần thể hiện rõ các đặc điểm của hệ thống phân tán:
- giao tiếp mạng bằng *TCP socket hoặc giao thức tương đương*
- mỗi peer có thể gửi và nhận tin đồng thời
- xử lý nhiều kết nối cùng lúc
- quản lý danh sách peer đang kết nố

### Hệ thống cần có các thành phần chính
- *Peer node:* gửi và nhận tin nhắn
- *Bootstrap/Tracker server (đơn giản):* hỗ trợ khám phá pe
