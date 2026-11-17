import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;

/** Run Code entry point: java Run.java httpServer or SimpleBrowser. */
public class Run {
    public static void main(String[] args) throws Exception {
        String main = args.length == 0 ? "SimpleBrowser" : args[0];
        if (!Set.of("httpServer", "SimpleBrowser", "build").contains(main)) {
            System.err.println("Mở httpServer.java (Server) hoặc SimpleBrowser.java (Client), rồi bấm Run Code.");
            return;
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Cần cài JDK 21 để chạy.");
        Files.createDirectories(Path.of(".build"));
        int result = compiler.run(null, System.out, System.err, "-encoding", "UTF-8", "-cp", "pdfbox-app.jar", "-d", ".build",
                "httpServer.java", "ServerDatabase.java", "SimpleBrowser.java", "BrowserAddress.java", "PdfPreview.java");
        if (result != 0) System.exit(result);
        if (main.equals("build")) return;
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classPath = String.join(java.io.File.pathSeparator, ".build", "postgresql-connector.jar", "pdfbox-app.jar");

        if (main.equals("SimpleBrowser")) {
            boolean serverRunning = false;
            try (var socket = new java.net.Socket("127.0.0.1", 8080)) {
                serverRunning = true;
            } catch (Exception ignored) {}

            if (!serverRunning) {
                System.out.println("[Run Code] Chưa thấy httpServer chạy, đang tự động bật httpServer chạy ngầm...");
                new ProcessBuilder(javaExecutable, "-cp", classPath, "httpServer").start();
                Thread.sleep(600);
            }
        }

        Process process = new ProcessBuilder(javaExecutable, "-cp", classPath, main)
                .inheritIO().start();
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
        System.exit(process.waitFor());
    }
}
