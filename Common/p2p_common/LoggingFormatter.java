package p2p_common;

import java.sql.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggingFormatter extends Formatter{


	public String format(LogRecord record) {

		StringBuffer sb = new StringBuffer();

		Date date = new Date((record.getMillis()));
		sb.append(date.toString()+"-"+record.getMillis());
		sb.append(";");
		
		sb.append(record.getLevel().getName());
		sb.append(";");

		sb.append( record.getSourceClassName());
		sb.append(";");
		
		sb.append( record.getSourceMethodName());
		sb.append(";");
	
		sb.append(formatMessage(record));
		sb.append("\r\n");
		
		return sb.toString();
	}
	
	


}
