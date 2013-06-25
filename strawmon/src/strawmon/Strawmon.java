package strawmon;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;

/**
 * Singleton that loops to monitor the target server and routes events between component objects.
 */
public class Strawmon implements StrawmonEventListener {
	private ServerStatus server;
	private IRCBot talker;
    private RobertsRules voter;

	private boolean serverUp = true;
	private int delay = 30000;

    /*
     * Constructor
     */
	public Strawmon() {
		// Configuration variables
		Properties config = new Properties();
		InputStream configFile;

        // Load configuration file

        // Set this to autogen the config file and die asking the user to config, but package a valid (or intentionally invalid) config with the jar

		try {
			configFile = new FileInputStream("config.txt");
			config.load(configFile);
		} catch (Exception e) {
			System.out.println("Configuration file not found");
			System.exit(0);
		}

        // Load the status check interval
		this.delay = Integer.parseInt(config.getProperty("DELAY"));
		
		// Load server status object
		this.server = new ServerStatus(config.getProperty("SERVER_ADDRESS"), Integer.parseInt(config.getProperty("SERVER_PORT")), config.getProperty("SERVER_LAUNCH_COMMAND"), Integer.parseInt(config.getProperty("MAX_DURATION")));
		this.server.addListener(this);

        // Load the vote handler
        this.voter = new RobertsRules();
        this.voter.addListener(this);

		// Load and connect IRC bot
		this.talker = new IRCBot(config.getProperty("IRC_NICK"), config.getProperty("IRC_NET"), config.getProperty("IRC_CHANNEL"));
		this.talker.addListener(this);

		// Wait for IRC server response
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			
		}

        // Introduce on IRC
		this.talker.outputMessages(new String[]{"StrawMon v0.4alpha joined. Say !help for commands."});

        // Loop on server status
		this.watchStatus();
	}

    /*
     * Check server status every this.delay milliseconds and trigger a reboot if the server isn't up or crashes.
     */
	public void watchStatus() {
		while(true) {
			try {
				// Update status variables or get false if server's down
				this.serverUp = this.server.loadServerStatus();

                // Tell the component ServerStatus object to restart the server process
				if (!this.serverUp){
					boolean rebootSuccessful = true;
					this.talker.outputMessages(new String[]{"Server unavailable or crashed. Restarting."});
					Thread.sleep(10000);
					rebootSuccessful = this.server.closeServer();

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

    /*
     * Implements StrawmonEventRaised as required by interface StrawmonEventListener.
     */
	public void StrawmonEventRaised(StrawmonEvent event) {
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
