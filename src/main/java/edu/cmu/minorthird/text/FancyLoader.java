package edu.cmu.minorthird.text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.learn.SampleClassificationProblem;
import edu.cmu.minorthird.text.learn.SampleExtractionProblem;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Configurable method of loading data objects.
 *
 * @author William Cohen
 */
public class FancyLoader
{
	private static Logger log = Logger.getLogger(FancyLoader.class);

	/** Property defining root of repository */
	public static final String REPOSITORY_PROP = "edu.cmu.minorthird.repository";
	/** Property defining location of raw data */
	public static final String DATADIR_PROP = "edu.cmu.minorthird.dataDir";
	/** Property defining location of labels added to data */
	public static final String LABELDIR_PROP = "edu.cmu.minorthird.labelDir";
	/** Property defining location of scripts for loading data */
	public static final String SCRIPTDIR_PROP = "edu.cmu.minorthird.scriptDir";
	/** When to expect sgml markup */
	public static final String SGML_MARKUP_PATTERN_PROP = "edu.cmu.minorthird.sgmlPattern";

	//
	// initialization of properties
	//

	private static Properties props = new Properties();
	private static boolean dataPropertiesFound = false;
	static {
		try {
			InputStream in = FancyLoader.class.getClassLoader().getResourceAsStream("data.properties");
			if (in != null) {
				props.load(in);
				log.debug("Loaded properties from stream "+in);
				dataPropertiesFound = true;
			} else {
				log.info("No data.properties found on classpath");
				dataPropertiesFound = false;
			}
		} catch (IOException e) {
			throw new IllegalStateException("error getting data.properties: "+e);
		}
		// override data.properties with command line, if a flag is present
		String[] ps = new String[] { 
				REPOSITORY_PROP, DATADIR_PROP, LABELDIR_PROP, SCRIPTDIR_PROP, SGML_MARKUP_PATTERN_PROP 
		}; 
		for (int i=0; i<ps.length; i++) {
			if (System.getProperty(ps[i])!=null) props.setProperty(ps[i], System.getProperty(ps[i]));
		}

		// fill in default values for DATADIR_PROP,LABELDIR_PROP,SCRIPTDIR_PROP relative to REPOSITORY_PROP
		String defaultRepositoryValue = System.getProperty(REPOSITORY_PROP,".");
		String defaultSGMLPattern = System.getProperty(SGML_MARKUP_PATTERN_PROP,".*");

		if (dataPropertiesFound) {
			if (props.getProperty(REPOSITORY_PROP)==null) {
				props.setProperty(REPOSITORY_PROP,defaultRepositoryValue);
			}
			if (props.getProperty(DATADIR_PROP)==null) {
				props.setProperty(DATADIR_PROP,props.getProperty(REPOSITORY_PROP)+"/data");
			}
			if (props.getProperty(LABELDIR_PROP)==null) {
				props.setProperty(LABELDIR_PROP,props.getProperty(REPOSITORY_PROP)+"/labels");
			}
			if (props.getProperty(SCRIPTDIR_PROP)==null) {
				props.setProperty(SCRIPTDIR_PROP,props.getProperty(REPOSITORY_PROP)+"/loaders");
			}
			if (props.getProperty(SGML_MARKUP_PATTERN_PROP)==null) {
				props.setProperty(SGML_MARKUP_PATTERN_PROP,defaultSGMLPattern);
			}
		} else {
			props.setProperty(SGML_MARKUP_PATTERN_PROP,defaultSGMLPattern);
			props.setProperty(DATADIR_PROP,defaultRepositoryValue);
			props.setProperty(LABELDIR_PROP,defaultRepositoryValue);
			props.setProperty(SCRIPTDIR_PROP,defaultRepositoryValue);
		}
		log.info("dataDir:   "+props.getProperty(DATADIR_PROP));
		log.info("labelDir:  "+props.getProperty(LABELDIR_PROP));
		log.info("scriptDir: "+props.getProperty(SCRIPTDIR_PROP));
		log.info("expect SGML in files matching '"+props.getProperty(SGML_MARKUP_PATTERN_PROP)+"'");
	};

