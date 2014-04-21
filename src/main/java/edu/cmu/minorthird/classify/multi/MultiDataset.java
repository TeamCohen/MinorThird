/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
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

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.BatchVersion;
import edu.cmu.minorthird.classify.CascadingBinaryLearner;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.FeatureFactory;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.classify.algorithms.linear.VotedPerceptron;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;
import edu.cmu.minorthird.util.gui.ZoomedViewer;

/**
 * A set of examples for learning.
 * 
 * @author Cameron Williams
 */

public class MultiDataset implements Dataset,Visible,Saveable{

	static final long serialVersionUID=20080130L;

	protected List<MultiExample> examples=new ArrayList<MultiExample>();

	protected List<Instance> unlabeledExamples=new ArrayList<Instance>();

	protected FeatureFactory factory=new FeatureFactory();
	
	protected List<Set<String>> classNameSets;

	public int numPosExamples=0;

	/** Overridden, provides ExampleSchema for first dimension */
	@Override
	public ExampleSchema getSchema(){
		ExampleSchema schema=
				new ExampleSchema(classNameSets.get(0)
						.toArray(new String[classNameSets.get(0).size()]));
		if(schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA))
			return ExampleSchema.BINARY_EXAMPLE_SCHEMA;
		else
			return schema;
	}

	public MultiExampleSchema getMultiSchema(){
		ExampleSchema[] schemas=new ExampleSchema[classNameSets.size()];
		for(int i=0;i<schemas.length;i++){
			schemas[i]=
					new ExampleSchema(classNameSets.get(i)
							.toArray(new String[classNameSets.get(i).size()]));
			if(schemas.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA))
				schemas[i]=ExampleSchema.BINARY_EXAMPLE_SCHEMA;
		}
		MultiExampleSchema multiSchema=new MultiExampleSchema(schemas);
		return multiSchema;
	}
	
	public MultiExample getMultiExample(int i){
		return examples.get(i);
	}

	@Override
	public FeatureFactory getFeatureFactory(){
		return factory;
	}

	//
	// methods for semisupervised data, part of the SemiSupervisedDataset
	// interface
	//
	public void addUnlabeled(Instance instance){
		unlabeledExamples.add(factory.compress(instance));
	}

	public Iterator<Instance> iteratorOverUnlabeled(){
		return unlabeledExamples.iterator();
	}

	// public ArrayList getUnlabeled() { return this.unlabeledExamples; }
	public int sizeUnlabeled(){
		return unlabeledExamples.size();
	}

	public boolean hasUnlabeled(){
		return (unlabeledExamples.size()>0)?true:false;
	}

	@Override
	public void add(Example example){
		throw new IllegalArgumentException(
				"You must add a MultiExample to a MutiDataset");
	}

	@Override
	public void add(Example example,boolean compress){
		throw new IllegalArgumentException(
				"You must add a MultiExample to a MutiDataset");
	}

	//
	// methods for labeled data, part of the Dataset interface
	//
	public void addMulti(MultiExample example){
		if(classNameSets==null){
			classNameSets=new ArrayList<Set<String>>(example.getMultiLabel().numDimensions());
			for(int i=0;i<classNameSets.size();i++){
				classNameSets.add(new TreeSet<String>());
			}
		}
		if(classNameSets.size()!=example.getMultiLabel().numDimensions())
			throw new IllegalArgumentException(
					"This example does not have the same number of dimensions as previous examples");

		examples.add(factory.compress(example));
		List<Set<String>> possibleLabels=example.getMultiLabel().possibleLabels();
		for(int i=0;i<classNameSets.size();i++){
			classNameSets.get(i).addAll(possibleLabels.get(i));
		}

		// Maybe change
		ClassLabel cl=example.getLabel();
		if(cl.isPositive())
			numPosExamples++;
	}

	public Dataset[] separateDatasets(){
		Example[] ex_one=(examples.get(0)).getExamples();
		Dataset[] d=new BasicDataset[ex_one.length];
		for(int i=0;i<d.length;i++){
			d[i]=new BasicDataset();
		}
		for(int i=0;i<examples.size();i++){
			Example[] ex=(examples.get(i)).getExamples();
			for(int j=0;j<ex.length;j++){
				d[j].add(ex[j]);
			}
		}
		return d;
	}

	public int getNumPosExamples(){
		return numPosExamples;
	}

	// Why don't we just overwrite these methods? Also, it's not an illegal argument. - frank
	@Override
	public Iterator<Example> iterator(){
		throw new IllegalArgumentException(
				"Must use multiIterator to iterate through MultiExamples");
	}

	public Iterator<MultiExample> multiIterator(){
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
		shuffle(new Random(999));
	}

	@Override
	public Dataset shallowCopy(){
		MultiDataset copy=new MultiDataset();
		for(Iterator<MultiExample> i=multiIterator();i.hasNext();){
			copy.addMulti(i.next());
		}
		return copy;
	}

	//
	// Implement Saveable interface.
	//
	static private final String FORMAT_NAME="Minorthird MultiDataset";

	@Override
	public String[] getFormatNames(){
		return new String[]{FORMAT_NAME};
	}

	@Override
	public String getExtensionFor(String s){
		return ".multidata";
	}

	@Override
	public void saveAs(File file,String format) throws IOException{
		if(!format.equals(FORMAT_NAME))
			throw new IllegalArgumentException("illegal format "+format);
		DatasetLoader.save(this,file);
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
		for(Iterator<MultiExample> i=this.multiIterator();i.hasNext();){
			MultiExample ex=i.next();
			buf.append(ex.toString());
			buf.append("\n");
		}
		return buf.toString();
	}

	public MultiDataset annotateData(){
		MultiDataset annotatedDataset=new MultiDataset();
		Splitter<MultiExample> splitter=new CrossValSplitter<MultiExample>(9);
		MultiDataset.MultiSplit s=this.MultiSplit(splitter);
		for(int x=0;x<9;x++){
			MultiClassifierTeacher teacher=
					new MultiDatasetClassifierTeacher(s.getTrain(x));
			ClassifierLearner lnr=
					new CascadingBinaryLearner(new BatchVersion(new VotedPerceptron()));
			MultiClassifier c=teacher.train(lnr);
			for(Iterator<MultiExample> i=s.getTest(x).multiIterator();i.hasNext();){
				MultiExample ex=i.next();
				Instance instance=ex.asInstance();
				MultiClassLabel predicted=c.multiLabelClassification(instance);
				Instance annotatedInstance=
						new InstanceFromPrediction(instance,predicted.bestClassName());
				MultiExample newEx=
						new MultiExample(annotatedInstance,ex.getMultiLabel(),ex
								.getWeight());
				annotatedDataset.addMulti(newEx);
			}
		}
		return annotatedDataset;
	}

	public MultiDataset annotateData(MultiClassifier multiClassifier){
		MultiDataset annotatedDataset=new MultiDataset();
		for(Iterator<MultiExample> i=this.multiIterator();i.hasNext();){
			MultiExample ex=i.next();
			Instance instance=ex.asInstance();
			MultiClassLabel predicted=
					multiClassifier.multiLabelClassification(instance);
			Instance annotatedInstance=
					new InstanceFromPrediction(instance,predicted.bestClassName());
			MultiExample newEx=
					new MultiExample(annotatedInstance,ex.getMultiLabel(),ex.getWeight());
			annotatedDataset.addMulti(newEx);
		}
		return annotatedDataset;
	}

	/** A GUI view of the dataset. */
	@Override
	public Viewer toGUI(){
		Viewer dbGui=new SimpleDatasetViewer();
		dbGui.setContent(this);
		Viewer instGui=GUI.newSourcedMultiExampleViewer();
		return new ZoomedViewer(dbGui,instGui);
	}

	public static class SimpleDatasetViewer extends ComponentViewer{

		static final long serialVersionUID=20080130L;
		
		@Override
		public boolean canReceive(Object o){
			return o instanceof Dataset;
		}

		@Override
		public JComponent componentFor(Object o){
			final MultiDataset d=(MultiDataset)o;
			final MultiExample[] tmp=new MultiExample[d.size()];
			int k=0;
			for(Iterator<MultiExample> i=d.multiIterator();i.hasNext();){
				tmp[k++]=i.next();
			}
			final JList jList=new JList(tmp);
			jList.setCellRenderer(new ListCellRenderer(){

				@Override
				public Component getListCellRendererComponent(JList el,Object v,
						int index,boolean sel,boolean focus){
					return GUI.conciseMultiExampleRendererComponent(
							tmp[index],60,sel);
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
		System.err.println("Split split() not implemented.");
		return null;
	}

	public class MultiSplit{

		Splitter<MultiExample> splitter;

		public MultiSplit(Splitter<MultiExample> splitter){
			this.splitter=splitter;
		}

		public int getNumPartitions(){
			return splitter.getNumPartitions();
		}

		public MultiDataset getTrain(int k){
			return invertMultiIteration(splitter.getTrain(k));
		}

		public MultiDataset getTest(int k){
			return invertMultiIteration(splitter.getTest(k));
		}
	}

	public MultiSplit MultiSplit(final Splitter<MultiExample> splitter){
		splitter.split(examples.iterator());
		return new MultiSplit(splitter);
	}

	private MultiDataset invertMultiIteration(Iterator<MultiExample> i){
		MultiDataset copy=new MultiDataset();
		while(i.hasNext())
			copy.addMulti(i.next());
		return copy;
	}

	//
	// test routine
	//

	/** Simple test routine */
	static public void main(String[] args){
		System.out.println("Not working yet");
	}

}
