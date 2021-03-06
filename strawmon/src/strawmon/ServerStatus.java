package strawmon;

import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.IOException;
import java.io.BufferedReader;

/**
 * Handle all server interaction.
 */
public class ServerStatus implements StrawmonEventRaiser {
	// Event listeners
	protected ArrayList<StrawmonEventListener> listeners = new ArrayList<>();

	// Connection variables
	private Socket serverConnection;
	private InetSocketAddress serverAddress;
	private byte[] serverListPingRequest = new byte[]{(byte) 0xFE, (byte) 0x01};
	
	// Status variables
	private String gameVersion;
	private String motd;
	private int playersOnline;
	private int maxPlayers;
    //private int pingVersion;
    //private int protocolVersion;

	// Launch command
	private String launchCommand;
	
	// Restart variables
	private int maxDuration = 600000;

    /*
     * Constructor
     */
	public ServerStatus(String host, int port, String command, int maxDuration) {
		this.serverAddress = new InetSocketAddress(host, port);
		this.launchCommand = command;
		this.maxDuration = maxDuration;
	}

    /*
     * Connect, get server status, and disconnect; return false if the server is down.
     */
	public boolean loadServerStatus() {
		boolean status;

		this.connect();
		status = this.getServerListPing();
		this.disconnect();
		
		return status;
	}

    /*
     * Public access to the restart methods.
     */
    public boolean restart() {
        try {
            this.closeServer();
            return this.launchServer();
        } catch (Exception e) {
            System.out.println("Error restarting server: " + e.getMessage());
            return false;
        }
    }

    /*
     * Open a socket to the server.
     */
	private void connect() {
		this.serverConnection = new Socket();
		try {
			this.serverConnection.setSoTimeout(15000);
		} catch (Exception e) {
			System.out.println("Probably non-fatal: failed to set socket timeout: " + e.getMessage());
		}
		
		try {
			this.serverConnection.connect(this.serverAddress);
		} catch (Exception e) {
			System.out.println("probably fatal! couldn't connect to server. being nice about it for now, though: " + e.getMessage());
		}
	}

    /*
     * Close an existing socket to the server.
     */
	private void disconnect() {
		try {
			this.serverConnection.close();
		} catch (Exception e) {
			System.out.println("Non-fatal: failed to close socket: " + e.getMessage());
		}
	}

	/*
	 * Returns true and populates server stats from the Server List Ping request in the Minecraft protocol. Returns false if the server is down.
	 *
	 * The first _byte_ of a correct ServerListPing response is 0xFF (255)
	 * The next _character_ is the response length
	 * The remainder is a null-delimited _string_
	 * http://mc.kev009.com/Server_List_Ping
	*/
	private boolean getServerListPing() {
		InputStream inStream;
		DataOutputStream out;
		int firstByte;
		char[] response;
		
		try {
			inStream = this.serverConnection.getInputStream();
			out = new DataOutputStream(this.serverConnection.getOutputStream());
		} catch (Exception e) {
			System.out.println("Non-fatal: failed to open socket input/output stream, skipping this check: " + e.getMessage());
			return true;
		}
		InputStreamReader inStreamReader = new InputStreamReader(inStream, Charset.forName("UTF-16BE"));
		
		// Send the request to the server
		try {
			out.write(this.serverListPingRequest);
		} catch (Exception e) {
			System.out.println("Non-fatal: failed to write to socket output stream, skipping this check: " + e.getMessage());
			return true;
		}
			
		// Read the first byte of the response
		// generally where it fails
		try {
			firstByte = inStream.read();
		} catch (Exception e) {
			System.out.println("Possibly fatal: Failed to read socket input stream, double-checking: " + e.getMessage());
			try {
				this.disconnect();
				Thread.sleep(30000);
				this.connect();
				inStream = this.serverConnection.getInputStream();
				out = new DataOutputStream(this.serverConnection.getOutputStream());
				inStreamReader = new InputStreamReader(inStream, Charset.forName("UTF-16BE"));
				out.write(this.serverListPingRequest);
				firstByte = inStream.read();
			} catch (Exception f) {
				System.out.println("Double-check failed, starting reboot sequence: " + f.getMessage());
				return false;
			}
		}
			
		// An improper response is interpreted as the server being down
		if (firstByte != 0xFF) {
			return false;
		}
		
		// If the server is up, read the rest of the stream
		try {
			int responseLength = inStreamReader.read();
			response = new char[responseLength];
			inStreamReader.read(response, 0, responseLength);
		} catch (Exception e) {
			System.out.println("Non-fatal: failed to read contents of ServerListPing stream, skipping this check: " + e.getMessage());
			return true;
		}
		
		// Convert the resulting character array into a string, then split it along the field delimiters (null characters)
		String responseString = new String(response);
		String[] data = responseString.split("\0");
		
		// Set the class variables using the protocol semantics
		this.gameVersion = (data[2]);
		this.motd = (data[3]);
		this.playersOnline = (Integer.parseInt(data[4]));
		this.maxPlayers = (Integer.parseInt(data[5]));
        //this.pingVersion = (Integer.parseInt(data[0].substring(1)));
        //this.protocolVersion = (Integer.parseInt(data[1]));

		return true;
	}

