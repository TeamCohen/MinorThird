/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

/**
 *
 * @author William Cohen
 */

public class SemiMarkovLearner implements BatchSequenceClassifierLearner,SequenceConstants
{
	private static Logger log = Logger.getLogger(CollinsPerceptronLearner.class);
	private static final boolean DEBUG = log.isDebugEnabled();

	private OnlineClassifierLearner innerLearnerPrototype;
	private OnlineClassifierLearner[] innerLearner;
	private int numberOfEpochs;

	public SemiMarkovLearner(){	this(new MarginPerceptron(0.0,false,true)); }
	public SemiMarkovLearner(OnlineClassifierLearner innerLearner) { this(innerLearner,5); }
	public SemiMarkovLearner(int epochs) { this(new MarginPerceptron(0.0,false,true),epochs); }

	public SemiMarkovLearner(OnlineClassifierLearner innerLearner,int epochs)
	{
		this.innerLearnerPrototype = innerLearner;
		this.numberOfEpochs = epochs;
	}

	public void setSchema(ExampleSchema schema)	{	;	}

	//
	// accessors
	//
	public OnlineClassifierLearner getInnerLearner() { return innerLearnerPrototype; }
	public void setInnerLearner(OnlineClassifierLearner newInnerLearner) {this.innerLearnerPrototype = newInnerLearner; }
	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int newNumberOfEpochs) { this.numberOfEpochs = newNumberOfEpochs; }
	public int getHistorySize() { return 1; }

	//
	// training scheme
	//

	public SequenceClassifier batchTrain(SequenceDataset sequenceDataset)
	{
		if (!(sequenceDataset instanceof SlidingWindowDataset))
			throw new IllegalArgumentException("not a slidingWindowDataset!");

		SlidingWindowDataset dataset = (SlidingWindowDataset)sequenceDataset;
		int maxSegmentSize = dataset.getMaxWindowSize();
		ExampleSchema schema = dataset.getSchema();
		innerLearner = SequenceUtils.duplicatePrototypeLearner(innerLearnerPrototype,schema.getNumberOfClasses());

		ProgressCounter pc =
			new ProgressCounter("training semi-markov "+innerLearnerPrototype.toString(), 
													"sequence",numberOfEpochs*dataset.numberOfSequences());

		for (int epoch=0; epoch<numberOfEpochs; epoch++) 
		{
			dataset.shuffle();

			// statistics for curious researchers
			int sequenceErrors = 0;
			int transitionErrors = 0;
			int transitions = 0;

			for (SlidingWindowDataset.Looper i=dataset.subsequenceIterator(); i.hasNext(); ) 
			{
				Classifier c = new SequenceUtils.MultiClassClassifier(schema,innerLearner);
				Segments viterbi = new ViterbiSearcher(c,schema,maxSegmentSize).bestSegments(i);
				if (DEBUG) log.debug("classifier: "+c);
				if (DEBUG) log.debug("viterbi:\n"+viterbi);

				/*
				boolean errorOnThisSequence=false;

        // accumulate weights for transitions associated with each class k
				Hyperplane[] accumPos = new Hyperplane[schema.getNumberOfClasses()];
				Hyperplane[] accumNeg = new Hyperplane[schema.getNumberOfClasses()];
				for (int k=0; k<schema.getNumberOfClasses(); k++) {
					accumPos[k] = new Hyperplane();
					accumNeg[k] = new Hyperplane();
				}

				for (int j=0; j<sequence.length; j++) {
					// is the instance at sequence[j] associated with a difference in the sum
					// of feature values over the viterbi sequence and the actual one? 
					boolean differenceAtJ = !viterbi[j].isCorrect( sequence[j].getLabel() );
					//System.out.println("differenceAtJ for J="+j+" "+differenceAtJ+" - label");
					for (int k=1; j-k>=0 && !differenceAtJ && k<=historySize; k++) {
						if (!viterbi[j-k].isCorrect( sequence[j-k].getLabel() )) {
							//System.out.println("differenceAtJ for J="+j+" true: k="+k);
							differenceAtJ = true;
						}
					}
					if (differenceAtJ) {
						transitionErrors++;
						errorOnThisSequence=true;
						InstanceFromSequence.fillHistory( history, sequence, j );

						Instance correctXj = new InstanceFromSequence( sequence[j], history );
						int correctClassIndex = schema.getClassIndex( sequence[j].getLabel().bestClassName() );
						accumPos[correctClassIndex].increment( correctXj, +1.0 );
						accumNeg[correctClassIndex].increment( correctXj, -1.0 );
						if (DEBUG) log.debug("+ update "+sequence[j].getLabel().bestClassName()+" "+correctXj.getSource());

						InstanceFromSequence.fillHistory( history, viterbi, j );
						Instance wrongXj = new InstanceFromSequence( sequence[j], history );
						int wrongClassIndex = schema.getClassIndex( viterbi[j].bestClassName() );
						accumPos[wrongClassIndex].increment( wrongXj, -1.0 );
						accumNeg[wrongClassIndex].increment( wrongXj, +1.0 );
						if (DEBUG) log.debug("- update "+viterbi[j].bestClassName()+" "+wrongXj.getSource());
					}
				} // example sequence j
				if (errorOnThisSequence) {
					sequenceErrors++;
					String subPopId = sequence[0].getSubpopulationId();
					Object source = "no source";
					for (int k=0; k<schema.getNumberOfClasses(); k++) {
						//System.out.println("adding class="+k+" example: "+accumPos[k]);
						innerLearner[k].addExample( 
							new Example( new HyperplaneInstance(accumPos[k],subPopId,source), ClassLabel.positiveLabel(+1.0) ));
						innerLearner[k].addExample( 
							new Example( new HyperplaneInstance(accumNeg[k],subPopId,source), ClassLabel.negativeLabel(-1.0) ));
					}
				}
				transitions += sequence.length;
				*/
				pc.progress();
			} // sequence i

			System.out.println("Epoch "+epoch+": sequenceErr="+sequenceErrors
												 +" transitionErrors="+transitionErrors+"/"+transitions);

			if (transitionErrors==0) break;

		} // epoch
		pc.finished();
			
		for (int k=0; k<schema.getNumberOfClasses(); k++) {
			innerLearner[k].completeTraining();
		}

		// construct the classifier
		return null;
	}

	static public class ViterbiSearcher
	{
		private Classifier classifier;
		private ExampleSchema schema;
		private int maxSegmentSize;
		public ViterbiSearcher(Classifier classifier,ExampleSchema schema,int maxSegmentSize)
		{
			this.classifier = classifier;
			this.schema = schema;
			this.maxSegmentSize = maxSegmentSize;
		}
		public Segments bestSegments(SlidingWindowDataset.Looper i) 
		{
			// for t=0..size, y=0 or 1, fty[t][y] is the highest score that
			// can be obtained with a segmentation of the tokens from 0..t
			// that ends with class y (where y=1 means "from dictionary", y=0
			// means "from null model")

			// initialize
			String[] history = new String[1];
			Example[] seq = i.nextSequence();
			int ny = schema.getNumberOfClasses();
			int backgroundClass = schema.getClassIndex( ExampleSchema.NEG_CLASS_NAME );
			double[][] fty = new double[seq.length+1][ny];
			BackPointer[][] trace = new BackPointer[seq.length+1][ny];
			for (int t=0; t<seq.length+1; t++) {
				for (int y=0; y<ny; y++) {
					fty[t][y] = -99999; //could be -Double.MAX_VALUE;
					trace[t][y] = null;
				}
			}
			for (int y=0; y<ny; y++) fty[0][y] = 0;
		
			// fill the matrix fty[t][y] = score of maximal segmentation
			// from 0..t that ends in y
			for (int t=0; t<seq.length+1; t++) {
				for (int y=0; y<ny; y++) {
					for (int lastY=0; lastY<ny; lastY++) {
						int maxSegSizeForY = y==backgroundClass ? 1 : maxSegmentSize;
						for (int lastT=Math.max(0, t-maxSegSizeForY); lastT<t; lastT++) {
							// find the classifier's score for the subsequence from lastT to t
							// with label y and previous label lastY
							Example segmentExample = i.getSubsequenceExample(lastT, t);
							history[0] = schema.getClassName( lastY );
							InstanceFromSequence segmentInstance = new InstanceFromSequence(segmentExample.asInstance(),history);
							double segmentScore = classifier.classification(segmentInstance).getWeight( schema.getClassName(y) );
							// store the max score (over all lastT,lastY) in fty
							if (segmentScore + fty[lastT][lastY] > fty[t][y]) {
								fty[t][y] = segmentScore + fty[lastT][lastY];
								trace[t][y] = new BackPointer(lastT,t,lastY);
							}
						}
					}
				}
			}
		
			// use the back pointers to find the best segmentation that ends at t==documentSize
			int bestEndY = -1;
			double bestEndYScore = -Double.MAX_VALUE;
			for (int y=0; y<ny; y++) {
				if (fty[seq.length][y] > bestEndYScore) {
					bestEndYScore = fty[seq.length][y];
					bestEndY = y;
				}
			}
			Segments result = new Segments();
			int y = bestEndY;
			for (BackPointer bp = trace[seq.length][y]; bp!=null; bp=trace[bp.lastT][bp.lastY]) {
				bp.onBestPath = true;
				if (y!=backgroundClass) result.add( new Segment(bp.lastT,bp.t) );
				y = bp.lastY;
			}
			if (DEBUG) dumpStuff(i,fty,trace);
			return result;
		}
	}
	private static class BackPointer {
		public int lastT, t,lastY;
		public boolean onBestPath;
		public BackPointer(int lastT, int t,int lastY) {
			this.lastT=lastT;
			this.t=t;
			this.lastY=lastY;
			this.onBestPath=false;
		}
	}

	private static void dumpStuff(SlidingWindowDataset.Looper i, double[][] fty, BackPointer[][] trace)
	{
		Example nullExample = new Example(new MutableInstance("*NULL*"),new ClassLabel("*NULL*"));
		java.text.DecimalFormat format = new java.text.DecimalFormat("####.###");
		System.out.println("t.y\tf(t,y)\tt'.y'\tspan");
		for (int t=0; t<fty.length; t++) {
			for (int y=0; y<2; y++) {
				BackPointer bp = trace[t][y];				
				Example ex = bp==null ? nullExample : i.getSubsequenceExample(bp.lastT,bp.t);
				if (bp==null) bp = new BackPointer(-1,-1,-1);
				String marker = bp.onBestPath? "<==" : "";
				System.out.println(t+"."+y+"\t"+format.format(fty[t][y])+"\t"+
													 bp.lastT+"."+bp.lastY+"  '"+ex.getSource()+"' "+marker);
			}
		}
	}

	static public class Segments extends TreeSet {
	}

	static public class Segment implements Comparable
	{
		public final int lo,hi;
		public Segment(int lo,int hi) { this.lo=lo; this.hi=hi; }
		public int compareTo(Object o) {
			Segment b = (Segment)o;
			int cmp = lo - b.lo;
			if (cmp!=0) return cmp;
			return hi - b.hi;
		}
	}
}

