package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.sequential.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.io.Serializable;
import java.util.*;

/**
 * Maximum entropy learner.  Based on calling the CRFLearner with
 * sequences of length 1.
 *
 * @author William cohen
 */

public class MaxEntLearner extends BatchClassifierLearner
{
	private CRFLearner crfLearner;
	public MaxEntLearner(){	crfLearner = new CRFLearner(); }
	public MaxEntLearner(String args) {	crfLearner = new CRFLearner(args); }
	public MaxEntLearner(String args[]) { crfLearner = new CRFLearner(args); }
	public void setSchema(ExampleSchema schema) { crfLearner.setSchema(schema); }
	
	public Classifier batchTrain(Dataset dataset)
	{
		SequenceDataset seqData = new SequenceDataset();
		for (Example.Looper i=dataset.iterator(); i.hasNext(); ) {
			Example e = i.nextExample();
			seqData.addSequence( new Example[]{e} );
		}
		SequenceClassifier c = crfLearner.batchTrain(seqData);
		return new MyClassifier(c);
	}
	
	public static class MyClassifier implements Classifier,Serializable
	{
		static private int serialVersionUID = 1;
		private final int CURRENT_SERIAL_VERSION = 1;

		private SequenceClassifier c;

		public MyClassifier(SequenceClassifier c) {	this.c = c;	}
		public ClassLabel classification(Instance instance) {
			ClassLabel[] labels = c.classification(new Instance[]{instance});
			return labels[0];
		}
		public String explain(Instance instance) {
			return c.explain(new Instance[]{instance});
		}
	}
}
