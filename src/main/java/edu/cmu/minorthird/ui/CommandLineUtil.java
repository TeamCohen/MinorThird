package edu.cmu.minorthird.ui;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.BatchVersion;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.classify.multi.MultiClassLabel;
import edu.cmu.minorthird.classify.multi.MultiDataset;
import edu.cmu.minorthird.classify.multi.MultiExample;
import edu.cmu.minorthird.classify.sequential.SequenceClassifierLearner;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.text.EncapsulatingAnnotatorLoader;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.Token;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.MixupCompatible;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.RefUtils;
import edu.cmu.minorthird.util.StringUtil;

/**
 * Minorthird-specific utilities for command line based interface routines.
 * 
 * @author William Cohen
 */

public class CommandLineUtil{

	private static Logger log=Logger.getLogger(CommandLineUtil.class);

	//
	// misc utilities
	//
	private static final String CANT_SET_ME="can't set";

	private static String safeGetRequiredAnnotation(SpanFeatureExtractor fe){
		if(fe instanceof MixupCompatible){
			String s=((MixupCompatible)fe).getRequiredAnnotation();
			// this might be called from a gui, so don't return null
			return (s==null)?"":s;
		}else{
			return CANT_SET_ME;
		}
	}

	protected static void safeSetRequiredAnnotation(SpanFeatureExtractor fe,
			String s){
		// this might be called from a gui, so do something reasonable with blank
		// strings
		if("".equals(s)||CANT_SET_ME.equals(s))
			return; // no update
		if(fe instanceof MixupCompatible){
			((MixupCompatible)fe).setRequiredAnnotation(s);
		}else{
			log.error("feature extractor is not MixupCompatible: "+fe);
		}
	}

	private static void safeSetAnnotatorLoader(SpanFeatureExtractor fe,String s){
		if(!(fe instanceof MixupCompatible)){
			log.error("fe is not MixupCompatible: "+fe);
		}else{
			try{
				((MixupCompatible)fe)
				.setAnnotatorLoader(new EncapsulatingAnnotatorLoader(s));
			}catch(Exception e){
				log.error("can't set AnnotatorLoader: "+e);
			}
		}
	}

	/**
	 * Build a sequential classification dataset from the necessary inputs.
	 */
	static public SequenceDataset toSequenceDataset(TextLabels labels,
			SpanFeatureExtractor fe,int historySize,String tokenProp){
		NestedTextLabels safeLabels=new NestedTextLabels(labels);
		safeLabels.shadowProperty(tokenProp);

		SequenceDataset seqData=new SequenceDataset();
		seqData.setHistorySize(historySize);
		for(Iterator<Span> j=labels.getTextBase().documentSpanIterator();j
		.hasNext();){
			Span document=j.next();
			Example[] sequence=new Example[document.size()];
			for(int i=0;i<document.size();i++){
				Token tok=document.getToken(i);
				String value=labels.getProperty(tok,tokenProp);
				if(value==null)
					value="NONE";
				Span tokenSpan=document.subSpan(i,1);
				Example example=
					new Example(fe.extractInstance(safeLabels,tokenSpan),
							new ClassLabel(value));
				sequence[i]=example;
			}
			seqData.addSequence(sequence);
		}
		return seqData;
	}

	/**
	 * Build a classification dataset from the necessary inputs.
	 */
	static public Dataset toDataset(TextLabels textLabels,
			SpanFeatureExtractor fe,String spanProp,String spanType){
		return toDataset(textLabels,fe,spanProp,spanType,null);
	}

	/**
	 * Build a classification dataset from the necessary inputs.
	 */
	static public Dataset toSeqDataset(TextLabels textLabels,
			SpanFeatureExtractor fe,String spanProp,String spanType){
		NestedTextLabels safeLabels=new NestedTextLabels(textLabels);
		safeLabels.shadowProperty(spanProp);

		Iterator<Span> candidateLooper=
			textLabels.getTextBase().documentSpanIterator();

		if(spanType.equals("combined")){
			Dataset seqDataset=new BasicDataset();
			int counter=0;
			for(Iterator<Span> i=candidateLooper;i.hasNext();){

				Span s=i.next();
				System.out.println("Span1 Document ID: "+s.getDocumentId()+
						"  Counter: "+counter);
				Set<String> types=textLabels.getTypes();
				for(Iterator<String> typeIterator=types.iterator();typeIterator
				.hasNext();){
					String type=typeIterator.next();
					int classLabel1=textLabels.hasType(s,type)?+1:-1;
					if(classLabel1>0)
						seqDataset.add(new Example(fe.extractInstance(safeLabels,s),
								ClassLabel.multiLabel(type,classLabel1)));
				}
				counter++;
			}

			return seqDataset;
		}
		throw new IllegalArgumentException(
		"either spanProp or spanType must be specified");
	}

	/**
	 * Build a classification dataset from the necessary inputs.
	 */
	static public Dataset toDataset(TextLabels textLabels,
			SpanFeatureExtractor fe,String spanProp,String spanType,
			String candidateType){
		// use this to print out a summary
		Map<String,Integer> countByClass=new HashMap<String,Integer>();

		NestedTextLabels safeLabels=new NestedTextLabels(textLabels);
		safeLabels.shadowProperty(spanProp);

		Iterator<Span> candidateLooper=
			candidateType!=null?textLabels.instanceIterator(candidateType)
					:textLabels.getTextBase().documentSpanIterator();

			// binary dataset - anything labeled as in this type is positive

			if(spanType!=null){
				Dataset dataset=new BasicDataset();
				for(Iterator<Span> i=candidateLooper;i.hasNext();){
					Span s=i.next();
					int classLabel=textLabels.hasType(s,spanType)?+1:-1;
					String className=
						classLabel<0?ExampleSchema.NEG_CLASS_NAME
								:ExampleSchema.POS_CLASS_NAME;
					dataset.add(new Example(fe.extractInstance(safeLabels,s),ClassLabel
							.binaryLabel(classLabel)));
					Integer cnt=(Integer)countByClass.get(className);
					if(cnt==null)
						countByClass.put(className,new Integer(1));
					else
						countByClass.put(className,new Integer(cnt.intValue()+1));
				}
				System.out.println("Number of examples by class: "+countByClass);
				return dataset;
			}
			// k-class dataset
			if(spanProp!=null){
				Dataset dataset=new BasicDataset();
				for(Iterator<Span> i=candidateLooper;i.hasNext();){
					Span s=i.next();
					String className=textLabels.getProperty(s,spanProp);
					if(className==null){
						dataset.add(new Example(fe.extractInstance(safeLabels,s),
								new ClassLabel("NEG")));
						// log.warn("no span property "+spanProp+" for document
						// "+s.getDocumentId()+" - will be ignored");
					}else{
						dataset.add(new Example(fe.extractInstance(safeLabels,s),
								new ClassLabel(className)));
					}
					Integer cnt=(Integer)countByClass.get(className);
					if(cnt==null)
						countByClass.put(className,new Integer(1));
					else
						countByClass.put(className,new Integer(cnt.intValue()+1));
				}
				System.out.println("Number of examples by class: "+countByClass);
				return dataset;
			}
			throw new IllegalArgumentException(
			"either spanProp or spanType must be specified");
	}

