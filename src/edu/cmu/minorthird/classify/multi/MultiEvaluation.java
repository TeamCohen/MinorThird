/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.multi;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedClassifier;
import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedDataset;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Stores some detailed results of evaluating a classifier on data with multiple labels.
 *
 * @author Cameron Williams
 */

public class MultiEvaluation implements Visible
{
    Evaluation[] evals;
    MultiExampleSchema schema;

  /** Create an evaluation for databases with this schema */

  public MultiEvaluation(MultiExampleSchema schema)
  {
    this.schema = schema;
    ExampleSchema[] exSchemas = schema.getSchemas();
    evals = new Evaluation[exSchemas.length];
    for(int i=0; i<evals.length; i++) {
	evals[i] = new Evaluation(exSchemas[i]);
    }
  }


  /** Test the classifier on the examples in the dataset and store the results. */
  public void extend(MultiClassifier c, MultiDataset d)
  {
    ProgressCounter pc = new ProgressCounter("classifying","example",d.size());    
    Classifier[] classifiers = c.getClassifiers();
    Dataset[] datasets = d.separateDatasets();    
    for(int i=0; i<evals.length; i++) {	
	evals[i].extend(classifiers[i], datasets[i], 1);
    }
    pc.progress();
    pc.finished();
  }


  /** Print summary statistics
   */
  public void summarize()
  {
      for(int i=0; i<evals.length; i++) {
	  System.out.println("Dimension: " + i);
	  double[] stats = evals[i].summaryStatistics();
	  String[] statNames = evals[i].summaryStatisticNames();
	  int maxLen = 0;
	  for (int j=0; j<statNames.length; j++) {
	      maxLen = Math.max(statNames[j].length(), maxLen);
	  }
	  for (int j=0; j<statNames.length; j++) {
	      System.out.print(statNames[j]+": ");
	      for (int k=0; k<maxLen-statNames[j].length(); k++) System.out.print(" ");
	      System.out.println(stats[j]);
	  }
      }
  }

    static public class EvaluationViewer extends ComponentViewer
    {
	private int eval_num;
	public EvaluationViewer(int eval_num) {
	    this.eval_num = eval_num;
	}
	public JComponent componentFor(Object o) {
	    MultiEvaluation me = (MultiEvaluation)o;
	    Evaluation e = me.evals[eval_num];
	    return e.toGUI();
	}
    }

    public Viewer toGUI()
    {
	ParallelViewer main = new ParallelViewer();
	
	for(int i=0; i<evals.length; i++) {
	    main.addSubView("Dimension: " + i, new EvaluationViewer(i));
	}
	main.setContent((Object)this);
	
	return main;
    }

    /*

  //
  // implement Saveable
  //
  final static private String EVAL_FORMAT_NAME = "Minorthird Evaluation";
  final static private String EVAL_EXT = ".eval";
  public String[] getFormatNames() { return new String[]{EVAL_FORMAT_NAME}; }
  public String getExtensionFor(String format) { return EVAL_EXT; }
  public void saveAs(File file,String formatName) throws IOException { save(file);	}
  public Object restore(File file) throws IOException	{	return load(file); }

  //
  //
  public void save(File file) throws IOException
  {
    PrintStream out = new PrintStream(new GZIPOutputStream(new FileOutputStream(file)));
    save(out);
  }
  private void save(PrintStream out) throws IOException
  {
    out.println(StringUtil.toString( schema.validClassNames() ));
    for (Iterator i=propertyKeyList.iterator(); i.hasNext(); ) {
      String prop = (String)i.next();
      String value = properties.getProperty(prop);
      out.println(prop+"="+value);
    }
    byOriginalPosition();
    for (Iterator i=entryList.iterator(); i.hasNext(); ) {
      Entry e = (Entry)i.next();
      out.println(
        e.predicted.bestClassName() +" "+
        e.predicted.bestWeight() +" "+
        e.actual.bestClassName());
    }
    out.close();
  }
  static public Evaluation load(File file) throws IOException
  {
    // disabled to avoid looping, since this is how we now de-serialize
    // first try loading a serialized version
    //try {	return (Evaluation)IOUtil.loadSerialized(file); } catch (Exception ex) { ;  }

    LineNumberReader in =new LineNumberReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
    String line = in.readLine();
    if (line==null) throw new IllegalArgumentException("no class list on line 1 of file "+file.getName());
    String[] classes = line.substring(1,line.length()-1).split(",");
    ExampleSchema schema = new ExampleSchema(classes);
    Evaluation result = new Evaluation(schema);
    while ((line = in.readLine())!=null) {
      if (line.indexOf('=')>=0) {
        // property
        String[] propValue = line.split("=");
        if (propValue.length==2) {
          result.setProperty(propValue[0],propValue[1]);
        } else if (propValue.length==1) {
          result.setProperty(propValue[0],"");
        } else {
          throw new IllegalArgumentException(file.getName()+" line "+in.getLineNumber()+": illegal format");
        }
      } else {
        String[] words = line.split(" ");
        if (words.length<3)
          throw new IllegalArgumentException(
            file.getName()+" line "+in.getLineNumber()+": illegal format");
        ClassLabel predicted = new ClassLabel(words[0],StringUtil.atof(words[1]));
        ClassLabel actual = new ClassLabel(words[2]);
        //double instanceWeight = StringUtil.atof(words[3]);
        MutableInstance instance = new MutableInstance("dummy");
        //instance.setWeight( instanceWeight );
        Example example = new Example(instance, actual );
        result.extend( predicted, example, DEFAULT_PARTITION_ID );
      }
    }
    in.close();
    return result;
    }*/
 
}
