package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;

import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.text.gui.TextBaseLabeler;
import edu.cmu.minorthird.text.gui.TextBaseEditor;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import org.apache.log4j.*;

import javax.swing.event.*;
import java.beans.*;
import java.lang.reflect.*;

/**
 * Minorthird-specific utilities for command line based interface routines.
 *
 * @author William Cohen
 */

class CommandLineUtil 
{
	private static Logger log = Logger.getLogger(CommandLineUtil.class);

	//
	// misc utilities
	//
	private static final String CANT_SET_ME = "can't set";

	private static String safeGetRequiredAnnotation(SpanFeatureExtractor fe)
	{
		if (fe instanceof MixupCompatible) {
			String s = ((MixupCompatible)fe).getRequiredAnnotation(); 
			// this might be called from a gui, so don't return null
			return (s==null) ? "" : s;
		} else {
			return CANT_SET_ME;
		}
	}
	private static void safeSetRequiredAnnotation(SpanFeatureExtractor fe,String s)
	{
		// this might be called from a gui, so do something reasonable with blank strings
		if ("".equals(s) || CANT_SET_ME.equals(s)) return; // no update 
		if (fe instanceof MixupCompatible) {
			((MixupCompatible)fe).setRequiredAnnotation(s); 
		} else {
			log.error("feature extractor is not MixupCompatible: "+fe);
		}
	}
	private static void safeSetAnnotatorLoader(SpanFeatureExtractor fe,String s)
	{
		if (!(fe instanceof MixupCompatible)) {
			log.error("fe is not MixupCompatible: "+fe);
		} else {
			try {
				((MixupCompatible)fe).setAnnotatorLoader(new EncapsulatingAnnotatorLoader(s));
			} catch (Exception e) {
				log.error("can't set AnnotatorLoader: "+e);
			}
		}
	}


	/** Build a sequential classification dataset from the necessary inputs. 
	 */
	static public SequenceDataset 
	toSequenceDataset(TextLabels labels,SpanFeatureExtractor fe,int historySize,String tokenProp)
	{
		NestedTextLabels safeLabels = new NestedTextLabels(labels);
		safeLabels.shadowProperty(tokenProp);

		SequenceDataset seqData = new SequenceDataset();
		seqData.setHistorySize(historySize);
		for (Span.Looper j=labels.getTextBase().documentSpanIterator(); j.hasNext(); ) {
			Span document = j.nextSpan();
			Example[] sequence = new Example[document.size()];
			for (int i=0; i<document.size(); i++) {
				Token tok = document.getToken(i);
				String value = labels.getProperty(tok, tokenProp);
				if (value==null) value = "NONE";
				Span tokenSpan = document.subSpan(i,1);
				Example example = new Example( fe.extractInstance(safeLabels,tokenSpan), new ClassLabel(value) );
				sequence[i] = example;
			}
			seqData.addSequence( sequence );
		}
		return seqData;
	}


	/** Build a classification dataset from the necessary inputs. 
	*/
  static public Dataset toDataset(TextLabels textLabels, SpanFeatureExtractor fe,String spanProp,String spanType)
	{
		return toDataset(textLabels,fe,spanProp,spanType,null);
	}

	/** Build a classification dataset from the necessary inputs. 
	 */
  static public Dataset 
	toDataset(TextLabels textLabels, SpanFeatureExtractor fe,String spanProp,String spanType,String candidateType)
  {
		// use this to print out a summary
		Map countByClass = new HashMap();

		NestedTextLabels safeLabels = new NestedTextLabels(textLabels);
		safeLabels.shadowProperty(spanProp);

		Span.Looper candidateLooper = 
			candidateType!=null ? 
			textLabels.instanceIterator(candidateType) : textLabels.getTextBase().documentSpanIterator();

		// binary dataset - anything labeled as in this type is positive
		if (spanType!=null) {
			Dataset dataset = new BasicDataset();
			for (Span.Looper i=candidateLooper; i.hasNext(); ) {
				Span s = i.nextSpan();
				int classLabel = textLabels.hasType(s,spanType) ? +1 : -1;
				String className = classLabel<0 ? ExampleSchema.NEG_CLASS_NAME : ExampleSchema.POS_CLASS_NAME;
				dataset.add( new Example( fe.extractInstance(safeLabels,s), ClassLabel.binaryLabel(classLabel)) );
				Integer cnt = (Integer)countByClass.get( className );
				if (cnt==null) countByClass.put( className, new Integer(1) );
				else countByClass.put( className, new Integer(cnt.intValue()+1) );
			}
			System.out.println("Number of examples by class: "+countByClass);
			return dataset;
		}
		// k-class dataset
		if (spanProp!=null) {
			Dataset dataset = new BasicDataset();
			for (Span.Looper i=candidateLooper; i.hasNext(); ) {
				Span s = i.nextSpan();
				String className = textLabels.getProperty(s,spanProp);
				if (className==null) {
					log.warn("no span property "+spanProp+" for document "+s.getDocumentId()+" - will be ignored");
				} else {
					dataset.add( new Example( fe.extractInstance(safeLabels,s), new ClassLabel(className)) );
				}
				Integer cnt = (Integer)countByClass.get( className );
				if (cnt==null) countByClass.put( className, new Integer(1) );
				else countByClass.put( className, new Integer(cnt.intValue()+1) );
			}
			System.out.println("Number of examples by class: "+countByClass);
			return dataset;
		}
		throw new IllegalArgumentException("either spanProp or spanType must be specified");
  }

