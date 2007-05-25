/* Copyright 2007, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.trees;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Set;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;



/**
 * Random Forests implementation. See http://www.stat.berkeley.edu/~breiman/RandomForests/
 * For algorith details.
 * @author Alexander Friedman
 */

public class RandomForests extends BatchBinaryClassifierLearner
{
  private static Logger log = Logger.getLogger(RandomForests.class);

  // We can use whatever batch learner as a base for this.
  private RandomTreeLearner baseLearner;
  private int               numComponents;

  private boolean           isThreaded  = true;
  private int               threadCount = 4;

  // 100 Is the reccomeneded default tree count
  public RandomForests() {this(100);}

  public RandomForests(int numComponents) {this(new FastRandomTreeLearner(), numComponents);}

  public RandomForests(RandomTreeLearner baseLearner,int numComponents) {
    this.baseLearner   = baseLearner;
    this.numComponents = numComponents;
  }


  private class LearnerThread extends Thread{

    Vector<Example>    examples;
    Vector<Feature>    features;
    List               classifiers;
    Hashtable          results;
    Semaphore          s;

    public LearnerThread(Vector<Example> examples, Vector<Feature> features, 
    		             List classifiers, Hashtable results, Semaphore s) {
      this.examples    = examples;
      this.features    = features;
      this.classifiers = classifiers;
      this.results     = results;
      this.s           = s;
    }

    public void run() {
      List<Example>          newData = new LinkedList<Example>();
      HashSet<Example>      oobData  = new HashSet<Example>();
      HashSet<Example>   duplicates  = new HashSet<Example>();
      // pick N random elements, with replacement
      // we also keep track of elements that are not in the training set, to look
      // at the error rate of each tree
      for(int i = 0 ; i < examples.size() ; i++) {
        Example e = (Example) examples.elementAt((int) Math.floor(Math.random() * examples.size()));

        if(duplicates.add(e)) { newData.add(e); }
      }

      for(Example e: examples) { if(!duplicates.contains(e)) { oobData.add(e); }}

      log.info("RandomForest is building tree "+ " with " + newData.size() + " elements");

      BinaryClassifier c = (BinaryClassifier) (baseLearner.batchTrain(newData,features));

      classifiers.add(c);
      results.put(c, oobData);
      if(isThreaded) { s.release(); }
    }
  }

  public Classifier batchTrain(Dataset dataset) {
	  Vector<Example> examples    = new Vector<Example>(dataset.size());
	  Vector<Feature> allFeatures = getDatasetFeatures(dataset);
	  int eSize                   = dataset.size();
	  Example.Looper it           = dataset.iterator();
	  
	  for(int i = 0 ; i< eSize; i++) {examples.add(i,it.nextExample());}
	     
	  Hashtable oobMap   = new Hashtable();
	  List classifiers   = new ArrayList(numComponents);
	  ProgressCounter pc = new ProgressCounter("RandomForest","treecounts",numComponents);

	  
	  int numThreads     = isThreaded ? (threadCount - 1) : 1;
	  Semaphore s        = new Semaphore(numThreads);


	  log.info("Random forests starting with " + dataset.size() + " elements");

	  log.info("example size: " + examples.size());
	  log.info("Learning classifier with " + baseLearner);
	  
	  for(int t=0; t<numComponents ; t++) {

		  // This is a closure.
		  if(isThreaded) s.acquireUninterruptibly();
		  Thread runnerT = new LearnerThread(examples,new Vector<Feature>(allFeatures),
				                             classifiers,oobMap,s);
		  
		  if(isThreaded) {
			  runnerT.start(); 
		  } else {
			  runnerT.run();
		  }
		  pc.progress(); // this will not report quite right...
	  }

	  // Acquire the whole semaphore. this will wait untilltill everyone is done.
	  if(isThreaded) {
		  s.acquireUninterruptibly(numThreads);
		  s.release(numThreads);
	  }
	  pc.finished();
	  printSomeStats(examples, oobMap);
	  return new VotingClassifier(classifiers);
  }

  private void printSomeStats(Vector<Example> examples, Hashtable oobMap) {
    printTreeShapeInfo(oobMap);
    printOobErrorEstimate(examples, oobMap);
  }

  private void printTreeShapeInfo(Hashtable oobmap) {

    int maxDepth[] = new int[oobmap.size()];
    int numNodes[] = new int[oobmap.size()];


    // map and fold would be really nice to have here...
    int i = 0;
    for (Enumeration e = oobmap.keys() ; e.hasMoreElements() ; ++i) {
      DecisionTree t = (DecisionTree) e.nextElement();
      maxDepth[i] = maxDepth(t);
      numNodes[i] = numNodes(t);
    }

    int avgNumNodes = 0,
        avgMaxDepth = 0,
        maxMaxDepth = 0;

    for (i = 0 ; i < oobmap.size() ; i++) {
      avgNumNodes += numNodes[i];
      avgMaxDepth += maxDepth[i];
      maxMaxDepth  = maxDepth[i] > maxMaxDepth ? maxDepth[i] : maxMaxDepth;
    }

    avgNumNodes = (int) Math.round((double)avgNumNodes / (double)i);
    avgMaxDepth = (int) Math.round((double)avgMaxDepth / (double)i);

    log.info("Average Number of nodes: "   + avgNumNodes);
    log.info("Average Max depth of tree: " + avgMaxDepth);
    log.info("Max Max depth of tree: "     + maxMaxDepth);

  }

