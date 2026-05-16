package dungcony.ds.interfaces;

public interface BootstrapSyncService {
    /**
     * Dang ky peer hien tai, join bootstrap, dong bo peer/group va offline message.
     */
    void registerAndJoinBootstrap();

    /**
     * Lam moi danh sach peer online va group tu bootstrap-server.
     */
    void refreshFromBootstrap();
}
