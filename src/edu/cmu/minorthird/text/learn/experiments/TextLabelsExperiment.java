package edu.cmu.minorthird.text.learn.experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.OnlineBinaryClassifierLearner;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.experiments.Expt;
import edu.cmu.minorthird.classify.experiments.FixedTestSetSplitter;
import edu.cmu.minorthird.classify.experiments.RandomSplitter;
import edu.cmu.minorthird.classify.sequential.BatchSequenceClassifierLearner;
import edu.cmu.minorthird.classify.sequential.GenericCollinsLearner;
import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanDifference;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.AnnotatorTeacher;
import edu.cmu.minorthird.text.learn.Extraction2TaggingReduction;
import edu.cmu.minorthird.text.learn.InsideOutsideReduction;
import edu.cmu.minorthird.text.learn.SampleFE;
import edu.cmu.minorthird.text.learn.SequenceAnnotatorLearner;
import edu.cmu.minorthird.text.learn.TextLabelsAnnotatorTeacher;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ParallelViewer;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TransformedViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Run an annotation-learning experiment based on pre-labeled text.
 * 
 * @author William Cohen
 */

public class TextLabelsExperiment implements Visible{

	private SampleFE.ExtractionFE fe=new SampleFE.ExtractionFE();

	private Extraction2TaggingReduction reduction=new InsideOutsideReduction();

	private int classWindowSize=3;

	private TextLabels labels;

	private Splitter<Span> splitter;

	private AnnotatorLearner learner;

	private String inputType,inputProp;

	private TextLabels testLabelsUsedInSplitter;

	private MonotonicTextLabels fullTestLabels;

	private MonotonicTextLabels[] testLabels;

	private String outputLabel;

	private Annotator[] annotators;

	private static Logger log=Logger.getLogger(TextLabelsExperiment.class);

	private ExtractionEvaluation extractionEval=new ExtractionEvaluation();

	/**
	 * @param labels
	 *          The labels and base to be annotated in the example These are the
	 *          training examples
	 * @param splitter
	 *          splitter for the documents in the labels to create test vs. train
	 * @param learnerName
	 *          AnnotatorLearner algorithm object to use
	 * @param spanType
	 *          spanType in the TextLabels to use as training data. (I.e., the
	 *          spanType to learn.
	 * @param spanProp
	 *          span property in the TextLabels to use as training data.
	 * @param outputLabel
	 *          the spanType that will be assigned to spans predicted to be of
	 *          type inputLabel by the learner (I.e., the output type associated
	 *          with the learned annotator.)
	 */
	public TextLabelsExperiment(TextLabels labels,Splitter<Span> splitter,
			String learnerName,String spanType,String spanProp,String outputLabel){
		this(labels,splitter,learnerName,spanType,spanProp,outputLabel,null);
	}

	public TextLabelsExperiment(TextLabels labels,Splitter<Span> splitter,
			String learnerName,String spanType,String spanProp,String outputLabel,
			Extraction2TaggingReduction reduce){
		if(reduce!=null)
			this.reduction=reduce;
		this.labels=labels;
		this.splitter=splitter;
		this.inputType=spanType;
		this.inputProp=spanProp;
		this.outputLabel=outputLabel;
		this.learner=toAnnotatorLearner(learnerName);
		learner.setAnnotationType(outputLabel);
	}

	public TextLabelsExperiment(TextLabels labels,Splitter<Span> splitter,
			AnnotatorLearner learner,String inputType,String outputLabel){
		this(labels,splitter,null,learner,inputType,null,outputLabel);
	}

	/**
	 * @param labels
	 *          TextLabels to train on
	 * @param splitter
	 *          how to partition labels into train/test
	 * @param testLabels
	 *          if splitter is a FixedTestSetSplitter, these are the labels for
	 *          the test cases. Otherwise the labels for the test cases are given
	 *          in the "labels" input.
	 * @param learner
	 *          the learner to user
	 * @param inputType
	 *          the spanType to learn to extract (if non-null)
	 * @param inputProp
	 *          the spanProp to learn to extract and label (if non-null)
	 * @param outputLabel
	 *          the spanType/spanProp used for predictions
	 */
	public TextLabelsExperiment(TextLabels labels,Splitter<Span> splitter,
			TextLabels testLabels,AnnotatorLearner learner,String inputType,
			String inputProp,String outputLabel){
		this.labels=labels;
		this.splitter=splitter;
		this.testLabelsUsedInSplitter=testLabels;
		this.inputType=inputType;
		this.inputProp=inputProp;
		this.outputLabel=outputLabel;
		this.learner=learner;
		learner.setAnnotationType(outputLabel);
	}

	public SampleFE.ExtractionFE getFE(){
		return fe;
	}

