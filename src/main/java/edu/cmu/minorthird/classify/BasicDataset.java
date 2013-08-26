/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;
import edu.cmu.minorthird.util.gui.ZoomedViewer;

/**
 * A set of examples for learning.
 *
 * @author William Cohen
 */

public class BasicDataset implements Dataset,Serializable,Visible,Saveable{

	static final long serialVersionUID=20080118L;

	protected FeatureFactory featureFactory;
	protected List<Example> examples;
	protected List<Instance> unlabeledExamples;
	protected Set<String> classNameSet;

	public BasicDataset(FeatureFactory featureFactory){
		this.featureFactory=featureFactory;
		examples=new ArrayList<Example>();
		unlabeledExamples=new ArrayList<Instance>();
		classNameSet=new TreeSet<String>();
	}

	public BasicDataset(){
		this(new FeatureFactory());
	}

	@Override
	public ExampleSchema getSchema(){
		ExampleSchema schema=new ExampleSchema(classNameSet.toArray(new String[classNameSet.size()]));
		if(schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA)){
			return ExampleSchema.BINARY_EXAMPLE_SCHEMA;
		}else{
			return schema;
		}
	}

	// methods for semisupervised data,  part of the SemiSupervisedDataset interface

	public void addUnlabeled(Instance instance){
		unlabeledExamples.add(featureFactory.compress(instance));
	}

	public Iterator<Instance> iteratorOverUnlabeled(){
		return unlabeledExamples.iterator();
	}

	//public ArrayList getUnlabeled() { return this.unlabeledExamples; }
	public int sizeUnlabeled(){
		return unlabeledExamples.size();
	}

	public boolean hasUnlabeled(){
		return (unlabeledExamples.size()>0)?true:false;
	}

	@Override
	public FeatureFactory getFeatureFactory(){
		return featureFactory;
	}

	//
	// methods for labeled data,  part of the Dataset interface
	//

	/**
	 * Add an example to the dataset. <br>
	 * <br>
	 * This method compresses the example before adding it to the dataset.  If 
	 * you don't want/need the example to be compressed then call {@link #add(Example, boolean)}
	 *
	 * @param example The Example that you want to add to the dataset.
	 */
	@Override
	public void add(Example example){
		this.add(example,true);
	}

	/**
	 * Add an Example to the dataset. <br>
	 * <br>
	 * This method lets the caller specify whether or not to compress the example
	 * before adding it to the dataset.
	 *
	 * @param example The example to add to the dataset
	 * @param compress Boolean specifying whether or not to compress the example.
	 */
	@Override
	public void add(Example example,boolean compress){
		if(compress)
			examples.add(featureFactory.compress(example));
		else
			examples.add(example);
		classNameSet.addAll(example.getLabel().possibleLabels());
	}

	@Override
	public Iterator<Example> iterator(){
		return examples.iterator();
	}

	@Override
	public int size(){
		return examples.size();
	}

	@Override
	public void shuffle(Random r){
		Collections.shuffle(examples,r);
	}

	@Override
	public void shuffle(){
		shuffle(new Random());
	}

	@Override
	public Dataset shallowCopy(){
		Dataset copy=new BasicDataset();
		for(Iterator<Example> i=iterator();i.hasNext();){
			copy.add(i.next());
		}
		return copy;
	}

	// Implement Saveable interface.

	static private final String FORMAT_NAME="Minorthird Dataset";

	@Override
	public String[] getFormatNames(){
		return new String[]{FORMAT_NAME};
	}

	@Override
	public String getExtensionFor(String s){
		return ".data";
	}

	@Override
	public void saveAs(File file,String format)throws IOException{
		if(!format.equals(FORMAT_NAME)){
			throw new IllegalArgumentException("illegal format: "+format);
		}
		else{
			DatasetLoader.save(this,file);
		}
	}

	@Override
	public Object restore(File file) throws IOException{
		try{
			return DatasetLoader.loadFile(file);
		}catch(NumberFormatException ex){
			throw new IllegalStateException("error loading from "+file+": "+ex);
		}
	}

	/** A string view of the dataset */
	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("");
		for(Iterator<Example> i=this.iterator();i.hasNext();){
			Example ex=i.next();
			buf.append(ex.toString());
			buf.append("\n");
		}
		return buf.toString();
	}

	/** A GUI view of the dataset. */
	@Override
	public Viewer toGUI(){
		Viewer dbGui=new SimpleDatasetViewer();
		dbGui.setContent(this);
		Viewer instGui=GUI.newSourcedExampleViewer();
		return new ZoomedViewer(dbGui,instGui);
	}

	public static class SimpleDatasetViewer extends ComponentViewer{

		static final long serialVersionUID=20071015;

		@Override
		public boolean canReceive(Object o){
			return o instanceof Dataset;
		}

		@Override
		public JComponent componentFor(Object o){
			final Dataset d=(Dataset)o;
			final Example[] tmp=new Example[d.size()];
			int k=0;
			for(Iterator<Example> i=d.iterator();i.hasNext();){
				tmp[k++]=i.next();
			}
			final JList jList=new JList(tmp);
			jList.setCellRenderer(new ListCellRenderer(){

				@Override
				public Component getListCellRendererComponent(JList el,Object v,
						int index,boolean sel,boolean focus){
					return GUI
					.conciseExampleRendererComponent(tmp[index],60,sel);
				}
			});
			monitorSelections(jList);
			return new JScrollPane(jList);
		}
	}

	//
	// splitter
	//

	@Override
	public Split split(final Splitter<Example> splitter){
		splitter.split(examples.iterator());
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

	private Dataset invertIteration(Iterator<Example> i){
		BasicDataset copy=new BasicDataset();
		while(i.hasNext())
			copy.add(i.next());
		return copy;
	}

	//
	// test routine
	//

	/** Simple test routine */
	static public void main(String[] args){
		try{
			BasicDataset data=(BasicDataset)SampleDatasets.sampleData("toy",false);
			new ViewerFrame("Toy Dataset",data.toGUI());
			System.out.println(data.getSchema());
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
