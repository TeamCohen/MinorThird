package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.classify.*;
import org.apache.log4j.Logger;

/** Some sample inputs to facilitate testing.
 *
 * @author William Cohen
 */

public class SampleClassificationProblem
{
	static private Logger log = Logger.getLogger(SampleClassificationProblem.class);

	static private String[] posTrain = SampleDatasets.posTrain;
	static private String[] negTrain = SampleDatasets.negTrain;
	static private String[] posTest = SampleDatasets.posTest;
	static private String[] negTest = SampleDatasets.negTest;
	
	static public TextLabels trainLabels()
	{
		return makeLabels( SampleDatasets.posTrain, SampleDatasets.negTest );
	}
	static public TextLabels testLabels()
	{
		return makeLabels( SampleDatasets.posTest, SampleDatasets.negTest );
	}

	static private TextLabels makeLabels(String[] pos, String[] neg)
	{
		BasicTextBase base = new BasicTextBase();
		for (int i=0; i<pos.length; i++) {
			base.loadDocument("pos."+i, pos[i]);
		}
		for (int i=0; i<neg.length; i++) {
			base.loadDocument("neg."+i, neg[i]);
		}
		BasicTextLabels labels = new BasicTextLabels(base);
		for (Span.Looper i=base.documentSpanIterator(); i.hasNext(); ) {
			Span s = i.nextSpan();
			if (s.getDocumentId().startsWith("pos")) {
				labels.addToType(s, "fun");
			}
		}
		new TextLabelsLoader().closeLabels(labels, TextLabelsLoader.CLOSE_ALL_TYPES);
		return labels;
	}
}
