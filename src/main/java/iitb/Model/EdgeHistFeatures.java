package iitb.Model;
import iitb.CRF.DataSequence;
import iitb.Utils.Counters;
/**
 *
 * @author Sunita Sarawagi
 * @since 1.1
 * @version 1.3
 *
 * Suppose if history size is H and number of labels in the model
 * graph is m, this will generate m^(H+1) features 
 */ 

public class EdgeHistFeatures extends FeatureTypes {
    /**
	 * 
	 */
	private static final long serialVersionUID = 9015553082100153318L;
	String fname;
    String labelNames[];
    int histsize;
    Counters ctr;
    boolean allDone;
    int histArr[];
    public EdgeHistFeatures(FeatureGenImpl m,String name, String labels[], int histsize) {
        super(m);
        fname=name;
        labelNames=labels;
        ctr = new Counters(histsize+1, m.numStates());
        this.histsize = histsize;
        histArr = new int[histsize];
    }
    public boolean startScanFeaturesAt(DataSequence data, int prevPos, int pos) {
        ctr.clear();
        allDone = false;
        if (pos < histsize)
            allDone = true;
        return allDone;
    }
    public boolean hasNext() {
        return (histsize > 1) && !allDone;
    }	
    public void next(FeatureImpl f) {
        f.ystart = ctr.value(1,1);
        f.yend = ctr.value(0,0);
        ctr.arrayCopy(histsize-1,1,histArr);
        f.historyArray = histArr;
        f.val = 1;
        allDone = !ctr.advance();

        String name="";
        for (int i = 0; i < histArr.length; i++) {
            if (histArr[i] != -1) {
                if (labelNames == null) {
                    name += ctr.value(histsize-1,1);
                } else {
                    int index = i + 1;
                    name += fname+"."+index+"."+labelNames[model.label(f.ystart)];
                }
            }
        }
        setFeatureIdentifier(ctr.value(histsize-1,0), model.label(f.yend),name,f);
    }
};
