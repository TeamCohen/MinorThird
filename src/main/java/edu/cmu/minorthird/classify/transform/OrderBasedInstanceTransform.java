package edu.cmu.minorthird.classify.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;

/**
 * @author Edoardo M. Airoldi
 * Date: Feb 6, 2004
 */

public class OrderBasedInstanceTransform implements InstanceTransform{

//	static private Logger log=Logger.getLogger(InfoGainInstanceTransform.class);

	private int TOP_FEATURES;

	private List<Pair> values;

	/** Constructor */
	public OrderBasedInstanceTransform(){
		this.TOP_FEATURES=100; // or max number of features if < 100
		this.values=new ArrayList<Pair>();
	}

	/** Not used */
	@Override
	public Instance transform(Instance instance){
		System.out.println("Warning: cannot transform instance with Info-Gain!");
		return instance;
	}

	/** Not used */
	@Override
	public Example transform(Example example){
		System.out.println("Warning: cannot transform example with Info-Gain!");
		return example;
	}

	/** Transform a dataset according to Info-Gain criterion */
	@Override
	public Dataset transform(Dataset dataset){
		final Comparator<Pair> VAL_COMPARATOR=new Comparator<Pair>(){
			@Override
			public int compare(Pair ig1,Pair ig2){
				if(ig1.value<ig2.value)
					return 1;
				else if(ig1.value>ig2.value)
					return -1;
				else
					return (ig1.feature).compareTo(ig2.feature);
			}
		};

		// collect features to keep
		Collections.sort(values,VAL_COMPARATOR);
		int maxIndex=Math.min(values.size(),TOP_FEATURES);
		Set<Feature> availableFeatures=new HashSet<Feature>();
		for(int j=0;j<maxIndex;j++){
			availableFeatures.add(values.get(j).feature);
		}
		// create masked dataset
		BasicDataset maskeDataset=new BasicDataset();
		for(Iterator<Example> i=dataset.iterator();i.hasNext();){
			Example e=i.next();
			Instance mi=new MaskedInstance(e.asInstance(),availableFeatures);
			Example ex=new Example(mi,e.getLabel());
			maskeDataset.add(ex);
		}
		return maskeDataset;
	}

	/** A class that we use to sort a TreeMap by values */
	private class Pair extends Object{

		double value;

		Feature feature;

		public Pair(double v,Feature f){
			this.value=v;
			this.feature=f;
		}

		@Override
		public String toString(){
			return "[ "+this.value+","+this.feature+" ]"; //this.key + " ]";
		}
	}

	// Accessory Methods

	/** Number of features with the highest Info-Gain scores to keep in the dataset */
	public void setNumberOfFeatures(int number){
		this.TOP_FEATURES=number;
	}

	/** Adds the Info-Gain score of feature f to the InstanceTransform */
	public void addFeatureVal(double infoGain,Feature f){
		Pair p=new Pair(infoGain,f);
		values.add(p);
	}

}
