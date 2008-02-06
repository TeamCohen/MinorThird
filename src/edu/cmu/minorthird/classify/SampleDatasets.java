/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedDataset;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;

/** Some sample inputs for learners.
 *
 * @author William Cohen
 */

public class SampleDatasets{

	static private Logger log=Logger.getLogger(SampleDatasets.class);

	public static final String[] posTrain={
		"a pricy doll house",
		"a little red fire truck","a red wagon",
		"a pricy red sports car",
		"punk queen barbie and ken",
		"a little red bike"
	};

	public static final String[] negTrain={
		"a a a a big 7-seater minivan with an automatic transmission",
		"a big house in the suburbs with a crushing mortgage",
		"a job for life at IBM",
		"a huge pile of tax forms, due yesterday",
		"huge pile of junk mail, bills, and catalogs"
	};

	public static final String[] posTest={
		"a pricy barbie doll",
		"a little yellow toy car",
		"a red 10 speed bike",
		"a red convertible porshe"
	};

	public static final String[] negTest={
		"a big pile of paperwork",
		"a huge backlog of email",
		"a life of woe and trouble"
	};

	private static Dataset makeData(String[] pos,String[] neg){
		Dataset result=new BasicDataset();
		for(int i=0;i<pos.length;i++){
			result.add(makeExample(+1,pos[i]));
		}
		for(int i=0;i<neg.length;i++){
			result.add(makeExample(-1,neg[i]));
		}
		return result;
	}

	private static Example makeExample(double label,String text){
		MutableInstance instance=new MutableInstance(text);
		StringTokenizer tok=new StringTokenizer(text);
		while(tok.hasMoreTokens()){
			String word=tok.nextToken();
			instance.addBinary(new Feature(word));
		}
		return new Example(instance,ClassLabel.binaryLabel(label));
	}

	/** Training data for a trivial classification problem.
	 */
	public static Dataset toyTrain(){
		return makeData(posTrain,negTrain);
	}

	/** Test data for a trivial classification problem.
	 */
	public static Dataset toyTest(){
		return makeData(posTest,negTest);
	}

	private static String[] posBayesTrain=
	{"a a pricy doll house","a a little red red fire truck","a red wagon",
		"a pricy red sports car","punk queen barbie and and ken",
	"a little red bike"};

	private static String[] negBayesTrain=
	{"a big big 7-seater minivan with with an an automatic transmission",
		"a big house in the suburbs with a a crushing mortgage",
		"a job for for life at at IBM",
		"a huge pile of of tax forms, due yesterday",
	"huge pile of of junk mail, bills, and catalogs"};

	private static String[] posBayesTest=
	{"a a pricy barbie doll","a little yellow toy car",
		"a a red 10 speed bike","a red convertible porshe"};

	private static String[] negBayesTest=
	{"a big pile of of paperwork","a huge backlog of email",
	"a life of woe and and trouble"};

	private static String[] posBayesExtremeTrain=
	{"p1 p1 p1 p2 p2 p3 p3 p4 p4 p5 p5 n1 n2 n3 n4 n5",
		"p1 p1 p2 p2 p2 p3 p3 p4 p4 p5 p5 n1 n2 n3 n4 n5",
		"p1 p1 p2 p2 p3 p3 p3 p4 p4 p5 p5 n1 n2 n3 n4 n5",
		"p1 p1 p2 p2 p3 p3 p4 p4 p4 p5 p5 n1 n2 n3 n4 n5",
	"p1 p1 p2 p2 p3 p3 p4 p4 p5 p5 p5 n1 n2 n3 n4 n5"};

	private static String[] negBayesExtremeTrain=
	{"p1 p2 p3 p4 p5 n1 n1 n1 n2 n2 n3 n3 n4 n4 n5 n5",
		"p1 p2 p3 p4 p5 n1 n1 n2 n2 n2 n3 n3 n4 n4 n5 n5",
		"p1 p2 p3 p4 p5 n1 n1 n2 n2 n3 n3 n3 n4 n4 n5 n5",
		"p1 p2 p3 p4 p5 n1 n1 n2 n2 n3 n3 n4 n4 n4 n5 n5",
	"p1 p2 p3 p4 p5 n1 n1 n2 n2 n3 n3 n4 n4 n5 n5 n5"};