	public void doExperiment(){
		splitter.split(labels.getTextBase().documentSpanIterator());

		annotators=new Annotator[splitter.getNumPartitions()];
		Set<Span> allTestDocuments=new TreeSet<Span>();
		for(int i=0;i<splitter.getNumPartitions();i++){
			for(Iterator<Span> j=splitter.getTest(i);j.hasNext();){
				// System.out.println("adding test case to allTestDocuments");
				allTestDocuments.add(j.next());
			}
		}
		// Progress counter
		ProgressCounter progressCounter=
				new ProgressCounter("train/test experiment","fold",splitter
						.getNumPartitions());

		// figure out what the test set should be
		try{
			// for most splitters, the test set will be a subset of the original
			// TextBase
			SubTextBase fullTestBase=
					new SubTextBase(labels.getTextBase(),allTestDocuments.iterator());
			fullTestLabels=
					new NestedTextLabels(new SubTextLabels(fullTestBase,labels));
			testLabels=new MonotonicTextLabels[splitter.getNumPartitions()];
		}catch(SubTextBase.UnknownDocumentException ex){
			// the other supported case is a fixed test set
			if(testLabelsUsedInSplitter==null)
				throw new IllegalArgumentException("exception: "+ex);
			if(!(splitter instanceof FixedTestSetSplitter))
				throw new IllegalArgumentException("illegal splitter "+splitter);
			fullTestLabels=new NestedTextLabels(testLabelsUsedInSplitter);
			testLabels=new MonotonicTextLabels[1];
			testLabels[0]=fullTestLabels;
		}

		for(int i=0;i<splitter.getNumPartitions();i++){
			log.info("For partition "+(i+1)+" of "+splitter.getNumPartitions());
			log.info("Creating teacher and train partition...");

			SubTextLabels trainLabels=null;
			try{
				SubTextBase trainBase=
						new SubTextBase(labels.getTextBase(),splitter.getTrain(i));
				trainLabels=new SubTextLabels(trainBase,labels);
			}catch(SubTextBase.UnknownDocumentException ex){
				throw new IllegalStateException("error building trainBase "+i+": "+ex);
			}
			AnnotatorTeacher teacher=
					new TextLabelsAnnotatorTeacher(trainLabels,inputType,inputProp);

			log.info("Training annotator: inputType="+inputType+" inputProp="+inputProp);
			annotators[i]=teacher.train(learner);

			// log.info("annotators["+i+"]="+annotators[i]);
			log.info("Creating test partition...");
			try{
				SubTextBase testBase=
						new SubTextBase(labels.getTextBase(),splitter.getTest(i));
				testLabels[i]=new MonotonicSubTextLabels(testBase,fullTestLabels);
			}catch(SubTextBase.UnknownDocumentException ex){
				// do nothing since testLabels[i] is already set
			}

			log.info("Labeling test partition, size="+testLabels[i].getTextBase().size());
			annotators[i].annotate(testLabels[i]);

			log.info("Evaluating test partition...");
			measurePrecisionRecall("Test partition "+(i+1),testLabels[i],false);

			// step progress counter
			progressCounter.progress();
		}
		measurePrecisionRecall("Overall performance",fullTestLabels,true);

		// end progress counter
		progressCounter.finished();
	}

	public ExtractionEvaluation getEvaluation(){
		return extractionEval;
	}

	@Override
	public Viewer toGUI(){
		ParallelViewer v=new ParallelViewer();
		for(int i=0;i<annotators.length;i++){
			final int index=i;
			v.addSubView("Annotator "+(i+1),new TransformedViewer(
					new SmartVanillaViewer()){

				static final long serialVersionUID=20080306L;

				@Override
				public Object transform(Object o){
					return annotators[index];
				}
			});
			v.addSubView("Test set "+(i+1),new TransformedViewer(
					new SmartVanillaViewer()){

				static final long serialVersionUID=20080306L;

				@Override
				public Object transform(Object o){
					return testLabels[index];
				}
			});
		}
		v.addSubView("Full test set",
				new TransformedViewer(new SmartVanillaViewer()){

					static final long serialVersionUID=20080306L;

					@Override
					public Object transform(Object o){
						return fullTestLabels;
					}
				});
		v.addSubView("Evaluation",new TransformedViewer(new SmartVanillaViewer()){

			static final long serialVersionUID=20080306L;

			@Override
			public Object transform(Object o){
				return extractionEval;
			}
		});
		v.setContent(this);
		return v;
	}

// private void measurePrecisionRecall(String tag,TextLabels labels){
// measurePrecisionRecall(tag,labels,false);
// }

