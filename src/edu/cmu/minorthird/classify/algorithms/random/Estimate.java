package edu.cmu.minorthird.classify.algorithms.random;

import edu.cmu.minorthird.classify.Feature;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Edoardo Airoldi
 * Date: Dec 11, 2004
 */

public class Estimate
{
   private String model;
   private String parameterization;
   private TreeMap pms;

   public Estimate(String mod, String param, TreeMap pms)
   {
      this.model = mod;
      this.parameterization = param;
      this.pms = pms;
   }

   public String getModel() { return this.model; }
   public String getParameterization() { return this.parameterization; }
   public TreeMap getPms() { return this.pms; }

   public String toString()
   {
      StringBuffer buf = new StringBuffer("[ ");
      buf.append("model="+model+", "+"parameterization="+parameterization+" : ");
      Set names= pms.keySet();
      for (Iterator i=names.iterator(); i.hasNext();)
      {
         String key = (String)i.next();
         double value = ((Double)pms.get(key)).doubleValue();
         buf.append(key+"="+value+", ");
      }
      buf.delete(buf.length()-2,buf.length());
      buf.append(" ]");
      return buf.toString();
   }

   public String toTableInViewer()
   {
      StringBuffer buf = new StringBuffer("");
      buf.append(model);
      Set names= pms.keySet();
      for (Iterator i=names.iterator(); i.hasNext();)
      {
         String key = (String)i.next();
         double value = ((Double)pms.get(key)).doubleValue();
         buf.append(", "+key+"="+value);
      }
      return buf.toString();
   }


   // Test Estimate
   static public void main(String[] args)
   {
      TreeMap mudelta = new TreeMap();
      mudelta.put( "mu",new Double(0.661) );
      mudelta.put( "delta",new Double(0.035) );
      Estimate theta = new Estimate("Binomial","mu/delta",mudelta);
      System.out.println(theta.toString());
      System.out.println("|"+theta.toTableInViewer()+"|");
      TreeMap pn = new TreeMap();
      pn.put( "p",new Double(0.661) );
      pn.put( "N",new Double(11) );
      Estimate gamma = new Estimate("Binomial","p/N",pn);
      System.out.println(gamma.toString());
      System.out.println("|"+gamma.toTableInViewer()+"|");
   }
}
