package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.experiments.*;

import java.util.*;

/**
 * Multi-class version of a binary classifier.  Puts classifiers in order of ones with
 * the most positive examples first.
 *
 * @author Cameron Williams
 */

public class CascadingBinaryLearner extends OneVsAllLearner{

	public String[] sortedClassNames;

	private List<Dataset> data=null;
	private List<Evaluation> eval=null;

	public CascadingBinaryLearner(){
		super();
	}

//	/** 
//	 * @deprecated use CascadingBinaryLearner(BatchClassifierLearner learner)
//	 * @param learnerFactory a ClassifierLearnerFactory which should produce a BinaryClassifier with each call.
//	 */
//	public CascadingBinaryLearner(ClassifierLearnerFactory learnerFactory){
//		super(learnerFactory);
//	}

	public CascadingBinaryLearner(String l){
		super(l);
	}

	public CascadingBinaryLearner(BatchClassifierLearner learner){
		this.learner=learner;
		this.learnerName=learner.toString();
		learnerFactory=new ClassifierLearnerFactory(learnerName);
	}

	@Override
	public void setSchema(ExampleSchema schema){
		this.schema=schema;
		innerLearner=new ArrayList<ClassifierLearner>();
		data=new ArrayList<Dataset>();
		//for (int i=0; i<innerLearner.size(); i++) {
		for(int i=0;i<schema.getNumberOfClasses();i++){
			innerLearner.add(learner.copy());
			innerLearner.get(i).setSchema(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
			data.add(new BasicDataset());
		}
	}

	private void createRankings(){
		// why 9?
		Splitter<Example> splitter=new CrossValSplitter<Example>(9);
		eval=new ArrayList<Evaluation>();
		for(int i=0;i<innerLearner.size();i++){
			Evaluation evaluation=Tester.evaluate(innerLearner.get(i),data.get(i),splitter);
			eval.add(evaluation);
		}
	}

	private void sortLearners(){
		List<BatchClassifierLearner> unsortedLearners=new ArrayList<BatchClassifierLearner>();
		String[] classNames=schema.validClassNames();
		List<String> unsortedClassNames=new ArrayList<String>();
		sortedClassNames=new String[schema.getNumberOfClasses()];
		for(int i=0;i<innerLearner.size();i++){
			unsortedLearners.add((BatchClassifierLearner)innerLearner.get(i));
			unsortedClassNames.add(classNames[i]);
		}

		//clear list so that it can be reconstructed in sorted order
		innerLearner.clear();

		int position=0;
		while(!unsortedLearners.isEmpty()){
			double maxKappa=-10.0;
			int learnerIndex=-1;
			//find learner with max positive examples
			for(int j=0;j<unsortedLearners.size();j++){
				try{
					//BatchClassifierLearner learner = ((BatchClassifierLearner)unsortedLearners.get(j));
					Evaluation evaluation=eval.get(j);
					double kappa=evaluation.kappa();
					if(kappa>=maxKappa){
						maxKappa=kappa;
						learnerIndex=j;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}

			//add learner to sortedLearners
			ClassifierLearner learner=unsortedLearners.remove(learnerIndex);
			innerLearner.add(learner);

			String className=unsortedClassNames.remove(learnerIndex);
			sortedClassNames[position]=className;
			position++;
		}

	}

	@Override
	public void addExample(Example answeredQuery){
		int classIndex=schema.getClassIndex(answeredQuery.getLabel().bestClassName());
		for(int i=0;i<innerLearner.size();i++){
			ClassLabel label=classIndex==i?ClassLabel.positiveLabel(1.0):ClassLabel.negativeLabel(-1.0);
			Example example=new Example(answeredQuery.asInstance(),label);
			innerLearner.get(i).addExample(example);
			data.get(i).add(example);
		}
	}

	@Override
	public void completeTraining(){
		for(int i=0;i<innerLearner.size();i++){
			innerLearner.get(i).completeTraining();
		}
		createRankings();
		sortLearners();
	}

	@Override
	public Classifier getClassifier(){
		Classifier[] classifiers=new Classifier[innerLearner.size()];
		for(int i=0;i<innerLearner.size();i++){
			classifiers[i]=innerLearner.get(i).getClassifier();
		}
		return new OneVsAllClassifier(sortedClassNames,classifiers);
	}
}