  private int maxDepth(DecisionTree t) {
    if (t instanceof DecisionTree.Leaf) return 1;
    DecisionTree.InternalNode n = (DecisionTree.InternalNode) t;
    int tb = maxDepth(n.getTrueBranch());
    int fb = maxDepth(n.getFalseBranch());
    return (tb > fb ? tb : fb) + 1;
  }

  private int numNodes(DecisionTree t) {
    if (t instanceof DecisionTree.Leaf) return 1;
    
    DecisionTree.InternalNode n = (DecisionTree.InternalNode) t;
    int tb = maxDepth(n.getTrueBranch());
    int fb = maxDepth(n.getFalseBranch());
    return tb + fb + 1;
  }

  private void printOobErrorEstimate(Vector<Example> examples, Hashtable oobMap) {
    int numCorrect = 0, numIncorrect = 0;
    // for each example
    //  find trees that have the example oob.
    //  vote with just those trees.
    for(Example e: examples) {
      double score = 0;
      for (Enumeration trees = oobMap.keys() ; trees.hasMoreElements();) {
        DecisionTree t  = (DecisionTree) trees.nextElement();
        HashSet oobData = (HashSet) oobMap.get(t);
        if(oobData.contains(e)) score += t.score(e.asInstance());
      }
      if(((e.getLabel().numericLabel()>0) && (score > 0)) ||
         ((e.getLabel().numericLabel()<0) && (score < 0))) {
        numCorrect++;
      } else {
        numIncorrect++;
      }
    }

    log.info("out of bag num correct: "     + numCorrect);
    log.info("out of bag num inCorrect: "   + numIncorrect);
    log.info("out of bag estimated error: " + (double) numIncorrect /
                                             ((double) (numCorrect + numIncorrect)));
  }


  /**
   * A set of RandomTree Classifiers
   * FIXME!! All of this stuff is copied directly for AdaBoost with (very)
   * minor modifications
   */
  public static class VotingClassifier extends BinaryClassifier implements Serializable,Visible {
    private List classifiers;
    public VotingClassifier(List classifiers) 
    {
      this.classifiers = classifiers;
    }
    
    // Hack. This can be used by anyone that needs the individual classifiers.
    public List getClassifiers() { return classifiers;}
    
    public double score(Instance instance) 
    {
      double totalScore = 0;
      for (Iterator i=classifiers.iterator(); i.hasNext(); ) {
        BinaryClassifier c = (BinaryClassifier)i.next();
        totalScore += c.score(instance);
      }

      if(totalScore > 0) {return 1; }
      else return -1;

    }
    public String explain(Instance instance) 
    {
      StringBuffer buf = new StringBuffer("");
      double totalScore = 0;
      for (Iterator i=classifiers.iterator(); i.hasNext(); ) {
        BinaryClassifier c = (BinaryClassifier)i.next();
        totalScore += c.score(instance);
        buf.append("score of "+c+": "+c.score(instance)+"\n");
        buf.append(StringUtil.indent(1,c.explain(instance))+"\n");
      }
      buf.append("total score: "+totalScore);
      return buf.toString();
    }
    public Explanation getExplanation(Instance instance) {
      Explanation.Node top = new Explanation.Node("Random Forest Explanation");

      double totalScore = 0;
      for (Iterator i=classifiers.iterator(); i.hasNext(); ) {
        BinaryClassifier c = (BinaryClassifier)i.next();
        totalScore += c.score(instance);

        Explanation.Node score   = new Explanation.Node("score of " + c);
        Explanation.Node scoreEx = new Explanation.Node(totalScore + " ");
        Explanation.Node childEx = c.getExplanation(instance).getTopNode();
        score.add(scoreEx);
        score.add(childEx);
        top.add(score);
      }
      Explanation.Node total = new Explanation.Node("total score: "+totalScore);
      top.add(total);
      Explanation ex = new Explanation(top);
      return ex;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer("[voting classifiers:\n");
      for (Iterator i=classifiers.iterator(); i.hasNext(); ) {
        BinaryClassifier c = (BinaryClassifier)i.next();
        buf.append(c.toString()+"\n");
      }
      buf.append("]");
      return buf.toString();
    }
    public Viewer toGUI()
    {
      Viewer v = new VotingClassifierViewer();
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
   * @return
   */
  public static Vector<Feature> getDatasetFeatures(Dataset dataset) {
	  Example.Looper             it = dataset.iterator();
	  HashSet<Feature> allFeatures  = new HashSet<Feature>();
	  for(Example example; it.hasNext();) { 
		  example = it.nextExample(); 
		  for(Feature.Looper j=example.binaryFeatureIterator(); j.hasNext();) {
			  Feature f = j.nextFeature();
			  allFeatures.add(f);
		  }
		  for(Feature.Looper j=example.numericFeatureIterator(); j.hasNext();) {
			  Feature f = j.nextFeature();
			  allFeatures.add(f);
		  }
	  }
	  
	  return new Vector<Feature>(allFeatures);
	  
  }
  private static class VotingClassifierViewer extends ComponentViewer 
  {
    public JComponent componentFor(Object o) {
      VotingClassifier bc = (VotingClassifier)o;
      JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      int ypos = 0;
      for (Iterator i=bc.classifiers.iterator(); i.hasNext(); ) {
        Classifier c = (Classifier)i.next();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = gbc.weighty = 0;
        gbc.gridx = 0; gbc.gridy = ypos++;
        Viewer subview = (c instanceof Visible) ? ((Visible)c).toGUI() : new VanillaViewer(c);
        subview.setSuperView(this);
        panel.add(subview, gbc);
      }
      JScrollPane scroller = new JScrollPane(panel);
      scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      return scroller;
    }
  }
}

