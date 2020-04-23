package p2p_common;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Vector;

public class OnlineClient implements Serializable{
	
	private static final long serialVersionUID = 6500203963930849937L;
	
	private int listeningPort;
	private InetAddress listeningIP;
	private Vector<SharedFile> fileList = new Vector<SharedFile>();
	private String uniqueId = "-2";
	
	public OnlineClient(String uniqueId, InetAddress listeningIP, int listeningPort) {
		this.uniqueId = uniqueId;
		this.listeningPort = listeningPort;
		this.listeningIP = listeningIP;
	}
	
	public String getId() {
		return uniqueId;
	}
		
	public int getListeningPort() {
		return listeningPort;
	}
	
	public void setListeningPort(int listeningPort) {
		this.listeningPort = listeningPort;
	}
	
	public InetAddress getListeningIP() {
		return listeningIP;
	}
	
	public void setListeningIP(InetAddress listeningIP) {
		this.listeningIP = listeningIP;
	}
	
	public Vector<SharedFile> getFileList() {
		return fileList;
	}
	
	public void setFileList(Vector<SharedFile> fileList) {
		this.fileList = fileList;
	}

}
