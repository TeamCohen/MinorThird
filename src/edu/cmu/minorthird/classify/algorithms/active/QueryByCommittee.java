package edu.cmu.minorthird.classify.algorithms.active;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.trees.DecisionTreeLearner;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;

/**
 * Implements the query-by-committee algorithm, using bagging to
 * generate a committee.
 *
 * @author William Cohen
 */

//
// easily extends to k-class case
//

public class QueryByCommittee implements ClassifierLearner
{
	static private Logger log = Logger.getLogger(QueryByCommittee.class);

	private ClassifierLearner innerLearner;
	/** Min number of labels to passively accept before starting to actively query */
	private int minLabelsBeforeQuerying = 5;
	

	private CommitteeLearner committeeLearner;
	private ExampleSchema schema;

	// need better data structure that supports easy removal,
	// perhaps a treemap with random indices.
	private TreeMap unlabeled;
	private RandomAccessDataset labeled;

	public QueryByCommittee()
	{
		this(new DecisionTreeLearner(), 5);
	}

	public QueryByCommittee(BatchClassifierLearner learner,int committeeSize) 
	{
		this.committeeLearner = new CommitteeLearner(learner,committeeSize);
		this.innerLearner = learner;
		reset();
	}

	final public void reset()
	{
		unlabeled = new TreeMap();
		labeled = new RandomAccessDataset();
		innerLearner.reset();
	}

	final public void setSchema(ExampleSchema schema)
	{
		this.schema = schema;
	}

	//
	// active learning code
	//

	final public void setInstancePool(Instance.Looper i) 
	{  
		unlabeled.clear();
		Random r = new Random(0);
		while (i.hasNext()) {
			unlabeled.put(new Double(r.nextDouble()), i.nextInstance());
		}
		log.info(unlabeled.size()+" unlabeled examples available");
	}

	final public boolean hasNextQuery() 
	{ 
		return unlabeled.size()>0;
	}

	final public Instance nextQuery() 
	{ 
		Object key = null;
		if (labeled.size()<minLabelsBeforeQuerying) {
			log.info("will pick next unlabeled example");
			key = unlabeled.firstKey();
		} else {
			log.info("will use committee to pick an unlabeled example");
			Classifier[] committee = committeeLearner.batchTrainCommittee(labeled);
			key = keyOfBestUnlabeledInstance( committee );
		}
		Instance query = (Instance)unlabeled.get(key);
		unlabeled.remove(key);
		return query;
	}

	private Object keyOfBestUnlabeledInstance( Classifier[] committee )
	{
			// find the unlabeled example with the highest level of disagreement
			double worstAgreement = 2.0;
			Object queryKey = null;
			for (Iterator i=unlabeled.keySet().iterator(); i.hasNext(); ) {
				Object key = i.next();
				Instance instance = (Instance)unlabeled.get(key);
				TObjectDoubleHashMap counts = new TObjectDoubleHashMap();
				double biggestCount=0;
				for (int j=0; j<committee.length; j++) {
					String best = committee[j].classification(instance).bestClassName();
					double c = counts.get(best) + 1;
					counts.put(best, c);
					if (c>biggestCount) biggestCount = c;
				}
				double agreement = biggestCount/committee.length;
				log.info("instance: "+instance+" committee: "+counts+" agreement: "+agreement);
				if (agreement<worstAgreement) {
					worstAgreement = agreement;
					queryKey = key;
					log.debug(" ==> best");
				}
			}
			log.info("queryInstance is: "+unlabeled.get(queryKey));
			return queryKey;
	}

	public void addExample(Example example)
	{
		log.info("adding example: "+example);
		labeled.add(example);
		innerLearner.addExample(example);
	}

	final public void completeTraining() 
	{ 
		innerLearner.completeTraining();
	}

	//
	// get trained classifier
	//

	public Classifier getClassifier() 
	{
		return innerLearner.getClassifier();
	}	
}
