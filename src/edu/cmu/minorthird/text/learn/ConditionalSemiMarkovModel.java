/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.algorithms.random.RandomElement;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.lookup.*;

import org.apache.log4j.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.BorderLayout;
import javax.swing.border.*;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.lookup.*;
/**
 * Learn to annotate based on a conditional semi-markov model,
 * learned from examples.
 *
 * @author William Cohen
 */

/*
	status/limitations: this only learns one label type, with a single
	binary classifier.
 */

public class ConditionalSemiMarkovModel 
{
	private static Logger log = Logger.getLogger(ConditionalSemiMarkovModel.class);
	private static final boolean DEBUG = log.isDebugEnabled();
	
	/** A learner for ConditionalSemiMarkovModel's.
	 */ 
	static public class CSMMLearner implements AnnotatorLearner 
	{
		private SpanFeatureExtractor fe;
		private OnlineBinaryClassifierLearner classifierLearner;
		private int epochs;
		private int maxSegmentSize = 5;
		// temporary storage
		private Span.Looper documentLooper;
		private List exampleList; 
		// type of annotation to produce
		private String annotationType;
		
		//
		// constructors
		//

		public CSMMLearner()
		{
			this( new CSMMSpanFE(), new VotedPerceptron(), 5, 5,"");
		}
		public CSMMLearner(int epochs)
		{
			this( new CSMMSpanFE(), new VotedPerceptron(), epochs, 5,"");
		}
		
		public CSMMLearner(int epochs, int maxSegmentSize)
		{
			this( new CSMMSpanFE(), new VotedPerceptron(), epochs, maxSegmentSize,"");
		}
		
		public CSMMLearner(String annotation)
		{
			this( new CSMMSpanFE(), new VotedPerceptron(), 5, 5,annotation);
		}

		public CSMMLearner(String dictionaryFile, String distanceNames, int maxSegmentSize)
		{
			this(dictionaryFile, distanceNames, 5, maxSegmentSize);
		}

		public CSMMLearner(String dictionaryFile, String distanceNames, int epoch, int maxSegmentSize) 
		{
			this(dictionaryFile, distanceNames, epoch, maxSegmentSize,"");
		}

		public CSMMLearner(String dictionaryFile, String distanceNames, int epoch, int maxSegmentSize, String mixFile)
		{
			this(dictionaryFile, distanceNames, epoch, maxSegmentSize,false,mixFile);
		}

		public CSMMLearner(String dictionaryFile, String distanceNames,
											 int epochSize, int maxSegmentSize, 
											 boolean addTraining,boolean doCrossVal,
											 String mixFile) {
			this(new CSMMWithDictionarySpanFE(dictionaryFile, distanceNames, addTraining,doCrossVal), 
					 new VotedPerceptron(), epochSize, maxSegmentSize,mixFile);
		}

		public 
		CSMMLearner(
			String dictionaryFile, String distanceNames, int epoch, int maxSegmentSize, boolean addTraining,String mixFile) 
		{
			this(dictionaryFile, distanceNames, epoch, maxSegmentSize,addTraining,true,mixFile);
		}
		
		public CSMMLearner(
			SpanFeatureExtractor fe,OnlineBinaryClassifierLearner classifierLearner,int epochs,int maxSegSz,String annotation)
		{
			this.fe = fe;
			if (annotation.length() > 0) {
				System.out.println("Reading annotations");
				((CSMMSpanFE)fe).setRequiredAnnotation(annotation,annotation+".mixup");
				((CSMMSpanFE)fe).setTokenPropertyFeatures("*"); // use all defined properties
			}
			this.classifierLearner = classifierLearner;
			this.epochs = epochs;
			this.maxSegmentSize = maxSegSz;
			reset();
		}
		
		//
		// getters/setters for gui
		//
		public OnlineBinaryClassifierLearner getLearner() { return classifierLearner; }
		public void setLearner(OnlineBinaryClassifierLearner newLearner) { this.classifierLearner=newLearner; }
		public int getEpochs() { return epochs; }
		public void setEpochs(int newEpochs) { this.epochs = newEpochs; }
		public int getMaxSegmentSize() { return maxSegmentSize; }
		public void setMaxSegmentSize(int newMaxSize) { this.maxSegmentSize = newMaxSize; }
		//
		public SpanFeatureExtractor getSpanFeatureExtractor()  { return fe; }
		public void setSpanFeatureExtractor(SpanFeatureExtractor fe)  { this.fe = fe; }

