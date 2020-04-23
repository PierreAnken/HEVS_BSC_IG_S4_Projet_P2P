package p2p_common;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;

public abstract class AppSkeleton{
	
	private InetAddress listenerIP = null;
	private int listenerPort;	
	private ConfigFile configFile;
	private int serverPort;
	private ComLink comLinkWithServer;
	private String uniqueId;
	private Scanner scanner = new Scanner(System.in);
	private Vector<ComLink> comLinkList = new Vector<ComLink>();
	private Vector<OnlineClient> clientList = new Vector<OnlineClient>();
	private boolean isServerConfig;
	
	public AppSkeleton(ConfigFile configFile, boolean isServerConfig) {
		
		this.isServerConfig = isServerConfig;
		this.configFile = configFile;
		
		//load configuration file
		configFile.load();
		
		//start the logger
		new LoggerP2P("loggerP2P",configFile.getParam("DebugMode").toLowerCase().equals("true"));
		
		//generate unique to application and save it
		setUniqueId(configFile.getParam("UniqueId"));
		
		
		//check parameters from config file for listener
		try {
			
			listenerIP = InetAddress.getByName(configFile.getParam("ListenerIP"));
			listenerPort = Integer.parseInt(configFile.getParam("ListenerPort"));
			
		} catch (UnknownHostException e) {
			LoggerP2P.logMsg(e.getMessage(),Level.SEVERE, getClass(), "constructor");
			new FatalError("Invalid format for ListenerIP in config file");
		}catch (NumberFormatException e) {
			LoggerP2P.logMsg(e.getMessage(),Level.SEVERE, getClass(), "constructor");
			new FatalError("Invalid format for ListenerPort in config file");
		}
		
		//try to start listener
		try {
			
			ServerSocket socket = new ServerSocket(listenerPort,9999,listenerIP);
    		new Thread(new ConnectionListener(socket, this)).start();
    		
		} catch (IllegalArgumentException e) {
			LoggerP2P.logMsg(e.getMessage(),Level.SEVERE, getClass(), "constructor");
			new FatalError("Invalid ListenerPort number in config file");
		} catch (IOException e) {
			LoggerP2P.logMsg(e.getMessage(),Level.SEVERE, getClass(), "constructor");
			new FatalError("Unable to start listener on "+listenerIP.getHostAddress()+":"+listenerPort+"\n"+e.getMessage());
		}
		
		//create console Listener
		new Thread(new ConsoleListener(this,scanner)).start();
		
		//if client 
		if(!isServerConfig) {
			//try to connect to server and create communication thread
			try {

				//create ComLink with server
				serverPort = Integer.parseInt(configFile.getParam("ServerPort"));
				String ip = configFile.getParam("ServerIP");
				
				//check if port from listener != port from server
				if(ip.equals(configFile.getParam("ListenerIP")) && serverPort == listenerPort) {
					LoggerP2P.logMsg("Server and Listener share same IP/Port", getClass(), "constructor");

					new FatalError("Error in config file. Server and client can't share the same ip and port.");

				}
				
				
				comLinkWithServer = new ComLink(this,new Socket(ip, serverPort));
				//new Thread(comLinkWithServer).start();

				
			}catch (NumberFormatException e) {
				LoggerP2P.logMsg(e.getMessage(),Level.SEVERE, getClass(), "constructor");
				new FatalError("Invalid format for ServerPort in config file");
			}
			catch (UnknownHostException e) {
				LoggerP2P.logMsg(e.getMessage(),Level.SEVERE, getClass(), "constructor");
				new FatalError("Invalid format for ServerIP in config file");
			} catch (IOException e) {
				LoggerP2P.logMsg(e.getMessage(),Level.SEVERE, getClass(), "constructor");
				new FatalError("Unable to connect to server "+configFile.getParam("ServerIP")+":"+serverPort+"\n"+e.getMessage());
			}
			

			//delete any temporary files at startup
			File sharedFolder = new File(AppSkeleton.getUserDir()+"\\shared");
			if(sharedFolder.exists())
				if(sharedFolder.isDirectory())
					for(File file : sharedFolder.listFiles())
						if(file.getName().toLowerCase().endsWith(".tmp"))
							file.delete();
		}
			
		LoggerP2P.logMsg("Application started", getClass(), "constructor");
	}
	
	public boolean isServer() {
		return isServerConfig;
	}
	
	public Vector<ComLink> getComLinkList() {
		return comLinkList;
	}
	
	public ComLink getComLinkFromId(String Id) {
		getComLinkList();
		for(ComLink comLink : getComLinkList())
			if(comLink.getId().equals(Id)) {
				return comLink;
			}
			return null;	
	}
	
	public ConfigFile getConfigFile() {
		return configFile;
	}
	
	public ComLink getComLinkWithServer() {
		return comLinkWithServer;
	}
	
	public abstract void OnClientConnected(Socket socket);
	public abstract void OnClientDisconnect(ComLink comLink);

	public abstract void onCmdReceived(ComCmd aCommand);	
	
	public String getUniqueId() {
		return uniqueId;
	}
	
	public int getListenerPort() {
		return listenerPort;
	}
	
	public InetAddress getListenerIP() {
		return listenerIP;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	
	
	public static String getUserDir() {
		String path = new File(".").getAbsolutePath();
		return path.substring(0,path.length()-2);
	}

	public Vector<OnlineClient> getClientList() {
		return clientList;
	}

	public void setClientList(Vector<OnlineClient> clientList) {
		this.clientList = clientList;
	}
	
	protected OnlineClient getClientFromId(String uniqueId) {
		
		for(OnlineClient registeredClient : getClientList())
			if(registeredClient.getId().equals(uniqueId)) {
				return registeredClient;
				}
		
		return null;
	}
	
	protected boolean isCommandAllowed(ComCmd cmd) {
		return((cmd.getId() != null && (getClientFromId(cmd.getId()) != null || cmd.getType() == CmdType.sendMyListenerInfo)) || cmd.getId().equals("Console"));
	}
	
	protected void registerClient(ComCmd cmd) {
		
		LoggerP2P.logMsg("Registering client "+cmd.getId(), getClass(), "registerClient");
		
		//check if client is registered
		OnlineClient client = getClientFromId(cmd.getId());
		if(client != null) {
			client.setListeningIP((InetAddress) cmd.getData()[0]);
			client.setListeningPort((int) cmd.getData()[1]);
		}
		// create client
		else {
			getClientList().add(new OnlineClient(cmd.getId(), (InetAddress)cmd.getData()[0],(int)cmd.getData()[1]));
			LoggerP2P.logMsg("Client "+cmd.getId()+" added to the client list", getClass(), "registerClient");
		}
		
		//check if ComLink for client already exist and remove it
		ComLink comLink = getComLinkFromId(cmd.getId());
		if(comLink != null) {
			getComLinkList().remove(comLink);
		}
		
		// add ID to new ComLink created upon connection
		boolean found = false;
		for(ComLink link : getComLinkList()) {
			if(link.getLink().getPort() == (int)cmd.getData()[2] && link.getLink().getInetAddress().equals((InetAddress)cmd.getData()[0])) {
				link.setId(cmd.getId());
				found = true;
				LoggerP2P.logMsg("Client id updated "+cmd.getId(), getClass(), "registerClient");
				break;
			}
		}
		if(!found)
			LoggerP2P.logMsg("Unable to update client "+cmd.getId(), getClass(), "registerClient");
	}
}
