/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.relational;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.SGMExample;
import edu.cmu.minorthird.classify.SGMFeatureFactory;
import edu.cmu.minorthird.classify.SampleDatasets;
import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * A core set of examples for stached graphical learning -- dataset + linksmap.
 *
 * @author Zhenzhen Kou
 */

public class CoreRelationalDataset extends BasicDataset implements Visible,
		Saveable,Dataset,Serializable{
	
	static final long serialVersionUID=20080128L;

	// this linksMap is static, why? - frank
	protected static Map<String,Map<String,Set<String>>> linksMap=new HashMap<String,Map<String,Set<String>>>();

	protected SGMFeatureFactory factory=new SGMFeatureFactory();

	public void addSGM(SGMExample example){
		this.addSGM(example,true);
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
	public void addSGM(SGMExample example,boolean compress){
		if(compress)
			examples.add(factory.compressSGM(example));
		else
			examples.add(example);
		classNameSet.addAll(example.getLabel().possibleLabels());
	}

	/**
	 * Add a link to the LinksMap of the dataset. <br>
	 * A link contains 'from', 'to', and 'type'
	 * Please note that if the link type is 'left', it means 
	 * the left ngb of 'from' is 'to'
	 * 
	 * @param link The Link that you want to add to the dataset.
	 */
	static public void addLink(Link link){
		String from=link.getFrom();
		String to=link.getTo();
		String type=link.getType();
		// Is there a reason we are cloning these things? I can't see why. - frank
		if(linksMap.containsKey(from)){
			if(linksMap.get(from).containsKey(type)){
				linksMap.get(from).get(type).add(to);
			}else{
				HashSet<String> set=new HashSet<String>();
				set.add(to);
				linksMap.get(from).put(type,(Set<String>)set.clone());
			}
		}else{
			HashMap<String,Set<String>> map=new HashMap<String,Set<String>>();
			HashSet<String> set=new HashSet<String>();
			set.add(to);
			map.put(type,(Set<String>)set.clone());
			linksMap.put(from,(Map<String,Set<String>>)map.clone());
		}

	}

	public SGMExample getExampleWithID(String id){
		for(Iterator<Example> i=examples.iterator();i.hasNext();){
			SGMExample e=(SGMExample)i.next();
			if(e.hasID(id)){
				return e;
			}
		}
		return null;
	}

	public static Map<String,Map<String,Set<String>>> getLinksMap(){
		return linksMap;
	}

	public static void setLinksMap(Map<String,Map<String,Set<String>>> linksMap){
		CoreRelationalDataset.linksMap=linksMap;
	}

	// test routine

	/** Simple test routine */
	static public void main(String[] args){
		try{
			BasicDataset data=(BasicDataset)SampleDatasets.sampleData("toy",false);
			System.out.println(data.getSchema());
			addLink(new Link("1","2","left"));
			addLink(new Link("1","3","left"));
			addLink(new Link("2","3","left"));
			System.out.println(linksMap);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