    /*
     * Close a running server. Return true if the server was closed, or if no server is running.
     */
	private boolean closeServer() throws IOException {
		// Commands
		String findPid = "netstat -ntpl";
		String firstKill = "kill -HUP ";
		String secondKill = "kill -INT ";
		String thirdKill = "kill -KILL ";
		String pidCheck = "ps -p ";

		String pid = "";
		boolean closed = false;
		Process ps;
		
		Runtime runtime = Runtime.getRuntime();
		
		// Identify Minecraft server process
		Process processFindPid = runtime.exec(findPid);
		BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(processFindPid.getInputStream()));

		String nextLine;
		while ((nextLine = stdOutReader.readLine()) != null) {
			if (nextLine.contains(Integer.toString(serverAddress.getPort()))) {
				nextLine = nextLine.trim();
				pid = nextLine.substring(nextLine.lastIndexOf(" ") + 1, nextLine.lastIndexOf("/"));
			}
		}
		
		// No server running
		if (pid == null || pid.isEmpty()) {
			closed = true;
		}

		// Kill server process as gently as possible
		if (!closed) {
			this.raiseServerDownEvent(new String[]{"Attempting to stop server with SIGHUP..."});
			runtime = Runtime.getRuntime();
			runtime.exec(firstKill + pid);
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				
			}
			
			// Check if server is still running
			runtime = Runtime.getRuntime();
			ps = runtime.exec(pidCheck + pid);
			stdOutReader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			while ((nextLine = stdOutReader.readLine()) != null) {
				if (nextLine.contains(pid)) {
					closed = true;
					break;
				}
			}
		}
			
		if (!closed) {
			this.raiseServerDownEvent(new String[]{"SIGHUP failed, attempting to stop server with SIGINT..."});
			runtime = Runtime.getRuntime();
			runtime.exec(secondKill + pid);
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				
			}

			// Check if server is still running
			runtime = Runtime.getRuntime();
			ps = runtime.exec(pidCheck + pid);
			stdOutReader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			while ((nextLine = stdOutReader.readLine()) != null) {
				if (nextLine.contains(pid)) {
					closed = true;
					break;
				}
			}
		}
		
		if (!closed) {
			this.raiseServerDownEvent(new String[]{"SIGINT failed, definitely stopping server with SIGKILL..."});
			runtime = Runtime.getRuntime();
			runtime.exec(thirdKill + pid);
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				
			}
		}

        return closed;
	}

    /*
     * Launch the server. Call only after the server has been closed or no socket can be established (cold boot).
     */
	private boolean launchServer() {
		this.raiseServerDownEvent(new String[]{"Launching server"});

		System.out.println(this.launchCommand);
		
		// Restart server
		Runtime runtime = Runtime.getRuntime();
		try {
			System.out.println(this.launchCommand);
			Process launcher = runtime.exec(this.launchCommand);
			InputStreamReader in = new InputStreamReader(launcher.getInputStream());
			in.close();
			InputStreamReader err = new InputStreamReader(launcher.getErrorStream());
			err.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println("exception in exec launchServer command");
		}
		int attemptDuration = 0;
		while (true) {
			try {
				Thread.sleep(10000);
			} catch (Exception ignored) {
				
			}

			if (this.loadServerStatus()) {
				break;
			} else {
				attemptDuration += 10000;
				if (attemptDuration >= this.maxDuration) {
					return false;
				}
			}
		}
		
		return true;		
	}

    /*
     * Respond to a status request or command.
     */
	public String[] getStatus(String statusRequest) {
		switch (statusRequest) {
			case "gameversion":
				return new String[]{"Minecraft version " + this.gameVersion};
			case "motd":
				return new String[]{this.motd};
			case "playersonline":
				return new String[]{"Current/Max Players: " + this.playersOnline + "/" + this.maxPlayers};
			case "reboot":
                RobertsRules chatRebootManager = new RobertsRules();
				//try {
				//	this.closeServer();
				//} catch (Exception e) {
				//	
				//}
				
				return new String[]{"Chat-triggered reboot not yet implemented."};
				//if (this.playersOnline < 2) {
				//	return new String[]{"Low population: troll protection activated. Restarting in three minutes. !nay to abort."};
				//} else {
				//	return new String[]{"[name] has introduced a motion to closeServer the server. Is the motion seconded? (!second)"};
				//}
				
			default:
				return new String[]{"Invalid request"};
		}
	}

    /*
     * Implements addListener as required by interface StrawmonEventRaiser.
     */
	public void addListener(StrawmonEventListener listener){
		this.listeners.add(listener);
	}

    /*
     * Implements removeListener as required by interface StrawmonEventRaiser.
     */
	public void removeListener(StrawmonEventListener listener){
		this.listeners.remove(listener);
	}

    /*
     * Implements raiseEvent as required by interface StrawmonEventRaiser.
     */
    private void raiseServerDownEvent(String[] messages) {
		ServerDownEvent event = new ServerDownEvent(this);
		event.setMessages(messages);
		for (StrawmonEventListener listener : this.listeners) {
			listener.StrawmonEventRaised(event);
		}
	}
}