	private static String[] posBayesExtremeTest=
	{"p1 p1 n1","p2 p2 n2","p3 p3 n3","p4 p4 n4","p5 p5 n5"};

	private static String[] negBayesExtremeTest=
	{"p1 n1 n1","p2 n2 n2","p3 n3 n3","p4 n4 n4","p5 n5 n5"};

	public static Dataset toyBayesExtremeTrain(){
		return makeBayesData(posBayesExtremeTrain,negBayesExtremeTrain);
	}

	public static Dataset toyBayesExtremeTest(){
		return makeBayesData(posBayesExtremeTest,negBayesExtremeTest);
	}

	private static String[] unlabeledBayesExtreme=
	{"p1 n1 n1","p2 n2 n2","p3 n3 n3","p1 p1 n1","p2 p2 n2","p3 p3 n3"};

	public static Dataset toyBayesExtremeUnlabeledTrain(){
		return makeUnlabeledBayesData(posBayesExtremeTrain,negBayesExtremeTrain,
				unlabeledBayesExtreme);
	}

	/** Makes test-data for generative Bayesian models */
	private static Dataset makeUnlabeledBayesData(String[] pos,String[] neg,
			String[] unlabeled){
		SemiSupervisedDataset result=new SemiSupervisedDataset();
		for(int i=0;i<pos.length;i++){
			result.add(makeLabeledBayesExample(new ClassLabel("POS"),pos[i]));
		}
		for(int i=0;i<neg.length;i++){
			result.add(makeLabeledBayesExample(new ClassLabel("NEG"),neg[i]));
		}
		for(int i=0;i<unlabeled.length;i++){
			result.addUnlabeled(makeUnlabeledBayesExample(unlabeled[i]));
		}
		return result;
	}

	/** Makes test-example for generative Bayesian models */
	private static Example makeLabeledBayesExample(ClassLabel label,String text){
		MutableInstance instance=new MutableInstance();
		StringTokenizer tok=new StringTokenizer(text);
		while(tok.hasMoreTokens()){
			String word=tok.nextToken();
			Feature f=new Feature(word);
			double w=instance.getWeight(f);
			if(w==0)
				instance.addBinary(f);
			else
				instance.addNumeric(f,w+1);
		}
		return new Example(instance,label);
	}

	/** Makes test-example for generative Bayesian models */
	private static Instance makeUnlabeledBayesExample(String text){
		MutableInstance instance=new MutableInstance();
		StringTokenizer tok=new StringTokenizer(text);
		while(tok.hasMoreTokens()){
			String word=tok.nextToken();
			Feature f=new Feature(word);
			double w=instance.getWeight(f);
			if(w==0)
				instance.addBinary(f);
			else
				instance.addNumeric(f,w+1);
		}
		return instance;
	}

	/** Makes test-data for generative Bayesian models */
	private static Dataset makeBayesData(String[] pos,String[] neg){
		Dataset result=new BasicDataset();
		for(int i=0;i<pos.length;i++){
			result.add(makeBayesExample(+1,pos[i]));
		}
		for(int i=0;i<neg.length;i++){
			result.add(makeBayesExample(-1,neg[i]));
		}
		return result;
	}

	/** Makes test-example for generative Bayesian models */
	private static Example makeBayesExample(double label,String text){
		MutableInstance instance=new MutableInstance();
		StringTokenizer tok=new StringTokenizer(text);
		while(tok.hasMoreTokens()){
			String word=tok.nextToken();
			Feature f=new Feature(word);
			double w=instance.getWeight(f);
			if(w==0)
				instance.addBinary(f);
			else
				instance.addNumeric(f,w+1);
		}
		return new Example(instance,ClassLabel.binaryLabel(label));
	}

	/** Training data for a trivial classification problem.
	 */
	public static Dataset toyBayesTrain(){
		return makeBayesData(posBayesTrain,negBayesTrain);
	}

	/** Test data for a trivial classification problem.
	 */
	public static Dataset toyBayesTest(){
		return makeBayesData(posBayesTest,negBayesTest);
	}

	/** Sparse numeric data - some values are 1.0, and some are zero. */
	public static Dataset makeSparseNumericData(Random r,int m){
		Dataset result=new BasicDataset();
		Feature fx=new Feature("x");
		for(int i=0;i<m;i++){
			MutableInstance instance=new MutableInstance();
			double x=r.nextDouble();
			if(x>0.7){
				instance.addNumeric(fx,1.0);
				result.add(new Example(instance,ClassLabel.binaryLabel(+1)));
			}else{
				result.add(new Example(instance,ClassLabel.binaryLabel(-1)));
			}
		}
		return result;
	}

