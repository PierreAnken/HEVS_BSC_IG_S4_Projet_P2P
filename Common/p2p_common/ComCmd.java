package p2p_common;

import java.io.Serializable;

public class ComCmd implements Serializable{

	private static final long serialVersionUID = -8860524478052210169L;
	
	private CmdType type;
	private Object[] data = new Object[0];
	private String uniqueId;
	
	public ComCmd(String uniqueId, CmdType type, Object[] data){
		this.type = type;
		this.data = data;
		this.uniqueId = uniqueId;
	}
	
	public ComCmd(String uniqueId, CmdType type){
		this.type = type;
		this.uniqueId = uniqueId;
	}
	

	public CmdType getType() {
		return type;
	}
	
	public Object[] getData() {
		return data;
	}
	
	public String getId() {
		return this.uniqueId;
	}
	
}
