package statusbot;

import java.util.ArrayList;

import org.jibble.pircbot.*;

public class IRCBot extends PircBot implements StatusbotEventRaiser {
	// Event listeners
	private ArrayList<StatusbotEventListener> listeners = new ArrayList<StatusbotEventListener>();
	
	public IRCBot(String name, String network, String channel) {
		this.setName(name);
		try {
			this.connect(network);
		} catch (Exception e) {
			// nick in use
		}
		this.joinChannel(channel);
	}
	
	/**
	 * Handle commands given by IRC users.
	 */
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
    	String command = message.substring(message.lastIndexOf("!") + 1);
    	// Help listing
    	if (command.equalsIgnoreCase("help")) {
    		this.outputMessages(new String[]{"Commands: !gameversion !motd !playersonline !reboot"});
    		//sendMessage(channel, "!pingversion");
    		//sendMessage(channel, "!protocolversion");
    	}

    	// Handle keywords
    	if (command.equalsIgnoreCase("gameversion")) {
    		this.raiseStatusRequestEvent("gameversion");
    	}
    	if (command.equalsIgnoreCase("motd")) {
    		this.raiseStatusRequestEvent("motd");    		
    	}
    	if (command.equalsIgnoreCase("playersonline")) {
    		this.raiseStatusRequestEvent("playersonline");    		
    	}
    	if (command.equalsIgnoreCase("reboot")) {
    		this.raiseStatusRequestEvent("reboot");    		
    	}
    }
    
    public void outputMessages(String[] messages) {
    	String[] channels = this.getChannels();
      	for(String channel : channels) {
    		for(String message : messages) {
    			this.sendMessage(channel, message);
    		}
    	}
    }
    
	public void addListener(StatusbotEventListener listener){
		this.listeners.add(listener);
	}
	
	public void removeListener(StatusbotEventListener listener){
		this.listeners.remove(listener);
	}
	
	private void raiseStatusRequestEvent(String statusRequest) {
		StatusRequestEvent event = new StatusRequestEvent(this);
		event.setStatusRequest(statusRequest);
		for(StatusbotEventListener listener : this.listeners) {
			listener.StatusbotEventRaised(event);
		}
	}
}
