/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.random.Estimate;
import edu.cmu.minorthird.classify.algorithms.random.Arithmetic;

import java.io.Serializable;
import java.util.*;

import org.apache.log4j.Logger;

import javax.swing.*;

import gnu.trove.TObjectDoubleIterator;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author Edoardo Airoldi
 * Date: Mar 15, 2004
 */

public class MultinomialClassifier implements Classifier, Visible,Serializable
{

   static private Logger log = Logger.getLogger(MultinomialClassifier.class);

   private double SCALE; // set by learner if needed
   private ArrayList classNames;
   private ArrayList classParameters;
   private HashMap featureModels;
   private ArrayList featureGivenClassParameters;
   private double featurePrior;
   private String unseenModel;

   // constructor
   public MultinomialClassifier()
   {
      this.classNames = new ArrayList();
      this.classParameters = new ArrayList();
      this.featureModels = new HashMap();
      this.featureGivenClassParameters = new ArrayList();
      this.featureGivenClassParameters.add( new WeightedSet() );
      this.featurePrior = 0.0;
      this.unseenModel = null;
   }


   //
   // methods in Classifier interface
   //
   public ClassLabel classification(Instance instance)
   {
      double[] score = score(instance);
      //System.out.println("size="+score.length);
      int maxIndex = 0;
      for (int i=0; i<score.length; i++)
      {
         //System.out.println("i="+i+" score="+score[i]);
         if ( score[i]>=score[maxIndex] )
         {
            maxIndex=i;
         }
      }
      return new ClassLabel( (String)classNames.get(maxIndex) );
   }

   public double[] score(Instance instance)
   {

      // compute example weight
      double exampleWeight = 0.0;
      for ( Feature.Looper j=instance.featureIterator(); j.hasNext(); )
      {
         Feature f = j.nextFeature();
         exampleWeight += instance.getWeight(f);
      }
      //System.out.println("id="+instance.getSource());

      double[] score = new double[classNames.size()];
      for (int i=0; i<classNames.size(); i++)
      {
         double classProb = ((Double)classParameters.get(i)).doubleValue();
         score[i] = Math.log(classProb);
      }

      for (Feature.Looper j=instance.featureIterator(); j.hasNext(); )
      {
         Feature f = j.nextFeature();
         double featureCounts = instance.getWeight(f);

         for (int i=0; i<classNames.size(); i++)
         {
            Estimate featureProb = (Estimate) ((HashMap)featureGivenClassParameters.get(i)).get(f);
            String model="";
            try { model=featureProb.getModel(); } catch (NullPointerException e) { model="unseen"; }

            if ( model.equals("Poisson") )
            {
               //score[i] += -featureProb*total/SCALE +featureCounts*Math.log(featureProb);  // try
               String parameterization = featureProb.getParameterization();
               if (parameterization.equals("weighted-lambda"))
               {
                  TreeMap pms = featureProb.getPms();
                  double lambda = ((Double)pms.get("lambda")).doubleValue();
                  score[i] += -lambda*exampleWeight/SCALE +featureCounts*Math.log(lambda);
               }
               else if (parameterization.equals("lambda"))
               {
                  TreeMap pms = featureProb.getPms();
                  double lambda = ((Double)pms.get("lambda")).doubleValue();
                  score[i] += -lambda*exampleWeight/SCALE +featureCounts*Math.log(lambda);
                  //System.out.println("ft = "+f+" :: counts = "+featureCounts+", mu["+i+"] = "+lambda+", log(mu["+i+"]) = "+Math.log(lambda)+", w = "+total+", scale = "+SCALE);
               }
            }
            else if ( model.equals("Naive-Bayes") )
            {
               //score[i] += featureCounts*Math.log(featureProb); //try
               String parameterization = featureProb.getParameterization();
               if (parameterization.equals("weighted-mean"))
               {
                  TreeMap pms = featureProb.getPms();
                  double mean = ((Double)pms.get("mean")).doubleValue();
                  score[i] += featureCounts*Math.log(mean);
               }
               else if (parameterization.equals("mean"))
               {
                  TreeMap pms = featureProb.getPms();
                  double mean = ((Double)pms.get("mean")).doubleValue();
                  score[i] += featureCounts*Math.log(mean);
               }
            }
            else if ( model.equals("Negative-Binomial") )
            {
               String parameterization = featureProb.getParameterization();
               if (parameterization.equals("mu/delta"))
               {
                  TreeMap pms = featureProb.getPms();
                  score[i] += logProbNegativeBinomialMuDelta( featureCounts,exampleWeight/SCALE,pms );
                  //System.out.println("f="+f+" :: ["+i+"] :: log-odds = "+score[i]+"\n");
               }
            }
            else if ( model.equals("Binomial") )
            {
               String parameterization = featureProb.getParameterization();
               if (parameterization.equals("p/N"))
               {
                  TreeMap pms = featureProb.getPms();
                  score[i] += logProbBinomialPN( featureCounts,exampleWeight/SCALE,pms );
                  //System.out.println("score["+i+"] = "+score[i]);
               }
               else if (parameterization.equals("mu/delta"))
               {
                  TreeMap pms = featureProb.getPms();
                  score[i] += logProbBinomialMuDelta( featureCounts,exampleWeight/SCALE,pms );
               }
            }
            else if ( model.equals("Dirichlet-Poisson MCMC") )
            {
               //score[i] += -featureProb*total/SCALE +featureCounts*Math.log(featureProb);  // try
               String parameterization = featureProb.getParameterization();
               if (parameterization.equals("weighted-lambda"))
               {
                  TreeMap pms = featureProb.getPms();
                  double lambda = ((Double)pms.get("lambda")).doubleValue();
                  score[i] += -lambda*exampleWeight/SCALE +featureCounts*Math.log(lambda);
               }
               else if (parameterization.equals("lambda"))
               {
                  TreeMap pms = featureProb.getPms();
                  double lambda = ((Double)pms.get("lambda")).doubleValue();
                  score[i] += -lambda*exampleWeight/SCALE +featureCounts*Math.log(lambda);
               }
            }
            else if ( model.equals("unseen") )
            {
               score[i] += 0.0;
            }
            else
            {
               System.out.println("error: model "+model+" not found!");
               System.exit(1);
            }
         }
      }
      return score;
   }

