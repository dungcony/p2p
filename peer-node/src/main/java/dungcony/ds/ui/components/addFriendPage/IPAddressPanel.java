package dungcony.ds.ui.components.addFriendPage;

import lombok.extern.slf4j.Slf4j;


import dungcony.ds.App;
import dungcony.ds.ui.components.Button;
import dungcony.ds.ui.components.RoundedPanel;
import dungcony.ds.ui.utils.ColorPalette;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


// Panel hiển thị địa chỉ IP và nút sao chép
@Slf4j
class IPAddressPanel extends RoundedPanel {
// Nhãn hiển thị địa chỉ IP
    private JLabel ipAddressLabel;
    // Nhãn tiêu đề
    private JLabel titleLabel;
    // Nút sao chép
    private Button copyButton;
    // Địa chỉ IP
    private String ipAddress;
    
    public IPAddressPanel() {
        super(10, ColorPalette.BACKGROUND);
        try {
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
            
            // Lấy địa chỉ IP từ hệ thống
            ipAddress = getWifiIPAddress();
            
            // Nhãn tiêu đề
            titleLabel = new JLabel("Địa chỉ IP của bạn:");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setForeground(ColorPalette.SECONDARY_TEXT);
            
            // Nhãn địa chỉ IP
            ipAddressLabel = new JLabel(ipAddress);
            ipAddressLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            ipAddressLabel.setForeground(ColorPalette.PRIMARY);
            
            // Nút sao chép
            copyButton = new Button(FontIcon.of(FontAwesome.COPY, 15));
            copyButton.setToolTipText("Sao chép địa chỉ IP");
            
            // Sự kiện click nút sao chép
            copyButton.addActionListener(e -> {
                try {
                    copyIPAddressToClipboard();
                } catch (Exception ex) {
                    log.error("Không thể sao chép địa chỉ IP vào clipboard\nChi tiết lỗi: " + ex.getMessage());
                    log.error("Chi tiết lỗi", ex);
                }
            });
            
            // Thêm các component
            add(titleLabel);
            add(ipAddressLabel);
            add(copyButton);
        } catch (Exception e) {
            log.error("Không thể khởi tạo panel địa chỉ IP\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }
    
    // Lấy địa chỉ IP WiFi
    // @return Chuỗi địa chỉ IP
    private String getWifiIPAddress() {
        if (App.peerNode != null) {
            return App.peerNode.getLocalPeer().addressKey();
        }
        try {
            // Các tên giao diện WiFi phổ biến
            Set<String> wifiInterfaceIdentifiers = new HashSet<>(Arrays.asList(
                // Các mẫu tên trên Windows
                "wi-fi", "wireless", "wlan", 
                // Các mẫu tên trên Linux
                "wlp", "wlo", "wlx", "wls", "ath", "wifi",
                // Các mẫu tên trên macOS
                "en", "airport",
                // Các mẫu tên chung
                "wireless"
            ));
            
            // Phương pháp 1: Tìm giao diện khởi động, không là loopback
            // và khớp với các mẫu tên mạng không dây phổ biến
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                // Bỏ qua giao diện tắt, loopback, ảo
                if (!networkInterface.isUp() || networkInterface.isLoopback() || 
                    networkInterface.isVirtual() || !networkInterface.supportsMulticast()) {
                    continue;
                }
                
                String interfaceName = networkInterface.getName().toLowerCase();
                String displayName = networkInterface.getDisplayName().toLowerCase();
                
                // Kiểm tra giao diện có phải WiFi không
                boolean isWifi = false;
                for (String pattern : wifiInterfaceIdentifiers) {
                    if (interfaceName.contains(pattern) || displayName.contains(pattern)) {
                        isWifi = true;
                        break;
                    }
                }
                
                // Trên Linux, đôi khi giao diện chỉ có tên như "wlan0" hoặc tương tự
                if (!isWifi && (interfaceName.matches("wlan\\d+") || displayName.matches("wlan\\d+"))) {
                    isWifi = true;
                }
                
                if (isWifi) {
                    // Lấy tất cả địa chỉ IP
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    
                    // Tìm địa chỉ IPv4
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress address = inetAddresses.nextElement();
                        
                        // Kiểm tra IPv4 và không phải loopback
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
            
            // Phương pháp 2: Tìm giao diện hoạt động
            // bằng cách chỉ kiểm tra các giao diện đang hoạt động và không phải loopback
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                // Bỏ qua giao diện tắt hoặc loopback
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                
                // Bỏ qua giao diện ảo
                if (networkInterface.isVirtual() || networkInterface.isPointToPoint()) {
                    continue;
                }
                
                // Lấy tất cả địa chỉ IP
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    
                    // Kiểm tra IPv4, không phải loopback, link local hay multicast
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() 
                        && !address.isLinkLocalAddress() && !address.isMulticastAddress()) {
                        // Đã tìm thấy địa chỉ phù hợp
                        String ipAddress = address.getHostAddress();
                        // Tránh trả về địa chỉ docker/VM
                        if (!(ipAddress.startsWith("192.168.") || ipAddress.startsWith("172.") || ipAddress.startsWith("10."))) {
                            return ipAddress;
                        } else {
                            // Dùng địa chỉ này làm dự phòng nếu không tìm được địa chỉ tốt hơn
                            ipAddress = address.getHostAddress();
                            return ipAddress;
                        }
                    }
                }
            }
            
            // Dự phòng: sử dụng localhost
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
            
        } catch (Exception e) {
            log.error("Không thể lấy địa chỉ IP Wi-Fi\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
            return "Không thể lấy địa chỉ IP";
        }
    }

    
    // Sao chép địa chỉ IP vào clipboard
    private void copyIPAddressToClipboard() {
        try {
            StringSelection stringSelection = new StringSelection(ipAddress);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);

            // Phản hồi trực quan
            SwingUtilities.invokeLater(() -> {
                try {
                    // Đổi màu nhấp nháy
                    ipAddressLabel.setForeground(ColorPalette.ACCENT);

                    // Trả lại màu sau 1 giây
                    Timer timer = new Timer(1000, evt -> {
                        try {
                            ipAddressLabel.setForeground(ColorPalette.PRIMARY);
                        } catch (Exception ex) {
                            log.error("Không thể reset màu label địa chỉ IP\nChi tiết lỗi: " + ex.getMessage());
                            log.error("Chi tiết lỗi", ex);
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                } catch (Exception ex) {
                    log.error("Không thể hiển thị phản hồi trực quan\nChi tiết lỗi: " + ex.getMessage());
                    log.error("Chi tiết lỗi", ex);
                }
            });
        } catch (Exception e) {
            log.error("Không thể sao chép địa chỉ IP vào clipboard\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }
    
    // Cập nhật địa chỉ IP hiển thị
    // @param newIpAddress địa chỉ IP mới
    public void updateIPAddress(String newIpAddress) {
        try {
            this.ipAddress = newIpAddress;
            this.ipAddressLabel.setText(newIpAddress);
        } catch (Exception e) {
            log.error("Không thể cập nhật địa chỉ IP\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    }
    
    // Lấy địa chỉ IP hiện tại
    // @return địa chỉ IP hiện tại
    public String getIpAddress() {
        try {
            return this.ipAddress;
        } catch (Exception e) {
            log.error("Không thể lấy địa chỉ IP\nChi tiết lỗi: " + e.getMessage());
            log.error("Chi tiết lỗi", e);
            return null;
        }
    }
}
