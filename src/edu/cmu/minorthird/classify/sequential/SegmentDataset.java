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
 * @author William Cohen
 */

public class SegmentDataset implements Dataset
{
	int maxWindowSize = -1;
	private ArrayList groupList = new ArrayList();
	private Set classNameSet = new HashSet();
	private int totalSize = 0;
  private FeatureFactory factory = new FeatureFactory();
  private boolean compressGroups = false;

	public SegmentDataset() {;}

  public void setDataCompression(boolean flag) { compressGroups=flag; }

	public int getMaxWindowSize() { return maxWindowSize; }
	public int size() { return totalSize; }

	public int getNumberOfSegmentGroups() { return groupList.size(); }

	/** Add a new sequence of examples to the dataset. */
	public void addCandidateSegmentGroup(CandidateSegmentGroup group)
	{
		if (maxWindowSize<0) maxWindowSize = group.getMaxWindowSize();
		if (maxWindowSize>=0 && group.getMaxWindowSize()!=maxWindowSize) {
			throw new IllegalArgumentException("mismatched window sizes: "+maxWindowSize+", "+group.getMaxWindowSize());
		}
    if (compressGroups) groupList.add(new CompactCandidateSegmentGroup(factory,group));
    else groupList.add(group);
		classNameSet.addAll( group.classNameSet() );
		totalSize += group.size();
	}

	public ExampleSchema getSchema()
	{
		ExampleSchema schema = new ExampleSchema((String[])classNameSet.toArray(new String[classNameSet.size()]));
		if (schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA)) return ExampleSchema.BINARY_EXAMPLE_SCHEMA;
		else return schema;
	}

	/** Add a new example to the dataset. */
	public void add(Example example)
	{
		MutableCandidateSegmentGroup g = new MutableCandidateSegmentGroup(1,1);
		g.setSubsequence(0,1,example.asInstance(),example.getLabel());
		addCandidateSegmentGroup(g);
	}

	/** Iterate over all examples */
	public Example.Looper iterator()
	{
		ArrayList result = new ArrayList();
		for (Iterator i=groupList.iterator(); i.hasNext(); ) {
			CandidateSegmentGroup g = (CandidateSegmentGroup)i.next();
			for (int j=0; j<g.getSequenceLength(); j++) {
				for (int k=1; k<=g.getMaxWindowSize(); k++) {
					Example e = g.getSubsequenceExample(j,j+k);
					if (e!=null) result.add(e);
				}
			}
		}
		return new Example.Looper(result);
	}


	public SegmentDataset.Looper candidateSegmentGroupIterator()
	{
		return new Looper();
	}

	public class Looper implements Iterator 
	{
		private Iterator i = groupList.iterator();
		public void remove() { throw new UnsupportedOperationException("can't remove!"); }
		public boolean hasNext() { return i.hasNext(); }
		public Object next() { return i.next(); }
		public CandidateSegmentGroup nextCandidateSegmentGroup() { return (CandidateSegmentGroup)next(); }
	}


	public String toString()
	{	
		StringBuffer buf = new StringBuffer("");
		buf.append("size = "+size()+"\n");
		for (Iterator i=groupList.iterator(); i.hasNext(); ) {
			buf.append( i.next() + "\n" );
		}
		return buf.toString();
	}

	/** Randomly re-order the examples. */
	public void shuffle(Random r)
	{
		Collections.shuffle(groupList,r);
	}
	
  /** Randomly re-order the examples. */
	public void shuffle()
	{
		Collections.shuffle(groupList,new Random(0));
	}

	/** Make a shallow copy of the dataset. */
	public Dataset shallowCopy()
	{
		SegmentDataset copy = new SegmentDataset();
		for (Iterator i=groupList.iterator(); i.hasNext(); ) {
			copy.addCandidateSegmentGroup( (CandidateSegmentGroup) i.next() );
		}
		return copy;
	}

	//
	// split
	//

	public Split split(final Splitter splitter)
	{
		splitter.split(groupList.iterator());
		return new Split() {
				public int getNumPartitions() { return splitter.getNumPartitions(); }
				public Dataset getTrain(int k) { return invertIteration(splitter.getTrain(k)); }
				public Dataset getTest(int k) { return invertIteration(splitter.getTest(k)); }
			};
	}
	protected Dataset invertIteration(Iterator i) 
	{
		SegmentDataset copy = new SegmentDataset();
		while (i.hasNext()) {
			Object o = i.next();
			copy.addCandidateSegmentGroup((CandidateSegmentGroup)o);
		}
		return copy;
	}

	/** A GUI view of the dataset. */
	public Viewer toGUI()
	{
		//return new VanillaViewer(this);
		Viewer dbGui = new BasicDataset.SimpleDatasetViewer();
		dbGui.setContent(this);
		Viewer instGui = GUI.newSourcedExampleViewer();
		return new ZoomedViewer(dbGui,instGui);
	}
}

