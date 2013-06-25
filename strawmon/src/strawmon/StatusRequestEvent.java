package strawmon;

public class StatusRequestEvent extends StrawmonEvent {
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
