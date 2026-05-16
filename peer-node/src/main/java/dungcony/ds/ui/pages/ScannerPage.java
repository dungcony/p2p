package dungcony.ds.ui.pages;

import dungcony.ds.App;
import dungcony.ds.model.PeerInfo;
import dungcony.ds.ui.components.addFriendPage.HeadingPanel;
import dungcony.ds.ui.components.scannerPage.FoundDevices;
import dungcony.ds.ui.components.scannerPage.ScanButton;
import dungcony.ds.ui.utils.ColorPalette;


import javax.swing.*;
import java.awt.*;

/** 
 * Trang quét các thiết bị gần đây trên cùng mạng
 * @see HeadingPanel
 * @see ScanButton
 * @see FoundDevices
 * @author Shoyeb Ansari
*/
public class ScannerPage extends JPanel {
    /** Panel tiêu đề */
    private HeadingPanel headingPanel;
    /** Nút quét */
    private ScanButton scanButton;
    /** Component hiển thị các thiết bị tìm thấy */
    private FoundDevices foundDevices;
    
    public ScannerPage() throws InterruptedException {
        headingPanel = new HeadingPanel("Quét peer gần đây");
        scanButton = new ScanButton();
        foundDevices = new FoundDevices();
        
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        // Đặt nền hiện đại
        this.setBackground(ColorPalette.BACKGROUND);
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
    
    private void setupLayout() {
        this.setLayout(new BorderLayout(0, 20));
        
        // Panel trên với tiêu đề và nút quét
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(ColorPalette.BACKGROUND);
        
        topPanel.add(headingPanel);
        topPanel.add(Box.createVerticalStrut(15));
        
        // Căn giữa nút quét
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(ColorPalette.BACKGROUND);
        buttonPanel.add(scanButton);
        topPanel.add(buttonPanel);
        
        // Thêm chức năng quét
        scanButton.addActionListener(e -> {
            try {
                scanNearbyUsers();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        });
        this.add(topPanel, BorderLayout.NORTH);
        this.add(foundDevices, BorderLayout.CENTER);
    }
    
    private void scanNearbyUsers() throws InterruptedException {
        System.out.println("[INFO] UI yêu cầu quét peer gần đây.");
        // Hiển thị loading ngay lập tức
        foundDevices.setLoadingPanel();
        
        SwingWorker<String[], Void> worker = new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() throws Exception {
                if (App.peerNode == null) {
                    System.out.println("[WARN] Bỏ qua quét vì App.peerNode đang null.");
                    return new String[0];
                }
                return App.peerNode.discoverPeersOnLocalNetwork().stream()
                        .map(PeerInfo::addressKey)
                        .toArray(String[]::new);
            }
            
            @Override
            protected void done() {
                try {
                    String[] foundDevicesList = get();
                    System.out.println("[INFO] UI quét xong. tìm thấy=" + foundDevicesList.length);
                    foundDevices.setFoundDevices(foundDevicesList);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Xử lý lỗi
                }
            }
        };
        
        worker.execute();
    }

}
