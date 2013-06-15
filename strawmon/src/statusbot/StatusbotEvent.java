package statusbot;

import java.util.EventObject;

abstract public class StatusbotEvent extends EventObject {
	private static final long serialVersionUID = 1;
	
	public StatusbotEvent(Object source) {
		super(source);
	}
	
	public Object getSource() {
		return this.source;
	}
}
