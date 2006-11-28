/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;

/**
 * A set of examples for learning.
 *
 * @author Cameron Williams
 */

public class MultiDataset implements Dataset,Visible,Saveable
{
   static private final long serialVersionUID = 1;
   private final int CURRENT_SERIAL_VERSION = 1;
   
   protected ArrayList examples = new ArrayList();
   protected ArrayList unlabeledExamples = new ArrayList();
   protected Set[] classNameSets = null;
   protected FeatureFactory factory = new FeatureFactory();

    public int numPosExamples = 0;

    /** Overridden, provides ExampleSchema for first dimension */
   public ExampleSchema getSchema()
   {
       ExampleSchema schema = new ExampleSchema((String[])classNameSets[0].toArray(new String[classNameSets[0].size()]));
	if (schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA)) return ExampleSchema.BINARY_EXAMPLE_SCHEMA;
	else return schema;
   }

    public MultiExampleSchema getMultiSchema() {
	ExampleSchema[] schemas = new ExampleSchema[classNameSets.length];
	for(int i=0; i<schemas.length; i++) {
	    schemas[i] = new ExampleSchema((String[])classNameSets[i].toArray(new String[classNameSets[i].size()]));
	    if (schemas.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA)) schemas[i] = ExampleSchema.BINARY_EXAMPLE_SCHEMA;
	}
	MultiExampleSchema multiSchema = new MultiExampleSchema(schemas);
	return multiSchema;
    }

   //
   // methods for semisupervised data,  part of the SemiSupervisedDataset interface
   //
   public void addUnlabeled(Instance instance) { unlabeledExamples.add( factory.compress(instance) ); }
   public Instance.Looper iteratorOverUnlabeled() { return new Instance.Looper( unlabeledExamples ); }
   //public ArrayList getUnlabeled() { return this.unlabeledExamples; }
   public int sizeUnlabeled() { return unlabeledExamples.size(); }
   public boolean hasUnlabeled() { return (unlabeledExamples.size()>0)? true : false; }


    public void add(Example example) {
	throw new IllegalArgumentException("You must add a MultiExample to a MutiDataset");
    }
    public void add(Example example, boolean compress) {
        throw new IllegalArgumentException("You must add a MultiExample to a MutiDataset");
    }

   //
   // methods for labeled data,  part of the Dataset interface
   //
   public void addMulti(MultiExample example)
   {
       if(classNameSets == null) {
	   classNameSets = new Set[example.getMultiLabel().numDimensions()];
	   for(int i=0; i<classNameSets.length; i++) {
	       classNameSets[i] = new TreeSet();
	   }
       }
       if(classNameSets.length != example.getMultiLabel().numDimensions())
	   throw new IllegalArgumentException("This example does not have the same number of dimensions as previous examples");

       examples.add(factory.compressMulti(example));
      Set[] possibleLabels = example.getMultiLabel().possibleLabels();
      for(int i=0; i<classNameSets.length; i++) {
	  classNameSets[i].addAll( possibleLabels[i] );
	  }

      // Maybe change
      ClassLabel cl = example.getLabel();
      if(cl.isPositive())
	  numPosExamples++;
   }

    public Dataset[] separateDatasets() {
	Example[] ex_one = ((MultiExample)examples.get(0)).getExamples();
	Dataset[] d = new BasicDataset[ex_one.length];
	for(int i=0; i<d.length; i++) {
	    d[i] = new BasicDataset();
	}
	for(int i=0; i<examples.size(); i++) {
	    Example[] ex = ((MultiExample)examples.get(i)).getExamples();	    
	    for(int j=0; j<ex.length; j++) {
		d[j].add(ex[j]);
	    }
	}
	return d;
    }

    public int getNumPosExamples() {
	return numPosExamples;
    }

    public Example.Looper iterator() {
	throw new IllegalArgumentException("Must use multiIterator to iterate through MultiExamples");
    }

   public MultiExample.Looper multiIterator()
   {
      return new MultiExample.Looper( examples );
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
      MultiDataset copy = new MultiDataset();
      for (MultiExample.Looper i=multiIterator(); i.hasNext(); ) {
         copy.addMulti(i.nextMultiExample());
      }
      return (Dataset)copy;
   }

   //
   // Implement Saveable interface.
   //
   static private final String FORMAT_NAME = "Minorthird MultiDataset";
   public String[] getFormatNames() { return new String[] {FORMAT_NAME}; }
   public String getExtensionFor(String s) { return ".multidata"; }
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
      for (MultiExample.Looper i=this.multiIterator(); i.hasNext(); ) {
         MultiExample ex = i.nextMultiExample();
         buf.append( ex.toString() );
         buf.append( "\n" );
      }
      return buf.toString();
   }

    public MultiDataset  annotateData() {
	MultiDataset annotatedDataset = new MultiDataset();
	Splitter splitter = new CrossValSplitter(9);
	MultiDataset.MultiSplit s = this.MultiSplit(splitter);
	for (int x=0; x<9; x++) {
	    MultiClassifierTeacher teacher = new MultiDatasetClassifierTeacher(s.getTrain(x));
	    ClassifierLearner lnr = new CascadingBinaryLearner(new BatchVersion(new VotedPerceptron()));
	    MultiClassifier c = teacher.train(lnr);
	    for(MultiExample.Looper i = s.getTest(x).multiIterator(); i.hasNext(); ) {
		MultiExample ex = i.nextMultiExample();		
		Instance instance = ex.asInstance();
		MultiClassLabel predicted = c.multiLabelClassification(instance);
		Instance annotatedInstance = new InstanceFromPrediction(instance, predicted.bestClassName());
		MultiExample newEx = new MultiExample(annotatedInstance, ex.getMultiLabel(), ex.getWeight());
		annotatedDataset.addMulti(newEx);
	    }
	}
	return annotatedDataset;
    }
    
    public MultiDataset annotateData(MultiClassifier multiClassifier) {
	MultiDataset annotatedDataset = new MultiDataset();
	for(MultiExample.Looper i = this.multiIterator(); i.hasNext(); ) {
	    MultiExample ex = i.nextMultiExample();		
	    Instance instance = ex.asInstance();
	    MultiClassLabel predicted = multiClassifier.multiLabelClassification(instance);
	    Instance annotatedInstance = new InstanceFromPrediction(instance, predicted.bestClassName());
	    MultiExample newEx = new MultiExample(annotatedInstance, ex.getMultiLabel(), ex.getWeight());
	    annotatedDataset.addMulti(newEx);
	}
	return annotatedDataset;
    }
    
   /** A GUI view of the dataset. */
   public Viewer toGUI()
   {
      Viewer dbGui = new SimpleDatasetViewer();
      dbGui.setContent(this);
      Viewer instGui = GUI.newSourcedMultiExampleViewer();
      return new ZoomedViewer(dbGui,instGui);
   }

   public static class SimpleDatasetViewer extends ComponentViewer
   {
      public boolean canReceive(Object o) {
         return o instanceof Dataset;
      }
      public JComponent componentFor(Object o) {
         final MultiDataset d = (MultiDataset)o;
         final MultiExample[] tmp = new MultiExample[d.size()];
         int k=0;
         for (MultiExample.Looper i=d.multiIterator(); i.hasNext(); ) {
            tmp[k++] = i.nextMultiExample();
         }
         final JList jList = new JList(tmp);
         jList.setCellRenderer( new ListCellRenderer() {
            public Component getListCellRendererComponent(JList el,Object v,int index,boolean sel,boolean focus){
               return GUI.conciseMultiExampleRendererComponent((MultiExample)tmp[index],60,sel);
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
    public class MultiSplit {
	Splitter splitter;
	public MultiSplit(Splitter splitter) {this.splitter = splitter;}
	public int getNumPartitions() { return splitter.getNumPartitions(); }
	public MultiDataset getTrain(int k) { return invertMultiIteration(splitter.getTrain(k)); }
	public MultiDataset getTest(int k) { return invertMultiIteration(splitter.getTest(k)); }
    }
    public MultiSplit MultiSplit(final Splitter splitter)
    {
       splitter.split(examples.iterator());       
       return new MultiSplit(splitter);
   }
    
   private Dataset invertIteration(Iterator i)
   {
      BasicDataset copy = new BasicDataset();
      while (i.hasNext()) copy.add((Example)i.next());
      return copy;
   }
    private MultiDataset invertMultiIteration(Iterator i)
    {
	MultiDataset copy = new MultiDataset();
	while (i.hasNext()) copy.addMulti((MultiExample)i.next());
	return copy;
    }
    


   //
   // test routine
   //

   /** Simple test routine */
   static public void main(String[] args)
   {
       System.out.println("Not working yet");
   }

}
