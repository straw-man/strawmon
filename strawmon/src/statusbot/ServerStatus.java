package statusbot;

import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.io.InputStream;
import java.util.ArrayList;
import java.io.IOException;
import java.io.BufferedReader;

public class ServerStatus implements StatusbotEventRaiser {
	// Event listeners
	protected ArrayList<StatusbotEventListener> listeners = new ArrayList<StatusbotEventListener>();

	// Connection variables
	private Socket serverConnection;
	private InetSocketAddress serverAddress;
	private byte[] serverListPingRequest = new byte[]{(byte) 0xFE, (byte) 0x01};
	
	// Status variables
	//private int pingVersion;
	//private int protocolVersion;
	private String gameVersion;
	private String motd;
	private int playersOnline;
	private int maxPlayers;
	
	// Launch command
	private String launchCommand;
	
	// Restart variables
	private int maxDuration = 600000;
	
	public ServerStatus(String host, int port, String command, int maxDuration) {
		this.serverAddress = new InetSocketAddress(host, port);
		this.launchCommand = command;
		this.maxDuration = maxDuration;
	}
	
	public boolean loadServerStatus() {
		boolean status = true;

		this.connect();
		status = this.getServerListPing();
		this.disconnect();
		
		return status;
	}
	
	private void connect() {
		this.serverConnection = new Socket();
		try {
			this.serverConnection.setSoTimeout(15000);
		} catch (Exception e) {
			System.out.println("(probably) Non-fatal: failed to set socket timeout: " + e.getMessage());
		}
		
		try {
			this.serverConnection.connect(this.serverAddress);
		} catch (Exception e) {
			System.out.println("probably fatal! couldn't connect to server. being nice about it for now, though: " + e.getMessage());
		}
	}
	
	private void disconnect() {
		try {
			this.serverConnection.close();
		} catch (Exception e) {
			System.out.println("Non-fatal: failed to close socket: " + e.getMessage());
		}
	}

	// Returns true if server is up, false otherwise
	public boolean getServerListPing() {
		/* 
		 * The first _byte_ of a correct ServerListPing response is 0xFF (255)
		 * The next _character_ is the response length
		 * The remainder is a null-delimited _string_
		 * http://mc.kev009.com/Server_List_Ping
		 */
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
		//this.pingVersion = (Integer.parseInt(data[0].substring(1)));
		//this.protocolVersion = (Integer.parseInt(data[1]));
		this.gameVersion = (data[2]);
		this.motd = (data[3]);
		this.playersOnline = (Integer.parseInt(data[4]));
		this.maxPlayers = (Integer.parseInt(data[5]));
		
		return true;
	}
	
	public boolean restart() throws IOException {
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
		
		return launch();
	}
	
	private boolean launch() {
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
			System.out.println("exception in exec launch command");
		}
		int attemptDuration = 0;
		while (true) {
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				
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
	
	public String[] getStatus(String statusRequest) {
		switch (statusRequest) {
			case "gameversion":
				return new String[]{"Minecraft version " + this.gameVersion};
			case "motd":
				return new String[]{this.motd};
			case "playersonline":
				return new String[]{"Current/Max Players: " + this.playersOnline + "/" + this.maxPlayers};
			case "reboot":
				//try {
				//	this.restart();
				//} catch (Exception e) {
				//	
				//}
				return new String[]{"Chat-triggered reboot not yet implemented."};
			default:
				return new String[]{"Invalid request"};
		}
	}

	public void addListener(StatusbotEventListener listener){
		this.listeners.add(listener);
	}
	
	public void removeListener(StatusbotEventListener listener){
		this.listeners.remove(listener);
	}

	private void raiseServerDownEvent(String[] messages) {
		ServerDownEvent event = new ServerDownEvent(this);
		event.setMessages(messages);
		for(StatusbotEventListener listener : this.listeners) {
			listener.StatusbotEventRaised(event);
		}
	}
}