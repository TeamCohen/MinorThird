/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.StringUtil;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

/** 
 * A conditional markov model classifier.
 * 
 * @author William Cohen
*/

public class BeamSearcher implements SequenceConstants, Serializable
{
	static private long serialVersionUID = 1;
	private final int CURRENT_SERIAL_VERSION = 1;

	static private boolean OLD_VERSION = false;

	static private Logger log = Logger.getLogger(BeamSearcher.class);
	static private final boolean DEBUG = false;

	// parameters of beam searcher
	private int historySize; 
	private String[] possibleClassLabels;
	private Classifier classifier;
	private int beamSize = 10;

	// caches current beam search
	transient private Beam beam = new Beam();
	transient private Instance[] instances;
	transient private String[] history;

	public BeamSearcher(Classifier classifier,int historySize,ExampleSchema schema)
	{
		this.classifier = classifier;
		this.historySize = historySize;
		this.possibleClassLabels = schema.validClassNames();
		if (possibleClassLabels.length<2) 
			throw new IllegalArgumentException("possibleClassLabels.length="+possibleClassLabels.length+" <2 ???");
	}

	public int getMaxBeamSize() { return beamSize; }

	public void setMaxBeamSize(int n) { beamSize=n; }
	
	/** Get the best label sequence, as determined by the beam search */
	public ClassLabel[] bestLabelSequence(Instance[] instances)
	{
		doSearch(instances);
		return viterbi(0);
	}

  static public Instance getBeamInstance(Instance instance,int historySize)
  {
    String[] history = new String[historySize];
    InstanceFromSequence.fillHistory( history, new String[]{}, 0);
    return new InstanceFromSequence(instance,history);
  }

	/** Do a beam search. */
	public void doSearch(Instance[] sequence)
	{
		this.instances = sequence;

		if (DEBUG) log.debug("searching over a "+sequence.length+"-instance sequence");
		if (DEBUG) log.debug("beamSize="+beamSize+" classes="+StringUtil.toString(possibleClassLabels));

		if (possibleClassLabels.length<2) 
			throw new IllegalStateException("possibleClassLabels.length="+possibleClassLabels.length+" <2 ???");

		history = new String[historySize];
		beam = new Beam();
		beam.add( new BeamEntry() );

		for (int i=0; i<instances.length; i++) {
			if (DEBUG) log.debug("predicting class for instance["+i+"]: "+instances[i].getSource());

			Beam nextBeam = new Beam();

			for (int j=0; j<Math.min( beam.size(), beamSize); j++) {
				BeamEntry entry = beam.get(j);
				if (DEBUG) log.debug("beam entry["+j+"]: "+entry);
				
				// classify example based on this history
				Instance beamInstance = entry.getBeamInstance(instances[i]);
				ClassLabel label = classifier.classification(beamInstance);
				if (DEBUG) log.debug("class for "+beamInstance+" => "+label);

				// add all possible classifications to the beam for the next iteration
				for (int el=0; el<possibleClassLabels.length; el++) {
					double w = label.getWeight(possibleClassLabels[el]);
					nextBeam.add( entry.extend( possibleClassLabels[el], w ) ); 
					if (DEBUG) log.debug("extending beam with "+possibleClassLabels[el]+" score: "+w);
				}
			}
			nextBeam.sort();
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
		BeamEntry entry = beam.get(k);	
		for (int i=0; i<instances.length; i++) {
			result[i] = entry.toClassLabel(i);
		}
		return result;
	}
  public float score(int k) {
    return (float)beam.get(k).score;
  }
	public String explain(Instance[] sequence)
	{
		StringBuffer buf = new StringBuffer("");
		doSearch(sequence);
		BeamEntry targetEntry = beam.get(0);	
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

	/** The search space. */
	private class Beam 
	{
		private ArrayList list = new ArrayList();
		// maps last historySize labels -> 
		private HashMap keyMap = new HashMap();
		
		public BeamEntry get(int i)	{	return (BeamEntry)list.get(i); }

		public void add(BeamEntry entry)
		{
			BeamKey key = new BeamKey(entry); 
			BeamEntry existingEntry = (BeamEntry) keyMap.get( key );
			if (existingEntry==null || existingEntry.score<entry.score) {
				if (existingEntry!=null) list.remove( existingEntry );
				list.add( entry );
				keyMap.put( key, entry );
			}
		}

		public int size()	{ return list.size(); }
		
		public void sort() { Collections.sort(list); }
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
		public void fillHistory(String[] history)
		{
			InstanceFromSequence.fillHistory( history, labels, labels.length );
		}
		public String toString()
		{
			return "[entry: "+labels+";"+scores+"; score:"+score+"]";
		}
	}

	/** Used to look for BeamEntry's that should be combined. BeamEntrys
	 * should be combined in the beam (with the higher score being
	 * retained) if their history is the same. */
	private class BeamKey
	{
		private String[] keyHistory = new String[historySize];
		public BeamKey(BeamEntry entry)
		{
			entry.fillHistory(keyHistory);
		}
		public int hashCode()
		{
			int h = 73643674;
			for (int i=0; i<keyHistory.length; i++) {
				if (OLD_VERSION) h = h^keyHistory.hashCode();
				else h = h^keyHistory[i].hashCode();
			}
			return h;
		}
		public boolean equals(Object o)
		{
			if (!(o instanceof BeamKey)) return false;
			BeamKey b = (BeamKey)o;
			if (!(b.keyHistory.length==keyHistory.length)) return false;
			for (int i=0; i<b.keyHistory.length; i++) {
				if (!keyHistory[i].equals(b.keyHistory[i])) return false;
			}
			return true;
		}
	    public String toString() {
		String path = "[Key ";
		for (int i=0; i<keyHistory.length; i++) {
		    path += (keyHistory[i] + " ");
		}
		path += "]";
		return path;
	    }
	}
}