   /** compute log-prob for feature f with x counts, in an instance of weight w */
   private double logProbNegativeBinomialMuDelta (double x, double w, TreeMap pms)
   {
      // retrieve parameters
      double m, d, logProb;
      try
      {
         m = ((Double)pms.get("mu")).doubleValue();
         d = ((Double)pms.get("delta")).doubleValue();
         //System.out.println(" m:"+m+" d:"+d+" x:"+x+" w:"+w);

         // compute log-prob
         if (d==0.0)
         {
            logProb = x*Math.log(m) -w*m;
            // = sum_f { -mu(+) +mu(-) + f_counts * [ log mu(+) - log mu(-) ] } + log Pr(+) - log Pr(-)
         }
         else //if (false)
         {
            logProb = Arithmetic.logGamma(x+m/d) -Arithmetic.logGamma(m/d)
                +x*Math.log(d) -x*Math.log(1.0+w*d);
         }
         //if ( new Double(logProb).isNaN() ) { logOdds = 0.0; }
      }
      catch (Exception e)
      {
         //System.out.println("warning: feature \""+f+"\" not in training set!");
         logProb = 0.0;
      }
      return logProb;
   }

   /** compute log-prob for feature f with x counts, in an instance of weight 1 */
   private double logProbBinomialPN (double x, double w, TreeMap pms)
   {
      double p, N, logProb=0.0;

      try{
         p = ((Double)pms.get("p")).doubleValue();
         N = ((Double)pms.get("N")).doubleValue();
         //System.out.println("p="+p+" N="+N);

         if (N==0.0)
         {
            // compute log-prob using Poisson model
            logProb = x*Math.log(p) -w*p;
         }
         else
         {

            // compute log-prob
            logProb = Arithmetic.logFactorial( (int)N ) -Arithmetic.logFactorial( ((int)N-(int)x) )
                +x*Math.log(p) +(N-x)*Math.log((1-p));
         }
      } catch (Exception e) {
         logProb = 0.0;
      }
      //System.out.println("logProb="+logProb);
      return logProb;
   }

   /** compute log-prob for feature f with x counts, in an instance of weight w */
   private double logProbBinomialMuDelta (double x, double w, TreeMap pms)
   {
      double m, d, logProb=0.0;

      try{
         m = ((Double)pms.get("mu")).doubleValue();
         d = ((Double)pms.get("delta")).doubleValue();

         if (d==0)
         {
            // compute log-prob using Poisson model
            logProb = x*Math.log(m) -w*m;
         }
         else
         {
            double N = Math.round(Math.max(m/d,x));
            double p = Math.min( Math.max(1e-7,w*d),1-1e-7 ); // not correct

            // compute log-prob
            logProb = Arithmetic.logGamma(N+1.0) -Arithmetic.logGamma(N-x+1.0)
                +x*Math.log(d) -x*Math.log((1.0-p)) +N*Math.log(1.0-p);
         }
      } catch (Exception e) {
         logProb = 0.0;
      }
      return logProb;
   }

