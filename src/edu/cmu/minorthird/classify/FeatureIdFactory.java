package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.util.AbstractLooper;
import gnu.trove.THashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.Serializable;
import java.util.*;

public class FeatureIdFactory implements java.io.Serializable
{
    private TObjectIntHashMap featureIds = new TObjectIntHashMap();

		public FeatureIdFactory (Dataset dataset)
		{
			int nextID = 1;
        for (Example.Looper i = dataset.iterator(); i.hasNext();)
        {
            Example e = i.nextExample();
            for (Feature.Looper j = e.featureIterator(); j.hasNext();)
            {
                Feature f = j.nextFeature();
								if (!featureIds.containsKey(f))
									featureIds.put(f,  nextID++);
            }
				}
		}

    public int getID(Feature feature)
    {
      int id = featureIds.get(feature);
      if (id < 0)
				return featureIds.size()+1;
      return id;
    }
}
