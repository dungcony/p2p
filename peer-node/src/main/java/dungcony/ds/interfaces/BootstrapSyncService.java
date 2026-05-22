package dungcony.ds.interfaces;

public interface BootstrapSyncService {
    // Đăng ký peer hiện tại, join bootstrap, đồng bộ peer/group và offline message.
    void registerAndJoinBootstrap();

    // Làm mới danh sách peer online và group từ bootstrap-server.
    void refreshFromBootstrap();
}