	/** Random data, defined by a simple boolean combination of thresholds
	 * over two dimensions, with up to 5 irrelevant dimensions, and m
	 * examples.
	 */
	public static Dataset makeNumericData(Random r,int dim,int m){
		Feature fx=new Feature("x");
		Feature fy=new Feature("y");

		Dataset result=new BasicDataset();
		String[] vars={"x","y","z","t","u","v","w"};
		if(dim>vars.length)
			throw new IllegalArgumentException("dim to big!");
		for(int i=0;i<m;i++){
			MutableInstance instance=new MutableInstance();
			for(int j=0;j<dim;j++){
				// for testing purposes, leave the 'x' feature out of the first
				// example, and the 'y' feature out of the second
				if(j!=i){
					instance.addNumeric(new Feature(vars[j]),r.nextDouble()*10);
				}
			}
			double x=instance.getWeight(fx);
			double y=instance.getWeight(fy);
			//double label = x<3 ? +1 : -1;
			double label=(x<3&&y<3||x>7&&y>7)?+1:-1;
			//if (r.nextDouble() < 0.5) label *= -1;
			result.add(new Example(instance,ClassLabel.binaryLabel(label)));
		}

		return result;
	}

	/** Data useful for testing univariate logistic regression.  The
	 * dataset will contain m examples, each with a single
	 * uniformly-distributed numeric feature x. The probability of the
	 * positive class will be chosen according to logistic(a*x + b).
	 */
	public static Dataset makeLogisticRegressionData(Random rand,int m,double a,
			double b){
		int numPos=0,numNeg=0;
		Dataset data=new BasicDataset();
		for(int i=0;i<m;i++){
			double x=rand.nextDouble();
			double p=MathUtil.logistic(a*x+b);
			double r=rand.nextDouble();
			ClassLabel y=p>r?ClassLabel.positiveLabel(1):ClassLabel.negativeLabel(-1);
			if(p>r)
				numPos++;
			else
				numNeg++;
			MutableInstance instance=new MutableInstance();
			instance.addNumeric(new Feature("x"),x);
			instance.addBinary(new Feature("bias"));
			data.add(new Example(instance,y));
		}
		System.out.println(m+" examples: "+numPos+" pos, "+numNeg+" neg");
		return data;
	}

	public static SequenceDataset makeToySequenceData(){
		return makeToySequenceData(new String[]{"you're a good man Charlie Brown",
				"where's Waldo?","alas dear Yorick, I knew him well"});
	}

	public static SequenceDataset makeToySequenceTestData(){
		return makeToySequenceData(new String[]{"hello, World War III",
		"to be or 2B, that is a question"});
	}

	public static SequenceDataset makeToySequenceData(String[] lines){
		SequenceDataset d=new SequenceDataset();
		for(int i=0;i<lines.length;i++){
			String[] w=lines[i].split(" ");
			Example[] seq=new Example[w.length];
			for(int j=0;j<w.length;j++){
				ClassLabel lab=
					Character.isUpperCase(w[j].charAt(0))?new ClassLabel("POS")
				:new ClassLabel("NEG");
					MutableInstance inst=new MutableInstance(lines[i]+":"+j,"line"+i);
					inst.addBinary(new Feature("here "+w[j]));
					if(j>1)
						inst.addBinary(new Feature("prev "+w[j-1]));
					if(j<w.length-1)
						inst.addBinary(new Feature("next "+w[j+1]));
					inst.addBinary(new Feature("casePattern "+
							w[j].replaceAll("[A-Z]+","A").replaceAll("[a-z]+","a")));
					seq[j]=new Example(inst,lab);
			}
			d.addSequence(seq);
		}
		return d;
	}

