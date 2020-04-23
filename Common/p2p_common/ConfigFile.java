package p2p_common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.logging.Level;

public class ConfigFile {

	private String filepath = AppSkeleton.getUserDir();
	private String fileName;
	private Vector<Parameter> parameters;
	private String error;
	private boolean isServerCOnfig;
	
	public ConfigFile(String fileName, boolean isServerCOnfig) {
		this.fileName = fileName;
		this.isServerCOnfig = isServerCOnfig;
	}
	
	public String getParam(String aKey) {
		for(Parameter param : parameters) {
			if(param.getKey().equals(aKey))
				return param.getValue();
		}
		return "";
	}
	
	public void setParam(String aKey,String aValue) {
		
		boolean valueFound = false;
		for(Parameter param : parameters) {
			if(param.getKey().equals(aKey)) {
				valueFound = true;
				param.setValue(aValue);
			}
		}
		if(!valueFound)
			parameters.add(new Parameter(aKey, aValue));
	}
	
	public void saveParams() {
		
		File configFile = new File(filepath+"\\"+fileName);
		
		try {
			PrintWriter pw = new PrintWriter(configFile.getAbsolutePath(), "UTF-8");
			
			//clear file
			pw.print("");
			
			//write configuration
			if(isServerCOnfig)
				pw.println("## Server configuration file\n");
			else
				pw.println("## Client configuration file\n");
			
			for(Parameter param : parameters) 
				if(!param.getKey().equals("") && !param.getValue().equals(""))
					pw.println(param.getKey()+" = "+param.getValue());
			
			pw.close();
					
		} catch (FileNotFoundException e) {
			LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, getClass(),"saveParams");
		} catch (IOException e) {
			LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, getClass(),"saveParams");
		}
			
	}
	
	public String getError() {
		return error;
	}
	
	public void load(){
		
		//check if file exist
		File configFile = new File(filepath+"\\"+fileName);
		if(!configFile.exists() || configFile.isDirectory())
			new FatalError("Config file not found "+configFile.getAbsolutePath());
		
		//load parameters
		FileInputStream fis;
		try {
			fis = new FileInputStream(configFile);
		
		BufferedReader br = new BufferedReader(new FileReader(configFile.getAbsolutePath()));
		String line = null;
		parameters = new Vector<Parameter>();
		while ((line = br.readLine()) != null) {
			
			line = line.replaceAll(" ","");
			
			if(!line.equals("") && !line.contains("##")) {
				
				String[] lineData = line.split("=");
				if(lineData.length != 2) {
					new FatalError("Configuration file corrupted");
					break;
				}
				
				String key = lineData[0];
				String value = lineData[1];
				if(key.equals("") || value.equals("")) {
					new FatalError("Configuration file corrupted");
					break;
				}
				else {
					parameters.add(new Parameter(key, value));
				}
			}
	    }
		
		
		fis.close();
		br.close();
		
		//check if unique id was already generated otherwise we generate it
		if(getParam("UniqueId").equals("")) {
			setParam("UniqueId",System.currentTimeMillis()+"-"+((int)(Math.random()*100000))+"");
			saveParams();
		}

		} catch (IOException e) {
			//we dont log the exception has the logger is not yet started
			new FatalError("Error loading parameters \n"+e.getMessage());
		}
	}
}
