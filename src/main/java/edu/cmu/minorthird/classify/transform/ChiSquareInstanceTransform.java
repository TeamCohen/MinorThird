package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;
import java.util.*;

/**
 * @author Vitor R. Carvalho
 * Date: March 2005
 * 
 * A simple feature filter based on Chi-squared statistic.
 */

public class ChiSquareInstanceTransform extends AbstractInstanceTransform{

	private int TOP_FEATURES;

	private List<Pair> featValues;//keeps all features

	private Set<Feature> availableFeatures=new HashSet<Feature>(); //keeps high score features

	private boolean isSorted;

	/** Default Constructor - numFeatures=100 */
	public ChiSquareInstanceTransform(){
		this.TOP_FEATURES=100; // default
		this.featValues=new ArrayList<Pair>();
	}

	/** Constructor - parameter is number of features */
	public ChiSquareInstanceTransform(int num){
		this.TOP_FEATURES=num; // number of top ranked features
		this.featValues=new ArrayList<Pair>();
	}

	/** Transform an instance according to Info-Gain criterion */
	@Override
	public Instance transform(Instance ins){
		// collect features to keep
		if(!isSorted){
			Collections.sort(featValues,VAL_COMPARATOR);
			isSorted=true;
			int maxIndex=Math.min(featValues.size(),TOP_FEATURES);
//			availableFeatures = new HashSet();
			for(int j=0;j<maxIndex;j++){
				availableFeatures.add((featValues.get(j)).feature);
			}
		}
		return new MaskedInstance(ins,availableFeatures);
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

	final Comparator<Pair> VAL_COMPARATOR=new Comparator<Pair>(){
		@Override
		public int compare(Pair ig1,Pair ig2){
			if(ig1.value<ig2.value)
				return 1;
			else if(ig1.value>ig2.value)
				return -1;
			else
				return ig1.feature.compareTo(ig2.feature);
		}
	};

	/** displays the top ranked features and their scores*/
	public String toString(int top){
		Collections.sort(featValues,VAL_COMPARATOR);
		int maxIndex=Math.min(featValues.size(),top);
		StringBuffer sb=new StringBuffer();
		for(int j=0;j<maxIndex;j++){
			Feature f=featValues.get(j).feature;
			double val=featValues.get(j).value;
			sb.append(f.toString()+" , "+val+"\n");
		}
		return sb.toString();
	}

	/** get top ranked features*/
	public Feature[] getTopFeatures(int top){
		Collections.sort(featValues,VAL_COMPARATOR);
		int maxIndex=Math.min(featValues.size(),top);
		Feature[] ff=new Feature[maxIndex];
		for(int j=0;j<maxIndex;j++){
			ff[j]=featValues.get(j).feature;
		}
		return ff;
	}

	/** Number of features with the highest scores to keep in the dataset */
	public void setNumberOfFeatures(int number){
		this.TOP_FEATURES=number;
		isSorted=false;
	}

	/** Adds a score of feature f to the InstanceTransform */
	public void addFeature(double infoGain,Feature f){
		Pair p=new Pair(infoGain,f);
		featValues.add(p);
		isSorted=false;
	}

}
