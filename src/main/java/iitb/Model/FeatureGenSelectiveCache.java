/** FeatureGenSelectiveCache.java
 * Created on Jul 19, 2005
 *
 * @author Sunita Sarawagi
 * @since 1.2
 * @version 1.3
 */
package iitb.Model;

import gnu.trove.map.hash.TIntIntHashMap;
import iitb.CRF.DataSequence;
import iitb.CRF.Feature;
import iitb.CRF.FeatureGenCache;

/**
 * @author sunita
 *
 */
public class FeatureGenSelectiveCache extends FeatureGenCache {
    FeatureGenImpl fgenImpl;
    /**
     * @param fgen
     */
    public FeatureGenSelectiveCache(FeatureGenImpl fgen, boolean edgeFeaturesXInd) {
        super(fgen,edgeFeaturesXInd);
        fgenImpl = fgen;
    }
    
	public boolean hasNext() {
		return super.hasNext()?true:fgenImpl.hasNext();
	}
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGenerator#next()
	 */
	public Feature next() {
		if (firstScan) {
			boolean needsCaching = fgenImpl.currentFeatureType.needsCaching();
			Feature f = fgenImpl.next();
			if (needsCaching)
			    cacheFeature(f);
			return f;
		} else {
		    if (super.hasNext())
		        return super.next();
			return fgenImpl.next();
		}
	}
    TIntIntHashMap idIndexMap = new TIntIntHashMap();
	int prevId=-1;
	/* (non-Javadoc)
	 * @see iitb.CRF.FeatureGeneratorNested#startScanFeaturesAt(iitb.CRF.DataSequence, int, int)
	 */
	protected void startScanFeaturesAt(DataSequence data, int prevPos, int pos, boolean nested) {
	    super.startScanFeaturesAt(data,prevPos,pos,nested);
	    if (firstScan) {
	        ;
	    } else {
			if (nested) 
				fgenImpl.startScanFeaturesAtOnlyNonCached(data,prevPos,pos);
			else 
				fgenImpl.startScanFeaturesAtOnlyNonCached(data,pos);
		}
	}	
}
