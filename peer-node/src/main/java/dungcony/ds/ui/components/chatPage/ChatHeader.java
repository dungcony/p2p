package dungcony.ds.ui.components.chatPage;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.ui.components.ChatProfile;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import java.awt.*;

// Component tiêu đề hiển thị thông tin người dùng được chọn
@Slf4j
public class ChatHeader extends JPanel {
    // Nhãn tên người dùng
    private JLabel userNameLabel;
    // Component ChatProfile bao ngoài
    private ChatProfile chatProfile;
    // Panel thông tin người dùng
    private JPanel userInformation;
    // Nhãn trạng thái online/offline
    private JLabel statusLabel;

    // Khởi tạo ChatHeader
    // @param userName Tên người dùng
    public ChatHeader(String userName) {
        try {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            initializeComponents(userName);
        } catch (Exception e) {
            log.error("Không thể khởi tạo header chat\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Khởi tạo các thành phần
    // @param userName Tên người dùng
    private void initializeComponents(String userName) {
        try {
            // Panel thông tin
            userInformation = createDeviceInfoPanel(userName, false);
            chatProfile = new ChatProfile(10, userInformation, ColorPalette.PANEL_BACKGROUND);
            userInformation.setBackground(Color.white);

            add(chatProfile);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorPalette.SECONDARY_TEXT.brighter()));

            setBackground(Color.WHITE);
        } catch (Exception e) {
            log.error("Không thể khởi tạo component\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Đặt tên người dùng
    // @param userName Tên người dùng
    public void setUserName(String userName) {
        try {
            userNameLabel.setText(userName);
        } catch (Exception e) {
            log.error("Không thể cập nhật tên người dùng\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Đặt trạng thái của người dùng
    // @param isOnline true nếu người dùng đang online
    public void setStatus(boolean isOnline) {
        try {
            if (isOnline) {
                statusLabel.setText("● Trực tuyến");
                statusLabel.setForeground(ColorPalette.ACCENT);
                return;
            }
            statusLabel.setText("● Ngoại tuyến");
            statusLabel.setForeground(ColorPalette.ERROR);
        } catch (Exception e) {
            log.error("Không thể cập nhật trạng thái\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Hiển thị header khi đang mở group chat
    public void setGroupStatus() {
        try {
            statusLabel.setText("Nhóm");
            statusLabel.setForeground(ColorPalette.PRIMARY);
        } catch (Exception e) {
            log.error("Không thể cập nhật trạng thái nhóm\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Hiển thị header khi đang mở broadcast toàn mạng
    public void setBroadcastStatus() {
        try {
            statusLabel.setText("Broadcast");
            statusLabel.setForeground(ColorPalette.PRIMARY);
        } catch (Exception e) {
            log.error("Không thể cập nhật trạng thái broadcast\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }

    // Tạo panel thông tin người dùng
    // @param userName Tên người dùng
    // @param isOnline Trạng thái online
    // @return Panel thông tin với tên và trạng thái
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
                isOnline ? "● Trực tuyến" : "● Ngoại tuyến"
            );
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            statusLabel.setForeground(isOnline ? ColorPalette.ACCENT : ColorPalette.ERROR);
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
            log.error("Không thể tạo panel thông tin thiết bị\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
            return new JPanel();
        }
    }
}
