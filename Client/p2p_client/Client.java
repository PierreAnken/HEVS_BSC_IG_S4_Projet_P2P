package p2p_client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.Level;

import p2p_common.FatalError;
import p2p_common.FileInDownload;
import p2p_common.FileSender;
import p2p_common.LoggerP2P;
import p2p_common.OnlineClient;
import p2p_common.AppSkeleton;
import p2p_common.CmdType;
import p2p_common.ComCmd;
import p2p_common.ComLink;
import p2p_common.ConfigFile;
import p2p_common.SharedFile;
import p2p_common.Tb;

public class Client extends AppSkeleton{
	
	final static String CONFIGFILENAME = "ClientConfig.p2p";
	Vector<SharedFile> serverFileList = new Vector<SharedFile>();
	Vector<SharedFile> localFileList = new Vector<SharedFile>();
	Vector<FileInDownload> filesInDownload = new Vector<FileInDownload>();
		
	public Client () {
		
		super(new ConfigFile(CONFIGFILENAME,false),false);
		
		System.out.println("Connected to server "+getComLinkWithServer().getLink().getInetAddress().getHostName()+":"+getComLinkWithServer().getLink().getPort());
		
		//register with server
		System.out.println("Registering with server...");
		getComLinkWithServer().sendCommand(new ComCmd(getUniqueId(), CmdType.sendMyListenerInfo, new Object[] {getListenerIP(),getListenerPort(),getComLinkWithServer().getLink().getLocalPort()}));
		Tb.Sleep(Thread.currentThread(),500);
		
		//send our file list to server
		System.out.println("Refreshing local file list..."); 
		refreshLocalFileList();
		getComLinkWithServer().sendCommand(new ComCmd(getUniqueId(), CmdType.sendMyFileList, new Object[]{localFileList}));
		
		Tb.Sleep(Thread.currentThread(),500);
		
		System.out.println("Retriving file list from server..."); 
		getComLinkWithServer().sendCommand(new ComCmd(getUniqueId(), CmdType.getAllFileList));
	}

	@Override
	public void OnClientConnected(Socket socket) {
		
		ComLink comLink = new ComLink(this,socket);
		getComLinkList().add(comLink);
		
		LoggerP2P.logMsg("Client connected", getClass(), "OnClientConnected");
	}
	
	@Override
	public void OnClientDisconnect(ComLink comLink) {
		
		//if we lost link with server we inform user and kill application
		if(comLink.equals(getComLinkWithServer())) {
			LoggerP2P.logMsg("Connection lost with server",Level.SEVERE,getClass(),"OnClientDisconnect"); 
			System.out.println("Connection lost with server, please restart application after download are completed.");
		}
		else {
			//we check if we are dowloading a file from user
			
			
			Vector<FileInDownload> toDelete = new Vector<FileInDownload>();
			
			for(int i = 0; i<filesInDownload.size(); i++) {
				if(filesInDownload.get(i).getRemoteClientId().equals(comLink.getId())) {
					LoggerP2P.logMsg("Connection lost with client sending us file"+filesInDownload.get(i).getSharedFile().getFileName(),Level.SEVERE,getClass(),"OnClientDisconnect"); 
					System.out.println("Connection lost with client sending us "+filesInDownload.get(i).getSharedFile().getFileName());
					toDelete.add(filesInDownload.get(i));
				}
			}
			
			for(int i = 0; i<toDelete.size(); i++)
				filesInDownload.remove(toDelete.get(i));
		}
	}
	
