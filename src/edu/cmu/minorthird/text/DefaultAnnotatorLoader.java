/* Copyright 2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * Default version of AnnotatorLoader.
 *
 * @author William Cohen
 */

public class DefaultAnnotatorLoader extends AnnotatorLoader{
	
	private static Logger log=Logger.getLogger(DefaultAnnotatorLoader.class);

	/** 
	 * Find the named resource file - usually a dictionary or trie for mixup. 
	 */
	@Override
	public InputStream findFileResource(String fileName){
		log.debug("Trying to find file resource: "+fileName);
		InputStream is=null;
		is=this.getClass().getClassLoader().getResourceAsStream(fileName);
		if(is==null){
			ClassLoader.getSystemResourceAsStream(fileName);
		}
		return is;
	}

	/**
	 * Find the named resource class - usually an annotator.
	 */
	@Override
	public Class<?> findClassResource(String className){
		Class<?> clazz=null;
		log.debug("Trying to find class with exact name: "+className);
		try{
			clazz=this.getClass().getClassLoader().loadClass(className);
		}
		catch(ClassNotFoundException e){
			log.debug("Cannot find class with exact name: "+className);
		}
		if(clazz==null){
			log.debug("Trying to find class within the same package as the AnnotatorLoader: "+className);
			try{
				clazz=this.getClass().getClassLoader().loadClass(this.getClass().getPackage().getName()+"."+className);
			}
			catch(ClassNotFoundException e){
				log.debug("Cannot find class within the same package: "+className);
			}
		}
		if(clazz==null){
			log.warn("Cannot find class with name: "+className);
			return null;
		}
		else{
			return clazz;
		}
	}
}
