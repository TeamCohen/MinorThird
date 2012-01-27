package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.*;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.*;

/**
 * Naive Bayes algorithm.  If feature weights are word counts then this 
 * implements the usual multinomial naive Bayes.  If feature weights
 * are presence/absence this implements binomial naive Bayes.
 *
 * @author William Cohen
 */

/*
 * classify with maximum value of of Pr(class|instance)
 *
 * Pr(class|instance) = Pr(instance|class)*Pr(class)
 * = log Pr(instance|class)*Pr(class)
 * = log prod_f Pr(f|class)*Pr(class)
 * = sum_f log Pr(f|class) + log Pr(class)
 *
 * score is log odds = log( Pr(+|instance) / Pr(-|instance) )
 *  = sum_f log [Pr(f|+) - Pr(f|-)] + log Pr(+) - log Pr(-i)
 */

public class NaiveBayes extends OnlineBinaryClassifierLearner implements
		Serializable{
	
	static final long serialVersionUID=20080130L;

	private static Logger log=Logger.getLogger(NaiveBayes.class);

	private Hyperplane numGivenPos,numGivenNeg;

	private Set<Feature> featureSet;

	private double numPos,numNeg;

	public NaiveBayes(){
		super();
		reset();
	}

	@Override
	public void reset(){
		log.info("resetting NaiveBayes");
		numGivenPos=new Hyperplane();
		numGivenNeg=new Hyperplane();
		featureSet=new HashSet<Feature>();
		numPos=0;
		numNeg=0;
	}

	@Override
	public void addExample(Example example){
		boolean isPos=example.getLabel().isPositive();
		if(isPos)
			numPos+=example.getWeight();
		else
			numNeg+=example.getWeight();
		for(Iterator<Feature> i=example.featureIterator();i.hasNext();){
			Feature f=i.next();
			if(isPos)
				numGivenPos.increment(f,1);
			else
				numGivenNeg.increment(f,1);
			featureSet.add(f);
		}
	}

	@Override
	public Classifier getClassifier(){
		Hyperplane c=new Hyperplane();
		for(Iterator<Feature> i=featureSet.iterator();i.hasNext();){
			Feature f=i.next();
			double featurePrior=getFeaturePrior();
			double m=getFeaturePriorPseudoCount();
			double pweight=
					estimatedLogProb(numGivenPos.featureScore(f),numPos,featurePrior,m);
			double nweight=
					estimatedLogProb(numGivenNeg.featureScore(f),numNeg,featurePrior,m);
			c.increment(f,pweight-nweight);
		}
		c.incrementBias(+estimatedLogProb(numPos,numPos+numNeg,0.5,1.0));
		c.incrementBias(-estimatedLogProb(numNeg,numPos+numNeg,0.5,1.0));
		// note these weights are multiplied by feature values when classifying,
		// which gives the multinomial Naive Bayes implementation.
		return c;
	}

	private double estimatedLogProb(double k,double n,double prior,
			double pseudoCounts){
		return Math.log((k+prior*pseudoCounts)/(n+pseudoCounts));
	}

	private double getFeaturePrior(){
		return 1.0/featureSet.size();
	}

	private double getFeaturePriorPseudoCount(){
		return 1.0;
	}

	@Override
	public String toString(){
		return "[NaiveBayes]";
	}
}
