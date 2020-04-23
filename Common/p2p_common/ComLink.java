package p2p_common;

import java.net.Socket;
import java.util.logging.Level;

public class ComLink{
	
	private AppSkeleton localApp;
	private Socket link;
	private String uniqueId = "-1";
	private SocketListener socketListener;
	
	public ComLink(AppSkeleton localApp, Socket link) {
		this.localApp = localApp;
		this.link = link;
		socketListener = new SocketListener(this);
		new Thread(socketListener).start();
	}
	
	
	public void sendCommand(ComCmd cmd) {
		if(!link.isClosed()) {
			LoggerP2P.logMsg("Cmd sent "+cmd.getId()+": "+cmd.getType()+" with data size "+cmd.getData().length, getClass(),"sendCommand");
			SerializationManager.ObjectToStream(cmd, link);
		}
		else
			LoggerP2P.logMsg("Cmd from "+cmd.getId()+": "+cmd.getType()+"cancelled due to socket closed",Level.WARNING, getClass(),"sendCommand");
	}
	
	
	public void onCmdReceived(ComCmd cmd) {
		LoggerP2P.logMsg("Cmd received: "+cmd.getType()+" from "+cmd.getId()+" with data size "+cmd.getData().length, getClass(),"onCmdReceived");
		localApp.onCmdReceived(cmd);
	}
	
	public Socket getLink() {
		return link;
	}
	
	public String getId() {
		return uniqueId;
	}
	
	public void setId(String uniqueId) {
		this.uniqueId = uniqueId;
	}	

	public SocketListener getSocketListener() {
		return socketListener;
	}
	
	public void socketDisconnected() {
		localApp.OnClientDisconnect(this);
	}
}
