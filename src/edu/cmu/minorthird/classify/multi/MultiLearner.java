package edu.cmu.minorthird.classify.multi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.ClassifierLearnerFactory;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.algorithms.linear.MaxEntLearner;

/**
 * ClassifierLearner for learning multiple dimensions
 * 
 * @author Cameron Williams
 */

public class MultiLearner implements ClassifierLearner{

	protected ClassifierLearnerFactory learnerFactory;

	protected ClassifierLearner learner;

	protected String learnerName;

	protected List<ClassifierLearner> innerLearner;

	protected MultiExampleSchema multiSchema;

	public MultiLearner(ClassifierLearner learner){
		this.learner=learner;
		this.learnerName=learner.toString();
	}
	
	public MultiLearner(){
		this(new MaxEntLearner());
	}

	@Override
	public ClassifierLearner copy(){
		MultiLearner learner=null;
		try{
			learner=(MultiLearner)this.clone();
			for(int i=0;i<innerLearner.size();i++){
				ClassifierLearner inner=innerLearner.get(i);
				learner.innerLearner.add(inner.copy());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return learner;
	}

	@Override
	public void setSchema(ExampleSchema schema){
		System.err.println("Must use setMultiSchema(MultiExampleSchema schema)");
	}

	@Override
	public ExampleSchema getSchema(){
		return null;
	}

	// Strange. Looks like all it does is copying the same learner during setting schema. - frank
	public void setMultiSchema(MultiExampleSchema schema){
		this.multiSchema=schema;
		innerLearner=new ArrayList<ClassifierLearner>();
		ExampleSchema[] schemas=multiSchema.getSchemas();
		for(int i=0;i<schemas.length;i++){
			innerLearner.add(learner.copy());
			innerLearner.get(i).setSchema(schemas[i]);
		}
	}
	
	public MultiExampleSchema getMultiSchema(){
		return multiSchema;
	}

	@Override
	public void reset(){
		if(innerLearner!=null){
			for(int i=0;i<innerLearner.size();i++){
				((innerLearner.get(i))).reset();
			}
		}
	}

	@Override
	public void setInstancePool(Iterator<Instance> it){
		List<Instance> list=new ArrayList<Instance>();
		while(it.hasNext())
			list.add(it.next());
		for(int i=0;i<innerLearner.size();i++){
			innerLearner.get(i).setInstancePool(list.iterator());
		}
	}

	@Override
	public boolean hasNextQuery(){
		for(int i=0;i<innerLearner.size();i++){
			if(innerLearner.get(i).hasNextQuery()){
				return true;
			}
		}
		return false;
	}

	@Override
	public Instance nextQuery(){
		for(int i=0;i<innerLearner.size();i++){
			if(innerLearner.get(i).hasNextQuery()){
				return innerLearner.get(i).nextQuery();
			}
		}
		return null;
	}

	@Override
	public void addExample(Example answeredQuery){
		System.err.println("Must use addMultiExample(MultiExample answeredQuery)");
	}

	public void addMultiExample(MultiExample answeredQuery){
		Example[] examples=answeredQuery.getExamples();
		for(int i=0;i<innerLearner.size();i++){
			innerLearner.get(i).addExample(examples[i]);
		}
	}

	@Override
	public void completeTraining(){
		for(int i=0;i<innerLearner.size();i++){
			innerLearner.get(i).completeTraining();
		}
	}

	/** Returns the classifier for the first dimension */
	@Override
	public Classifier getClassifier(){
		if(innerLearner.get(0)==null){
			return null;
		}
		else{
			return innerLearner.get(0).getClassifier();
		}
	}

	public MultiClassifier getMultiClassifier(){
		Classifier[] classifiers=new Classifier[innerLearner.size()];
		for(int i=0;i<innerLearner.size();i++){
			classifiers[i]=innerLearner.get(i).getClassifier();
		}
		return new MultiClassifier(classifiers);
	}

}