/* Copyright 2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.mixup.*;
import org.apache.log4j.*;
import java.util.*;
import java.io.*;

/**
 * AnnotatorLoader which contains locally a list of Annotator
 * definitions, in the form of a list of class files, and/or mixup
 * files.
 */

public class EncapsulatingAnnotatorLoader extends AnnotatorLoader implements Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	static private Logger log = Logger.getLogger(EncapsulatingAnnotatorLoader.class);

	private Map fileNameToContentsMap;
	private ClassLoader myClassLoader;

	/**
	 * @param path Filenames, separated by the current value of
	 * File.pathSeparator, which should be "encapsulated".  Encapsulated
	 * files should be .mixup files or .class files, which will be read
	 * in when the AnnotatorLoader is created. Their contents will
	 * serialized along with it.  When finding annotators (for a
	 * TextLabels object, in a require call) an
	 * EncapsulatingAnnotatorLoader will load these files in preference
	 * to anything on the current classpath.
	 */
	public EncapsulatingAnnotatorLoader(String path)
	{
		this(true,path);
	}

	/**
	 *
	 * @param asFiles: if true, elements of the path are file names.  If
	 * false, elements of the path should be resources that can be found
	 * with getResourceAsStream().  The 'asFiles=false' option was
	 * mostly provided so that file-system-independent unit tests
	 * could be written.
	 *
	 */
	public EncapsulatingAnnotatorLoader(boolean asFiles,String path)
	{
		fileNameToContentsMap = new HashMap();
		String[] fileName = path.split(File.pathSeparator); 
		for (int i=0; i<fileName.length; i++) {
			try {
				File file = new File(fileName[i]);
				InputStream s = 
					asFiles ? new FileInputStream(file) 
					: EncapsulatingAnnotatorLoader.class.getClassLoader().getResourceAsStream(fileName[i]);
				byte[] contents = new byte[s.available()];
				s.read(contents);
				fileNameToContentsMap.put( file.getName(), contents );
			} catch (IOException e) {
				throw new IllegalArgumentException("can't open file '"+fileName[i]+"': "+e);
			}
		}
		myClassLoader = new EncapsulatingClassLoader();
	}

	/** Find the named resource file - usually a dictionary or trie for mixup. */
	public InputStream findFileResource(String fileName)
	{
		log.info("looking for file resource "+fileName+" with encapsulated loader");
		byte[] contents = (byte[])fileNameToContentsMap.get(fileName);
		if (contents!=null) {
			log.info("encapsulated resource found containing "+contents.length+" bytes");
			return new ByteArrayInputStream(contents);
		} else {
			log.info("calling default class loader to find resource");
			return EncapsulatingAnnotatorLoader.class.getClassLoader().getResourceAsStream(fileName);
		}
	}

	/** Find the named resource class - usually an annotator. */
	public Class findClassResource(String className)
	{
		try {
			return myClassLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private class EncapsulatingClassLoader extends ClassLoader implements Serializable
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;

		public Class findClass(String className) throws ClassNotFoundException
		{
			log.info("looking for class "+className+" with encapsulated loader");
			byte[] contents = (byte[])fileNameToContentsMap.get(className+".class");
			if (contents!=null) {
				log.info("encapsulated class definition found containing "+contents.length+" bytes");
				return defineClass(className,contents,0,contents.length);
			}	else {
				log.info("calling default class loader to find class");
				return EncapsulatingAnnotatorLoader.class.getClassLoader().loadClass(className);
			}
		}
	}
}
