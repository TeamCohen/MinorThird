package edu.cmu.minorthird.util;

import org.apache.log4j.*;
import java.beans.*;
import java.lang.reflect.*;

/**
 */
public class RefUtils
{
	private static Logger log = Logger.getLogger(RefUtils.class);
	private static int LHS=0, RHS=1;

	static private String toGetter(String s)
	{
		return "get"+s.substring(0,1).toUpperCase()+s.substring(1);
	}

	static private String toSetter(String s)
	{
		return "set"+s.substring(0,1).toUpperCase()+s.substring(1);
	}

	static private Object toObject(String s,Class c)
	{
		if ((c==Boolean.class  || c==boolean.class) && c.equals("true")) return new Boolean(true);
		else if ((c==Boolean.class || c==boolean.class) && s.equals("false")) return new Boolean(false);
		else if (c==Integer.class || c==int.class) return new Integer(StringUtil.atoi(s));
		else if (c==Double.class || c==double.class) return new Double(StringUtil.atof(s));
		else try {
			return Class.forName(s).newInstance();
		} catch (Exception ex1) { 
			try {
				return Class.forName("edu.cmu.minorthird."+s).newInstance();
			} catch (Exception ex2) {
				throw new IllegalArgumentException("can't create instance of '"+s+"' or 'edu.bcmu.minorthird."+s+"'");
			}
		}
	}

	public static Object modify(Object obj,String modifications)
	{
		String[] mods = modifications.split("[,;]\\s*");
		for (int i=0; i<mods.length; i++) {
			String[] sides = mods[i].split("=");
			if (sides.length!=2) {
				log.warn("Illegal modification (should be of the form x=y): "+mods[i]);
				continue;
			}
			String[] path = sides[LHS].split("\\.");
			Object objToChange=obj;
			try {
				for (int j=0; j<path.length-1; j++) {
					Method m = objToChange.getClass().getMethod(toGetter(path[j]),new Class[]{});
					objToChange = m.invoke(objToChange, new Object[]{});
				}
				// find a setter method m
				Method m = null; 
				Method ms[] = objToChange.getClass().getMethods();
				String setterName = toSetter(path[path.length-1]);
				for (int k=0; k<ms.length; k++) {
					if (setterName.equals(ms[k].getName())) {
						m = ms[k];
						break;
					}
				}
				if (m==null) log.warn("No setter defined for '"+path[path.length-1]+"'");
				else {
					Class[] expectedClasses = m.getParameterTypes();
					Object rhs = toObject(sides[RHS], expectedClasses[0]);
					m.invoke(objToChange, new Object[]{rhs});
				}
			} catch (Exception ex) {
				log.warn("Can't execute modification '"+mods[i]+"': error was "+ex);
			}
		}
		return obj;
	}
}

