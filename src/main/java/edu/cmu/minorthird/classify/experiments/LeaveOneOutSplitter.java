package edu.cmu.minorthird.classify.experiments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.minorthird.classify.HasSubpopulationId;
import edu.cmu.minorthird.classify.Splitter;

/** 
 * Do N-fold cross-validation, where N is the number of different
 * subpopulations.
 *
 * @author William Cohen
 */

public class LeaveOneOutSplitter<T> implements Splitter<T>{

	private Random random;

	private Splitter<T> crossValSplitter;
	
	public LeaveOneOutSplitter(Random random){
		this.random=random;
	}

	public LeaveOneOutSplitter(){
		this(new Random());
	}

	@Override
	public void split(Iterator<T> i){
		List<T> buf=new ArrayList<T>();
		Set<String> subpops=new HashSet<String>();
		while(i.hasNext()){
			T t=i.next();
			buf.add(t);
			// find subpop id, and record it
			String id;
			if(t instanceof HasSubpopulationId){
				id=((HasSubpopulationId)t).getSubpopulationId();
			}
			else{
				id="youNeeekID#"+subpops.size();
			}
			subpops.add(id);
		}
		crossValSplitter=new CrossValSplitter<T>(random,subpops.size());
		crossValSplitter.split(buf.iterator());
	}

	@Override
	public int getNumPartitions(){
		return crossValSplitter.getNumPartitions();
	}

	@Override
	public Iterator<T> getTrain(int k){
		return crossValSplitter.getTrain(k);
	}

	@Override
	public Iterator<T> getTest(int k){
		return crossValSplitter.getTest(k);
	}

	@Override
	public String toString(){
		return "[LeaveOneOutSplitter]";
	}
}
