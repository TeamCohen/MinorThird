package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.algorithms.random.RandomElement;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Works with datasets of binary examples.  Splits POS and NEG examples into
 * k separate disjoints folds, separately, and then returns k train/test splits
 * where each train set is the union of k-1 folds, and the test set is the k-th
 * fold.  Does NOT preserve subpopulation information.
 *
 * @author Edoardo Airoldi
 * Date: Dec 8, 2003
 */

public class StratifiedCrossValSplitter implements Splitter {

    static private Logger log = Logger.getLogger(StratifiedCrossValSplitter.class);

    private RandomElement random;
    private int folds;
    private List strata;

    public StratifiedCrossValSplitter(RandomElement random, int folds) {
        this.random = random; this.folds = folds;
    }
    public StratifiedCrossValSplitter(int folds) {
        this(new RandomElement(), folds);
    }
    public StratifiedCrossValSplitter() {
        this(new RandomElement(), 5);
    }

    public void split(Iterator i) {
        strata = new ArrayList();
        for (Iterator j = new StrataSorter(random,i).strataIterator(); j.hasNext(); ) {
            strata.add( j.next() );
        }
    }

    public int getNumPartitions() { return folds; }

    public Iterator getTrain(int k) {
        List trainList = new ArrayList();
        for (int i=0; i<strata.size(); i++) {
            for (int j=0; j<((List)strata.get(i)).size(); j++) {
                if (j%folds != k) {
                    trainList.add( ((List)strata.get(i)).get(j) );
                }
            }
        }
        return trainList.iterator();
    }

    public Iterator getTest(int k) {
        List testList = new ArrayList();
        for (int i=0; i<strata.size(); i++) {
            for (int j=0; j<((List)strata.get(i)).size(); j++) {
                if (j%folds == k) {
                    testList.add( ((List)strata.get(i)).get(j) );
                }
            }
        }
        return testList.iterator();
    }
    public String toString() { return "["+folds+"-Stratified CV splitter]"; }

}
