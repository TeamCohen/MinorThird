/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * A SequenceDataset that additionally includes examples for 'sliding
 * windows' over the original data.  
 * 
 * <p>In particular, a plain old SequenceDataset contains arrays of
 * Examples, where typically an Example array is derived from a
 * parallel array of objects src[], and for an example array seq[],
 * seq[i] is an example derived from a corresponding object src[i].
 * In a SlidingWindowDataset there is are also examples derived from
 * subsequences src[j,j+1,...,i] for all i-L<=j<=i.
 *
 * <p>Setting up a sliding window dataset requires a series of calls.
 * For each sequence of objects, first add the sequence of examples
 * with addSequence(); then add all the non-unit-size sliding windows
 * with addSubsequenceExample(); then call finishSequence().
 *
 * <p>To access the stuff inside the sliding window dataset,
 * there is a special iterator, a SlidingWindowDataset.Looper.
 * This allows you to access the sequences, plus the window
 * features.
 *
 * @author William Cohen
 */

public class SlidingWindowDataset extends SequenceDataset
{
	private int maxWindowSize = 3;
	private ArrayList windowList = new ArrayList();
	private Example[][] currentWindows = null;

	public SlidingWindowDataset(int maxWindowSize) { this.maxWindowSize = maxWindowSize; }
	public SlidingWindowDataset() { this(3); }

	public int getMaxWindowSize() { return maxWindowSize; }

	/** Add a new sequence of examples to the dataset. */
	public void addSequence(Example[] sequence)
	{
		super.addSequence(sequence);
		// will hold sliding-window examples: currentWindows[i][len-1] holds
		// the example from i-len to i
		currentWindows = new Example[sequence.length][maxWindowSize-1];
	}

	/** Add an example corresponding to the subsequence
	 * between object[startPosition] and object[endPosition]
	 */
	public void addSubsequenceExample(int startPosition,int endPosition,Example example)
	{
		if (endPosition-startPosition>maxWindowSize) 
			throw new IllegalArgumentException("this SlidingWindowDataset only holds subsequences of size <="+maxWindowSize);
		if (endPosition-startPosition<2) 
			throw new IllegalArgumentException("subsequences must have length >=2"); 
		currentWindows[startPosition][endPosition-startPosition-2] = factory.compress( example );
	}

	/** Complete addition of subsequence examples for this most recently-added sequence.
	 */
	public void finishSequence()
	{
		windowList.add(currentWindows);
		if (windowList.size() != sequenceList.size()) {
			throw new IllegalStateException(
				"<#sequences: "+sequenceList.size()+"> != <#calls to finishSequence: "+windowList.size()+">");
		}
	}

	public SlidingWindowDataset.Looper subsequenceIterator()
	{
		return new Looper();
	}

	public class Looper implements Iterator 
	{
		private int i=0; 
		public void remove() { throw new UnsupportedOperationException("can't remove!"); }
		public boolean hasNext() { return i<sequenceList.size(); }
		public Object next() { return sequenceList.get(i++); }
		public Example[] nextSequence() { return (Example[])next(); }
		public Example getSubsequenceExample(int lo,int hi)	{ 
			if (hi-lo>maxWindowSize || hi-lo<2) throw new IllegalArgumentException("subsequences must have length > 2"); 
			Example[][] windows = (Example[][])windowList.get(i-1);
			return windows[lo][hi-lo-2];
		}
	}


	public String toString()
	{	
		StringBuffer buf = new StringBuffer("");
		buf.append(super.toString());
		buf.append("\n");
		for (SlidingWindowDataset.Looper i=subsequenceIterator(); i.hasNext(); ) {
			Example[] seq = i.nextSequence();
			for (int lo=0; lo<seq.length; lo++) {
				for (int len=2; len<=maxWindowSize; len++) {
					if (lo+len<seq.length) {
						buf.append("sequence["+lo+":"+(lo+len)+"]: ");
						buf.append(i.getSubsequenceExample(lo,lo+len).toString());
						buf.append("\n");
					}
				}
			}
		}
		return buf.toString();
	}

	public static void main(String[] args) throws IOException
	{
		SequenceDataset d = SampleDatasets.makeToySequenceData();
		//new ViewerFrame("Sequence data",d.toGUI());

		SlidingWindowDataset swd = new SlidingWindowDataset(4);

		for (Iterator i=d.sequenceIterator(); i.hasNext(); ) {
			Example[] seq = (Example[])i.next();
			swd.addSequence(seq);
			for (int lo=0; lo<seq.length; lo++) {
				for (int len=2; len<=4; len++) {
					if (lo+len<seq.length) {
						MutableInstance inst = new MutableInstance();
						for (int j=lo; j<lo+len; j++) {
							for (Feature.Looper k=seq[j].featureIterator(); k.hasNext(); ) {
								Feature f = k.nextFeature();
								inst.addBinary(f);
							}
						}
						//System.out.println("lo="+lo+" len="+len);
						swd.addSubsequenceExample(lo,lo+len,new Example(inst,new ClassLabel(ExampleSchema.POS_CLASS_NAME)));
					}
				}
			}
			swd.finishSequence();
		}
		System.out.println(swd.toString());
	}
}
