package statusbot;

public class ServerDownEvent extends StatusbotEvent {
	private static final long serialVersionUID = 1;
	private String[] messages;
	
	public ServerDownEvent(Object source) {
		super(source);
	}
	
	public void setMessages(String[] messages) {
		this.messages = messages;
	}
	
	public String[] getMessages() {
		return this.messages;
	}
}
