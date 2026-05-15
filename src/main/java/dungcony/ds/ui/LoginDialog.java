package dungcony.ds.ui;

import dungcony.ds.peer.PeerNode;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField nameField = new JTextField(System.getProperty("user.name", "peer"), 20);
    private final JTextField portField = new JTextField(String.valueOf(PeerNode.DEFAULT_PORT), 20);
    private boolean confirmed;

    public LoginDialog() {
        setTitle("Start P2P Chat");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        form.add(new JLabel("Peer name"));
        form.add(nameField);
        form.add(new JLabel("Listen port"));
        form.add(portField);

        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> confirm());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(startButton);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private void confirm() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port out of range");
            }
            confirmed = true;
            dispose();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port must be a number from 1 to 65535.", "Invalid port", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getPeerName() {
        String name = nameField.getText();
        return name == null || name.isBlank() ? "peer" : name.trim();
    }

    public int getPeerPort() {
        return Integer.parseInt(portField.getText().trim());
    }
}
