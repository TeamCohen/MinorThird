/** HistoryManager.java
 * @author Sunita Sarawagi
 * @since 1.1
 * @version 1.3
 */
package iitb.CRF;

import iitb.Utils.Counters;

import java.io.Serializable;

class EdgeGenerator implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -4872905008657745029L;
	int offset;
    int numOrigY;
    int histsize;
    EdgeGenerator(int histsize, int numOrigY) {
	offset = 1;
	for (int i = 0; i < histsize-1; i++)
	    offset *= numOrigY;
	this.numOrigY = numOrigY;
	this.histsize = histsize;
    }
    int first(int destY) {
	return destY/numOrigY;
    }
    int next(int destY, int currentSrcY) {
	return currentSrcY + offset;
    }
    int firstY(int pos) {
	return 0;
    }
    int nextY(int currentY, int pos) {
	if ((pos >= histsize-1) || (currentY < numOrigY-1))
	    return currentY+1;
	if (currentY >= Math.pow(numOrigY,(pos+1)))
	    return numOrigY*offset;
	return currentY+1;
    }
};

class HistoryManager implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2916617303265831850L;
	int histsize;
    int numOrigY;
    int numY;
    HistoryManager(int histsize, int num) {
	this.histsize = histsize;
	numOrigY = num;
	this.numY = num;
	for (int i = 0; i < histsize-1; numY *= num, i++);
    };
    FeatureGenerator getFeatureGen(FeatureGenerator fgen) {
	if (histsize == 1)
	    return fgen;
	return new FeatureGeneratorWithHistory(fgen);
    };
    DataIter mapTrainData(DataIter trainData) {
	if (histsize == 1)
	    return trainData;
	return  new DataIterHistory(trainData);
    };
    void set_y(DataSequence data, int i, int label) {
	if (histsize > 1) data.set_y(i,label%numOrigY);
    }
    int getOrigY(int label){
        return label%numOrigY;
    }
    EdgeGenerator getEdgeGenerator() {
	return new EdgeGenerator(histsize,numOrigY);
    }
    class FeatureHist implements Feature {
	Feature orig;
	Counters ctr;
	FeatureHist() {}
	FeatureHist(int histsize, int numOrigY) {
	    ctr = new Counters(histsize+1, numOrigY);
	}
	void init(Feature f) {
	    orig = f;
	    ctr.clear();
	    ctr.fix(0,orig.y());
	    ctr.fix(histsize,0); 
	    if (orig.yprev() != -1) {
		ctr.fix(1,orig.yprev());
	    }
	    /*
	    if (pos == 0) {
		for (int i = 1; i < histsize; i++)
		    ctr.fix(i,0);
	    }
	    */
	    if (orig.yprevArray() != null) {
		for (int i = 0; i < orig.yprevArray().length; i++) {
		    if (orig.yprevArray()[i] != -1)
			ctr.fix(i+1, orig.yprevArray()[i]);
		}
	    }
	}
	boolean advance() {
	    return ctr.advance();
	}
	public int index() {return orig.index();}
	public int y() {return ctr.value(histsize-1,0);}
	public int yprev() {
	    if ((orig.yprevArray() == null) || (orig.yprevArray()[histsize-1] == -1))
		return -1;
	    return ctr.value(histsize,1);
	}
	public int[] yprevArray() {return null;}
	public float value() {return orig.value();}
	String type() {return "H ";}
	void print() {System.out.println(type() + index() + " " + y() + " " + yprev() + " " + value());}
    };
    class FeatureGeneratorWithHistory implements FeatureGenerator {
	/**
		 * 
		 */
		private static final long serialVersionUID = 8104535757570574219L;
	FeatureGenerator fgen;
	FeatureHist currentFeature, histFeature;
	boolean allDone;
	
	iitb.Model.FeatureImpl feature = new iitb.Model.FeatureImpl();
	FeatureGeneratorWithHistory(FeatureGenerator fgen) {
	    this.fgen = fgen;	    
	    histFeature = new FeatureHist(histsize,numOrigY);
	}
	public int numFeatures() {return fgen.numFeatures();} // + numY*numOrigY;}
	public void startScanFeaturesAt(DataSequence data, int pos) {
	    fgen.startScanFeaturesAt(data,pos);
	    allDone = false;
	    if (fgen.hasNext()) {
		currentFeature = histFeature;
		currentFeature.init(fgen.next());
	    } else
		allDone = true;
	}
	public boolean hasNext() {return !allDone;}
	public Feature next() {
	    feature.copy(currentFeature);
	    //currentFeature.print();
	    boolean nextY = currentFeature.advance();
	    if (!nextY) {
		if (fgen.hasNext())
		    currentFeature.init(fgen.next());
		//else if	(currentFeature != edgeFeatures) 
		    // currentFeature = edgeFeatures;
		else
		    allDone = true;
	    }
	    return feature;
	}
    /* (non-Javadoc)
     * @see iitb.CRF.FeatureGenerator#featureName(int)
     */
    public String featureName(int featureIndex) {
        return fgen.featureName(featureIndex);
    }
    };
    class DataSequenceHist implements DataSequence {
	/**
		 * 
		 */
		private static final long serialVersionUID = 1280727093574327309L;
	Counters cntr;
	transient DataSequence orig;
	DataSequenceHist() {cntr = new Counters(histsize,numOrigY);}
	void init(DataSequence orig) {
	    this.orig = orig;
	}
	public int length() {return orig.length();}
	public int y(int i) {
	    cntr.clear();
	    for(int k = histsize-1; k >= 0; k--)
		if (i-k >= 0) 
		    cntr.fix(k,orig.y(i-k));
		else
		    cntr.fix(k,0);
	    return cntr.value();
	}
	public Object x(int i) {return orig.x(i);}
	public void set_y(int i, int label) {}
    };
    class DataIterHistory implements DataIter {
	DataIter orig;
	DataSequenceHist dataSeq;
	DataIterHistory(DataIter orig) {
	    this.orig = orig;
	    dataSeq = new DataSequenceHist();
	}
	public void startScan() {orig.startScan();}
	public boolean hasNext() {return orig.hasNext();}
	public DataSequence next() {dataSeq.init(orig.next()); return dataSeq;}
    };
};
