package edu.cmu.minorthird.classify.algorithms.linear;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.algorithms.random.Arithmetic;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.MathUtil;

import java.io.Serializable;
import java.util.*;

import org.apache.log4j.Logger;

import javax.swing.*;

import gnu.trove.TObjectDoubleIterator;

/** A generative Model for word-counts based on the Negative-Binomial Distribution.
 *
 * @author Edoardo Airoldi
 * Date: Jul 12, 2004
 */

public class NegativeBinomialClassifier extends BinaryClassifier implements Visible, Serializable
{

   static private Logger log = Logger.getLogger(PoissonClassifier.class);

   private double SCALE;    // gets initialized by the NB learner
   private double priorPos; // ...
   private double priorNeg; // ...
   private TreeMap pmsFeatureGivenPos;
   private TreeMap pmsFeatureGivenNeg;

   public NegativeBinomialClassifier() {
      this.pmsFeatureGivenNeg = new TreeMap();
      this.pmsFeatureGivenPos = new TreeMap();
   }

   /** Inner product of PoissonClassifier and instance weights. */
   public double score(Instance instance)
   {
      double totCnt = 0.0;
      for ( Feature.Looper i=instance.featureIterator(); i.hasNext(); )
      {
         Feature f = i.nextFeature();
         totCnt += instance.getWeight(f);
      }
      double score = 0.0;
      for ( Feature.Looper i=instance.featureIterator(); i.hasNext(); )
      {
         Feature f = i.nextFeature();
         score += logOddsNB( f,instance.getWeight(f),totCnt/SCALE );
      }
      score += +Math.log(priorPos/priorNeg);
      return score;
   }

   /** Justify inner product of Negative-Binomial Classifier and instance weights. */
   public String explain(Instance instance)
   {
      double totCnt = 0.0;
      for ( Feature.Looper i=instance.featureIterator(); i.hasNext(); )
      {
         Feature f = i.nextFeature();
         totCnt += instance.getWeight(f);
      }
      // explain
      StringBuffer buf = new StringBuffer("");
      for ( Feature.Looper i=instance.featureIterator(); i.hasNext(); )
      {
         Feature f = i.nextFeature();

         // retrieve parameters
         double mNeg, dNeg, mPos, dPos, x;
         try
         {
            x = instance.getWeight(f);
            TreeMap mdn = (TreeMap) pmsFeatureGivenNeg.get(f);
            mNeg = ((Double)mdn.get("mu")).doubleValue();
            dNeg = ((Double)mdn.get("delta")).doubleValue();
            TreeMap mdp = (TreeMap) pmsFeatureGivenPos.get(f);
            mPos = ((Double)mdp.get("mu")).doubleValue();
            dPos = ((Double)mdp.get("delta")).doubleValue();

            if (buf.length()>0) buf.append(" + ");
            buf.append(f+" <"+x+"*"+(Math.log(mPos/mNeg))+"-"+(totCnt/SCALE)+"*"+(+mPos-mNeg)+">");
         }
         catch (Exception e)
         {
            System.out.println("warning:"+e);
         }
      }
      buf.append(" + bias<"+Math.log(priorPos/priorNeg)+">");
      buf.append(" = "+score(instance) );
      return buf.toString();

   }



   //
   // Get, Set, Check, ...
   //

   /** Set the scale term for the NB classifier to value */
   public void setScale(double value)
   {
      this.SCALE = value;
   }

   /** Set the prior for positive documents */
   public void setPriorPos(double k,double n, double prior, double pseudoCounts)
   {
      //System.out.println( ". "+Math.log((k+prior*pseudoCounts) / (n+pseudoCounts) ) );
      this.priorPos = (k+prior*pseudoCounts) / (n+pseudoCounts);
   }

   /** Set the prior for negative documents */
   public void setPriorNeg(double k,double n, double prior, double pseudoCounts)
   {
      //System.out.println( ". "+Math.log((k+prior*pseudoCounts) / (n+pseudoCounts) ) );
      this.priorNeg = (k+prior*pseudoCounts) / (n+pseudoCounts);
   }

