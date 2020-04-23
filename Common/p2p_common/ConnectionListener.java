package p2p_common;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

public class ConnectionListener implements Runnable{
	private ServerSocket serverSocket;
	private AppSkeleton parent;
	
	public ConnectionListener(ServerSocket serverSocket, AppSkeleton parent) {
		this.serverSocket = serverSocket;
		this.parent = parent;
	}
	
	@Override
	public void run() {
		
		while(true) {
			try {
				parent.OnClientConnected(serverSocket.accept());
				
			} catch (IOException e) {
				LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, getClass(),"run");
			}
		}
	}

}
