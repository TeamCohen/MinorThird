package edu.cmu.minorthird.classify.experiments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Splitter;

/**
 * Works with datasets of binary examples.  Splits POS and NEG examples into
 * k separate disjoints folds, separately, and then returns k train/test splits
 * where each train set is the union of k-1 folds, and the test set is the k-th
 * fold.  Does NOT preserve subpopulation information.
 *
 * @author Edoardo Airoldi
 * Date: Dec 8, 2003
 */

public class StratifiedCrossValSplitter implements Splitter<Example>{

//    static private Logger log = Logger.getLogger(StratifiedCrossValSplitter.class);

	private Random random;

	private int folds;

	private List<List<Example>> strata;

	public StratifiedCrossValSplitter(Random random,int folds){
		this.random=random;
		this.folds=folds;
	}

	public StratifiedCrossValSplitter(int folds){
		this(new Random(),folds);
	}

	public StratifiedCrossValSplitter(){
		this(5);
	}

	@Override
	public void split(Iterator<Example> i){
		strata=new ArrayList<List<Example>>();
		for(Iterator<List<Example>> j=new StrataSorter(random,i).strataIterator();j.hasNext();){
			strata.add(j.next());
		}
	}

	@Override
	public int getNumPartitions(){
		return folds;
	}

	@Override
	public Iterator<Example> getTrain(int k){
		List<Example> trainList=new ArrayList<Example>();
		for(int i=0;i<strata.size();i++){
			for(int j=0;j<strata.get(i).size();j++){
				if(j%folds!=k){
					trainList.add(strata.get(i).get(j));
				}
			}
		}
		return trainList.iterator();
	}

	@Override
	public Iterator<Example> getTest(int k){
		List<Example> testList=new ArrayList<Example>();
		for(int i=0;i<strata.size();i++){
			for(int j=0;j<strata.get(i).size();j++){
				if(j%folds==k){
					testList.add(strata.get(i).get(j));
				}
			}
		}
		return testList.iterator();
	}

	@Override
	public String toString(){
		return "["+folds+"-Stratified CV splitter]";
	}

}
