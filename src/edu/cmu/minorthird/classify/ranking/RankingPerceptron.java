/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.ranking;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;

import java.util.*;
import java.io.*;

/**
 * A ranking method based on a voted perceptron.
 */

public class RankingPerceptron extends BatchRankingLearner
{
    private int numEpochs;

    public RankingPerceptron() 
    { 
	this(100);
    }

    public RankingPerceptron(int numEpochs)
    { 
	this.numEpochs=numEpochs;
    }

    public Classifier batchTrain(Dataset data) 
    {
	Hyperplane h = new Hyperplane();
	Hyperplane s = new Hyperplane();
	int numUpdates = 0;
	Map rankingMap = splitIntoRankings(data);
	ProgressCounter pc = new ProgressCounter("perceptron training", "epoch", numEpochs);
	for (int e=0; e<numEpochs; e++) {
	    //System.out.println("epoch "+e+"/"+numEpochs);
	    for (Iterator i=rankingMap.keySet().iterator(); i.hasNext(); ) {
		String subpop = (String)i.next();
		List ranking = (List)rankingMap.get(subpop);
		numUpdates += batchTrainSubPop( h, s, ranking );
	    }
	    pc.progress();
	}
	pc.finished();
	// turn sum hyperplane into an average
	s.multiply( 1.0/((double)numUpdates) );
	//new ViewerFrame("hyperplane", s.toGUI());
	return s;
    }

    // return the number of times h has been updated
    private int batchTrainSubPop( Hyperplane h, Hyperplane s, List ranking )
    {
	int updates = 0;
	sortByScore(h,ranking);
	boolean positiveExampleEncountered = false;
	boolean negativeExampleEncountered = false;
	int lowestBadPositiveExample = -1, highestBadNegativeExample = ranking.size();
	for (int i=0; i<ranking.size(); i++) {	
	    Example exi = (Example)ranking.get(i);
	    if (exi.getLabel().isNegative() && !positiveExampleEncountered) {
		highestBadNegativeExample = Math.min(i, highestBadNegativeExample);
	    } else if (exi.getLabel().isPositive() && negativeExampleEncountered) {
		lowestBadPositiveExample = Math.max(i, lowestBadPositiveExample);
	    }
	    if (exi.getLabel().isPositive()) positiveExampleEncountered=true;
	    if (exi.getLabel().isNegative()) negativeExampleEncountered=true;
	}
	if (lowestBadPositiveExample>=0 && highestBadNegativeExample<ranking.size()) {
	    Example neg = (Example)ranking.get(highestBadNegativeExample);		
	    Example pos = (Example)ranking.get(lowestBadPositiveExample);
	    h.increment( neg, -1.0 );
	    h.increment( pos, +1.0 );
	    s.increment( h );
	    updates++;
	}
	return updates;
    }

}
