package edu.cmu.minorthird.classify.experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.cmu.minorthird.classify.HasSubpopulationId;

/**
 * Helper class for splitting up iterators over Instances
 * 
 * @author William Cohen
 */

public class SubpopSorter<T>{

	private SortedMap<String,List<T>> clusterMap; // map subpopulationId's to arrayLists of examples

	private List<String> subpopList;

	private int nextUniqId=0;

	/**
	 * Create a SubpopSorter. Iterator i must iterate over Instances.
	 */
	public SubpopSorter(Random random,Iterator<T> i){
		clusterMap=new TreeMap<String,List<T>>();
		while(i.hasNext()){
			T instance=i.next();
			String id=null;
			if(instance instanceof HasSubpopulationId){
				id=((HasSubpopulationId)instance).getSubpopulationId();
			}
			if(id==null){
				id="youNeeekID#"+(nextUniqId++);
			}
			List<T> list=clusterMap.get(id);
			if(list==null){
				clusterMap.put(id,(list=new ArrayList<T>()));
			}
			list.add(instance);
		}
		subpopList=new ArrayList<String>(clusterMap.keySet().size());
		for(Iterator<String> j=clusterMap.keySet().iterator();j.hasNext();){
			String key=j.next();
			Collections.shuffle(clusterMap.get(key),random);
			subpopList.add(key);
		}
		Collections.shuffle(subpopList,random);
	}
	
	/**
	 * Create a SubpopSorter. Iterator i must iterate over Instances.
	 */
	public SubpopSorter(Iterator<T> i){
		// not sure if this should be Random() or Random(0) - frank
		this(new Random(),i);
	}

	/**
	 * Return an iterator over lists of subpopulations. The subpopulations, and
	 * the lists of Instances within each subpopulation, are randomly ordered.
	 */
	public Iterator<List<T>> subpopIterator(){
		return new MyIterator(0);
	}

	private class MyIterator implements Iterator<List<T>>{

		private int i;

		public MyIterator(int i){
			this.i=i;
		}

		@Override
		public boolean hasNext(){
			return i<subpopList.size();
		}

		@Override
		public List<T> next(){
			return clusterMap.get(subpopList.get(i++));
		}

		@Override
		public void remove(){
			throw new UnsupportedOperationException("can't remove");
		}
	}
}