	/**
	 * Build a classification dataset from the necessary inputs.
	 */
	static public MultiDataset toMultiDataset(MonotonicTextLabels textLabels,
			SpanFeatureExtractor fe,String[] multiSpanProp){
		// use this to print out a summary
		Map<String,Integer> countByClass=new HashMap<String,Integer>();

		NestedTextLabels safeLabels=new NestedTextLabels(textLabels);

		// k-class dataset
		if(multiSpanProp!=null){
			MultiDataset dataset=new MultiDataset();

			for(Iterator<Span> i=textLabels.getTextBase().documentSpanIterator();i
			.hasNext();){
				Span s=i.next();
				String[] classNames=new String[multiSpanProp.length];
				ClassLabel[] classLabels=new ClassLabel[multiSpanProp.length];

				// Creates the array of classLabels for each document
				for(int j=0;j<multiSpanProp.length;j++){
					String spanProp=multiSpanProp[j];
					classNames[j]=textLabels.getProperty(s,spanProp);
					if(classNames[j]==null){
						classLabels[j]=new ClassLabel("NEG");
					}else{
						classLabels[j]=new ClassLabel(classNames[j]);
					}
					Integer cnt=(Integer)countByClass.get(classNames[j]);
					if(cnt==null)
						countByClass.put(classNames[j],new Integer(1));
					else
						countByClass.put(classNames[j],new Integer(cnt.intValue()+1));
				}
				dataset.addMulti(new MultiExample(fe.extractInstance(safeLabels,s),
						new MultiClassLabel(classLabels)));
			}
			return dataset;
		}
		throw new IllegalArgumentException(
		"either spanProp or spanType must be specified");
	}

	//
	// stuff for command-line parsing
	//

	/**
	 * Create a new object from a fragment of bean shell code, and make sure it's
	 * the correct type.
	 */
	public static Object newObjectFromBSH(String s,Class<?> expectedType){
		try{
			bsh.Interpreter interp=new bsh.Interpreter();
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
			if(!s.startsWith("new")&&!s.startsWith("bsh.source"))
				s="new "+s;
			Object o=interp.eval(s);
			if(!expectedType.isInstance(o)){
				throw new IllegalArgumentException(s+" did not produce "+expectedType);
			}
			return o;
		}catch(bsh.EvalError e){
			log.error(e.toString());
			throw new IllegalArgumentException("error parsing '"+s+"':\n"+e);
		}
	}

	/**
	 * Create a new object from a fragment of bean shell code.
	 */
	static Object newObjectFromBSH(String s){
		try{
			bsh.Interpreter interp=new bsh.Interpreter();
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
			if(!s.startsWith("new")&&!s.startsWith("bsh.source"))
				s="new "+s;
			Object o=interp.eval(s);
			return o;
		}catch(bsh.EvalError e){
			log.error(e.toString());
			throw new IllegalArgumentException("error parsing '"+s+"':\n"+e);
		}
	}

	/**
	 * Decode splitter names. Examples of splitter names are: k5, for 5-fold
	 * crossvalidation, s10, for stratified 10-fold crossvalidation, r70, for
	 * random split into 70% training and 30% test. The splitter name "-help" will
	 * print a help message to System.out.
	 */
	static Splitter toSplitter(String splitterName){
		if(splitterName.charAt(0)=='k'){
			int folds=
				StringUtil.atoi(splitterName.substring(1,splitterName.length()));
			return new CrossValSplitter(folds);
		}
		if(splitterName.charAt(0)=='r'){
			double pct=
				StringUtil.atoi(splitterName.substring(1,splitterName.length()))/100.0;
			return new RandomSplitter(pct);
		}
		if("-help".equals(splitterName)){
			System.out.println("Valid splitter names:");
			System.out
			.println(" kN              N-fold cross-validation, e.g. k5 is 5-CV");
			System.out
			.println(" rNN             single random train-test split with NN% going to train");
			System.out.println("                 e.g, r70 is a 70%-30% split");
			System.out
			.println(" other           anything else is interpreted as bean shell script");
			return new RandomSplitter(0.70);
		}
		return (Splitter)newObjectFromBSH(splitterName,Splitter.class);
	}

	//
	// useful sets of parameters that can be read from command line
	// 

	/** Basic parameters used by almost everything. */
	public static class BaseParams extends BasicCommandLineProcessor{

		public MonotonicTextLabels labels=null;

		public String repositoryKey="";

		public boolean showLabels=false,showResult=false,classic=false;

		public void labels(String repositoryKey){
			this.repositoryKey=repositoryKey;
			this.labels=
				(MonotonicTextLabels)FancyLoader.loadTextLabels(repositoryKey);
		}

		public void showLabels(){
			this.showLabels=true;
		}

		public void showResult(){
			this.showResult=true;
		}

		public void classic(){
			this.classic=true;
		}

		public String labelsFilenameHelp=
			new String(
			"The Directory of labeled or unlabeled documents you\n                                 would like to load OR the repository key");

		@Override
		public void usage(){
			System.out.println("basic parameters:");
			System.out
			.println(" -labels REPOSITORY_KEY          "+labelsFilenameHelp);
			System.out
			.println(" [-showLabels]                   interactively view textBase loaded by -labels");
			System.out
			.println(" [-showResult]                   interactively view final result of this operation");
			System.out.println();
		}

		// for GUI
		// public String getLabels() { return repositoryKey; }
		// public void setLabels(String key) { labels(key); }
		public String getLabelsFilename(){
			return repositoryKey;
		}

		public void setLabelsFilename(String name){
			if(name.endsWith(".labels"))
				labels(name.substring(0,name.length()-".labels".length()));
			else
				labels(name);
		}

		public String getRepositoryKey(){
			return repositoryKey;
		}

		public void setRepositoryKey(String key){
			labels(key);
		}

		public Object[] getAllowedRepositoryKeyValues(){
			return FancyLoader.getPossibleTextLabelKeys();
		}

		// help button
		public String getLabelsFilenameHelp(){
			return labelsFilenameHelp;
		}
		// don't expose these in GUI
		// public boolean getShowLabels() { return showLabels; }
		// public void setShowLabels(boolean flag ) { showLabels=flag; }
		// public boolean getShowResult() { return showResult; }
		// public void setShowResult(boolean flag ) { showResult=flag; }
	}

	/** Basic parameters used by everything with a gui. */
	public static class GUIParams extends BasicCommandLineProcessor{

		public boolean useGUI;

		public void gui(){
			useGUI=true;
		}

