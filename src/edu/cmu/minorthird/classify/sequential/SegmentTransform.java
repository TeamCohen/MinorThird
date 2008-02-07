package edu.cmu.minorthird.classify.sequential;

import java.util.Iterator;

import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.transform.InstanceTransform;
import edu.cmu.minorthird.util.ProgressCounter;

/*package*/ class SegmentTransform
{
  private InstanceTransform innerTransform;

  public SegmentTransform(InstanceTransform innerTransform) { this.innerTransform = innerTransform; }

  public SegmentDataset transform(SegmentDataset dataset)
  { 
    ProgressCounter pc = 
      new ProgressCounter("adding dictionary distances","segment group",dataset.getNumberOfSegmentGroups());
    SegmentDataset transformed = new SegmentDataset();
    for (Iterator<CandidateSegmentGroup> i = dataset.candidateSegmentGroupIterator(); i.hasNext(); ) {
      transformed.addCandidateSegmentGroup( transform(i.next()) );
      pc.progress();
    }
    pc.finished();
    return transformed;
  }
  public CandidateSegmentGroup transform(CandidateSegmentGroup g) 
  { 
    MutableCandidateSegmentGroup result = 
      new MutableCandidateSegmentGroup(g.getMaxWindowSize(), g.getSequenceLength());
    for (int lo=0; lo<g.getSequenceLength(); lo++) {
      for (int len=1; len<=g.getMaxWindowSize(); len++) {
        Instance gInstance = g.getSubsequenceInstance(lo,lo+len);
        if (gInstance!=null) {
          Instance tInstance = innerTransform.transform(gInstance);
          //Feature f = new Feature("distToSome POS");
          //if (tInstance.getWeight(f)!=0) System.out.println("useful instance: "+tInstance);
          result.setSubsequence(lo, lo+len, tInstance, g.getSubsequenceLabel(lo,lo+len));
        }
      }
    }
    return result;
  }
}
