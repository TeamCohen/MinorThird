/* Copyright 2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * AnnotatorLoader which contains locally a list of Annotator
 * definitions, in the form of a list of class files, and/or mixup
 * files.
 */

public class EncapsulatingAnnotatorLoader extends AnnotatorLoader implements
Serializable{

	static private final long serialVersionUID=20080303L;

	// special serialization code
	private void readObject(java.io.ObjectInputStream in) throws IOException,
	ClassNotFoundException{
		System.out.println("Reading EncapsulatingAnnotatorLoader");
		in.defaultReadObject();
		for(Iterator<String> i=fileNameToContentsMap.keySet().iterator();i
		.hasNext();){
			String fileName=i.next();
			if(fileName.endsWith(".class")){
				String className=
					fileName.substring(0,fileName.length()-".class".length());
				try{
					System.out.println("loading class "+className);
					myClassLoader.loadClass(className);
					//ClassLoader.getSystemClassLoader().loadClass(className);
				}catch(ClassNotFoundException ex){
					ex.printStackTrace();
					log.warn("error recovering class: "+ex);
				}
			}
		}
	}

	static private Logger log=
		Logger.getLogger(EncapsulatingAnnotatorLoader.class);

	private Map<String,byte[]> fileNameToContentsMap;

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
	public EncapsulatingAnnotatorLoader(String path){
		this(true,path);
	}

	/**
	 *
	 * @param asFiles if true, elements of the path are file names.  If
	 * false, elements of the path should be resources that can be found
	 * with getResourceAsStream().  The 'asFiles=false' option was
	 * mostly provided so that file-system-independent unit tests
	 * could be written.
	 *
	 */
	public EncapsulatingAnnotatorLoader(boolean asFiles,String path){
		fileNameToContentsMap=new HashMap<String,byte[]>();
		String[] fileName=path.split(File.pathSeparator);

		for(int i=0;i<fileName.length;i++){
			try{
				File file=new File(fileName[i]);
				InputStream s=null;
				if(asFiles){
					s=new FileInputStream(file);
				}
				else{
					s=EncapsulatingAnnotatorLoader.class.getClassLoader().getResourceAsStream(fileName[i]);
				}
				byte[] contents=new byte[s.available()];
				s.read(contents);
				fileNameToContentsMap.put(file.getName(),contents);
				if(file.getName().endsWith(".mixup")){
					redirectionProps.put(file.getName().substring(0,file.getName().length()-6),file.getName());
					//fileNameToContentsMap.put(file.getName().substring(0,file.getName().length()-6),contents);
				}
			}catch(IOException e){
				throw new IllegalArgumentException("can't open file '"+fileName[i]+"': "+e);
			}
		}
		myClassLoader=new EncapsulatingClassLoader();
	}

	/** Find the named resource file - usually a dictionary or trie for mixup. */
	@Override
	public InputStream findFileResource(String fileName){
		log.info("Looking for file resource: "+fileName);
		byte[] contents=fileNameToContentsMap.get(fileName);
		if(contents!=null){
			log.info("Encapsulated resource found containing "+contents.length+" bytes");
			return new ByteArrayInputStream(contents);
		}else{
			log.info("Calling Java class loader to find resource: "+fileName);
			return this.getClass().getClassLoader().getResourceAsStream(fileName);
		}
	}

	/** Find the named resource class - usually an annotator. */
	@Override
	public Class<?> findClassResource(String className){
		try{
			Class<?> clazz=myClassLoader.loadClass(className);
			return clazz;
		}catch(ClassNotFoundException e){
			return null;
		}
	}

	public class EncapsulatingClassLoader extends ClassLoader implements Serializable{

		static private final long serialVersionUID=20080303L;

		@Override
		public Class<?> findClass(String className) throws ClassNotFoundException{
			log.info("Looking for class "+className+" with encapsulated loader");
			byte[] contents=fileNameToContentsMap.get(className+".class");
			if(contents!=null){
				log.info("Encapsulated class definition found containing "+
						contents.length+" bytes");
				try{
					return defineClass(className,contents,0,contents.length);
				}catch(NoClassDefFoundError e){
					// try again, interpreting the wrong name error message and just name it accordingly
					Pattern msgPat=Pattern.compile("\\(wrong name\\: (.+?)\\)");
					String message=e.getMessage();
					Matcher m=msgPat.matcher(message);
					if(m.find()){
						String realClassName=m.group(1).replaceAll("[\\/]+",".");
						Class<?> clazz=Class.forName(realClassName);
						return clazz;
					}
					else{
						return null;
					}
				}
			}else{
				log.info("calling default class loader to find class");
				//for some reason the EncapsulatingAnnotatorLoader doesn't seem to work; just use Class.forName
				return Class.forName(className);
				//return EncapsulatingAnnotatorLoader.class.getClassLoader().loadClass(className+".class");
			}
		}

	}
}
