/*
 * Created on Dec 3, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package iitb.CRF;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Constraint {
	public static int UNION=1;
	public static int PAIR_DISALLOW=2;
	public static final int ALLOW_ONLY = 3;
	public int type();
}