		@Override
		public void usage(){
			System.out.println("presentation parameters:");
			System.out
			.println(" -gui                            use graphic interface to set parameters");
			System.out.println();
		}
	}

	/** Parameters used by all 'train' routines. */
	public static class SaveParams extends BasicCommandLineProcessor{

		public File saveAs=null;

		private String saveAsName=null;

		public void saveAs(String fileName){
			this.saveAs=new File(fileName);
			this.saveAsName=fileName;
		}

		public String saveHelp=
			new String("Save final result of this operation in FILE");

		@Override
		public void usage(){
			System.out.println("save parameters:");
			System.out.println(" [-saveAs FILE]                  "+saveHelp);
			System.out.println();
		}

		// for gui
		public String getSaveAs(){
			return saveAsName==null?"n/a":saveAsName;
		}

		public void setSaveAs(String s){
			saveAs("n/a".equals(s)?null:s);
		}

		// help button
		public String getSaveAsHelp(){
			return saveHelp;
		}
	}

	/** Parameters used by all 'train' routines. */
	public static class EditParams extends BasicCommandLineProcessor{

		public File editFile=null;

		private String editFileName=null;

		public String extractedType=null,trueType=null;

		public void extractedType(String s){
			this.extractedType=s;
		}

		public void trueType(String s){
			this.trueType=s;
		}

		public void edit(String fileName){
			this.editFile=new File(fileName);
			this.editFileName=fileName;
		}

		private String editFilenameHelp=
			new String("stored result of hand-edited changes to labels in FILE");

		private String extractedTypeHelp=
			new String("debugging or labeling proposed spans of type TYPE");

		private String trueTypeHelp=
			new String("hand-corrected labels saved as type TYPE");

		@Override
		public void usage(){
			System.out.println("edit parameters:");
			System.out.println(" [-edit FILE]             "+editFilenameHelp);
			System.out.println(" [-extractedType TYPE]    "+extractedTypeHelp);
			System.out.println(" [-trueType TYPE]         "+trueTypeHelp);
			System.out.println();
		}

		// for gui
		public String getEditFilename(){
			return editFileName==null?"n/a":editFileName;
		}

		public void setEditFilename(String s){
			edit("n/a".equals(s)?null:s);
		}

		public String getExtractedType(){
			return extractedType==null?"n/a":extractedType;
		}

		public void setExtractedType(String s){
			extractedType("n/a".equals(s)?null:s);
		}

		public String getTrueType(){
			return trueType==null?"n/a":trueType;
		}

		public void setTrueType(String s){
			trueType("n/a".equals(s)?null:s);
		}

		// help buttons
		public String getEditFilenameHelp(){
			return editFilenameHelp;
		}

		public String getExtractedTypeHelp(){
			return extractedTypeHelp;
		}

		public String getTrueTypeHelp(){
			return trueTypeHelp;
		}
	}

	/** Parameters encoding the 'training signal' for classification learning. */
	public static class ClassificationSignalParams extends
	BasicCommandLineProcessor{

		private BaseParams base=new BaseParams();

		protected String spanPropString=null;

		/** Not recommended, but required for bean-shell like visualization */
		public ClassificationSignalParams(){
			super();
		}

		public ClassificationSignalParams(BaseParams base){
			this.base=base;
			if(spanPropString!=null)
				this.spanProp=createSpanProp(this.spanPropString,this.base.labels);
		}

		public String spanType=null;

		public String spanProp=null;

		public void spanType(String s){
			this.spanType=s;
		}

		public void spanProp(String s){
			if(s.indexOf(',')==-1)
				this.spanProp=s;
			else{
				this.spanPropString=s;
				if(base.labels!=null)
					this.spanProp=createSpanProp(this.spanPropString,this.base.labels);
			}
		}

		public String candidateType=null;

		public void candidateType(String s){
			this.candidateType=s;
		}

		// useful abstractions
		public String getOutputType(String output){
			return spanType==null?null:output;
		}

		public String getOutputProp(String output){
			return spanProp==null?null:output;
		}

		private String spanTypeHelp=
			new String(
			"create binary dataset, where candidates that\n                                 are marked with spanType TYPE are positive");

		private String spanPropHelp=
			new String(
			"create multi-class dataset, where candidates\n                                 are given a class determine by the spanProp PROP");

		private String candidateTypeHelp=
			new String(
			"classify all spans of the given TYPE.\n                                 - default is to classify all document spans");

		@Override
		public void usage(){
			System.out.println("classification 'signal' parameters:");
			System.out.println(" -spanType TYPE                  "+spanTypeHelp);
			System.out.println(" -spanProp PROP                  "+spanPropHelp);
			System.out
			.println("                                 - exactly one of spanType, spanProp should be specified");
			System.out.println(" [-candidateType TYPE]           "+candidateTypeHelp);
			System.out.println();
		}

		// for gui
		public String getSpanType(){
			return safeGet(spanType,"n/a");
		}

		public void setSpanType(String t){
			this.spanType=safePut(t,"n/a");
		}

		public String getSpanProp(){
			return safeGet(spanProp,"n/a");
		}

		public void setSpanProp(String p){
			spanProp=safePut(p,"n/a");
		}

		public Object[] getAllowedSpanTypeValues(){
			return base.labels==null?new String[]{}:base.labels.getTypes().toArray();
		}

		public Object[] getAllowedSpanPropValues(){
			return base.labels==null?new String[]{}:base.labels.getSpanProperties()
					.toArray();
		}

		// subroutines for gui setters/getters
		protected String safeGet(String s,String def){
			return s==null?def:s;
		}

		protected String safePut(String s,String def){
			return def.equals(s)?null:s;
		}

		public String getCandidateType(){
			return safeGet(candidateType,"top");
		}

		public void setCandidateType(String s){
			candidateType=safePut(s,"top");
		}

		public Object[] getAllowedCandidateTypeValues(){
			return getAllowedSpanTypeValues();
			// return base.labels==null ? new String[]{} :
			// base.labels.getTypes().toArray();
		}

		// help buttons
		public String getSpanTypeHelp(){
			return spanTypeHelp;
		}

		public String getSpanPropHelp(){
			return spanPropHelp;
		}

		public String getCandidateTypeHelp(){
			return candidateTypeHelp;
		}
	}

	/** Parameters for training a classifier. */
	public static class TrainClassifierParams extends BasicCommandLineProcessor{

		public boolean showData=false;

		public String learnerName="NaiveBayes()";

		public ClassifierLearner learner=new Recommended.NaiveBayes();

		public SpanFeatureExtractor fe=new Recommended.DocumentFE();

		private String embeddedAnnotators="";

		public String output="_prediction";

		public void showData(){
			this.showData=true;
		}

		public void learner(String s){
			this.learnerName=s;
			this.learner=
				(ClassifierLearner)newObjectFromBSH(s,ClassifierLearner.class);
		}

		public void output(String s){
			this.output=s;
		}

