package p2p_common;

import java.util.logging.Level;

public class SocketListener implements Runnable{
	
	ComLink comLink;
		
	public SocketListener(ComLink comLink) {
		this.comLink = comLink;
	}

	@Override
	public void run() {
		
		//listen to commands
		while(true) {
			try {
				
				Object object = SerializationManager.streamToObject(comLink.getLink());
				
				if(object instanceof ComCmd) {
					ComCmd cmd = (ComCmd)object;
					if(cmd.getType() == CmdType.stopCom)
						break;
					else
						//pass them to parent
						comLink.onCmdReceived((ComCmd)object);
				}
				
				Tb.Sleep(Thread.currentThread(), 200);
			}
			catch (Exception e) {
				LoggerP2P.logMsg(e.getMessage(), Level.SEVERE,getClass(),"run");
				break;
			}
		}
		
		LoggerP2P.logMsg("SocketListener stopped",getClass(),"run");
		comLink.socketDisconnected();
		Thread.currentThread().interrupt();
	}
}
