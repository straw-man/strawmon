package strawmon;

/**
 * To be implemented by classes that raise events.
 */
public interface StrawmonEventRaiser {
    // Register an event listener object.
	public void addListener(StrawmonEventListener listener);

    // Unregister an event listener object.
	public void removeListener(StrawmonEventListener listener);

    // Raise an event.
    protected void raiseEvent(StrawmonEvent event, String[] messages);
}