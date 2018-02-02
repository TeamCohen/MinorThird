/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import gnu.trove.TObjectDoubleHashMap;

/**
 * Replaces feature counts by a TFIDF version of counts.
 * 
 * @author William Cohen
 */

public class TFIDFTransformLearner implements InstanceTransformLearner,
		Serializable{

	static final long serialVersionUID=20080201L;

	private TObjectDoubleHashMap featureFreq;

	private double numDocuments;

	/** The schema's not used here... */
	@Override
	public void setSchema(ExampleSchema schema){
		;
	}

	@Override
	public InstanceTransform batchTrain(Dataset dataset){
		// figure out frequency of each feature
		numDocuments=dataset.size();
		featureFreq=new TObjectDoubleHashMap();
		for(Iterator<Example> i=dataset.iterator();i.hasNext();){
			Example e=i.next();
			for(Iterator<Feature> j=e.featureIterator();j.hasNext();){
				Feature f=j.next();
				double d=featureFreq.get(f);
				featureFreq.put(f,d+1);
			}
		}
		// build an InstanceTransform that removes low-frequency features
		return new TFIDFWeighter(numDocuments,featureFreq);
	}

	private class TFIDFWeighter extends AbstractInstanceTransform implements
			Serializable{

		static final long serialVersionUID=20080201L;

		private double numDocuments;

		private TObjectDoubleHashMap featureFreq;

		public TFIDFWeighter(double numDocuments,TObjectDoubleHashMap featureFreq){
			this.numDocuments=numDocuments;
			this.featureFreq=featureFreq;
		}

		@Override
		public Instance transform(Instance instance){
			double norm=0.0;
			for(Iterator<Feature> i=instance.featureIterator();i.hasNext();){
				Feature g=i.next();
				double unnormalized=unnormalizedTFIDFWeight(g,instance);
				norm+=unnormalized*unnormalized;
			}
			norm=Math.sqrt(norm);
			MutableInstance result=
					new MutableInstance(instance.getSource(),instance
							.getSubpopulationId());
			for(Iterator<Feature> i=instance.featureIterator();i.hasNext();){
				Feature f=i.next();
				double w=unnormalizedTFIDFWeight(f,instance);
				result.addNumeric(f,w/norm);
			}
			return result;
		}

		private double unnormalizedTFIDFWeight(Feature f,Instance instance){
			double df=featureFreq.get(f);
			if(df==0)
				df=1; // assume new words are important
			return Math.log(instance.getWeight(f)+1)*Math.log(numDocuments/df);
		}

		@Override
		public String toString(){
			return "[TFIDFWeighter]";
		}
	}
}
