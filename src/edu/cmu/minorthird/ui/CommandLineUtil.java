package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;


/**
 * minorthird-specific utilities for command line based interface routines.
 *
 * @author William Cohen
 */

class CommandLineUtil 
{
	private static Logger log = Logger.getLogger(CommandLineUtil.class);

	//
	// misc utilities
	//


	/**
	 * Build a classification dataset from the necessary inputs.
	 */
  static public Dataset toDataset(TextLabels textLabels, SpanFeatureExtractor fe,String spanProp,String spanType)
  {
		// binary dataset - anything labeled as in this type is positive
		if (spanType!=null) {
			Dataset dataset = new BasicDataset();
			for (Span.Looper i=textLabels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan();
				double classLabel = textLabels.hasType(s,spanType) ? +1 : -1;
				dataset.add( new BinaryExample( fe.extractInstance(textLabels,s), classLabel) );
			}
			return dataset;
		}
		// k-class dataset
		if (spanProp!=null) {
			Dataset dataset = new BasicDataset();
			for (Span.Looper i=textLabels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan();
				String className = textLabels.getProperty(s,spanProp);
				if (className==null) {
					log.warn("no span property "+spanProp+" for document "+s.getDocumentId()+" - will be ignored");
				} else {
					dataset.add( new Example( fe.extractInstance(textLabels,s), new ClassLabel(className)) );
				}
			}
			return dataset;
		}
		throw new IllegalArgumentException("either spanProp or spanType must be specified");
  }

	/** Summarize an Evaluation
	 */
	static public void summarizeEvaluation(Evaluation e)
	{
		double[] stats = e.summaryStatistics();
		String[] statNames = e.summaryStatisticNames();
		int maxLen = 0;
		for (int i=0; i<statNames.length; i++) {
			maxLen = Math.max(statNames[i].length(), maxLen); 
		}
		for (int i=0; i<statNames.length; i++) {
			System.out.print(statNames[i]+": ");
			for (int j=0; j<maxLen-statNames[i].length(); j++) System.out.print(" ");
			System.out.println(stats[i]);
		}
	}

	//
	// stuff for command-line parsing
	//

	/** Create a new object from a fragment of bean shell code,
	 * and make sure it's the correct type 
	 */
	static Object newObjectFromBSH(String s,Class expectedType)
	{
		try {
			bsh.Interpreter interp = new bsh.Interpreter();
			interp.eval("import edu.cmu.minorthird.classify.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
			interp.eval("import edu.cmu.minorthird.classify.transform.*;");
			interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
			interp.eval("import edu.cmu.minorthird.text.learn.*;");
			interp.eval("import edu.cmu.minorthird.text.*;");
			if (!s.startsWith("new"))	s = "new "+s;
			Object o = interp.eval(s);
			if (!expectedType.isInstance(o)) {
				throw new IllegalArgumentException(s+" did not produce "+expectedType);
			}
			return o;
		} catch (bsh.EvalError e) {
			throw new IllegalArgumentException("error parsing '"+s+"':\n"+e);
		}
	}

	
	/** Decode splitter names.  Examples of splitter names are: k5,
	 * for 5-fold crossvalidation, s10, for stratified 10-fold
	 * crossvalidation, r70, for random split into 70% training and 30%
	 * test.
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
		if (splitterName.charAt(0)=='s') {
			int folds = StringUtil.atoi(splitterName.substring(1,splitterName.length()));
			return new StratifiedCrossValSplitter(folds);
		}
		throw new IllegalArgumentException("illegal splitterName '"+splitterName+"'");
	}

	//
	// useful sets of parameters that can be read from command line
	// 

	/** Basic parameters used by almost everything. */
	public static class BaseParams extends BasicCommandLineProcessor {
		public MutableTextLabels labels=null;
		public boolean showLabels=false, showResult=false;
		public void labels(String repositoryKey) { 
			this.labels = (MutableTextLabels)FancyLoader.loadTextLabels(repositoryKey); 
		}
		public void showLabels() { this.showLabels=true; }
		public void showResult() { this.showResult=true; }
	}
	
	/** Parameters used by all 'train' routines. */
	public static class SaveParams extends BasicCommandLineProcessor {
		public File saveAs=null;
		public void saveAs(String fileName) { this.saveAs = new File(fileName); }
	}

	/** Parameters encoding the 'training signal' for classification learning. */
	public static class ClassificationSignalParams extends BasicCommandLineProcessor {
		public String spanProp=null;
		public String spanType=null;
		public void spanProp(String s) { this.spanProp=s; }
		public void spanType(String s) { this.spanType=s; }
	}

	/** Parameters for training a classifier. */
	public static class TrainClassifierParams extends BasicCommandLineProcessor {
		public boolean showData=false;
		public ClassifierLearner learner;
		public void showData() { this.showData=true; }
		public void learner(String s) { this.learner = (ClassifierLearner)newObjectFromBSH(s,ClassifierLearner.class); }
	}

	/** Parameters for testing a stored classifier. */
	public static class TestClassifierParams extends BasicCommandLineProcessor {
		public boolean showClassifier=false;
		public boolean showData=false;
		public File loadFrom;
		public void showClassifier() { this.showClassifier=true; }
		public void showData() { this.showData=true; }
		public void loadFrom(String s) {
			this.loadFrom = new File(s);
		}
	}

	/** Parameters for doing train/test evaluation of a classifier. */
	public static class SplitterParams extends BasicCommandLineProcessor {
		public Splitter splitter=null;
		public TextLabels labels=null;
		public void splitter(String s) { this.splitter = toSplitter(s); }
		public void test(String s) { this.labels = FancyLoader.loadTextLabels(s); }
	}

	/** Parameters encoding the 'training signal' for extraction learning. */
	public static class ExtractionSignalParams extends BasicCommandLineProcessor {
		public String spanType=null;
		public void spanType(String s) { this.spanType=s; }
	}

	/** Parameters for training an extractor. */
	public static class TrainExtractorParams extends BasicCommandLineProcessor {
		public AnnotatorLearner learner;
		public void learner(String s) { this.learner = (AnnotatorLearner)newObjectFromBSH(s,AnnotatorLearner.class); }
	}

}
