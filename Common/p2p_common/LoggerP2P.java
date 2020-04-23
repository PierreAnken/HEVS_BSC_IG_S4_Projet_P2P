package p2p_common;

import java.util.Date;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerP2P{
	
	// Info for all the useful operations
	// Warning for all the possible network errors
	// Severe for the exceptions
	
	public static Logger logger;
	private static LoggingFormatter formatter = new LoggingFormatter();
	private static int month = 0;
	private static int year = 0;
	
	public LoggerP2P (String loggerName,boolean debugMode){
		LoggerP2P.logger = Logger.getLogger(loggerName);
		logger.setUseParentHandlers(debugMode);
	}
	
	
	public static void logMsg(String msg, Class<?> aClass, String aMethod){
		logMsg(msg, Level.INFO, aClass, aMethod);
	}	
	

	public static void logMsg(String msg, Level level, Class<?> aClass, String aMethod){
		
		// Get the year and the month	
		Date date = new Date();
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int yearNow  = localDate.getYear();
		int monthNow = localDate.getMonthValue();
		
		try{

			//if the date need an update we recreate the handler
			if(year != yearNow || month != monthNow) {
				for(Handler handler : logger.getHandlers())
					logger.removeHandler(handler);
				
				year = yearNow;
				month = monthNow;
				
				String directory = AppSkeleton.getUserDir()+"\\logs\\"+year;
				
				File logPath = new File(directory);
				
				if(!logPath.exists() || !logPath.isDirectory())
					logPath.mkdirs();
				
				String monthS = month+"";
				if(monthS.length() == 1)
					monthS = "0"+monthS;
				
				
				FileHandler fh = new FileHandler(directory+"\\"+monthS+".log",true);
				logger.addHandler(fh);
		
				//use a custom formatter 
				fh.setFormatter(formatter);
			}
			
			logger.logp(level,aClass.getName(), aMethod, msg);

		} catch (RuntimeException ex) {
			//we do nothing as we can't log
		} catch (IOException ex) {
			//we do nothing as we can't log
		}
		
	}
}