	/** Return an array of a possible arguments to FancyLoader.loadTextLabels()
	 */
	public static Object[] getPossibleTextLabelKeys()
	{
		List<String> result = new ArrayList<String>();
		for (int i=1; i<=3; i++) {
			result.add( "sample"+i+".train");
			result.add( "sample"+i+".test" );
		}
		result.add("sample3.unlabeled");
		File dir = new File(props.getProperty(SCRIPTDIR_PROP));
		if (dir!=null) {
			String[] files = dir.list();
			for (int i=0; files!=null && i<files.length; i++) {
				result.add( files[i]); 
			}
		}
		return result.toArray();
	}

	/**
	 * Try to load a TextLabels object 'foo' in one of these ways.
	 *
	 * <ol>
	 * <li>If 'foo' is "sampleK.train" or "sampleK.test" for K=1,2,3
	 * then a hard-coded small sample TextLabels object will be returned.
	 *
	 * <li>If 'foo' is the name of a file, treat it as a bean shell
	 * script, and return the result of executing it.  
	 *
	 * <li>If script is a file stem "foo" and a file "foo.base" exists,
	 * load a textBase from "foo.base" (one document per line, line name used
	 * as document id). 
	 *
	 * <li>If script is a file stem "foo" and a directory "foo" exists,
	 * load a textBase from "foo" (one document per file). 
	 *
	 * <li>If a file named "data.properties" is on the classpath, and
	 * 'foo' is the name of a file in the value of the parameter
	 * edu.cmu.minorthird.scriptDir, as defined in data.properties,
	 * treat that file as a bean shell script, and return the result
	 * of executing it.  When the script is executed, the variables
	 * "dataDir" and "labelDir" will be bound to Files defined by
	 * edu.cmu.minorthird.dataDir and edu.cmu.minorthird.labelDir. 
	 * </ol>
	 *
	 * SGML markup in the files "foo/*" or "foo.base" will be
	 * interpreted as annotations iff "foo" matches the regex defined by
	 * edu.cmu.minorthird.sgmlPattern.  After any SGML markup is
	 * interpreted, FancyLoader will look for additional labels in
	 * "foo.labels" or "foo.mixup", in that order.
	 *
	 * @param script the name of the bean shell script, directory, file, ...
	 * @return TextLabels object
	 */
	public static TextLabels loadTextLabels(String script)
	{
		if ("sample1.train".equals(script)) return SampleExtractionProblem.trainLabels();
		else if ("sample1.test".equals(script)) return SampleExtractionProblem.testLabels();
		if ("sample2.train".equals(script)) return SampleExtractionProblem.taggerTrainLabels();
		else if ("sample2.test".equals(script)) return SampleExtractionProblem.taggerTestLabels();
		if ("sample3.train".equals(script)) return SampleClassificationProblem.trainLabels();
		else if ("sample3.test".equals(script)) return SampleClassificationProblem.testLabels();
		if ("sample3.unlabeled".equals(script)) return SampleClassificationProblem.unlabeled();

		String scriptDir = getProperty(SCRIPTDIR_PROP);
		File f = new File(new File(scriptDir), script);
		if (f.exists() && !f.isDirectory()) {
			log.info("Loading using beanShell script "+f);
			try {
				Object obj = loadObject(script);
				if (obj!=null && obj instanceof TextLabels) {
					return (TextLabels)obj;
				} else {
					throw new IllegalArgumentException(
							"script "+script+" from dir "+scriptDir+" returns an invalid object: "+obj);
				}
			} catch (bsh.EvalError e) {
				log.info("Error running beanShell script "+f+": "+e);
			} catch (IOException e) {
				log.info("Error loading bean shell file "+f+": "+e);
			}
		}

		File baseFile = new File(script + ".base");
		File baseDir = new File(script);
		boolean sgmlExpected = false;
		System.out.println("The script name is: " + script);
		String pattern = props.getProperty(SGML_MARKUP_PATTERN_PROP);
		try {
			sgmlExpected = Pattern.compile(pattern).matcher(script).matches();
			log.info("Pattern '"+pattern+"' "
					+(sgmlExpected?"does":"does not")+" match '"+script+"' so SGML markup "
					+(sgmlExpected?"is":"is not")+" expected in documents");
		} catch (PatternSyntaxException ex) {
			log.error("can't match illegal "+SGML_MARKUP_PATTERN_PROP+" regex: "+pattern);
		}

		try 
		{
			TextBase base = null;
			TextBaseLoader tbl = null;
			if (baseDir.exists() && baseDir.isDirectory()) {
				log.info("Loading documents from files in directory "+baseDir);
				tbl = new TextBaseLoader(
						TextBaseLoader.DOC_PER_FILE,sgmlExpected);
				try{
					base = tbl.load(baseDir);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else if (baseFile.exists()) {
				log.info("Loading documents from lines in file "+baseFile);
				tbl = new TextBaseLoader(
						TextBaseLoader.DOC_PER_LINE,sgmlExpected);
				try {
					base = tbl.load(baseFile);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			if (base==null) {
				log.info("Expected to find beanShell script in "+scriptDir+"/"+script
						+" or else a file named '"+baseFile+"' or a directory named '"+baseDir+"'");
				log.error("Can't find documents for key '"+script+"'");
				return null;
			}

			MonotonicTextLabels labels = sgmlExpected ? tbl.getLabels() : new BasicTextLabels(base);
			File labelFile = new File(script + ".labels");
			if (labelFile.exists()) {
				log.info("Loading annotations from "+labelFile);
				new TextLabelsLoader().importOps((MutableTextLabels)labels,base,labelFile);
				// frank: trying to fix this bug "ignoring 001.txt because token 0 not labeled in Span..."
				new TextLabelsLoader().closeLabels((MutableTextLabels)labels,TextLabelsLoader.CLOSE_ALL_TYPES);
			}			

			File mixupFile = new File(script + ".mixup");
			if (mixupFile.exists()) {
				log.info("Adding annotations with "+mixupFile);
				MixupInterpreter interp = new MixupInterpreter(new MixupProgram(mixupFile));
				interp.eval(labels);
				labels = interp.getCurrentLabels();
			}
			return labels;
		} catch (IOException ex) {
			log.error("IO error loading '"+script+"': "+ex);
		} catch (Mixup.ParseException ex) {
			log.error("Mixup error loading '"+script+"': "+ex);
		} /*catch (java.text.ParseException ex) {
	    log.error("Error loading textbase '"+script+"': "+ex);
	    }*/
		log.error("no data found for key: "+script); 
		return null;
	}

	private static Object loadObject(String script) throws bsh.EvalError,IOException
	{
		String dataDir = getProperty(DATADIR_PROP);
		String labelDir = getProperty(LABELDIR_PROP);
		String scriptDir = getProperty(SCRIPTDIR_PROP);
		log.debug("loading with dataDir: "+dataDir+" labelDir: "+labelDir+" scriptDir: "+scriptDir);
		File f =  new File(new File(scriptDir),script);
		if (!f.exists()) throw new IllegalArgumentException("can't find file "+f.getAbsolutePath());
		log.debug("loading object defined by "+f.getAbsolutePath());
		bsh.Interpreter interpreter = new bsh.Interpreter();
		interpreter.set("dataDir", new File(dataDir));
		interpreter.set("labelDir", new File(labelDir));
		return interpreter.source(f.getAbsolutePath());
	}

	public static String getProperty(String prop) {
		String v = System.getProperty(prop);
		return v!=null ? v : props.getProperty(prop);
	}

	static public void main(String[] args) throws bsh.EvalError, IOException 
	{
		Object o = FancyLoader.loadObject(args[0]);
		System.out.println("loaded "+o);
		if (o instanceof TextLabels) {
			TextBaseViewer.view((TextLabels) o );
		}
	}
}
