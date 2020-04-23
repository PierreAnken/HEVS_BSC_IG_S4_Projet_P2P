package p2p_common;

public class FileInDownload {
	
	private SharedFile file;
	private int nextPart = 1;
	private String remoteClientId = "";
	
	public FileInDownload(SharedFile file) {
		this.file = file;
	}

	public int getNextPart() {
		return nextPart;
	}

	public void setNextPart(int nextPart) {
		this.nextPart = nextPart;
	}

	public SharedFile getSharedFile() {
		return file;
	}

	public String getRemoteClientId() {
		return remoteClientId;
	}

	public void setRemoteClientId(String remoteClientId) {
		this.remoteClientId = remoteClientId;
	}

}
