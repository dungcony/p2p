package dungcony.ds.ui.components.chatPage;

import dungcony.ds.ui.components.ChatProfile;
import dungcony.ds.ui.components.ModernButton;
import dungcony.ds.ui.utils.ColorPalette;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Component tiêu đề hiển thị thông tin người dùng được chọn
 */
public class ChatHeader extends JPanel {
    /** Nút quay lại ở chế độ mobile */
    private ModernButton backButton;
    /** Nhãn tên người dùng */
    private JLabel userNameLabel;
    /** Component ChatProfile bao ngoài */
    private ChatProfile chatProfile;
    /** Panel thông tin người dùng */
    private JPanel userInformation;
    /** Nhãn trạng thái online/offline */
    private JLabel statusLabel;


    private boolean isMobileMode = false;

    /**
     * Khởi tạo ChatHeader
     * @param userName Tên người dùng
     */
    public ChatHeader(String userName, String ipAddress) {
        try {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            initializeComponents(userName, ipAddress);
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể khởi tạo header chat\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Khởi tạo các thành phần
     * @param userName Tên người dùng
     */
    private void initializeComponents(String userName, String ipAdress) {
        try {
            // Panel thông tin
            userInformation = createDeviceInfoPanel(userName, false);
            chatProfile = new ChatProfile(10, userInformation, ColorPalette.PANEL_BACKGROUND);
            userInformation.setBackground(Color.white);

            // Nút quay lại
            backButton = new ModernButton("", Color.WHITE, Color.WHITE);
            backButton.setIcon(FontIcon.of(FontAwesome.ARROW_LEFT, 20));
            backButton.setBorder(new EmptyBorder(2, 10, 0, 0));
            backButton.setVisible(false); // Ẩn lúc đầu

            add(backButton);
            add(chatProfile);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorPalette.SECONDARY_TEXT.brighter()));

            setBackground(Color.WHITE);
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể khởi tạo component\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Đặt tên người dùng
     * @param userName Tên người dùng
     */
    public void setUserName(String userName) {
        try {
            userNameLabel.setText(userName);
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể cập nhật tên người dùng\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Đặt trạng thái của người dùng
     * @param isOnline true nếu người dùng đang online
     */
    public void setStatus(boolean isOnline) {
        try {
            if (isOnline) {
                statusLabel.setText("Trực tuyến");
                statusLabel.setForeground(ColorPalette.PRIMARY);
                return;
            }
            statusLabel.setText("Ngoại tuyến");
            statusLabel.setForeground(ColorPalette.SECONDARY_TEXT);
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể cập nhật trạng thái\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tạo panel thông tin người dùng
     * @param userName Tên người dùng
     * @param isOnline Trạng thái online
     * @return Panel thông tin với tên và trạng thái
     */
    private JPanel createDeviceInfoPanel(String userName, boolean isOnline) {
        try {
            JPanel panel = new JPanel(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();

            // Tên người dùng
            userNameLabel = new JLabel(userName);
            userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            userNameLabel.setForeground(ColorPalette.TEXT);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(0, 0, 2, 0);
            panel.add(userNameLabel, gbc);

            statusLabel = new JLabel(
                isOnline? "Trực tuyến": "Ngoại tuyến"
            );
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            statusLabel.setForeground(ColorPalette.SECONDARY_TEXT);
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 0, 0);
            panel.add(statusLabel, gbc);

            JLabel lastMessageTime = new JLabel("");
            lastMessageTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridheight = 2;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(0, 20, 0, 0);
            panel.add(lastMessageTime, gbc);

            return panel;
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể tạo panel thông tin thiết bị\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
            return new JPanel();
        }
    }

    
    
    public void addBackFunctinality(ActionListener backFunctionality) {
        try {
            backButton.addActionListener(backFunctionality);
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể thêm chức năng quay lại\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    // Gọi khi chế độ mobile thay đổi
    public void setMobileMode(boolean isMobile) {
        try {
            this.isMobileMode = isMobile;
            backButton.setVisible(isMobile);
            revalidate();
            repaint();
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể thiết lập chế độ mobile\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public JButton getBackButton() {
        try {
            return backButton;
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể lấy nút quay lại\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
