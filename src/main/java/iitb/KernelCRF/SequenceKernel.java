/*
 * Created on Jun 27, 2008
 * @author sunita
 */
package iitb.KernelCRF;

import java.io.Serializable;

import iitb.CRF.DataSequence;


public interface SequenceKernel extends Serializable {
    public double kernel(DataSequence d1, int p1, DataSequence d2, int p2);
}
