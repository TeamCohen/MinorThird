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
		return toDataset(textLabels,fe,spanProp,spanType,null);
	}

  static public Dataset toDataset(TextLabels textLabels, SpanFeatureExtractor fe,String spanProp,String spanType,String candidateType)
  {
		Span.Looper candidateLooper = 
			candidateType!=null ? textLabels.instanceIterator(candidateType) : textLabels.getTextBase().documentSpanIterator();

		// binary dataset - anything labeled as in this type is positive
		if (spanType!=null) {
			Dataset dataset = new BasicDataset();
			for (Span.Looper i=candidateLooper; i.hasNext(); ) {
				Span s = i.nextSpan();
				double classLabel = textLabels.hasType(s,spanType) ? +1 : -1;
				dataset.add( new BinaryExample( fe.extractInstance(textLabels,s), classLabel) );
			}
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
			interp.eval("import edu.cmu.minorthird.ui.*;");
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
		public void usage() {
			System.out.println("basic parameters:");
			System.out.println(" -labels REPOSITORY_KEY   load text from REPOSITORY_KEY ");
			System.out.println(" [-showLabels]            interactively view textBase loaded by -labels");
			System.out.println(" [-showResult]            interactively view final result of this operation");
		}
	}
	
	/** Parameters used by all 'train' routines. */
	public static class SaveParams extends BasicCommandLineProcessor {
		public File saveAs=null;
		public void saveAs(String fileName) { this.saveAs = new File(fileName); }
		public void usage() {
			System.out.println("save parameters:");
			System.out.println(" [-saveAs FILE]           save final result of this operation in FILE");
		}
	}

	/** Parameters encoding the 'training signal' for classification learning. */
	public static class ClassificationSignalParams extends BasicCommandLineProcessor {
		public String spanProp=null;
		public String spanType=null;
		public String candidateType=null;
		public void candidateType(String s) { this.candidateType=s; }
		public void spanProp(String s) { this.spanProp=s; }
		public void spanType(String s) { this.spanType=s; }
		public void usage() {
			System.out.println("classification 'signal' parameters:");
			System.out.println(" -spanType TYPE           create binary dataset, where candidates that");
			System.out.println("                          are marked with spanType TYPE are positive");
			System.out.println(" -spanProp PROP           create multi-class dataset, where candidates");
			System.out.println("                          are given a class determine by the spanProp PROP");
			System.out.println("                          - exactly one of spanType, spanProp should be specified");
			System.out.println(" [-candidate TYPE]        classify all spans of the given TYPE");
			System.out.println("                          - default is to classify all document spans");
		}
	}

	/** Parameters for training a classifier. */
	public static class TrainClassifierParams extends BasicCommandLineProcessor {
		public boolean showData=false;
		public ClassifierLearner learner = new Recommended.NaiveBayes();
		public SpanFeatureExtractor fe = new Recommended.DocumentFE();
		public void showData() { this.showData=true; }
		public void learner(String s) { this.learner = (ClassifierLearner)newObjectFromBSH(s,ClassifierLearner.class); }
		public CommandLineProcessor fe(String s) { 
			this.fe = (SpanFeatureExtractor)newObjectFromBSH(s,SpanFeatureExtractor.class); 
			if (this.fe instanceof CommandLineProcessor.Configurable) {
				return ((CommandLineProcessor.Configurable)this.fe).getCLP();
			} else {
				return null;
			}
		}
		public void usage() {
			System.out.println("classification training parameters:");
			System.out.println(" [-learner BSH]           Bean-shell code to create a ClassifierLearner");
			System.out.println("                          - default is \"new Recommended.NaiveBayes()\"");
			System.out.println(" [-fe FE]                 Bean-shell code to create a SpanFeatureExtractor");
			System.out.println("                          - default is \"new Recommended.DocumentFE()\"");
			System.out.println("                          - if FE implements CommandLineProcessor.Configurable then" );
			System.out.println("                            immediately following command-line arguments are passed to it");
		}
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
		public void usage() {
			System.out.println("classifier testing parameters:");
			System.out.println(" -loadFrom FILE           file containing serialized ClassifierAnnotator");
			System.out.println("                          - as learned by TrainClassifier.");
			System.out.println(" [-showData]              - interactively view the test dataset");
		}
	}

	/** Parameters for testing a stored classifier. */
	public static class TestExtractorParams extends BasicCommandLineProcessor {
		public boolean showExtractor=false;
		public File loadFrom;
		public void showExtractor() { this.showExtractor=true; }
		public void loadFrom(String s) {this.loadFrom = new File(s);}
		public void usage() {
			System.out.println("extractor testing parameters:");
			System.out.println(" -loadFrom FILE           file containing serialized Annotator, learned by TrainExtractor.");
			System.out.println(" [-showExtractor]         interactively view the loaded extractor");
		}
	}

	/** Parameters for doing train/test evaluation of a classifier. */
	public static class SplitterParams extends BasicCommandLineProcessor {
		public Splitter splitter=null;
		public TextLabels labels=null;
		public void splitter(String s) { this.splitter = toSplitter(s); }
		public void test(String s) { this.labels = FancyLoader.loadTextLabels(s); }
		public void usage() {
			System.out.println("train/test experimentation parameters:");
			System.out.println(" -splitter SPLITTER       specify splitter, e.g. -k5, -s10, -r70");
			System.out.println(" -test REPOSITORY_KEY     specify source for test data");
			System.out.println(" -test REPOSITORY_KEY     - exactly one of -splitter, -test should be specified");
		}
	}

	/** Parameters encoding the 'training signal' for extraction learning. */
	public static class ExtractionSignalParams extends BasicCommandLineProcessor {
		public String spanType=null;
		public void spanType(String s) { this.spanType=s; }
	}

	/** Parameters for training an extractor. */
	public static class TrainExtractorParams extends BasicCommandLineProcessor {
		public AnnotatorLearner learner;
		public SpanFeatureExtractor fe = null;
		public void learner(String s) { this.learner = (AnnotatorLearner)newObjectFromBSH(s,AnnotatorLearner.class); }
		public CommandLineProcessor fe(String s) { 
			this.fe = (SpanFeatureExtractor)newObjectFromBSH(s,SpanFeatureExtractor.class); 
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
			System.out.println("                            immediately following command-line arguments are passed to it");
		}
	}
}
