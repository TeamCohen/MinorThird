/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.MathUtil;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/** 
 * A conditional markov model classifier.
 * 
 * @author William Cohen
*/

public class BeamSearcher implements SequenceConstants, Serializable
{
	static private int serialVersionUID = 1;
	static private Logger log = Logger.getLogger(BeamSearcher.class);
	static private final boolean DEBUG = false;

	// parameters of beam searcher
	private int historySize; 
	private String[] possibleClassLabels;
	private Classifier classifier;
	private int beamSize = 10;

	// caches current beam search
	transient private ArrayList beam = new ArrayList();
	transient private Instance[] instances;
	transient private String[] history;

	public BeamSearcher(Classifier classifier,int historySize,ExampleSchema schema)
	{
		this.classifier = classifier;
		this.historySize = historySize;
		this.possibleClassLabels = schema.validClassNames();
	}

	public int getMaxBeamSize() { return beamSize; }
	public void setMaxBeamSize(int n) { beamSize=n; }

	
	/** Get the best label sequence, as determined by the beam search */
	public ClassLabel[] bestLabelSequence(Instance[] instances)
	{
		doSearch(instances);
		return viterbi(0);
	}

	/** Do a beam search. */
	public void doSearch(Instance[] sequence)
	{
		this.instances = sequence;

		history = new String[historySize];
		beam = new ArrayList();
		beam.add( new BeamEntry() );

		for (int i=0; i<instances.length; i++) {
			if (DEBUG) log.debug("predicting class for instance["+i+"]: "+instances[i].getSource());

			ArrayList nextBeam = new ArrayList();

			for (int j=0; j<Math.min( beam.size(), beamSize); j++) {
				BeamEntry entry = (BeamEntry)beam.get(j);
				if (DEBUG) log.debug("beam entry["+j+"]: "+entry);

				// classify example based on this history
				Instance beamInstance = entry.getBeamInstance(instances[i]);
				ClassLabel label = classifier.classification(beamInstance);
				if (DEBUG) log.debug("class for "+beamInstance+" => "+label);

				// add other possible classifications to the beam for the next iteration
				for (int el=0; el<possibleClassLabels.length; el++) {
					double w = label.getWeight(possibleClassLabels[el]);
					nextBeam.add( entry.extend( possibleClassLabels[el], w ) ); 
					if (DEBUG) log.debug("extending beam with "+possibleClassLabels[el]+" score: "+w);
				}
			}
			Collections.sort(nextBeam);
			beam = nextBeam;
		}
	}

	/** Return the number of solutions found in the beam. */
	public int getNumberOfSolutionsFound()
	{
		return beam.size();
	}

	/** Retrieve the k-th best result of the previous beam search.  To
	 * get the best, use viterbi(0), the second best is viterbi(1),
	 * etc. */
	public ClassLabel[] viterbi(int k)
	{
		ClassLabel[] result = new ClassLabel[instances.length];
		BeamEntry entry = (BeamEntry)beam.get(k);	
		for (int i=0; i<instances.length; i++) {
			result[i] = entry.toClassLabel(i);
		}
		return result;
	}

	public String explain(Instance[] sequence)
	{
		StringBuffer buf = new StringBuffer("");
		doSearch(sequence);
		BeamEntry targetEntry = (BeamEntry)beam.get(0);	
		BeamEntry entry = new BeamEntry();
		for (int i=0; i<sequence.length; i++) {
			buf.append("Classification for instance "+i+" is "
								 +targetEntry.labels[i]+" (score "+targetEntry.scores[i]+"):\n");
			buf.append( classifier.explain(entry.getBeamInstance(sequence[i])) );
			entry = entry.extend( targetEntry.labels[i], targetEntry.scores[i] );
			buf.append("\nRunning total score: "+entry.score+"\n\n");
		}
		return buf.toString();
	}

	/** A single entry in the beam */
	private class BeamEntry implements Comparable
	{
		/* Labels assigned so far. */
		public String[] labels = new String[0];
		/* Score associated with each label assigned so far. */
		public double[] scores = new double[0];
		/** Total score of labels so far */
		public double score = 0.0;

		/** Implement Comparable */
		public int compareTo(Object other) 
		{
			return MathUtil.sign(((BeamEntry)other).score - score);
		}

		/** Convert i-th label stored to a class label */
		public ClassLabel toClassLabel(int i)
		{
			return new ClassLabel(labels[i],scores[i]);
		}

		/** Extend the beam with one additional label */
		public BeamEntry extend(String label,double labelScore)
		{
			BeamEntry result = new BeamEntry();
			result.labels = new String[ labels.length+1 ];
			result.scores = new double[ labels.length+1 ];
			for (int i=0; i<labels.length; i++) {
				result.labels[i] = labels[i];
				result.scores[i] = scores[i];
			}
			result.labels[labels.length] = label;
			result.scores[labels.length] = labelScore;
			result.score = score+labelScore;
			return result;
		}
		public Instance getBeamInstance(Instance instance)
		{
			fillHistory(history);
			return new InstanceFromSequence(instance,history);
		}

		private void fillHistory(String[] history)
		{
			InstanceFromSequence.fillHistory( history, labels, labels.length );
		}
		public String toString()
		{
			return "[entry: "+labels+";"+scores+"; score:"+score+"]";
		}
	}
}
