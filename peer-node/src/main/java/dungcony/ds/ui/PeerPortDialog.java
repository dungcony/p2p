package dungcony.ds.ui;

import lombok.extern.slf4j.Slf4j;


import javax.swing.*;
import java.awt.*;

/**
 * Dialog chọn cổng TCP cho profile peer mới
 */
@Slf4j
public class PeerPortDialog extends JDialog {
private final JTextField portField;
    private final int bootstrapPort;
    private boolean confirmed;
    private int peerPort;

    // Tạo dialog nhập port lắng nghe cho profile mới
    public PeerPortDialog(int defaultPeerPort, int bootstrapPort) {
        this.bootstrapPort = bootstrapPort;
        this.peerPort = defaultPeerPort;
        this.portField = new JTextField(String.valueOf(defaultPeerPort), 20);

        setTitle("Cổng peer");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridLayout(1, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        form.add(new JLabel("Cổng peer"));
        form.add(portField);

        JButton saveButton = new JButton("Lưu");
        saveButton.addActionListener(e -> confirm());
        JButton cancelButton = new JButton("Hủy");
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

    // Kiểm tra port hợp lệ và không trùng cổng bootstrap-server
    private void confirm() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port out of range");
            }
            if (port == bootstrapPort) {
                JOptionPane.showMessageDialog(this,
                        "Cổng peer không được trùng với cổng bootstrap " + bootstrapPort + ".",
                        "Cổng peer không hợp lệ",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            this.peerPort = port;
            this.confirmed = true;
            log.info("Đã xác nhận cổng peer. cổng={}", peerPort);
            dispose();
        } catch (NumberFormatException e) {
            log.warn("Đã từ chối cổng peer: {}", portField.getText());
            JOptionPane.showMessageDialog(this,
                    "Cổng phải là số từ 1 đến 65535.",
                    "Cổng peer không hợp lệ",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Cho biết người dùng đã bấm Save hay hủy dialog
    public boolean isConfirmed() {
        return confirmed;
    }

    // Lấy peer port đã xác nhận để lưu vào profile
    public int getPeerPort() {
        return peerPort;
    }
}
