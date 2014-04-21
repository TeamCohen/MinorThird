/*
 * Created on Jun 28, 2008
 * @author sunita
 */
package iitb.KernelCRF;

import java.util.Vector;


import iitb.CRF.CrfParams;
import iitb.CRF.Trainer;
import iitb.KernelCRF.KernelCRF.SupportVector;

public abstract class KernelTrainer extends Trainer {

    public KernelTrainer(CrfParams p) {
        super(p);
    }
    public abstract Vector<SupportVector> getSupportVectors();
    public abstract SequenceKernel getKernel();
}
