/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.StackedGraphicalLearning;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;

/**
 * A real set of examples for stached graphical learning -- coreset + relational template.
 * Currently the legalAggregators include EXISTS and COUNT
 * @author Zhenzhen Kou
 */

public class RealRelationalDataset extends CoreRelationalDataset implements Visible,Saveable,Dataset,Serializable
{
   
	 protected static HashMap Aggregators = new HashMap();
	 
    public static Set legalAggregators = new HashSet(); 
    static { 
			legalAggregators.add("EXISTS"); 
			legalAggregators.add("COUNT"); 
    }

    /**
     * Add an aggregator, i.e., save the info. in RelTemp sccript
     *
     * @param String oper, String L_type.
     */
    static public void addAggregator(String oper, String L_type) {
			
			if( legalAggregators.contains(oper) ){
				if(Aggregators.containsKey(L_type)){
					((HashSet)Aggregators.get(L_type)).add(oper);
				}else{
					HashSet hs = new HashSet();
					hs.add(oper);
					Aggregators.put(L_type, hs.clone());
				}
				
			}else{
				System.out.println(oper + " is not a legal aggregator");
			}
		return;

    }

		public HashMap getAggregators(){return Aggregators; }
		public void setAggregators( HashMap inputAggregators){
			this.Aggregators=inputAggregators;
			
		}

   public Split split(final Splitter splitter)
   {
      splitter.split(examples.iterator());
      return new Split() {
         public int getNumPartitions() { return splitter.getNumPartitions(); }
         public Dataset getTrain(int k) { return invertIteration(splitter.getTrain(k)); }
         public Dataset getTest(int k) { return invertIteration(splitter.getTest(k)); }
      };
   }
   private RealRelationalDataset invertIteration(Iterator i)
   {
      RealRelationalDataset copy = new RealRelationalDataset();
      while (i.hasNext()) copy.addSGM((SGMExample)i.next());
      copy.setAggregators(this.Aggregators);
			copy.setLinksMap(this.getLinksMap());
      return copy;
   }   
   


   
   
   //
   // test routine
   //

   /** Simple test routine */
   static public void main(String[] args)
   {
      try {
         RealRelationalDataset data = new RealRelationalDataset();
         DatasetLoader.loadRelFile(new File("test.osf"), data);
         DatasetLoader.loadLinkFile(new File("test.lsf"), data);
         DatasetLoader.loadRelTempFile(new File("relTemp.txt"), data);
//         ViewerFrame f = new ViewerFrame("Toy Dataset",data.toGUI());
         System.out.println(data.getSchema());
/*         addAggregator( "COUNT","left");
         addAggregator( "COUNT", "right");
         addAggregator( "EXISTS", "right");
         addAggregator( "EXIST", "right");
*/         System.out.println("Links: "+data.getLinksMap());
         System.out.println("aggregators: "+data.getAggregators());
         for ( Iterator i= data.examples.iterator(); i.hasNext(); ){
						SGMExample e=(SGMExample)i.next();
		         System.out.println(e);
         
        }
                 
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
