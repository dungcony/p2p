package dungcony.ds.ui.components;

import dungcony.ds.ui.utils.ColorPalette;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Component thẻ hiển thị thông tin người dùng
public class ChatProfile extends RoundedPanel {
    // Biểu tượng tròn
    private CircularIcon icon;
    // Panel nội dung bên phải
    private JPanel contentPanel;
    // Cờ hover
    private boolean isHovered = false;
    // Cờ chọn
    private boolean isSelected = false;

    // @param contentPanel Panel bên phải của profile
    public ChatProfile(int radius, JPanel contentPanel, Color color) {
        super(radius, color);
        this.icon = new CircularIcon(FontIcon.of(FontAwesome.USER, 30));
        this.contentPanel = contentPanel;
        
        initializeComponents();
        setupLayout();
        setupInteractions();
    }
    
    // Khởi tạo các thành phần của ChatProfile
    private void initializeComponents() {
        // Thiết lập panel chính
        this.setBackground(ColorPalette.PANEL_BACKGROUND);
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        this.setPreferredSize(new Dimension(0, 70));
        
        // Thiết lập panel nội dung
        contentPanel.setBackground(ColorPalette.PANEL_BACKGROUND);
        contentPanel.setOpaque(false);
    }
    
    // Thiết lập bố cục của ChatProfile
    private void setupLayout() {
        this.setLayout(new BorderLayout(15, 0));
        this.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        
        this.add(icon, BorderLayout.WEST);
        this.add(contentPanel, BorderLayout.CENTER);
    }
    
    // Thiết lập các tương tác: hover, click...
    private void setupInteractions() {
        // Hiệu ứng hover
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                isSelected = !isSelected;
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Màu nền theo trạng thái
        Color bgColor;
        if (isSelected) {
            bgColor = ColorPalette.SECONDARY.brighter();
        } else if (isHovered) {
            bgColor = new Color(ColorPalette.INPUT_BACKGROUND.getRed(), 
                              ColorPalette.INPUT_BACKGROUND.getGreen(), 
                              ColorPalette.INPUT_BACKGROUND.getBlue(), 180);
        } else {
            bgColor = ColorPalette.PANEL_BACKGROUND;
        }
        
        // Vẽ nền bo tròn
        g2d.setColor(bgColor);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        
        // Vẽ viền nhẹ
        if (isSelected) {
            g2d.setColor(ColorPalette.PRIMARY);
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        } else if (isHovered) {
            g2d.setColor(new Color(ColorPalette.SECONDARY_TEXT.getRed(), 
                                 ColorPalette.SECONDARY_TEXT.getGreen(), 
                                 ColorPalette.SECONDARY_TEXT.getBlue(), 60));
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        }
        
        g2d.dispose();
        super.paintComponent(g);
    }
    
    // Đặt trạng thái chọn
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        repaint();
    }
    
    // Kiểm tra có đang được chọn không
    public boolean isSelected() {
        return isSelected;
    }
}
