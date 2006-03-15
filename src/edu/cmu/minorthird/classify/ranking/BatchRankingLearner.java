/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.ranking;

import edu.cmu.minorthird.classify.*;
import java.util.*;


/**
 * Learns to rank sets of examples, rather than to classify individual examples.
 *
 * Data is presented to a BatchRankingLearner as an ordinary binary
 * dataset.  Examples from the same subpopulation are comparable, and
 * should be ranked so that positive examples have a higher score than
 * negative examples.
 *
 * @author William Cohen
 */

public abstract class BatchRankingLearner extends BatchBinaryClassifierLearner
{
	/** Sort a dataset into 'rankings'.  Each ranking is a List of
	 * Examples such that all positive examples in the list should be
	 * ranked above all negative examples. Returns a map so that
	 * map.get(key) is an ArrayList of examples.
	 */
	static public Map splitIntoRankings(Dataset data)
	{
		Map map = new HashMap();
		for (Example.Looper i=data.iterator(); i.hasNext(); ) {
	    Example ex = i.nextExample();
	    List list = (List)map.get( ex.getSubpopulationId() );
	    if (list==null) map.put( ex.getSubpopulationId(), (list = new ArrayList()) );
	    list.add( ex );
		}
		return map;
	}

	/**
	 * Split a Map output by splitIntoRankings into lists that contain
	 * exactly one positive example each.
	 */
	static public Map listsWithOneExampleEach(Map rankingLists)
	{
		Map map1 = new HashMap();
		for (Iterator i=rankingLists.keySet().iterator(); i.hasNext(); ) {
			String key = (String)i.next();
			List posExamples = new ArrayList();
			List negExamples = new ArrayList();
			List ranking = (List)rankingLists.get(key);
			for (int j=0; j<ranking.size(); j++) {	
				Example exi = (Example)ranking.get(j);
				if (exi.getLabel().isPositive()) {
					posExamples.add(exi);
				} else {
					negExamples.add(exi);
				}
			}
			for (int j=0; j<posExamples.size(); j++) {
				Example exi = (Example)posExamples.get(j);
				List ranking1 = new ArrayList();
				ranking1.addAll( negExamples );
				ranking1.add( exi );
				map1.put( key+"."+j, ranking1 );
			}
		}
		return map1;
	}

	/** Sort a List of Instances by score according to the classifier. */
	static public void sortByScore( final BinaryClassifier c, List data)
	{
		Collections.sort( data, new Comparator() {
				public int compare(Object a,Object b) {
					Instance instA = (Instance)a;
					Instance instB = (Instance)b;
					double diff = c.score( instB ) - c.score( instA );
					int cmp = diff>0 ? +1 : (diff<0 ? -1: 0 );
					if (cmp!=0) return cmp;
					// rather than be random, sort negative examples
					// above positive if scores are the same
					if ((a instanceof Example) && (b instanceof Example)) {
						Example exA = (Example)a;
						Example exB = (Example)b;
						return (int)(exA.getLabel().numericLabel() - exB.getLabel().numericLabel());
					} else {
						return 0;
					}
				}
	    });
	}
}
