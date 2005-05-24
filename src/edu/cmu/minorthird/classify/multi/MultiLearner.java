package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;

import java.util.*;

/**
 * ClassifierLearner for learning multiple dimensions
 *
 * @author Cameron Williams
 */

public class MultiLearner implements ClassifierLearner
{
    protected ClassifierLearnerFactory learnerFactory;
    protected BatchClassifierLearner learner;
    protected String learnerName;
    protected ArrayList innerLearner = null;
    protected MultiExampleSchema multiSchema; 

    public static class IllegalArgumentException extends Exception {
	public IllegalArgumentException(String s) { super(s); }
    }

    public MultiLearner()
    {
	this(new MaxEntLearner());
    }
    
    public MultiLearner(ClassifierLearner learner){
	this.learner = new MaxEntLearner();
	this.learnerName = learner.toString();
	
    }
    public void setSchema(ExampleSchema schema){
	System.out.println("Must use setMultSchema(MultiExampleSchema schema)");
    }

    public void setMultiSchema(MultiExampleSchema schema)
    {
	this.multiSchema = schema;
	innerLearner = new ArrayList();
	ExampleSchema[] schemas = multiSchema.getSchemas();
	for(int i=0; i<schemas.length; i++) {
	    innerLearner.add(((BatchClassifierLearner)learner).copy());	   
	    ((ClassifierLearner)(innerLearner.get(i))).setSchema(schemas[i]);
	}
    }
    public void reset()
    {
	if (innerLearner!=null) {
	    for (int i=0; i<innerLearner.size(); i++) {
		((ClassifierLearner)(innerLearner.get(i))).reset();
	    }
	}
    }
    public void setInstancePool(Instance.Looper looper) 
    {
	ArrayList list = new ArrayList();
	while (looper.hasNext()) list.add(looper.next());
	for (int i=0; i<innerLearner.size(); i++) {
	    ((ClassifierLearner)(innerLearner.get(i))).setInstancePool( new Instance.Looper(list) );
	}
    }
    public boolean hasNextQuery()
    {
	for (int i=0; i<innerLearner.size(); i++) {
	    if (((ClassifierLearner)(innerLearner.get(i))).hasNextQuery()) return true;
	}
	return false;
    }

    public Instance nextQuery()
    {
	for (int i=0; i<innerLearner.size(); i++) {
	    if (((ClassifierLearner)(innerLearner.get(i))).hasNextQuery()) return ((ClassifierLearner)innerLearner.get(i)).nextQuery();
	}
	return null;
    }

    /** Throws error, you need a multiExample */
    public void addExample(Example answeredQuery)
    {
	/*int classIndex = schema.getClassIndex( answeredQuery.getLabel().bestClassName() );
	for (int i=0; i<innerLearner.size(); i++) {
	    ClassLabel label = classIndex==i ? ClassLabel.positiveLabel(1.0) : ClassLabel.negativeLabel(-1.0);
	    ((ClassifierLearner)(innerLearner.get(i))).addExample( new Example( answeredQuery.asInstance(), label ) );
	    }*/
	System.out.println("You must add a multiExample to a multi learner");
    }

    public void addMultiExample(MultiExample answeredQuery) {
	Example[] examples = answeredQuery.getExamples();
	for (int i=0; i<innerLearner.size(); i++) {
	    ((ClassifierLearner)(innerLearner.get(i))).addExample(examples[i]);
	}
    }

    public void completeTraining()
    {
	for (int i=0; i<innerLearner.size(); i++) {
	    ((ClassifierLearner)(innerLearner.get(i))).completeTraining();
	}
    }

    /** Returns the classifier for the first dimension */
    public Classifier getClassifier() {
	if (innerLearner.get(0) == null) return null;
	return ((ClassifierLearner)(innerLearner.get(0))).getClassifier();
    }

    public MultiClassifier getMultiClassifier()
    {
	Classifier[] classifiers = new Classifier[ innerLearner.size() ];
	for (int i=0; i<innerLearner.size(); i++) {
	    classifiers[i] = ((ClassifierLearner)(innerLearner.get(i))).getClassifier();
	}
	return new MultiClassifier( classifiers );
    }

}