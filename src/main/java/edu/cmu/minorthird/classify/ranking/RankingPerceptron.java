/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.ranking;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.algorithms.linear.Hyperplane;
import edu.cmu.minorthird.util.ProgressCounter;

/**
 * A ranking method based on a voted perceptron.
 */

public class RankingPerceptron extends BatchRankingLearner
{
	private int numEpochs;
	private static final double MARGIN = 0.1;

	public RankingPerceptron() 
	{ 
		this(100);
	}

	public RankingPerceptron(int numEpochs)
	{ 
		this.numEpochs=numEpochs;
	}

	@Override
	public Classifier batchTrain(Dataset data) 
	{
		Hyperplane h = new Hyperplane();
		Hyperplane s = new Hyperplane();
		int numUpdates = 0;
		Map<String,List<Example>> rankingMap = listsWithOneExampleEach( splitIntoRankings(data) );
		//Map rankingMap = splitIntoRankings(data);
		ProgressCounter pc = new ProgressCounter("perceptron training", "epoch", numEpochs);
		for (int e=0; e<numEpochs; e++) {
	    //System.out.println("epoch "+e+"/"+numEpochs);
	    for (Iterator<String> i=rankingMap.keySet().iterator(); i.hasNext(); ) {
				String subpop = i.next();
				List<Example> ranking = rankingMap.get(subpop);
				numUpdates += batchTrainSubPop( h, s, ranking );
	    }
	    pc.progress();
		}
		pc.finished();
		// turn sum hyperplane into an average
		s.multiply( 1.0/(numUpdates) );
		//new ViewerFrame("hyperplane", s.toGUI());
		return s;
	}

	// return the number of times h has been updated
	private int batchTrainSubPop( Hyperplane h, Hyperplane s, List<Example> ranking )
	{
		sortByScore(h,ranking);
		int updates = 0;
//		int highestNegativeIndex = ranking.size();
		Example highestNegativeExample = null;
		for (int i=0; i<ranking.size(); i++) {	
	    Example exi = ranking.get(i);
			if (exi.getLabel().isNegative()) {
//				highestNegativeIndex = i;
				highestNegativeExample = ranking.get(i);		
				break;
			}
		}
		// look for positive example, update
		for (int i=0; i<ranking.size(); i++) {	
	    Example exi = ranking.get(i);
			if (exi.getLabel().isPositive()) {			
				if (highestNegativeExample!=null && (h.score(exi) < h.score(highestNegativeExample)+MARGIN)) {
				//if (i>highestNegativeIndex) {
					// the positive example is ranked below the
					// highestNegativeExample, which is incorrect
					Example pos = ranking.get(i);
					h.increment( highestNegativeExample, -1.0);
					h.increment( pos, +1.0 );
				}
				s.increment( h );
				updates++;
			}
		}
		return updates;
	}
}
