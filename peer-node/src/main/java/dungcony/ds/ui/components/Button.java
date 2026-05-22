package dungcony.ds.ui.components;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

// Component nút bấm
public class Button extends JButton {

    private static final Logger LOGGER = LoggerFactory.getLogger(Button.class);

    private boolean isActive = false;

    public Button(Icon icon) {
        super(icon);
        
        setFocusable(false);
        setBackground(Color.white);
        setForeground(ColorPalette.TEXT);

    }


    public void setIsActive(boolean isActive) {
        LOGGER.debug("Đã cập nhật trạng thái active của nút: " + isActive);
        this.isActive = isActive;
        if (isActive) {
            this.setBorder(new MatteBorder(0, 0, 0, 2, ColorPalette.PRIMARY));
        } else {
            this.setBorder(BorderFactory.createEmptyBorder());
        }

        revalidate();
    }
    
    public boolean getIsActive() {
        return isActive;
    }
}

