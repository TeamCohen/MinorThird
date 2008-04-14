/* Copyright 2003, Carnegie Mellon, All Rights Reserved */
package edu.cmu.minorthird.text;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.BinaryBatchVersion;
import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetClassifierTeacher;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.algorithms.linear.NaiveBayes;
import edu.cmu.minorthird.classify.algorithms.trees.AdaBoost;
import edu.cmu.minorthird.text.learn.SampleFE;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.util.IOUtil;

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
		edu.cmu.minorthird.text.BasicTextBase base = new BasicTextBase();
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
//		edu.cmu.minorthird.text.TextBaseLoader loader = new edu.cmu.minorthird.text.TextBaseLoader();
//		File file = new File("examples/webmasterDataLines.txt");
		//loader.setFirstWordIsDocumentId(true);
		//loader.loadLines(base,file);
		MutableTextLabels labels = new BasicTextLabels( base );
		new edu.cmu.minorthird.text.TextLabelsLoader().importOps(labels,base,new File("examples/addChangeDelete.env"));

		System.out.println("learning a test concept");
		SpanFeatureExtractor fe = SampleFE.BAG_OF_WORDS;
		Dataset data = new BasicDataset();
		for (Iterator<Span> i=base.documentSpanIterator(); i.hasNext(); ) {
			edu.cmu.minorthird.text.Span s = i.next();
			double label = labels.hasType(s,"delete") ? +1 : -1;
			TextLabels textLabels = new EmptyLabels();
			data.add( new Example( fe.extractInstance(textLabels,s), ClassLabel.binaryLabel(label) ) );
		}
		ClassifierLearner learner = new AdaBoost(new BinaryBatchVersion(new NaiveBayes()), 10);
		Classifier c = new DatasetClassifierTeacher(data).train(learner);

		System.out.println("saving it in "+filename);
		IOUtil.saveSerialized((Serializable)c, new File(filename));
	}
}