		//
		// AnnotatorLearner implementation: query all documents, and accumulate examples in exampleList
		//
		public void reset()	{	exampleList = new ArrayList();	}
		public void setDocumentPool(Span.Looper documentLooper) {	this.documentLooper = documentLooper;	}
		public boolean hasNextQuery() {	return documentLooper.hasNext(); }
		public Span nextQuery() {	return documentLooper.nextSpan();	}
		public void setAnswer(AnnotationExample answeredQuery) { exampleList.add(answeredQuery); }
		public void setAnnotationType(String s)	{	this.annotationType = s;}
		public String getAnnotationType()	{	return annotationType;}
		
		
		/** Learning takes place here.
		 */
		public Annotator getAnnotator() 
		{ 
			classifierLearner.reset();
			
			log.debug("processing "+exampleList.size()+" examples for "+epochs+" epochs");
			
			ProgressCounter pc = new ProgressCounter("training CSMM", "document", epochs*exampleList.size()); 
			
			if (fe.getClass().getName().endsWith("CSMMWithDictionarySpanFE")) ((CSMMWithDictionarySpanFE)fe).train(exampleList.iterator());
			for (int i=0; i<epochs; i++) {
				
				for (Iterator j=exampleList.iterator(); j.hasNext(); ) {
					
					AnnotationExample example = (AnnotationExample)j.next();
					Span doc = example.getDocumentSpan();
					if (DEBUG) log.debug("updating from "+doc);
					
					// get best segmentation, given current classifier
					Segments viterbi = 
						bestSegments(doc,example.getLabels(),fe,classifierLearner.getBinaryClassifier(),maxSegmentSize);
					if (DEBUG) log.debug("viterbi solution:\n" + viterbi);
					
					// train classifier on any false positives
					Segments correct = correctSegments(example);
					if (DEBUG) log.debug("correct spans:\n" + correct);
					Span previousSpan = null;
					for (Span.Looper k=viterbi.iterator(); k.hasNext(); ) {
						Span span = k.nextSpan();
						if (!correct.contains( span )) {
							if (DEBUG) log.debug("false pos: "+span);
							classifierLearner.addExample( exampleFor(example,span,previousSpan, -1) );
						} 
						previousSpan = span;
					}
					// train classifier on any false negatives
					// ignoring context (for now)
					previousSpan = null;
					for (Span.Looper k=correct.iterator(); k.hasNext(); ) {
						Span span = k.nextSpan();
						if (!viterbi.contains( span )) {
							if (DEBUG) log.debug("false neg: "+span);
							classifierLearner.addExample( exampleFor(example,span,previousSpan, +1) );
						} 
						previousSpan = span;
					}
					
					
					pc.progress();
					
				}//epoch
				
				if (DEBUG) {
					ViewerFrame f = 
						new ViewerFrame("classifier after epoch "+i, 
														new SmartVanillaViewer( classifierLearner.getBinaryClassifier() ));
				}
				
				pc.finished();
				
			}//all epochs
			
			return new CSMMAnnotator(fe,classifierLearner.getBinaryClassifier(),annotationType,maxSegmentSize);
		}
		
		// build an example from a span and its context
		private Example exampleFor(AnnotationExample example, Span span, Span prevSpan, double numberLabel)
		{
			Instance instance =  fe.extractInstance(example.getLabels(),span);
			String prevLabel;
			if (prevSpan!=null && prevSpan.getRightBoundary().equals(span.getLeftBoundary())) {
				prevLabel = ExampleSchema.POS_CLASS_NAME;
			} else {
				prevLabel = ExampleSchema.NEG_CLASS_NAME;
			}
			Instance instanceFromSeq = new InstanceFromSequence(instance, new String[]{prevLabel});
			if (DEBUG) log.debug("example for "+span+": "+instanceFromSeq);
			return new Example( instanceFromSeq, ClassLabel.binaryLabel(numberLabel));
		}
		
