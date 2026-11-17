import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/** Distinguish a local path from a web address before adding a scheme. */
public final class BrowserAddress {
    public static URI resolve(String input) {
        String value = input.trim();
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if (value.isEmpty()) throw new IllegalArgumentException("Hãy nhập địa chỉ web hoặc đường dẫn file.");
        if (value.matches("(?s)^[A-Za-z]:[\\\\/].*") || value.startsWith("\\\\") || value.startsWith("/")) {
            return Path.of(value).toAbsolutePath().normalize().toUri();
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("file:")) return Path.of(URI.create("file:" + value.substring(5).replace(" ", "%20"))).normalize().toUri();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            int colon = value.indexOf(':');
            return URI.create(value.substring(0, colon).toLowerCase(Locale.ROOT) + value.substring(colon).replace(" ", "%20"));
        }
        boolean localHost = value.matches("(?i)^(localhost|(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|\\[[0-9a-f:]+\\])(?::[0-9]+)?(?:[/?#].*)?$");
        boolean explicitPort = value.matches("^[^/:]+:[0-9]+(?:[/?#].*)?$");
        if (!explicitPort && value.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*"))
            throw new IllegalArgumentException("Chỉ hỗ trợ http://, https://, file:// hoặc đường dẫn tuyệt đối.");
        return URI.create((localHost || explicitPort ? "http://" : "https://") + value.replace(" ", "%20"));
    }
}
