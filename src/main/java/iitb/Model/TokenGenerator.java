package iitb.Model;

import java.io.Serializable;

/**
 * 
 * @author Sunita Sarawagi
 * @since 1.1
 * @version 1.3
 */
public class TokenGenerator implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2611659127209805030L;
	boolean firstCall;
    Object x;
    public void startScan(Object xArg) {x=xArg; firstCall=true;}
    public boolean hasNext() {return firstCall;}
    public Object next() {firstCall = false; return x;}
    public Object getKey(Object xArg) {return xArg; }
};
