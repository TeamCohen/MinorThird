/* Copyright 2007, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.trees;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BatchBinaryClassifierLearner;
import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.Explanation;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.VanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.Visible;

/**
 * Random Forests implementation. See http://www.stat.berkeley.edu/~breiman/RandomForests/
 * For algorith details.
 * @author Alexander Friedman
 */
// TODO - pass in random seed/set threads to 1 for reproducibility
// rename to ..Learner
public class RandomForests extends BatchBinaryClassifierLearner{

	private static Logger log=Logger.getLogger(RandomForests.class);

	// We can use whatever batch learner as a base for this.
	private FastRandomTreeLearner baseLearner;

	private int numComponents;

	private int selectSize=1;

	private boolean selectSizeLog=true; // This seems to be a good default

	// store the OOB map for the trees and print stats. This eats lots of memory and kills the collector
	// on larger datasets
	private boolean collectStats=false;

	private boolean isThreaded=true;

	private int threadCount=4;

	private Random rand;

	// by default, scale the weights of examples
	private boolean scaleWeights=true;

	// 101 Is the reccomeneded default tree count - odd to force decision
	public RandomForests(){
		this(101);
	}

	public RandomForests(int numComponents){
		this(new FastRandomTreeLearner(),numComponents);
	}

	public RandomForests(FastRandomTreeLearner baseLearner,int numComponents){
		this.baseLearner=baseLearner;
		this.numComponents=numComponents;
		this.threadCount=java.lang.Runtime.getRuntime().availableProcessors();
		rand=new Random();

		log.info("setting number of random forest threads to "+threadCount);
	}

	public RandomForests setThreaded(boolean b){
		isThreaded=b;
		return this;
	}

	public RandomForests setThreadCount(int c){
		threadCount=c;
		return this;
	}

	public RandomForests setCollectStats(boolean b){
		collectStats=b;
		return this;
	}

	public RandomForests setScaleWeights(boolean b){
		scaleWeights=b;
		return this;
	}

	// sets the selection size to log_2(numFeatures)
	public RandomForests setSelectionSizeLog(){
		selectSizeLog=true;
		return this;
	}

	public RandomForests setSelectionSize(int c){
		selectSize=c;
		selectSizeLog=false;
		return this;
	}

	public static RandomForests RepeatableForest(){

		RandomForests rf=
				new RandomForests(new FastRandomTreeLearner().setRandomSeed(0),101);
		rf.rand=new Random(0);
		rf.setThreaded(false);
		return rf;
	}

	private class LearnerThread extends Thread{

		Vector<Example> examples;

		Vector<Feature> features;

		List<Classifier> classifiers;

		Hashtable<Classifier,Set<Example>> results;

		Semaphore s;

		public LearnerThread(Vector<Example> examples,Vector<Feature> features,
				List<Classifier> classifiers,Hashtable<Classifier,Set<Example>> results,Semaphore s){
			this.examples=examples;
			this.features=features;
			this.classifiers=classifiers;
			this.results=results;
			this.s=s;
		}

		@Override
		public void run(){
			List<Example> newData=new LinkedList<Example>();
			Set<Example> oobData=new HashSet<Example>();
			Set<Example> duplicates=new HashSet<Example>();
			// pick N random elements, with replacement
			// we also keep track of elements that are not in the training set, to look
			// at the error rate of each tree
			for(int i=0;i<examples.size();i++){
				Example e=
						examples.elementAt((int)Math.floor(rand.nextDouble()*
								examples.size()));

				if(duplicates.add(e)){
					newData.add(e);
				}
			}

			for(Example e:examples){
				if(!duplicates.contains(e)&&collectStats){
					oobData.add(e);
				}
			}

			log.debug("RandomForest is building tree "+" with "+newData.size()+
					" elements");

			BinaryClassifier c=
					(BinaryClassifier)(baseLearner.batchTrain(newData,features));

			classifiers.add(c);
			if(collectStats){
				results.put(c,oobData);
			}
			if(isThreaded){
				s.release();
			}
		}
	}

