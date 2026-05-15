package dungcony.ds.ui.components;

import dungcony.ds.ui.utils.ColorPalette;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

/** Component nút bấm */
public class Button extends JButton {

    private boolean isActive = false;

    public Button(Icon icon) {
        super(icon);
        
        setFocusable(false);
        setBackground(Color.white);
        setForeground(ColorPalette.TEXT);

    }


    public void setIsActive(boolean isActive) {
        System.out.println("I came here");
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

