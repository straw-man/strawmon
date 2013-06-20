package statusbot;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;

public class Strawmon implements StatusbotEventListener {
	private ServerStatus server;
	private IRCBot talker;
	private boolean serverUp = true;
	private int delay = 30000;
	
	public Strawmon() {
		// Load configuration
		Properties config = new Properties();
		InputStream configFile;
		try {
			configFile = new FileInputStream("config.txt");
			config.load(configFile);
		} catch (Exception e) {
			System.out.println("config file not found");
			System.exit(0);
		}
		this.delay = Integer.parseInt(config.getProperty("DELAY"));
		
		// Load server status object
		this.server = new ServerStatus(config.getProperty("SERVER_ADDRESS"), Integer.parseInt(config.getProperty("SERVER_PORT")), config.getProperty("SERVER_LAUNCH_COMMAND"), Integer.parseInt(config.getProperty("MAX_DURATION")));
		this.server.addListener(this);
		
		// Load and connect IRC bot
		this.talker = new IRCBot(config.getProperty("IRC_NICK"), config.getProperty("IRC_NET"), config.getProperty("IRC_CHANNEL"));
		this.talker.addListener(this);
		
		// Wait for IRC server response
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			
		}

		this.talker.outputMessages(new String[]{"StrawMon v0.4alpha joined. Say !help for commands."});
		
		this.watchStatus();
	}
	
	public void watchStatus() {
		// Loop on server status
		while(true) {
			try {
				// Check if server is currently up; if so, update status variables
				this.serverUp = this.server.loadServerStatus();
				
				if (!this.serverUp){
					// Server down: take action
					boolean rebootSuccessful = true;
					this.talker.outputMessages(new String[]{"Server unavailable or crashed. Restarting."});
					Thread.sleep(10000);
					rebootSuccessful = this.server.restart();

					// If reboot fails, request human intervention and exit
					if (rebootSuccessful) {
						this.talker.outputMessages(new String[]{"Server restarted."});
					} else {
						this.talker.outputMessages(new String[]{"Server reboot failed. Human intervention required. Exiting."});
						System.exit(0);
					}
				}
				
				// Wait the configured duration for the next status check
				Thread.sleep(this.delay);
			} catch (Exception e) {
				
			}
		}
	}
	
	public void StatusbotEventRaised(StatusbotEvent event) {
		if (event instanceof StatusRequestEvent) {
			// Request status from ServerStatus and instruct IRCBot to respond
			StatusRequestEvent cast = (StatusRequestEvent) event;
			this.talker.outputMessages(this.server.getStatus(cast.getStatusRequest()));
		} else if (event instanceof ServerDownEvent) {
			// Instruct IRCBot to output messages raised by ServerStatus
			ServerDownEvent cast = (ServerDownEvent) event;
			this.talker.outputMessages(cast.getMessages());
		}
	}
}
