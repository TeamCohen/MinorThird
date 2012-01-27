/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import java.util.*;

/**
 * A group of sliding windows associated with a sequence.
 *
 * <p>In more detail: just as a SequenceDataset holds a set of
 * Example[] objects, a SegmentDataset holds a set of
 * CandidateSegmentGroup objects.  Each CandidateSegmentGroup is
 * derived from a sequence of source objects s1....sN.  The
 * CandidateSegmentGroup holds an instance for each subsequence of up
 * to K adjacent source objects (the subsequence is called a
 * <i>segment</i>, or a <i>sliding window</i>). Here K is the "maxWindowSize", and N
 * is the "sequenceLength".
 *
 *<p>To illustrate, suppose the original sequence is a,b,c,d,e and K=3.
 * Then the sliding window will hold instances created from these
 * subsequences: a,b,c,d,e,ab,bc,cd,de,abc,bcd,cde.
 *
 * @author William Cohen
 */

public interface CandidateSegmentGroup extends HasSubpopulationId
{
  /** Return the length of the original sequence that the segments
   * were derived from.
   */
	public int getSequenceLength(); 

  /** Return the maximum segment length.
   */
	public int getMaxWindowSize();

  /** Number of instances stored.
   */
	public int size();

  /** Return the example corresponding to the segment from positions start..end
   */
	public Example getSubsequenceExample(int start,int end);

  /** Return the class label associated with getSubsequenceExample(start,end).
   */
	public ClassLabel getSubsequenceLabel(int start,int end);

  /** Return the instance corresponding to the segment from positions start...end.
   */
	public Instance getSubsequenceInstance(int start,int end);

  /** Return the set of strings associated with ClassLabels on any of the stored segments.
   */
	public Set<String> classNameSet();

  /** Return the subpopulationId for the original sequence.
   */
	@Override
	public String getSubpopulationId();
}

