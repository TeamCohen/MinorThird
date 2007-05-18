/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.relational;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;

/**
 * A core set of examples for stached graphical learning -- dataset + linksmap.
 *
 * @author Zhenzhen Kou
 */

public class CoreRelationalDataset extends BasicDataset implements Visible,Saveable,Dataset,Serializable
{
   
   protected static HashMap LinksMap = new HashMap();
	 protected SGMFeatureFactory factory = new SGMFeatureFactory();




    public void addSGM(SGMExample example) {
        this.addSGM(example, true);
    }
    
    /**
     * Add an Example to the dataset. <br>
     * <br>
     * This method lets the caller specify whether or not to compress the example
     * before adding it to the dataset.
     *
     * @param example The example to add to the dataset
     * @param compress Boolean specifying whether or not to compress the example.
     */
    public void addSGM (SGMExample example, boolean compress) {
        if (compress)
            examples.add(factory.compressSGM(example));
        else
            examples.add(example);
        classNameSet.addAll(example.getLabel().possibleLabels());
    }



    /**
     * Add a link to the LinksMap of the dataset. <br>
     * A link contains 'from', 'to', and 'type'
     * Please note that if the link type is 'left', it means 
     * the left ngb of 'from' is 'to'
     * @param link The Link that you want to add to the dataset.
     */
    static public void addLink(Link link) {
			String from = link.getFrom();
			String to = link.getTo();
			String type = link.getType();
			
			if( LinksMap.containsKey(from) ){
				if ( ((HashMap)LinksMap.get(from)).containsKey(type) ){
					((HashSet)((HashMap)LinksMap.get(from)).get(type)).add(to);
					
				}else{
					HashSet hs = new HashSet();
					hs.add(to);
					((HashMap)LinksMap.get(from)).put(type, hs.clone());
				}
			}else{
				HashMap hm = new HashMap();
				HashSet hs = new HashSet();
				hs.add(to); 			
				hm.put(type, hs.clone());
				LinksMap.put(from, hm.clone());
				//System.out.println("new obj link");
			}
		return;

    }

		public SGMExample getExamplewithID( String ID){
			for ( Iterator i= examples.iterator(); i.hasNext(); ){
				SGMExample e=(SGMExample)i.next();
				if( e.hasID(ID) )
					return e;
				
			} 
			return null;
		}
		public HashMap getLinksMap(){return LinksMap; }
		public void setLinksMap( HashMap inputLinksMap){
			this.LinksMap=inputLinksMap;
			
		}

   //
   // test routine
   //

   /** Simple test routine */
   static public void main(String[] args)
   {
      try {
         BasicDataset data = (BasicDataset)SampleDatasets.sampleData("toy",false);
//         ViewerFrame f = new ViewerFrame("Toy Dataset",data.toGUI());
         System.out.println(data.getSchema());
         addLink( new Link("1", "2", "left"));
         addLink( new Link("1", "3", "left"));
         addLink( new Link("2", "3", "left"));
         System.out.println(LinksMap);

      } catch (Exception e) {
         e.printStackTrace();
      }
   }



}
