/*
 * Created on Nov 18, 2004
 *
 */
package iitb.CRF;

/**
 * @author sunita
 *
 */
public interface CandidateSegments {
	/**
	 * The number of candidate segments ending at given position
	 */
	int numCandSegmentsEndingAt(int endPos);
	/**
	 * The start position of the segNum-th segment ending at "endPos" 
	 */
	int candSegmentStart(int endPos, int segNum);
}
