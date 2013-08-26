package iitb.Utils;
import java.util.*;
import java.io.*;

/*
 * ====================================================================
 * Copyright (c) 1995-1999 Purple Technology, Inc. All rights
 * reserved.
 * 
 * PLAIN LANGUAGE LICENSE: Do whatever you like with this code, free
 * of charge, just give credit where credit is due. If you improve it,
 * please send your improvements to server@purpletech.com. Check
 * http://www.purpletech.com/server/ for the latest version and news.
 *
 * LEGAL LANGUAGE LICENSE: Redistribution and use in source and binary
 * forms, with or without modification, are permitted provided that
 * the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. The names of the authors and the names "Purple Technology,"
 * "Purple Server" and "Purple Chat" must not be used to endorse or
 * promote products derived from this software without prior written
 * permission. For written permission, please contact
 * server@purpletech.com.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND PURPLE TECHNOLOGY ``AS
 * IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * AUTHORS OR PURPLE TECHNOLOGY BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 **/


/**
 * Parses command-line options.
 * <pre>
 * Command-line usage: java Options [properties.txt] -param [val] ...
 *   properties.txt - optional file in Properties format
 *   param - property name
 *   val - value for that property (set to null if not present)
 * </pre>
 * @author Alex
 * @version VERSIONDATA
 */
public class Options extends java.util.Properties
{

    public static Options parse(String[] args) {
	Options o = new Options(args);
	return o;
    }

    //////////

    /** @serial **/
    protected String[] args;

    public boolean appendValues = false;
    
    public Options(Properties defaults, String[] args) {
	super(defaults);
	this.args = args;
	parse();
    }
    
    public Options(String[][] defaults, String[] args)
    {
	Properties def = new Properties();
	for (int i=0; i<defaults.length; ++i) {
	    if (defaults[i][1] != null)
		def.put(defaults[i][0], defaults[i][1]);
	}
	this.defaults = def;
	this.args = args;
	parse();
    }

    public Options(String[] args) {
	this.args = args;
	parse();
    }

    public Options() {}
    private static String[] noargs = {};
    public Options(String[][] defaults) {
	this(defaults, noargs);
    }
    
    public void parse() {
	parse(0);
    }

    public void parse(int startIndex)
    {
	String name = null;
	String value = null;

	int i = startIndex;
	
	if (args.length <= startIndex)
	    return;
	
	if (args[startIndex].charAt(0) != '-') {
	    // read it as a properties file
	    try {
		FileInputStream file = new FileInputStream(args[0]);
		this.load(file);
	    }
	    catch (IOException e) {
		e.printStackTrace();
	    }

	    // start reading override parameters from the command line
	    i++;
	}

	for (; i<args.length; ++i)
	{
	    String arg = args[i];
	    if (arg.charAt(0) == '-') {
		add(name, value);
		name = value = null;

		if (arg.length()==1) {
		    name = "-"; value = null;
		}
		else {
		    name = arg.substring(1);
		}
		/* for multiple-flags
		   for (int j = 1; j<arg.length(); ++j) {
		   add(name, value);
		   name = value = null;
		   // for each flag
		   name = ""+arg.charAt(j);
		   }
		*/
	    }
	    else	// not a -
	    {
		value = arg;
		add(name, value);
		name = value = null;
	    }
	}
	add(name, value);
    }
	
    protected void add(String name, String valueNew)
    {
	if (name == null) return;
	if (valueNew == null) valueNew = "";
	if (appendValues) {
	    if (this.get(name) != null) {
		String value = (String)this.get(name);
		valueNew = value + " " + valueNew;
	    }
	}
	this.put(name, valueNew);
	//		System.out.println(name + "=>" + valueNew);
    }

    public void add(int startIndex, String args[]) {
	this.args = args;
	parse(startIndex);
    }
    public void print(PrintStream out)
    {
	String key;
	String val;
	//	SortedSet set = new TreeSet(); // JDK 1.2
	// JDK 1.1 compatible
	SortedSet set = new TreeSet( new Comparator() {
	    public int compare(Object a, Object b) {
		return ((String)a).compareTo((String) b);
	    }
	} );
	Enumeration e = propertyNames();
	while (e.hasMoreElements()) {
	    Object o = e.nextElement();
	    set.add(o);
	}
	Iterator i = set.iterator();
	while (i.hasNext()) {
	    key = (String)i.next();
	    val = (String)getProperty(key);
	    out.println(key + " = " + val);
	}
    }
	
    public String getString(String key) {
	return (String)getProperty(key);
    }
	
    public int getIntLoose(String key) {
	try {
	    return Integer.parseInt( (String)getProperty(key) );
	} catch (Exception e) {
	    return 0;
	}
    }

    public double getDoubleLoose(String key) {
	try {
	    return new Double((String)getProperty(key)).doubleValue();
	} catch (Exception e) {
	    return 0;
	}
    }
	
    public int getInt(String key) throws ConfigException {
	try {
	    if (getProperty(key) == null)
		return 0;
	    return Integer.parseInt( (String)getProperty(key) );
	} catch (Exception e) {
	    throw new ConfigException(key + " cannot parse to integer ");
	}
    }

    public double getDouble(String key) throws ConfigException {
	try {
	    return new Double((String)getProperty(key)).doubleValue();
	} catch (Exception e) {
	    throw new ConfigException(key + " cannot parse to double ");
	}
    }
    public String getMandatoryProperty(final String name)
	throws ConfigException
    {
	String ans = getProperty(name);
	if ( ans == null )
	    throw new ConfigException(name + " not defined in config");
	return ans;
    }
    /**
     * The main method for running this class as a standalone application
     * @param args The argument list sent to the program.
     */

    public static void main(String args[])
    {
	Options.parse(args).list(System.out);
    }
}

