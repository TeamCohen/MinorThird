/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.StringUtil;

/**
 * A conditional markov model classifier.
 * 
 * @author William Cohen
 */

public class BeamSearcher implements SequenceConstants,Serializable{

	static private final long serialVersionUID=20080207L;

	static private boolean OLD_VERSION=false;

	static private Logger log=Logger.getLogger(BeamSearcher.class);

	static private final boolean DEBUG=false;

	// parameters of beam searcher
	private int historySize;

	private String[] possibleClassLabels;

	private Classifier classifier;

	private int beamSize=10;

	// caches current beam search
	
	transient private Beam beam=new Beam();

	private boolean caching=false;
	transient private Instance[] instances;
	transient private int numInstances;

	transient private String[] history;

	public BeamSearcher(Classifier classifier,int historySize,ExampleSchema schema){
		this.classifier=classifier;
		this.historySize=historySize;
		this.possibleClassLabels=schema.validClassNames();
		// System.out.println(Arrays.toString(possibleClassLabels));
		if(possibleClassLabels.length<2)
			throw new IllegalArgumentException("possibleClassLabels.length="+
					possibleClassLabels.length+" <2 ???");
	}

	public int getMaxBeamSize(){
		return beamSize;
	}

	public void setMaxBeamSize(int n){
		beamSize=n;
	}
	
	public boolean isCaching(){
		return caching;
	}
	
	public void setCaching(boolean caching){
		this.caching=caching;
	}

	/** Get the best label sequence, as determined by the beam search */
	public ClassLabel[] bestLabelSequence(Instance[] instances){
		doSearch(instances);
		return viterbi(0);
	}

	static public Instance getBeamInstance(Instance instance,int historySize){
		String[] history=new String[historySize];
		InstanceFromSequence.fillHistory(history,new String[]{},0);
		return new InstanceFromSequence(instance,history);
	}

	/** Do a beam search. */
	public void doSearch(Instance[] sequence){
		this.instances=sequence;

		if(DEBUG)
			log.debug("searching over a "+sequence.length+"-instance sequence");
		if(DEBUG)
			log.debug("beamSize="+beamSize+" classes="+
					StringUtil.toString(possibleClassLabels));

		if(possibleClassLabels.length<2)
			throw new IllegalStateException("possibleClassLabels.length="+
					possibleClassLabels.length+" <2 ???");

		history=new String[historySize];
		beam=new Beam();
		beam.add(new BeamEntry());

		for(int i=0;i<instances.length;i++){
			if(DEBUG)
				log.debug("predicting class for instance["+i+"]: "+
						instances[i].getSource());

			Beam nextBeam=new Beam();

			for(int j=0;j<Math.min(beam.size(),beamSize);j++){
				BeamEntry entry=beam.get(j);
				if(DEBUG)
					log.debug("beam entry["+j+"]: "+entry);

				// classify example based on this history
				Instance beamInstance=entry.getBeamInstance(instances[i]);
				ClassLabel label=classifier.classification(beamInstance);
				if(DEBUG)
					log.debug("class for "+beamInstance+" => "+label);

				// add all possible classifications to the beam for the next iteration
				for(int el=0;el<possibleClassLabels.length;el++){
					double w=label.getWeight(possibleClassLabels[el]);
					nextBeam.add(entry.extend(possibleClassLabels[el],w));
					if(DEBUG)
						log.debug("extending beam with "+possibleClassLabels[el]+
								" score: "+w);
				}
			}
			nextBeam.sort();
			beam=nextBeam;
		}
		
		numInstances=this.instances.length;
		if(!caching){
			this.instances=null;
		}
		
	}

