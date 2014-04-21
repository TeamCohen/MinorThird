/*
 * Created on Nov 17, 2004
 *
 */
package iitb.CRF;

/**
 *
 */
public interface Segmentation {
	int numSegments(); 	//number of segments in the record
	int segmentLabel(int segmentNum); 	//label of each segment
	int segmentStart(int segmentNum);
	int segmentEnd(int segmentNum); 
	// get the segment-id that contains the given offset position
	int getSegmentId(int offset);
	void setSegment(int segmentStart, int segmentEnd, int label); 
};
