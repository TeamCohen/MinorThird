/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * A dataset of sequences of examples.
 *
 * @author William Cohen
 */

public class SequenceDataset implements Dataset,SequenceConstants,Visible,Saveable
{
	protected ArrayList sequenceList = new ArrayList(); 
	protected int totalSize = 0;
	private int historyLength = 1;
	private String[] history = new String[historyLength];
	protected Set classNameSet = new HashSet();
	protected FeatureFactory factory = new FeatureFactory();

	/** Set the current history length.
	 * Examples produced by the iterator() will
	 * contain the last k class labels as features.
	 */
	public void setHistorySize(int k)
	{
		historyLength = k;
		history = new String[historyLength];
	}

	/** Return the current history length.
	 * Examples produced by the iterator() will
	 * contain the last k class labels as features.
	 */
	public int getHistorySize()
	{
		return historyLength;
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
		addSequence(new Example[]{example});
	}

	/** Add a new sequence of examples to the dataset. */
	public void addSequence(Example[] sequence)
	{
		Example[] compressedSeq = new Example[sequence.length];
		for (int i=0; i<sequence.length; i++) {
			compressedSeq[i] = factory.compress( sequence[i] );
			classNameSet.addAll( sequence[i].getLabel().possibleLabels() );
		}
		sequenceList.add(compressedSeq);
		totalSize += sequence.length;
	}

	/** Iterate over all examples, extended so as to contain history information. */
	public Example.Looper iterator()
	{
		return new Example.Looper(new MyIterator());
	}

	/** Return the number of examples. */
	public int size()
	{
		return totalSize;
	}

	/** Return the number of sequences. */
	public int numberOfSequences()
	{
		return sequenceList.size();
	}

	/** Return an iterator over all sequences. 
	 * Each item returned by this will be of type Example[]. */
	public Iterator sequenceIterator()
	{
		return sequenceList.iterator();
	}

	/** Randomly re-order the examples. */
	public void shuffle(Random r)
	{
		Collections.shuffle(sequenceList,r);
	}

	/** Randomly re-order the examples. */
	public void shuffle()
	{
		shuffle(new Random(0));
	}

	/** Make a shallow copy of the dataset. Sequences are shared, but not the 
	 * ordering of the Sequences. */
	public Dataset shallowCopy()
	{
		SequenceDataset copy = new SequenceDataset();
		copy.setHistorySize( getHistorySize() );
		for (Iterator i=sequenceList.iterator(); i.hasNext(); ) {
			copy.addSequence( (Example[]) i.next() );
		}
		return copy;
	}

	//
	// split
	//

	public Split split(final Splitter splitter)
	{
		splitter.split(sequenceList.iterator());
		return new Split() {
				public int getNumPartitions() { return splitter.getNumPartitions(); }
				public Dataset getTrain(int k) { return invertIteration(splitter.getTrain(k)); }
				public Dataset getTest(int k) { return invertIteration(splitter.getTest(k)); }
			};
	}
	protected Dataset invertIteration(Iterator i) 
	{
		SequenceDataset copy = new SequenceDataset();
		copy.setHistorySize( getHistorySize() );
		while (i.hasNext()) {
			Object o = i.next();
			copy.addSequence((Example[])o);
		}
		return copy;
	}

	//
	// iterate over examples, having added extra history fields to them
	//

	private class MyIterator implements Iterator
	{
		private Iterator i;
		private Example[] buf;
		private int j;
		public MyIterator()
		{
			i = sequenceList.iterator();
			if (i.hasNext()) buf = (Example[])i.next();
			else buf = new Example[]{};
			j = 0;
		}
		public boolean hasNext()
		{
			return (j<buf.length || i.hasNext());
		}
		public Object next()
		{
			if (j>=buf.length) {
				buf = (Example[])i.next();
				j = 0;
			}
			// build history
			InstanceFromSequence.fillHistory( history, buf, j );
			//for (int k=0; k<historyLength; k++) {
			//	if (j-k-1>=0) history[k] = buf[j-k-1].getLabel().bestClassName();
			//  else history[k] = NULL_CLASS_NAME;
			//}
			Example e = buf[j++];
			if (e==null) throw new IllegalStateException("null example at pos "+j+" buf "+StringUtil.toString(buf));
			return new Example(new InstanceFromSequence(e,history), e.getLabel());
		}
		public void remove()
		{
			throw new UnsupportedOperationException("can't remove");
		}
	}


	public String toString()
	{
		StringBuffer buf = new StringBuffer("[SeqData:\n");
		for (Iterator i=sequenceList.iterator(); i.hasNext(); ) {
			Example[] seq = (Example[]) i.next();
			for (int j=0; j<seq.length; j++) {
				buf.append(" "+seq[j]);
			}
			buf.append("\n");
		}
		buf.append("]");
		return buf.toString();
	}

	//
	// Implement Saveable interface. 
	//
	static private final String FORMAT_NAME = "Minorthird Sequential Dataset";
	public String[] getFormatNames() { return new String[] {FORMAT_NAME}; } 
	public String getExtensionFor(String s) { return ".seqdata"; }
	public void saveAs(File file,String format) throws IOException
	{
		if (!format.equals(FORMAT_NAME)) throw new IllegalArgumentException("illegal format "+format);
		DatasetLoader.saveSequence(this,file);
	}
	public Object restore(File file) throws IOException
	{
		try {
			return DatasetLoader.loadSequence(file);
		} catch (NumberFormatException ex) {
			throw new IllegalStateException("error loading from "+file+": "+ex);
		}
	}

	/** A GUI view of the dataset. */
	public Viewer toGUI()
	{
		Viewer dbGui = new MyDataViewer();
		dbGui.setContent(this);
		Viewer seqGui = GUI.newSourcedExampleViewer();
		return new ZoomedViewer(dbGui,seqGui);
	}

	private static class MyDataViewer extends ComponentViewer
	{
		public JComponent componentFor(Object o) {
			SequenceDataset d = (SequenceDataset)o;
			final Example[] arr = new Example[d.size()];
			int k=0;
			for (Example.Looper i=d.iterator(); i.hasNext(); ) {
				arr[k++] = i.nextExample();
			}
			JList jList = new JList(arr);
			jList.setCellRenderer( new ListCellRenderer() {
					public Component getListCellRendererComponent(JList el,Object v,int index,boolean sel,boolean focus){
						return GUI.conciseExampleRendererComponent(arr[index],100,sel);
					}});
			monitorSelections(jList);
			return new JScrollPane(jList);		
		}
	}

	public static void main(String[] args) throws IOException
	{
		SequenceDataset d = SampleDatasets.makeToySequenceData();
		System.out.println(d.toString());
		ViewerFrame f = new ViewerFrame("Sequence data",d.toGUI());
		if (args.length>0) 
			DatasetLoader.saveSequence(d,new File(args[0]));
	}
}
