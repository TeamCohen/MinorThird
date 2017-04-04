/** KernelViterbi.java
 * Created on Jun 28, 2008
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
package iitb.KernelCRF;

import iitb.CRF.DataSequence;
import iitb.CRF.Viterbi;
import iitb.KernelCRF.KernelCRF.SupportVector;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

public class KernelViterbi extends Viterbi {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5747241353594394559L;
	KernelCRF model;
    public KernelViterbi(KernelCRF model, int bs) {
        super(model, bs);
        this.model = model;
    }
    @Override
    protected void computeLogMi(DataSequence dataSeq, int i, int ell, double[] lambda) {
        Ri.assign(0); Mi.assign(0);
        for (SupportVector sv : model.svecs) {
            computeLogMi(dataSeq,i,Mi,Ri,sv.dataSeq,sv.yseq,sv.alpha);
        }
    }
    protected double kernelValue(DataSequence d1, int p1, DataSequence d2, int p2) {
        return model.kernel.kernel(d1, p1, d2, p2);
    }
    protected void computeLogMi(DataSequence dataSeq, int i, DoubleMatrix2D kMi, DoubleMatrix1D kRi, DataSequence consData, YSequence yseq, double alpha) {
        if (consData==null) return;
        for (int j = 0; j < consData.length(); j++) {
            int consY = yseq.getY(j);
            int ypp = consData.y(j);
            if (consY != ypp) {
                double kval = kernelValue(dataSeq, i, consData, j);
                kRi.set(consY, kRi.get(consY)-alpha*kval);
                kRi.set(ypp, kRi.get(ypp)+alpha*kval);
            }
            if ((i > 0) && (j > 0)) {
                int consPrevY = yseq.getY(j-1);
                int yprev = consData.y(j-1);
                if ((consY != consData.y(j)) || (consPrevY != consData.y(j-1))) {
                    kMi.set(consPrevY, consY, kMi.get(consPrevY, consY)-alpha);
                    kMi.set(yprev, ypp, kMi.get(yprev, ypp)+alpha);
                }
            }
        }
    }
}
