package strawmon;

public class OutgoingMessageEvent extends StrawmonEvent {
	private static final long serialVersionUID = 1;
	private String[] messages;
	
	public OutgoingMessageEvent(StrawmonEvent source) {
		super(source);
	}
	
	public void setMessages(String[] messages) {
		this.messages = messages;
	}
	
	public String[] getMessages() {
		return this.messages;
	}
}
