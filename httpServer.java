import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.StringTokenizer;

public class httpServer implements Runnable {

    static final File WEB_ROOT = new File(".");
    static final String DEFAULT_FILE = "index.html";
    static final int PORT = 8080;
    private final Socket connect;
    private String requestMethod = "GET";
    private String requestTarget = "/";
    private String requestData = "";

    public httpServer(Socket c) {
        this.connect = c;
    }

    public static void main(String[] args) {
        try {
            ServerDatabase.initialize();
        } catch (Exception e) {
            System.err.println("[PostgreSQL] Không khởi động Server vì chưa kết nối được database: " + e.getMessage());
            System.exit(1);
        }
        try (ServerSocket serverConnect = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"))) {
            printLanAddresses();
            System.out.println("=============================================================");
            System.out.println("   HTTP SERVER ĐA DỊCH VỤ (RFC 2616) ĐÃ SẴN SÀNG KẾT NỐI");
            System.out.println("=============================================================");
            System.out.println("-> Cổng lắng nghe: " + PORT + " (lắng nghe trên 0.0.0.0 - toàn bộ mạng)");
            System.out.println("-> Địa chỉ máy này:        http://localhost:" + PORT + "/index.html");
            System.out.println("-> Máy Phát: dùng địa chỉ Wi-Fi/LAN được in ở trên.");
            System.out.println("-> Hỗ trợ định dạng: .html, .css, .pdf, .mp3, .mp4, .png, .jpg, .json");
            System.out.println("=============================================================\n");

            while (true) {
                Socket clientSocket = serverConnect.accept();
                new Thread(new httpServer(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi máy chủ HTTP: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printLanAddresses() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        System.out.println("[Địa chỉ " + iface.getDisplayName() + "] http://" + addr.getHostAddress() + ":" + PORT + "/index.html");
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void run() {
        try (InputStream rawIn = connect.getInputStream();
             OutputStream rawOut = connect.getOutputStream()) {

            connect.setSoTimeout(5000);
            BufferedInputStream in = new BufferedInputStream(rawIn);

            String requestLine = readLine(in);
            if (requestLine == null || requestLine.isEmpty()) return;
            System.out.println("[Request]: " + requestLine);

            StringTokenizer tokenizer = new StringTokenizer(requestLine);
            String method = tokenizer.nextToken().toUpperCase();
            String fileRequested = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "/";
            requestMethod = method;
            requestTarget = fileRequested;

            // Đọc toàn bộ HTTP Request Headers
            int contentLength = 0;
            String headerLine;
            int headerBytes = 0;
            while ((headerLine = readLine(in)) != null && !headerLine.isEmpty()) {
                headerBytes += headerLine.length();
                if (headerBytes > 65536) throw new IOException("Headers quá lớn");
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.substring(15).trim());
                }
                if (headerLine.toLowerCase().startsWith("transfer-encoding:"))
                    throw new IOException("Chỉ hỗ trợ request có Content-Length");
            }
            if (contentLength < 0 || contentLength > 1048576) throw new IOException("Body vượt giới hạn 1 MB");

            // Nếu là POST, đọc nội dung body
            StringBuilder postData = new StringBuilder();
            if ("POST".equals(method) && contentLength > 0) {
                byte[] buf = in.readNBytes(contentLength);
                if (buf.length != contentLength) throw new EOFException("Body chưa đủ Content-Length");
                postData.append(new String(buf, StandardCharsets.UTF_8));
                requestData = postData.toString();
                System.out.println("[POST Body]: " + postData);
            }

            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                byte[] errBytes = "<html><body><h1>501 Not Implemented</h1></body></html>".getBytes(StandardCharsets.UTF_8);
                sendHttpResponse(rawOut, "501 Not Implemented", "text/html; charset=UTF-8", errBytes, true);
                return;
            }

            fileRequested = URI.create(fileRequested).getPath();
            if (fileRequested.endsWith("/")) {
                fileRequested += DEFAULT_FILE;
            }
            if (fileRequested.startsWith("/")) {
                fileRequested = fileRequested.substring(1);
            }

            Path root = WEB_ROOT.toPath().toRealPath();
            Path requested = root.resolve(fileRequested).normalize();
            // Only publish web resources; never serve Java source, database config or Git files.
            if (!requested.startsWith(root) || (!method.equals("POST") && !fileRequested.toLowerCase().matches(".*\\.(html?|css|js|pdf|mp3|mp4|png|jpe?g|json|ico)"))
                    || (Files.exists(requested) && !requested.toRealPath().startsWith(root))) {
                sendHttpResponse(rawOut, "403 Forbidden", "text/plain; charset=UTF-8", "Forbidden".getBytes(StandardCharsets.UTF_8), !method.equals("HEAD"));
                return;
            }
            File file = requested.toFile();
            byte[] bodyBytes;
            String contentType = getContentType(fileRequested);

            if (file.exists() && !file.isDirectory() && !"POST".equals(method)) {
                bodyBytes = Files.readAllBytes(file.toPath());
            } else if ("POST".equals(method)) {
                String postResponseHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>POST Result</title><link rel='stylesheet' href='style.css'></head><body>"
                        + "<div class='header'><h1>Kết quả Xử lý Yêu cầu POST từ Client</h1></div>"
                        + "<div class='card'>"
                        + "<h2>Dữ liệu đã nhận thành công từ Body:</h2>"
                        + "<p><span>" + escapeHtml(postData.toString()) + "</span></p>"
                        + "<p><a href='index.html'>⬅ Quay lại trang chủ</a></p>"
                        + "</div></body></html>";
                bodyBytes = postResponseHtml.getBytes(StandardCharsets.UTF_8);
                contentType = "text/html; charset=UTF-8";
            } else {
                // File 404
                String notFoundHtml = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>404 Not Found</title></head><body>"
                        + "<h1>404 File Not Found</h1><p>Tài nguyên " + escapeHtml(fileRequested) + " không tồn tại trên máy chủ.</p></body></html>";
                bodyBytes = notFoundHtml.getBytes(StandardCharsets.UTF_8);
                sendHttpResponse(rawOut, "404 Not Found", "text/html; charset=UTF-8", bodyBytes, !method.equals("HEAD"));
                return;
            }

            // Với HEAD: chỉ gửi Header. Với GET và POST: gửi cả Header + Body
            boolean includeBody = method.equals("GET") || method.equals("POST");
            sendHttpResponse(rawOut, "200 OK", contentType, bodyBytes, includeBody);

        } catch (Exception e) {
            System.err.println("Lỗi xử lý kết nối: " + e.getMessage());
        } finally {
            try {
                connect.close();
            } catch (IOException ignored) {}
        }
    }

    private void sendHttpResponse(OutputStream out, String status, String contentType, byte[] body, boolean includeBody) throws IOException {
        String databaseHeader;
        try {
            long id = ServerDatabase.record(connect.getInetAddress().getHostAddress(), requestMethod, requestTarget,
                    requestData, Integer.parseInt(status.substring(0, 3)), body.length);
            System.out.println("[PostgreSQL] ĐÃ LƯU #" + id + " | " + connect.getInetAddress().getHostAddress() + " | " + requestMethod + " " + requestTarget);
            databaseHeader = "X-Database-Saved: true\r\nX-Database-Record-Id: " + id + "\r\n";
        } catch (java.sql.SQLException e) {
            System.err.println("[PostgreSQL] LƯU THẤT BẠI, SQLState=" + e.getSQLState());
            status = "503 Service Unavailable";
            contentType = "text/plain; charset=UTF-8";
            body = "Server chưa lưu được PostgreSQL. Kiểm tra database trên máy Server.".getBytes(StandardCharsets.UTF_8);
            databaseHeader = "X-Database-Saved: false\r\n";
        }
        String headers = "HTTP/1.1 " + status + "\r\n"
                + databaseHeader
                + "Server: VKU-Java-MultiService-HTTP-Server/3.0\r\n"
                + "Date: " + new Date() + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        if (includeBody) {
            out.write(body);
        }
        out.flush();
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') bytes.write(b);
            if (bytes.size() > 8192) throw new IOException("Dòng HTTP quá dài");
        }
        if (b == -1 && bytes.size() == 0) return null;
        return bytes.toString(StandardCharsets.ISO_8859_1);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String getContentType(String fileRequested) {
        String lower = fileRequested.toLowerCase();
        if (lower.endsWith(".htm") || lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        return "text/plain; charset=UTF-8";
    }
}