	//
	// stuff for command-line parsing
	//

	/** Create a new object from a fragment of bean shell code,
	 * and make sure it's the correct type.
	 */
	static Object newObjectFromBSH(String s,Class expectedType)
	{
		try {
			bsh.Interpreter interp = new bsh.Interpreter();
			interp.eval("import edu.cmu.minorthird.classify.*;");
			interp.eval("import edu.cmu.minorthird.classify.experiments.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
			interp.eval("import edu.cmu.minorthird.classify.transform.*;");
			interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
			interp.eval("import edu.cmu.minorthird.text.learn.*;");
			interp.eval("import edu.cmu.minorthird.text.*;");
			interp.eval("import edu.cmu.minorthird.ui.*;");
			interp.eval("import edu.cmu.minorthird.util.*;");
			if (!s.startsWith("new"))	s = "new "+s;
			Object o = interp.eval(s);
			if (!expectedType.isInstance(o)) {
				throw new IllegalArgumentException(s+" did not produce "+expectedType);
			}
			return o;
		} catch (bsh.EvalError e) {
			log.error(e.toString());
			throw new IllegalArgumentException("error parsing '"+s+"':\n"+e);
		}
	}

    /** Create a new object from a fragment of bean shell code.
	 */
	static Object newObjectFromBSH(String s)
	{
		try {
			bsh.Interpreter interp = new bsh.Interpreter();
			interp.eval("import edu.cmu.minorthird.classify.*;");
			interp.eval("import edu.cmu.minorthird.classify.experiments.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
			interp.eval("import edu.cmu.minorthird.classify.transform.*;");
			interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
			interp.eval("import edu.cmu.minorthird.text.learn.*;");
			interp.eval("import edu.cmu.minorthird.text.*;");
			interp.eval("import edu.cmu.minorthird.ui.*;");
			if (!s.startsWith("new"))	s = "new "+s;
			Object o = interp.eval(s);
			return o;
		} catch (bsh.EvalError e) {
			log.error(e.toString());
			throw new IllegalArgumentException("error parsing '"+s+"':\n"+e);
		}
	}

	
	/** Decode splitter names.  Examples of splitter names are: k5, for
	 * 5-fold crossvalidation, s10, for stratified 10-fold
	 * crossvalidation, r70, for random split into 70% training and 30%
	 * test.  The splitter name "-help" will print a help message to
	 * System.out.
	 */
	static Splitter toSplitter(String splitterName)
	{
		if (splitterName.charAt(0)=='k') {
			int folds = StringUtil.atoi(splitterName.substring(1,splitterName.length()));
			return new CrossValSplitter(folds);
		}
		if (splitterName.charAt(0)=='r') {
			double pct = StringUtil.atoi(splitterName.substring(1,splitterName.length())) / 100.0;
			return new RandomSplitter(pct);
		}
		if ("-help".equals(splitterName)) {
			System.out.println("Valid splitter names:");
			System.out.println(" kN              N-fold cross-validation, e.g. k5 is 5-CV");
			System.out.println(" rNN             single random train-test split with NN% going to train");
			System.out.println("                 e.g, r70 is a 70%-30% split");
			System.out.println(" other           anything else is interpreted as bean shell script");
			return new RandomSplitter(0.70);
		} 
		return (Splitter) newObjectFromBSH(splitterName, Splitter.class);
	}

	//
	// useful sets of parameters that can be read from command line
	// 

	/** Basic parameters used by almost everything. */
	public static class BaseParams extends BasicCommandLineProcessor {
		public TextLabels labels=null;
		private String repositoryKey="";
		public boolean showLabels=false, showResult=false;
		public void labels(String repositoryKey) { 
			this.repositoryKey = repositoryKey;
			this.labels = (TextLabels)FancyLoader.loadTextLabels(repositoryKey); 
		}
		public void showLabels() { this.showLabels=true; }
		public void showResult() { this.showResult=true; }
		public void usage() {
			System.out.println("basic parameters:");
			System.out.println(" -labels REPOSITORY_KEY   load text from REPOSITORY_KEY");
			System.out.println(" [-showLabels]            interactively view textBase loaded by -labels");
			System.out.println(" [-showResult]            interactively view final result of this operation");
			System.out.println();
		}
		// for GUI
		//public String getLabels() { return repositoryKey; }
		//public void setLabels(String key) { labels(key); }
		public String getLabelsFilename() { return repositoryKey; }
		public void setLabelsFilename(String name) { 
			if (name.endsWith(".labels")) labels(name.substring(0,name.length()-".labels".length()));
			else labels(name);
		}
		public String getRepositoryKey() { return repositoryKey; }
		public void setRepositoryKey(String key) { labels(key); }
		public Object[] getAllowedRepositoryKeyValues() { return FancyLoader.getPossibleTextLabelKeys(); }
		//don't expose these in GUI
		//public boolean getShowLabels() { return showLabels; }
		//public void setShowLabels(boolean flag ) { showLabels=flag; }
		//public boolean getShowResult() { return showResult; }
		//public void setShowResult(boolean flag ) { showResult=flag; }
	}
	
	/** Parameters used by all 'train' routines. */
	public static class SaveParams extends BasicCommandLineProcessor {
		public File saveAs=null;
		private String saveAsName=null;
		public void saveAs(String fileName) { this.saveAs = new File(fileName); this.saveAsName=fileName; }
		public void usage() {
			System.out.println("save parameters:");
			System.out.println(" [-saveAs FILE]           save final result of this operation in FILE");
			System.out.println();
		}
		// for gui
		public String getSaveAs() { return saveAsName==null ? "n/a" : saveAsName; }
		public void setSaveAs(String s) { saveAs( "n/a".equals(s) ? null : s ); }
	}

	/** Parameters used by all 'train' routines. */
	public static class EditParams extends BasicCommandLineProcessor {
		public File editFile=null;
		private String editFileName=null;
		public String extractedType=null,trueType=null;
		public void extractedType(String s) { this.extractedType=s; }
		public void trueType(String s) { this.trueType=s; }
		public void edit(String fileName) { this.editFile = new File(fileName); this.editFileName=fileName; }
		public void usage() {
			System.out.println("edit parameters:");
			System.out.println(" [-edit FILE]             stored result of hand-edited changes to labels in FILE");
			System.out.println(" [-extracted TYPE]        debugging or labeling proposed spans of type TYPE");
			System.out.println(" [-true TYPE]             hand-corrected labels saved as type YPE");
			System.out.println();
		}
		// for gui
		public String getEditFilename() { return editFileName==null ? "n/a" : editFileName; }
		public void setEditFilename(String s) { edit( "n/a".equals(s) ? null : s ); }
		public String getExtractedType() { return extractedType==null ? "n/a" : extractedType; }
		public void setExtractedType(String s) { extractedType( "n/a".equals(s) ? null : s ); }
		public String getTrueType() { return trueType==null ? "n/a" : trueType; }
		public void setTrueType(String s) { trueType( "n/a".equals(s) ? null : s ); }
	}

	/** Parameters encoding the 'training signal' for classification learning. */
	public static class ClassificationSignalParams extends ExtractionSignalParams {
		private BaseParams base=new BaseParams();
		/** Not recommended, but required for bean-shell like visualization */
		public ClassificationSignalParams() {super();}
		public ClassificationSignalParams(BaseParams base) {super(base);}
		public String candidateType=null;
		public void candidateType(String s) { this.candidateType=s; }
		// useful abstractions
		public String getOutputType(String output) { return spanType==null ? null : output;	}
		public String getOutputProp(String output) { return spanProp==null ? null : output; }
		public void usage() {
			System.out.println("classification 'signal' parameters:");
			System.out.println(" -spanType TYPE           create binary dataset, where candidates that");
			System.out.println("                          are marked with spanType TYPE are positive");
			System.out.println(" -spanProp PROP           create multi-class dataset, where candidates");
			System.out.println("                          are given a class determine by the spanProp PROP");
			System.out.println("                          - exactly one of spanType, spanProp should be specified");
			System.out.println(" [-candidateType TYPE]    classify all spans of the given TYPE");
			System.out.println("                          - default is to classify all document spans");
			System.out.println();
		}
		// for gui
		public String getCandidateType() { return safeGet(candidateType,"top"); }
		public void setCandidateType(String s) { candidateType = safePut(s,"top"); }
		public Object[] getAllowedCandidateTypeValues() { 
			return base.labels==null ? new String[]{} : base.labels.getTypes().toArray();
		}
	}

	/** Parameters for training a classifier. */
	public static class TrainClassifierParams extends BasicCommandLineProcessor {
		public boolean showData=false;
		public ClassifierLearner learner = new Recommended.NaiveBayes();
		public SpanFeatureExtractor fe = new Recommended.DocumentFE();
		private String embeddedAnnotators = "";
		public String output="_prediction";
		public void showData() { this.showData=true; }
		public void learner(String s) { this.learner = (ClassifierLearner)newObjectFromBSH(s,ClassifierLearner.class); }
		public void output(String s) { this.output=s; }
		public CommandLineProcessor fe(String s) { 
			this.fe = (SpanFeatureExtractor)newObjectFromBSH(s,SpanFeatureExtractor.class); 
			if (this.fe instanceof CommandLineProcessor.Configurable) {
				return ((CommandLineProcessor.Configurable)this.fe).getCLP();
			} else {
				return null;
			}
		}
		public void mixup(String s) { safeSetRequiredAnnotation(fe,s);	}
		public void embed(String s) { embeddedAnnotators=s; safeSetAnnotatorLoader(fe,s);	}
    public void option(String s, Object o) {
      int i = s.indexOf("=");
      if(i > 0) {
        String ans = s.substring(0,i);
        int slen = s.length();
        String value = s.substring(i+1,slen);
			
        try {
			    //Object o = newObjectFromBSH(sub,AnnotatorLearner.class); 
			    //Object o = (Object)learner;
			    BeanInfo info = Introspector.getBeanInfo(o.getClass());
			    PropertyDescriptor[] props = info.getPropertyDescriptors();
			    
			    String pname = new String (" ");
			    Class type = null;
			    Method writer = null, reader = null;
			    int x=0, len = props.length;
			    while (!pname.equals(ans)&& x<len) {
            pname = props[x].getDisplayName();
            type = props[x].getPropertyType();
            reader = props[x].getReadMethod();
            writer = props[x].getWriteMethod();
            x++;
			    }
			    if (x == len)
            System.out.println("Did not find Classifier Option!");

			    if (type.equals(boolean.class)) {
            writer.invoke(o,new Object[]{new Boolean(value)});
            Object val = reader.invoke(o,new Object[]{});
			    } else if (type.equals(int.class)) {
            writer.invoke(o,new Object[]{new Integer(value)});
            Object val = reader.invoke(o,new Object[]{});
			    } else if (type.equals(double.class)) {
            writer.invoke(o,new Object[]{new Double(value)});
            Object val = reader.invoke(o,new Object[]{});
			    } else if (type.equals(String.class)) {
            writer.invoke(o,new Object[]{new String(value)});
            Object val = reader.invoke(o,new Object[]{});
			    }
        } catch (Exception e) {
			    System.out.println("Cannot find class");
        }
      } else {
        System.out.println ("Cannot compute option - no object defined");
      }
    }
    public void LearnerOp(String s) { 
      Object o = (Object)learner;
      option(s, o);		    
		}
    public void learnerOp(String s) { 
      LearnerOp(s); 
    }
    public void feOp(String s) {
      if(fe != null) {
        Object o = (Object)fe;
        option(s, o);			
      } else 
        System.out.println("You must define a feature extractor before setting it's options");
		}
	        
		public void usage() {
			System.out.println("classification training parameters:");
			System.out.println(" [-learner BSH]           Bean-shell code to create a ClassifierLearner");
			System.out.println("                          - default is \"new Recommended.NaiveBayes()\"");
			System.out.println(" [-showData]              interactively view the constructed training dataset");
			System.out.println(" [-fe FE]                 Bean-shell code to create a SpanFeatureExtractor");
			System.out.println("                          - default is \"new Recommended.DocumentFE()\"");
			System.out.println("                          - if FE implements CommandLineProcessor.Configurable then" );
			System.out.println("                            immediately following command-line arguments are passed to it");
			System.out.println(" [-mixup STRING]          run named mixup code before extracting features");
			System.out.println(" [-embed STRING]          embed the listed annotators in the feature extractor");
			System.out.println(" [-output STRING]         the type or property that is produced by the learned");
			System.out.println("                            ClassifierAnnotator - default is \"_prediction\"");
			System.out.println(" [-LearnerOp STRING=VALUE] Extra options that can be defined with the learner");
			System.out.println("                           - defaults are set");
			System.out.println("                           - ex: displayDatasetBeforeLearning=true");
			System.out.println(" [-feOp STRING=VALUE]      Extra options that can be defined with the feature extractor");
			System.out.println("                           - defaults are set");
			System.out.println("                           - ex: featureWindowSize=4");
			System.out.println();
		}
		// for gui
		public boolean getShowData() { return showData; }
		public void setShowData(boolean flag) { this.showData=flag; }	
		public ClassifierLearner getLearner() { return learner; }
		public void setLearner(ClassifierLearner learner) { this.learner=learner; }
		public String getOutput() { return output; } 
		public void setOutput(String s) { output(s); }
		public SpanFeatureExtractor getFeatureExtractor() { return fe; }
		public void setFeatureExtractor(SpanFeatureExtractor fe) { this.fe=fe; }
		public String getMixup() { return safeGetRequiredAnnotation(fe); }
		public void setMixup(String s) { safeSetRequiredAnnotation(fe,s); }
		public String getEmbeddedAnnotators() { return embeddedAnnotators; }		
		public void setEmbeddedAnnotators(String s) { embeddedAnnotators=s; safeSetAnnotatorLoader(fe,s); }
	}

	/** Parameters for testing a stored classifier. */
	public static class TestClassifierParams extends LoadAnnotatorParams {
		public boolean showClassifier=false;
		public boolean showData=false;
		public boolean showTestDetails=false;
		public void showClassifier() { this.showClassifier=true; }
		public void showData() { this.showData=true; }
		public void showTestDetails() { this.showTestDetails=true; }
		public void usage() {
			System.out.println("classifier testing parameters:");
			System.out.println(" -loadFrom FILE           file containing serialized ClassifierAnnotator");
			System.out.println("                          - as learned by TrainClassifier.");
			System.out.println(" [-showData]              interactively view the test dataset");
			System.out.println(" [-showTestDetails]       visualize test examples along with evaluation");
			System.out.println(" [-showClassifier]        interactively view the classifier");
			System.out.println();
		}
		// for gui
		public boolean getShowClassifier() { return showClassifier; }
		public void setShowClassifier(boolean flag) { this.showClassifier=flag; }
		public boolean getShowData() { return showData; }
		public void setShowData(boolean flag) { this.showData=flag; }
		public boolean getShowTestDetails() { return showTestDetails; }
		public void setShowTestDetails(boolean flag) { this.showTestDetails=flag; }
	}

	/** Parameters for testing a stored classifier. */
	public static class TestExtractorParams extends LoadAnnotatorParams {
		public boolean showExtractor=false;
		public void showExtractor() { this.showExtractor=true; }
		public void usage() {
			System.out.println("extractor testing parameters:");
			System.out.println(" -loadFrom FILE           file holding serialized Annotator, learned by TrainExtractor.");
			System.out.println(" [-showExtractor]         interactively view the loaded extractor");
			System.out.println();
		}
		// for gui
		public boolean getShowExtractor() { return showExtractor; }
		public void setShowExtractor(boolean flag) { this.showExtractor=flag; }
	}

	/** Parameters for testing a stored classifier. */
	public static class LoadAnnotatorParams extends BasicCommandLineProcessor {
		public File loadFrom;
		private String loadFromName;
		public void loadFrom(String s) {this.loadFrom = new File(s); this.loadFromName=s; }
		public void usage() {
			System.out.println("annotation loading parameters:");
			System.out.println(" -loadFrom FILE           file containing serialized Annotator");
			System.out.println();
		}
		// for gui
		public String getLoadFrom() { return loadFromName; }
		public void setLoadFrom(String s) { loadFrom(s); }
	}

	/** Parameters for doing train/test evaluation of a classifier. */
	public static class SplitterParams extends BasicCommandLineProcessor {
		public Splitter splitter=new RandomSplitter(0.70); 
		public TextLabels labels=null;
		public boolean showTestDetails=false;
		private String repositoryKey="";
		public void splitter(String s) { this.splitter = toSplitter(s); }
		public void showTestDetails() { this.showTestDetails = true; }
		public void test(String s) { 
		    this.repositoryKey = s;
		    this.labels = (TextLabels)FancyLoader.loadTextLabels(repositoryKey); 
			//this.labels = FancyLoader.loadTextLabels(s); 
		}
	        public void option(String s, Object o) {
		    int i = s.indexOf("=");
		    if(i > 0) {
			String ans = s.substring(0,i);
			int slen = s.length();
			String value = s.substring(i+1,slen);
			
			try {
			    //Object o = newObjectFromBSH(sub,AnnotatorLearner.class); 
			    //Object o = (Object)learner;
			    BeanInfo info = Introspector.getBeanInfo(o.getClass());
			    PropertyDescriptor[] props = info.getPropertyDescriptors();
			    
			    String pname = new String (" ");
			    Class type = null;
			    Method writer = null, reader = null;
			    int x=-1, len = props.length;
			    while (!pname.equals(ans)&& x<len) {
				x++;
				pname = props[x].getDisplayName();
				type = props[x].getPropertyType();
				reader = props[x].getReadMethod();
				writer = props[x].getWriteMethod();
				
			    }
			    if (x == len)
				System.out.println("Did not find Splitter Option!");

			    if (type.equals(boolean.class)) {
				writer.invoke(o,new Object[]{new Boolean(value)});
				Object val = reader.invoke(o,new Object[]{});
			    } else if (type.equals(int.class)) {
				writer.invoke(o,new Object[]{new Integer(value)});
				Object val = reader.invoke(o,new Object[]{});
			    } else if (type.equals(double.class)) {
				writer.invoke(o,new Object[]{new Double(value)});
				Object val = reader.invoke(o,new Object[]{});
			    } else if (type.equals(String.class)) {
				writer.invoke(o,new Object[]{new String(value)});
				Object val = reader.invoke(o,new Object[]{});
			    }

			} catch (Exception e) {
			    System.out.println("Cannot find class");
			}
			    
		    } else
			System.out.println ("Cannot compute option - no object defined");
	        }
	        public void SplitterOp(String s) { 
		    Object o = (Object)splitter;
		    option(s, o);		    
		}
		public void usage() {
			System.out.println("train/test experimentation parameters:");
			System.out.println(" -splitter SPLITTER       specify splitter, e.g. -k5, -s10, -r70");
			System.out.println(" [-showTestDetails]       visualize test examples along with evaluation");
			System.out.println(" -test REPOSITORY_KEY     specify source for test data");
			System.out.println("                          - at most one of -splitter, -test should be specified");
			System.out.println("                            default splitter is r70");
			System.out.println(" [-SplitterOp STRING=VALUE]Extra options that can be defined with the splitter");
			System.out.println("                           - ex: trainFraction=.07");
			System.out.println();
		}
	        //for the gui
	        public String getTestFilename() { return repositoryKey; }
		public void setTestFilename(String name) { 
			if (name.endsWith(".labels")) test(name.substring(0,name.length()-".labels".length()));
			else test(name);
		}
	        public String getTestKey() { return repositoryKey; }
		public void setTestKey(String key) { test(key); }
	        public Object[] getAllowedTestKeyValues() { return FancyLoader.getPossibleTextLabelKeys(); }
		public Splitter getSplitter() { return splitter; }
		public void setSplitter(Splitter splitter) { this.splitter=splitter; }
		public boolean getShowTestDetails() { return showTestDetails; }
		public void setShowTestDetails(boolean flag) { this.showTestDetails=flag; }
	}

	/** Parameters encoding the 'training signal' for extraction learning. */
	public static class ExtractionSignalParams extends BasicCommandLineProcessor {
		private BaseParams base=new BaseParams();
		/** Not recommended, but required for bean-shell like visualization */
		public ExtractionSignalParams() {;}
		public ExtractionSignalParams(BaseParams base) {this.base=base;}
		public String spanType=null;
		public String spanProp=null;
		public void spanType(String s) { this.spanType=s; }
		public void spanProp(String s) { this.spanProp=s; }
		public void usage() {
			System.out.println("extraction 'signal' parameters:");
			System.out.println(" -spanType TYPE           learn how to extract the given TYPE");
			System.out.println(" -spanProp PROP           learn how to extract spans with the given property");
			System.out.println("                          and label them with the given property");
		}
		// for gui
		public String getSpanType() { return safeGet(spanType,"n/a");}
		public void setSpanType(String t) { this.spanType = safePut(t,"n/a"); }
		public String getSpanProp() { return safeGet(spanProp,"n/a"); }
		public void setSpanProp(String p) { spanProp = safePut(p,"n/a"); }
		public Object[] getAllowedSpanTypeValues() { 
			return base.labels==null ? new String[]{} : base.labels.getTypes().toArray();
		}
		public Object[] getAllowedSpanPropValues() { 
			return base.labels==null ? new String[]{} : base.labels.getSpanProperties().toArray();
		}
		// subroutines for gui setters/getters
		protected String safeGet(String s,String def) { return s==null?def:s; }
		protected String safePut(String s,String def) { return def.equals(s)?null:s; }
	}

	/** Parameters encoding the 'training signal' for learning a token-tagger. */
	public static class TaggerSignalParams extends BasicCommandLineProcessor {
		private BaseParams base=new BaseParams();
		/** Not recommended, but required for bean-shell like visualization */
		public TaggerSignalParams() {;}
		public TaggerSignalParams(BaseParams base) {this.base=base;}
		public String tokenProp=null;
		public void tokenProp(String s) { this.tokenProp=s; }
		public void usage() {
			System.out.println("tagger 'signal' parameters:");
			System.out.println(" -tokenProp TYPE          create a sequential dataset, where tokens are");
			System.out.println("                          given the class associated with this token property");
			System.out.println();
		}
		// for gui
		public String getTokenProp() { return tokenProp==null?"n/a": tokenProp; }
		public void setTokenProp(String t) { this.tokenProp = "n/a".equals(t)?null:t; } 
		public Object[] getAllowedTokenPropValues() { 
			return base.labels==null ? new String[]{} : base.labels.getTokenProperties().toArray();
		}
	}

	/** Parameters for training an extractor. */
	public static class TrainExtractorParams extends BasicCommandLineProcessor {
	    public AnnotatorLearner learner = new Recommended.VPHMMLearner();
	    public SpanFeatureExtractor fe = null;
	    private String learnerName;
	    private String embeddedAnnotators="";
	    public String output="_prediction";
	    public void learner(String s) { 
		this.learnerName = s;
		this.learner = (AnnotatorLearner)newObjectFromBSH(s,AnnotatorLearner.class); 
		if (fe!=null) learner.setSpanFeatureExtractor(fe);
	    }
	    public void output(String s) { 
		System.out.println("We are in the output function!");
		this.output=s; }
	    public void option(String s, Object o) {
		int i = s.indexOf("=");
		if(i > 0) {
		    String ans = s.substring(0,i);
		    int slen = s.length();
		    String value = s.substring(i+1,slen);
		    
		    try {
			//Object o = newObjectFromBSH(sub,AnnotatorLearner.class); 
			//Object o = (Object)learner;
			BeanInfo info = Introspector.getBeanInfo(o.getClass());
			PropertyDescriptor[] props = info.getPropertyDescriptors();
			
			String pname = new String (" ");
			Class type = null;
			Method writer = null, reader = null;
			int x=-1, len = props.length;
			while (!pname.equals(ans)&& x<len) {
			    x++;
			    pname = props[x].getDisplayName();
			    type = props[x].getPropertyType();
			    reader = props[x].getReadMethod();
			    writer = props[x].getWriteMethod();
			    
			}
			if (x == len)
			    System.out.println("Did not find Option!");
			
			if (type.equals(boolean.class)) {
			    writer.invoke(o,new Object[]{new Boolean(value)});
			    Object val = reader.invoke(o,new Object[]{});
			} else if (type.equals(int.class)) {
			    writer.invoke(o,new Object[]{new Integer(value)});
			    Object val = reader.invoke(o,new Object[]{});
			} else if (type.equals(double.class)) {
			    writer.invoke(o,new Object[]{new Double(value)});
			    Object val = reader.invoke(o,new Object[]{});
			} else if (type.equals(String.class)) {
			    writer.invoke(o,new Object[]{new String(value)});
			    Object val = reader.invoke(o,new Object[]{});
			}
		    } catch (Exception e) {
			System.out.println("Cannot find class");
		    }
		} else
		    System.out.println ("Cannot compute option - no object defined");
	    }
	    public void LearnerOp(String s) { 
		Object o = (Object)learner;
		option(s, o);		    
	    }
	    public void feOp(String s) {
		if(fe != null) {
		    Object o = (Object)fe;
		    option(s, o);			
		} else 
		    System.out.println("You must define a Feature Extrator before setting it's options");
	    }
	    public void mixup(String s) { 
		if (fe==null) fe = learner.getSpanFeatureExtractor();
		safeSetRequiredAnnotation(fe,s);	
	    }
	    public void embed(String s) { 
		if (fe==null) fe = learner.getSpanFeatureExtractor();
		embeddedAnnotators=s; 
		safeSetAnnotatorLoader(fe,s);	
	    }
	    public CommandLineProcessor fe(String s) { 
		this.fe = (SpanFeatureExtractor)newObjectFromBSH(s,SpanFeatureExtractor.class); 
		if (learner!=null) learner.setSpanFeatureExtractor(fe);
		if (this.fe instanceof CommandLineProcessor.Configurable) {
		    return ((CommandLineProcessor.Configurable)this.fe).getCLP();
		} else {
		    return null;
		}
	    }
	    public void usage() {
		System.out.println("extraction training parameters:");
		System.out.println(" [-learner BSH]           Bean-shell code to create an AnnotatorLearner ");
		//System.out.println("                          - default is \"new Recommended.NaiveBayes()\"");
		System.out.println(" [-fe FE]                 Bean-shell code to create a SpanFeatureExtractor");
		System.out.println("                          - default is \"new Recommended.TokenFE()\"");
		System.out.println("                          - if FE implements CommandLineProcessor.Configurable then" );
		System.out.println("                            immediately following arguments are passed to it");
		System.out.println(" [-mixup STRING]          run named mixup code before extracting features");
		System.out.println(" [-embed STRING]          embed the listed annotators in the feature extractor");
		System.out.println(" [-output STRING]         the type or property that is produced by the learned");
		System.out.println("                           Annotator - default is \"_prediction\"");
		System.out.println(" [-LearnerOp STRING=VALUE] Extra options that can be defined with the learner");
		System.out.println("                           - defaults are set");
		System.out.println("                           - ex: displayDatasetBeforeLearning=true");
		System.out.println(" [-feOp STRING=VALUE]      Extra options that can be defined with the feature extractor");
		System.out.println("                           - defaults are set");
		System.out.println("                           - ex: featureWindowSize=4");
		System.out.println();
	    }
		// for gui
		public AnnotatorLearner getLearner() { return learner; }
		public void setLearner(AnnotatorLearner learner) { this.learner=learner; }
		public String getOutput() { return output; }
		public void setOutput(String s) { this.output=s; }
		public String getMixup() { 
			if (fe==null) fe = learner.getSpanFeatureExtractor();
			return safeGetRequiredAnnotation(fe);
		}
		public void setMixup(String s) { 
			if (fe==null) fe = learner.getSpanFeatureExtractor();
			safeSetRequiredAnnotation(fe,s);
		}
		public String getEmbeddedAnnotators() { 
			return embeddedAnnotators; 
		}		
		public void setEmbeddedAnnotators(String s) 
		{ 
			if (fe==null) fe = learner.getSpanFeatureExtractor();
			embeddedAnnotators=s; 
			safeSetAnnotatorLoader(fe,s); 
		}
	}

	/** Parameters for training an extractor. */
	public static class TrainTaggerParams extends BasicCommandLineProcessor {
		public SequenceClassifierLearner learner = new Recommended.VPTagLearner();
		public SpanFeatureExtractor fe = new Recommended.TokenFE();
		public String output="_prediction";
		public boolean showData=false;
		public void showData() { this.showData=true; }
		public void learner(String s) { 
			this.learner = (SequenceClassifierLearner)newObjectFromBSH(s,SequenceClassifierLearner.class); 
		}
		public void output(String s) { this.output=s; }
		public CommandLineProcessor fe(String s) { 
			this.fe = (SpanFeatureExtractor)newObjectFromBSH(s,SpanFeatureExtractor.class); 
			if (this.fe instanceof CommandLineProcessor.Configurable) {
				return ((CommandLineProcessor.Configurable)this.fe).getCLP();
			} else {
				return null;
			}
		}
		public void usage() {
			System.out.println("tagger training parameters:");
			System.out.println(" [-learner BSH]           Bean-shell code to create an SequenceClassifierLearner ");
			System.out.println(" [-showData]              interactively view the constructed training dataset");
			System.out.println(" [-fe FE]                 Bean-shell code to create a SpanFeatureExtractor");
			System.out.println("                          - default is \"new Recommended.TokenFE()\"");
			System.out.println("                          - if FE implements CommandLineProcessor.Configurable then" );
			System.out.println("                            immed. following command-line arguments are passed to it");
			System.out.println(" [-output STRING]         the type or property that is produced by the learned");
			System.out.println("                            Annotator - default is \"_prediction\"");
			System.out.println();
		}
		// for gui
		public SequenceClassifierLearner getLearner() { return learner; }
		public void setLearner(SequenceClassifierLearner learner) { this.learner=learner; }
		public String getOutput() { return output; }
		public void setOutput(String s) { this.output=s; }
		public SpanFeatureExtractor getFeatureExtractor() { return fe; }
		public void setFeatureExtractor(SpanFeatureExtractor fe) { this.fe=fe; }
		public boolean getShowData() { return showData; }
		public void setShowData(boolean flag) { this.showData=flag; }	
	}

	static public class MixupParams extends BasicCommandLineProcessor {
		public String fileName = null;
		// for command line processing
		public void mixup(String s) { fileName=s; }
		public void usage() {
			System.out.println("mixup program parameters:");
			System.out.println(" -mixup FILE              run mixup program in FILE (existing file, or name on classpath)");
			System.out.println();
		}
		// for gui
		public String getMixupProgramFilename() { return fileName; }
		public void setMixupProgramFilename(String s) { mixup(s); }
	}

	static public class AnnotatorOutputParams extends BasicCommandLineProcessor {
		private static final String[] ALLOWED_VALUES = {"minorthird","xml","strings"};
		public String format = "minorthird";
		public void format(String s) { format=s; }
	        public void toXML(String directoryName) {
		    System.out.println("Creating XML documents");
		}
		public void usage() {
			System.out.println("annotation output parameters:");
			System.out.println(" -mixup FILE              run mixup program in FILE (existing file, or name on classpath)");
			System.out.println(" -format TYPE             output results in appropriate TYPE, which must be either");
			System.out.println("                          'minorthird', 'xml', or 'strings'");
			System.out.println();
		}
		// for gui
		public String getOutputFormat() { return format; }
		public void setOutputFormat(String s) { format=s; }
		public String[] getAllowedOutputFormatValues() { return ALLOWED_VALUES; }
	}

        static public class ViewLabelsParams extends BasicCommandLineProcessor {
	        public void toXML(String key) {
		    System.out.println("Creating XML documents");

		    try {
			MutableTextLabels labels = (MutableTextLabels)FancyLoader.loadTextLabels(key);
			
			String str = null;
			
			TextLabelsLoader x = new TextLabelsLoader();
			TextBase base = labels.getTextBase();
			
			//for testing
			//File userEditedLabelFile = new File("labels.env");
			//TextBaseEditor.edit(labels, userEditedLabelFile);
			
			//(new File("./xml-" + key)).mkdir();
			File f = new File("./xml3-"+key);
			FileOutputStream fos = new FileOutputStream(f);
			PrintWriter outfile = new PrintWriter(fos);
			//int num =0;
			for (Span.Looper i = base.documentSpanIterator(); i.hasNext();)
			    {
				String doc = i.nextSpan().getDocumentId();
				//File f = new File("./xml-"+key+"/xml-" + key);
				//FileOutputStream fos = new FileOutputStream(f);
				//PrintWriter outfile = new PrintWriter(fos);
				str = x.createXMLmarkup(doc ,labels);
				outfile.println(str);
				outfile.println();
				//outfile.close();
			    } 
			outfile.close();
		    }		  		    
		    catch (Exception e) {
			e.printStackTrace();
			System.out.println("something wrong..");
			System.exit(1);}
		}
	    public void usage() {
		System.out.println("labels output parameters:");             
			System.out.println(" -toXML DIRECTORY_NAME    create documents with embedded XML tags and put in directory");
			System.out.println();
		}
	}

}
