package edu.cmu.minorthird.classify.algorithms.svm;

import edu.cmu.minorthird.classify.FeatureIdFactory;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.MutableInstance;
import libsvm.svm;
import libsvm.svm_node;
import libsvm.svm_model;
import libsvm.svm_problem;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides some basic utilities for dealing with libsvm.
 * It can convert Features to nodes, instances to node arrays and Datasets to problems.
 *
 * @author ksteppe
 */
public class SVMUtils
{
    private static Logger log = Logger.getLogger(SVMUtils.class);

    /**
     * convert the given dataset into a svm_problem object by looping
     * through the examples and features - features are resorted numericly
     *
     * @param dataset - must contain features with integer names
     * @return a fully loaded svm_problem object
     */
    protected static svm_problem 
    convertToMultiClassSVMProblem(Dataset           dataset, 
    		                      FeatureIdFactory  idFactory, 
    		                      ExampleSchema     schema)
    {
        //count number for length                                                                                                                                                                                                                                     
        svm_problem problem = new svm_problem();

        Example.Looper it = dataset.iterator();
        problem.l = it.estimatedSize();
        problem.y = new double[problem.l];
        problem.x = new svm_node[problem.l][];

        for (int i = 0; it.hasNext(); i++) {
        	Example example = it.nextExample();
        	problem.y[i] = schema.getClassIndex(example.getLabel().bestClassName());
        	problem.x[i] = instanceToNodeArray(example, idFactory);
        }
        return problem;
    }
    
    /**
     * convert the given dataset into a svm_problem object by looping
     * through the examples and features - features are resorted numericly
     *
     * @param dataset - must contain features with integer names
     * @return a fully loaded svm_problem object
     */
    protected static svm_problem 
    convertToSVMProblem(Dataset            dataset, 
    		            FeatureIdFactory   idFactory)
    {
        svm_problem problem = new svm_problem();
        Example.Looper it = dataset.iterator();

        problem.l = it.estimatedSize();
        problem.y = new double[problem.l];
        problem.x = new svm_node[problem.l][];

        for (int i = 0; it.hasNext(); i++) {

            Example example = it.nextExample();
            problem.y[i] = example.getLabel().numericLabel();
            problem.x[i] = instanceToNodeArray(example, idFactory);
        }
        
        return problem;
    }
    
    /**
     * converts the feature into an svm_node
     *
     * @param f        Feature to convert into a node
     * @param instance Instance feature is in - used to retrieve the weight of the feature
     * @return svm_node
     */
    protected static svm_node 
    featureToNode(Feature            f, 
    		      Instance           instance, 
                  FeatureIdFactory   idFactory)
    {
        svm_node svm_node = new svm_node();
        svm_node.index = idFactory.getID(f);
        svm_node.value = instance.getWeight(f);
        return svm_node;
    }
    
    /**
     * creates the node array from an instance
     *
     * @param instance Instance to convert
     * @return node array with all the features from the instance
     */
    protected static svm_node[] 
    instanceToNodeArray(Instance           instance, 
    		            FeatureIdFactory   idFactory)
    {
        Feature.Looper fLoop = instance.featureIterator();

        svm_node[] nodeArray;
        List nodeList = new ArrayList();
        while (fLoop.hasNext())
        {
            Feature f = fLoop.nextFeature();
            nodeList.add(featureToNode(f, instance, idFactory));
        }
        
        Collections.sort(nodeList, NODE_COMPARATOR);
        nodeArray = (svm_node[]) nodeList.toArray(new svm_node[0]);

        return nodeArray;
    }

    /**
     * creates an instance from the node array
     * 
     * @param   svmNodesInput  svm_node array from LIBSVM
     * @param   idFactory      FeatureIdFactory object holds feature and it's id information
     * 
     * @return  Instance       Instance with the Features converted from input node array
     * 
     */
    protected static Instance 
    nodeArrayToInstance(svm_node[]        svmNodesInput,
	                FeatureIdFactory  idFactory)
    {
	// convert node array into Feature array
	Feature[] fTemp = new Feature[svmNodesInput.length];
		
	for (int index = 0; index < svmNodesInput.length; ++index) {
			
	    fTemp[index] = nodeToFeature(svmNodesInput[index], idFactory);//convert svm_node to Feature
			
	}
		
	//Generate instance from Feature array
	MutableInstance instanceTemp = new MutableInstance();
		
	for (int index = 0; index < svmNodesInput.length; ++index) {
			
	    if (fTemp[index] == null) {
	       
		log.debug("Unable to create an instance because of svm_node id = " + svmNodesInput[index].index);

		return null;
	    }
			
	    instanceTemp.addNumeric(fTemp[index], svmNodesInput[index].value);
			
	}

	return instanceTemp;
    }

    /**
     * convert the svm_node to the feature
     * 
     * @param  svmNodeInput  svm_node from LIBSVM
     * @param  idFactory     FeatureIdFactory object holds feature and it's id information.
     * 
     * @return Feature       Feature converted from svm_node
     * 
     */
    protected static Feature
    nodeToFeature(svm_node          svmNodeInput,
		  FeatureIdFactory  idFactory)
    {		
	return idFactory.getFeature(svmNodeInput.index);
    }
    
    /**
     * prints the given svm_problem to string format
     * - if I fixed the line starters, and input a Writer, I could use this
     * to save svm_problem objects too
     *
     * @param problem svm_problem
     */
    protected static void 
    outputProblem(svm_problem problem)
    {
        log.debug("size: " + problem.l);
        for (int i = 0; i < problem.l; i++)
        {
            String data = problem.y[i] + " ";
            for (int j = 0; j < problem.x[i].length; j++)
            {
                data += problem.x[i][j].index + ":" + problem.x[i][j].value + " ";
            }

            log.debug("example: " + data);

        }
    }
    
    private static Comparator NODE_COMPARATOR = new Comparator()
    {
        public int compare(Object o1, Object o2)
        {
            svm_node n1 = (svm_node) o1;
            svm_node n2 = (svm_node) o2;

            return n1.index - n2.index;
        }

    };

}
