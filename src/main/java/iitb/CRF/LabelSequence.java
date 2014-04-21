/*
 * Created on Jul 10, 2005
 *
 */
package iitb.CRF;

/**
 * @author sunita
 *
 */
public class LabelSequence {
    double score;
    public int[] labels;
    public LabelSequence() {}
    /**
     * @param length
     */
    LabelSequence(int length) {
        labels = new int[length];
    }
    public double score() {return score;}
    public void apply(DataSequence data) {
        for (int i = 0; i < labels.length; i++)
            data.set_y(i,labels[i]);
    }
    /**
     * @param prevPos
     * @param pos
     * @param label
     */
    public void add(int prevPos, int pos, int label) {
        labels[pos]=label;
    }
    public void doneAdd() {};
}
