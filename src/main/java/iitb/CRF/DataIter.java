package iitb.CRF;
/**
 * The basic interface to be implemented by the user of this package for
 * providing training and test data to the learner.
 *
 * @author Sunita Sarawagi
 *
 */ 

public interface DataIter {
    void startScan();
    boolean hasNext();
    DataSequence next();
};