   public String explain(Instance instance)
   {
      StringBuffer buf = new StringBuffer("");
      for (Feature.Looper j=instance.featureIterator(); j.hasNext(); ) {
         Feature f = j.nextFeature();
         if (buf.length()>0) buf.append("\n + ");
         else buf.append("   ");
         //buf.append( f+"<"+instance.getWeight(f)+"*"+featureScore(f)+">");
      }
      //buf.append( "\n + bias<"+featureScore( BIAS_TERM )+">" );
      buf.append("\n = "+score(instance) );
      return buf.toString();
   }


   //
   // Get, Set, Check
   //
   public void setScale(double value)
   {
      this.SCALE = value;
   }

   public void setPrior(double pi)
   {
      this.featurePrior = pi;
   }

   public void setUnseenModel(String str)
   {
      this.unseenModel = str;
   }

   public double getLogLikelihood(Example example)
   {
      //System.out.println( example );
      int idx = -1;
      for (int i=0; i<classNames.size(); i++)
      {
         if ( classNames.get(i).equals(example.getLabel().bestClassName()) )
         {
            idx = i;
            break;
         }
      }
      //System.out.println( "class="+classNames.get(idx) );
      Instance instance = example.asInstance();
      double loglik = 0.0;
      //System.out.println("instance="+instance);
      for (Feature.Looper j=instance.featureIterator(); j.hasNext(); ) {
         Feature f = j.nextFeature();
         double featureCounts = instance.getWeight(f);
         double featureProb = ((WeightedSet)featureGivenClassParameters.get(idx)).getWeight(f);
         double classProb = ((Double)classParameters.get(idx)).doubleValue();
         //System.out.println("feature="+f+" counts="+featureCounts+" prob="+featureProb+" class="+classProb);

         String model = getFeatureModel(f);
         if ( model.equals("Poisson") )
         {
            loglik += -featureProb +featureCounts*Math.log(featureProb);
         }
         else if ( model.equals("Naive-Bayes") )
         {
            loglik += featureCounts*Math.log(featureProb);
         }
         else if ( model.equals("unseen") )
         {
            System.out.println("unseen: "+f);
         }
         else
         {
            System.out.println("error: model "+model+" not found!");
            System.exit(1);
         }
      }
      return loglik;
   }

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

   public void setFeatureGivenClassParameter(Feature f, int j, Estimate pms)
   {
      HashMap hmap;
      try
      {
         hmap = (HashMap)featureGivenClassParameters.get(j);
         hmap.put( f,pms );
         featureGivenClassParameters.set( j,hmap );
      }
      catch (Exception NoHashMapforClassJ)
      {
         hmap = null;
         hmap = new HashMap();
         hmap.put( f,pms ) ;
         featureGivenClassParameters.add( j,hmap );
      }
   }

   public void setFeatureGivenClassParameter(Feature f, int j, double probabilityOfOccurrence)
   {
      System.out.println("Should not happen!");
   }

   public void setClassParameter(int j, double probabilityOfOccurrence)
   {
      try {
         classParameters.get( j );
      } catch (Exception x) {
         classParameters.add( j,new Double(probabilityOfOccurrence) );
         //System.out.println(". added in "+j+" >> pi="+probabilityOfOccurrence);
      }
   }

   public void setFeatureModel(Feature feature, String model)
   {
      featureModels.put( feature,model );
   }

   public String getFeatureModel(Feature feature)
   {
      try
      {
         String model = featureModels.get(feature).toString();
         return model;
      }
      catch (NullPointerException x)
      {
         return "unseen";
      }
   }

   public Feature.Looper featureIterator()
   {
      // 1. create a new WeightedSet with all features
      TObjectDoubleHashMap map = new TObjectDoubleHashMap();
      for (int i=0; i<classNames.size(); i++)
      {
         HashMap hmap = (HashMap) featureGivenClassParameters.get(i);
         for (Iterator j=hmap.keySet().iterator(); j.hasNext();)
         {
            Feature f = (Feature)j.next();
            double w = 0.0;
            map.put(f,w);
         }
      }
      // 2. create global feature iterator
      final TObjectDoubleIterator ti = map.iterator();
      Iterator i = new Iterator() {
         public boolean hasNext() { return ti.hasNext(); }
         public Object next() { ti.advance(); return ti.key(); }
         public void remove() { ti.remove(); }
      };
      return new Feature.Looper(i);
   }

   public Object[] keys()
   {
      TObjectDoubleHashMap map = new TObjectDoubleHashMap();
      for (int i=0; i<classNames.size(); i++)
      {
         HashMap hmap = (HashMap) featureGivenClassParameters.get(i);
         for (Iterator j=hmap.keySet().iterator(); j.hasNext();)
         {
            Feature f = (Feature)j.next();
            double w = 0.0;
            map.put(f,w);
         }
      }
      return map.keys();
   }

