package dungcony.ds.ui.utils;

import dungcony.ds.ui.components.ModernButton;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// Dialog hiện đại - Triển khai JOptionPane tùy chỉnh với giao diện đẹp
// Hỗ trợ các loại dialog: thông báo, cảnh báo, lỗi và xác nhận
public class Dialog {
    // Các loại tin nhắn
    public static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
    public static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
    public static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
    public static final int QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE;
    
    // Các loại nút
    public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
    public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
    public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;
    
    // Giá trị trả về của nút
    public static final int YES_OPTION = JOptionPane.YES_OPTION;
    public static final int NO_OPTION = JOptionPane.NO_OPTION;
    public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    public static final int OK_OPTION = JOptionPane.OK_OPTION;
    public static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;
    
    // Hiển thị dialog thông báo
    public static void showMessageDialog(Component parentComponent, Object message, 
                                         String title, int messageType) {
        JDialog dialog = createStyledDialog(parentComponent, message, title, messageType, 
                                            JOptionPane.DEFAULT_OPTION, null);
        dialog.setVisible(true);
    }
    
    // Hiển thị dialog xác nhận
    public static int showConfirmDialog(Component parentComponent, Object message,
                                       String title, int optionType, int messageType) {
        JDialog dialog = createStyledDialog(parentComponent, message, title, messageType, 
                                           optionType, null);
        
        // Lấy giá trị trả về từ option pane
        dialog.setVisible(true);
        JOptionPane optionPane = (JOptionPane) dialog.getContentPane().getComponent(0);
        Object selectedValue = optionPane.getValue();
        
        if (selectedValue == null)
            return CLOSED_OPTION;
        
        if (selectedValue instanceof Integer)
            return ((Integer) selectedValue).intValue();
            
        return CLOSED_OPTION;
    }
    
    // Hiển thị dialog nhập liệu
    public static String showInputDialog(Component parentComponent, Object message,
                                        String title, int messageType) {
        JTextField textField = new JTextField(20);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(new CompoundBorder(
            textField.getBorder(),
            new EmptyBorder(5, 5, 5, 5)));
        
        JDialog dialog = createStyledDialog(parentComponent, 
                                          createInputPanel(message, textField), 
                                          title, messageType, 
                                          JOptionPane.OK_CANCEL_OPTION, textField);
        
        dialog.setVisible(true);
        
        JOptionPane optionPane = (JOptionPane) dialog.getContentPane().getComponent(0);
        Object selectedValue = optionPane.getValue();
        
        if (selectedValue != null && ((Integer) selectedValue).intValue() == OK_OPTION) {
            return textField.getText();
        }
        
        return null;
    }
    
    // Tạo panel nhập liệu
    private static JPanel createInputPanel(Object message, JTextField textField) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setOpaque(false);
        
        if (message instanceof Component) {
            panel.add((Component) message, BorderLayout.NORTH);
        } else {
            JLabel messageLabel = new JLabel(message.toString());
            messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageLabel.setForeground(ColorPalette.TEXT);
            panel.add(messageLabel, BorderLayout.NORTH);
        }
        