	/**
	 * Do a beam search, constraining the bestLabel for each classification to
	 * match the non-null values in the template.
	 * 
	 *<p>
	 * This would be better folded in with the one-arg version of doSearch, but is
	 * kept separate for backward compatibility.
	 */
	public void doSearch(Instance[] sequence,ClassLabel[] template){
		
		this.instances=sequence;

		if(DEBUG)
			log.debug("searching over a "+sequence.length+"-instance sequence");
		if(DEBUG)
			log.debug("beamSize="+beamSize+" classes="+
					StringUtil.toString(possibleClassLabels));

		if(possibleClassLabels.length<2)
			throw new IllegalStateException("possibleClassLabels.length="+
					possibleClassLabels.length+" <2 ???");

		history=new String[historySize];
		beam=new Beam();
		beam.add(new BeamEntry());

		for(int i=0;i<instances.length;i++){
			if(DEBUG)
				log.debug("predicting class for instance["+i+"]: "+
						instances[i].getSource());

			Beam nextBeam=new Beam();

			for(int j=0;j<Math.min(beam.size(),beamSize);j++){
				BeamEntry entry=beam.get(j);
				if(DEBUG)
					log.debug("beam entry["+j+"]: "+entry);

				// classify example based on this history
				Instance beamInstance=entry.getBeamInstance(instances[i]);
				ClassLabel label=classifier.classification(beamInstance);
				if(DEBUG)
					log.debug("class for "+beamInstance+" => "+label);

				// add all possible classifications to the beam for the next iteration
				for(int el=0;el<possibleClassLabels.length;el++){
					if(template.length<i+1||template[i]==null||
							template[i].bestClassName().equals(possibleClassLabels[el])){
						double w=label.getWeight(possibleClassLabels[el]);
						nextBeam.add(entry.extend(possibleClassLabels[el],w));
						if(DEBUG)
							log.debug("extending beam with "+possibleClassLabels[el]+
									" score: "+w);
					}else{
						if(DEBUG)
							log.debug("discarding "+possibleClassLabels[el]+
									" as template mismatch");
					}
				}
			}
			nextBeam.sort();
			beam=nextBeam;
		}
		
		numInstances=this.instances.length;
		if(!caching){
			this.instances=null;
		}
		
	}

	/** Return the number of solutions found in the beam. */
	public int getNumberOfSolutionsFound(){
		return beam.size();
	}

	/**
	 * Retrieve the k-th best result of the previous beam search. To get the best,
	 * use viterbi(0), the second best is viterbi(1), etc.
	 */
	public ClassLabel[] viterbi(int k){
		ClassLabel[] result=new ClassLabel[numInstances];
		BeamEntry entry=beam.get(k);
		for(int i=0;i<numInstances;i++){
			result[i]=entry.toClassLabel(i);
		}
		return result;
	}

	public float score(int k){
		return (float)beam.get(k).score;
	}

	public String explain(Instance[] sequence){
		StringBuffer buf=new StringBuffer("");
		doSearch(sequence);
		BeamEntry targetEntry=beam.get(0);
		BeamEntry entry=new BeamEntry();
		for(int i=0;i<sequence.length;i++){
			buf.append("Classification for instance "+i+" is "+targetEntry.labels[i]+
					" (score "+targetEntry.scores[i]+"):\n");
			buf.append(classifier.explain(entry.getBeamInstance(sequence[i])));
			entry=entry.extend(targetEntry.labels[i],targetEntry.scores[i]);
			buf.append("\nRunning total score: "+entry.score+"\n\n");
		}
		return buf.toString();
	}

	public Explanation getExplanation(Instance[] sequence){
		doSearch(sequence);
		BeamEntry targetEntry=beam.get(0);
		BeamEntry entry=new BeamEntry();
		Explanation.Node top=new Explanation.Node("BeamSearcher Classification");
		for(int i=0;i<sequence.length;i++){
			Explanation.Node seqEx=
					new Explanation.Node("Classification for instance "+i+" is "+
							targetEntry.labels[i]+" (score "+targetEntry.scores[i]+"):\n");
			Explanation.Node explan=
					classifier.getExplanation(sequence[i]).getTopNode();
			if(explan==null)
				explan=
						new Explanation.Node(classifier.explain(entry
								.getBeamInstance(sequence[i])));
			seqEx.add(explan);
			entry=entry.extend(targetEntry.labels[i],targetEntry.scores[i]);
			Explanation.Node score=
					new Explanation.Node("\nRunning total score: "+entry.score+"\n\n");
			seqEx.add(score);
			top.add(seqEx);
		}
		Explanation ex=new Explanation(top);
		return ex;
	}

