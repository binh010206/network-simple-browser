import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** Render one page at a time, off the Swing event thread. */
public final class PdfPreview extends JPanel {
    private final JLabel picture = new JLabel("", SwingConstants.CENTER);
    private final JLabel pageInfo = new JLabel();
    private final JButton previous = new JButton("Trang trước");
    private final JButton next = new JButton("Trang sau");
    private byte[] source;
    private int page;
    private int count;
    private int generation;

    public PdfPreview() {
        super(new BorderLayout());
        JPanel controls = new JPanel();
        controls.add(previous);
        controls.add(pageInfo);
        controls.add(next);
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(picture), BorderLayout.CENTER);
        previous.addActionListener(e -> render(page - 1));
        next.addActionListener(e -> render(page + 1));
    }

    public void showPdf(byte[] bytes) {
        source = bytes;
        count = 0;
        render(0);
    }

    public void clear() {
        generation++;
        source = null;
        picture.setIcon(null);
        picture.setText("");
    }

    private void render(int requestedPage) {
        if (source == null || requestedPage < 0 || (count > 0 && requestedPage >= count)) return;
        int ticket = ++generation;
        byte[] bytes = source;
        previous.setEnabled(false);
        next.setEnabled(false);
        picture.setIcon(null);
        picture.setText("Đang đọc PDF...");
        new SwingWorker<BufferedImage, Void>() {
            int total;
            protected BufferedImage doInBackground() throws IOException {
                try (PDDocument document = Loader.loadPDF(bytes)) {
                    total = document.getNumberOfPages();
                    if (total == 0) throw new IOException("PDF không có trang");
                    return new PDFRenderer(document).renderImageWithDPI(requestedPage, 110);
                }
            }
            protected void done() {
                if (ticket != generation) return;
                try {
                    picture.setIcon(new ImageIcon(get()));
                    picture.setText("");
                    page = requestedPage;
                    count = total;
                    pageInfo.setText("Trang " + (page + 1) + " / " + count);
                    previous.setEnabled(page > 0);
                    next.setEnabled(page + 1 < count);
                } catch (Exception e) {
                    pageInfo.setText("Không thể xem PDF");
                    picture.setText("PDF bị lỗi hoặc cần mật khẩu. Dùng nút Mở ứng dụng mặc định.");
                }
            }
        }.execute();
    }
}
