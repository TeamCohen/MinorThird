/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.transform;

import edu.cmu.minorthird.classify.*;

import java.util.Set;
import java.util.HashSet;

/**
 * @author Edoardo Airoldi
 * Date: Dec 20, 2004
 */

public class MakeBinaryTransform implements InstanceTransformLearner
{
   /** Default constructor */
   public MakeBinaryTransform() {;}

   /** The schema's not used here... */
   public void setSchema(ExampleSchema schema) {;}

   public InstanceTransform batchTrain(Dataset dataset)
   {
      BasicFeatureIndex fidx = new BasicFeatureIndex(dataset);

      final Set activeFeatureSet = new HashSet();
      for (Feature.Looper i=fidx.featureIterator(); i.hasNext();)
      {
         activeFeatureSet.add( i.nextFeature() );
      }

      // build an InstanceTransform that transforms counts into 0/1
      return new AbstractInstanceTransform() {
         public Instance transform(Instance instance) {
            Instance i = new MutableInstance();
            for (Feature.Looper j=instance.featureIterator();j.hasNext();)
            {
               Feature ft = j.nextFeature();
               double wgt = instance.getWeight(ft);
               if (wgt>0) { wgt=1.0; } else { wgt=0.0; }
               ((MutableInstance)i).addNumeric(ft,wgt);
            }
            return new MaskedInstance(i, activeFeatureSet);
         }
         public String toString() {
            return "[InstanceTransform: from counts to 0/1]";
         }
      };
   }

}
