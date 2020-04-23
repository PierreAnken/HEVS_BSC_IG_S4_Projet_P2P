package p2p_common;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import java.util.logging.Level;

public class SharedFile implements Serializable{
	
	private static final long serialVersionUID = -1275294113773145873L;
	
	private File file;
	private double fileSize;
	private String fileName;
	private String hash;
	private Vector<String> clientWithFileId = new Vector<String>();
	
	public SharedFile(File file, String clientId){
		try {

			if(file.exists()) 
				if(file.isFile()){
					
					//SHA-1 checksum 
					MessageDigest shaDigest = MessageDigest.getInstance("SHA-1");
					hash = Tb.getFileChecksum(shaDigest, file);
					
					this.file = file;
					fileSize = file.length();
					fileName = file.getName();

				}
							
				else if(file.isDirectory()){
					directoryToSharedFileList(file, clientId);
				}
			
			clientWithFileId.add(clientId);
		} 
		catch (NoSuchAlgorithmException e) {
			LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, getClass(),"constructor");
		}
		catch (IOException e) {
			LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, getClass(),"constructor");
		}
	}
	
	public File getFile() {
		return file;
	}
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public double getFileSize() {
		return fileSize;
	}
	
	public String getHash() {
		return hash;
	}
	
	public Vector<String> getClientsWithFileId() {
		return clientWithFileId;
	}
	
	public void addClientWithFile(String clientId) {
		if(!clientWithFileId.contains(clientId))
			clientWithFileId.add(clientId);
	}
	
	public void removeClientWithFile(String clientId) {
		clientWithFileId.remove(clientId);
	}
	
	public static SharedFile fileToSharedFile (File file, String clientId){
		SharedFile fileData = new SharedFile(file, clientId);
		return fileData;
	}
	
	public static Vector<SharedFile> directoryToSharedFileList (File directory, String clientId){
		File[] list = directory.listFiles(); 
		return	fileListToSharedFileList (list, clientId);
	}
	
	public static Vector<SharedFile> fileListToSharedFileList (File [] fileList, String clientId){
		Vector<SharedFile> filesShared = new Vector<SharedFile>();

		for(File file : fileList){
			
			//ignore .tmp file
			if(!file.getName().toLowerCase().endsWith(".tmp"))
				filesShared.add(new SharedFile(file, clientId));
		}
		return filesShared;
	}
	
	public String toString() {
		double fileSizeT =  fileSize;
		String unit;
		
		if(fileSizeT<1000) {
			unit = "o";
		}else if(fileSizeT<1000*1000) {
			unit = "ko";
			fileSizeT /= 1000;
		
		}else if(fileSizeT<1000*1000*1000) {
			unit = "Mo";
			fileSizeT /= 1000*1000;
		}else {
			unit = "Go";
			fileSizeT /= 1000*1000*1000;
		}
		fileSizeT = Math.round(fileSizeT * 100.0) / 100.0;
		
		return fileName+" \n    Size: "+fileSizeT+" "+unit+"\n    Checksum: "+hash+"\n";
		
	}

}