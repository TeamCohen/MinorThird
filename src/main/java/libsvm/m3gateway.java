// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   m3gateway.java

package libsvm;


// Referenced classes of package libsvm:
//            svm_model, svm_node

public class m3gateway
{

    public m3gateway(svm_model svm_model1)
    {
        m_model = svm_model1;
    }

    public double[][] getCoefficientsForSVsInDecisionFunctions()
    {
        return m_model.sv_coef;
    }

    public double[] getConstantsInDecisionFunctions()
    {
        return m_model.rho;
    }

    public int[] getLabelForEachClass()
    {
        return m_model.label;
    }

    public int[] getNumSVsForEachClass()
    {
        return m_model.nSV;
    }

    public int getNumberOfClasses()
    {
        return m_model.nr_class;
    }

    public int getNumberOfSupportVectors()
    {
        return m_model.l;
    }

    public double[] getProbAInfo()
    {
        return m_model.probA;
    }

    public double[] getProbBInfo()
    {
        return m_model.probB;
    }

    public svm_node getSVMnode(int i, int j)
    {
        return m_model.SV[i][j];
    }

    public svm_node[][] getSVMnodes()
    {
        return m_model.SV;
    }

    public svm_node[] getSVMnodes(int i)
    {
        return m_model.SV[i];
    }

    private svm_model m_model;
}
