package dungcony.ds.ui.pages;

import dungcony.ds.ui.components.navBar.NavBar;

import javax.swing.*;
import java.awt.*;

// Trang điều hướng bên trái của ứng dụng
// @author Shoyeb Ansari
public class NavigationPage extends JLayeredPane {
    // Thanh điều hướng
    private NavBar navBar;

    public NavigationPage() {
        // Khởi tạo NavBar
        navBar = new NavBar();
        
        // Thiết lập layout cho JLayeredPane
        this.setLayout(new BorderLayout());
        this.setBackground(Color.white);

        // Thêm navbar vào panel
        this.add(navBar, BorderLayout.WEST, Integer.valueOf(2));
    }
}