	/** The search space. */
	private class Beam{

		private List<BeamEntry> list=new ArrayList<BeamEntry>();

		// maps last historySize labels ->
		private Map<BeamKey,BeamEntry> keyMap=new HashMap<BeamKey,BeamEntry>();

		public BeamEntry get(int i){
			return list.get(i);
		}

		public void add(BeamEntry entry){
			BeamKey key=new BeamKey(entry);
			BeamEntry existingEntry=keyMap.get(key);
			if(existingEntry==null||existingEntry.score<entry.score){
				if(existingEntry!=null)
					list.remove(existingEntry);
				list.add(entry);
				keyMap.put(key,entry);
			}
		}

		public int size(){
			return list.size();
		}

		public void sort(){
			Collections.sort(list);
		}
	}

	/** A single entry in the beam */
	private class BeamEntry implements Comparable<BeamEntry>{

		/* Labels assigned so far. */
		public String[] labels=new String[0];

		/* Score associated with each label assigned so far. */
		public double[] scores=new double[0];

		/** Total score of labels so far */
		public double score=0.0;

		/** Implement Comparable */
		@Override
		public int compareTo(BeamEntry other){
			return MathUtil.sign(other.score-score);
		}

		/** Convert i-th label stored to a class label */
		public ClassLabel toClassLabel(int i){
			return new ClassLabel(labels[i],scores[i]);
		}

		/** Extend the beam with one additional label */
		public BeamEntry extend(String label,double labelScore){
			BeamEntry result=new BeamEntry();
			result.labels=new String[labels.length+1];
			result.scores=new double[labels.length+1];
			for(int i=0;i<labels.length;i++){
				result.labels[i]=labels[i];
				result.scores[i]=scores[i];
			}
			result.labels[labels.length]=label;
			result.scores[labels.length]=labelScore;
			result.score=score+labelScore;
			return result;
		}

		public Instance getBeamInstance(Instance instance){
			fillHistory(history);
			return new InstanceFromSequence(instance,history);
		}

		public void fillHistory(String[] history){
			InstanceFromSequence.fillHistory(history,labels,labels.length);
		}

		@Override
		public String toString(){
			return "[entry: "+labels+";"+scores+"; score:"+score+"]";
		}
	}

	/**
	 * Used to look for BeamEntry's that should be combined. BeamEntrys should be
	 * combined in the beam (with the higher score being retained) if their
	 * history is the same.
	 */
	private class BeamKey{

		private String[] keyHistory=new String[historySize];

		public BeamKey(BeamEntry entry){
			entry.fillHistory(keyHistory);
		}

		@Override
		public int hashCode(){
			int h=73643674;
			for(int i=0;i<keyHistory.length;i++){
				if(OLD_VERSION)
					h=h^keyHistory.hashCode();
				else
					h=h^keyHistory[i].hashCode();
			}
			return h;
		}

		@Override
		public boolean equals(Object o){
			if(!(o instanceof BeamKey))
				return false;
			BeamKey b=(BeamKey)o;
			if(!(b.keyHistory.length==keyHistory.length))
				return false;
			for(int i=0;i<b.keyHistory.length;i++){
				if(!keyHistory[i].equals(b.keyHistory[i]))
					return false;
			}
			return true;
		}

		@Override
		public String toString(){
			String path="[Key ";
			for(int i=0;i<keyHistory.length;i++){
				path+=(keyHistory[i]+" ");
			}
			path+="]";
			return path;
		}
	}
}