	/**
	 * Makes a sample 3 class dataset
	 *
	 * @param random A random number generator for building the dataset.
	 * @param numInstances The number of instances to be created.
	 *
	 */
	public static Dataset makeToy3ClassData(Random random,int numInstances){

		String[][] features=new String[][]{
				{"money","cash","sleep","booze","chocolate","fun","beer","pizza"},
				{"stocks","bonds","money","cash","influence","power","fame"},
				{"chocolate","beer","pizza","pringles","popcorn","spam","crisco"}
		};

		String[] labels=new String[]{"homer","marge","bart"};

		Dataset dataset=new BasicDataset();

		for(int i=0;i<numInstances;i++){
			int classLabel=random.nextInt(labels.length);
			int numFeatures=random.nextInt(3)+2;
			MutableInstance instance=new MutableInstance();
			for(int j=0;j<numFeatures;j++){
				int feature=random.nextInt(features[classLabel].length);
				instance.addBinary(new Feature(new String[]{"word",features[classLabel][feature]}));
			}
			dataset.add(new Example(instance,new ClassLabel(labels[classLabel])));
		}
		return dataset;
	}

	public static Dataset sampleData(String name,boolean isTest){
		if("toy".equals(name)){
			if(isTest)
				return toyTest();
			else
				return toyTrain();
		}else if("bayes".equals(name)){
			if(isTest)
				return toyBayesTest();
			else
				return toyBayesTrain();
		}else if("bayesExtreme".equals(name)){
			if(isTest)
				return toyBayesExtremeTest();
			else
				return toyBayesExtremeTrain();
		}else if("bayesUnlabeled".equals(name)){
			if(isTest)
				return toyBayesExtremeTest();
			else
				return toyBayesExtremeUnlabeledTrain();
		}else if("num".equals(name)){
			if(isTest)
				return makeNumericData(new Random(666),2,20);
			else
				return makeNumericData(new Random(999),2,20);
		}else if("logistic".equals(name)){
			if(isTest)
				return makeLogisticRegressionData(new Random(666),50,2,-2);
			else
				return makeLogisticRegressionData(new Random(999),50,2,-2);
		}else if("bigLogistic".equals(name)){
			if(isTest)
				return makeLogisticRegressionData(new Random(666),1000,2,-2);
			else
				return makeLogisticRegressionData(new Random(999),1000,2,-2);
		}else if("sparseNum".equals(name)){
			if(isTest)
				return makeSparseNumericData(new Random(666),20);
			else
				return makeSparseNumericData(new Random(999),20);
		}else if("toy3".equals(name)){
			if(isTest)
				return makeToy3ClassData(new Random(666),50);
			else
				return makeToy3ClassData(new Random(999),50);
		}else if("toySeq".equals(name)){
			if(isTest)
				return makeToySequenceTestData();
			else
				return makeToySequenceData();
		}else{
			throw new IllegalArgumentException("illegal dataset name '"+name+"'");
		}
	}

	public static void main(String[] args){
		try{
			Dataset train=sampleData(args[0],false);
			Dataset test=sampleData(args[0],true);
			log.debug("Train dataset is: ");
			log.debug(train.toString());
			log.debug("Test dataset is:");
			log.debug(test.toString());
			if(args.length>0){
				ClassifierLearner learner=
					(ClassifierLearner)Class.forName(args[1]).newInstance();
				boolean active=args.length>=3&&"active".equals(args[2]);
				ClassifierTeacher teacher=new DatasetClassifierTeacher(train,active);
				Classifier c=teacher.train(learner);
				log.info("Classifier: "+c);
				traceClassifier("Train",c,train);
				traceClassifier("Test",c,test);
				if(c instanceof Visible){
					new ViewerFrame(args[1]+" on "+args[0],((Visible)c).toGUI());
				}
			}
		}catch(Exception e){
			System.out
			.println("usage: [toy|num] edu.cmu.minorthird.classify.SomeLearner [active]");
			e.printStackTrace();
		}
	}

	static private void traceClassifier(String datasetName,Classifier c,Dataset d){
		log.info("");
		log.info("Performance on dataset "+datasetName+":");
		for(Iterator<Example> i=d.iterator();i.hasNext();){
			Example e=i.next();
			if(c instanceof BinaryClassifier){
				double actual=e.getLabel().numericLabel();
				double predicted=c.classification(e).posWeight();
				String ok=predicted*actual>=0?"Y":"N";
				log.info(ok+"\tpred="+predicted+"\tactual="+actual+"\t"+e);
			}else{
				ClassLabel actual=e.getLabel();
				ClassLabel predicted=c.classification(e);
				String ok=predicted.isCorrect(actual)?"Y":"N";
				log.info(ok+"\tpred="+predicted+"\tactual="+actual+"\t"+e);
			}
		}
	}
}
