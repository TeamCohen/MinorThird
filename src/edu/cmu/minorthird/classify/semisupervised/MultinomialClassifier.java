/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.semisupervised;

import edu.cmu.minorthird.util.gui.Visible;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.classify.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author Edoardo Airoldi
 * Date: Mar 15, 2004
 */

public class MultinomialClassifier implements Classifier, Visible,Serializable
{

  static private Logger log = Logger.getLogger(MultinomialClassifier.class);

  private ArrayList classNames;
  private ArrayList classParameters;
  private ArrayList featureGivenClassParameters;

  // constructor
  public MultinomialClassifier()
  {
    this.classNames = new ArrayList();
    this.classParameters = new ArrayList();
    this.featureGivenClassParameters = new ArrayList();
    this.featureGivenClassParameters.add( new WeightedSet() );
  }


  //
  // methods in Classifier interface
  //
  public ClassLabel classification(Instance instance)
  {
    double[] score = score(instance);
    int maxIndex = 0;
    for (int i=0; i<score.length; i++)
    {
      if ( score[i]>score[maxIndex] )
      {
        maxIndex=i;
      }
    }
    //System.out.println( classNames.get(0)+","+score[0]+" "+classNames.get(1)+","+score[1]);
    return new ClassLabel( (String)classNames.get(maxIndex) );
  }

  public double[] score(Instance instance)
  {
    //System.out.println( "class="+classNames.get(0)+" counts="+featureGivenClassParameters.get(0) );
    //System.out.println( "class="+classNames.get(1)+" counts="+featureGivenClassParameters.get(1) );
    double[] score = new double[classNames.size()];
    for (int i=0; i<classNames.size(); i++)
    {
      score[i] = 0.0;
      //System.out.println("instance="+instance);
      for (Feature.Looper j=instance.featureIterator(); j.hasNext(); ) {
        Feature f = j.nextFeature();
        double featureCounts = instance.getWeight(f);
        double featureProb = ((WeightedSet)featureGivenClassParameters.get(i)).getWeight(f);
        double classProb = ((Double)classParameters.get(i)).doubleValue();
        //System.out.println("feature="+f+" counts="+featureCounts+" prob="+featureProb+" class="+classProb);
        score[i] += featureCounts*Math.log(featureProb)+Math.log(classProb);
      }
    }
    return score;
  }

  public String explain(Instance instance)
  {
    return "Once upon a time ...";
  }


  //
  // Get, Set, Check
  //
  public void reset()
  {
    this.classParameters = new ArrayList();
    this.featureGivenClassParameters = new ArrayList();
    //this.featureGivenClassParameters.add( new WeightedSet() );
  }

  public boolean isPresent(ClassLabel label)
  {
    boolean isPresent = false;
    for (int i=0; i<classNames.size(); i++ )
    {
      if ( classNames.get(i).equals(label.bestClassName()) ) { isPresent= true; }
    }
    return isPresent;
  }

  public void addValidLabel(ClassLabel label)
  {
    classNames.add( label.bestClassName() );
  }

  public ClassLabel getLabel(int i)
  {
    return new ClassLabel( (String)classNames.get(i) );
  }

  public int indexOf(ClassLabel label)
  {
    return classNames.indexOf( label.bestClassName() );
  }

  public void setFeatureGivenClassParameter(Feature f, int j, double probabilityOfOccurrence)
  {
    WeightedSet wset;
    try
    {
      wset = (WeightedSet)featureGivenClassParameters.get(j);
      wset.add( f,probabilityOfOccurrence );
      featureGivenClassParameters.set( j,wset );
    }
    catch (Exception t)
    {
      wset = null;
      wset = new WeightedSet();
      wset.add( f,probabilityOfOccurrence ) ;
      featureGivenClassParameters.add( j,wset );
    }
  }

  public void setClassParameter(int j, double probabilityOfOccurrence)
  {
    classParameters.add( j,new Double(probabilityOfOccurrence) );
  }


  //
  // GUI related stuff
  //
  public Viewer toGUI()
  {
    return null;
  }

}