	private void measurePrecisionRecall(String tag,TextLabels labels,
			boolean isOverallMeasure){
		if(inputType!=null){
			// only need one span difference here
			SpanDifference sd=
					new SpanDifference(labels.instanceIterator(outputLabel),labels
							.instanceIterator(inputType),labels.closureIterator(inputType));
			System.out.println(tag+":");
			System.out.println(sd.toSummary());
			extractionEval.extend(tag,sd,isOverallMeasure);
		}else{
			// will need one span difference for each possible property value
			Set<String> propValues=new HashSet<String>();
			for(Iterator<Span> i=labels.getSpansWithProperty(inputProp);i.hasNext();){
				Span s=i.next();
				propValues.add(labels.getProperty(s,inputProp));
			}
			SpanDifference[] sd=new SpanDifference[propValues.size()];
			int k=0;
			for(Iterator<String> i=propValues.iterator();i.hasNext();k++){
				String val=i.next();
				sd[k]=
						new SpanDifference(propertyIterator(labels,outputLabel,val),
								propertyIterator(labels,inputProp,val),labels.getTextBase()
										.documentSpanIterator());
				String tag1=tag+" for "+inputProp+":"+val;
				System.out.println(tag1+":");
				System.out.println(sd[k].toSummary());
				extractionEval.extend(tag1,sd[k],false);
			}
			SpanDifference sdAll=new SpanDifference(sd);
			String tag1=tag+" (micro-averaged) for "+inputProp;
			System.out.println(tag1+":");
			System.out.println(sdAll.toSummary());
			extractionEval.extend(tag1,sdAll,isOverallMeasure);
		}
		if(isOverallMeasure)
			extractionEval.measureTotalSize(labels.getTextBase());
	}

	private Iterator<Span> propertyIterator(TextLabels labels,String prop,
			String value){
		List<Span> accum=new ArrayList<Span>();
		for(Iterator<Span> i=labels.getSpansWithProperty(prop);i.hasNext();){
			Span s=i.next();
			if(value==null||value.equals(labels.getProperty(s,prop))){
				accum.add(s);
			}
		}
		return accum.iterator();
	}