   /*public Feature.Looper featureIterator()
   {
      // 1. create a new WeightedSet with all features
      TObjectDoubleHashMap map = new TObjectDoubleHashMap();
      for (int i=0; i<classNames.size(); i++)
      {
         WeightedSet wset = (WeightedSet)featureGivenClassParameters.get(i);
         for (Iterator j=wset.iterator(); j.hasNext();)
         {
            Feature f = (Feature)j.next();
            double w = wset.getWeight(f);
            map.put(f,w);
         }
      }
      // 2. create global feature iterator
      final TObjectDoubleIterator ti = map.iterator();
      Iterator i = new Iterator() {
         public boolean hasNext() { return ti.hasNext(); }
         public Object next() { ti.advance(); return ti.key(); }
         public void remove() { ti.remove(); }
      };
      return new Feature.Looper(i);
   }

   public Object[] keys()
   {
      TObjectDoubleHashMap map = new TObjectDoubleHashMap();
      for (int i=0; i<classNames.size(); i++)
      {
         WeightedSet wset = (WeightedSet)featureGivenClassParameters.get(i);
         for (Iterator j=wset.iterator(); j.hasNext();)
         {
            Feature f = (Feature)j.next();
            double w = wset.getWeight(f);
            map.put(f,w);
         }
      }
      return map.keys();
   }*/


   //
   // GUI related stuff
   //
   public Viewer toGUI()
   {
      Viewer gui = new ControlledViewer(new MyViewer(), new MultinomialClassifierControls());
      gui.setContent(this);
      return gui;
   }

   static private class MultinomialClassifierControls extends ViewerControls
   {
      // how to sort
      private JRadioButton absoluteValueButton,valueButton,nameButton,noneButton;
      public void initialize()
      {
         add(new JLabel("Sort by"));
         ButtonGroup group = new ButtonGroup();;
         nameButton = addButton("name",group,true);
         valueButton = addButton("weight",group,false);
         absoluteValueButton = addButton("|weight|",group,false);
      }
      private JRadioButton addButton(String s,ButtonGroup group,boolean selected)
      {
         JRadioButton button = new JRadioButton(s,selected);
         group.add(button);
         add(button);
         button.addActionListener(this);
         return button;
      }
   }

   static private class MyViewer extends ComponentViewer implements Controllable
   {
      private MultinomialClassifierControls controls = null;
      private MultinomialClassifier h = null;

      public void applyControls(ViewerControls controls)
      {
         this.controls = (MultinomialClassifierControls)controls;
         setContent(h,true);
         revalidate();
      }
      public boolean canReceive(Object o) {	return o instanceof MultinomialClassifier;	}

      public JComponent componentFor(Object o)
      {
         h = (MultinomialClassifier)o;
         Object[] keys = h.keys();
         Object[][] tableData = new Object[keys.length][(h.classNames.size()+1)];
         int k=0;
         for (Feature.Looper i=h.featureIterator(); i.hasNext(); ) {
            Feature f = i.nextFeature();
            tableData[k][0] = f;
            for (int l=0; l<h.classNames.size(); l++)
            {
               String content =
                   ((Estimate)((HashMap)h.featureGivenClassParameters.get(l)).get(f)).toTableInViewer();
               tableData[k][(l+1)] = content;
               //tableData[k][(l+1)] = new Double( ((WeightedSet)h.featureGivenClassParameters.get(l)).getWeight(f) );
            }
            k++;
         }
         if (controls!=null) {
            Arrays.sort(
                tableData,
                new Comparator() {
                   public int compare(Object a,Object b) {
                      Object[] ra = (Object[])a;
                      Object[] rb = (Object[])b;
                      if (controls.nameButton.isSelected())
                         return ra[0].toString().compareTo(rb[0].toString());
                      Double da = (Double)ra[1];
                      Double db = (Double)rb[1];
                      if (controls.valueButton.isSelected())
                         return MathUtil.sign( db.doubleValue() - da.doubleValue() );
                      else
                         return MathUtil.sign( Math.abs(db.doubleValue()) - Math.abs(da.doubleValue()) );
                   }
                });
         }
         String[] columnNames = new String[(h.classNames.size()+1)];
         columnNames[0] = "Feature Name";
         for (int i=0; i<h.classNames.size(); i++)
         {
            columnNames[(i+1)] = "Class "+h.classNames.get(i);
         }
         JTable table = new JTable(tableData,columnNames);
         monitorSelections(table,0);
         return new JScrollPane(table);
      }
   }


   public String toString() { return null; }

}
