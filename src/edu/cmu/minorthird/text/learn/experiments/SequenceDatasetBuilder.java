/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.learn.experiments;

import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.util.gui.ViewerFrame;

import java.io.File;

/** Convert a TextEnv to a SequenceDataset.
 *
 * @author William Cohen
*/

public class SequenceDatasetBuilder
{
	static public final SpanFeatureExtractor DEFAULT_FE =
	new SpanFeatureExtractor() {
			public Instance extractInstance(Span s) {
				return extractInstance(new EmptyEnv(), s);
			}
			public Instance extractInstance(TextEnv env,Span s) {
				FeatureBuffer buf = new FeatureBuffer(env,s);
				SpanFE.from(s,buf).tokens().eq().lc().emit();
				SpanFE.from(s,buf).tokens().eq().tr("[A-Z]+","X").tr("[a-z]+","x").tr("[0-9]+","0").emit();
				SpanFE.from(s,buf).left().token(-1).eq().lc().emit();
				SpanFE.from(s,buf).right().token(0).eq().lc().emit();
				return buf.getInstance();
			}
		};

	static public SequenceDataset build(TextEnv env, String userlabelType, SpanFeatureExtractor fe, int historySize)
	{
		AnnotatorTeacher teacher = new TextEnvAnnotatorTeacher(env,userlabelType);
		SequenceAnnotatorLearner learner = new SequenceAnnotatorLearner(fe,historySize) {
				public Annotator getAnnotator() { return null; }
			};
		teacher.train(learner);
		return learner.getSequenceDataset();
	}

	public static void main(String[] args) 
	{
		try {
			//TextEnv env = SampleExtractionProblem.trainEnv();
			TextEnv env = FancyLoader.loadTextEnv(args[0]);
			SequenceDataset data = build(env, args[1], DEFAULT_FE, 3);
			ViewerFrame f = new ViewerFrame(args[0]+" "+args[1], data.toGUI());
			if (args.length>2) {
				DatasetLoader.saveSequence(data, new File(args[2]));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: env userLabel outputFile");
		}
	}
}
