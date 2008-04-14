package edu.cmu.minorthird.text;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.ClassifierLearner;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.algorithms.linear.PoissonLearner;
import edu.cmu.minorthird.classify.experiments.CrossValSplitter;
import edu.cmu.minorthird.classify.experiments.Evaluation;
import edu.cmu.minorthird.classify.experiments.Tester;
import edu.cmu.minorthird.text.learn.SpanFE;
import edu.cmu.minorthird.util.Globals;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 *
 * This class is responsible for...
 *
 * @author ksteppe
 */
public class BayesClassifiersTest extends TestCase{

	Logger log=Logger.getLogger(this.getClass());

	/**
	 * Standard test class constructior for BayesClassifiersTest
	 * @param name Name of the test
	 */
	public BayesClassifiersTest(String name){
		super(name);
	}

	/**
	 * Convinence constructior for BayesClassifiersTest
	 */
	public BayesClassifiersTest(){
		super("BayesClassifiersTest");
	}

	/**
	 * setUp to run before each test
	 */
	protected void setUp(){
		Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();
		//TODO add initializations if needed
	}

	/**
	 * clean up to run after each test
	 */
	protected void tearDown(){
		//TODO clean up resources if needed
	}

	/**
	 * Base test for BayesClassifiersTest
	 */
	public void testBayesClassifiersTest(){

		try{ // load the documents into a textbase

			File dir=new File(Globals.DATA_DIR+"bayes-testData");
			TextBaseLoader loader=new TextBaseLoader();
			TextBase base=loader.load(dir);

// set up labels
			MutableTextLabels labels=new BasicTextLabels(base);
			new TextLabelsLoader().importOps(labels,base,new File(Globals.DATA_DIR+
					"bayes-testData.labels"));

// for verification/correction of the labels, if we care...
//TextBaseLabeler.label( labels, new File("my-document-labels.env"));

// set up a simple bag-of-words feature extractor
			SpanFE fe=new SpanFE(){
				static final long serialVersionUID=20080302L;
				public void extractFeatures(TextLabels labels,Span s){
					try{
						from(s).tokens().eq().lc().punk()
								.usewords("examples/t1.words.text").emit();
					}catch(IOException e){
						log.error(e,e);
					}
					//SpanFE.from(s,buf).tokens().eq().lc().punk().stopwords("remove").emit();
				}
			};

// check
			log.debug(labels.getTypes().toString());

// create a binary dataset for the class 'rr'
			Dataset data=new BasicDataset();
			for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
				Span s=i.next();
				//System.out.println( labels );
				double label=labels.hasType(s,"rr")?+1:-1;
				TextLabels textLabels=new EmptyLabels();
				data.add(new Example(fe.extractInstance(textLabels,s),ClassLabel
						.binaryLabel(label)));
			}

			new ViewerFrame("rr data",data.toGUI());
//      System.exit(0);

// pick a learning algorithm
			ClassifierLearner learner=new PoissonLearner();

// do a 10-fold cross-validation experiment
			Evaluation v=Tester.evaluate(learner,data,new CrossValSplitter<Example>(10));

// display the results
			new ViewerFrame("Results of 10-fold CV on 'rr'",v.toGUI());
		}catch(Exception e){
			log.error(e,e);
			fail();
		}
	}

	/**
	 * Creates a TestSuite from all testXXX methods
	 * @return TestSuite
	 */
	public static Test suite(){
		return new TestSuite(BayesClassifiersTest.class);
	}

	/**
	 * Run the full suite of tests with text output
	 * @param args - unused
	 */
	public static void main(String args[]){
		junit.textui.TestRunner.run(suite());

	}
}
