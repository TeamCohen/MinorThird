package edu.cmu.minorthird.util;

import java.lang.reflect.*;
import java.util.*;

/**
 * A lightweight command-line processing tool.
 *
 * @author William Cohen
 */
public interface CommandLineProcessor 
{
	/** Loop thru the command-line arguments.
	 * Option pairs like "-foo bar" cause a call to x.foo("bar").  Options
	 * like "-gorp" cause a call to x.gorp().  Options without
	 * corresponding methods are considered errors.
	 * If an error occurs, then x.usage(msg) is called.
	 *
	 * <p> If function like -foo happens to return a non-null
	 * CommandLineProcessor, that processor is invoked on the arguments
	 * immediately after foo.
	 */
	public void processArguments(String[] args);

	/** Try to consume a the command-line argument at position i.
	 * Return the number of arguments successfully consumed.
	 */
	public int consumeArguments(String[] args,int startPos);

	/** Prints errorMessage and then calls usage(). 
	 */
	public void usage(String errorMessage); 

	/** Give usage() information. 
	 */
	public void usage();

	/** Interface for objects that can be configured with command-line arguments.
	 * Configuration for x is done by calling <code>x.getCLP().processArgs(ags).</code>
	 */
	public interface Configurable 
	{
		/** Produce a command-line processor that configures this object. */
		public CommandLineProcessor getCLP();
	}
}
