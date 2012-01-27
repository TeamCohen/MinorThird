/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.FeatureFactory;
import edu.cmu.minorthird.classify.GUI;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ZoomedViewer;

/**
 * A SequenceDataset that additionally includes examples for 'sliding
 * windows' over the original data.  
 *
 * @author William Cohen
 */

public class SegmentDataset implements Dataset{

	int maxWindowSize=-1;

	private List<CandidateSegmentGroup> groupList=new ArrayList<CandidateSegmentGroup>();

	private Set<String> classNameSet=new HashSet<String>();

	private int totalSize=0;

	private FeatureFactory factory=new FeatureFactory();

	private boolean compressGroups=true;

	public SegmentDataset(){
		;
	}

	public void setDataCompression(boolean flag){
		compressGroups=flag;
	}

	@Override
	public FeatureFactory getFeatureFactory(){
		return factory;
	}

	public int getMaxWindowSize(){
		return maxWindowSize;
	}

	@Override
	public int size(){
		return totalSize;
	}

	public int getNumberOfSegmentGroups(){
		return groupList.size();
	}

	/** Add a new sequence of examples to the dataset. */
	public void addCandidateSegmentGroup(CandidateSegmentGroup group){
		if(maxWindowSize<0)
			maxWindowSize=group.getMaxWindowSize();
		if(maxWindowSize>=0&&group.getMaxWindowSize()!=maxWindowSize){
			throw new IllegalArgumentException("mismatched window sizes: "+
					maxWindowSize+", "+group.getMaxWindowSize());
		}
		if(compressGroups)
			groupList.add(new CompactCandidateSegmentGroup(factory,group));
		else
			groupList.add(group);
		classNameSet.addAll(group.classNameSet());
		totalSize+=group.size();
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

	/**
	 * Add an example to the dataset. <br>
	 * <br>
	 * This method compresses the example before adding it to the dataset.  If
	 * you want/need the example to be compressed then call {@link #add(Example, boolean)}
	 *
	 * @param example The Example that you want to add to the dataset.
	 */
	@Override
	public void add(Example example){
		add(example,false);
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
		MutableCandidateSegmentGroup g=new MutableCandidateSegmentGroup(1,1);

		if(compress)
			g.setSubsequence(0,1,factory.compress(example.asInstance()),example
					.getLabel());
		else
			g.setSubsequence(0,1,example.asInstance(),example.getLabel());
		addCandidateSegmentGroup(g);
	}

	/** Iterate over all examples */
	@Override
	public Iterator<Example> iterator(){
		List<Example> result=new ArrayList<Example>();
		for(Iterator<CandidateSegmentGroup> i=groupList.iterator();i.hasNext();){
			CandidateSegmentGroup g=i.next();
			for(int j=0;j<g.getSequenceLength();j++){
				for(int k=1;k<=g.getMaxWindowSize();k++){
					Example e=g.getSubsequenceExample(j,j+k);
					if(e!=null)
						result.add(e);
				}
			}
		}
		return result.iterator();
	}

	public Iterator<CandidateSegmentGroup> candidateSegmentGroupIterator(){
		return groupList.iterator();
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("");
		buf.append("size = "+size()+"\n");
		for(Iterator<CandidateSegmentGroup> i=groupList.iterator();i.hasNext();){
			buf.append(i.next()+"\n");
		}
		return buf.toString();
	}

	/** Randomly re-order the examples. */
	@Override
	public void shuffle(Random r){
		Collections.shuffle(groupList,r);
	}

	/** Randomly re-order the examples. */
	@Override
	public void shuffle(){
		Collections.shuffle(groupList,new Random(0));
	}

	/** Make a shallow copy of the dataset. */
	@Override
	public Dataset shallowCopy(){
		SegmentDataset copy=new SegmentDataset();
		for(Iterator<CandidateSegmentGroup> i=groupList.iterator();i.hasNext();){
			copy.addCandidateSegmentGroup(i.next());
		}
		return copy;
	}

	//
	// split
	//
	
	@Override
	public Split split(final Splitter<Example> splitter){
		throw new UnsupportedOperationException();
	}
	
	public Split splitCandidateSegmentGroup(final Splitter<CandidateSegmentGroup> splitter){
		splitter.split(groupList.iterator());
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

	protected Dataset invertIteration(Iterator<CandidateSegmentGroup> i){
		SegmentDataset copy=new SegmentDataset();
		while(i.hasNext()){
			CandidateSegmentGroup o=i.next();
			copy.addCandidateSegmentGroup(o);
		}
		return copy;
	}

	/** A GUI view of the dataset. */
	@Override
	public Viewer toGUI(){
		//return new VanillaViewer(this);
		Viewer dbGui=new BasicDataset.SimpleDatasetViewer();
		dbGui.setContent(this);
		Viewer instGui=GUI.newSourcedExampleViewer();
		return new ZoomedViewer(dbGui,instGui);
	}

	public int getNumPosExamples(){
		return -1;
	}
}