		public CommandLineProcessor fe(String s){
			this.fe=
				(SpanFeatureExtractor)newObjectFromBSH(s,SpanFeatureExtractor.class);
			if(this.fe instanceof CommandLineProcessor.Configurable){
				return ((CommandLineProcessor.Configurable)this.fe).getCLP();
			}else{
				return null;
			}
		}

		public void mixup(String s){
			safeSetRequiredAnnotation(fe,s);
		}

		public void embed(String s){
			embeddedAnnotators=s;
			safeSetAnnotatorLoader(fe,s);
		}

		public void other(String s){
			option(s);
		}

		public void option(String s){
			Object o=this;
			RefUtils.modify(o,s);
		}

		public void LearnerOp(String s){
			Object o=(Object)learner;
			RefUtils.modify(o,s);
		}

		public void learnerOp(String s){
			LearnerOp(s);
		}

		public void feOp(String s){
			if(fe!=null){
				Object o=(Object)fe;
				RefUtils.modify(o,s);
			}else
				System.out
				.println("You must define a feature extractor before setting it's options");
		}

		private String learnerHelp=
			new String(
			"Bean-shell code to create a ClassifierLearner\n                                 - default is \"new Recommended.NaiveBayes()\"");

		private String showDataHelp=
			new String("interactively view the constructed training dataset");

		private String feHelp=
			new String(
			"Bean-shell code to create a SpanFeatureExtractor\n                                 - default is \"new Recommended.DocumentFE()\" ");

		private String mixupHelp=
			new String("run named mixup code before extracting features");

		private String embedHelp=
			new String("embed the listed annotators in the feature extractor");

		private String outputHelp=
			new String(
			"the type or property that is produced by the learned\n                                 ClassifierAnnotator - default is \"_prediction\"");

		@Override
		public void usage(){
			System.out.println("classification training parameters:");
			System.out.println(" [-learner BSH]                  "+learnerHelp);
			System.out.println(" [-showData]                     "+showDataHelp);
			System.out.println(" [-fe FE]                        "+feHelp);
			System.out
			.println("                                 - if FE implements CommandLineProcessor.Configurable then");
			System.out
			.println("                                   immediately following command-line arguments are passed to it");
			System.out.println(" [-mixup STRING]                 "+mixupHelp);
			System.out.println(" [-embed STRING]                 "+embedHelp);
			System.out.println(" [-output STRING]                "+outputHelp);
			System.out
			.println(" [-LearnerOp STRING=VALUE]       Extra options that can be defined with the learner");
			System.out
			.println("                                  - defaults are set");
			System.out
			.println("                                  - ex: displayDatasetBeforeLearning=true");
			System.out
			.println(" [-feOp STRING=VALUE]            Extra options that can be defined with the feature extractor");
			System.out
			.println("                                  - defaults are set");
			System.out
			.println("                                  - ex: featureWindowSize=4");
			System.out.println();
		}

		// for gui
		public boolean getShowData(){
			return showData;
		}

		public void setShowData(boolean flag){
			this.showData=flag;
		}

		public ClassifierLearner getLearner(){
			return learner;
		}

		public void setLearner(ClassifierLearner learner){
			learnerName=learner.getClass().toString();
			if(learner instanceof BatchVersion){
				learnerName=
					((((BatchVersion)learner).getInnerLearner())).toString()+"()";
			}
			System.out.println(learnerName);
			this.learner=learner;
		}

		public String getOutput(){
			return output;
		}

		public void setOutput(String s){
			output(s);
		}

		public SpanFeatureExtractor getFeatureExtractor(){
			return fe;
		}

		public void setFeatureExtractor(SpanFeatureExtractor fe){
			this.fe=fe;
		}

		public String getMixup(){
			return safeGetRequiredAnnotation(fe);
		}

		public void setMixup(String s){
			safeSetRequiredAnnotation(fe,s);
		}

		public String getEmbeddedAnnotators(){
			return embeddedAnnotators;
		}

		public void setEmbeddedAnnotators(String s){
			embeddedAnnotators=s;
			safeSetAnnotatorLoader(fe,s);
		}

		// help buttons
		public String getLearnerHelp(){
			return learnerHelp;
		}

		public String getShowDataHelp(){
			return showDataHelp;
		}

		public String getFeatureExtractorHelp(){
			return feHelp;
		}

		public String getMixupHelp(){
			return mixupHelp;
		}

		public String getEmbeddedAnnotatorsHelp(){
			return embedHelp;
		}

		public String getOutputHelp(){
			return outputHelp;
		}
	}

	/** Parameters for testing a stored classifier. */
	public static class TestClassifierParams extends LoadAnnotatorParams{

		public boolean showClassifier=false;

		public boolean showData=false;

		public boolean showTestDetails=true;

		public void showClassifier(){
			this.showClassifier=true;
		}

		public void showData(){
			this.showData=true;
		}

		public void showTestDetails(String bool){
			this.showTestDetails=(new Boolean(bool)).booleanValue();
		}

		private String loadFromHelp=
			new String(
			"file containing serialized ClassifierAnnotator\n - as learned by TrainClassifier");

		private String showDataHelp=
			new String(
			"interactively view the test dataset in a new window when you run the experiment");

		private String showTestDetailsHelp=
			new String(
			"visualize test examples along with evaluation\n -Default: true");

		private String showClassifierHelp=
			new String(
			"interactively view the classifier in a new window when you run the experiment");

		@Override
		public void usage(){
			System.out.println("classifier testing parameters:");
			System.out.println(" -loadFrom FILE           "+loadFromHelp);
			System.out.println(" [-showData]              "+showDataHelp);
			System.out.println(" [-showTestDetails BOOL]  "+showTestDetailsHelp);
			System.out.println(" [-showClassifier]        "+showClassifierHelp);
			System.out.println();
		}

		// for gui
		public boolean getShowClassifier(){
			return showClassifier;
		}

		public void setShowClassifier(boolean flag){
			this.showClassifier=flag;
		}

		public boolean getShowData(){
			return showData;
		}

		public void setShowData(boolean flag){
			this.showData=flag;
		}

		public boolean getShowTestDetails(){
			return showTestDetails;
		}

		public void setShowTestDetails(boolean flag){
			this.showTestDetails=flag;
		}

		// help buttons
		@Override
		public String getLoadFromHelp(){
			return loadFromHelp;
		}

		public String getShowDataHelp(){
			return showDataHelp;
		}

		public String getShowTestDetailsHelp(){
			return showTestDetailsHelp;
		}

		public String getShowClassifierHelp(){
			return showClassifierHelp;
		}
	}

	/** Parameters for testing a stored classifier. */
	public static class TestExtractorParams extends LoadAnnotatorParams{

		public boolean showExtractor=false;

		public void showExtractor(){
			this.showExtractor=true;
		}

