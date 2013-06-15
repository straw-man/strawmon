package statusbot;

public class StatusRequestEvent extends StatusbotEvent {
	private static final long serialVersionUID = 1;
	private String statusRequest;
	
	public StatusRequestEvent(Object source) {
		super(source);
	}
	
	public void setStatusRequest(String statusRequest) {
		this.statusRequest = statusRequest;
	}
	
	public String getStatusRequest() {
		return this.statusRequest;
	}
}
