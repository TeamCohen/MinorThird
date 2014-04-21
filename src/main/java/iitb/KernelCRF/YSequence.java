/*
 * Created on Jun 28, 2008
 * @author sunita
 */
package iitb.KernelCRF;

import iitb.CRF.Soln;

import java.io.Serializable;
import java.util.BitSet;

/**
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
public class YSequence implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -108787926046669135L;
	int numBits;
    BitSet yseq = new BitSet();
    public YSequence(int labeling[], int numLabels) {
        numBits = iitb.Utils.Utils.log2Ceil(2*numLabels);//1 bit for segment marker.
        for (int pos = 0; pos < labeling.length; pos++) {
            int y = labeling[pos];
            for (int bitPos = pos*numBits; y > 0; y = y >> 1, bitPos++) {
                if ((y & 1) > 0) 
                    yseq.set(bitPos);
            }
            yseq.set((pos+1)*numBits-1);
        }
    }
    public YSequence(Soln soln, int numLabels) {
        numBits = iitb.Utils.Utils.log2Ceil(2*numLabels);//1 bit for segment marker.

        for (; soln != null; soln = soln.prevSoln) {
            for(int pos = soln.pos;pos > soln.prevPos(); pos--) {
                int y = soln.label;
                for (int bitPos = pos*numBits; y > 0; y = y >> 1, bitPos++) {
                    if ((y & 1) > 0) 
                        yseq.set(bitPos);
                }
                //assert(getY(pos)==soln.label);
                //if (soln.pos-soln.prevPos()>1) {
                yseq.set((soln.prevPos()+2)*numBits-1);
                //}
            }
        }

        
    }
    @Override
    public int hashCode() {
        return yseq.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return (obj == yseq) || 
        ((obj != null) && yseq.equals(((YSequence)obj).yseq));
    }
    public int getY(int j) {
        int y = 0;
        int bn=0;
        // -1 because segmentStartMarker on one end.
        for (int p = numBits*j; bn < numBits-1; p++, bn++) {
            y += yseq.get(p)?(1 << bn):0;
        }
        return y;
    }
    public int[] getYArray(int n) {
    	int ys[] = new int[n];
    	for (int i = 0; i < ys.length; i++) {
			ys[i] = getY(i);
		}
    	return ys;
    }
    public boolean segStart(int j) {
        return yseq.get(j*numBits+numBits-1);
    }
    @Override
    public String toString() {
        String str="";
        int dataLen = (yseq.length()+numBits-1)/numBits;
        for (int i = 0; i < dataLen; i++) {
            str += (i + ":" + getY(i)+" "); //+ "?" + segStart(i)+ " ");
        }
        return str;
    }
}