# Bộ tài liệu nộp đồ án P2P Chat System

## 1. Thông tin đồ án

| Mục | Nội dung |
| --- | --- |
| Tên đồ án | P2P Chat System |
| Chủ đề | Hệ thống chat ngang hàng sử dụng TCP Socket |
| Ngôn ngữ | Java 21 |
| Giao diện | Java Swing |
| Build tool | Maven multi-module |
| Thành phần chính | `peer-node`, `bootstrap-server` |
| Lưu trữ | JSON local tại peer, SQLite tại bootstrap server |

> Nhóm điền thêm tên lớp, giảng viên hướng dẫn, danh sách thành viên và ngày nộp theo mẫu của môn học.

## 2. Danh mục sản phẩm cần nộp

### 2.1. Mã nguồn chương trình và hướng dẫn chạy

- Mã nguồn nằm trong thư mục gốc project `p2p/`.
- Hướng dẫn chạy chi tiết: [01_huong_dan_chay.md](01_huong_dan_chay.md).
- Script chạy nhanh trên Windows:
  - `run-bootstrap.bat`: chạy bootstrap/tracker server.
  - `run.bat`: chạy một peer node.

### 2.2. Báo cáo đồ án

Các phần báo cáo được tách thành từng file để dễ nộp và bảo trì:

| STT | File | Nội dung |
| --- | --- | --- |
| 1 | [02_bao_cao_kien_truc_he_thong.md](02_bao_cao_kien_truc_he_thong.md) | Kiến trúc tổng quan, module, luồng hoạt động, lưu trữ và các quyết định thiết kế. |
| 2 | [03_giao_thuc_trao_doi_thong_diep.md](03_giao_thuc_trao_doi_thong_diep.md) | Giao thức TCP giữa các peer, giao thức command với bootstrap, kiểu message, ACK, retry. |
| 3 | [04_co_che_peer_discovery.md](04_co_che_peer_discovery.md) | Cơ chế khám phá peer qua bootstrap, fallback qua peer đã biết, đồng bộ online/offline. |
| 4 | [05_xu_ly_loi_va_thu_nghiem_he_thong.md](05_xu_ly_loi_va_thu_nghiem_he_thong.md) | Xử lý lỗi, kịch bản lỗi, test tự động, test thủ công, đánh giá kết quả. |

## 3. Gợi ý cấu trúc khi ghép thành một báo cáo duy nhất

Nếu giảng viên yêu cầu nộp một file báo cáo duy nhất, có thể ghép theo thứ tự:

1. Trang bìa
2. Mục lục
3. Giới thiệu và mục tiêu
4. Kiến trúc hệ thống
5. Giao thức trao đổi thông điệp
6. Cơ chế peer discovery
7. Xử lý lỗi và thử nghiệm hệ thống
8. Kết luận, hạn chế và hướng phát triển
9. Phụ lục hướng dẫn chạy

## 4. Checklist trước khi nộp

- [ ] Điền đầy đủ thông tin nhóm ở đầu báo cáo.
- [ ] Chạy lại `mvn test` để có kết quả kiểm thử mới nhất.
- [ ] Kiểm tra `run-bootstrap.bat` và ít nhất hai cửa sổ `run.bat` chạy được.
- [ ] Xóa dữ liệu thử không cần nộp trong `tmp/` nếu có.
- [ ] Không nộp thư mục `target/` nếu giảng viên chỉ yêu cầu mã nguồn.
- [ ] Đảm bảo project dùng Java 21 đúng với báo cáo.
