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
	/** Loop thru the command-line arguments.  Option pairs like "-foo
	 * bar" should cause a call to x.foo("bar"), where x is this
	 * CommandLineProcessor.  Options like "-gorp" cause a call to
	 * x.gorp().  Options without corresponding methods are considered
	 * errors.  If an error occurs, then an IllegalArgumentException is
	 * thrown.
	 *
	 * <p>If function like -foo happens to return a non-null
	 * CommandLineProcessor, that processor is should be invoked on the
	 * arguments immediately after foo.
	 *
	 * <p>The optional "-config FILE" should cause a Properties object
	 * to be loaded from FILE, and the property/value pairs in the
	 * Properties object to be treated as if they were option/argument
	 * pairs.  The order of lines in the property file may not be
	 * preserved, however, and duplicated keys will be ignored.
	 *
	 * <p>For instance, calling -config FILE for a FILE containing
	 *<code><pre>
	 *baz=
	 *foo=bar
	 *</pre></code>
	 * would be the same as the command options <code>-foo bar -baz</code>
	 * or possibly <code>-baz -foo bar</code>
	 * 
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