		@Override
		public void usage(){
			System.out.println("extractor testing parameters:");
			System.out
			.println(" -loadFrom FILE           file holding serialized Annotator, learned by TrainExtractor.");
			System.out
			.println(" [-showExtractor]         interactively view the loaded extractor");
			System.out.println();
		}

		// for gui
		public boolean getShowExtractor(){
			return showExtractor;
		}

		public void setShowExtractor(boolean flag){
			this.showExtractor=flag;
		}
	}

	/*
	 * public static class LoadMultiAnnotatorParams extends LoadAnnotatorParams {
	 * public boolean cross = false; public void cross() { cross=true; } // for
	 * gui public boolean getCross() { return cross; } public void
	 * setCross(boolean cross) {this.cross = cross;} }
	 */

	/** Parameters for testing a stored classifier. */
	public static class LoadAnnotatorParams extends BasicCommandLineProcessor{

		public File loadFrom;

		private String loadFromName;

		public void loadFrom(String s){
			this.loadFrom=new File(s);
			this.loadFromName=s;
		}

		private String loadFromHelp=
			new String("file containing serialized Annotator");

		@Override
		public void usage(){
			System.out.println("annotation loading parameters:");
			System.out.println(" -loadFrom FILE           "+loadFromHelp);
			System.out.println();
		}

		// for gui
		public String getLoadFrom(){
			return loadFromName;
		}

		public void setLoadFrom(String s){
			loadFrom(s);
		}

		// help button
		public String getLoadFromHelp(){
			return loadFromHelp;
		}
	}

	/** Parameters encoding the 'training signal' for extraction learning. */
	public static class OnlineSignalParams extends BasicCommandLineProcessor{

//		private OnlineBaseParams base=new OnlineBaseParams();

		/** Not recommended, but required for bean-shell like visualization */
		public OnlineSignalParams(){
			;
		}

		public OnlineSignalParams(OnlineBaseParams base){
//			this.base=base;
		}

		public String spanType=null;

		public void spanType(String s){
			this.spanType=s;
		}

		public String getOutputType(String output){
			return spanType==null?null:output;
		}

		public String spanTypeHelp=
			new String("learn how to extract the given TYPE");

		@Override
		public void usage(){
			System.out.println("extraction 'signal' parameters:");
			System.out.println(" -spanType TYPE           "+spanTypeHelp);
		}

		// for gui
		public String getSpanType(){
			return spanType;
		}

		public void setSpanType(String t){
			this.spanType=t;
		}

		// help button
		public String getSpanTypeHelp(){
			return spanTypeHelp;
		}
	}

	/** Basic parameters used by almost everything. */
	public static class OnlineBaseParams extends BasicCommandLineProcessor{

		public MutableTextLabels labeledData=null;

		public String repositoryKey="";

		public boolean showLabels=false,showResult=false;

		public void labeledData(String repositoryKey){
			this.repositoryKey=repositoryKey;
			this.labeledData=
				(MutableTextLabels)FancyLoader.loadTextLabels(repositoryKey);
		}

		public void showLabels(){
			this.showLabels=true;
		}

		public void showResult(){
			this.showResult=true;
		}

		private String labeledDataHelp=
			new String("REPOSITORY_KEY or directory that contains labeledData");

		private String showLabelsHelp=
			new String("interactively view textBase loaded by -labels");

		private String showResultHelp=
			new String("interactively view final result of this operation");

		@Override
		public void usage(){
			System.out.println("basic parameters:");
			System.out.println(" -labeledData                  "+labeledDataHelp);
			System.out.println(" [-showLabels]                 "+showLabelsHelp);
			System.out.println(" [-showResult]                 "+showResultHelp);
			System.out.println();
		}

		// for GUI
		// public String getLabels() { return repositoryKey; }
		// public void setLabels(String key) { labels(key); }
		public String getLabelsFilename(){
			return repositoryKey;
		}

		public void setLabelsFilename(String name){
			if(name.endsWith(".labels"))
				labeledData(name.substring(0,name.length()-".labels".length()));
			else
				labeledData(name);
		}

		public String getRepositoryKey(){
			return repositoryKey;
		}

		public void setRepositoryKey(String key){
			labeledData(key);
		}

		public Object[] getAllowedRepositoryKeyValues(){
			return FancyLoader.getPossibleTextLabelKeys();
		}

		// help button
		public String getLabelsFilenameHelp(){
			return labeledDataHelp;
		}
		// don't expose these in GUI
		// public boolean getShowLabels() { return showLabels; }
		// public void setShowLabels(boolean flag ) { showLabels=flag; }
		// public boolean getShowResult() { return showResult; }
		// public void setShowResult(boolean flag ) { showResult=flag; }
	}

	/** Parameters for Adding Examples to a Online Classifier */
	public static class OnlineLearnerParams extends BasicCommandLineProcessor{

		public MutableTextLabels data=null;

		public MonotonicTextLabels labels=null;

		public String repositoryKey=null;

		public boolean experiment=false;

		public File loadFrom;

		private String loadFromName;

		public void loadFrom(String s){
			this.loadFrom=new File(s);
			this.loadFromName=s;
		}

		public void data(String dirName){
			this.repositoryKey=dirName;
			this.data=(MutableTextLabels)FancyLoader.loadTextLabels(dirName);
			this.labels=(MonotonicTextLabels)FancyLoader.loadTextLabels(dirName);
		}

		public void experiment(){
			this.experiment=true;
		}

		@Override
		public void usage(){
			System.out.println("Online Learning loading parameters:");
			System.out
			.println(" -loadFrom FILE           file containing serialized Annotator");
			System.out
			.println(" -data DIRECTORY        Directory containing new data you would like to add");
			System.out
			.println(" -experiment            Perform an experiment with labeled data -");
			System.out
			.println("                        See if Online Learning give you an advantage");
			System.out.println();
		}

		// for gui
		public String getLoadFrom(){
			return loadFromName;
		}

		public void setLoadFrom(String s){
			loadFrom(s);
		}

		public String getLabelsFilename(){
			return repositoryKey;
		}

		public void setLabelsFilename(String name){
			if(name.endsWith(".labels"))
				data(name.substring(0,name.length()-".labels".length()));
			else
				data(name);
		}
	}

	/** Parameters for doing train/test evaluation of a classifier. */
	public static class SplitterParams extends BasicCommandLineProcessor{

		public Splitter splitter=new RandomSplitter(0.70);

		public MonotonicTextLabels labels=null;

		public boolean showTestDetails=true;

		private String repositoryKey="";

		public void splitter(String s){
			this.splitter=toSplitter(s);
		}

		public void showTestDetails(String bool){
			this.showTestDetails=(new Boolean(bool)).booleanValue();
		}

		public void test(String s){
			this.repositoryKey=s;
			this.labels=
				(MonotonicTextLabels)FancyLoader.loadTextLabels(repositoryKey);
			// this.labels = FancyLoader.loadTextLabels(s);
		}

