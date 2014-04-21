package edu.cmu.minorthird.classify.algorithms.active;

import edu.cmu.minorthird.classify.*;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * Routines for forming and working with committees.
 *
 * @author William Cohen
 */

public class CommitteeLearner 
{
	static private Logger log = Logger.getLogger(CommitteeLearner.class);

	private BatchClassifierLearner learner;
	private int committeeSize; 

	public CommitteeLearner(BatchClassifierLearner learner,int committeeSize)
	{
		this.learner = learner;
		this.committeeSize = committeeSize;
	}

	/** Learn a committee */
	public Classifier[] batchTrainCommittee(RandomAccessDataset data)
	{
		// build the committee
		Classifier[] committee = new Classifier[committeeSize]; 
			
		Random rand = new Random(0);
		for (int i=0; i<committee.length; i++) {
			Dataset ithBag = bag(rand,data); 
			log.info("training committee member on "+ithBag.size()+" examples");
			committee[i] = learner.batchTrain( ithBag );
			log.info("committee member #"+i+":\n"+committee[i]);
		}
		return committee;
	}
	
	private Dataset bag(Random r,RandomAccessDataset data)
	{
		Dataset result = new BasicDataset(); 
		for (int i=0; i<data.size(); i++) {
			int k = (int) (r.nextDouble()*data.size());
			log.debug("bag: add example #"+k+": "+data.getExample(k));
			result.add( data.getExample(k) );
		}
		return result;
	}
}
