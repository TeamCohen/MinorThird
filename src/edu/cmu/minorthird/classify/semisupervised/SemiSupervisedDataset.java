package edu.cmu.minorthird.classify.semisupervised;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.Saveable;

import javax.swing.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.awt.*;

/**
 * @author Edoardo Airoldi
 * Date: Jul 19, 2004
 */

public class SemiSupervisedDataset implements Dataset,SemiSupervisedActions,Visible,Saveable
{
   protected ArrayList examples = new ArrayList();
   protected ArrayList unlabeledExamples = new ArrayList();
   protected Set classNameSet = new TreeSet();
   protected FeatureFactory factory = new FeatureFactory();

   public ExampleSchema getSchema()
   {
      ExampleSchema schema = new ExampleSchema((String[])classNameSet.toArray(new String[classNameSet.size()]));
      if (schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA)) return ExampleSchema.BINARY_EXAMPLE_SCHEMA;
      else return schema;
   }

   //
   // methods for semi-supervised data,  part of the SemiSupervisedDataset interface
   //
   public void addUnlabeled(Instance instance) { unlabeledExamples.add( factory.compress(instance) ); }
   public Instance.Looper iteratorOverUnlabeled() { return new Instance.Looper( unlabeledExamples ); }
   //public ArrayList getUnlabeled() { return this.unlabeledExamples; }
   public int sizeUnlabeled() { return unlabeledExamples.size(); }
   public boolean hasUnlabeled() { return (unlabeledExamples.size()>0)? true : false; }


   //
   // methods for labeled data,  part of the Dataset interface
   //
   public void add(Example example)
   {
      examples.add( factory.compress(example) );
      classNameSet.addAll( example.getLabel().possibleLabels() );
   }

   public Example.Looper iterator()
   {
      return new Example.Looper( examples );
   }

   public int size()
   {
      return examples.size();
   }

   public void shuffle(Random r)
   {
      Collections.shuffle(examples,r);
   }

   public void shuffle()
   {
      shuffle(new Random(999));
   }

   public Dataset shallowCopy()
   {
      Dataset copy = new BasicDataset();
      for (Example.Looper i=iterator(); i.hasNext(); ) {
         copy.add(i.nextExample());
      }
      return copy;
   }

   //
   // Implement Saveable interface.
   //
   static private final String FORMAT_NAME = "Minorthird Dataset";
   public String[] getFormatNames() { return new String[] {FORMAT_NAME}; }
   public String getExtensionFor(String s) { return ".data"; }
   public void saveAs(File file,String format) throws IOException
   {
      if (!format.equals(FORMAT_NAME)) throw new IllegalArgumentException("illegal format "+format);
      DatasetLoader.save(this,file);
   }
   public Object restore(File file) throws IOException
   {
      try {
         return DatasetLoader.loadFile(file);
      } catch (NumberFormatException ex) {
         throw new IllegalStateException("error loading from "+file+": "+ex);
      }
   }

   /** A string view of the dataset */
   public String toString()
   {
      StringBuffer buf = new StringBuffer("");
      for (Example.Looper i=this.iterator(); i.hasNext(); ) {
         Example ex = i.nextExample();
         buf.append( ex.toString() );
         buf.append( "\n" );
      }
      return buf.toString();
   }

   /** A GUI view of the dataset. */
   public Viewer toGUI()
   {
      Viewer dbGui = new BasicDataset.SimpleDatasetViewer();
      dbGui.setContent(this);
      Viewer instGui = GUI.newSourcedExampleViewer();
      return new ZoomedViewer(dbGui,instGui);
   }

   public static class SimpleDatasetViewer extends ComponentViewer
   {
      public boolean canReceive(Object o) {
         return o instanceof Dataset;
      }
      public JComponent componentFor(Object o) {
         final Dataset d = (Dataset)o;
         final Example[] tmp = new Example[d.size()];
         int k=0;
         for (Example.Looper i=d.iterator(); i.hasNext(); ) {
            tmp[k++] = i.nextExample();
         }
         final JList jList = new JList(tmp);
         jList.setCellRenderer( new ListCellRenderer() {
            public Component getListCellRendererComponent(JList el,Object v,int index,boolean sel,boolean focus){
               return GUI.conciseExampleRendererComponent((Example)tmp[index],60,sel);
            }});
         monitorSelections(jList);
         return new JScrollPane(jList);
      }
   }

   //
   // splitter
   //

   public Split split(final Splitter splitter)
   {
      splitter.split(examples.iterator());
      return new Split() {
         public int getNumPartitions() { return splitter.getNumPartitions(); }
         public Dataset getTrain(int k) { return invertIteration(splitter.getTrain(k)); }
         public Dataset getTest(int k) { return invertIteration(splitter.getTest(k)); }
      };
   }
   private Dataset invertIteration(Iterator i)
   {
      BasicDataset copy = new BasicDataset();
      while (i.hasNext()) copy.add((Example)i.next());
      return copy;
   }


   //
   // test routine
   //

   /** Simple test routine */
   static public void main(String[] args)
   {
      try {
         BasicDataset data = (BasicDataset)SampleDatasets.sampleData("toy",false);
         ViewerFrame f = new ViewerFrame("Toy Dataset",data.toGUI());
         System.out.println(data.getSchema());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
