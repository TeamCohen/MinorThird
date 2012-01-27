package edu.cmu.minorthird.classify;

import java.util.*;

/**
 * Multi-class version of a binary classifier.  Puts classifiers in order of ones with
 * the most positive examples first.
 *
 * @author Cameron Williams
 */

public class MostFrequentFirstLearner extends OneVsAllLearner{

	public String[] sortedClassNames;

	public MostFrequentFirstLearner(){
		super();
	}

//	/**
//	 * @deprecated use MostFrequentFirstLearner(BatchClassifierLearner learner)
//	 * @param learnerFactory a ClassifierLearnerFactory which should produce a BinaryClassifier with each call.
//	 */
//	public MostFrequentFirstLearner(ClassifierLearnerFactory learnerFactory){
//		super(learnerFactory);
//	}

	public MostFrequentFirstLearner(String l){
		super(l);
	}

	public MostFrequentFirstLearner(BatchClassifierLearner learner){
		this.learner=learner;
		this.learnerName=learner.toString();
		learnerFactory=new ClassifierLearnerFactory(learnerName);

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
			int maxPosEx=0;
			int learnerIndex=-1;
			//find learner with max positive examples
			for(int j=0;j<unsortedLearners.size();j++){
				try{
					BatchClassifierLearner learner=
							(unsortedLearners.get(j));
					Dataset d=learner.dataset;
					int numPosEx=0;
					for(Iterator<Example> it=d.iterator();it.hasNext();){
						Example example=it.next();
						if(example.getLabel().isPositive())
							numPosEx++;
					}
					if(numPosEx>maxPosEx){
						maxPosEx=numPosEx;
						learnerIndex=j;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}

			//add learner to sortedLearners
			ClassifierLearner learner=
					unsortedLearners.remove(learnerIndex);
			innerLearner.add(learner);

			String className=unsortedClassNames.remove(learnerIndex);
			sortedClassNames[position]=className;
			position++;
		}

	}

	@Override
	public void completeTraining(){
		for(int i=0;i<innerLearner.size();i++){
			(innerLearner.get(i)).completeTraining();
		}
		sortLearners();
	}

	@Override
	public Classifier getClassifier(){
		Classifier[] classifiers=new Classifier[innerLearner.size()];
		for(int i=0;i<innerLearner.size();i++){
			classifiers[i]=((innerLearner.get(i))).getClassifier();
		}
		return new OneVsAllClassifier(sortedClassNames,classifiers);
	}
}