		public void SplitterOp(String s){
			Object o=(Object)splitter;
			RefUtils.modify(o,s);
		}

		private String splitterHelp=
			new String(
			"The Splitter you would like to use to divide your training and testing data");

		private String showTestDetailsHelp=
			new String("visualize test examples along with evaluation");

		private String testHelp=
			new String(
			"Specify directory or repository key of test data\n  -Note: splitter will be ignored with this option");

		@Override
		public void usage(){
			System.out.println("train/test experimentation parameters:");
			System.out
			.println(" -splitter SPLITTER               specify splitter, e.g. -k5, -s10, -r70");
			System.out
			.println("                                  - At most one of -splitter, -test should be specified.");
			System.out
			.println("                                  - The default splitter is r70.");
			System.out.println(" [-showTestDetails true|false]    "+
					showTestDetailsHelp);
			System.out
			.println(" -test REPOSITORY_KEY             specify source for test data");
			System.out
			.println(" [-SplitterOp STRING=VALUE]       Extra options that can be defined with the splitter");
			System.out
			.println("                                  - ex: trainFraction=.07");
			System.out.println();
		}

		// for the gui
		public String getTestFilename(){
			return repositoryKey;
		}

		public void setTestFilename(String name){
			if(name.endsWith(".labels"))
				test(name.substring(0,name.length()-".labels".length()));
			else
				test(name);
		}

		public String getTestKey(){
			return repositoryKey;
		}

		public void setTestKey(String key){
			test(key);
		}

		public Object[] getAllowedTestKeyValues(){
			return FancyLoader.getPossibleTextLabelKeys();
		}

		public Splitter getSplitter(){
			return splitter;
		}

		public void setSplitter(Splitter splitter){
			this.splitter=splitter;
		}

		public boolean getShowTestDetails(){
			return showTestDetails;
		}

		public void setShowTestDetails(boolean flag){
			this.showTestDetails=flag;
		}

		// help buttons
		public String getSplitterHelp(){
			return splitterHelp;
		}

		public String getShowTestDetailsHelp(){
			return showTestDetailsHelp;
		}

		public String getTestFilenameHelp(){
			return testHelp;
		}
	}

	/** Creates a Mixup program that defines a SpanProp from a list of Span Types */
	public static String createSpanProp(String spanTypes,MonotonicTextLabels labels){

		if(spanTypes.indexOf(",")==-1){
			return spanTypes;
		}

		StringBuilder b=new StringBuilder();
		b.append("provide createSpanPropMixup;\n");

		String property=null;
		int catIndex=spanTypes.indexOf(":");
		if(catIndex<0){
			property=new String("_property");
		}
		else{
			property=spanTypes.substring(0,catIndex);
		}

		String[] types=spanTypes.substring(catIndex+1,spanTypes.length()).split(",");
		
		for(int i=0;i<types.length;i++){
			for(Iterator<Span> sl=labels.instanceIterator(types[i]);sl.hasNext();){
				Span s=sl.next();
				labels.setProperty(s,property,types[i]);
				b.append("defSpanProp ").append(property).append(":").append(types[i]).append("=: ... [@").append(types[i]).append("] ...;\n");
			}
		}
		
		try{
			MixupProgram prog=new MixupProgram(b.toString());
			MixupInterpreter interpreter=new MixupInterpreter(prog);
			interpreter.eval(labels);
		}catch(Exception e){
			e.printStackTrace();
		}
		return property;
	}

	private static String[] createMultiSpanProp(File multiSpanProps,
			MonotonicTextLabels labels) throws Exception{
		ProgressCounter pc=
			new ProgressCounter("loading file "+multiSpanProps.getName(),"line");
		LineNumberReader in=null;
		try{
			in=new LineNumberReader(new FileReader(multiSpanProps));
		}catch(Exception e){
			System.out.println("Cannot open multiSpanProp file");
			e.printStackTrace();
		}
		String line;
		List<String> spanPropList=new ArrayList<String>();
		String[] spanProps;
		if(in!=null){
			while((line=in.readLine())!=null){
				spanPropList.add(createSpanProp(line,labels));
				pc.progress();
			}
			spanProps=(String[])spanPropList.toArray(new String[0]);
			return spanProps;
		}else
			return null;
	}

	/** Parameters encoding the 'training signal' for extraction learning. */
	public static class ExtractionSignalParams extends BasicCommandLineProcessor{

		private BaseParams base=new BaseParams();

		protected String spanPropString=null;

		/** Not recommended, but required for bean-shell like visualization */
		public ExtractionSignalParams(){
			;
		}

		public ExtractionSignalParams(BaseParams base){
			this.base=base;
			if(spanPropString!=null)
				this.spanProp=createSpanProp(this.spanPropString,this.base.labels);
		}

		public String spanType=null;

		public String spanProp=null;

		public void spanType(String s){
			this.spanType=s;
		}

		public void spanProp(String s){
			if(s.indexOf(',')==-1)
				this.spanProp=s;
			else{
				this.spanPropString=s;
				if(base.labels!=null)
					this.spanProp=createSpanProp(this.spanPropString,this.base.labels);
			}
		}

		private String spanTypeHelp=
			new String("learn how to extract the given TYPE");

		private String spanPropHelp=
			new String(
			"learn how to extract spans with the given property\n and label them with the given property");

		@Override
		public void usage(){
			System.out.println("extraction 'signal' parameters:");
			System.out.println(" -spanType TYPE           "+spanTypeHelp);
			System.out.println(" -spanProp PROP           "+spanPropHelp);
			System.out.println(" -spanProp PROPERTY:SpanType1,SpanType2");
			System.out
			.println("                          learn how to extract spans with the named property and span types");
			System.out
			.println("                          and label them with the name property");
		}

		// for gui
		public String getSpanType(){
			return safeGet(spanType,"n/a");
		}

		public void setSpanType(String t){
			this.spanType=safePut(t,"n/a");
		}

		public String getSpanProp(){
			return safeGet(spanProp,"n/a");
		}

		public void setSpanProp(String p){
			spanProp=safePut(p,"n/a");
		}

		public Object[] getAllowedSpanTypeValues(){
			return base.labels==null?new String[]{}:base.labels.getTypes().toArray();
		}

		public Object[] getAllowedSpanPropValues(){
			return base.labels==null?new String[]{}:base.labels.getSpanProperties()
					.toArray();
		}

		// subroutines for gui setters/getters
		protected String safeGet(String s,String def){
			return s==null?def:s;
		}

		protected String safePut(String s,String def){
			return def.equals(s)?null:s;
		}

		// help buttons
		public String getSpanTypeHelp(){
			return spanTypeHelp;
		}

		public String getSpanPropHelp(){
			return spanPropHelp;
		}
	}

