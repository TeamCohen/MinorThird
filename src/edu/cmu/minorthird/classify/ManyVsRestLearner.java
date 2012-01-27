/**
 * Created on May 18, 2006
 * @author Vitor R. Carvalho
 */
package edu.cmu.minorthird.classify;

import java.util.*;

/**
 * Multi-class version of a binary classifier; Generalizes OneVsAllLearner. 
 
 * OneVsAll allows one class with positive labels for each example, and the rest with negative labels. 
 * In ManyVsRest, in each example many classes may be positive. 
 * 
 * @author Vitor Carvalho.
 */

public class ManyVsRestLearner extends OneVsAllLearner{
    
    public ManyVsRestLearner() {
		super();
	}

    public ManyVsRestLearner(BatchClassifierLearner learner1) {
		super(learner1);
	}

    public ManyVsRestLearner(String learnerName) {
    	super(learnerName);
    }
    
    @Override
		public ClassifierLearner copy() {
	    ManyVsRestLearner learner = null;
		try {
		    learner =(ManyVsRestLearner)(this.clone());
		    if(innerLearner!= null) {
				learner.innerLearner.clear();
				for (int i=0; i<innerLearner.size(); i++) {
				    ClassifierLearner inner = (innerLearner.get(i));
				    learner.innerLearner.add(inner.copy());
				}
		    }
		} catch (Exception e) {
		    System.out.println("Can't CLONE ManyVsRestLearner!!");
		    e.printStackTrace();
		}
		return learner;
    }

    @Override
		public void addExample(Example answeredQuery)
    {    	
    	Set<String> possibleLabels = answeredQuery.getLabel().possibleLabels();    	
    	for (int i=0; i<innerLearner.size(); i++) {
    		boolean positive = possibleLabels.contains(schema.getClassName(i));
    		ClassLabel label = positive ? ClassLabel.positiveLabel(1.0) : ClassLabel.negativeLabel(-1.0);
    		((innerLearner.get(i))).addExample( new Example( answeredQuery.asInstance(), label ) );
    	}
    }
}
