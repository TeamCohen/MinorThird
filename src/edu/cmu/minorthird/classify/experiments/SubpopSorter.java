package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.HasSubpopulationId;
import edu.cmu.minorthird.classify.algorithms.random.RandomElement;

import java.util.*;

/** Helper class for splitting up iterators over Instances
 *
 * @author William Cohen
 */

/*package*/ class SubpopSorter
{
	private Map clusterMap;   // map subpopulationId's to arrayLists of examples
	private List subpopList;
	private int nextUniqId = 0;

	/** Create a SubpopSorter. Iterator i must iterate over
	 * Instances. */
	public SubpopSorter(Iterator i)
	{
		this(new RandomElement(),i);
	}

	/** Create a SubpopSorter. Iterator i must iterate over
	 * Instances. */
	public SubpopSorter(RandomElement random,Iterator i)
	{
		clusterMap = new TreeMap();
		while (i.hasNext()) {
			Object o = i.next();
			String id = null;
			if (o instanceof HasSubpopulationId) {
				id = ((HasSubpopulationId)o).getSubpopulationId();
			}
			if (id==null) id = "youNeeekID#"+(nextUniqId++);
			ArrayList list = (ArrayList)clusterMap.get(id);
			if (list==null) clusterMap.put(id, (list=new ArrayList()));
			list.add( o );
		}
		subpopList = new ArrayList( clusterMap.keySet().size() );
		for (Iterator j=clusterMap.keySet().iterator(); j.hasNext(); ) {
			Object key = j.next();
			Collections.shuffle( (ArrayList)clusterMap.get(key),new Random(0) );
			subpopList.add( key );
		}
		Collections.shuffle( subpopList,new Random(0) );
	}

	/** Return an iterator over lists of subpopulations.
	 * The subpopulations, and the lists of Instances within
	 * each subpopulation, are randomly ordered.
	 */
	public Iterator subpopIterator() {
		return new MyIterator(0);
	}

	private class MyIterator implements Iterator {
		private int i;
		public MyIterator(int i) {this.i = i;}
		public boolean hasNext() { return i<subpopList.size(); }
		public Object next() { return clusterMap.get( subpopList.get(i++) ); }
		public void remove() { throw new UnsupportedOperationException("can't remove"); }
	}
}
