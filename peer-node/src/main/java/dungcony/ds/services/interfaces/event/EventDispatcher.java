package dungcony.ds.services.interfaces.event;

/**
 * Abstraction để phát event lên thread xử lý phù hợp
 */
public interface EventDispatcher {
    void dispatch(Runnable runnable);
}
