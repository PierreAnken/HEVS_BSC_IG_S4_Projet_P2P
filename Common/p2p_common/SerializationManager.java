package p2p_common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;

public class SerializationManager {
	

	public static byte[] ObjectToBytes(Object anObject) {
		
		try {
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(anObject);
			return out.toByteArray();
					
			} catch (IOException e) {
				LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, SerializationManager.class,"ObjectToBytes");
			}
		
		return null;
	}
	
	public static void ObjectToStream(Object anObject, Socket link) {
		
		try {
			
			DataOutputStream dos = new DataOutputStream(link.getOutputStream());
			
			byte[] objectByte = ObjectToBytes(anObject);
			dos.writeInt(objectByte.length);
			dos.write(objectByte);
				 
		} catch (IOException e) {
			LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, SerializationManager.class,"ObjectToStream");
		}
	}
	
	public static Object BytesToObject(byte[] objectB) {
		
		try {
			
			ObjectInputStream ois =  new ObjectInputStream(new ByteArrayInputStream(objectB));

			return ois.readObject();
				
		} catch (IOException | ClassNotFoundException e) {
			LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, SerializationManager.class,"BytesToObject");
		}
		
		return null;
	}
	
	public static Object streamToObject(Socket link) {
		
		try {
						
			DataInputStream dis = new DataInputStream(link.getInputStream());
	       int length = dis.readInt();
	       				
	        byte[] objectByte = null;
	        
	        if(length>0) {
	        	objectByte = new byte[length];
	            dis.readFully(objectByte, 0, objectByte.length);
	            return BytesToObject(objectByte);
	        }
	        
		} catch (Exception e) {
			LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, SerializationManager.class,"streamToObject");
			return new ComCmd("streamToObject", CmdType.stopCom);
		} 
		return null;
	}
		
}
