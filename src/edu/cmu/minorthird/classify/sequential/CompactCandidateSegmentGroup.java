/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
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

public class CompactCandidateSegmentGroup implements CandidateSegmentGroup
{
	private int maxWindowSize,sequenceLength, totalSize;
  private Set classNameSet;
	private String subPopId;
  // the segment from start to start+L is window[start][L-1].
	private Instance[] unitInstance;
  private Delta[][] delta;
	private ClassLabel[][] label;
  private Object[][] segmentSource;

  /** Creates a new holder for sliding-window instances.
   * @param maxWindowSize the maximum length of any sliding window
   * @param sequenceLength the length of the original sequence
   */
	public CompactCandidateSegmentGroup(FeatureFactory factory,CandidateSegmentGroup group)
	{ 
		this.sequenceLength = group.getSequenceLength();
		this.maxWindowSize = group.getMaxWindowSize ();
    this.totalSize = group.size();
    this.classNameSet = group.classNameSet();
    this.subPopId = group.getSubpopulationId();
    unitInstance = new Instance[sequenceLength];
		delta = new Delta[sequenceLength][maxWindowSize];
		label = new ClassLabel[sequenceLength][maxWindowSize];
		segmentSource = new Object[sequenceLength][maxWindowSize];
    for (int i=0; i<sequenceLength; i++) {
      unitInstance[i] = factory.compress( group.getSubsequenceInstance(i,i+1) );
    }
    for (int i=0; i<sequenceLength; i++) {
      for (int j=i+1; j-i<=maxWindowSize; j++) {
        if (group.getSubsequenceInstance(i,j)!=null) {
          label[i][j-i-1] = group.getSubsequenceLabel(i,j);
          segmentSource[i][j-i-1] = group.getSubsequenceInstance(i,j).getSource();
          delta[i][j-i-1] = new Delta(i,j,group.getSubsequenceInstance(i,j));
        }
      }
    }
	}

  //
  // helpers to construct feature iterators and/or compute weights
  //

  private Set binaryFeatureSet(int start,int end,Instance otherInstance)
  {
    Set s = new HashSet();
    for (int i=start; i<end; i++) {
      addAll( s, unitInstance[i].binaryFeatureIterator() ); 
    }
    if (otherInstance!=null) addAll( s, otherInstance.binaryFeatureIterator() );
    return s;
  }

  private Set numericFeatureSet(int start,int end,Instance otherInstance)
  {
    Set s = new HashSet();
    for (int i=start; i<end; i++) {
      addAll( s, unitInstance[i].numericFeatureIterator() ); 
    }
    if (otherInstance!=null) addAll( s, otherInstance.numericFeatureIterator() );
    return s;
  }

  private Set featureSet(int start,int end,Instance otherInstance)
  {
    Set s = new HashSet();
    s.addAll( binaryFeatureSet(start,end,otherInstance));
    s.addAll( numericFeatureSet(start,end,otherInstance));
    return s;
  }

  private void addAll(Set s,Iterator i) { while (i.hasNext()) s.add(i.next()); }

  private double getSumWeight(int start,int end,Feature f)
  {
    double w = 0;
    for (int i=start; i<end; i++) {
      w += unitInstance[i].getWeight(f);
    }
    return w;
  }

  // encode differences between a segmentInstance and the sum of the
  // weights of the unit instance

  private class Delta
  {
    public TObjectDoubleHashMap deltaWeight = new TObjectDoubleHashMap();
    public THashSet zeroWeights = new THashSet();
    public Delta(int start,int end,Instance segmentInstance)
    {
      for (Iterator i=featureSet(start,end,segmentInstance).iterator(); i.hasNext(); ) {
        Feature f = (Feature)i.next();
        double segmentWeight = segmentInstance.getWeight(f);
        if (segmentWeight==0) zeroWeights.add(f);
        else {
          double sumWeight = getSumWeight(start,end,f);
          if (segmentWeight!=sumWeight) deltaWeight.put( f, segmentWeight-sumWeight );
        }
      }
    }
  }

  //
  // lazily construct an instance from a Delta
  //
  private class DeltaInstance extends AbstractInstance
  {
    private int start,end;
    private Delta diff;

    public DeltaInstance(int start,int end)
    {
      this.start = start;
      this.end = end;
      this.diff = delta[start][end-start-1];
      this.source = segmentSource[start][end-start-1];  
      this.subpopulationId = subPopId;
    }
    public double getWeight(Feature f)
    {
      if (diff.zeroWeights.contains(f)) return 0;
      else return getSumWeight(start,end,f) + diff.deltaWeight.get(f);
    }
    public Feature.Looper binaryFeatureIterator() 
    { 
      return noZeros(diff.zeroWeights,binaryFeatureSet(start,end,null)); 
    }
    public Feature.Looper numericFeatureIterator() 
    { 
      return noZeros(diff.zeroWeights, numericFeatureSet(start,end,null));
    }
    public Feature.Looper featureIterator() 
    { 
      return noZeros(diff.zeroWeights, featureSet(start,end,null));
    }
    private Feature.Looper noZeros( Set exclude, Set set )
    {
      set.removeAll( exclude );
      return new Feature.Looper(set);
    }
  }

  //
  // implement the rest of the interface...
  //

	public Example getSubsequenceExample(int start,int end)
	{
    if (end-start==1) return new Example(unitInstance[start],label[start][0]);
    else if (delta[start][end-start-1]!=null) 
			return new Example(new DeltaInstance(start,end), label[start][end-start-1]);
		else 
			return null;
	}

  /** Return the class label associated with getSubsequenceExample(start,end).
   */
	public ClassLabel getSubsequenceLabel(int start,int end) { return label[start][end-start-1]; }

  /** Return the instance corresponding to the segment from positions start...end.
   */
	public Instance getSubsequenceInstance(int start,int end) 
  { 
    if (end-start==1) return new Example(unitInstance[start],label[start][0]);
    else if (delta[start][end-start-1]!=null) 
			return new DeltaInstance(start,end);
		else 
			return null;
  }

	public int getSequenceLength() { return sequenceLength; }

	public int getMaxWindowSize() { return maxWindowSize; }

	public String getSubpopulationId() { return subPopId; }

	public int size() {	return totalSize; }

	public Set classNameSet() { return classNameSet; }


}

