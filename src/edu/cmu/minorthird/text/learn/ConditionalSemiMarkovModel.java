/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.gui.*;

import org.apache.log4j.Logger;
import java.util.*;

//
// comments: a problem with this is that if score(Carmen Sandiego,+)=4 and
// score(Carmen,-)=3 and score(Sandiego,-)=3, then it's better to pick
// <<Carmen,-1><Sandiego,-1>> over <<Carmen Sandiego>+1>
// 
//

/**
 * Learn to annotate based on a conditional semi-markov model
 * learned from examples.
 *
 * @author William Cohen
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
		// temporary storage
		private Span.Looper documentLooper;
		private List exampleList; 
		// type of annotation to produce
		private String annotationType;
		
		public CSMMLearner()
		{
			this( new CSMMSpanFE(), new VotedPerceptron(), 5);
		}

		public CSMMLearner(
			SpanFeatureExtractor fe,OnlineBinaryClassifierLearner classifierLearner,int epochs)
		{
			this.fe = fe;
			this.classifierLearner = classifierLearner;
			this.epochs = epochs;
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

			for (int i=0; i<epochs; i++) {

				for (Iterator j=exampleList.iterator(); j.hasNext(); ) {

					AnnotationExample example = (AnnotationExample)j.next();
					Span doc = example.getDocumentSpan();
					if (DEBUG) log.debug("updating from "+doc);

					// get best segmentation, given current classifier
					Segments viterbi = bestSegments(doc,fe,classifierLearner.getBinaryClassifier());
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


				}//epoch

				if (DEBUG) {
					ViewerFrame f = 
						new ViewerFrame("classifier after epoch "+i, 
														new SmartVanillaViewer( classifierLearner.getBinaryClassifier() ));
				}

			}//all epochs


			return new CSMMAnnotator(fe,classifierLearner.getBinaryClassifier(),annotationType);
		}

		// build an example from a span and its context
		private BinaryExample exampleFor(AnnotationExample example, Span span, Span prevSpan, double numberLabel)
		{
			Instance instance =  fe.extractInstance(span);
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


	static public class CSMMAnnotator extends AbstractAnnotator	
	{
		private SpanFeatureExtractor fe;
		private BinaryClassifier classifier;
		private String annotationType;
		public CSMMAnnotator(SpanFeatureExtractor fe,BinaryClassifier classifier,String annotationType)
		{
			this.fe = fe;
			this.classifier = classifier;
			this.annotationType = annotationType;
		}
		public void doAnnotate(MonotonicTextLabels labels)
		{
			for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span doc = i.nextSpan();
				Segments viterbi = bestSegments(doc,fe,classifier);				
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
	// some sort of Searcher class would be nicer here...
	// 

	static private int[] maxSegmentSize = new int[]{ 1, 5 };

	// viterbi
	static public Segments bestSegments(Span documentSpan,SpanFeatureExtractor fe,BinaryClassifier classifier)
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
					for (int lastT=Math.max(0, t-maxSegmentSize[y]); lastT<t; lastT++) {
						Span segment = documentSpan.subSpan(lastT, t-lastT);
						double segmentScore = score(lastY,y,lastT,t,segment,fe,classifier);
						if (segmentScore + fty[lastT][lastY] > fty[t][y]) {
							fty[t][y] = segmentScore + fty[lastT][lastY];
							trace[t][y] = new BackPointer(segment,lastT,lastY);
						}
					}
				}
			}
		}
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
	score(int lastY,int y,int lastT,int t,Span segment,SpanFeatureExtractor fe,BinaryClassifier cls)
	{
		String prevLabel = lastY==1 ? ExampleSchema.POS_CLASS_NAME : ExampleSchema.NEG_CLASS_NAME;
		Instance segmentInstance = new InstanceFromSequence(fe.extractInstance(segment),new String[]{prevLabel});
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
	
	static public class CSMMSpanFE extends SpanFE
	{
		private int windowSize = 2;
		public CSMMSpanFE() { super(new EmptyLabels()); }
		public void extractFeatures(Span span) {
			extractFeatures(new EmptyLabels(),span);
		}
		public void extractFeatures(TextLabels labels,Span span) {
			from(span).eq().emit();
			from(span).tokens().eq().lc().emit();
			from(span).eq().charTypePattern().emit();
			from(span).size().emit();
			for (int i=0; i<windowSize; i++) {
				from(span).left().token(-(i+1)).eq().charTypePattern().emit();
				from(span).left().token(-(i+1)).eq().charTypePattern().emit();
				from(span).right().token(i).eq().charTypePattern().emit();
				from(span).right().token(i).eq().charTypePattern().emit();
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
