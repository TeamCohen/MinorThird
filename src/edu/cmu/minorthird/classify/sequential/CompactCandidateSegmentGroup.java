/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import java.io.Serializable;
import java.util.*;
import gnu.trove.*;

/**
 * A more space-efficient version of a CandidateSegmentGroup.
 *
 * Space is saved by explicitly storing the instances for the
 * unit-length segments, plus "deltas" for each non-unit length segment.
 * Each "delta" encodes the difference between the segment instance
 * and the sum of the unit-length instances it covers.
 * 
 * @author William Cohen
 */

public class CompactCandidateSegmentGroup implements CandidateSegmentGroup,
		Serializable{

	static final long serialVersionUID=20080207L;
	
	private int maxWindowSize,sequenceLength,totalSize;

	private Set<String> classNameSet;

	private String subPopId;

	// the segment from start to start+L is window[start][L-1].
	private Instance[] unitInstance;

	private Delta[][] delta;

	private ClassLabel[][] label;

	private Object[][] segmentSource;

	/** Creates a new holder for sliding-window instances. */
	public CompactCandidateSegmentGroup(FeatureFactory factory,
			CandidateSegmentGroup group){
		// The length of the original sequence
		this.sequenceLength=group.getSequenceLength();
		// The maximum length of any sliding window
		this.maxWindowSize=group.getMaxWindowSize();
		this.totalSize=group.size();
		this.classNameSet=group.classNameSet();
		this.subPopId=group.getSubpopulationId();
		unitInstance=new Instance[sequenceLength];
		delta=new Delta[sequenceLength][maxWindowSize];
		label=new ClassLabel[sequenceLength][maxWindowSize];
		segmentSource=new Object[sequenceLength][maxWindowSize];
		for(int i=0;i<sequenceLength;i++){
			unitInstance[i]=factory.compress(group.getSubsequenceInstance(i,i+1));
		}
		for(int i=0;i<sequenceLength;i++){
			for(int j=i+1;j-i<=maxWindowSize;j++){
				if(group.getSubsequenceInstance(i,j)!=null){
					label[i][j-i-1]=group.getSubsequenceLabel(i,j);
					segmentSource[i][j-i-1]=group.getSubsequenceInstance(i,j).getSource();
					delta[i][j-i-1]=
							new Delta(factory,i,j,group.getSubsequenceInstance(i,j));
				}
			}
		}
	}

	//
	// helpers to construct feature iterators and/or compute weights
	//

	/** The binary features in in any unitInstance between start...end
	 * or otherInstance.  Equivalently, the features in the sum of
	 * {unitInstance[start],...,unitInstance[end-1],otherInstance}
	 */
	private Set<Feature> binaryFeatureSet(int start,int end,Instance otherInstance){
		Set<Feature> s=new HashSet<Feature>();
		for(int i=start;i<end;i++){
			addAll(s,unitInstance[i].binaryFeatureIterator());
		}
		if(otherInstance!=null)
			addAll(s,otherInstance.binaryFeatureIterator());
		return s;
	}

	/** Analogous to binaryFeatureSet */
	private Set<Feature> numericFeatureSet(int start,int end,Instance otherInstance){
		Set<Feature> s=new HashSet<Feature>();
		for(int i=start;i<end;i++){
			addAll(s,unitInstance[i].numericFeatureIterator());
		}
		if(otherInstance!=null)
			addAll(s,otherInstance.numericFeatureIterator());
		return s;
	}

	/** Analogous to binaryFeatureSet */
	private Set<Feature> featureSet(int start,int end,Instance otherInstance){
		Set<Feature> s=new HashSet<Feature>();
		s.addAll(binaryFeatureSet(start,end,otherInstance));
		s.addAll(numericFeatureSet(start,end,otherInstance));
		return s;
	}

	private void addAll(Set<Feature> s,Iterator<Feature> i){
		while(i.hasNext())
			s.add(i.next());
	}

	/** Get sum of weight of f over in all unitInstance between start and end */
	private double getSumWeight(int start,int end,Feature f){
		double w=0;
		for(int i=start;i<end;i++){
			w+=unitInstance[i].getWeight(f);
		}
		return w;
	}

	/** encode differences between a segmentInstance and the sum of the
	 * weights of the unit instances between start and end.
	 */

	private class Delta implements Serializable{

		static final long serialVersionUID=20080207L;
		
		public TObjectDoubleHashMap deltaWeight=new TObjectDoubleHashMap();

		public THashSet zeroWeights=new THashSet();

		public Delta(FeatureFactory factory,int start,int end,
				Instance segmentInstance){
			for(Iterator<Feature> i=featureSet(start,end,segmentInstance).iterator();i
					.hasNext();){
				Feature f=i.next();
				// replace the feature with its canonical version, so
				// that variant versions are not stored in the
				// deltaWeight, zeroWeights hash tables
				f=factory.getFeature(f);
				double segmentWeight=segmentInstance.getWeight(f);
				if(segmentWeight==0)
					zeroWeights.add(f);
				else{
					double sumWeight=getSumWeight(start,end,f);
					if(segmentWeight!=sumWeight)
						deltaWeight.put(f,segmentWeight-sumWeight);
				}
			}
			/*
			  System.out.println("segmentInstance: "+segmentInstance);
			  System.out.println("deltaInstance:   "+new DeltaInstance(start,end,this,
			  segmentInstance.getSource(),
			  segmentInstance.getSubpopulationId()));
			 */
		}
	}

	/** Construct an instance from the unit instances and a delta. 
	 */

	private class DeltaInstance extends AbstractInstance implements Serializable{

		static final long serialVersionUID=20080207L;
		
		private int start,end;

		private Delta diff;

		public DeltaInstance(int start,int end){
			this.start=start;
			this.end=end;
			this.diff=delta[start][end-start-1];
			this.source=segmentSource[start][end-start-1];
			this.subpopulationId=subPopId;
		}

		// for debugging mostly
		public DeltaInstance(int start,int end,Delta initDelta,Object initSource,
				String initSubPopId){
			this.start=start;
			this.end=end;
			this.diff=initDelta;
			this.source=initSource;
			this.subpopulationId=initSubPopId;
		}

		@Override
		public double getWeight(Feature f){
			if(diff.zeroWeights.contains(f))
				return 0;
			else
				return getSumWeight(start,end,f)+diff.deltaWeight.get(f);
		}

		@Override
		public Iterator<Feature> binaryFeatureIterator(){
			return adjust(binaryFeatureSet(start,end,null),diff.zeroWeights,null);
		}

		@Override
		public Iterator<Feature> numericFeatureIterator(){
			return adjust(numericFeatureSet(start,end,null),diff.zeroWeights,
					diff.deltaWeight);
		}

		@Override
		public Iterator<Feature> featureIterator(){
			return adjust(featureSet(start,end,null),diff.zeroWeights,
					diff.deltaWeight);
		}
		
		@Override
		public int numFeatures(){
			System.err.println("numFeatures not implemented!");
			return -1;
		}

		private Iterator<Feature> adjust(final Set<Feature> set,THashSet exclude,
				TObjectDoubleHashMap include){
			// like set.removeAll(exclude) but faster
			exclude.forEach(new TObjectProcedure(){
				@Override
				public boolean execute(Object o){
					set.remove(o);
					return true; // indicates it's ok to invoke this procedure again
				}
			});
			if(include!=null){
				// like set.addAll( include ) but faster
				include.forEachKey(new TObjectProcedure(){
					@Override
					public boolean execute(Object key){
						set.add((Feature)key);
						return true; // indicates it's ok to invoke this procedure again
					}
				});
			}
			return set.iterator();
		}
	}

	//
	// implement the rest of the interface...
	//

	@Override
	public Example getSubsequenceExample(int start,int end){
		if(end-start==1)
			return new Example(unitInstance[start],label[start][0]);
		else if(delta[start][end-start-1]!=null)
			return new Example(new DeltaInstance(start,end),label[start][end-start-1]);
		else
			return null;
	}

	/** Return the class label associated with getSubsequenceExample(start,end).
	 */
	@Override
	public ClassLabel getSubsequenceLabel(int start,int end){
		return label[start][end-start-1];
	}

	/** Return the instance corresponding to the segment from positions start...end.
	 */
	@Override
	public Instance getSubsequenceInstance(int start,int end){
		if(end-start==1)
			return new Example(unitInstance[start],label[start][0]);
		else if(delta[start][end-start-1]!=null)
			return new DeltaInstance(start,end);
		else
			return null;
	}

	@Override
	public int getSequenceLength(){
		return sequenceLength;
	}

	@Override
	public int getMaxWindowSize(){
		return maxWindowSize;
	}

	@Override
	public String getSubpopulationId(){
		return subPopId;
	}

	@Override
	public int size(){
		return totalSize;
	}

	@Override
	public Set<String> classNameSet(){
		return classNameSet;
	}

}
