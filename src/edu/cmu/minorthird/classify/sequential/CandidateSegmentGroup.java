/* Copyright 2003-2004, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import java.util.*;

/**
 * A group of sliding windows associated with a sequence.
 *
 * @author William Cohen
 */

public class CandidateSegmentGroup
{
	private int maxWindowSize,sequenceLength;
	private Instance[][] window;
	private ClassLabel[][] label;
	private Set classNameSet;
	private int totalSize;

	public CandidateSegmentGroup(int maxWindowSize,int sequenceLength) 
	{ 
		this.sequenceLength = sequenceLength;
		this.maxWindowSize = maxWindowSize; 
		window = new Instance[sequenceLength][maxWindowSize];
		label = new ClassLabel[sequenceLength][maxWindowSize];
		totalSize = 0;
	}
	public void setSubsequence(int start,int end,Instance newInstance,ClassLabel newLabel) 
	{
		if (window[start][end-start-1]==null) totalSize++;
		window[start][end-start-1] = newInstance;
		label[start][end-start-1] = newLabel;
	}
	public void setSubsequence(int start,int end,Instance newInstance)
	{
		window[start][end-start-1] = newInstance;
	}
	public int getSequenceLength() 
	{ 
		return sequenceLength; 
	}
	public int getMaxWindowSize()
	{
		return maxWindowSize;
	}
	public int size()
	{
		return totalSize;
	}
	public Example getSubsequenceExample(int start,int end)
	{
		if (window[start][end-start-1]!=null) 
			return new Example(window[start][end-start-1], label[start][end-start-1]);
		else 
			return null;
	}
	public ClassLabel getSubsequenceLabel(int start,int end)
	{
		return label[start][end-start-1];
	}
	public Instance getSubsequenceInstance(int start,int end)
	{
		return window[start][end-start-1];
	}
	public Set classNameSet()
	{
		Set result = new HashSet();
		for (int i=0; i<label.length; i++) {
			for (int j=0; j<label[i].length; j++) {
				if (label[i][j]!=null) result.addAll( label[i][j].possibleLabels() );
			}
		}
		return result;
	}

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

