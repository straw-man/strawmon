package strawmon;

import java.util.ArrayList;

import org.jibble.pircbot.*;

/**
 * Handle all IRC interaction.
 */
public class IRCBot extends PircBot implements StrawmonEventRaiser {
	// Event listeners
	private ArrayList<StrawmonEventListener> listeners = new ArrayList<StrawmonEventListener>();
	
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
    		this.outputMessages(new String[]{"Commands: !gameversion !motd !playersonline !reboot !kill"});
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
    	if (command.equalsIgnoreCase("kill")) {
    		System.exit(0);    		
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
    
	public void addListener(StrawmonEventListener listener){
		this.listeners.add(listener);
	}
	
	public void removeListener(StrawmonEventListener listener){
		this.listeners.remove(listener);
	}
	
	private void raiseStatusRequestEvent(String statusRequest) {
		StatusRequestEvent event = new StatusRequestEvent(this);
		event.setStatusRequest(statusRequest);
		for (StrawmonEventListener listener : this.listeners) {
			listener.StrawmonEventRaised(event);
		}
	}
}
