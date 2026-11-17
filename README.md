# VKU - Lab 4: Simple Web Browser & HTTP Server

Dự án Lập trình mạng: Xây dựng trình duyệt Web đơn giản (**SimpleBrowser**) và Máy chủ Web đa luồng (**httpServer**) hỗ trợ chuẩn HTTP RFC 2616, hiển thị trang HTML/CSS, phát đa phương tiện (MP3, MP4, PDF), tải tệp tin và lưu trữ lịch sử duyệt web vào Cơ sở dữ liệu (PostgreSQL / MySQL / CSV).

---

## 📁 Cấu trúc thư mục

```text
network-simple-browser/
├── httpServer.java            # Máy chủ HTTP đa luồng (hỗ trợ GET, POST, HEAD, Range, Content-Type)
├── SimpleBrowser.java         # Trình duyệt Web đồ họa Swing (gửi request, render HTML/CSS, tải file, quét LAN)
├── start_server.bat           # File khởi chạy 1-click Web Server
├── start_browser.bat          # File khởi chạy 1-click Trình duyệt Web
├── index.html                 # Trang chủ mẫu giới thiệu đa phương tiện
├── style.css                  # Giao diện CSS hiện đại cho trang web
├── sample.pdf                 # Tệp tài liệu mẫu kiểm tra xem/tải PDF
├── sample.mp3                 # Tệp âm thanh mẫu kiểm tra phát Audio
├── sample.mp4                 # Tệp video mẫu kiểm tra phát Video
├── postgresql-connector.jar   # Driver kết nối PostgreSQL
├── mysql-connector.jar        # Driver kết nối MySQL
└── README.md                  # Hướng dẫn chi tiết
```

---

## 🚀 Hướng dẫn chạy thử nghiệm kết nối 2 máy (Mạng LAN / Wi-Fi)

> **Điều kiện tiên quyết:** Cả 2 laptop kết nối cùng một mạng Wi-Fi (hoặc phát Wi-Fi Hotspot từ điện thoại).

### Bước 1: Khởi động Web Server (trên Máy 1)
1. Trên **Máy 1**, nhấp đúp vào file `start_server.bat` (hoặc mở Terminal gõ `javac httpServer.java` rồi `java httpServer`).
2. Màn hình console sẽ xuất hiện thông báo:
   ```text
   =============================================================
      HTTP SERVER ĐA DỊCH VỤ (RFC 2616) ĐÃ SẴN SÀNG KẾT NỐI
   =============================================================
   -> Cổng lắng nghe: 8080 (lắng nghe trên 0.0.0.0 - toàn bộ mạng)
   -> Địa chỉ máy này:        http://localhost:8080/index.html
   -> Địa chỉ cho MÁY KHÁC:   http://192.168.x.x:8080/index.html
   =============================================================
   ```
   *(Ghi nhớ địa chỉ IP LAN hiển thị trên màn hình, ví dụ: `192.168.6.243`)*.

> [!NOTE]
> Nếu máy khác không kết nối được, hãy kiểm tra Windows Firewall trên Máy 1 xem đã mở cổng 8080 chưa (xem mục Xử lý sự cố bên dưới).

---

### Bước 2: Kết nối từ Trình duyệt (trên Máy 2)
1. Trên **Máy 2**, nhấp đúp vào file `start_browser.bat` để mở giao diện **SimpleBrowser**.
2. **Cách 1 (Tự động - Khuyên dùng):** Bấm nút **`Quét LAN`** trên thanh công cụ:
   - Trình duyệt sẽ tự động quét mạng nội bộ tìm Máy 1 đang mở cổng 8080.
   - Khi tìm thấy, URL sẽ tự động được điền: `http://192.168.x.x:8080/index.html`.
3. **Cách 2 (Nhập thủ công):**
   - Nhập vào thanh địa chỉ: `http://<IP_MÁY_1>:8080/index.html` (Ví dụ: `http://192.168.6.243:8080/index.html`).
   - Bấm **`Gửi Request`** (hoặc Enter).
4. Bạn cũng có thể mở trình duyệt Chrome / Edge trên Máy 2 và gõ địa chỉ trên để kiểm chứng tính tương thích chuẩn HTTP RFC 2616!

---

## 🌟 Các tính năng nổi bật được hỗ trợ

1. **Giao thức HTTP RFC 2616 đầy đủ:**
   - Hỗ trợ các phương thức: `GET`, `POST` (gửi form dữ liệu), `HEAD` (kiểm tra header dung lượng).
   - Mã trạng thái: `200 OK`, `404 Not Found`, `400 Bad Request`, `501 Not Implemented`.
   - Đa luồng (`multi-threading`): Xử lý đồng thời nhiều client truy cập cùng lúc.

2. **Duyệt Web & Đa phương tiện:**
   - Render văn bản HTML và định dạng CSS với giao diện hiện đại.
   - Hỗ trợ liên kết siêu văn bản (`Hyperlink`): Nhấp vào link trên trang web để chuyển trang tự động.
   - Nút điều hướng: `< Lùi` (Back), `Tiến >` (Forward).
   - Nút **`📥 Tải file (Download)`**: Tải tài liệu PDF, bài hát MP3, video MP4 lưu về máy tính.
   - Nút **`Mở Chrome`**: Mở liên kết hiện tại bằng trình duyệt mặc định của hệ thống.

3. **Cơ sở dữ liệu Lịch sử (Database Integration):**
   - Tự động lưu mọi lượt truy cập (Thời gian, Method, URL, Trạng thái HTTP, Dung lượng) vào:
     - **PostgreSQL**: Bảng `browser_history` (Database `lab8_server1`).
     - **MySQL**: Bảng `browser_history` (Database `vku_network_db`).
     - **CSV Fallback**: Tự động lưu vào file `browser_history.csv` nếu máy chưa cài Database.
   - Nút **`🗄️ CSDL`**: Xem thống kê và kiểm tra kết nối CSDL trực quan.
   - Nút **`Lịch sử`**: Xem bảng danh sách lịch sử truy cập, có chức năng tìm kiếm và xóa lịch sử.

---

## 🛠️ Xử lý sự cố tường lửa (Windows Firewall)

Nếu Máy 2 không kết nối được đến Máy 1 dù đã cùng mạng Wi-Fi:
1. Mở PowerShell với quyền **Run as Administrator** trên Máy 1.
2. Chạy lệnh sau để cho phép cổng 8080:
   ```powershell
   netsh advfirewall firewall add rule name="Lab4_HTTP_8080" dir=in action=allow protocol=TCP localport=8080
   ```
3. Hoặc chuyển loại mạng Wi-Fi từ `Public` sang `Private`:
   ```powershell
   Set-NetConnectionProfile -Name "Tên_WiFi" -NetworkCategory Private
   ```
