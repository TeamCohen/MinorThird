/* Copyright 2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.mixup.*;
import org.apache.log4j.*;
import java.util.*;
import java.io.*;

/**
 * Default version of AnnotatorLoader.
 *
 * @author William Cohen
 */

public class DefaultAnnotatorLoader extends AnnotatorLoader
{
	static private Logger log = Logger.getLogger(DefaultAnnotatorLoader.class);

	/** Find the named resource file - usually a dictionary or trie for mixup. */
	public InputStream findFileResource(String fileName)
	{
		log.info("looking for file resource "+fileName);
		return DefaultAnnotatorLoader.class.getClassLoader().getResourceAsStream(fileName);
	}

	/** Find the named resource class - usually an annotator. */
	public Class findClassResource(String className)
	{
		try {
			return DefaultAnnotatorLoader.class.getClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
