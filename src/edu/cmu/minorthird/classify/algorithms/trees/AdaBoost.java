/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.trees;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/** 
 * Generalized version of AdaBoost, as described in Robert E. Schapire
 * and Yoram Singer,  Improved boosting algorithms using
 * confidence-rated predictions.  Machine Learning, 37(3):297-336,
 * 1999.
 * <p>
 * The base learner intended for this is the decision-tree learner
 * in this package.
 *
 * @author William Cohen
 */

public class AdaBoost extends BatchBinaryClassifierLearner 
{
  private static Logger log = Logger.getLogger(AdaBoost.class);

  private BatchClassifierLearner baseLearner;
  private int maxRounds = 100;

  /** AdaBoost.L is a logistic-regression version of AdaBoost. */
  static public class L extends AdaBoost 
  {
    public L() {	super(); }
    public L(BatchClassifierLearner baseLearner,int maxRounds) { super(baseLearner,maxRounds); }
    protected double discountFactor(double y,double yhat)
    {
      return 1.0 + Math.exp( y*yhat );
    }
  }

  public AdaBoost() {	this(new DecisionTreeLearner(),10);	}
  public AdaBoost(BatchClassifierLearner baseLearner,int maxRounds)
  {
    this.baseLearner = baseLearner;
    this.maxRounds = maxRounds;
  }

  public int getMaxRounds() { return maxRounds; }
  public void setMaxRounds(int n) { this.maxRounds=n; }
  public BatchClassifierLearner getBaseLearner() { return baseLearner; }
  public void setBaseLearner(BatchClassifierLearner learner) { this.baseLearner=learner; }

  public Classifier batchTrain(Dataset dataset)
  {
    // so that a local copy of weights can be stored...
    Dataset weightedData = new BasicDataset();
    for (Example.Looper i=dataset.iterator(); i.hasNext(); ) {
      Example e =  i.nextExample(); 
      weightedData.add( new Example(e.asInstance(), e.getLabel()) );
    }
		
    List classifiers = new ArrayList(maxRounds); 

    ProgressCounter pc = new ProgressCounter("boosting","round",maxRounds);

    // boost 
    for (int t=0; t<maxRounds; t++) {

      log.info("Adaboost is starting round "+(t+1)+"/"+maxRounds);
      log.info("Learning classifier with "+baseLearner);
      BinaryClassifier c = (BinaryClassifier)baseLearner.batchTrain(weightedData);
      classifiers.add(c);

      // re-weight data, assuming score of classifier is as required by the booster

      if (log.isDebugEnabled()) log.debug("classifier is "+c);
      log.info("Generating new distribution");
      double z = 0; // normalization factor
      for (Example.Looper k = weightedData.iterator(); k.hasNext(); ) {
        Example xk = k.nextExample();
        double yk = xk.getLabel().numericLabel();
        double yhatk = c.score(xk);
        // for Adaboost.L, multiply weight for example xk by 1/( 1 + exp( yk * c.score(xk) ) )
        // for Adaboost, multiply weight for example xk by 1/exp( yk * c.score(xk))  
        //double factor =  Math.exp( yk * yhatk );
        //double w = xk.getWeight();
        //xk.setWeight( w/factor );
        xk.setWeight( xk.getWeight() / discountFactor(yk, yhatk) );
        z += xk.getWeight();
      }
      for (Example.Looper i=weightedData.iterator(); i.hasNext(); ) {
        Example e = i.nextExample(); 
        e.setWeight( e.getWeight() / z );
      }
      pc.progress();
    }

    pc.finished();
    return new BoostedClassifier(classifiers);
  }

  protected double discountFactor(double y,double yhat)
  {
    return Math.exp( y*yhat );
  }

  /**
   * A set of boosted weak classifiers.
   */
  private static class BoostedClassifier extends BinaryClassifier implements Serializable,Visible {
    private List classifiers;
    public BoostedClassifier(List classifiers) 
    {
      this.classifiers = classifiers;
    }
    public double score(Instance instance) 
    { 
      double totalScore = 0;
      for (Iterator i=classifiers.iterator(); i.hasNext(); ) {
        BinaryClassifier c = (BinaryClassifier)i.next();
        totalScore += c.score(instance);
      }
      return totalScore;
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
    public String toString() 
    {
      StringBuffer buf = new StringBuffer("[boosted classifier:\n");			
      for (Iterator i=classifiers.iterator(); i.hasNext(); ) {
        BinaryClassifier c = (BinaryClassifier)i.next();
        buf.append(c.toString()+"\n");
      }
      buf.append("]");
      return buf.toString();
    }
    public Viewer toGUI()
    {
      Viewer v = new BoostedClassifierViewer();
      v.setContent(this);
      return v;
    }
  }

  private static class BoostedClassifierViewer extends ComponentViewer 
  {
    public JComponent componentFor(Object o) {
      BoostedClassifier bc = (BoostedClassifier)o;
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

