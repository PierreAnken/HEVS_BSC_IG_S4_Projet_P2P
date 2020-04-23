package p2p_server;


import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

import p2p_common.ComCmd;
import p2p_common.ComLink;
import p2p_common.ConfigFile;
import p2p_common.LoggerP2P;
import p2p_common.AppSkeleton;
import p2p_common.CmdType;
import p2p_common.OnlineClient;
import p2p_common.SharedFile;

public class Server extends AppSkeleton{

	final static String CONFIGFILENAME = "ServerConfig.p2p";
	private Vector<SharedFile> serverRegisteredFiles = new Vector<SharedFile>();
	
	public Server (){
		super(new ConfigFile(CONFIGFILENAME, true),true);
		System.out.println("Server started, use \"help\" to get command list");
	}

	//we know what we receive as data
	@SuppressWarnings("unchecked") 
	@Override
	public void onCmdReceived(ComCmd cmd) {

		//discard any command from non registered client other then registering info
		if(!isCommandAllowed(cmd)) {
			LoggerP2P.logMsg("Command discarded: "+cmd.getType(),getClass(),"onCmdReceived"); 
			return;
		}
		
		switch (cmd.getType())
		{
		
		case help:		
			System.out.println("===== Command list =====");
			System.out.println("\"online : get a list with all online clients");
			System.out.println("\"disconnect x\" : disconnect client with id \"x\"");
			System.out.println("\"info x\" : get info from client with id \"x\"");
			System.out.println("\"help\" : display this menu");
			break;
		
		case disconnect:
			
			int idClient = (int)cmd.getData()[0];
			
			if(idClient > getClientList().size()-1 || idClient < 0)
				System.out.println("Invalid client Id");
			else {
				try {
					getComLinkFromId(getClientList().get(idClient).getId()).getLink().close();
					System.out.println("\nClient "+getClientList().get(idClient).getId()+" sucessfully disconnected");
					getClientList().remove(getClientList().get(idClient));

				} catch (IOException e) {
					LoggerP2P.logMsg("Unable to disconnect client: "+getClientList().get(idClient).getId(),getClass(),"onCmdReceived:disconnectClient"); 
					System.out.println("\nUnable to disconnect client");
				}
			}
			
			break;
			
			case info:
			
				int idClient2 = (int)cmd.getData()[0];
				
				if(idClient2 > getClientList().size()-1 || idClient2 < 0)
					System.out.println("Invalid client Id");
				else {
					
					System.out.println("\nClient "+getClientList().get(idClient2).getId());
			
					Vector<SharedFile> clientFiles = getClientList().get(idClient2).getFileList();
					if(clientFiles.size() == 0)
						System.out.println("Client donsen't share any file");
					else {
						System.out.println("\nFile list:\n");
						for(SharedFile file : clientFiles) {
							System.out.println("    "+file);
						}
					}
					
				}
			
				break;
			
			case online:
				
				if(getClientList().size() == 0)
					System.out.println("No client connected");
				else {
					int i = 0;
					for(OnlineClient client : getClientList()) {
						System.out.println("\nConnected clients:");
						System.out.println("["+i+"] Unique Identifier: "+ client.getId()+"\n    IP: "+client.getListeningIP().getHostName());
						i++;
					}
				}
				
				break;
			
		case sendMyListenerInfo:		
			registerClient(cmd);
			break;
			
		case sendMyFileList:
			
			//search client and update his file list
			for(OnlineClient registeredClient : getClientList())
				if(registeredClient.getId().equals(cmd.getId())) {
					registeredClient.setFileList((Vector<SharedFile>) cmd.getData()[0]);
					break;
				}
			String files = "";
			for(SharedFile file : (Vector<SharedFile>)cmd.getData()[0])
				files+=file.getFileName()+" ";
			LoggerP2P.logMsg("    Filelist detail: "+files,getClass(),"onCmdReceived:sendMyFileList"); 
			
			break;
		
		case getAllFileList:
			
			//get user files
			Vector<SharedFile> userFiles = new Vector<SharedFile>();
			for(OnlineClient client2 : getClientList())
				if(client2.getId().equals(cmd.getId())) {
					userFiles = client2.getFileList();
					break;
				}
			
			updateRegisteredFileList();
			
			//we create an array with the file he dosen't already own
			Vector<SharedFile> fileListToSend = new Vector<SharedFile>();
			String filesName = "";
			for(SharedFile serverFile : serverRegisteredFiles) {
				boolean duplicate = false;
				for(SharedFile userFile : userFiles) {
					if(serverFile.getHash().equals(userFile.getHash()) && serverFile.getFileName().equals(userFile.getFileName())) {
						duplicate = true;
						break;
					}
				}
				if(!duplicate) {
					fileListToSend.add(serverFile);
					filesName += serverFile.getFileName()+" ";
				}
			}
			
			getComLinkFromId(cmd.getId()).sendCommand(new ComCmd(getUniqueId(), CmdType.sendAllFileList, new Object[] {fileListToSend}));
			
			LoggerP2P.logMsg("FileList sent to client: "+filesName, getClass(),"onCmdReceived"); 
			
			break;
			
		case getClientWithFile:
			
			String[] fileInfo = (String[]) cmd.getData();
			String fileName = fileInfo[0];
			String fileHash = fileInfo[1];
			
			LoggerP2P.logMsg("Client "+cmd.getId()+" asking file "+fileName+"/"+fileHash, getClass(),"onCmdReceived:getClientWithFile");
			updateRegisteredFileList();
			
			//check if file is registered on server and get client id
			Vector<String> listIdClientWithFile = new Vector<String>();
			
			for(SharedFile serverFile : serverRegisteredFiles) {
				LoggerP2P.logMsg(serverFile.getFileName()+" is own by"+serverFile.getClientsWithFileId().size(), getClass(),"onCmdReceived");
				if(serverFile.getHash().equals(fileHash) && serverFile.getFileName().equals(fileName)) {
					listIdClientWithFile = serverFile.getClientsWithFileId();
					LoggerP2P.logMsg("Asked file found. "+listIdClientWithFile.size()+" client(s) own this file.", getClass(),"onCmdReceived");
				   
					break;
				}
			}
			
			Vector<OnlineClient> listClientWithFile = new Vector<OnlineClient>();
					
			//we only return clients which are online
			for(OnlineClient client4 : getClientList())
				//if client id match file and is not client asking for file
				if(listIdClientWithFile.contains(client4.getId()) && client4.getId() != cmd.getId())
					//if client is online
					if(!getComLinkFromId(client4.getId()).getLink().isClosed())
						listClientWithFile.add(client4);
			
			//send to client list with remote client who own the file
			getComLinkFromId(cmd.getId()).sendCommand(new ComCmd(getUniqueId(), CmdType.sendClientWithFile, new Object[] {listClientWithFile,fileName,fileHash}));
			
			break;
		default:
			break;
		}
		LoggerP2P.logMsg("Cmd "+cmd.getType()+" processed", getClass(),"onCmdReceived:getClientWithFile");
	}

	
	private void updateRegisteredFileList() {
		
		serverRegisteredFiles = new Vector<SharedFile>();
		for(OnlineClient registeredClient : getClientList()) {
			
			//for each file from each registered client
			for(SharedFile sharedFile : registeredClient.getFileList()) {
				boolean duplicate = false;
				
				//we check if file is already registered on server
				for(SharedFile serverFile : serverRegisteredFiles)
					if(serverFile.getHash().equals(sharedFile.getHash()) && serverFile.getFileName().equals(sharedFile.getFileName())) {
						duplicate = true;
						//if yes we add the client as file owner
						serverFile.addClientWithFile(registeredClient.getId());
					}
				//else we add the file in the server list
				if(!duplicate) {
					sharedFile.addClientWithFile(registeredClient.getId());
					serverRegisteredFiles.add(sharedFile);
				}
			}
		}
		LoggerP2P.logMsg("Update from server file completed. Server has "+getClientList().size()+" client(s) and "+serverRegisteredFiles.size()+" file(s)", getClass(),"updateRegisteredFileList");
		
	}
	
	@Override
	public void OnClientConnected(Socket socket) {
		
		ComLink linkWithClient = new ComLink(this, socket);	
		getComLinkList().add(linkWithClient);
		LoggerP2P.logMsg("Client connected", getClass(),"OnClientConnected");
		
	}
	@Override
	public void OnClientDisconnect(ComLink comLink) {
		
		//we remove client from registered clients
		for(OnlineClient client : getClientList()) {
			if(client.getId().equals(comLink.getId())) {
				getClientList().remove(client);
				break;
			}
		}
		
		LoggerP2P.logMsg("Client "+comLink.getId()+" disconnected", getClass(),"OnClientDisconnect");
		
		//we remove his file from serverRegisteredFiles
		updateRegisteredFileList();
		
	}
	
	public static void main(String[] args) {
		new Server();
	}
}
