// Some algorithms for Hidden Markov Models (Chapter 3): Viterbi,
// Forward, Backward, Baum-Welch.  We compute with log probabilities.

// Notational conventions: 

// i     = 1,...,L           indexes x, the observed string, x_0 not a symbol
// k,ell = 0,...,hmm.nstate-1  indexes hmm.state(k)   a_0 is the start state

//Zhenzhen Kou

package edu.cmu.minorthird.classify.sequential;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ExampleSchema;


public class HMMLearner implements BatchSequenceClassifierLearner,SequenceConstants{

  @Override
	public int getHistorySize() { return 1; }
  protected int historySize;
  protected String[] history;
  int numClasses;
  HMM hmmModel;

/*
  // some key factors in a HMM, these can be obtained in Batchtrain via parsing the dataset
  // state = array of state names (except initial state), nstate: number of states
  // amat  = matrix of transition probabilities (except initial state)
  // esym  = string of emission names, nesym: number of emissions
  // emat  = matrix of emission probabilities
*/



  public HMMLearner(int historySize) {
    this.historySize = historySize;
    this.history = new String[historySize];
  }
  
  public HMMLearner(){
  	this(0);
  }


	protected static Logger log = Logger.getLogger(CollinsPerceptronLearner.class);
	protected static final boolean DEBUG = log.isDebugEnabled();

	protected int numberOfEpochs;

	public int getNumberOfEpochs() { return numberOfEpochs; }
	public void setNumberOfEpochs(int newNumberOfEpochs) { this.numberOfEpochs = newNumberOfEpochs; }



	@Override
	public void setSchema(ExampleSchema schema)	{	;	}

	@Override
	public SequenceClassifier batchTrain(SequenceDataset dataset)
	{
System.out.println("\nbatch train is called\n");
		ExampleSchema schema = dataset.getSchema();
		this.numClasses = schema.getNumberOfClasses();

//// so here when you call the 		MultiClassHMMClassifier, it's like return MultiClassHMMClassifier( dataset)
		
		MultiClassHMMClassifier hmm = new MultiClassHMMClassifier( dataset)	;

				
		 hmm.baumwelch(0.00001);
		 // hmm.hmmModel.print(new SystemOut());

		return hmm;
	}

}

