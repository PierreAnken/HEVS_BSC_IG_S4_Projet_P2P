package p2p_common;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import p2p_client.Client;

public class FileSender implements Runnable{
	
	private Client client;
	private ComLink comLink;
	private SharedFile sharedFile;
	
	public FileSender(Client client, ComLink comLink,SharedFile sharedFile) {
		this.client = client;
		this.comLink = comLink;
		this.sharedFile = sharedFile;
	}
 
	@Override
	public void run() {
	
		try {
			LoggerP2P.logMsg("sending process started",getClass(),"run:sendmode");
			
			FileInputStream fis = new FileInputStream(sharedFile.getFile());
			
			//Birate = 5ko/sec 5 because we sent 5 file part per second from 1ko
			byte[] buffer = new byte[1024];
			int readLength = -1;
			int partNbr = 1;
						
				while ((readLength = fis.read(buffer)) > 0) {
					
					byte[] filePart = Arrays.copyOf(buffer,readLength);
					comLink.sendCommand(new ComCmd(client.getUniqueId(), CmdType.filePart, new Object[] {sharedFile,filePart,partNbr}));
					LoggerP2P.logMsg("Part "+partNbr+" sent",getClass() ,"onCmdReceived:getFile");
					partNbr++;
					Tb.Sleep(Thread.currentThread(), 200);
				}
			
			LoggerP2P.logMsg("File "+sharedFile.getFileName()+" completely sent to client",getClass(),"onCmdReceived:getFile");
			fis.close();
		} catch (IOException e) {
			LoggerP2P.logMsg(e.getMessage(),getClass() ,"run:sendmode");
		}
	}
}
