package p2p_common;

import java.util.Scanner;
import java.util.logging.Level;

public class ConsoleListener implements Runnable{
	private AppSkeleton myApp;
	private Scanner sc;
	private CmdType[] allowedCommand;
	
	public ConsoleListener(AppSkeleton myApp, Scanner sc) {
		this.myApp = myApp;
		this.sc = sc;
		
		if(myApp.isServer())
			allowedCommand = new CmdType[] {CmdType.help,CmdType.disconnect, CmdType.info,CmdType.online};
		else 
			allowedCommand = new CmdType[] {CmdType.help,CmdType.refresh, CmdType.dl};
	}
	
	
	@Override
	public void run() {
		
		//listen to console
		while(true){
			String userInput = sc.nextLine();
			String[] params = userInput.split(" ");
			boolean cmdAccepted = false;
			
			if(params.length > 0 && params.length <3) {
				
				for(CmdType cmdT : allowedCommand)
					if(cmdT.toString().equals(params[0].toLowerCase())) {
						
						if((cmdT == CmdType.dl || cmdT == CmdType.disconnect || cmdT == CmdType.info)&& params.length == 2) {
							
							try {
								
								int id = Integer.parseInt(params[1]);
								
								myApp.onCmdReceived(new ComCmd("Console",cmdT, new Object[] {id}));
								cmdAccepted = true;
								
							}catch(NumberFormatException e) {
								LoggerP2P.logMsg(e.getMessage(), Level.SEVERE, getClass(),"run");
							}
						}
						

						else if(cmdT == CmdType.help || cmdT == CmdType.refresh || cmdT == CmdType.online) {
							cmdAccepted = true;
							myApp.onCmdReceived(new ComCmd("Console",cmdT));
						}	
						
						
						break;
					}
			}
			
			if(!cmdAccepted)
				System.out.println("Unknown command *"+userInput+"* or invalid parameter. Use \"help\" to get the available commands");
		}
	}

	public AppSkeleton getApp() {
		return myApp;
	}
}
