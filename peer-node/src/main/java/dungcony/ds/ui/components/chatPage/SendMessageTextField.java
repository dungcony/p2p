package dungcony.ds.ui.components.chatPage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/** 
 * Một lớp tùy chỉnh để tạo trường nhập liệu có placeholder
 */
public class SendMessageTextField extends JTextField implements FocusListener {
    /** Nội dung của placeholder */
    private String placeholder;
    /** Cờ báo hiệu placeholder đang hiển thị hay không */
    private boolean showingPlaceholder;
    /** Màu sắc của placeholder */
    private Color placeholderColor = Color.GRAY;
    private Color normalColor = Color.BLACK;
    /** Để biết liệu placeholder có bị vô hiệu hóa hay không */
    private boolean wasDisabled = false;

    public SendMessageTextField(String placeholder) {
        super();
        this.placeholder = placeholder;
        this.showingPlaceholder = true;

        // Thiết lập văn bản và kiểu dáng ban đầu cho placeholder
        super.setText(placeholder);
        setForeground(placeholderColor);

        // Thêm trình lắng nghe tiêu điểm để xử lý hành vi của placeholder
        addFocusListener(this);
    }

    public SendMessageTextField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder;
        this.showingPlaceholder = true;

        // Thiết lập văn bản và kiểu dáng ban đầu cho placeholder
        super.setText(placeholder);
        setForeground(placeholderColor);

        // Thêm trình lắng nghe tiêu điểm để xử lý hành vi của placeholder
        addFocusListener(this);
    }

    @Override
    public String getText() {
        try {
            return showingPlaceholder ? "" : super.getText();
        } catch (Exception e) {
            System.out.println("[ERROR] Lỗi khi lấy văn bản\nThông báo lỗi: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void setText(String text) {
        try {
            if (text == null || text.isEmpty()) {
                // Xóa văn bản hiện tại trước
                super.setText("");
                showingPlaceholder = false;
                setForeground(normalColor);
                
                // Nếu không có tiêu điểm và đang được bật, hiển thị placeholder
                if (!hasFocus() && isEnabled()) {
                    showPlaceholder();
                }
            } else {
                showingPlaceholder = false;
                super.setText(text);
                setForeground(normalColor);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Lỗi khi thiết lập văn bản\nThông báo lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Phương thức hiển thị placeholder
     */
    private void showPlaceholder() {
        try {
            // nếu không có gì trong placeholder
            if (super.getText().isEmpty()) {
                showingPlaceholder = true;
                super.setText(placeholder);
                setForeground(placeholderColor);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Lỗi khi hiển thị placeholder\nThông báo lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Phương thức ẩn placeholder */
    private void hidePlaceholder() {
        try {
            if (showingPlaceholder) {
                showingPlaceholder = false;
                super.setText("");
                setForeground(normalColor);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Lỗi khi ẩn placeholder\nThông báo lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        try {
            boolean wasEnabled = isEnabled();
            super.setEnabled(enabled);
            
            if (!wasEnabled && enabled) {
                // Vừa được bật lại
                wasDisabled = true;
                // Sử dụng SwingUtilities.invokeLater để đảm bảo điều này xảy ra sau tất cả các cập nhật UI khác
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (super.getText().isEmpty() && !hasFocus()) {
                            showPlaceholder();
                        }
                        wasDisabled = false;
                    } catch (Exception ex) {
                        System.out.println("[ERROR] Lỗi khi xử lý hiển thị placeholder sau khi bật lại\nThông báo lỗi: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
            } else if (wasEnabled && !enabled) {
                // Vừa bị tắt - không thay đổi trạng thái placeholder
                wasDisabled = true;
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Lỗi khi thiết lập trạng thái bật/tắt\nThông báo lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 
     * @return Lấy văn bản placeholder
     */
    public String getPlaceholder() {
        try {
            return placeholder;
        } catch (Exception e) {
            System.out.println("[ERROR] Lỗi khi lấy placeholder\nThông báo lỗi: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Thiết lập văn bản placeholder
     * @param placeholder Văn bản sẽ được đặt trong placeholder
     */
    public void setPlaceholder(String placeholder) {
        try {
            this.placeholder = placeholder;
            if (showingPlaceholder) {
                super.setText(placeholder);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể đặt placeholder\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 
     * @return Place holder color {@code Color}
     */
    public Color getPlaceholderColor() {
        try {
            return placeholderColor;
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể lấy màu placeholder\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
            return Color.GRAY;
        }
    }
    
    /**
     * 
     * @return Place holder color {@code Color}
     */
    public void setPlaceholderColor(Color placeholderColor) {
        try {
            this.placeholderColor = placeholderColor;
            if (showingPlaceholder) {
                setForeground(placeholderColor);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể đặt placeholder color\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        try {
            if (showingPlaceholder && isEnabled()) {
                hidePlaceholder();
            }
        } catch (Exception ex) {
            System.out.println("[ERROR] Không thể xử lý focus vào ô nhập\nChi tiết lỗi: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        try {
            if (super.getText().isEmpty() && isEnabled() && !wasDisabled) {
                showPlaceholder();
            }
        } catch (Exception ex) {
            System.out.println("[ERROR] Không thể xử lý rời focus khỏi ô nhập\nChi tiết lỗi: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Helper method to check if we're showing placeholder (for debugging)
    public boolean isShowingPlaceholder() {
        try {
            return showingPlaceholder;
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể kiểm tra trạng thái hiển thị placeholder\nChi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