   /** compute log-odds for feature f with x counts, in an instance of weight w */
   private double logOddsNB(Feature f, double x, double w)
   {
      // retrieve parameters
      double mNeg, dNeg, mPos, dPos, logOdds;
      try
      {
         TreeMap mdn = (TreeMap) pmsFeatureGivenNeg.get(f);
         mNeg = ((Double)mdn.get("mu")).doubleValue();
         dNeg = ((Double)mdn.get("delta")).doubleValue();
         TreeMap mdp = (TreeMap) pmsFeatureGivenPos.get(f);
         mPos = ((Double)mdp.get("mu")).doubleValue();
         dPos = ((Double)mdp.get("delta")).doubleValue();

         // compute log-odds
         if (dPos==0.0 || dNeg==0.0)
         {
            logOdds = x*(Math.log(mPos/mNeg)) -w*(mPos-mNeg);
         }
         else
         {
            logOdds = Arithmetic.logGamma(x+mPos/dPos) -Arithmetic.logGamma(mPos/dPos)
                -Arithmetic.logGamma(x+mNeg/dNeg) +Arithmetic.logGamma(mNeg/dNeg)
                +x*Math.log(dPos/dNeg) -x*Math.log((1.0+w*dPos)/(1.0+w*dNeg));
         }
         //if ( new Double(logOdds).isNaN() ) { logOdds = 0.0; }
      }
      catch (Exception e)
      {
         logOdds = 0.0;
      }
      return logOdds;
   }

   /** Store parameters for f|negative */
   public void setPmsNeg(Feature f, TreeMap tmap)
   {
      pmsFeatureGivenNeg.put(f,tmap);
   }

   /** Store parameters for f|positive */
   public void setPmsPos(Feature f, TreeMap tmap)
   {
      pmsFeatureGivenPos.put(f,tmap);
   }

   private double featureScore(Feature f, String p, String c)
   {
      double value = 0.0;
      try
      {
         if ( c.equals("POS") )
         {
            value = ((Double)((TreeMap)pmsFeatureGivenPos.get(f)).get(p)).doubleValue();
         }
         else if ( c.equals("NEG") )
         {
            value = ((Double)((TreeMap)pmsFeatureGivenNeg.get(f)).get(p)).doubleValue();
         }
      }
      catch (Exception e)
      {
         System.out.println("error: ... in NB.toGui.featureScore("+f+","+p+","+c+")");
         System.exit(1);
      }
      return value;
   }

   public Feature.Looper featureIterator()
   {
      final Iterator ti = pmsFeatureGivenPos.keySet().iterator();
      Iterator i = new Iterator() {
         public boolean hasNext() { return ti.hasNext(); }
         public Object next() { return ti.next(); }
         public void remove() { ti.remove(); }
      };
      return new Feature.Looper(i);
   }



   //
   // GUI related Methods
   //

   public Viewer toGUI()
   {
      Viewer gui = new ControlledViewer(new MyViewer(), new NegBinControls());
      gui.setContent(this);
      return gui;
   }

   static private class NegBinControls extends ViewerControls
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
      private NegBinControls controls = null;
      private NegativeBinomialClassifier h  = null;

      public void applyControls(ViewerControls controls)
      {
         this.controls = (NegBinControls)controls;
         setContent(h,true);
         revalidate();
      }
      public boolean canReceive(Object o) {	return o instanceof Hyperplane;	}

      public JComponent componentFor(Object o)
      {
         h = (NegativeBinomialClassifier)o;
         //
         // Note: if transorming batch learner is used only
         //       tableData[.][1] gets displayed.
         //
         Object[] keys = h.pmsFeatureGivenNeg.keySet().toArray();
         Object[][] tableData = new Object[keys.length][5];
         int k=0;
         for (Feature.Looper i=h.featureIterator(); i.hasNext(); ) {
            Feature f = i.nextFeature();
            tableData[k][0] = f;
            tableData[k][1] = new Double( h.featureScore( f,"mu","NEG" ) );
            tableData[k][2] = new Double( h.featureScore( f,"delta","NEG" ) );
            tableData[k][3] = new Double( h.featureScore( f,"mu","POS" ) );
            tableData[k][4] = new Double( h.featureScore( f,"delta","POS" ) );
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
         String[] columnNames = {"Feature Name", "mu Neg", "delta Neg", "mu Pos", "delta Pos"};
         JTable table = new JTable(tableData,columnNames);
         monitorSelections(table,0);
         return new JScrollPane(table);
      }
   }


   public String toString() {
      String a = pmsFeatureGivenNeg.toString();
      String b = pmsFeatureGivenPos.toString();
      return ( "Neg: "+a + "\n" + "Pos: "+b );
   }
}


