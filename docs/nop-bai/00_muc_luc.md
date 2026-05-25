# Bộ tài liệu nộp đồ án P2P Chat System

## 1. Thông tin đồ án

| Mục                 | Nội dung                                           |
| ------------------- | -------------------------------------------------- |
| Tên đồ án           | P2P Chat System                                    |
| Chủ đề              | Hệ thống chat ngang hàng sử dụng TCP Socket        |
| Ngôn ngữ            | Java 21                                            |
| Giao diện           | Java Swing                                         |
| Build tool          | Maven multi-module                                 |
| Thành phần chính    | `peer-node`, `bootstrap-server`                    |
| Lưu trữ             | JSON local tại peer, SQLite tại bootstrap server  |

## 2. Danh mục sản phẩm cần nộp

### 2.1. Mã nguồn chương trình và hướng dẫn chạy

- Mã nguồn nằm trong thư mục gốc project `p2p/`.
- Hướng dẫn chạy chi tiết: [01_huong_dan_chay.md](01_huong_dan_chay.md).
- Script chạy nhanh trên Windows:
  - `run-bootstrap.bat`: chạy bootstrap/tracker server.
  - `run.bat` hoặc `run-peer.bat`: chạy một peer node bằng tham số CLI.
  - `run-all-peers.bat`: chạy bootstrap và 3 peer demo Alice/Bob/Carol.

### 2.2. Báo cáo đồ án

Các phần báo cáo được tách thành từng file để dễ nộp và bảo trì:

| STT | File                                                                             | Nội dung                                                                                          |
| --- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| 1   | [02_bao_cao_kien_truc_he_thong.md](02_bao_cao_kien_truc_he_thong.md)             | Kiến trúc tổng quan, module, luồng hoạt động, lưu trữ và các quyết định thiết kế.                 |
| 2   | [03_giao_thuc_trao_doi_thong_diep.md](03_giao_thuc_trao_doi_thong_diep.md)       | Giao thức TCP giữa các peer, giao thức command với bootstrap, kiểu message, ACK, retry.           |
| 3   | [04_co_che_peer_discovery.md](04_co_che_peer_discovery.md)                       | Cơ chế khám phá peer qua bootstrap, fallback qua peer đã biết, đồng bộ online/offline.            |
| 4   | [05_xu_ly_loi_va_thu_nghiem_he_thong.md](05_xu_ly_loi_va_thu_nghiem_he_thong.md) | Xử lý lỗi, kịch bản lỗi, test tự động, test thủ công, đánh giá kết quả.                           |
