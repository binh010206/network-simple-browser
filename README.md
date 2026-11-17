# Lab 4 — HTTP Browser và Server

Máy Server chạy Java + PostgreSQL. Máy Client chỉ cần JDK 21 và VS Code.

## Chạy bằng Run Code

1. Mở **đúng thư mục repo** trong VS Code, cài extension Code Runner.
2. Server: mở `httpServer.java`, bấm **Run Code** (Ctrl+Alt+N).
3. Client: mở `SimpleBrowser.java`, bấm **Run Code**.
4. Nhập `http://IP_MAY_SERVER:8080/index.html` trên Client.

Server in các IP hiện tại khi khởi động. Chọn IP Wi-Fi/LAN cùng mạng với Client.
Cho phép TCP 8080 qua firewall trên Server. Không cần mở cổng PostgreSQL ra mạng.
Nếu chạy cả Server và Client trên cùng máy, dùng hai cửa sổ VS Code.

## PostgreSQL chỉ trên Server

Tạo database và tài khoản riêng hoặc dùng database hiện có mà tài khoản có quyền tạo bảng.
Tạo `database.local.properties` trên máy Server (file này được Git bỏ qua):

```properties
url=jdbc:postgresql://127.0.0.1:5432/TEN_DATABASE
user=TEN_USER
password=MAT_KHAU_CUA_BAN
```

Có thể dùng biến môi trường BROWSER_DB_URL, BROWSER_DB_USER, BROWSER_DB_PASSWORD thay thế.
Server tự tạo bảng `public.browser_history`, thêm cột `client_ip` nếu bảng đã tồn tại.
Server chỉ khởi động khi kết nối được database. Mỗi request được lưu trước khi trả response.
Header `X-Database-Saved: true` và `X-Database-Record-Id` xác nhận bản ghi đã lưu.
Nếu database lỗi lúc xử lý request, Server trả HTTP 503 và `X-Database-Saved: false`.

Client lưu lịch sử cục bộ vào CSV; xóa lịch sử Client không xóa nhật ký PostgreSQL.

## Demo dữ liệu thật

Gửi GET `/index.html`, HEAD `/sample.pdf`, POST với `message=Phát kiểm tra`, tải PDF/MP3/MP4.
Mở pgAdmin trên Server, chọn database đã cấu hình, mở Query Tool và chạy:

```sql
SELECT id, time, client_ip, method, url, post_data, status_code, content_length
FROM public.browser_history
ORDER BY id DESC;
```

Bấm F5 chạy lại truy vấn sau mỗi lần gửi request. Không cần cài PostgreSQL trên máy Client.

## Thanh địa chỉ: web và file trên máy

- `vku.udn.vn` tự chuyển thành `https://vku.udn.vn`.
- `localhost:8080` hoặc `10.x.x.x:8080` dùng HTTP để demo LAN.
- Dán đường dẫn tuyệt đối, kể cả có dấu/khoảng trắng/ngoặc kép, hoặc URI `file:///` để đọc file trên chính máy chạy Browser.
- Xem trực tiếp HTML, văn bản UTF-8 (TXT/CSV/JSON/XML/MD/mã nguồn), ảnh PNG/JPEG/GIF/BMP và PDF có nút chuyển trang.
- Mở đường dẫn thư mục để chọn file bên trong. File không tồn tại có thông báo lỗi.
- Định dạng khác và file trên 64 MB có nút mở bằng ứng dụng mặc định; không giả vờ đã xem được nội dung.
- File cục bộ không gửi lên Server/PostgreSQL. Lịch sử trên Client vẫn lưu để dùng nút Lùi/Tiến.
- PDF dùng [Apache PDFBox 3.0.8](https://pdfbox.apache.org/), đóng gói trong `pdfbox-app.jar` (Apache License 2.0, giấy phép nằm trong JAR).
