package dungcony.ds.ui.components.scannerPage;

import dungcony.ds.App;
import dungcony.ds.ui.components.ChatProfile;
import dungcony.ds.ui.components.LoadingComponent;
import dungcony.ds.ui.components.ModernButton;
import dungcony.ds.ui.components.ModernScrollBarUI;
import dungcony.ds.ui.utils.ColorPalette;
import dungcony.ds.ui.utils.Dialog;

import javax.swing.*;
import java.awt.*;

public class FoundDevices extends JPanel {

    // Container hiển thị các thiết bị gần đây
    private JPanel devicesContainer;
    // Thanh cuộn
    private JScrollPane scrollPane;
    // Nhãn tiêu đề
    private JLabel statusLabel;
    // Panel khi không tìm thấy thiết bị
    private NoDevicePanel noDeviceFoundPanel;

    // Component loading
    private JPanel loader;

    private LoadingComponent loadingComponent;
    // Danh sách thiết bị tìm thấy
    String[] foundDevices;

    // Khởi tạo lớp {@code FoundDevices}
    public FoundDevices()  {
        initializeComponents();
        setupLayout();
        addFoundDevices();
    }
    
    // Khởi tạo các thành phần
    private void initializeComponents() {
        // Thiết lập container chính
        this.setBackground(ColorPalette.BACKGROUND);
        this.setLayout(new BorderLayout(0, 15));
        
        // Nhãn trạng thái
        statusLabel = new JLabel("Thiết bị tìm thấy:", SwingConstants.LEFT);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(ColorPalette.TEXT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));
        
        // Container thiết bị
        devicesContainer = new JPanel();
        devicesContainer.setLayout(new BoxLayout(devicesContainer, BoxLayout.Y_AXIS));
        devicesContainer.setBackground(ColorPalette.PANEL_BACKGROUND);
        devicesContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Thanh cuộn
        scrollPane = new JScrollPane(devicesContainer);
        scrollPane.setBackground(ColorPalette.BACKGROUND);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Trang trí thanh cuộn
        scrollPane.getVerticalScrollBar().setBackground(ColorPalette.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        // no device found panel

        // Loading
        loader = new JPanel();
        loader.setLayout(new BorderLayout());
        loadingComponent = new LoadingComponent();
        loadingComponent.setLoadingText("Đang quét thiết bị gần đây...");
        loader.add(loadingComponent, BorderLayout.CENTER);
        noDeviceFoundPanel = new NoDevicePanel();
    }


    // Thiết lập bố cục
    private void setupLayout() {
        add(statusLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    // Thêm các thiết bị tìm thấy vào container
    private void addFoundDevices() {
        if (foundDevices == null) {
            // Panel không tìm thấy thiết bị
            addNoDevicesFoundPanel();
            return;
        }

        // Thêm từng thiết bị
        for (String ip: foundDevices) {
            addDevice("User", ip);
        }
    }
    
    // Cập nhật danh sách thiết bị tìm thấy
    // @param foundDevices mảng thiết bị mới
    public void setFoundDevices(String[] foundDevices) {
        // Xóa hiển thị cũ
        devicesContainer.removeAll();

        // Cập nhật trạng thái
        this.foundDevices = foundDevices;
        // Hiển thị lại
        addFoundDevices();
    }

    

    // Thêm panel không tìm thấy thiết bị
    private void addNoDevicesFoundPanel() {
        devicesContainer.add(noDeviceFoundPanel);
    }


    // Thêm thiết bị vào container
    // @param deviceName Tên thiết bị
    // @param ipAddress  Địa chỉ IP
    public void addDevice(String deviceName, String ipAddress)  {
        
        JPanel deviceInfo = createDeviceInfoPanel(deviceName, ipAddress);
        ChatProfile deviceProfile = new ChatProfile(12, deviceInfo, ColorPalette.BACKGROUND);
        deviceProfile.setBackground(ColorPalette.BACKGROUND);
        // Khoảng cách giữa các thiết bị
        if (devicesContainer.getComponentCount() > 0) {
            devicesContainer.add(Box.createVerticalStrut(8));
        }
        
        devicesContainer.add(deviceProfile);
        devicesContainer.revalidate();
        devicesContainer.repaint();
    }
    
    // Tạo panel thông tin thiết bị
    // @param deviceName Tên thiết bị
    // @param ipAddress  Địa chỉ IP
    private JPanel createDeviceInfoPanel(String deviceName, String ipAddress) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorPalette.PANEL_BACKGROUND);
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Tên thiết bị
        JLabel nameLabel = new JLabel(deviceName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(ColorPalette.TEXT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 2, 0);
        panel.add(nameLabel, gbc);
        
        // Địa chỉ IP
        JLabel ipLabel = new JLabel(ipAddress);
        ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ipLabel.setForeground(ColorPalette.SECONDARY_TEXT);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(ipLabel, gbc);
        
        // Nút thêm
        ModernButton addButton = new ModernButton("Thêm", ColorPalette.PRIMARY, ColorPalette.SECONDARY);
        // Thêm người dùng vào cơ sở dữ liệu
        addButton.addActionListener(e -> addUser(ipAddress));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 20, 0, 0);
        panel.add(addButton, gbc);
        
        return panel;
    }

    // Thêm người dùng vào cơ sở dữ liệu
    // @param ip Địa chỉ IP
    private void addUser(String ip) {
        String response = Dialog.showInputDialog(this, "Nhập tên peer", "Thêm peer", Dialog.QUESTION_MESSAGE);
        if (response != null && response.isBlank()) {
            Dialog.showMessageDialog(this, "Vui lòng nhập tên peer", "Tên không hợp lệ", Dialog.ERROR_MESSAGE);
            return;
        }
        if (response != null && App.peerNode != null) {
            dungcony.ds.model.PeerInfo peerInfo = App.peerNode.addKnownPeer(response, ip);
            App.peerNode.discoverPeersFromKnownPeer(peerInfo);
        }
    }

    public void setLoadingPanel() {
        devicesContainer.removeAll();
        devicesContainer.add(loader);
        devicesContainer.revalidate();
        loadingComponent.startLoading();
    }
}

// Component hiển thị khi không tìm thấy thiết bị
class NoDevicePanel extends JPanel {
    JLabel label;
    public NoDevicePanel() {
        label = new JLabel("Không tìm thấy thiết bị", SwingConstants.CENTER);
        label.setForeground(ColorPalette.SECONDARY_TEXT);
        label.setFont(new Font("Segoe UI", Font.ITALIC, 20));
        setBackground(ColorPalette.PANEL_BACKGROUND);
        add(label);
    }
}
