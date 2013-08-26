package iitb.Segment;
import iitb.CRF.*;
/**
 *
 * @author Sunita Sarawagi
 *
 */ 

public interface TrainData extends DataIter {
    int size();   // number of training records
    void startScan(); // start scanning the training data
    boolean hasMoreRecords(); 
    public TrainRecord nextRecord();
    boolean hasNext(); 
    public DataSequence next();
};

