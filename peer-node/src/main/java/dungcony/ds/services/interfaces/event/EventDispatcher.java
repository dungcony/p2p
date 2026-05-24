package dungcony.ds.services.interfaces.event;

public interface EventDispatcher {
    void dispatch(Runnable runnable);
}
