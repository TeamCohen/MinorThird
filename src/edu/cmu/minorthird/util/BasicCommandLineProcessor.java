package edu.cmu.minorthird.util;

import org.apache.log4j.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * A lightweight command-line processing tool.
 *
 * @author William Cohen
 */
abstract public class BasicCommandLineProcessor implements CommandLineProcessor
{
	private static Logger log = Logger.getLogger(BasicCommandLineProcessor.class);

	final public void processArguments(String[] args) 
	{
		int k = consumeArguments(args,0);
		if (k<args.length) usage("illegal argument "+args[k]);
	}

	final public int consumeArguments(String[] args,int startPos)
	{
		try {
			int pos = startPos;
			while (pos<args.length) {
				String arg = args[pos];
				if (arg.startsWith("--")) {
					arg = arg.substring(2);
				} else if (arg.startsWith("-")) {
					arg = arg.substring(1);
				} else {
					return pos-startPos;
				}
				try {
					Method m = getClass().getMethod(arg,new Class[]{});
					log.info(getClass()+" consuming '-"+arg+"'");
					Object result = m.invoke(this, new Object[]{});
					pos += 1;
					if (result instanceof CommandLineProcessor && result!=null) {
						pos += ((CommandLineProcessor)result).consumeArguments(args,pos);
					}
				} catch (NoSuchMethodException ex) {
					try {
						Method ms = getClass().getMethod(arg,new Class[]{String.class});
						if (pos+1<args.length) {
							log.info(getClass()+" consuming '-"+arg+"' '"+args[pos+1]+"'");
							Object result = ms.invoke(this, new String[]{args[pos+1]});
							pos += 2;
							if (result instanceof CommandLineProcessor) {
								pos += ((CommandLineProcessor)result).consumeArguments(args,pos);
							}
						} else {
							usage("no argument found to option '-"+arg+"'");
							pos += 1;
						}
					} catch (NoSuchMethodException ex2) {
						return pos-startPos; 
					}
				}
			}			
			return pos-startPos;
		} catch (IllegalAccessException iax) {
			usage("error: "+iax);
			return 0;
		} catch (InvocationTargetException itx) {
			usage("error: "+itx);
			return 0;
		}
	}


	public void usage(String errorMessage) 
	{
		System.out.println(errorMessage);
		usage();
	}

	/** Override this to print a meaningful usage error. */
	public void usage()
	{
		Method[] genericMethods = Object.class.getMethods();
		Set stopList = new HashSet();
		stopList.add("usage");
		stopList.add("help");
		for (int i=0; i<genericMethods.length; i++) {
			Class[] params = genericMethods[i].getParameterTypes();
			if (params.length==0 || params.length==1 && params[0].equals(String.class)) {
				stopList.add( genericMethods[i].getName() );
			}
		}
		log.info("usage for "+getClass());
		Method[] methods = getClass().getMethods();
		for (int i=0; i<methods.length; i++) {
			Class[] params = methods[i].getParameterTypes();
			if (!stopList.contains(methods[i].getName())) {
				if (params.length==0) {
					System.out.print(" [-"+methods[i].getName()+"]");
				} else if (params.length==1 && params[0].equals(String.class)) {
					System.out.print(" [-"+methods[i].getName()+" foo]");				
				}
			}
		}
		System.out.println();
	}

	/** Override this to make --help do something other than call usage */
	public void help() 
	{
		usage();
	}


	/**
	 * Test or example code.
	 * Sample output, try invoke with arguments -scoff you -laff -family -mom gerbil  -taunt
	 */
	static public void main(String[] args)
	{
		CommandLineProcessor p = new BasicCommandLineProcessor() {
				String mother="hamster";
				String father="elderberries";
				public void laff() { System.out.println("bwa ha ha ha ha ha!"); }
				public void scoff(String atWhat) { System.out.println("I scoff derisively at "+atWhat+"!"); }
				public CommandLineProcessor family() { 
					return new BasicCommandLineProcessor() {
							public void mom(String s) { mother=s; }
							public void dad(String s) { father=s; }
						};
				}
				public void taunt() { 
					System.out.println("Your mother was a "+mother+" and your father smelled of "+father+"!!!");
				}
				//public void usage() { System.out.println("usage: [-laff] [-scoff foo]"); }
			};
		p.processArguments(args);
	}
}
