/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.lookup.*;

import org.apache.log4j.*;
import java.util.*;
import java.io.*;

/**
 * Learn to annotate based on a conditional semi-markov model
 * learned from examples.
 *
 * @author William Cohen
 */

/*
	status/limitations: this only learns one label types, with a single
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
		private int[] maxSegmentSize = new int[]{ 1, 5 };
		// temporary storage
		private Span.Looper documentLooper;
		private List exampleList; 
		// type of annotation to produce
		private String annotationType;
		
		public CSMMLearner()
		{
			this( new CSMMSpanFE(), new VotedPerceptron(), 5, 5);
		}

		public CSMMLearner(int epochs)
		{
			this( new CSMMSpanFE(), new VotedPerceptron(), epochs, 5);
		}

		public CSMMLearner(int epochs, int maxSegmentSize)
		{
			this( new CSMMSpanFE(), new VotedPerceptron(), epochs, maxSegmentSize);
		}

		public CSMMLearner(String annotation)
		{
			this();
			((CSMMSpanFE)fe).setRequiredAnnotation(annotation,annotation+".mixup");
		}

		public CSMMLearner(
			SpanFeatureExtractor fe,OnlineBinaryClassifierLearner classifierLearner,int epochs,int maxSegmentSize)
		{
			this.fe = fe;
			this.classifierLearner = classifierLearner;
			this.epochs = epochs;
			this.maxSegmentSize[1] = maxSegmentSize;
			reset();
		}

		//
		// query all documents, and accumulate examples in exampleList
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
		private BinaryExample exampleFor(AnnotationExample example, Span span, Span prevSpan, double numberLabel)
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
			return new BinaryExample( instanceFromSeq, numberLabel);
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
	static public class CSMMAnnotator extends AbstractAnnotator	
	{
		private SpanFeatureExtractor fe;
		private BinaryClassifier classifier;
		private String annotationType;
		private int[] maxSegSize;
		public 
		CSMMAnnotator(SpanFeatureExtractor fe,BinaryClassifier classifier,String annotationType,int[] maxSegSize)
		{
			this.fe = fe;
			this.classifier = classifier;
			this.annotationType = annotationType;
			this.maxSegSize = maxSegSize;
		}
		public void doAnnotate(MonotonicTextLabels labels)
		{
			for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span doc = i.nextSpan();
				Segments viterbi = bestSegments(doc,labels,fe,classifier,maxSegSize);				
				for (Iterator j=viterbi.iterator(); j.hasNext(); ) {
					Span span = (Span)j.next();
					labels.addToType( span, annotationType );
				}
			}
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
		Span documentSpan,TextLabels labels,SpanFeatureExtractor fe,BinaryClassifier classifier,int[] maxSegSize)
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
					for (int lastT=Math.max(0, t-maxSegSize[y]); lastT<t; lastT++) {
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
		String prevLabel = lastY==1 ? ExampleSchema.POS_CLASS_NAME : ExampleSchema.NEG_CLASS_NAME;
		//System.out.println("score with labels "+labels.getClass());
 		Instance segmentInstance = 
			new InstanceFromSequence(fe.extractInstance(labels,segment),new String[]{prevLabel});
		if (DEBUG) log.debug("score: "+cls.score(segmentInstance)+"\t"+segment);
		//return cls.score(segmentInstance);
		if (y==1) return cls.score( segmentInstance );
		else return 0;
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
	static public class CSMMSpanFE extends SpanFE
	{
		private int windowSize = 5;
		private String requiredAnnotation = null;
		private String requiredAnnotationFileToLoad = null;

		public CSMMSpanFE() { super(new EmptyLabels()); }
		public void setRequiredAnnotation(String requiredAnnotation,String requiredAnnotationFileToLoad)
		{
			this.requiredAnnotation = requiredAnnotation;
			this.requiredAnnotationFileToLoad = requiredAnnotationFileToLoad;
		}
		public void extractFeatures(Span span) {
			extractFeatures(new EmptyLabels(),span);
		}
		public void extractFeatures(TextLabels labels,Span span) 
		{
			if (requiredAnnotation!=null) labels.require(requiredAnnotation,requiredAnnotationFileToLoad);

			// exact text of span
			from(span).eq().lc().emit();
			from(span).eq().charTypePattern().emit();
			// tokens in the span
			from(span).tokens().eq().lc().emit();
			from(span).eq().charTypePattern().emit();
			// length properties
			from(span).size().emit();
			from(span).exactSize().emit();
			// first and last tokens
			from(span).token(0).eq().lc().emit();
			from(span).token(0).eq().charTypePattern().lc().emit();
			from(span).token(-1).eq().lc().emit();
			from(span).token(-1).eq().charTypePattern().lc().emit();
			// window to left and right
			for (int i=0; i<windowSize; i++) {
				from(span).left().token(-(i+1)).eq().emit();
				from(span).left().token(-(i+1)).eq().charTypePattern().emit();
				from(span).right().token(i).eq().emit();
				from(span).right().token(i).eq().charTypePattern().emit();
			}
			// properties
			for (Iterator i=labels.getTokenProperties().iterator(); i.hasNext(); ) {
				String p = (String)i.next();
				// tokens
				from(span).tokens().prop(p).emit();
				// first & last
				from(span).token(0).prop(p).emit();
				from(span).token(-1).prop(p).emit();
				// window
				for (int j=0; j<windowSize; j++) {
					from(span).left().token(-(j+1)).prop(p).emit();
					from(span).right().token(j).prop(p).emit();
				}
			}
		}
	}

	static public class CSMMWithDictionarySpanFE extends CSMMSpanFE
	{
		SoftDictionary dictionary;
		StringDistance distances[];
		Feature features[];
		// distanceNames has to be "/" separated list of distance functions that
		// we want to apply 
		public CSMMWithDictionarySpanFE(String dictionaryFile, String distanceNames) { 
			super();
			try {
		    dictionary = new SoftDictionary();
		    dictionary.load(new File(dictionaryFile));
				
		    distances = DistanceLearnerFactory.buildArray(distanceNames);
		    // now create features corresponding to each distance function.
		    features = new Feature[distances.length];
		    for (int d = 0; d < distances.length; d++) {
					features[d] =  Feature.Factory.getFeature(distances[d].toString());
		    }
			} catch (IOException e) {
		    e.printStackTrace();
			}
		}
		public void extractFeatures(TextLabels labels,Span span) {
			super.extractFeatures(labels,span);
			String spanString = span.asString();
			String closestMatch = (String)dictionary.lookup(spanString);
			if (closestMatch != null) {
		    // create various types of similarity measures.
		    for (int d = 0; d < distances.length; d++) {
					double score = distances[d].score(spanString,closestMatch);
					if (score != 0) {
						// instance has been created by the parent.
						instance.addNumeric(features[d], score);
					}
		    }
			}
		}
	}

	// a proposed segmentation of a document
	static public class Segments
	{
		private Set spanSet;
		public Segments(Set spanSet)	{	this.spanSet = spanSet;	}
		public Span.Looper iterator() {	return new BasicSpanLooper(spanSet.iterator());	}
		public boolean contains(Span span) { return spanSet.contains(span);	}
		public String toString() { return "[Segments: "+spanSet.toString()+"]"; }
	}
}
