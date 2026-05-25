package dungcony.ds.services.impl.event;

import dungcony.ds.services.interfaces.event.EventDispatcher;

import javax.swing.SwingUtilities;

/**
 * Event dispatcher đưa callback UI về Swing event dispatch thread
 */
public class SwingEventDispatcher implements EventDispatcher {
    @Override
    public void dispatch(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
