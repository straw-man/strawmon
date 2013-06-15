package statusbot;

public interface StatusbotEventRaiser {
	public void addListener(StatusbotEventListener listener);
	public void removeListener(StatusbotEventListener listener);
}
