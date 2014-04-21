package edu.cmu.minorthird.classify.sequential;

import java.util.Iterator;
import java.util.Vector;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.ProgressCounter;

/**
 * Sequential learner based on the perceptron algorithm that takes the
 * top-k viterbi paths and subtracts those within a margin of beta of
 * the correct.
 *
 * @author Sunita Sarawagi */

public class MarginPerceptronLearner extends CollinsPerceptronLearner
{
	float beta = (float)0.05;
	int topK = 10;
	public MarginPerceptronLearner()
	{
		this(3,5,(float)0.05);
	}
	public MarginPerceptronLearner(int numberOfEpochs)
	{
		this(3,numberOfEpochs,(float)0.05);
	}
	public MarginPerceptronLearner(int historySize,int numberOfEpochs, float beta) {
		this(historySize, numberOfEpochs,beta,10);
	}
	public MarginPerceptronLearner(int historySize,int numberOfEpochs, float beta, int topK)
	{
		super(historySize, numberOfEpochs);
		this.beta = beta;
		this.topK = topK;
	}
	@Override
	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
		ExampleSchema schema = dataset.getSchema();
		MultiClassVPClassifier c = new MultiClassVPClassifier(schema);
		
		ProgressCounter pc =
	    new ProgressCounter("training sequence perceptron","sequence",getNumberOfEpochs()*dataset.numberOfSequences());
		
		Vector<ClassLabel[]> viterbiS = new Vector<ClassLabel[]>();
		for (int epoch=0; epoch<getNumberOfEpochs(); epoch++) 
		{
			// statistics for curious researchers
			int sequenceErrors = 0;
			int transitionErrors = 0;
			int transitions = 0;
			
			for (Iterator<Example[]> i=dataset.sequenceIterator(); i.hasNext(); ) 
			{
				Example[] sequence = i.next();
				BeamSearcher beam = new BeamSearcher(c,getHistorySize(),schema);
				beam.doSearch(sequence);			
				
				float corrScore = getScore(sequence, c);
				if (DEBUG) log.debug("corrScore: " + corrScore);
				viterbiS.clear();
				int maxNum = Math.min(beam.getNumberOfSolutionsFound(),topK);
				for (int k = 0; k < maxNum; k++) {
			    ClassLabel[] viterbi = beam.viterbi(k);
			    float thisScore = beam.score(k);
			    if (DEBUG) log.debug("viterbi: "+k + " score " + thisScore);
			    if (DEBUG) log.debug(sequenceToString(viterbi));
			    if (thisScore < corrScore*(1-beta))
						break;
			    if (!isCorrect(viterbi,sequence)) {
						viterbiS.add(viterbi);
			    }
					
				}
				if (DEBUG) log.debug("added: " + viterbiS.size());
				boolean errorOnThisSequence=false;
				if (viterbiS.size() > 0) {
					for (int j=0; j<sequence.length; j++) {
						boolean differenceAtJ = false;
						for (int s = 0; s < viterbiS.size(); s++) {
							ClassLabel[] viterbi = viterbiS.elementAt(s);
							differenceAtJ = !viterbi[j].isCorrect( sequence[j].getLabel() );
							for (int k=1; j-k>=0 && !differenceAtJ && k<=getHistorySize(); k++) {
								if (!viterbi[j-k].isCorrect( sequence[j-k].getLabel() )) {
									differenceAtJ = true;
								}
							}
							if (differenceAtJ) break;
						}
						
						if (differenceAtJ) { // i.e., if phi(sequence,j) != phi(viterbi,j)
							transitionErrors++;
							errorOnThisSequence=true;
							InstanceFromSequence.fillHistory( history, sequence, j );
							Instance correctXj = new InstanceFromSequence( sequence[j], history );
							c.update( sequence[j].getLabel().bestClassName(), correctXj, 1.0 ); 
							for (int s = 0; s < viterbiS.size(); s++) {
								ClassLabel[] viterbi = viterbiS.elementAt(s);
								InstanceFromSequence.fillHistory( history, viterbi, j );
								Instance wrongXj = new InstanceFromSequence( sequence[j], history );
								c.update( viterbi[j].bestClassName(), wrongXj, -1.0/viterbiS.size()); 
							}
						}
					} // example sequence j
				}
				// for voted perceptron needs this...
				c.completeUpdate(); 
				
				if (errorOnThisSequence) sequenceErrors++;
				transitions += sequence.length;
				
				pc.progress();
			} // sequence i
			
			System.out.println("Epoch "+epoch+": sequenceErr="+sequenceErrors
												 +" transitionErrors="+transitionErrors+"/"+transitions);
			
			if (transitionErrors==0) break;
			
		} // epoch
		pc.finished();
		c.setVoteMode(true);
		// we can use a CMM here, since the classifier is constructed to the same
		// beam search will work
		return new CMM(c, getHistorySize(), schema );
	}
	float getScore(Example[] sequence, Classifier classifier) {
		float score = 0;
		for (int j=0; j<sequence.length; j++) {
	    InstanceFromSequence.fillHistory( history, sequence, j );
	    Instance correctXj = new InstanceFromSequence( sequence[j], history );
	    score += classifier.classification(correctXj).getWeight(sequence[j].getLabel().bestClassName());
		}
		return score;
	}
	boolean isCorrect(ClassLabel[] viterbi, Example[] sequence) {
		for (int j=0; j<sequence.length; j++) {
	    if (!viterbi[j].isCorrect( sequence[j].getLabel()))
				return false;
		}
		return true;
	}
	String sequenceToString(ClassLabel[] viterbi) {
		String path="";
		for (int j=0; j<viterbi.length; j++) {
	    path += (viterbi[j].bestClassName() + " ");
		}
		return path;
	}
}

