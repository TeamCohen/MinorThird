/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.util.*;

import java.util.*;
import org.apache.log4j.*;

/**
 *
 * Semi-markov version of GenericCollinsLearner. 
 *
 * @author William Cohen
 */

public class SegmentGenericCollinsLearner implements BatchSegmenterLearner,SequenceConstants
{
	private static Logger log = Logger.getLogger(CollinsPerceptronLearner.class);
	private static final boolean DEBUG = log.isDebugEnabled();

	private OnlineClassifierLearner innerLearnerPrototype;
	private OnlineClassifierLearner[] innerLearner;
	private int numberOfEpochs;
	private int maxSegmentSize;

	public SegmentGenericCollinsLearner()
	{
		this(new MarginPerceptron(0.0,false,true));
	}

	public SegmentGenericCollinsLearner(OnlineClassifierLearner innerLearner)
	{
		this(innerLearner,5);
	}

	public SegmentGenericCollinsLearner(int epochs)
	{
		this(new MarginPerceptron(0.0,false,true),epochs);
	}

	public SegmentGenericCollinsLearner(OnlineClassifierLearner innerLearner,int epochs)
	{
		this(innerLearner,4,epochs);
	}

	public SegmentGenericCollinsLearner(OnlineClassifierLearner innerLearner,int maxSegmentSize,int epochs)
	{
		this.maxSegmentSize = maxSegmentSize;
		this.innerLearnerPrototype = innerLearner;
		this.numberOfEpochs = epochs;
	}

	public void setSchema(ExampleSchema schema)	{	;	}

