package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.random.RandomElement;

import java.util.*;

/** 
 * Do N-fold cross-validation, where N is the number of different
 * subpopulations.
 *
 * @author William Cohen
 */

public class LeaveOneOutSplitter implements Splitter
{
	private RandomElement random;
  private Splitter crossValSplitter;
  
	public LeaveOneOutSplitter()
  {
    this(new RandomElement(0));
  }

	public LeaveOneOutSplitter(RandomElement random)
	{
		this.random = random; 
	}

  public void split(Iterator i)
  {
    List buf = new ArrayList();
    Set subpops = new HashSet();
    while (i.hasNext()) {
      Object o = i.next();
      buf.add(o);
      // find subpop id, and record it
      String id = (o instanceof HasSubpopulationId)
                  ? ((HasSubpopulationId)o).getSubpopulationId()
                  : "youNeeekID#"+subpops.size();
      subpops.add(id);
    }
    crossValSplitter = new CrossValSplitter(random,subpops.size());
    crossValSplitter.split(buf.iterator());
  }

	public int getNumPartitions() { return crossValSplitter.getNumPartitions(); }

	public Iterator getTrain(int k) { return crossValSplitter.getTrain(k); }

	public Iterator getTest(int k) { return crossValSplitter.getTest(k); }

	public String toString() { return "[LeaveOneOutSplitter]"; }
}

