package edu.cmu.minorthird.util;

import org.apache.log4j.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * A lightweight command-line processing tool.
 *
 * @author William Cohen
 */
public class JointCommandLineProcessor implements CommandLineProcessor
{
	private static Logger log = Logger.getLogger(JointCommandLineProcessor.class);

	private CommandLineProcessor[] subprocessor;

	public JointCommandLineProcessor(CommandLineProcessor[] subprocessor) 
	{ 
		this.subprocessor=subprocessor; 
	}

	final public void processArguments(String[] args) 
	{
		int k = consumeArguments(args,0);
		if (k<args.length) usage("illegal argument "+args[k]);
	}

	final public int consumeArguments(String[] args,int startPos)
	{
		int pos=startPos;
		boolean somethingConsumed=true;
		while (pos<args.length && somethingConsumed) {
			if ("-help".equals(args[pos]) || "--help".equals(args[pos])) {
				help();
				pos++;
				continue;
			}
			if ("-config".equals(args[pos]) || "--config".equals(args[pos])) {
				if (pos+1<args.length) {
					BasicCommandLineProcessor.config(args[pos+1],this);
					pos+=2;
				} else {
					usage("missing argument for -config");
				}
				continue;
			}
			somethingConsumed = false;
			for (int i=0; !somethingConsumed && i<subprocessor.length; i++) {
				int k = subprocessor[i].consumeArguments(args,pos);
				//System.out.println("arg "+pos+" subprocessor "+i+" result "+k);
				if (k>0) {
					log.info("subprocessor"+i+" consumed "+k+" args at pos="+pos);
					pos += k;
					somethingConsumed = true;
				}
			}
		}
		return pos-startPos;
	}

	final public void usage(String errorMessage) 
	{
		System.out.println(errorMessage);
		usage();
	}

	final public void usage()
	{
		for (int i=0; i<subprocessor.length; i++) {
			log.info("subprocessor"+i+" usage invoked");
			subprocessor[i].usage();
		}
	}

	final public void help()
	{
		usage();
	}


	static public void main(String[] args)
	{
		CommandLineProcessor cp1 = new BasicCommandLineProcessor() {
				public void one() { System.out.println("one"); }
			};
		CommandLineProcessor cp2 = new BasicCommandLineProcessor() {
				public void two() { System.out.println("two"); }
				public void tree() { System.out.println("three"); }
			};
		CommandLineProcessor jcp = new JointCommandLineProcessor(new CommandLineProcessor[]{cp1,cp2});
		jcp.processArguments(args);
	}
}