	//
	// accessors
	//
	public OnlineClassifierLearner getInnerLearner() { 
		return innerLearnerPrototype; 
	}
	public void setInnerLearner(OnlineClassifierLearner newInnerLearner) {
		this.innerLearnerPrototype = newInnerLearner;
	}
	public int getHistorySize() { return 1; }
	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int newNumberOfEpochs) { this.numberOfEpochs = newNumberOfEpochs; }
	

	public Segmenter batchTrain(SegmentDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		innerLearner = SequenceUtils.duplicatePrototypeLearner(innerLearnerPrototype,schema.getNumberOfClasses());

		ProgressCounter pc =
			new ProgressCounter("training segments "+innerLearnerPrototype.toString(), 
													"sequence",numberOfEpochs*dataset.getNumberOfSegmentGroups());

		for (int epoch=0; epoch<numberOfEpochs; epoch++) 
		{
			//dataset.shuffle();

			// statistics for curious researchers
			int sequenceErrors = 0;
			int transitionErrors = 0;
			int transitions = 0;

			for (SegmentDataset.Looper i=dataset.candidateSegmentGroupIterator(); i.hasNext(); ) 
			{
				Classifier c = new SequenceUtils.MultiClassClassifier(schema,innerLearner);
				if (DEBUG) log.debug("classifier is: "+c);

				CandidateSegmentGroup g = i.nextCandidateSegmentGroup();
				Segmentation viterbi = 
					new SegmentCollinsPerceptronLearner.ViterbiSearcher(c,schema,maxSegmentSize).bestSegments(g);
				if (DEBUG) log.debug("viterbi "+maxSegmentSize+"\n"+viterbi);
				Segmentation correct = correctSegments(g,schema,maxSegmentSize);
				if (DEBUG) log.debug("correct segments:\n"+correct);

				boolean errorOnThisSequence=false;

        // accumulate weights for transitions associated with each class k
				Hyperplane[] accumPos = new Hyperplane[schema.getNumberOfClasses()];
				Hyperplane[] accumNeg = new Hyperplane[schema.getNumberOfClasses()];
				for (int k=0; k<schema.getNumberOfClasses(); k++) {
					accumPos[k] = new Hyperplane();
					accumNeg[k] = new Hyperplane();
				}

				int fp = compareSegmentsAndIncrement(schema, viterbi, correct, accumNeg, +1, g);
				if (fp>0) errorOnThisSequence = true;
				int fn = compareSegmentsAndIncrement(schema, correct, viterbi, accumPos, +1, g);
				if (fn>0) errorOnThisSequence = true;
				if (errorOnThisSequence) sequenceErrors++;
				transitionErrors += fp+fn;

				if (errorOnThisSequence) {
					sequenceErrors++;
					String subPopId = g.getSubpopulationId();
					Object source = "no source";
					for (int k=0; k<schema.getNumberOfClasses(); k++) {
						//System.out.println("adding class="+k+" example: "+accumPos[k]);
						innerLearner[k].addExample( 
							new Example( new HyperplaneInstance(accumPos[k],subPopId,source), ClassLabel.positiveLabel(+1.0) ));
						innerLearner[k].addExample( 
							new Example( new HyperplaneInstance(accumNeg[k],subPopId,source), ClassLabel.negativeLabel(-1.0) ));
					}
					
				}

				transitions += correct.size();
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

		Classifier c = new SequenceUtils.MultiClassClassifier(schema,innerLearner);
		return new SegmentCollinsPerceptronLearner.ViterbiSegmenter(c, schema, maxSegmentSize);
	}

	/** Compare the target segments to the 'otherSegments', and update
	 * the classifier by sum_x [delta*x], for each example x
	 * corresponding to a target segment that's not in otherSegments.
	 */
	private int compareSegmentsAndIncrement(
		ExampleSchema schema,Segmentation segments,Segmentation otherSegments,
		Hyperplane[] accum,double delta,CandidateSegmentGroup g)
	{
		int errors = 0;
		// first, work out the name of the previous class for each segment
		Map map = previousClassMap(segments,schema);
		Map otherMap = previousClassMap(otherSegments,schema);
		String[] history = new String[1];
		for (Iterator j=segments.iterator(); j.hasNext(); ) {
			Segmentation.Segment seg = (Segmentation.Segment)j.next();
			String previousClass = (String) map.get(seg);
			if (seg.lo>=0 && (!otherSegments.contains(seg) || !otherMap.get(seg).equals(previousClass))) {
				errors++;
				history[0] = previousClass;
				Instance instance = new InstanceFromSequence( g.getSubsequenceExample(seg.lo,seg.hi), history);
				if (DEBUG) log.debug("class "+schema.getClassName(seg.y)+" update "+delta+" for: "+instance.getSource());
				accum[seg.y].increment( instance, delta );
			}
		}
		return errors;
	}

	/** Build a mapping from segment to string name of previous segment.
	 * This should let you look up segments which are logically
	 * equivalent, as well as ones which are pointer-equivalent (==)
	 */
	private Map previousClassMap(Segmentation segments,ExampleSchema schema)
	{
		// use a treemap so that logically equivalent segments be mapped to same previousClass
		Map map = new TreeMap(); 
		Segmentation.Segment previousSeg = null;
		for (Iterator j=segments.iterator(); j.hasNext(); ) {
			Segmentation.Segment seg = (Segmentation.Segment)j.next();
			String previousClassName = previousSeg==null ? NULL_CLASS_NAME : schema.getClassName(previousSeg.y);
			map.put( seg, previousClassName);
			previousSeg = seg;
		}
		return map;
	}

	/** Collect the correct segments for this example.  These are defined as 
	 * all segments with non-NEGATIVE labels, and all unit-length negative labels
	 * not inside a positives label.
	 */
	private Segmentation correctSegments(CandidateSegmentGroup g,ExampleSchema schema,int maxSegmentSize)
	{
		Segmentation result = new Segmentation(schema);
		int pos, len;
		for (pos=0; pos<g.getSequenceLength(); ) {
			boolean addedASegmentStartingAtPos = false;
			for (len=1; !addedASegmentStartingAtPos && len<=maxSegmentSize; len++) {
				Instance inst = g.getSubsequenceInstance(pos,pos+len);
				ClassLabel label = g.getSubsequenceLabel(pos,pos+len);
				if (inst!=null && !label.isNegative()) {
					result.add( new Segmentation.Segment(pos,pos+len,schema.getClassIndex(label.bestClassName())) );
					addedASegmentStartingAtPos = true;
					pos += len;
				}
			}
			if (!addedASegmentStartingAtPos) {
				Instance inst = g.getSubsequenceInstance(pos,pos+1);
				ClassLabel label = g.getSubsequenceLabel(pos,pos+1);
				result.add( new Segmentation.Segment(pos,pos+1,schema.getClassIndex(ExampleSchema.NEG_CLASS_NAME)) );
				pos += 1;
			}
		}
		return result;
	}

}