        panel.add(textField, BorderLayout.CENTER);
        return panel;
    }
    
    // Tạo dialog có giao diện tùy chỉnh
    private static JDialog createStyledDialog(Component parentComponent, Object message,
                                             String title, int messageType, int optionType,
                                             JComponent focusComponent) {
        // Tạo option pane
        JOptionPane optionPane = new JOptionPane(message, messageType, optionType);
        
        // Tùy chỉnh các nút
        customizeButtons(optionPane);
        
        // Tạo dialog
        JDialog dialog = optionPane.createDialog(parentComponent, title);
        
        // Đặt màu nền
        dialog.setBackground(ColorPalette.BACKGROUND);
        
        // Trang trí nội dung dialog
        Container contentPane = dialog.getContentPane();
        if (contentPane instanceof JComponent) {
            JComponent cp = (JComponent) contentPane;
            cp.setBackground(ColorPalette.BACKGROUND);
            cp.setBorder(new EmptyBorder(15, 15, 15, 15));
        }
        
        // Trang trí phần tin nhắn
        customizeMessageArea(optionPane, messageType);
        
        // Đặt biểu tượng
        setCustomIcon(optionPane, messageType);
        
        // Focus vào ô nhập liệu nếu có
        if (focusComponent != null) {
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    focusComponent.requestFocusInWindow();
                }
            });
        }
        
        dialog.pack();
        centerDialog(dialog, parentComponent);
        
        return dialog;
    }
    
    // Thay thế các nút mặc định bằng ModernButton
    private static void customizeButtons(JOptionPane optionPane) {
        // Lấy panel các nút
        Component[] components = optionPane.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Component[] panelComps = panel.getComponents();
                
                for (Component panelComp : panelComps) {
                    if (panelComp instanceof JPanel) {
                        JPanel buttonPanel = (JPanel) panelComp;
                        Component[] buttons = buttonPanel.getComponents();
                        
                        // Thay từng nút bằng ModernButton
                        for (int i = 0; i < buttons.length; i++) {
                            if (buttons[i] instanceof JButton) {
                                JButton oldButton = (JButton) buttons[i];
                                ModernButton newButton = new ModernButton(oldButton.getText(), ColorPalette.PRIMARY, ColorPalette.SECONDARY);
                                newButton.setPreferredSize(new Dimension(100, 36));
                                
                                // Sao chép các sự kiện
                                for (ActionListener listener : oldButton.getActionListeners()) {
                                    newButton.addActionListener(listener);
                                }
                                
                                // Thêm khoảng cách
                                buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
                                
                                // Thay thế nút
                                buttonPanel.remove(oldButton);
                                buttonPanel.add(newButton, i);
                                buttonPanel.revalidate();
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Đặt biểu tượng theo loại dialog
    private static void setCustomIcon(JOptionPane optionPane, int messageType) {
        FontIcon icon;
        
        switch (messageType) {
            case INFORMATION_MESSAGE:
                icon = FontIcon.of(FontAwesome.INFO_CIRCLE, 32);
                icon.setIconColor(new Color(0, 120, 212)); // Info blue
                break;
            case WARNING_MESSAGE:
                icon = FontIcon.of(FontAwesome.EXCLAMATION_TRIANGLE, 32);
                icon.setIconColor(new Color(255, 186, 8)); // Warning yellow
                break;
            case ERROR_MESSAGE:
                icon = FontIcon.of(FontAwesome.TIMES_CIRCLE, 32);
                icon.setIconColor(new Color(232, 17, 35)); // Error red
                break;
            case QUESTION_MESSAGE:
                icon = FontIcon.of(FontAwesome.QUESTION_CIRCLE, 32);
                icon.setIconColor(new Color(0, 120, 212)); // Question blue
                break;
            default:
                return;
        }
        
        optionPane.setIcon(icon);
    }
    
    // Trang trí vùng tin nhắn
    private static void customizeMessageArea(JOptionPane optionPane, int messageType) {
        Component[] components = optionPane.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                panel.setOpaque(false);
                panel.setBackground(ColorPalette.BACKGROUND);
                
                Component[] panelComps = panel.getComponents();
                for (Component panelComp : panelComps) {
                    if (panelComp instanceof JLabel) {
                        JLabel label = (JLabel) panelComp;
                        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                        label.setForeground(ColorPalette.TEXT);
                    }
                    
                    if (panelComp instanceof JPanel) {
                        ((JPanel) panelComp).setOpaque(false);
                        ((JPanel) panelComp).setBackground(ColorPalette.BACKGROUND);
                    }
                }
            }
        }
    }
    
    // Căn giữa dialog
    private static void centerDialog(JDialog dialog, Component parent) {
        if (parent == null || !parent.isShowing()) {
            // Căn giữa màn hình
            dialog.setLocationRelativeTo(null);
        } else {
            // Căn giữa trên component cha
            Point parentLocation = parent.getLocationOnScreen();
            Dimension parentSize = parent.getSize();
            Dimension dialogSize = dialog.getSize();
            
            int x = parentLocation.x + (parentSize.width - dialogSize.width) / 2;
            int y = parentLocation.y + (parentSize.height - dialogSize.height) / 2;
            
            dialog.setLocation(x, y);
        }
    }
}
