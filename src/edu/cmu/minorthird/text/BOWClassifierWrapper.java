/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text;


import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.text.learn.SampleFE;
import edu.cmu.minorthird.util.IOUtil;

import java.io.*;

/** Use a bag-of-words classifier to classify strings.
 *
 * <p>Don't use this: the class text/learn/ClassifierAnnotator is
 * preferable in everyway. This class will eventually be removed from
 * Minorthird.
 *
 * @deprecated
 */

public class BOWClassifierWrapper
{
	private BinaryClassifier classifier;
	private SpanFeatureExtractor fe = SampleFE.BAG_OF_WORDS;

	public BOWClassifierWrapper(File file) throws IOException
	{
		classifier = (BinaryClassifier)IOUtil.loadSerialized(file);
	}

  public BOWClassifierWrapper(InputStream in) throws IOException
  {
    classifier = (BinaryClassifier)IOUtil.loadSerialized(in);
  }

	public double getScore(String string)
	{
		edu.cmu.minorthird.text.TextBase base = new BasicTextBase();
		base.loadDocument("dummyID",string);
		edu.cmu.minorthird.text.Span span = base.documentSpan("dummyID");
		Instance instance = fe.extractInstance(new EmptyLabels(),span);
		return classifier.score(instance);
	}


	//
	// test cases
  //

	public static void main(String[] args)
	{
		try {
			if (args.length==2 && "-create".equals(args[0])) {
				testCreateOutput(args[1]);
			} else {
				BOWClassifierWrapper w = new BOWClassifierWrapper(new File(args[0]));
				for (int i=1; i<args.length; i++) {
					System.out.println("score: "+w.getScore(args[i])+" on input: '"+args[i]+"'");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: -create [file] or [file] testString1 testString2 ...");
		}
	}

	private static void testCreateOutput(String filename) throws IOException
	{
		System.out.println("loading data and labels");
		edu.cmu.minorthird.text.TextBase base = new BasicTextBase();
		edu.cmu.minorthird.text.TextBaseLoader loader = new edu.cmu.minorthird.text.TextBaseLoader();
		File file = new File("examples/webmasterDataLines.txt");
		loader.setFirstWordIsDocumentId(true);
		loader.loadLines(base,file);
		MutableTextLabels labels = new BasicTextLabels( base );
		new edu.cmu.minorthird.text.TextLabelsLoader().importOps(labels,base,new File("examples/addChangeDelete.env"));

		System.out.println("learning a test concept");
		SpanFeatureExtractor fe = SampleFE.BAG_OF_WORDS;
		Dataset data = new BasicDataset();
		for (edu.cmu.minorthird.text.Span.Looper i=base.documentSpanIterator(); i.hasNext(); ) {
			edu.cmu.minorthird.text.Span s = i.nextSpan();
			double label = labels.hasType(s,"delete") ? +1 : -1;
			data.add( new Example( fe.extractInstance(s), ClassLabel.binaryLabel(label) ) );
		}
		ClassifierLearner learner = new AdaBoost(new BinaryBatchVersion(new NaiveBayes()), 10);
		Classifier c = new DatasetClassifierTeacher(data).train(learner);

		System.out.println("saving it in "+filename);
		IOUtil.saveSerialized((Serializable)c, new File(filename));
	}
}