		// the correct segments, as defined by the example
		private Segments correctSegments(AnnotationExample example)
		{
			Set set = new TreeSet();
			String id = example.getDocumentSpan().getDocumentId();
			String type = example.getInputType();
			for (Span.Looper i=example.getLabels().instanceIterator( type, id ); i.hasNext(); ) {
				set.add( i.nextSpan() );
			}
			return new Segments( set );
		}
	} // class CSMMLearner
	
	
	// annotate a document using a learned model
	static public class CSMMAnnotator extends AbstractAnnotator	implements Visible,ExtractorAnnotator,Serializable 
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;
		
		private SpanFeatureExtractor fe;
		private BinaryClassifier classifier;
		private String annotationType;
		private int maxSegSize;
		public Viewer toGUI()
		{
			Viewer v = new ComponentViewer() {
					public JComponent componentFor(Object o) {
						CSMMAnnotator ann = (CSMMAnnotator)o;
						JPanel mainPanel = new JPanel();
						mainPanel.setLayout(new BorderLayout());
						mainPanel.add(
							new JLabel("CSMM: segsize "+maxSegSize),
							BorderLayout.NORTH);
						Viewer subView = new SmartVanillaViewer(ann.classifier);
						subView.setSuperView(this);
						mainPanel.add(subView,BorderLayout.SOUTH);
						mainPanel.setBorder(new TitledBorder("Conditional Semi-Markov-Model"));
						return new JScrollPane(mainPanel);
					}
				};
			v.setContent(this);
			return v;
		}
		public 
		CSMMAnnotator(SpanFeatureExtractor fe,BinaryClassifier classifier,String annotationType,int maxSegSize)
		{
			this.fe = fe;
			this.classifier = classifier;
			this.annotationType = annotationType;
			this.maxSegSize = maxSegSize;
		}
		public String getSpanType() { return annotationType; }
		public void doAnnotate(MonotonicTextLabels labels)
		{
			ProgressCounter pc = new ProgressCounter("annotating","document",labels.getTextBase().size());
			for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span doc = i.nextSpan();
				Segments viterbi = bestSegments(doc,labels,fe,classifier,maxSegSize);				
				for (Iterator j=viterbi.iterator(); j.hasNext(); ) {
					Span span = (Span)j.next();
					labels.addToType( span, annotationType );
				}
				pc.progress();
			}
			pc.finished();
		}
		public String explainAnnotation(TextLabels labels,Span documentSpan)
		{
			return "not implemented";
		}
	}
	
	//
	// viterbi algorithm
	//
	
	static public Segments bestSegments(
		Span documentSpan,TextLabels labels,SpanFeatureExtractor fe,BinaryClassifier classifier,int maxSegSize)
	{
		// for t=0..size, y=0 or 1, fty[t][y] is the highest score that
		// can be obtained with a segmentation of the tokens from 0..t
		// that ends with class y (where y=1 means "from dictionary", y=0
		// means "from null model")
		
		// initialize
		double[][] fty = new double[documentSpan.size()+1][2];
		BackPointer[][] trace = new BackPointer[documentSpan.size()+1][2];
		for (int t=0; t<documentSpan.size()+1; t++) {
			for (int y=0; y<2; y++) {
				fty[t][y] = -99999; //could be -Double.MAX_VALUE;
				trace[t][y] = null;
			}
		}
		fty[0][0] = fty[0][1] = 0;
		
		// fill the fty matrix
		for (int t=0; t<documentSpan.size()+1; t++) {
			for (int y=0; y<2; y++) {
				for (int lastY=0; lastY<2; lastY++) {
					int maxSegSizeForY = y==0 ? 1 : maxSegSize;
					for (int lastT=Math.max(0, t-maxSegSizeForY); lastT<t; lastT++) {
						Span segment = documentSpan.subSpan(lastT, t-lastT);
						double segmentScore = score(labels,lastY,y,lastT,t,segment,fe,classifier);
						if (segmentScore + fty[lastT][lastY] > fty[t][y]) {
							fty[t][y] = segmentScore + fty[lastT][lastY];
							trace[t][y] = new BackPointer(segment,lastT,lastY);
						}
					}
				}
			}
		}
		
		// use the back pointers to find the best segmentation that ends at t==documentSize
		int y = (fty[documentSpan.size()][1] > fty[documentSpan.size()][0]) ? 1 : 0;
		Set result = new TreeSet();
		for (BackPointer bp = trace[documentSpan.size()][y]; bp!=null; bp=trace[bp.lastT][bp.lastY]) {
			bp.onBestPath = true;
			if (y==1) result.add( bp.span );
			y = bp.lastY;
		}
		if (DEBUG) dumpStuff(fty,trace);
		return new Segments(result);
	}
	
	private static void dumpStuff(double[][] fty, BackPointer[][] trace)
	{
		java.text.DecimalFormat format = new java.text.DecimalFormat("####.###");
		System.out.println("t.y\tf(t,y)\tt'.y'\tspan");
		for (int t=0; t<fty.length; t++) {
			for (int y=0; y<2; y++) {
				BackPointer bp = trace[t][y];				
				String spanText = bp==null ? "*NULL*" : bp.span.asString();
				if (bp==null) bp = new BackPointer((Span)null,-1,-1);
				String marker = bp.onBestPath? "<==" : "";
				System.out.println(t+"."+y+"\t"+format.format(fty[t][y])+"\t"+
													 bp.lastT+"."+bp.lastY+"  '"+spanText+"' "+marker);
			}
		}
	}
	
	
	// used by viterbi
	static private double 
	score(TextLabels labels,int lastY,int y,int lastT,int t,Span segment,SpanFeatureExtractor fe,BinaryClassifier cls)
	{
		if (y == 0)	return 0;
		String prevLabel = lastY==1 ? ExampleSchema.POS_CLASS_NAME : ExampleSchema.NEG_CLASS_NAME;
		//System.out.println("score with labels "+labels.getClass());
 		Instance segmentInstance = 
			new InstanceFromSequence(fe.extractInstance(labels,segment),new String[]{prevLabel});
		if (DEBUG) log.debug("score: "+cls.score(segmentInstance)+"\t"+segment);
		//		System.out.println("score" + cls.score(segmentInstance)+"\t"+segment + " instance " + segmentInstance);
		return cls.score( segmentInstance );
	}
	
	private static class BackPointer {
		public Span span; 
		public int lastT, lastY;
		public boolean onBestPath;
		public BackPointer(Span span,int lastT, int lastY) {
			this.span=span;
			this.lastT=lastT;
			this.lastY=lastY;
			this.onBestPath=false;
		}
	}
	
	//
	// convert a span to an instance
	//
	static public class CSMMSpanFE extends SampleFE.ExtractionFE
	{
		public CSMMSpanFE() { super(); }
		public CSMMSpanFE(int windowSize) { super(windowSize); }
		
		public void extractFeatures(Span span) {
			extractFeatures(new EmptyLabels(),span);
		}
		
		public void extractFeatures(TextLabels labels,Span span) 
		{
			super.extractFeatures(labels,span);
			// text of span & its charTypePattern
			from(span).eq().lc().emit();
			if (useCharType) from(span).eq().charTypes().emit();
			if (useCompressedCharType) from(span).eq().charTypePattern().emit();
			// length properties of span
			from(span).size().emit();
			from(span).exactSize().emit();
			// first and last tokens
			from(span).token(0).eq().lc().emit();
			from(span).token(-1).eq().lc().emit();
			if (useCharType) {
				from(span).token(0).eq().charTypes().lc().emit();
				from(span).token(-1).eq().charTypes().lc().emit();
			}
			if (useCompressedCharType) {
				from(span).token(0).eq().charTypePattern().lc().emit();
				from(span).token(-1).eq().charTypePattern().lc().emit();
			}
			// use marked properties of tokens for first & last tokens in span
			for (int i=0; i<tokenPropertyFeatures.length; i++) {
				String p = tokenPropertyFeatures[i];
				// first & last tokens
				from(span).token(0).prop(p).emit();
				from(span).token(-1).prop(p).emit();
				from(span).subSpan(1,span.size()-2).tokens().prop(p).emit();
			}
		}
	};
	
	/**
	 * Feature extractor for providing distance-based features on terms.
	 * Dictionary can be specified either as an external file or by using the training spans.
	 * - Sunita Sarawagi
	 */
	static public class CSMMWithDictionarySpanFE extends CSMMSpanFE
	{
		boolean addTrainingSegsToDictionary;
		boolean useCrossVal;
		SoftDictionary dictionary;
		StringDistance distances[];
		Feature features[];
		// distanceNames has to be "/" separated list of distance functions that
		// we want to apply 
		public CSMMWithDictionarySpanFE(String dictionaryFile, String distanceNames) {
	    this(dictionaryFile,distanceNames,false,false);
		} 
		public CSMMWithDictionarySpanFE(String dictionaryFile, String distanceNames, boolean addTraining, boolean useCrossValArg) { 
	    super();
	    try {
				addTrainingSegsToDictionary = addTraining;
				useCrossVal = useCrossValArg;
				dictionary = new SoftDictionary();
				distances = DistanceLearnerFactory.buildArray(distanceNames);
				if (dictionaryFile.length() > 0) {
					dictionary.load(new File(dictionaryFile));
					trainDistances();
				}
				// now create features corresponding to each distance function.
				features = new Feature[distances.length];
				for (int d = 0; d < distances.length; d++) {
					// save the feature name
					features[d] =  new Feature(distances[d].toString());
				}
	    } catch (IOException e) {
				e.printStackTrace();
	    }
		}
		public void trainDistances() {
	    for (int d = 0; d < distances.length; d++) {
				// train anything that's also a distance learner
				if (distances[d] instanceof StringDistanceLearner) {
					distances[d] = dictionary.getTeacher().train( (StringDistanceLearner)distances[d] );
				}
	    }
		}
		public void train(Iterator iter) {
	    if (!addTrainingSegsToDictionary)
				return;
	    int numAdded = 0;
	    float total = 0;
	    for (; iter.hasNext(); ) {
				AnnotationExample example = (AnnotationExample)iter.next();
				String id = example.getDocumentSpan().getDocumentId();
				String type = example.getInputType();
				for (Span.Looper i=example.getLabels().instanceIterator( type, id ); i.hasNext(); ) {
					String thisSeg = ((Span)i.nextSpan()).asString();
					/**
					 * Uncomment for reporting distances..
					 
					 float dist = (float)dictionary.lookupDistance(thisSeg);
					 System.out.println("Match " + thisSeg + " => " + dictionary.lookup(thisSeg)+ " at " + dictionary.lookupDistance(thisSeg));
					 if (dist > 0)
					 total += dist;
					*/
					numAdded++;
					dictionary.put(id,thisSeg,null);
				}
	    }
	    trainDistances();
	    //	    System.out.println("Average distance " + total/numAdded + " over " + numAdded);
		}
		public void extractFeatures(TextLabels labels,Span span) {
	    super.extractFeatures(labels,span);
	    StringWrapper spanString = new BasicStringWrapper(span.asString());
	    String id = ((addTrainingSegsToDictionary && useCrossVal)?span.getDocumentId():null);
	    Object closestMatch = dictionary.lookup(id,spanString);
	    if (closestMatch != null) {
				// create various types of similarity measures.
				for (int d = 0; d < distances.length; d++) {
					double score = distances[d].score(spanString, (StringWrapper)closestMatch);
					if (score != 0) {
						// instance has been created by the parent.
						instance.addNumeric(features[d], score);
					}
				}
	    }
		}
	};
	
	// a proposed segmentation of a document
	static public class Segments
	{
		private Set spanSet;
		public Segments(Set spanSet)	{	this.spanSet = spanSet;	}
		public Span.Looper iterator() {	return new BasicSpanLooper(spanSet.iterator());	}
		public boolean contains(Span span) { return spanSet.contains(span);	}
		public String toString() { return "[Segments: "+spanSet.toString()+"]"; }
	}
	
};
