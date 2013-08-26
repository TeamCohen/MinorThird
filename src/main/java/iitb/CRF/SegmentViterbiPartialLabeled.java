/** SegmentViterbiPartialLabeled.java
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
package iitb.CRF;

import gnu.trove.set.hash.TIntHashSet;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

public class SegmentViterbiPartialLabeled extends SegmentViterbi {

	public SegmentViterbiPartialLabeled(SegmentCRF nestedModel, int bs) {
		super(nestedModel, bs);
	}

	public SegmentViterbiPartialLabeled(CRF model, int bs) {
		super(model, bs);
	}
	@Override
	protected void computeLogMi(DataSequence dataSeq, int i, int ell, double[] lambda) {
		super.computeLogMi(dataSeq, i, ell, lambda);
		if (dataSeq.y(i) >= 0) {
			for (int j = 0; j < Ri.size(); j++) {
				if (j != dataSeq.y(i))
					Ri.set(j, RobustMath.LOG0);
			}
			assert(Ri.get(dataSeq.y(i)) > RobustMath.LOG0);
		}
		if (usedLabels != null && labelConstraints != null && 
				usedLabels.length >= dataSeq.length() && usedLabels[i].size() > 0) {
			for (int y = 0; y < Ri.size(); y++) {
				if (!labelConstraints.valid(usedLabels[i], y, -1)) {
					Ri.set(y, RobustMath.LOG0);
				}
			}
		}
	}
	TIntHashSet usedLabels[];
	public double viterbiSearch(DataSequence dataSeq, double lambda[], 
            DoubleMatrix2D[][] Mis, DoubleMatrix1D[][] Ris, 
            boolean constraints, boolean calCorrectScore) {
        if(constraints)
            labelConstraints = LabelConstraints.checkConstraints((CandSegDataSequence)dataSeq, labelConstraints);
        else
            labelConstraints = null;
        if (labelConstraints != null) {
        	if (usedLabels==null || usedLabels.length < dataSeq.length()) {
        		usedLabels = new TIntHashSet[dataSeq.length()];
        		for (int i = 0; i < usedLabels.length; i++) {
					usedLabels[i] = new TIntHashSet();
				}
        	} else {
        		for (int i = 0; i < dataSeq.length(); i++) {
					usedLabels[i].clear();
				}
        	}
        	
        	for (int i = dataSeq.length()-1; i >= 0; i--) {
				if (dataSeq.y(i) < 0) continue;
				if (!labelConstraints.conflicting(dataSeq.y(i))) continue;
				for (int j = i-1; j >= 0; j--) {
					usedLabels[j].add(dataSeq.y(i));
				}
			}
        }
        return super.viterbiSearch(dataSeq, lambda, Mis, Ris, calCorrectScore);
    }
}
