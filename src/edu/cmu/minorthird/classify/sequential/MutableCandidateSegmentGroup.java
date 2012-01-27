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

public class MutableCandidateSegmentGroup implements CandidateSegmentGroup
{
	private int maxWindowSize,sequenceLength;
	// the segment from start to start+L is window[start][L-1].
	private Instance[][] window;
	// parallel to window
	private ClassLabel[][] label;
	private int totalSize;
	private String subPopId = null;

	/** Creates a new holder for sliding-window instances.
	 * @param maxWindowSize the maximum length of any sliding window
	 * @param sequenceLength the length of the original sequence
	 */
	public MutableCandidateSegmentGroup(int maxWindowSize,int sequenceLength) 
	{ 
		this.sequenceLength = sequenceLength;
		this.maxWindowSize = maxWindowSize; 
		window = new Instance[sequenceLength][maxWindowSize];
		label = new ClassLabel[sequenceLength][maxWindowSize];
		totalSize = 0;
	}

	/** Specify the Instance associated with positions start...end, and associate
	 * the label with that Instance.  
	 * @param start starting position of segment in the original
	 * sequence from which newInstance was derived.
	 * @param end ending position, using Java conventions--e.g., start=2 and end=3 is a
	 * segment containing one element, which had index 2 in the original sequence.
	 */
	public void setSubsequence(int start,int end,Instance newInstance,ClassLabel newLabel) 
	{
		setSubPopId( newInstance.getSubpopulationId() );
		if (window[start][end-start-1]==null) totalSize++;
		window[start][end-start-1] = newInstance;
		label[start][end-start-1] = newLabel;
	}

	/** Specify the Instance associated with positions start...end.
	 */
	public void setSubsequence(int start,int end,Instance newInstance)
	{
		setSubPopId( newInstance.getSubpopulationId() );
		window[start][end-start-1] = newInstance;
	}

	// helper to check for different subPopId's in instances added to group...
	private void setSubPopId(String newSubpopId)
	{
		if (subPopId!=null && !subPopId.equals(newSubpopId)) {
			throw new IllegalArgumentException("grouping instances with different subPopId?");
		}
		subPopId = newSubpopId;
	}

	//
	// implement the rest of the interface...
	//

	@Override
	public Example getSubsequenceExample(int start,int end)
	{
		if (window[start][end-start-1]!=null) 
			return new Example(window[start][end-start-1], label[start][end-start-1]);
		else 
			return null;
	}

	/** Return the class label associated with getSubsequenceExample(start,end).
	 */
	@Override
	public ClassLabel getSubsequenceLabel(int start,int end) { return label[start][end-start-1]; }

	/** Return the instance corresponding to the segment from positions start...end.
	 */
	@Override
	public Instance getSubsequenceInstance(int start,int end) { return window[start][end-start-1]; }

	@Override
	public int getSequenceLength() { return sequenceLength; }

	@Override
	public int getMaxWindowSize() { return maxWindowSize; }

	@Override
	public String getSubpopulationId() { return subPopId; }

	@Override
	public int size() {	return totalSize; }

	@Override
	public Set<String> classNameSet()
	{
		Set<String> result = new HashSet<String>();
		for (int i=0; i<label.length; i++) {
			for (int j=0; j<label[i].length; j++) {
				if (label[i][j]!=null) result.addAll( label[i][j].possibleLabels() );
			}
		}
		return result;
	}

	//
	// debug output
	//

	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer(""); 
		for (int lo=0; lo<window.length; lo++) {
			for (int len=1; len<=maxWindowSize; len++) {
				buf.append( lo+".."+(lo+len)+": ");
				if (window[lo][len-1]==null) {
					buf.append("NULL");
				} else {
					buf.append(window[lo][len-1].toString());
					if (label[lo][len-1]!=null) buf.append(";"+label[lo][len-1]);
				}
				buf.append("\n");
			}
		}
		return buf.toString();
	}

}

