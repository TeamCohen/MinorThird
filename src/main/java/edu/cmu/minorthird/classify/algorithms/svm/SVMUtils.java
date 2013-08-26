package edu.cmu.minorthird.classify.algorithms.svm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import libsvm.svm_node;
import libsvm.svm_problem;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.FeatureFactory;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;

/**
 * Provides some basic utilities for dealing with libsvm.
 * It can convert Features to nodes, instances to node arrays and Datasets to problems.
 *
 * @author ksteppe, Frank Lin
 */
public class SVMUtils{

	static Logger logger=Logger.getLogger(SVMUtils.class);

	public static String toString(svm_node node){
		StringBuilder b=new StringBuilder();
		b.append(node.index).append("=").append(node.value);
		return b.toString();
	}

	public static String toString(svm_node[] nodes){
		StringBuilder b=new StringBuilder();
		for(int i=0;i<nodes.length;i++){
			b.append(nodes[i].index).append("=").append(nodes[i].value);
			if(i<nodes.length-1){
				b.append(" ");
			}
		}
		return b.toString();
	}

	public static String toString(svm_problem problem){
		StringBuilder b=new StringBuilder();
		for(int i=0;i<problem.y.length;i++){
			b.append(problem.y[i]).append(" : ").append(toString(problem.x[i])).append("\n");
		}
		return b.toString();
	}

	private static final Comparator<svm_node> NODE_COMPARATOR=new Comparator<svm_node>(){
		@Override
		public int compare(svm_node n1,svm_node n2){
			return n1.index-n2.index;
		}
	};

	/**
	 * converts the feature into an svm_node
	 *
	 * @param feature Feature to convert into a node
	 * @param instance Instance feature is in - used to retrieve the weight of the feature
	 * @return svm_node
	 */
	public static svm_node featureToNode(Feature feature,Instance instance){
		svm_node svm_node=new svm_node();
		// important: LIBSVM feature index starts at 1, not 0
		svm_node.index=feature.getID()+1;
		svm_node.value=instance.getWeight(feature);
		return svm_node;
	}

	/**
	 * creates the node array from an instance
	 *
	 * @param instance Instance to convert
	 * @return node array with all the features from the instance
	 */
	public static svm_node[] instanceToNodeArray(Instance instance){
		List<svm_node> nodes=new ArrayList<svm_node>();
		Iterator<Feature> it=instance.featureIterator();
		while(it.hasNext()){
			Feature feature=it.next();
			nodes.add(featureToNode(feature,instance));
		}
		// sorting in ascending order is required by LIBSVM
		Collections.sort(nodes,NODE_COMPARATOR);
		return nodes.toArray(new svm_node[nodes.size()]);
	}

	/**
	 * convert the given dataset into a svm_problem object by looping
	 * through the examples and features - features are resorted numericly
	 *
	 * @param dataset - must contain features with integer names
	 * @return a fully loaded svm_problem object
	 */
	public static svm_problem convertToSVMProblem(Dataset dataset){

		// create the problem data structure
		svm_problem problem=new svm_problem();
		problem.l=dataset.size();
		problem.y=new double[problem.l];
		problem.x=new svm_node[problem.l][];

		// fill it with instance information
		Iterator<Example> it=dataset.iterator();
		for(int i=0;it.hasNext();i++){
			Example example=it.next();
			// call different label index methods depending on schema
			if(dataset.getSchema().equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA)){
				problem.y[i]=example.getLabel().numericLabel();
			}
			else{
				problem.y[i]=dataset.getSchema().getClassIndex(example.getLabel().bestClassName());
			}
			problem.x[i]=instanceToNodeArray(example);
		}

		return problem;
	}

	/**
	 * convert a svm_node to a feature
	 * 
	 * @param node svm_node from LIBSVM
	 * @param featureFactory FeatureIdFactory object holds feature and its id information.
	 * @return Feature Feature converted from svm_node
	 */
	public static Feature nodeToFeature(svm_node node,FeatureFactory featureFactory){
		// important: LIBSVM feature index starts at 1, not 0
		return featureFactory.getFeature(node.index-1);
	}

	/**
	 * creates an instance from the node array
	 * 
	 * @param nodes svm_node array from LIBSVM
	 * @param featureFactory FeatureFactory object holds feature and its id
	 * @return Instance Instance with the Features converted from input node array
	 */
	public static Instance nodeArrayToInstance(svm_node[] nodes,FeatureFactory featureFactory){
		MutableInstance instance=new MutableInstance();
		for(int i=0;i<nodes.length;i++){
			Feature feature=nodeToFeature(nodes[i],featureFactory);
			if(feature!=null){
				instance.addNumeric(feature,nodes[i].value);
			}
			else{
				return null;
			}
		}
		return instance;
	}

}