	//we know what we receive as data
	@SuppressWarnings("unchecked")
	@Override
	public void onCmdReceived(ComCmd cmd) {
		
		//discard any command from non registered client
		if(!isCommandAllowed(cmd) && cmd.getType() == CmdType.getFile) {
			LoggerP2P.logMsg("Command discarded: "+cmd.getType(),getClass(),"onCmdReceived"); 
			return;
		}

		switch (cmd.getType())
		{
		
		
		case help:		
			System.out.println("\"refresh\" : get the list of file available on server");
			System.out.println("\"dl x\" : download the file with id \"x\"");
			System.out.println("\"help\" : display this menu");
			break;
		
		case sendMyListenerInfo:		
			registerClient(cmd);
			break;
			
		case sendAllFileList:
			
			serverFileList = (Vector<SharedFile>) cmd.getData()[0];			
			
			if(serverFileList.size() == 0)
				System.out.println("No new file available on server. Use \"help\" to see available commands.");
			else {
				System.out.println("Available files:\n");
				int i = 0;
				for(SharedFile sharedFile : serverFileList) {
											
					System.out.println("["+i+"] "+sharedFile);
					i++;
				}
				System.out.println("\nTo download a file use the command \"dl + [file ID]\" Sample \"dl 0\"");
			}
			
			break;
		case refresh:
			System.out.println("Retriving file list from server..."); 
			refreshLocalFileList();
			getComLinkWithServer().sendCommand(new ComCmd(getUniqueId(), CmdType.sendMyFileList, new Object[]{localFileList}));
			getComLinkWithServer().sendCommand(new ComCmd(getUniqueId(), CmdType.getAllFileList));
			
			break;
			
		case dl:
			
			int fileId = (int) cmd.getData()[0];
			if(fileId < 0 || fileId > serverFileList.size()-1)
				System.out.println("Invalid file id");
			else {
				
				//check if file is not already in download
				SharedFile fileAsked = serverFileList.get(fileId);
				
				boolean alreadyInDownload = false; 
				for(int i = 0; i<filesInDownload.size(); i++)
					if(filesInDownload.get(i).getSharedFile().getFileName().equals(fileAsked.getFileName())) {
						alreadyInDownload = true;
						break;
					}
				
				if(alreadyInDownload)
					System.out.println(fileAsked.getFileName()+" is already under download...");
				else {
					System.out.println("Asking server for clients with this file ..."); 	
					filesInDownload.add(new FileInDownload(fileAsked));
					getComLinkWithServer().sendCommand(new ComCmd(getUniqueId(), CmdType.getClientWithFile, new String[]{fileAsked.getFileName(),fileAsked.getHash()}));
				}
			}
			break;
		
		case sendClientWithFile:
			
			//we received from server the list of client with the file we need
			Vector<OnlineClient> listClientWithFile = (Vector<OnlineClient>) cmd.getData()[0];
			
			if(listClientWithFile.size() == 0) {
				System.out.println("No client with the file "+cmd.getData()[1]+" currently online."); 
				LoggerP2P.logMsg("No client online with file",getClass(),"onCmdReceived:sendClientWithFile"); 
			}
			else {
				
				for(OnlineClient clientRemote : listClientWithFile) {
					
					//try to contact client and ask for file
					try {
						System.out.println("Trying to contact client with ip "+clientRemote.getListeningIP().getHostAddress());
						LoggerP2P.logMsg("Trying to contact "+clientRemote.getListeningIP().getHostAddress()+":"+clientRemote.getListeningPort(),getClass(),"onCmdReceived:sendClientWithFile");
						
						if(clientRemote.getListeningIP().isReachable(250)) {
						
							ComLink comLinkinkWithOtherClient = new ComLink(this, new Socket(clientRemote.getListeningIP(), clientRemote.getListeningPort()));
							comLinkinkWithOtherClient.setId(clientRemote.getId());
							getComLinkList().add(comLinkinkWithOtherClient);

							//we register with other client
							comLinkinkWithOtherClient.sendCommand(new ComCmd(getUniqueId(), CmdType.sendMyListenerInfo, new Object[] {getListenerIP(),getListenerPort(),comLinkinkWithOtherClient.getLink().getLocalPort()}));
							
							Tb.Sleep(Thread.currentThread(), 500);
							
							//we ask the client for file
							String fileName = (String) cmd.getData()[1];
							String fileHash = (String) cmd.getData()[2];
																					
							Tb.Sleep(Thread.currentThread(), 500);
							
							//ask client for file
							comLinkinkWithOtherClient.sendCommand(new ComCmd(getUniqueId(), CmdType.getFile, new Object[]{fileName,fileHash} ));					
							System.out.println("Connected to remote client, file asked.");
							break;
						}
						else {
							LoggerP2P.logMsg("Client "+clientRemote.getListeningIP().getHostAddress()+" not reachable", getClass(), "onCmdReceived:sendClientWithFile");
							System.out.println("Client "+clientRemote.getListeningIP().getHostAddress()+" not reachable");
						}
												
					} catch (IOException e) {
						LoggerP2P.logMsg(e.getMessage(), Level.SEVERE,getClass() ,"onCmdReceived:sendClientWithFile");
						System.out.println("Unable to contact client "+clientRemote.getListeningIP().getHostAddress()+":"+clientRemote.getListeningPort());
					}
				}
			}
			break;
		
		case getFile:
			
			//another client is asking us to send him a file
			String askedFileName = (String) cmd.getData()[0];
			String askedFileHash = (String) cmd.getData()[1];
			SharedFile askedFile = null;
			
			//check if file exist in share folder
			refreshLocalFileList();
			for(SharedFile localfile : localFileList) {
				if(localfile.getFileName().equals(askedFileName) && localfile.getHash().equals(askedFileHash)) {
					askedFile = localfile;
					LoggerP2P.logMsg(askedFileName+" is available in share folder",getClass() ,"onCmdReceived:getFile");
				}
			}
			
			ComLink comLinkWithClient = getComLinkFromId(cmd.getId());
										
			//if file not found we inform client and server that file is not there anymore
			if(askedFile == null) {
				comLinkWithClient.sendCommand(new ComCmd(getUniqueId(), CmdType.fileTransfertInfo, new Object[] {"Le fichier"+askedFileName+"n'est plus disponible."}));
				LoggerP2P.logMsg(askedFileName+" not found locally, informing server and client",getClass(),"onCmdReceived:getFile");
				
				refreshLocalFileList();
				getComLinkWithServer().sendCommand(new ComCmd(getUniqueId(), CmdType.sendMyFileList, new Object[]{localFileList}));
			}
			else {
				LoggerP2P.logMsg("starting new thread to send file",getClass(),"onCmdReceived:getFile");
				new Thread(new FileSender(this, comLinkWithClient, askedFile)).start();
			}
				
			
			break;
			
		case filePart:
			
			SharedFile fileReceived = (SharedFile) cmd.getData()[0];
			int partNbr = (int)cmd.getData()[2];
			File fileToWrite = new File(AppSkeleton.getUserDir()+"\\shared\\"+fileReceived.getFileName()+".tmp");
			FileInDownload fileAsked = null;
			
			try {
							
				//check if we asked the file
				for(int i = 0; i<filesInDownload.size(); i++)
					if(filesInDownload.get(i).getSharedFile().getHash().equals(fileReceived.getHash())) {
						fileAsked = filesInDownload.get(i);
						
						//we update the client ID sending us this file
						fileAsked.setRemoteClientId(cmd.getId());
					}
				
				if(fileAsked == null) {
					LoggerP2P.logMsg("received file "+fileReceived.getFileName()+" part"+partNbr+" but we never asked it",Level.WARNING,getClass(),"onCmdReceived:filePart");
				}
				else {
				
					//check if part received match what we expect
					if(fileAsked.getNextPart() != partNbr) {
						LoggerP2P.logMsg("invalid part received from file "+fileReceived.getFileName(),Level.WARNING,getClass(),"onCmdReceived:filePart");
						
						//error - remove file from file we want to receive so user can download it again
						filesInDownload.remove(fileAsked);
					}
					else
					{
					
						//if file dosen't exit locally
						if(!fileToWrite.exists()) {

							//if we receive part 1 we create file
							if(partNbr == 1) {
								fileToWrite.createNewFile();
								System.out.println("Receiving "+fileReceived.getFileName()+" from remote client.");
							}
							else {
								LoggerP2P.logMsg("Part received >1 but file not found locally "+fileReceived.getFileName(),Level.WARNING,getClass(),"onCmdReceived:filePart");
								
								//error - remove file from file we want to receive so user can download it again
								filesInDownload.remove(fileAsked);
								return;
							}
						}
						else{
							//we erase old tmp file
							if(partNbr == 1) {
								try {
									fileToWrite.delete();
									System.out.println("Old tmp file "+fileReceived.getFileName()+" removed from shared folder and download restarted");
								}catch(Exception e) {
									LoggerP2P.logMsg("Unable to delete old tmp file "+fileToWrite.getName(),Level.SEVERE,getClass(),"onCmdReceived:filePart");
									System.out.println("Unable to delete old tmp file "+fileReceived.getFileName()+" from shared folder.");
									return;
								}
							}
							
						}
						
						//write in file
						FileOutputStream fos = new FileOutputStream(fileToWrite,true);
						byte[] filePart = (byte[]) cmd.getData()[1];
						fos.write(filePart);
						fos.close();
						
						//update part we wait
						fileAsked.setNextPart(fileAsked.getNextPart()+1);
						
						//check file result after write
						SharedFile fileWritten = new SharedFile(fileToWrite, getUniqueId());
						
						//if file is complete
						if(fileWritten.getFileSize() == fileReceived.getFileSize()) {
							LoggerP2P.logMsg("File "+fileReceived.getFileName()+ ".tmp completly received, lets rename it",getClass(), "onCmdReceived:filePart");
							
							//check file integrity
							if(fileWritten.getHash().equals(fileReceived.getHash())){
								//we rename it
								if(!fileToWrite.renameTo(new File(AppSkeleton.getUserDir()+"\\shared\\"+fileReceived.getFileName())))
									System.out.println("File rename failled, please check your share folder");	
								else {
									System.out.println("File "+fileReceived.getFileName()+" succesfully downloaded");
									LoggerP2P.logMsg("File "+fileReceived.getFileName()+ " succesfully downloaded",getClass(), "onCmdReceived:filePart");
									
									//we remove it from current download
									filesInDownload.remove(fileAsked);
									
									//we update our file list with server
									refreshLocalFileList();
									getComLinkWithServer().sendCommand(new ComCmd(getUniqueId(), CmdType.sendMyFileList, new Object[]{localFileList}));
								}
							}
							else {
								if(fileToWrite.delete())
					        		System.out.println("File corrupted, successfully deleted");
								else
									System.out.println("File "+fileReceived.getFileName()+" is corrupted and could not be deleted, please check your share folder");
							}
							
							//remove file from download list
							filesInDownload.remove(fileAsked);
							
						}
						else
							LoggerP2P.logMsg("File "+fileReceived.getFileName()+ ".tmp incomplete, waiting next part "+fileWritten.getFileSize()+"/"+fileReceived.getFileSize(),getClass(), "onCmdReceived:filePart");
						
					}
				}
			} catch (IOException e) {
				LoggerP2P.logMsg(e.getMessage(),getClass() ,"onCmdReceived:filePart");
			}
			break;
		
		case fileTransfertInfo:
			System.out.println(cmd.getData()[0]);
			break;
			
		default:
			break;
		}
		LoggerP2P.logMsg("Cmd "+cmd.getType()+" processed",getClass(),"onCmdReceived");
	}
	
	private void refreshLocalFileList() {
		File sharedFolder = new File(AppSkeleton.getUserDir()+"\\shared");
	
		if(sharedFolder.exists()){
			if(sharedFolder.isDirectory())
				localFileList = SharedFile.directoryToSharedFileList(sharedFolder, getUniqueId());
		}
		else {
			
			//try to create shared folder
			if (!new File(AppSkeleton.getUserDir()+"\\shared").mkdirs()) 
				new FatalError("Unable to create missing folder"+AppSkeleton.getUserDir()+"\\shared");
			
		}
	}
	
	public static void main(String[] args) {
		 new Client();
	}


}
