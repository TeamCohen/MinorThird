/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.mixup.*;
import org.apache.log4j.*;
import java.util.*;
import java.io.*;

/**
 * Analogous to a ClassLoader, this finds annotators by name,
 * so they can be applied to a set of labels.
 */

public abstract class AnnotatorLoader
{
	private static Logger log = Logger.getLogger(AnnotatorLoader.class);
	private static Properties redirectionProps = new Properties();
	static {
		InputStream s = ClassLoader.getSystemResourceAsStream("annotators.config");
		if (s==null) log.warn("can't find annotators.config");
		else	{
			try {	redirectionProps.load(s);	} 
			catch (IOException e) {	log.warn("error trying to load annotators.config: "+e);	}
		}
	}

	/** Find the named resource file - usually a dictionary or trie for mixup. */
	abstract public InputStream findFileResource(String fileName);

	/** Find the named resource class - usually an annotator. */
	abstract public Class findClassResource(String className);

	/** Find an annotator for the given annotationType, from the listed
	 * source.  If the source is non-null, it is located via
	 * findFileResource (if it ends in .mixup) or findClassResource
	 * (otherwise). If the source is null, the following rules are
	 * followed, in order, to find the source.
	 * <ol>
	 * <li>If the classpath contains a file "annotation.properties" that
	 * defines the annotator source for 'foo' to be 'bar', follow the
	 * rules above for source 'bar' (i.e., find a file resource 'bar'
	 * if 'bar' ends in .mixup,  and a class resource otherwise.)
	 * <li>If one can find a file resource "foo.mixup", use that as the source.
	 * <li>Use 'foo' as a class name.
	 * </ol>
	 */
	final public Annotator findAnnotator(String annotationType, String source)
	{
		String redirect;
		InputStream s;

		log.info("finding annotator for "+annotationType+" source="+source);

		if (source!=null && source.endsWith(".mixup")) {
			log.debug("non-null mixup");
			return findMixupAnnotatorFromStream(source,findFileResource(source));
		} else if (source!=null) {
			log.debug("non-null non-mixup");
			return findNativeAnnotatorFromString(source);
		} else if ((redirect = redirectionProps.getProperty(annotationType))!=null) {
			log.debug("redirected to "+redirect);
			return findAnnotator(annotationType,redirect);
		} else if ((s = findFileResource(annotationType+".mixup"))!=null) {
			log.debug("file resource "+s+" for "+annotationType+".mixup");
			return  findMixupAnnotatorFromStream(annotationType+".mixup",s);
		} else {
			log.debug("trying as class "+annotationType);
			return findNativeAnnotatorFromString(annotationType);
		}
	}

	final private Annotator findMixupAnnotatorFromStream(String fileName,InputStream s)
	{
		log.info("finding MixupProgram "+fileName+" in stream "+s);
		if (s==null) {
			log.warn("couldn't find mixup program "+fileName+" using "+this);
			return null;
		} 
		try {
			byte[] buf = new byte[s.available()];
			s.read(buf);
			MixupProgram p = new MixupProgram(new String(buf));
			return new MixupAnnotator(p);
		} catch (Mixup.ParseException e) {
			log.warn("error parsing "+fileName+": "+e);
			return null;
		} catch (IOException e) {
			log.warn("error loading "+fileName+": "+e);
			return null;
		}
	}

	final private Annotator findNativeAnnotatorFromString(String className)
	{
		try {
			Class c = findClassResource(className);
			Object o = c.newInstance();
			if (o instanceof Annotator) return (Annotator)o;
			else log.warn(c+", found from "+className+" via "+this+", is not an instance of Annotator");
		} catch (Exception e) {
			log.warn(this+" can't find class named "+className+": "+e);
		}
		return null;
	}
}