	public AnnotatorLearner toAnnotatorLearner(String s){
		try{
			OnlineBinaryClassifierLearner learner=
					(OnlineBinaryClassifierLearner)Expt.toLearner(s);

			BatchSequenceClassifierLearner seqLearner=
					new GenericCollinsLearner(learner,classWindowSize);
			return new SequenceAnnotatorLearner(seqLearner,fe,reduction);
		}catch(IllegalArgumentException ex){
			/* that's ok, maybe it's something else */;
		}
		try{
			BatchSequenceClassifierLearner seqLearner=
					(BatchSequenceClassifierLearner)SequenceAnnotatorExpt.toSeqLearner(s);
			return new SequenceAnnotatorLearner(seqLearner,fe,reduction);
		}catch(IllegalArgumentException ex){
			/* that's ok, maybe it's something else */;
		}
		try{
			bsh.Interpreter interp=new bsh.Interpreter();
			interp.eval("import edu.cmu.minorthird.text.*;");
			interp.eval("import edu.cmu.minorthird.text.learn.*;");
			interp.eval("import edu.cmu.minorthird.classify.*;");
			interp.eval("import edu.cmu.minorthird.classify.experiments.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
			interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
			return (AnnotatorLearner)interp.eval(s);
		}catch(bsh.EvalError e){
			throw new IllegalArgumentException("error parsing learnerName '"+s+
					"':\n"+e);
		}
	}

	static public BatchSequenceClassifierLearner toSeqLearner(String learnerName){
		try{
			bsh.Interpreter interp=new bsh.Interpreter();
			interp.eval("import edu.cmu.minorthird.classify.*;");
			interp.eval("import edu.cmu.minorthird.classify.experiments.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
			interp.eval("import edu.cmu.minorthird.classify.sequential.*;");
			interp.eval("import edu.cmu.minorthird.classify.transform.*;");
			return (BatchSequenceClassifierLearner)interp.eval(learnerName);
		}catch(bsh.EvalError e){
			throw new IllegalArgumentException("error parsing learnerName '"+
					learnerName+"':\n"+e);
		}
	}

	public TextLabels getTestLabels(){
		return fullTestLabels;
	}

	public static void main(String[] args){
		Splitter<Span> splitter=new RandomSplitter<Span>(0.7);
		String outputLabel="_prediction";
		String learnerName="new CollinsPerceptronLearner()";
		TextLabels labels=null;
		String spanType=null,spanProp=null,saveFileName=null,show=null,annotationNeeded=
				null;
		List<String> featureMods=new ArrayList<String>();
		Extraction2TaggingReduction reduction=null;

		try{
			int pos=0;
			while(pos<args.length){
				String opt=args[pos++];
				if(opt.startsWith("-lab")){
					labels=FancyLoader.loadTextLabels(args[pos++]);
				}else if(opt.startsWith("-lea")){
					learnerName=args[pos++];
				}else if(opt.startsWith("-split")){
					splitter=Expt.toSplitter(args[pos++],Span.class);
				}else if(opt.startsWith("-in")){
					spanType=args[pos++];
				}else if(opt.startsWith("-spanT")){
					spanType=args[pos++];
				}else if(opt.startsWith("-spanP")){
					spanProp=args[pos++];
				}else if(opt.startsWith("-out")){
					outputLabel=args[pos++];
				}else if(opt.startsWith("-save")){
					saveFileName=args[pos++];
				}else if(opt.startsWith("-show")){
					show=args[pos++];
				}else if(opt.startsWith("-mix")){
					annotationNeeded=args[pos++];
				}else if(opt.startsWith("-fe")){
					featureMods.add(args[pos++]);
				}else if(opt.startsWith("-reduction")){
					try{
						bsh.Interpreter interp=new bsh.Interpreter();
						interp.eval("import edu.cmu.minorthird.text.learn.*;");
						reduction=(Extraction2TaggingReduction)interp.eval(args[pos++]);
					}catch(bsh.EvalError e){
						throw new IllegalArgumentException("error parsing reductionName '"+
								args[pos-1]+"':\n"+e);
					}
				}else{
					usage();
					return;
				}
			}
			if(labels==null||learnerName==null||splitter==null||
					(spanProp==null&&spanType==null)||outputLabel==null){
				usage();
				return;
			}
			if(spanProp!=null&&spanType!=null){
				usage();
				return;
			}
			TextLabelsExperiment expt=
					new TextLabelsExperiment(labels,splitter,learnerName,spanType,
							spanProp,outputLabel,reduction);
			if(annotationNeeded!=null){
				expt.getFE().setRequiredAnnotation(annotationNeeded);
				expt.getFE().setAnnotationProvider(annotationNeeded+".mixup");
				expt.getFE().setTokenPropertyFeatures("*"); // use all defined
				// properties
				labels.require(annotationNeeded,annotationNeeded+".mixup");
			}
			for(Iterator<String> i=featureMods.iterator();i.hasNext();){
				String mod=i.next();
				if(mod.startsWith("window=")){
					expt.getFE().setFeatureWindowSize(
							StringUtil.atoi(mod.substring("window=".length())));
					System.out.println("fe windowSize => "+
							expt.getFE().getFeatureWindowSize());
				}else if(mod.startsWith("charType")){
					expt.getFE().setUseCharType(
							mod.substring("charType".length(),1).equals("+"));
					System.out.println("fe windowSize => "+expt.getFE().getUseCharType());
				}else if(mod.startsWith("charPattern")){
					expt.getFE().setUseCompressedCharType(
							mod.substring("charPattern".length(),1).equals("+"));
					System.out.println("fe windowSize => "+
							expt.getFE().getUseCompressedCharType());
				}else{
					usage();
					return;
				}
			}

			expt.doExperiment();
			if(saveFileName!=null){
				new TextLabelsLoader().saveTypesAsOps(expt.getTestLabels(),new File(
						saveFileName));
			}
			if(show!=null){
				TextBaseViewer.view(expt.getTestLabels());
				if(show.startsWith("all")){
					new ViewerFrame("Experiment",expt.toGUI());
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			usage();
			return;
		}
	}

	private static void usage(){
		String[] usageLines=
				new String[]{
						"usage: options are:",
						"       -label labelsKey   dataset to load",
						"       -spanType type     defines the extraction target",
						"       -spanProp prop     defines the extraction target (specify exactly one of -spanType or -spanProp)",
						"       -learn learner     Java code to construct the learner, which could be an ",
						"                          an AnnotatorLearner, a BatchSequenceClassifierLearner, or an OnlineClassifierLearner",
						"                          - a BatchSequenceClassifierLearner is used to defined a SequenceAnnotatorLearner",
						"                          and an OnlineClassifierLearner is used to define a GenericCollinsLearner",
						"                          optional, default \"new CollinsPerceptronLearner()\"",
						"       -out outputLabel   label assigned to predictions",
						"                          optional, default _prediction",
						"       -split splitter    splitter to use, in format used by minorthird.classify.experiments.Expt.toSplitter()",
						"                          optional, default r70",
						"       -save fileName     file to save extended TextLabels in (train data + predictions)",
						"                          optional",
						"       -show xxxx         how much detail on experiment to show - xxx=all shows the most",
						"                          optional",
						"       -mix yyyy          augment feature extracture to first execute 'require yyyy,yyyy.mixup'",
						"                          optional",
						"       -fe zzzz           change default feature extractor with one of these options zzzz:",
						"                          window=K charType+ charType- charPattern+ charPattern-",};
		for(int i=0;i<usageLines.length;i++)
			System.out.println(usageLines[i]);
	}
}
