/** LogMessage.java
 * 
 * @author Amit Jaiswal
 * @version 1.3
 */
package iitb.Utils;
import java.util.*;
import java.io.*;

/**
 * This class is to issue warnings against using unsafe operations with the provision that
 * the warning is issued only k number of times, where k is a user defined parameter. The
 * key to uniquely identify a warning is the message string, or a key defined by the user.
 */
public class LogMessage {
	static class Message {
		public String msg;
		public int maxTimes;
		public int currNo;
		Message(String msg, int maxTimes) {
			this.msg = msg;
			this.maxTimes = maxTimes;
			this.currNo = 0;
		}	
	};

	private static Hashtable<String, Message> msgTable = new Hashtable<String, Message>();

	/**
	 * @param msg The message to be displayed
	 * @param times The maximum number of times the warning will be displayed
	 */
	private static String getMessage(String key, String msg, int times) {
		if(key == null || msg == null || times <= 0) {
			return null;
		}

		if(msgTable.get(key) == null) {
			msgTable.put(key, new Message(msg, times));
		}

		Message message = (Message)msgTable.get(key);
		message.currNo++;
		if(message.currNo <= message.maxTimes) {
			return message.msg;
		} else {
			return null;
		}	
	}

	/**
	 * @param key String to uniquely identify a warning
	 * @param msg The message to be displayed
	 * @param times The maximum number of times the warning will be displayed
	 * @param out The output stream
	 */
	public static void issueWarning(String key, String msg, int times, PrintStream out) {
		String warnStr = getMessage(key, msg, times);
		if(warnStr != null) {
			out.println(warnStr);
		}	
	}	
	
	public static void issueWarning(String key, String msg) {
		issueWarning(key, msg, 1, System.out);
	}	
	
	public static void issueWarning(String msg) {
		issueWarning(msg, msg, 1, System.out);
	}	
};
