/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.minorthird.classify.BasicFeatureIndex;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;

/**
 * @author Edoardo Airoldi
 * Date: Dec 20, 2004
 */

public class MakeBinaryTransform implements InstanceTransformLearner
{
   /** Default constructor */
   public MakeBinaryTransform() {;}

   /** The schema's not used here... */
   @Override
	public void setSchema(ExampleSchema schema) {;}

   @Override
	public InstanceTransform batchTrain(Dataset dataset)
   {
      BasicFeatureIndex fidx = new BasicFeatureIndex(dataset);

      final Set<Feature> activeFeatureSet = new HashSet<Feature>();
      for (Iterator<Feature> i=fidx.featureIterator(); i.hasNext();)
      {
         activeFeatureSet.add( i.next() );
      }

      // build an InstanceTransform that transforms counts into 0/1
      return new AbstractInstanceTransform() {
         @Override
				public Instance transform(Instance instance) {
            Instance i = new MutableInstance();
            for (Iterator<Feature> j=instance.featureIterator();j.hasNext();)
            {
               Feature ft = j.next();
               double wgt = instance.getWeight(ft);
               if (wgt>0) { wgt=1.0; } else { wgt=0.0; }
               ((MutableInstance)i).addNumeric(ft,wgt);
            }
            return new MaskedInstance(i, activeFeatureSet);
         }
         @Override
				public String toString() {
            return "[InstanceTransform: from counts to 0/1]";
         }
      };
   }

}
