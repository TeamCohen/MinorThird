/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.FeatureFactory;
import edu.cmu.minorthird.classify.GUI;
import edu.cmu.minorthird.classify.SampleDatasets;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;
import edu.cmu.minorthird.util.gui.ZoomedViewer;

/**
 * A dataset of sequences of examples.
 *
 * @author William Cohen
 */

public class SequenceDataset implements Dataset,SequenceConstants,Visible,
		Saveable{

	protected List<Example[]> sequenceList=new ArrayList<Example[]>();

	protected int totalSize=0;

	private int historyLength=1;

	private String[] history=new String[historyLength];

	protected Set<String> classNameSet=new HashSet<String>();

	protected FeatureFactory factory=new FeatureFactory();

	@Override
	public FeatureFactory getFeatureFactory(){
		return factory;
	}

	/** Set the current history length.
	 * Examples produced by the iterator() will
	 * contain the last k class labels as features.
	 */
	public void setHistorySize(int k){
		historyLength=k;
		history=new String[historyLength];
	}

	/** Return the current history length.
	 * Examples produced by the iterator() will
	 * contain the last k class labels as features.
	 */
	public int getHistorySize(){
		return historyLength;
	}

	@Override
	public ExampleSchema getSchema(){
		ExampleSchema schema=
				new ExampleSchema(classNameSet
						.toArray(new String[classNameSet.size()]));
		if(schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA))
			return ExampleSchema.BINARY_EXAMPLE_SCHEMA;
		else
			return schema;
	}

	/** Add a new example to the dataset. <br>
	 * <br>
	 * This method compresses the example before adding it to the
	 * the dataset.  To prevent this compresstion call {@link #add(Example, boolean)}.
	 *
	 * @param example The example to add to the dataset.
	 */
	@Override
	public void add(Example example){
		addSequence(new Example[]{example});
	}

	/** Add a new example to the dataset. <br>
	 * <br>
	 * This method allows the caller to specify if they want the examples to be
	 * compressed or not.
	 *
	 * @param example The example to add to the dataset.
	 * @param compress Boolean specifying whether or not to compress the example.
	 */
	@Override
	public void add(Example example,boolean compress){
		addSequence(new Example[]{example},compress);
	}

	/** Add a new sequence of examples to the dataset. <br>
	 * <br>
	 * This method compresses each example before adding it to the
	 * the dataset.  To prevent this compresstion call {@link #addSequence(Example[], boolean)}.
	 */
	public void addSequence(Example[] sequence){
		addSequence(sequence,true);
	}

	/** Add a new sequence of examples to the dataset <br>
	 * <br>
	 * This method allows the caller to specify if they want the examples to be 
	 * compressed or not.
	 *
	 * @param sequence The sequence of examples to add to the dataset
	 * @param compress Boolean specifying whether or not to compress the examples.
	 */
	public void addSequence(Example[] sequence,boolean compress){
		// If the user wants to compress the examples in the sequence then
		//   create a new array and fill it with the compressed examples.
		//   Then add the new array of examples to the dataset.
		if(compress){
			Example[] compressedSeq=new Example[sequence.length];
			for(int i=0;i<sequence.length;i++){
				compressedSeq[i]=factory.compress(sequence[i]);
				classNameSet.addAll(sequence[i].getLabel().possibleLabels());
			}
			sequenceList.add(compressedSeq);
		}
		// If the caller doesn't want the examples compressed then just add 
		//   the array of examples to the dataset.
		else{
			sequenceList.add(sequence);
		}
		totalSize+=sequence.length;
	}

	/** Iterate over all examples, extended so as to contain history information. */
	@Override
	public Iterator<Example> iterator(){
		return new MyIterator();
	}

	/** Return the number of examples. */
	@Override
	public int size(){
		return totalSize;
	}

	/** Return the number of sequences. */
	public int numberOfSequences(){
		return sequenceList.size();
	}

	/** Return an iterator over all sequences. 
	 * Each item returned by this will be of type Example[]. */
	public Iterator<Example[]> sequenceIterator(){
		return sequenceList.iterator();
	}

	/** Randomly re-order the examples. */
	@Override
	public void shuffle(Random r){
		Collections.shuffle(sequenceList,r);
	}

	/** Randomly re-order the examples. */
	@Override
	public void shuffle(){
		shuffle(new Random(0));
	}

	/** Make a shallow copy of the dataset. Sequences are shared, but not the 
	 * ordering of the Sequences. */
	@Override
	public Dataset shallowCopy(){
		SequenceDataset copy=new SequenceDataset();
		copy.setHistorySize(getHistorySize());
		for(Iterator<Example[]> i=sequenceList.iterator();i.hasNext();){
			copy.addSequence(i.next());
		}
		return copy;
	}

	//
	// split
	//

	@Override
	public Split split(final Splitter<Example> splitter){
		throw new UnsupportedOperationException("Use splitSequence instead.");
	}

	public Split splitSequence(final Splitter<Example[]> splitter){
		splitter.split(sequenceList.iterator());
		return new Split(){

			@Override
			public int getNumPartitions(){
				return splitter.getNumPartitions();
			}

			@Override
			public Dataset getTrain(int k){
				return invertIteration(splitter.getTrain(k));
			}

			@Override
			public Dataset getTest(int k){
				return invertIteration(splitter.getTest(k));
			}
		};
	}

	protected Dataset invertIteration(Iterator<Example[]> i){
		SequenceDataset copy=new SequenceDataset();
		copy.setHistorySize(getHistorySize());
		while(i.hasNext()){
			Example[] o=i.next();
			copy.addSequence(o);
		}
		return copy;
	}

	//
	// iterate over examples, having added extra history fields to them
	//

	private class MyIterator implements Iterator<Example>{

		private Iterator<Example[]> i;

		private Example[] buf;

		private int j;

		public MyIterator(){
			i=sequenceList.iterator();
			if(i.hasNext())
				buf=i.next();
			else
				buf=new Example[]{};
			j=0;
		}

		@Override
		public boolean hasNext(){
			return(j<buf.length||i.hasNext());
		}

		@Override
		public Example next(){
			if(j>=buf.length){
				buf=i.next();
				j=0;
			}
			// build history
			InstanceFromSequence.fillHistory(history,buf,j);
			//for (int k=0; k<historyLength; k++) {
			//	if (j-k-1>=0) history[k] = buf[j-k-1].getLabel().bestClassName();
			//  else history[k] = NULL_CLASS_NAME;
			//}
			Example e=buf[j++];
			if(e==null)
				throw new IllegalStateException("null example at pos "+j+" buf "+
						StringUtil.toString(buf));
			return new Example(new InstanceFromSequence(e,history),e.getLabel());
		}

		@Override
		public void remove(){
			throw new UnsupportedOperationException("can't remove");
		}
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("[SeqData:\n");
		for(Iterator<Example[]> i=sequenceList.iterator();i.hasNext();){
			Example[] seq=i.next();
			for(int j=0;j<seq.length;j++){
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
	static private final String FORMAT_NAME="Minorthird Sequential Dataset";

	@Override
	public String[] getFormatNames(){
		return new String[]{FORMAT_NAME};
	}

	@Override
	public String getExtensionFor(String s){
		return ".seqdata";
	}

	@Override
	public void saveAs(File file,String format) throws IOException{
		if(!format.equals(FORMAT_NAME))
			throw new IllegalArgumentException("illegal format "+format);
		DatasetLoader.saveSequence(this,file);
	}

	@Override
	public Object restore(File file) throws IOException{
		try{
			return DatasetLoader.loadSequence(file);
		}catch(NumberFormatException ex){
			throw new IllegalStateException("error loading from "+file+": "+ex);
		}
	}

	/** A GUI view of the dataset. */
	@Override
	public Viewer toGUI(){
		Viewer dbGui=new MyDataViewer();
		dbGui.setContent(this);
		Viewer seqGui=GUI.newSourcedExampleViewer();
		return new ZoomedViewer(dbGui,seqGui);
	}

	private static class MyDataViewer extends ComponentViewer{

		static final long serialVersionUID=20080207L;

		@Override
		public JComponent componentFor(Object o){
			SequenceDataset d=(SequenceDataset)o;
			final Example[] arr=new Example[d.size()];
			int k=0;
			for(Iterator<Example> i=d.iterator();i.hasNext();){
				arr[k++]=i.next();
			}
			JList jList=new JList(arr);
			jList.setCellRenderer(new ListCellRenderer(){

				@Override
				public Component getListCellRendererComponent(JList el,Object v,
						int index,boolean sel,boolean focus){
					return GUI.conciseExampleRendererComponent(arr[index],100,sel);
				}
			});
			monitorSelections(jList);
			return new JScrollPane(jList);
		}
	}

	public static void main(String[] args) throws IOException{
		SequenceDataset d=SampleDatasets.makeToySequenceData();
		System.out.println(d.toString());
		new ViewerFrame("Sequence data",d.toGUI());
		if(args.length>0)
			DatasetLoader.saveSequence(d,new File(args[0]));
	}

	public int getNumPosExamples(){
		return -1;
	}
}
