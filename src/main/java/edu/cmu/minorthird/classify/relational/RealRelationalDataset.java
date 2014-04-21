/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.relational;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.SGMExample;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * A real set of examples for stacked graphical learning -- coreset + relational
 * template. Currently the legalAggregators include EXISTS and COUNT
 * 
 * @author Zhenzhen Kou
 */

public class RealRelationalDataset extends CoreRelationalDataset implements
		Visible,Saveable,Dataset,Serializable{

	static final long serialVersionUID=20080128L;

	protected static Map<String,Set<String>> aggregators=new HashMap<String,Set<String>>();

	public static Set<String> legalAggregators=new HashSet<String>();

	static{
		legalAggregators.add("EXISTS");
		legalAggregators.add("COUNT");
	}

	/**
	 * Add an aggregator, i.e., save the info. in RelTemp sccript
	 * 
	 * @param oper
	 * @param L_type
	 */
	static public void addAggregator(String oper,String L_type){

		if(legalAggregators.contains(oper)){
			if(aggregators.containsKey(L_type)){
				aggregators.get(L_type).add(oper);
			}else{
				HashSet<String> set=new HashSet<String>();
				set.add(oper);
				// why o why clone? - frank
				aggregators.put(L_type,(HashSet<String>)set.clone());
			}

		}else{
			System.out.println(oper+" is not a legal aggregator");
		}
		return;

	}

	public static Map<String,Set<String>> getAggregators(){
		return aggregators;
	}

	public static void setAggregators(Map<String,Set<String>> aggregators){
		RealRelationalDataset.aggregators=aggregators;
	}
	
//	public Iterator<Example> iterator(){
//		final Iterator<Example> inner=examples.iterator();
//		return new Iterator<Example>(){
//			public boolean hasNext(){
//				return inner.hasNext();
//			}
//			public Example next(){
//				return inner.next();
//			}
//			public void remove(){
//				inner.remove();
//			}
//		};
//	}
	
	@Override
	public Split split(final Splitter<Example> splitter){
		splitter.split(iterator());
		return new Split(){

			@Override
			public int getNumPartitions(){
				return splitter.getNumPartitions();
			}

			@Override
			public Dataset getTrain(int k){
				return invertIteration(splitter.getTrain(k));
			}

			@Override
			public Dataset getTest(int k){
				return invertIteration(splitter.getTest(k));
			}
		};
	}
	
	private RealRelationalDataset invertIteration(Iterator<Example> i){
		RealRelationalDataset copy=new RealRelationalDataset();
		while(i.hasNext()){
			copy.addSGM((SGMExample)i.next());
		}
		return copy;
	}

//	public Split splitSGM(final Splitter<SGMExample> splitter){
//		splitter.split(iteratorSGM());
//		return new Split(){
//
//			public int getNumPartitions(){
//				return splitter.getNumPartitions();
//			}
//
//			public Dataset getTrain(int k){
//				return invertIterationSGM(splitter.getTrain(k));
//			}
//
//			public Dataset getTest(int k){
//				return invertIterationSGM(splitter.getTest(k));
//			}
//		};
//	}
//
//	private RealRelationalDataset invertIterationSGM(Iterator<SGMExample> i){
//		RealRelationalDataset copy=new RealRelationalDataset();
//		while(i.hasNext()){
//			copy.addSGM(i.next());
//		}
//		return copy;
//	}

	// test routine

	/** Simple test routine */
	static public void main(String[] args){
		try{
			RealRelationalDataset data=new RealRelationalDataset();
			DatasetLoader.loadRelFile(new File("test.osf"),data);
			DatasetLoader.loadLinkFile(new File("test.lsf"),data);
			DatasetLoader.loadRelTempFile(new File("relTemp.txt"),data);
			System.out.println(data.getSchema());
			System.out.println("Aggregators: "+RealRelationalDataset.getAggregators());
			for(Iterator<Example> i=data.examples.iterator();i.hasNext();){
				SGMExample e=(SGMExample)i.next();
				System.out.println(e);

			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
