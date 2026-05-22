package dungcony.ds.ui.components.scannerPage;

import dungcony.ds.ui.components.ModernButton;
import dungcony.ds.ui.utils.ColorPalette;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// Nút quét cho trang scanner
public class ScanButton extends ModernButton {
    
    // Cờ cho biết đang quét hay không
    private boolean isScanning = false;
    // Icon quét
    private FontIcon scanIcon;
    // Icon loading
    private FontIcon loadingIcon;

    private Timer animationTimer;
    private int rotationAngle = 0;
    private List<ActionListener> scanListeners = new ArrayList<>();
    

    public ScanButton() {
        super("Quét thiết bị", ColorPalette.PRIMARY, ColorPalette.SECONDARY);
        initializeIcons();
        initializeButton();
        setupAnimationTimer();
        setupInteractions();
    }
    
    // Khởi tạo icon
    private void initializeIcons() {
        scanIcon = FontIcon.of(FontAwesome.SEARCH, 16);
        scanIcon.setIconColor(Color.WHITE);
        
        loadingIcon = FontIcon.of(FontAwesome.REFRESH, 16);
        loadingIcon.setIconColor(Color.WHITE);
    }
    
    // Khởi tạo nút
    private void initializeButton() {
        this.setText("Quét thiết bị");
        this.setFont(new Font("Segoe UI", Font.BOLD, 14));
        this.setForeground(Color.WHITE);
        this.setBackground(ColorPalette.PRIMARY);
        this.setPreferredSize(new Dimension(180, 45));
        this.setMinimumSize(new Dimension(180, 45));
        this.setMaximumSize(new Dimension(180, 45));
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.setFocusPainted(false);
        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setOpaque(false);
        setIcon(scanIcon);
    }
    
    private void setupAnimationTimer() {
        animationTimer = new Timer(50, e -> {
            if (isScanning) {
                rotationAngle += 15;
                if (rotationAngle >= 360) {
                    rotationAngle = 0;
                }
                repaint();
            }
        });
    }
    
    // Thiết lập tương tác chuột
    private void setupInteractions() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isScanning) {
                    startScanning();
                }
            }
        });
    }
    
    
    // Bắt đầu quét
    public void startScanning() {
        if (!isScanning) {
            isScanning = true;
            rotationAngle = 0;
            animationTimer.start();
            
            // Thông báo bắt đầu quét
            // fireScanStarted();
            
            // Tự dừng sau 3 giây
            Timer stopTimer = new Timer(3000, e -> stopScanning());
            stopTimer.setRepeats(false);
            stopTimer.start();
            
            repaint();
        }
    }
    
    public void stopScanning() {
        if (isScanning) {
            isScanning = false;
            animationTimer.stop();
            
            // Thông báo kết thúc quét
            // fireScanCompleted();
            
            repaint();
        }
    }
    
    // Lấy trạng thái quét
    // @return true nếu đang quét
    public boolean isScanning() {
        return isScanning;
    }
    
    // Xử lý sự kiện quét
    public void addScanListener(ActionListener listener) {
        scanListeners.add(listener);
    }
    
    public void removeScanListener(ActionListener listener) {
        scanListeners.remove(listener);
    }
}
