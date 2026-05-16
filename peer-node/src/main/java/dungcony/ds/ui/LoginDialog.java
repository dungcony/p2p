package dungcony.ds.ui;

import dungcony.ds.peer.PeerNode;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField idField;
    private final JTextField nameField;
    private final JTextField portField;
    private boolean confirmed;

    /**
     * Tạo dialog nhập tên peer và port lắng nghe trước khi khởi động PeerNode.
     */
    public LoginDialog() {
        this("peer-local", System.getProperty("user.name", "peer"), PeerNode.DEFAULT_PORT);
    }

    /**
     * Tạo dialog nhập peer với giá trị mặc định lấy từ cấu hình đã lưu.
     */
    public LoginDialog(String defaultPeerId, String defaultPeerName, int defaultPort) {
        this.idField = new JTextField(defaultPeerId == null || defaultPeerId.isBlank() ? "peer-local" : defaultPeerId, 20);
        this.nameField = new JTextField(defaultPeerName == null || defaultPeerName.isBlank() ? "peer" : defaultPeerName, 20);
        this.portField = new JTextField(String.valueOf(defaultPort), 20);
        setTitle("Start P2P Chat");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
//        form.add(new JLabel("Peer ID"));
//        form.add(idField);
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

    /**
     * Kiểm tra port hợp lệ và đóng dialog khi người dùng xác nhận.
     */
    private void confirm() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port out of range");
            }
            confirmed = true;
            System.out.println("[INFO] Login confirmed. peerId=" + getPeerId()
                    + ", peerName=" + getPeerName() + ", port=" + port);
            dispose();
        } catch (NumberFormatException e) {
            System.out.println("[WARN] Login rejected because port is invalid: " + portField.getText());
            JOptionPane.showMessageDialog(this, "Port must be a number from 1 to 65535.", "Invalid port", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Cho biết người dùng đã bấm Start hay đã hủy dialog.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Lấy id đăng ký ổn định của peer, dùng làm user_id trên bootstrap-server.
     */
    public String getPeerId() {
        String peerId = idField.getText();
        return peerId == null || peerId.isBlank() ? "peer-local" : peerId.trim();
    }

    /**
     * Lấy tên peer từ input, dùng giá trị mặc định nếu người dùng để trống.
     */
    public String getPeerName() {
        String name = nameField.getText();
        return name == null || name.isBlank() ? "peer" : name.trim();
    }

    /**
     * Lấy port đã nhập để PeerNode mở TCPServer.
     */
    public int getPeerPort() {
        return Integer.parseInt(portField.getText().trim());
    }
}
