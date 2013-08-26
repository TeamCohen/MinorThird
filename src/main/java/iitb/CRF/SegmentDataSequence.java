package iitb.CRF;
/**
 * The training/test instance for segment sequence data as needed by NestedCRF.
 * @author Sunita Sarawagi
 *
 */ 


public interface SegmentDataSequence extends DataSequence {
    /** get the end position of the segment starting at segmentStart */
    int getSegmentEnd(int segmentStart); 
    /** set segment boundary and label */
    void setSegment(int segmentStart, int segmentEnd, int y);
};