	@Override
	public Classifier batchTrain(Dataset dataset){
		Vector<Example> examples=new Vector<Example>(dataset.size());
		Vector<Feature> allFeatures=getDatasetFeatures(dataset);
		int eSize=dataset.size();
		Iterator<Example> it=dataset.iterator();

		double pos=0;
		double neg=0;
		for(int i=0;i<eSize;i++){
			Example e=it.next();
			// copy the example;
			examples
					.addElement(new Example(e.asInstance(),e.getLabel(),e.getWeight()));
//       if (e.getLabel().numericLabel()>0) pos ++;
//       else                               neg ++;
			if(e.getLabel().numericLabel()>0)
				pos+=e.getWeight();
			else
				neg+=e.getWeight();

		}

		if(scaleWeights){
			double pRatio=(pos/(pos+neg))+.0001;
			double nRatio=(neg/(pos+neg))+.0001;
			for(Example e:examples){
				if(e.getLabel().numericLabel()>0){
					e.setWeight(e.getWeight()/pRatio);
				}else{
					e.setWeight(e.getWeight()/nRatio);
				}
			}
		}

		// set the subset size of the base learner;
		if(selectSizeLog){
			baseLearner.setSubsetSize(Math.max((int)Math.floor(Math.log(allFeatures
					.size())/
					Math.log(2)),1));
		}else{
			baseLearner.setSubsetSize(selectSize);
		}

		Hashtable<Classifier,Set<Example>> oobMap=new Hashtable<Classifier,Set<Example>>();
		List<Classifier> classifiers=new ArrayList<Classifier>(numComponents);
		ProgressCounter pc=
				new ProgressCounter("RandomForest","treecount",numComponents);

		int numThreads=isThreaded?threadCount:1;
		Semaphore s=new Semaphore(numThreads);

		log.info("Random forests starting with "+dataset.size()+" elements, "+
				allFeatures.size()+" features");

		log.info("example size: "+examples.size());
		log.info("Learning classifier with "+baseLearner);

		for(int t=0;t<numComponents;t++){

			// This is a closure.
			if(isThreaded)
				s.acquireUninterruptibly();
			Thread runnerT=
					new LearnerThread(examples,new Vector<Feature>(allFeatures),
							classifiers,oobMap,s);

			if(isThreaded){
				runnerT.start();
			}else{
				runnerT.run();
			}
			pc.progress(); // this will not report quite right...
		}

		// Acquire the whole semaphore. this will wait untill everyone is done.
		if(isThreaded){
			s.acquireUninterruptibly(numThreads);
			s.release(numThreads);
		}
		pc.finished();
		if(collectStats){
			printSomeStats(examples,oobMap);
		}
		return new VotingClassifier(classifiers);
	}

	private void printSomeStats(Vector<Example> examples,Hashtable<Classifier,Set<Example>> oobMap){
		printTreeShapeInfo(oobMap);
		printOobErrorEstimate(examples,oobMap);
	}

	private void printTreeShapeInfo(Hashtable<Classifier,Set<Example>> oobmap){

		int maxDepth[]=new int[oobmap.size()];
		int numNodes[]=new int[oobmap.size()];

		// map and fold would be really nice to have here...
		int i=0;
		for(Enumeration<Classifier> e=oobmap.keys();e.hasMoreElements();++i){
			DecisionTree t=(DecisionTree)e.nextElement();
			maxDepth[i]=maxDepth(t);
			numNodes[i]=numNodes(t);
		}

		int avgNumNodes=0,avgMaxDepth=0,maxMaxDepth=0;

		for(i=0;i<oobmap.size();i++){
			avgNumNodes+=numNodes[i];
			avgMaxDepth+=maxDepth[i];
			maxMaxDepth=maxDepth[i]>maxMaxDepth?maxDepth[i]:maxMaxDepth;
		}

		avgNumNodes=(int)Math.round((double)avgNumNodes/(double)i);
		avgMaxDepth=(int)Math.round((double)avgMaxDepth/(double)i);

		log.info("Average Number of nodes: "+avgNumNodes);
		log.info("Average Max depth of tree: "+avgMaxDepth);
		log.info("Max Max depth of tree: "+maxMaxDepth);

	}

	private int maxDepth(DecisionTree t){
		if(t instanceof DecisionTree.Leaf)
			return 1;
		DecisionTree.InternalNode n=(DecisionTree.InternalNode)t;
		int tb=maxDepth(n.getTrueBranch());
		int fb=maxDepth(n.getFalseBranch());
		return (tb>fb?tb:fb)+1;
	}

	private int numNodes(DecisionTree t){
		if(t instanceof DecisionTree.Leaf)
			return 1;

		DecisionTree.InternalNode n=(DecisionTree.InternalNode)t;
		int tb=maxDepth(n.getTrueBranch());
		int fb=maxDepth(n.getFalseBranch());
		return tb+fb+1;
	}

	private void printOobErrorEstimate(Vector<Example> examples,Hashtable<Classifier,Set<Example>> oobMap){
		int numCorrect=0,numIncorrect=0;
		// for each example
		//  find trees that have the example oob.
		//  vote with just those trees.
		for(Example e:examples){
			double score=0;
			for(Enumeration<Classifier> trees=oobMap.keys();trees.hasMoreElements();){
				DecisionTree t=(DecisionTree)trees.nextElement();
				Set<Example> oobData=oobMap.get(t);
				if(oobData.contains(e))
					score+=t.score(e.asInstance());
			}
			if(((e.getLabel().numericLabel()>0)&&(score>0))||
					((e.getLabel().numericLabel()<0)&&(score<0))){
				numCorrect++;
			}else{
				numIncorrect++;
			}
		}

		log.info("out of bag num correct: "+numCorrect);
		log.info("out of bag num inCorrect: "+numIncorrect);
		log.info("out of bag estimated error: "+(double)numIncorrect/
				((double)(numCorrect+numIncorrect)));
	}

