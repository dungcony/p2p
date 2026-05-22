package dungcony.ds.ui.components.navBar;

import lombok.extern.slf4j.Slf4j;



import dungcony.ds.ui.components.Button;
import dungcony.ds.ui.router.RouterManager;
import dungcony.ds.ui.utils.ColorPalette;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;

// Thanh điều hướng với các nút chuyển trang
// @author Shoyeb Ansari
@Slf4j
public class NavBar extends JPanel {
// Các nút
    // Nút menu
    private Button menuButton;  
    // Nút chat
    private Button chatButton;
    // Nút mở chat trực tiếp
    private Button addFriendButton;
    // Cờ cho biết thanh điều hướng có đang mở rộng không
    private boolean isExpanded = false;

    
    public NavBar() {
        // Khởi tạo các nút với icon
        initialiseButtons();   
        
        // Bố cục dọc
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Đặt vị trí
        this.setBounds(0, 0, 200, getHeight());
        this.setBackground(Color.white);

        // Thêm các nút
        addButtons();
    }

    // Thêm các nút vào thanh điều hướng
    private void addButtons() {
        add(menuButton);
        add(Box.createRigidArea(new Dimension(0, 20)));  // thêm khoảng cách giữa menu và các nút còn lại
        add(chatButton);
        add(addFriendButton);
        add(Box.createVerticalGlue());
    }

    // Khởi tạo các nút với icon, kích thước và tooltip
    private void initialiseButtons() {
        try {
            // Đặt icon
            FontIcon icon = FontIcon.of(FontAwesome.LIST, 24, ColorPalette.PRIMARY);  
    
            menuButton = new Button(icon);  
            icon = FontIcon.of(FontAwesome.COMMENTS, 24, ColorPalette.PRIMARY);  
            chatButton = new Button(icon);
            icon = FontIcon.of(FontAwesome.USER_PLUS, 24, ColorPalette.PRIMARY);
            addFriendButton = new Button(icon);
            
            // Thêm sự kiện click
            menuButton.addActionListener(e -> toggleNav());   // thêm logic mở rộng thanh điều hướng
            chatButton.addActionListener(e -> openChatsPage());
            addFriendButton.addActionListener(e -> openAddFriendPage());
    
            // Đặt tooltip
            menuButton.setToolTipText("Mở điều hướng");
            addFriendButton.setToolTipText("Chat trực tiếp");
            chatButton.setToolTipText("Tin nhắn");        
    
            // Đặt padding cho các nút
            menuButton.setBorder(BorderFactory.createEmptyBorder(10, 5, 8, 12));
            chatButton.setBorder(BorderFactory.createEmptyBorder(10, 5, 8, 15));
            addFriendButton.setBorder(BorderFactory.createEmptyBorder(10, 5, 8, 11));
    
    
            Button[] buttons = {menuButton, chatButton, addFriendButton};
    
            for (Button button: buttons) {
                button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                button.setHorizontalAlignment(JButton.LEFT);
                button.setIconTextGap(8);
            }
        } catch (Exception e) {
            log.error("Không thể khởi tạo nút trên thanh điều hướng\nChi tiết lỗi: {}", e.getMessage());
            log.error("Chi tiết lỗi", e);
        }
    } 

    // Thêm tiêu đề cho các nút khi mở rộng
    public void setTitleToButtons() {
        chatButton.setText("Tin nhắn");
        addFriendButton.setText("Chat trực tiếp");
    }
    
    // Xóa tiêu đề các nút khi thu gọn
    public void removeTitleToButtons() {
        chatButton.setText("");
        addFriendButton.setText("");
    }

    // Mở trang chat
    private void openChatsPage() {
        RouterManager.getInstance().navigateTo("chats");
    }
    
    // Mở trang thêm bạn bè
    private void openAddFriendPage() {
        RouterManager.getInstance().navigateTo("addFriend");
    }

    // Đóng/mở rộng thanh điều hướng
    private void toggleNav() {
        isExpanded = !isExpanded;

        if (isExpanded) {
            setTitleToButtons();
        } else {
            removeTitleToButtons();
        }

        this.repaint();
        this.revalidate();
    }
}
