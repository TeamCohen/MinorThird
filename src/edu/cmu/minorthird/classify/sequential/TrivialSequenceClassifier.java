/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.sequential;

import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Instance;


/**
 * Apply a classifier to each item in a sequence in turn,
 * using as history the previous predicted items.
 *
 * @author William Cohen
 */

public class TrivialSequenceClassifier implements SequenceClassifier,SequenceConstants
{
	private Classifier classifier;
	private int historySize;

	public TrivialSequenceClassifier(Classifier classifier,int historySize) 
	{ 
		this.classifier=classifier; 
		this.historySize=historySize;
	}

	@Override
	public ClassLabel[] classification(Instance[] sequence)
	{
		String[] history = new String[historySize];

		ClassLabel[] result = new ClassLabel[sequence.length];
		for (int i=0; i<result.length; i++) {
			for (int j=0; j<history.length; j++) {
				history[j] = (i-1-j)<0 ? NULL_CLASS_NAME : result[i-1-j].bestClassName();
			}
			result[i] = classifier.classification( new InstanceFromSequence(sequence[i], history) );
		}
		return result;
	}

	@Override
	public String explain(Instance[] sequence)
	{
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<sequence.length; i++) {
			buf.append("classification of element "+i+" of sequence:\n");
			buf.append(classifier.explain(sequence[i]));
			buf.append("\n");
		}
		return buf.toString();
	}

    @Override
		public Explanation getExplanation(Instance[] sequence) 
    {
	Explanation.Node top = new Explanation.Node("TrivialSequenceClassifier Explanation");
	for (int i=0; i<sequence.length; i++) {
	    Explanation.Node seq = new Explanation.Node("classification of element "+i+" of sequence:\n");
	    Explanation.Node seqEx = (classifier.getExplanation(sequence[i])).getTopNode();
	    if(seqEx == null) 
		seqEx = new Explanation.Node(classifier.explain(sequence[i]));
	    seq.add(seqEx);
	    top.add(seq);
	}
	Explanation ex = new Explanation(top);
	return ex;
    }
}