	/**
	 * A set of RandomTree Classifiers
	 * FIXME!! All of this stuff is copied directly for AdaBoost with (very)
	 * minor modifications
	 */
	public static class VotingClassifier extends BinaryClassifier implements
			Serializable,Visible{

		static final long serialVersionUID=20080609L;
		
		private List<Classifier> classifiers;

		public VotingClassifier(List<Classifier> classifiers){
			this.classifiers=classifiers;
		}

		// Hack. This can be used by anyone that needs the individual classifiers.
		public List<Classifier> getClassifiers(){
			return classifiers;
		}

		@Override
		public double score(Instance instance){
			double totalScore=0;
			for(Iterator<Classifier> i=classifiers.iterator();i.hasNext();){
				BinaryClassifier c=(BinaryClassifier)i.next();
				totalScore+=c.score(instance);
			}

			return totalScore;
		}

		@Override
		public String explain(Instance instance){
			StringBuffer buf=new StringBuffer("");
			double totalScore=0;
			for(Iterator<Classifier> i=classifiers.iterator();i.hasNext();){
				BinaryClassifier c=(BinaryClassifier)i.next();
				totalScore+=c.score(instance);
				buf.append("score of "+c+": "+c.score(instance)+"\n");
				buf.append(StringUtil.indent(1,c.explain(instance))+"\n");
			}
			buf.append("total score: "+totalScore);
			return buf.toString();
		}

		@Override
		public Explanation getExplanation(Instance instance){
			Explanation.Node top=new Explanation.Node("Random Forest Explanation");

			double totalScore=0;
			for(Iterator<Classifier> i=classifiers.iterator();i.hasNext();){
				BinaryClassifier c=(BinaryClassifier)i.next();
				totalScore+=c.score(instance);

				Explanation.Node score=new Explanation.Node("score of "+c);
				Explanation.Node scoreEx=new Explanation.Node(totalScore+" ");
				Explanation.Node childEx=c.getExplanation(instance).getTopNode();
				score.add(scoreEx);
				score.add(childEx);
				top.add(score);
			}
			Explanation.Node total=new Explanation.Node("total score: "+totalScore);
			top.add(total);
			Explanation ex=new Explanation(top);
			return ex;
		}

		@Override
		public String toString(){
			StringBuffer buf=new StringBuffer("[voting classifiers:\n");
			for(Iterator<Classifier> i=classifiers.iterator();i.hasNext();){
				BinaryClassifier c=(BinaryClassifier)i.next();
				buf.append(c.toString()+"\n");
			}
			buf.append("]");
			return buf.toString();
		}

		@Override
		public Viewer toGUI(){
			Viewer v=new VotingClassifierViewer();
			v.setContent(this);
			return v;
		}
	}

	/**
	 * Return a vector of all features in the dataset.
	 * 
	 * This should really be in the 'Dataset' interface
	 * 
	 * @param dataset
	 * @return a vector of all features in the dataset
	 */
	public static Vector<Feature> getDatasetFeatures(Dataset dataset){
		Iterator<Example> it=dataset.iterator();
		HashSet<Feature> allFeatures=new HashSet<Feature>();
		for(Example example;it.hasNext();){
			example=it.next();
			for(Iterator<Feature> j=example.binaryFeatureIterator();j.hasNext();){
				Feature f=j.next();
				allFeatures.add(f);
			}
			for(Iterator<Feature> j=example.numericFeatureIterator();j.hasNext();){
				Feature f=j.next();
				allFeatures.add(f);
			}
		}

		return new Vector<Feature>(allFeatures);

	}

	private static class VotingClassifierViewer extends ComponentViewer{

		static final long serialVersionUID=20080609L;
		
		@Override
		public JComponent componentFor(Object o){
			VotingClassifier bc=(VotingClassifier)o;
			JPanel panel=new JPanel();
			panel.setLayout(new GridBagLayout());
			int ypos=0;
			for(Iterator<Classifier> i=bc.classifiers.iterator();i.hasNext();){
				Classifier c=i.next();
				GridBagConstraints gbc=new GridBagConstraints();
				gbc.fill=GridBagConstraints.HORIZONTAL;
				gbc.weightx=gbc.weighty=0;
				gbc.gridx=0;
				gbc.gridy=ypos++;
				Viewer subview=
						(c instanceof Visible)?((Visible)c).toGUI():new VanillaViewer(c);
				subview.setSuperView(this);
				panel.add(subview,gbc);
			}
			JScrollPane scroller=new JScrollPane(panel);
			scroller
					.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			return scroller;
		}
	}
}
