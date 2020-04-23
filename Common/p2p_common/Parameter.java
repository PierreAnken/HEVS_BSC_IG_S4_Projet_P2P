package p2p_common;

public class Parameter {
	private String paramKey;
	private String paramValue;
	
	public Parameter(String paramKey, String paramValue){
		this.paramKey = paramKey;
		this.paramValue = paramValue;
	}

	public String getKey() {
		return paramKey;
	}

	public void setKey(String paramKey) {
		this.paramKey = paramKey;
	}

	public String getValue() {
		return paramValue;
	}

	public void setValue(String paramValue) {
		this.paramValue = paramValue;
	}
}
