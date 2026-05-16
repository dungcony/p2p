package dungcony.ds.ui;

import javax.swing.*;
import java.awt.*;

public class PeerPortDialog extends JDialog {
    private final JTextField portField;
    private final int bootstrapPort;
    private boolean confirmed;
    private int peerPort;

    /**
     * Tao dialog nhap port lang nghe cho profile moi.
     */
    public PeerPortDialog(int defaultPeerPort, int bootstrapPort) {
        this.bootstrapPort = bootstrapPort;
        this.peerPort = defaultPeerPort;
        this.portField = new JTextField(String.valueOf(defaultPeerPort), 20);

        setTitle("Peer Port");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridLayout(1, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        form.add(new JLabel("Peer port"));
        form.add(portField);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> confirm());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Kiem tra port hop le va khong trung cong bootstrap-server.
     */
    private void confirm() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port out of range");
            }
            if (port == bootstrapPort) {
                JOptionPane.showMessageDialog(this,
                        "Peer port cannot be the same as bootstrap port " + bootstrapPort + ".",
                        "Invalid peer port",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            this.peerPort = port;
            this.confirmed = true;
            System.out.println("[INFO] Peer port confirmed. port=" + peerPort);
            dispose();
        } catch (NumberFormatException e) {
            System.out.println("[WARN] Peer port rejected: " + portField.getText());
            JOptionPane.showMessageDialog(this,
                    "Port must be a number from 1 to 65535.",
                    "Invalid peer port",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Cho biet nguoi dung da bam Save hay huy dialog.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Lay peer port da xac nhan de luu vao profile.
     */
    public int getPeerPort() {
        return peerPort;
    }
}