	/** Parameters encoding the 'training signal' for extraction learning. */
	public static class MultiClassificationSignalParams extends
	BasicCommandLineProcessor{

		private BaseParams base=new BaseParams();

		public File multiSpanPropFile;

		public String multiSpanPropFileName;

		public String[] multiSpanProp;

		public boolean cross=false;

		/** Not recommended, but required for bean-shell like visualization */
		public MultiClassificationSignalParams(){
			;
		}

		public MultiClassificationSignalParams(BaseParams base){
			this.base=base;
			try{
				if(multiSpanPropFile!=null)
					this.multiSpanProp=
						createMultiSpanProp(multiSpanPropFile,this.base.labels);
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		public void multiSpanPropFile(String s){
			multiSpanPropFileName=s;
			File f=new File(s);
			multiSpanPropFile=f;
			try{
				multiSpanProp=createMultiSpanProp(f,this.base.labels);
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		public void cross(){
			this.cross=true;
		}

		private String multiSpanPropFileHelp=
			new String(
			"File that contains your definition of spanProperty\n -Format(1 spanProp per line): SPAN_PROP_NAME:SPAN_TYPE1,SPAN_TYP2,....");

		private String crossHelp=
			new String(
			"Classify dataset and add the classification as features to each document");

		@Override
		public void usage(){
			System.out.println("multi class classification 'signal' parameters:");
			System.out.println(" -multSpanProp FILE             "+
					multiSpanPropFileHelp);
			System.out.println(" -cross                         "+crossHelp);
			System.out.println("");
		}

		// for gui
		public String getMultiSpanPropFile(){
			return multiSpanPropFileName;
		}

		public void setMultiSpanPropFile(String s){
			multiSpanPropFileName=s;
			File f=new File(s);
			multiSpanPropFile=f;
			try{
				multiSpanProp=createMultiSpanProp(multiSpanPropFile,this.base.labels);
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		public boolean getCross(){
			return cross;
		}

		public void setCross(boolean b){
			cross=b;
		}

		// help buttons
		public String getMultiSpanPropFileHelp(){
			return multiSpanPropFileHelp;
		}

		public String getCrossHelp(){
			return crossHelp;
		}
	}

	/** Parameters encoding the 'training signal' for learning a token-tagger. */
	public static class TaggerSignalParams extends BasicCommandLineProcessor{

		private BaseParams base=new BaseParams();

		/** Not recommended, but required for bean-shell like visualization */
		public TaggerSignalParams(){
			;
		}

		public TaggerSignalParams(BaseParams base){
			this.base=base;
		}

		public String tokenProp=null;

		public void tokenProp(String s){
			this.tokenProp=s;
		}

		private String tokenPropHelp=
			new String(
			"create a sequential dataset, where tokens are\n given the class associated with this token property");

		@Override
		public void usage(){
			System.out.println("tagger 'signal' parameters:");
			System.out.println(" -tokenProp TYPE          "+tokenPropHelp);
			System.out.println();
		}

		// for gui
		public String getTokenProp(){
			return tokenProp==null?"n/a":tokenProp;
		}

		public void setTokenProp(String t){
			this.tokenProp="n/a".equals(t)?null:t;
		}

		public Object[] getAllowedTokenPropValues(){
			return base.labels==null?new String[]{}:base.labels.getTokenProperties()
					.toArray();
		}

		// help button
		public String getTokenPropHelp(){
			return tokenPropHelp;
		}
	}

	/** Parameters for training an extractor. */
	public static class TrainExtractorParams extends BasicCommandLineProcessor{

		public AnnotatorLearner learner=new Recommended.VPHMMLearner();

		public SpanFeatureExtractor fe=null;

		public String mixup=null;

		//private String learnerName;

		private String embeddedAnnotators="";

		public String output="_prediction";

		public void learner(String s){
			//this.learnerName=s;
			this.learner=(AnnotatorLearner)newObjectFromBSH(s,AnnotatorLearner.class);
			if(fe!=null)
				learner.setSpanFeatureExtractor(fe);
		}

		public void output(String s){
			this.output=s;
		}

		public void other(String s){
			Object o=this;
			RefUtils.modify(o,s);
		}

		public void option(String s){
			Object o=this;
			RefUtils.modify(o,s);
		}

		public void learnerOp(String s){
			Object o=(Object)learner;
			RefUtils.modify(o,s);
		}

		public void feOp(String s){
			if(fe!=null){
				Object o=(Object)fe;
				RefUtils.modify(o,s);
			}else
				System.out
				.println("You must define a Feature Extrator before setting it's options");
		}

		public void mixup(String s){
			mixup=s;
			if(fe==null)
				fe=learner.getSpanFeatureExtractor();
			safeSetRequiredAnnotation(fe,s);
		}

		public void embed(String s){
			if(fe==null)
				fe=learner.getSpanFeatureExtractor();
			embeddedAnnotators=s;
			safeSetAnnotatorLoader(fe,s);
		}

		public CommandLineProcessor fe(String s){
			this.fe=
				(SpanFeatureExtractor)newObjectFromBSH(s,SpanFeatureExtractor.class);
			if(learner!=null)
				learner.setSpanFeatureExtractor(fe);
			if(this.fe instanceof CommandLineProcessor.Configurable){
				return ((CommandLineProcessor.Configurable)this.fe).getCLP();
			}else{
				return null;
			}
		}

		private String learnerHelp=
			new String(
			"Bean-shell code to create a ClassifierLearner\n - default is \"new Recommended.NaiveBayes()\"");

//		private String feHelp=
//				new String(
//						"Bean-shell code to create a SpanFeatureExtractor\n - default is \"new Recommended.DocumentFE()\" ");

		private String mixupHelp=
			new String("run named mixup code before extracting features");

		private String embedHelp=
			new String("embed the listed annotators in the feature extractor");

		private String outputHelp=
			new String(
			"the type or property that is produced by the learned\n ClassifierAnnotator - default is \"_prediction\"");

		@Override
		public void usage(){
			System.out.println("extraction training parameters:");
			System.out
			.println(" [-learner BSH]           Bean-shell code to create an AnnotatorLearner ");
			// System.out.println(" - default is \"new Recommended.NaiveBayes()\"");
			System.out
			.println(" [-fe FE]                 Bean-shell code to create a SpanFeatureExtractor");
			System.out
			.println("                          - default is \"new Recommended.TokenFE()\"");
			System.out
			.println("                          - if FE implements CommandLineProcessor.Configurable then");
			System.out
			.println("                            immediately following arguments are passed to it");
			System.out
			.println(" [-mixup STRING]          run named mixup code before extracting features");
			System.out
			.println(" [-embed STRING]          embed the listed annotators in the feature extractor");
			System.out
			.println(" [-output STRING]         the type or property that is produced by the learned");
			System.out
			.println("                           Annotator - default is \"_prediction\"");
			System.out
			.println(" [-learnerOp STRING=VALUE] Extra options that can be defined with the learner");
			System.out.println("                           - defaults are set");
			System.out
			.println("                           - ex: displayDatasetBeforeLearning=true");
			System.out
			.println(" [-feOp STRING=VALUE]      Extra options that can be defined with the feature extractor");
			System.out.println("                           - defaults are set");
			System.out
			.println("                           - ex: featureWindowSize=4");
			System.out.println();
		}

		// for gui
		public AnnotatorLearner getLearner(){
			return learner;
		}

		public void setLearner(AnnotatorLearner learner){
			this.learner=learner;
		}

		public String getOutput(){
			return output;
		}

		public void setOutput(String s){
			this.output=s;
		}

		public String getMixup(){
			if(fe==null)
				fe=learner.getSpanFeatureExtractor();
			return safeGetRequiredAnnotation(fe);
		}

		public void setMixup(String s){
			if(fe==null)
				fe=learner.getSpanFeatureExtractor();
			safeSetRequiredAnnotation(fe,s);
		}

		public String getEmbeddedAnnotators(){
			return embeddedAnnotators;
		}

		public void setEmbeddedAnnotators(String s){
			if(fe==null)
				fe=learner.getSpanFeatureExtractor();
			embeddedAnnotators=s;
			safeSetAnnotatorLoader(fe,s);
		}

		// help buttons
		public String getLearnerHelp(){
			return learnerHelp;
		}

		// public String getFeHelp() { return feHelp; }
		public String getMixupHelp(){
			return mixupHelp;
		}

		public String getEmbeddedAnnotatorsHelp(){
			return embedHelp;
		}

		public String getOutputHelp(){
			return outputHelp;
		}
	}

	/** Parameters for training an extractor. */
	public static class TrainTaggerParams extends BasicCommandLineProcessor{

		public SequenceClassifierLearner learner=new Recommended.VPTagLearner();

		public SpanFeatureExtractor fe=new Recommended.TokenFE();

		public String output="_prediction";

		public boolean showData=false;

		public void showData(){
			this.showData=true;
		}

		public void learner(String s){
			this.learner=
				(SequenceClassifierLearner)newObjectFromBSH(s,
						SequenceClassifierLearner.class);
		}

		public void output(String s){
			this.output=s;
		}

		public CommandLineProcessor fe(String s){
			this.fe=
				(SpanFeatureExtractor)newObjectFromBSH(s,SpanFeatureExtractor.class);
			if(this.fe instanceof CommandLineProcessor.Configurable){
				return ((CommandLineProcessor.Configurable)this.fe).getCLP();
			}else{
				return null;
			}
		}

		private String learnerHelp=
			new String(
			"Bean-shell code to create a ClassifierLearner\n - default is \"new Recommended.NaiveBayes()\"");

		private String showDataHelp=
			new String("interactively view the constructed training dataset");

		private String feHelp=
			new String(
			"Bean-shell code to create a SpanFeatureExtractor\n - default is \"new Recommended.DocumentFE()\" ");

		private String outputHelp=
			new String(
			"the type or property that is produced by the learned\n ClassifierAnnotator - default is \"_prediction\"");

		@Override
		public void usage(){
			System.out.println("tagger training parameters:");
			System.out
			.println(" [-learner BSH]           Bean-shell code to create an SequenceClassifierLearner ");
			System.out
			.println(" [-showData]              interactively view the constructed training dataset");
			System.out
			.println(" [-fe FE]                 Bean-shell code to create a SpanFeatureExtractor");
			System.out
			.println("                          - default is \"new Recommended.TokenFE()\"");
			System.out
			.println("                          - if FE implements CommandLineProcessor.Configurable then");
			System.out
			.println("                            immed. following command-line arguments are passed to it");
			System.out
			.println(" [-output STRING]         the type or property that is produced by the learned");
			System.out
			.println("                            Annotator - default is \"_prediction\"");
			System.out.println();
		}

		// for gui
		public SequenceClassifierLearner getLearner(){
			return learner;
		}

		public void setLearner(SequenceClassifierLearner learner){
			this.learner=learner;
		}

		public String getOutput(){
			return output;
		}

		public void setOutput(String s){
			this.output=s;
		}

		public SpanFeatureExtractor getFeatureExtractor(){
			return fe;
		}

		public void setFeatureExtractor(SpanFeatureExtractor fe){
			this.fe=fe;
		}

		public boolean getShowData(){
			return showData;
		}

		public void setShowData(boolean flag){
			this.showData=flag;
		}

		// help buttons
		public String getLearnerHelp(){
			return learnerHelp;
		}

		public String getShowDataHelp(){
			return showDataHelp;
		}

		public String getFeHelp(){
			return feHelp;
		}

		public String getOutputHelp(){
			return outputHelp;
		}
	}

	static public class MixupParams extends BasicCommandLineProcessor{

		public String fileName=null;

		// for command line processing
		public void mixup(String s){
			fileName=s;
		}

		private String mixupHelp=
			new String(
			"run mixup program in FILE (existing file, or name on classpath)");

		@Override
		public void usage(){
			System.out.println("mixup program parameters:");
			System.out
			.println(" -mixup FILE              run mixup program in FILE (existing file, or name on classpath)");
			System.out.println();
		}

		// for gui
		public String getMixupProgramFilename(){
			return fileName;
		}

		public void setMixupProgramFilename(String s){
			mixup(s);
		}

		// help button
		public String getMixupHelp(){
			return mixupHelp;
		}
	}

	static public class AnnotatorOutputParams extends BasicCommandLineProcessor{

		private static final String[] ALLOWED_VALUES={"minorthird","xml","strings"};

		public String format="minorthird";

		public void format(String s){
			format=s;
		}

		private String formatHelp=
			new String(
			"output results in format TYPE (either 'minorthird', 'xml', or 'strings'");

		@Override
		public void usage(){
			System.out.println("annotation output parameters:");
			System.out.println(" -format TYPE             "+formatHelp);
			System.out.println();
		}

		// for gui
		public String getOutputFormat(){
			return format;
		}

		public void setOutputFormat(String s){
			format=s;
		}

		public String[] getAllowedOutputFormatValues(){
			return ALLOWED_VALUES;
		}

		// help buttons
		public String getOutputFormatHelp(){
			return formatHelp;
		}

	}

	static public class ViewLabelsParams extends BasicCommandLineProcessor{

		public void toXML(String key){
			System.out.println("Creating XML documents");

			try{
				MutableTextLabels labels=
					(MutableTextLabels)FancyLoader.loadTextLabels(key);
				TextBase base=labels.getTextBase();
				TextLabelsLoader x=new TextLabelsLoader();

				for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
					String doc=i.next().getDocumentId();
					String str=x.createXMLmarkup(doc,labels);
					System.out.println(str);
				}
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("something wrong....");
				System.exit(1);
			}
		}

		@Override
		public void usage(){
			System.out.println("labels output parameters:");
			System.out
			.println(" -toXML DIRECTORY_NAME    create documents with embedded XML tags and put in directory");
			System.out.println();
		}
	}

}
