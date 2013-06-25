package strawmon;

/**
 * To be implemented by classes that listen for events.
 */
public interface StrawmonEventListener {
    // Respond to an event raised by an object with this as a registered listener.
	public void StrawmonEventRaised(StrawmonEvent event);
}
