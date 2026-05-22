package dungcony.ds.ui.components.addFriendPage;

import lombok.extern.slf4j.Slf4j;

import dungcony.ds.ui.components.RoundedPanel;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

@Slf4j
public class CenterPanel extends JPanel {

    private static final int MEDIUM_WIDTH = 768;

    // Panel bên phải
    private RightPanel rightPanel;
    // Panel bên trái
    private LeftPanel leftPanel;

    public CenterPanel() {
        try {
            // Đặt màu nền
            setBackground(ColorPalette.BACKGROUND);
            setBorder(new EmptyBorder(20, 20, 20, 20));

            // Tạo các panel
            rightPanel = new RightPanel();
            leftPanel = new LeftPanel();

            // Sử dụng layout có thể thay đổi động
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            // Thêm khoảng cách giữa các panel
            add(Box.createHorizontalStrut(20));
            add(leftPanel);
            add(Box.createHorizontalStrut(40));
            add(rightPanel);
            add(Box.createHorizontalStrut(20));

            // Lắng nghe sự kiện thay đổi kích thước
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    try {
                        updateLayout();
                    } catch (Exception ex) {
                        log.error("Chi tiết lỗi", ex);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Chi tiết lỗi", e);
        }
    }

    // Cập nhật bố cục khi thay đổi kích thước
    private void updateLayout() {
        try {
            int width = getWidth();
            removeAll();

            // Chuyển layout theo chiều rộng
            if (width < MEDIUM_WIDTH) {
                // Bố cục dọc
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                add(Box.createVerticalStrut(20));
                add(leftPanel);
                add(Box.createVerticalStrut(40));
                add(rightPanel);
                add(Box.createVerticalStrut(20));
            } else {
                // Bố cục ngang
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                add(Box.createHorizontalStrut(20));
                add(leftPanel);
                add(Box.createHorizontalStrut(40));
                add(rightPanel);
                add(Box.createHorizontalStrut(20));
            }

            // Tính toán lại kích thước panel
            rightPanel.setMaximumSize(new Dimension(
                    width < MEDIUM_WIDTH ? width - 40 : (width / 2) - 60,
                    Integer.MAX_VALUE));

            rightPanel.updateInternalWrapping(
                    width < MEDIUM_WIDTH ? width - 40 : (width / 2) - 60, width);

            revalidate();
            repaint();
        } catch (Exception e) {
            log.error("Không thể cập nhật bố cục\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }
}

// Panel trái của trang thêm bạn bè
@Slf4j
class LeftPanel extends RoundedPanel {

    // Nhãn logo LAN Messenger
    private JLabel logoLabel;
    // Nhãn tiêu đề
    private JLabel titleLabel;

    public LeftPanel() {
        super(15, ColorPalette.PANEL_BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // Tiêu đề
        titleLabel = new JLabel("Nhắn tin LAN");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(ColorPalette.PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Logo
        logoLabel = new JLabel();
        loadImage();
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Thêm các component
        add(Box.createVerticalGlue());
        add(titleLabel);
        add(Box.createVerticalStrut(20));
        add(logoLabel);
        add(Box.createVerticalStrut(10));

        // Dòng slogan
        JLabel taglineLabel = new JLabel("Kết nối an toàn với các peer của bạn");
        taglineLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        taglineLabel.setForeground(ColorPalette.SECONDARY_TEXT);
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(taglineLabel);
        add(Box.createVerticalGlue());
    }

    // Tải ảnh logo
    private void loadImage() {
        try {
            ImageIcon image = new ImageIcon("public/images/app/logo.png");
            image = new ImageIcon(image.getImage().getScaledInstance(280, 280, Image.SCALE_SMOOTH));
            logoLabel.setIcon(image);
        } catch (Exception e) {
            log.error("Không thể tải ảnh\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }
}

// Panel phải của trang thêm bạn bè
class RightPanel extends RoundedPanel {
    private static final int LARGE_WIDTH = 1200;
    // Panel hiển thị IP và nút copy
    private IPAddressPanel ipAddressPanel;
    // Panel hướng dẫn người dùng
    private TextLabelForRightPanel textPanel;

    public RightPanel() {
        super(15, ColorPalette.PANEL_BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // Tiêu đề panel
        JLabel titleLabel = new JLabel("Chia sẻ kết nối của bạn");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(ColorPalette.PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);
        // Khoảng cách
        add(Box.createVerticalStrut(20));
        ipAddressPanel = new IPAddressPanel();
        add(ipAddressPanel);

        textPanel = new TextLabelForRightPanel();
        add(textPanel);
    }

    // Cập nhật kích thước {@code textPanel} khi thay đổi kích thước
    // @param width chiều rộng tính toán được
    // @param parentWidth chiều rộng cửa sổ
    public void updateInternalWrapping(int width, int parentWidth) {
        textPanel.updateInternalWrapping(width);
        ipAddressPanel.setPreferredSize(new Dimension(Math.min(300, width - 60), 60));
        if (parentWidth < LARGE_WIDTH - 100) {
            ipAddressPanel.setPreferredSize(new Dimension(Math.min(300, width - 60), 80));
        }
    }
}

// Container chứa text hướng dẫn
class TextLabelForRightPanel extends JPanel {
    // Ô text hướng dẫn có thể wrap
    private JTextArea instructionLabel;

    public TextLabelForRightPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        instructionLabel = new JTextArea(
                "Gửi địa chỉ IP này cho peer khác để thiết lập kết nối. " +
                        "Khi họ nhập địa chỉ này, hai bên có thể bắt đầu chat.");
        instructionLabel.setLineWrap(true);
        instructionLabel.setWrapStyleWord(true);
        instructionLabel.setEditable(false);
        instructionLabel.setFocusable(false);
        instructionLabel.setOpaque(false);
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        instructionLabel.setForeground(ColorPalette.TEXT);
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

        add(instructionLabel, BorderLayout.CENTER);
    }

    //
    // @param width Chiều rộng mới đã tính cho {@code instructionLabel}
    public void updateInternalWrapping(int width) {
        int textAreaWidth = width - 40;
        instructionLabel.setPreferredSize(new Dimension(textAreaWidth, 90));
        revalidate();
        repaint();
    }
}
