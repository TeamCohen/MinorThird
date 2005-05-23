package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.algorithms.linear.*;

import java.util.*;

/**
 * Multi-class version of a binary classifier.  Puts classifiers in order of ones with
 * the most positive examples first.
 *
 * @author Cameron Williams
 */

public class MostFrequentFirstLearner extends OneVsAllLearner
{

    public String[] sortedClassNames; 

    public MostFrequentFirstLearner()
    {
	super();
    }

    /**
     * @deprecated use MostFrequentFirstLearner(BatchClassifierLearner learner)
     * @param learnerFactory a ClassifierLearnerFactory which should produce a BinaryClassifier with each call.
     */
    public MostFrequentFirstLearner(ClassifierLearnerFactory learnerFactory)
    {
	super(learnerFactory);
    }
    public MostFrequentFirstLearner(String l) {
	super(l);	
    }
    public MostFrequentFirstLearner(BatchClassifierLearner learner) {
	this.learner = learner;
	this.learnerName = learner.toString();
	learnerFactory = new ClassifierLearnerFactory(learnerName);
	
    }

    private void sortLearners() {
	ArrayList unsortedLearners = new ArrayList();
	String[] classNames = schema.validClassNames();
	ArrayList unsortedClassNames = new ArrayList();
	sortedClassNames = new String[schema.getNumberOfClasses()];
	for (int i=0; i<innerLearner.size(); i++) {
	    unsortedLearners.add((BatchClassifierLearner)innerLearner.get(i));
	    unsortedClassNames.add(classNames[i]);
	}

	//clear list so that it can be reconstructed in sorted order
	innerLearner.clear();

	int position = 0;
	while(!unsortedLearners.isEmpty()) {
	    int maxPosEx = 0;
	    int learnerIndex = -1;
	    //find learner with max positive examples
	    for(int j=0; j<unsortedLearners.size(); j++) {
		try {
		    BatchClassifierLearner learner = ((BatchClassifierLearner)unsortedLearners.get(j));
		    Dataset d = learner.dataset;
		    int numPosEx = d.getNumPosExamples();
		    if(numPosEx>maxPosEx) {
			maxPosEx = numPosEx;
			learnerIndex = j;
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	    
	    //add learner to sortedLearners
	    ClassifierLearner learner = (ClassifierLearner)unsortedLearners.remove(learnerIndex);
	    innerLearner.add(learner);

	    String className = (String)unsortedClassNames.remove(learnerIndex);
	    sortedClassNames[position] = className;
	    position++;
	}
       
    }

    public void completeTraining()
    {
	for (int i=0; i<innerLearner.size(); i++) {
	    ((ClassifierLearner)innerLearner.get(i)).completeTraining();
	}
	sortLearners();
    }

    public Classifier getClassifier()
    {
	Classifier[] classifiers = new Classifier[ innerLearner.size() ];
	for (int i=0; i<innerLearner.size(); i++) {
	    classifiers[i] = ((ClassifierLearner)(innerLearner.get(i))).getClassifier();
	}
	return new OneVsAllClassifier( sortedClassNames, classifiers );
    }
}