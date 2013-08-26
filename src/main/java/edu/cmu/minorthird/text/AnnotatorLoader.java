/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Analogous to a ClassLoader, this finds annotators by name, so they can be
 * applied to a set of labels.
 */

public abstract class AnnotatorLoader{

	private static Logger log=Logger.getLogger(AnnotatorLoader.class);

	protected static Properties redirectionProps=new Properties();

	static{
		InputStream s=ClassLoader.getSystemResourceAsStream("annotators.config");
		if(s==null){
			try{
				s=new FileInputStream("./config/annotators.config");
			}catch(IOException e){
				log.warn("Can't find annotators.config.");
				log.warn("classpath: "+System.getProperty("java.class.path"));
				log.warn(e);
			}
		}else{
			try{
				redirectionProps.load(s);
			}catch(IOException e){
				log.warn("error trying to load annotators.config: "+e);
			}
		}
	}

	/** Find the named resource file - usually a dictionary or trie for mixup. */
	abstract public InputStream findFileResource(String fileName);

	/** Find the named resource class - usually an annotator. */
	abstract public Class<?> findClassResource(String className);

	/**
	 * Find an annotator for the given annotationType, from the listed source. If
	 * the source is non-null, it attempted to be located via findFileResource and
	 * if it does not find it there it uses the findClassResource. If the source
	 * is null, the following rules are followed, in order, to find the source.
	 * <ol>
	 * <li>If the classpath contains a file "annotation.properties" that defines
	 * the annotator source for 'foo' to be 'bar', follow the rules above for
	 * source 'bar' (i.e., find a file resource 'bar' if 'bar' ends in .mixup, and
	 * a class resource otherwise.)
	 * <li>If one can find a file resource "foo.mixup", use that as the source.
	 * <li>Use 'foo' as a class name.
	 * </ol>
	 */
	final public Annotator findAnnotator(String annotationType,String source){

		log.debug("Trying to load annotator with annotation type \""+annotationType+"\" from source \""+source+"\"");
		if(source!=null){
			// see if the source is a mixup file
			if(source.endsWith(".mixup")){
				log.debug("Trying to load annotator from mixup file: "+source);
				// first use findFileResource method
				InputStream is=findFileResource(source);
				// if that fails, try to load the file directly
				if(is==null){
					try{
						is=new FileInputStream(source);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				if(is==null){
					log.warn("Cannot load annotator from source: "+source);
					return null;
				}
				else{
					return findMixupAnnotatorFromStream(source,is);
				}
			}
			// if source isn't mixup, then it's either part of an encapsulated annotator or is a class that needs to be loaded natively by java
			else{
				log.debug("Trying to load annotator from non-mixup source: "+source);
				// first check to see if the saved annotator is being served as a object from a stream such as if the annotator is encapsulated inside another annotator
				log.debug("Trying to load annotator from a file stream: "+source);
				Annotator ann=findSavedAnnotatorFromStream(source,findFileResource(source));
				if(ann==null){
					// otherwise find the native annotator from class name
					log.debug("Trying to load annotator from class name: "+source);
					ann=findNativeAnnotatorFromString(source);
				}
				if(ann==null){
					log.warn("Cannot load annotator from source: "+source);
					return null;
				}
				else{
					return ann;
				}
			}
		}
		// if the source does not lead us to the annotator check the annotation type
		else{
			log.debug("Source not provided, trying to load from annotation type");
			// Check to see if the annotation type specifies a redirection
			String redirect=redirectionProps.getProperty(annotationType);
			if(redirect!=null){
				log.debug("Redirected to "+redirect);
				return findAnnotator(annotationType,redirect);
			}
			else{
				log.debug("No redirection, assuming the annotation type is source and trying again");
				return findAnnotator(null,annotationType);
			}
		}
	}

	// This method attempts to locate an annotator named as provided using the
	// supplied input stream
	final private Annotator findSavedAnnotatorFromStream(String annotatorName,
			InputStream s){
		log.debug("Trying to find saved Annotator "+annotatorName+" from stream "+s);
		if(s!=null){
			try{
				byte[] buf=new byte[s.available()];
				s.read(buf);
				ByteArrayInputStream input=new ByteArrayInputStream(buf);
				ObjectInputStream objInput=new ObjectInputStream(input);
				return (Annotator)objInput.readObject();
			}catch(IOException e){
				e.printStackTrace();
				return null;
			}catch(ClassNotFoundException e){
				e.printStackTrace();
				return null;
			}
		}
		else{
			log.warn("Cannot find saved Annotator because InputStream is null");
			return null;
		}
	}

	final private Annotator findMixupAnnotatorFromStream(String fileName,
			InputStream s){
		log.debug("finding MixupProgram "+fileName+" in stream "+s);
		if(s==null){
			log.warn("couldn't find mixup program "+fileName+" using "+this);
			return null;
		}
		try{
			byte[] buf=new byte[s.available()];
			s.read(buf);
			MixupProgram p=new MixupProgram(new String(buf));
			return new MixupAnnotator(p);
		}catch(Mixup.ParseException e){
			log.warn("error parsing "+fileName+": "+e);
			return null;
		}catch(IOException e){
			log.warn("error loading "+fileName+": "+e);
			return null;
		}
	}

	final private Annotator findNativeAnnotatorFromString(String className){
		log.debug("Looking for native annotator class "+className);
		try{
			Class<?> c=findClassResource(className);
			Object o=c.newInstance();
			if(o instanceof Annotator)
				return (Annotator)o;
			else
				log.warn(c+", found from "+className+" via "+this+
				", is not an instance of Annotator");
		}catch(Exception e){
			log.warn(this+" can't find class named "+className+": "+e);
		}
		return null;
	}
}
