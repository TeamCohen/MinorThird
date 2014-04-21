/** KernelCRF.java
 * Created on Jun 28, 2008
 * 
 * @author Sunita Sarawagi
 * @since 1.3
 * @version 1.3
 */
package iitb.KernelCRF;

import iitb.CRF.CRF;
import iitb.CRF.DataIter;
import iitb.CRF.DataSequence;
import iitb.CRF.Evaluator;
import iitb.CRF.FeatureGenerator;
import iitb.CRF.Viterbi;

import java.io.Serializable;
import java.util.Properties;
import java.util.Vector;

public class KernelCRF extends CRF {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5041774715605661933L;
	public static class SupportVector implements Serializable {
        /**
		 * 
		 */
		private static final long serialVersionUID = -5611055066177234585L;
		DataSequence dataSeq;
        YSequence yseq;
        double alpha;
        public SupportVector(DataSequence dataSeq, YSequence yseq, double alpha) {
            this.dataSeq = dataSeq;
            this.yseq = yseq;
            this.alpha = alpha;
        }
    }
    Vector<SupportVector> svecs;
    SequenceKernel kernel;
    public KernelCRF(int numLabels, FeatureGenerator fgen, Properties configOptions) {
        super(numLabels, fgen, configOptions);
    }
    public KernelCRF(int numLabels, FeatureGenerator fgen, String arg) {
        super(numLabels, fgen, arg);
    }
    @Override
    public double[] train(DataIter trainData, Evaluator evaluator, float instanceWts[]) {
        return train(trainData,evaluator,instanceWts,null);
    }
    @Override
    public double[] train(DataIter trainData, Evaluator evaluator, float instanceWts[], float misClassCosts[][]) {
        KernelTrainer trainer = (KernelTrainer) getTrainer();
        trainer.train(this, trainData, null, evaluator, instanceWts, misClassCosts);
        svecs = trainer.getSupportVectors();
        kernel=trainer.getKernel();
        return new double[getFeatureGenerator().numFeatures()]; // this is just to not break code downstream.
    }
    @Override
    public Viterbi getViterbi(int beamsize) {
        return new KernelViterbi(this,beamsize);
    }
}
