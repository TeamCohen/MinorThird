/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.*;
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

/** Stores some detailed results of evaluating a classifier on data.
 *
 * @author William Cohen
 */

public class Evaluation implements Visible,Serializable,Saveable
{
  private static Logger log = Logger.getLogger(Evaluation.class);

  // serialization stuff
  static private final long serialVersionUID = 1;
  private final int CURRENT_VERSION_NUMBER = 1;

  //
  // private data
  //

  // all entries
  static public final int DEFAULT_PARTITION_ID = 0;

  private ArrayList entryList = new ArrayList();
  // cached values
  transient private Matrix cachedPRCMatrix = null;
  transient private Matrix cachedConfusionMatrix = null;
  // dataset schema
  private ExampleSchema schema;
  // properties
  private Properties properties = new Properties();
  private ArrayList propertyKeyList = new ArrayList();
  // are all classes binary?
  private boolean isBinary = true;

  /** Create an evaluation for databases with this schema */

  public Evaluation(ExampleSchema schema)
  {
    this.schema = schema;
    isBinary = schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
  }


  /** Test the classifier on the examples in the dataset and store the results. */
  public void extend(Classifier c,Dataset d, int cvID)
  {
    ProgressCounter pc = new ProgressCounter("classifying","example",d.size());
    for (Example.Looper i=d.iterator(); i.hasNext(); ) {
      Example ex = i.nextExample();
      ClassLabel p = c.classification( ex );
      extend(p,ex,cvID);
      pc.progress();
    }
    pc.finished();
  }

  /** Test the SequenceClassifier on the examples in the dataset and store the results. */
  public void extend(SequenceClassifier c,SequenceDataset d)
  {
    for (Iterator i=d.sequenceIterator(); i.hasNext(); ) {
      Example[] seq = (Example[])i.next();
      ClassLabel[] pred = c.classification( seq );
      for (int j=0; j<seq.length; j++) {
        extend( pred[j], seq[j], DEFAULT_PARTITION_ID );
      }
    }
  }

   /** Test the classifier on the examples in the dataset and store the results. */
   public void extend(SemiSupervisedClassifier c,SemiSupervisedDataset d, int cvID)
   {
     ProgressCounter pc = new ProgressCounter("classifying","example",d.size());
     for (Example.Looper i=d.iterator(); i.hasNext(); ) {
       Example ex = i.nextExample();
       ClassLabel p = c.classification( ex );
       extend(p,ex,cvID);
       pc.progress();
     }
     pc.finished();
   }

  /** Record the result of predicting the give class label on the given example */
  public void extend(ClassLabel predicted, Example example, int cvID)
  {
		if (predicted.bestClassName()==null) 
			throw new IllegalArgumentException("predicted can't be null! for example: "+example);
		if (example.getLabel()==null) throw new IllegalArgumentException("predicted can't be null!");
    if (log.isDebugEnabled()) {
      String ok = predicted.isCorrect(example.getLabel()) ? "Y" : "N";
      log.debug("ok: "+ok+"\tpredict: "+predicted+"\ton: "+example);
    }
    entryList.add( new Entry(example.asInstance(), predicted, example.getLabel(), entryList.size(), cvID) );
		// calling these extends the schema to cover these classes
		extendSchema( example.getLabel() );
		extendSchema( predicted );
    // clear caches
    cachedPRCMatrix = null;
  }

  public void setProperty(String prop,String value)
  {
    if (properties.getProperty(prop)==null) {
      propertyKeyList.add(prop);
    }
    properties.setProperty(prop,value);
  }
  public String getProperty(String prop)
  {
    return properties.getProperty(prop,"=unassigned=");
  }

	//
	// low-level access
	//
	public ClassLabel getPrediction(int i)
	{
		return ((Entry)entryList.get(i)).predicted;
	}
	public ClassLabel getActual(int i)
	{
		return ((Entry)entryList.get(i)).actual;
	}
	public boolean isCorrect(int i)
	{
		return getPrediction(i).isCorrect(getActual(i));
	}

  //
  // simple statistics
  //

  /** Weighted total errors. */
  public double errors()
  {
    double errs = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
			if (e.actual.bestClassName()==null) throw new IllegalArgumentException("actual label is null?");
      errs += e.predicted.isCorrect(e.actual) ? 0 : e.w;
    }
    return errs;
  }

  /** Weighted total errors on POSITIVE examples. */
  public double errorsPos()
  {
		if (!isBinary) return -1;
    double errsPos = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      if ( "POS".equals(e.actual.bestClassName()) ) {
        errsPos += e.predicted.isCorrect(e.actual) ? 0 : e.w;
      }
    }
    return errsPos;
  }

  /** Weighted total errors on POSITIVE examples with partitionID = ID. */
  public double errorsPos(int ID)
  {
		if (!isBinary) return -1;
    double errsPos = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      if ( "POS".equals(e.actual.bestClassName()) & e.partitionID==ID ) {
        errsPos += e.predicted.isCorrect(e.actual) ? 0 : e.w;
      }
    }
    return errsPos;
  }

  /** Weighted total errors on NEGATIVE examples. */
  public double errorsNeg()
  {
    double errsNeg = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      if ( "NEG".equals(e.actual.bestClassName()) ) {
        errsNeg += e.predicted.isCorrect(e.actual) ? 0 : e.w;
      }
    }
    return errsNeg;
  }

  /** Weighted total errors on NEGATIVE examples with partitionID = ID. */
  public double errorsNeg(int ID)
  {
    double errsNeg = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      if ( "NEG".equals(e.actual.bestClassName()) & e.partitionID==ID ) {
        errsNeg += e.predicted.isCorrect(e.actual) ? 0 : e.w;
      }
    }
    return errsNeg;
  }

  /** standard deviation of total errors on POSITIVE examples. */
  public double stDevErrorsPos()
  {
		if (!isBinary) return -1;
    int cvFolds=0;
    for (int i=0; i<entryList.size(); i++)
    {
      Entry e = getEntry(i);
      if (e.partitionID>cvFolds) { cvFolds=e.partitionID+1; }
    }
    double mean = errorsPos()/numberOfPositiveExamples();
    double variance = 0.0;
    for (int k=0; k<cvFolds; k++)
    {
      variance += Math.pow( errorsPos(k)/numberOfPositiveExamples(k)-mean,2 ) / ((double)cvFolds);
    }
    return Math.sqrt( variance );
  }

  /** standard deviation of total errors on POSITIVE examples. */
  public double stDevErrorsNeg()
  {
		if (!isBinary) return -1;
    int cvFolds=0;
    for (int i=0; i<entryList.size(); i++)
    {
      Entry e = getEntry(i);
      if (e.partitionID>cvFolds) { cvFolds=e.partitionID+1; }
    }
    double mean = errorsNeg()/numberOfNegativeExamples();
    double variance = 0.0;
    for (int k=0; k<cvFolds; k++)
    {
      variance += Math.pow( errorsNeg(k)/numberOfNegativeExamples(k)-mean,2 ) / ((double)cvFolds);
    }
    return Math.sqrt( variance );
  }

  /** Total weight of all instances. */
  public double numberOfInstances()
  {
    double n = 0;
    for (int i=0; i<entryList.size(); i++) {
      n += getEntry(i).w;
    }
    return n;
  }

  /** Total weight of all POSITIVE examples. */
  public double numberOfPositiveExamples()
  {
		if (!isBinary) return -1;
    double n = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      if ( "POS".equals(e.actual.bestClassName()) ) {
        n += e.w;
      }
    }
    return n;
  }

  /** Total weight of all POSITIVE examples with partitionID = ID. */
  public double numberOfPositiveExamples(int ID)
  {
		if (!isBinary) return -1;
    double n = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      if ( "POS".equals(e.actual.bestClassName()) & e.partitionID==ID ) {
        n += e.w;
      }
    }
    return n;
  }

  /** Total weight of all NEGATIVE examples. */
  public double numberOfNegativeExamples()
  {
    double n = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      if ( "NEG".equals(e.actual.bestClassName()) ) {
        n += e.w;
      }
    }
    return n;
  }

  /** Total weight of all NEGATIVE examples with partitionID = ID. */
  public double numberOfNegativeExamples(int ID)
  {
    double n = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      if ( "NEG".equals(e.actual.bestClassName()) & e.partitionID==ID ) {
        n += e.w;
      }
    }
    return n;
  }

  /** Error rate. */
  public double errorRate()
  {
    return errors()/numberOfInstances();
  }

  /** Error rate on Positive examples. */
  public double errorRatePos()
  {
    return errorsPos()/numberOfPositiveExamples();
  }

  /** Error rate on Negative examples. */
  public double errorRateNeg()
  {
    return errorsNeg()/numberOfNegativeExamples();
  }

  /** Balanced Error rate. */
  public double errorRateBalanced()
  {
    return 0.5*(errorsPos()/numberOfPositiveExamples())+0.5*(errorsNeg()/numberOfNegativeExamples());
  }

  /** Non-interpolated average precision. */
  public double averagePrecision()
  {
    if (!isBinary) return -1;

    double total=0, n=0;
    Matrix m = precisionRecallScore();
    double lastRecall = 0;
    for (int i=0; i<m.values.length; i++) {
      if (m.values[i][1] > lastRecall) {
        n++;
        total += m.values[i][0];
      }
      lastRecall = m.values[i][1];
    }
    return total/n;
  }

  /** Max f1 values at any cutoff. */
  public double maxF1()
  {
    if (!isBinary) return -1;

    double maxF1 = 0;
    Matrix m = precisionRecallScore();
    for (int i=0; i<m.values.length; i++) {
      double p = m.values[i][0];
      double r = m.values[i][1];
      if (p>0 || r>0) {
        double f1 = (2*p*r) / (p+r);
        maxF1 = Math.max(maxF1, f1);
      }
    }
    return maxF1;
  }

	public double kappa()
	{
		Matrix cm = confusionMatrix();
		double n = entryList.size();
		int k = schema.getNumberOfClasses(); 

		double[] numActual = new double[k];
		double[] numPredicted = new double[k];
		double numAgree = 0.0;
		for (int i=0; i<k; i++) {
			numAgree += cm.values[i][i];
			for (int j=0; j<k; j++) {
				numActual[i] += cm.values[i][j];
				numPredicted[i] += cm.values[j][i];
			}
		}
		
		double randomAgreement = 0.0;
		for (int i=0; i<k; i++) {
			randomAgreement += (numActual[i]/n) * (numPredicted[i]/n);
		}
		return (numAgree/n - randomAgreement) / (1.0 - randomAgreement);
	}

  /** Average logloss on all examples. */
  public double averageLogLoss()
  {
    double tot = 0;
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      double confidence = e.predicted.getWeight( e.actual.bestClassName() );
      double error = e.predicted.isCorrect(e.actual) ? +1 : -1;
      tot += Math.log( 1.0 + Math.exp( confidence * error ) );
    }
    return tot/entryList.size();
  }

  public double precision()
  {
		if (!isBinary) return -1;
    Matrix cm = confusionMatrix();
    int p = classIndexOf(ExampleSchema.POS_CLASS_NAME);
    int n = classIndexOf(ExampleSchema.NEG_CLASS_NAME);
    //cm is actual, predicted
    return cm.values[p][p]/(cm.values[p][p] + cm.values[n][p]);
  }
  public double recall()
  {
		if (!isBinary) return -1;
    Matrix cm = confusionMatrix();
    int p = classIndexOf(ExampleSchema.POS_CLASS_NAME);
    int n = classIndexOf(ExampleSchema.NEG_CLASS_NAME);
    //cm is actual, predicted
    return cm.values[p][p]/(cm.values[p][p] + cm.values[p][n]);
  }
  public double f1()
  {
		if (!isBinary) return -1;
    double p = precision();
    double r = recall();
    return (2*p*r) / (p+r);
  }
  public double[] summaryStatistics()
  {
    return new double[] {
      errorRate(),
      errorRateBalanced(),
      errorRatePos(),
      stDevErrorsPos(),
      errorRateNeg(),
      stDevErrorsNeg(),
      averagePrecision(),
      maxF1(),
      averageLogLoss(),
      recall(),
      precision(),
      f1(),
			kappa()
    };
  }
  static public String[] summaryStatisticNames()
  {
    return new String[] {
      "Error Rate",
      "Balanced Error Rate",
      ". Error Rate on Positive exs.",
      ". std. Deviation",
      ". Error Rate on Negative exs.",
      ". std. Deviation",
      "Average Precision",
      "Maximum F1",
      "Average Log Loss",
      "Recall",
      "Precision",
      "F1",
      "Kappa" };
  }

  //
  // complex statistics, ie ones that are harder to visualize
  //

  private static class Matrix {
    public double[][] values;
    public Matrix(double[][] values) { this.values=values; }
    public String toString()
    {
      StringBuffer buf = new StringBuffer("");
      for (int i=0; i<values.length; i++) {
        buf.append(StringUtil.toString(values[i])+"\n");
      }
      return buf.toString();
    }
  }

  /** Return a confusion matrix.
   */
  public Matrix confusionMatrix()
  {
    if (cachedConfusionMatrix!=null) return cachedConfusionMatrix;

    String[] classes = getClasses();
    // count up the errors
    double[][] confused = new double[classes.length][classes.length];
    for (int i=0; i<entryList.size(); i++) {
      Entry e = getEntry(i);
      confused[classIndexOf(e.actual)][classIndexOf(e.predicted)]++;
    }
    cachedConfusionMatrix = new Matrix(confused);
    return cachedConfusionMatrix;
  }

  public String[] getClasses()
  {
    return schema.validClassNames();
  }

  /** Return array of precision,recall,logitScore.
   */
  public Matrix precisionRecallScore()
  {
    if (cachedPRCMatrix!=null) return cachedPRCMatrix;

    if (!isBinary)
      throw new IllegalArgumentException("can't compute precisionRecallScore for non-binary data");
    byBinaryScore();
    int allActualPos = 0;
    int lastIndexOfActualPos = 0;
    ProgressCounter pc = new ProgressCounter("counting positive examples","examples",entryList.size());
    for (int i=0; i<entryList.size(); i++) {
      if (getEntry(i).actual.isPositive()) {
        allActualPos++;
        lastIndexOfActualPos = i;
      }
      pc.progress();
    }
    pc.finished();
    double truePosSoFar = 0;
    double falsePosSoFar = 0;
    double precision=1, recall=1, score=0;
    ProgressCounter pc2 = new ProgressCounter("computing statistics","examples",lastIndexOfActualPos);
    double[][] result = new double[lastIndexOfActualPos+1][3];
    for (int i=0; i<=lastIndexOfActualPos; i++) {
      Entry e = getEntry(i);
      score = e.predicted.posWeight();
      if (e.actual.isPositive()) truePosSoFar++;
      else falsePosSoFar++;
      if (truePosSoFar+falsePosSoFar>0) precision = truePosSoFar/(truePosSoFar + falsePosSoFar);
      if (allActualPos>0) recall = truePosSoFar/allActualPos;
      result[i][0] = precision;
      result[i][1] = recall;
      result[i][2] = score;
      pc2.progress();
    }
    pc2.finished();
    cachedPRCMatrix = new Matrix(result);
    return cachedPRCMatrix;
  }

  /** Return eleven-point interpolated precision.
   * Precisely, result is an array p[] of doubles
   * such that p[i] is the maximal precision value
   * for any point with recall>=i/10.
   *
   */
  public double[] elevenPointPrecision()
  {
    Matrix m = precisionRecallScore();
    //System.out.println("prs = "+m);
    double[] p = new double[11];
    p[0] = 1.0;
    for (int i=0; i<m.values.length; i++) {
      double r = m.values[i][1];
      //System.out.println("row "+i+", recall "+r+": "+StringUtil.toString(m.values[i]));
      for (int j=1; j<=10; j++) {
        if (r >= j/10.0) {
          p[j] = Math.max(p[j],m.values[i][0]);
          //System.out.println("update p["+j+"] => "+p[j]);
        }
      }
    }
    return p;
  }


  //
  // views of data
  //

  /** Detailed view. */
  public String toString()
  {
    StringBuffer buf = new StringBuffer("");
    for (int i=0; i<entryList.size(); i++) {
      buf.append( getEntry(i) + "\n" );
    }
    return buf.toString();
  }

  static public class PropertyViewer extends ComponentViewer
  {
    public JComponent componentFor(Object o)
    {
      final Evaluation e = (Evaluation)o;
      final JPanel panel = new JPanel();
      final JTextField propField = new JTextField(10);
      final JTextField valField = new JTextField(10);
      final JTable table = makePropertyTable(e);
      final JScrollPane tableScroller = new JScrollPane(table);
      final JButton addButton =
          new JButton(new AbstractAction("Insert Property") {
            public void actionPerformed(ActionEvent event) {
              e.setProperty(propField.getText(), valField.getText());
              tableScroller.getViewport().setView(makePropertyTable(e));
              tableScroller.revalidate();
              panel.revalidate();
            }
          });
      panel.setLayout(new GridBagLayout());
      GridBagConstraints gbc = fillerGBC();
      //gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridwidth=3;
      panel.add(tableScroller,gbc);
      panel.add(addButton,myGBC(0));
      panel.add(propField,myGBC(1));
      panel.add(valField,myGBC(2));
      return panel;
    }
    private GridBagConstraints myGBC(int col) {
      GridBagConstraints gbc = fillerGBC();
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridx = col;
      gbc.gridy = 1;
      return gbc;
    }
    private JTable makePropertyTable(final Evaluation e)
    {
      Object[][] table = new Object[e.propertyKeyList.size()][2];
      for (int i=0; i<e.propertyKeyList.size(); i++) {
        table[i][0] = e.propertyKeyList.get(i);
        table[i][1] = e.properties.get(e.propertyKeyList.get(i));
      }
      String[] colNames = new String[] { "Property", "Property's Value" };
      return new JTable(table,colNames);
    }
  }

  public class SummaryViewer extends ComponentViewer {
    public JComponent componentFor(Object o) {
      Evaluation e = (Evaluation)o;
      double[] ss = e.summaryStatistics();
      String[] ssn = e.summaryStatisticNames();
      Object[][] oss = new Object[ss.length][2];
      for (int i=0; i<ss.length; i++) {
        oss[i][0] = ssn[i];
        oss[i][1] = new Double(ss[i]);
      }
      JTable jtable = new JTable(oss,new String[] { "Statistic", "Value" });
      jtable.setDefaultRenderer(Object.class, new MyTableCellRenderer());
      jtable.setVisible(true);
      return new JScrollPane( jtable );
    }
  }

  static public class ElevenPointPrecisionViewer extends ComponentViewer {
    public JComponent componentFor(Object o) {
      Evaluation e = (Evaluation)o;
      double[] p = e.elevenPointPrecision();
      LineCharter lc = new LineCharter();
      lc.startCurve("Interpolated Precision");
      for (int i=0; i<p.length; i++) {
        lc.addPoint( i/10.0, p[i] );
      }
      return lc.getPanel("11-Pt Interpolated Precision vs. Recall", "Recall", "Precision");
    }
  }

  static public class ConfusionMatrixViewer extends ComponentViewer
  {
    public JComponent componentFor(Object o) {
      Evaluation e = (Evaluation)o;
      JPanel panel = new JPanel();
      Matrix m = e.confusionMatrix();
      String[] classes = e.getClasses();
      panel.setLayout(new GridBagLayout());
      //add( new JLabel("Actual class"), cmGBC(0,0) );
      GridBagConstraints gbc = cmGBC(0,1);
      gbc.gridwidth = classes.length;
      panel.add( new JLabel("Predicted Class"), gbc );
      for (int i=0; i<classes.length; i++) {
        panel.add( new JLabel(classes[i]), cmGBC(1,i+1) );
      }
      for (int i=0; i<classes.length; i++) {
        panel.add( new JLabel(classes[i]), cmGBC(i+2,0) );
        for (int j=0; j<classes.length; j++) {
          panel.add( new JLabel(Double.toString(m.values[i][j])), cmGBC( i+2,j+1 ) );
        }
      }
      return panel;
    }
    private GridBagConstraints cmGBC(int i,int j)
    {
      GridBagConstraints gbc = new GridBagConstraints();
      //gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = gbc.weighty = 0;
      gbc.gridy = i;
      gbc.gridx = j;
      gbc.ipadx = gbc.ipady = 20;
      return gbc;
    }
  }


	/** Print summary statistics
	 */
	public void summarize()
	{
		double[] stats = summaryStatistics();
		String[] statNames = summaryStatisticNames();
		int maxLen = 0;
		for (int i=0; i<statNames.length; i++) {
			maxLen = Math.max(statNames[i].length(), maxLen); 
		}
		for (int i=0; i<statNames.length; i++) {
			System.out.print(statNames[i]+": ");
			for (int j=0; j<maxLen-statNames[i].length(); j++) System.out.print(" ");
			System.out.println(stats[i]);
		}
	}


  public Viewer toGUI()
  {
    ParallelViewer main = new ParallelViewer();

    main.addSubView("Summary",new SummaryViewer());
    main.addSubView("Properties",new PropertyViewer());
    if (isBinary) main.addSubView("11Pt Precision/Recall",new ElevenPointPrecisionViewer());
    main.addSubView("Confusion Matrix",new ConfusionMatrixViewer());
    main.addSubView("Debug", new VanillaViewer());
    main.setContent(this);

    return main;
  }


  //
  // one entry in the evaluation
  //
  private static class Entry implements Serializable
  {
    private static final long serialVersionUID = -4069980043842319179L;
    transient public Instance instance = null;
    public int partitionID;
    public int index;
    public ClassLabel predicted,actual;
    public int h;
    public double w=1.0;
    public Entry(Instance i,ClassLabel p,ClassLabel a,int k, int id)
    {
      instance=i; predicted=p; actual=a;	index=k;  partitionID=id;
      h=instance.hashCode();
    }
    public String toString()
    {
      double w = predicted.bestWeight();
      return predicted+"\t"+actual+"\t"+instance;
    }
  }

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
  }


  //
  // convenience methods
  //
  private Entry getEntry(int i)
  {
    return (Entry)entryList.get(i);
  }
  private int classIndexOf(ClassLabel classLabel)
  {
    return classIndexOf(classLabel.bestClassName());
  }
  private int classIndexOf(String classLabelName)
  {
		return  schema.getClassIndex(classLabelName);
	}
	private void extendSchema(ClassLabel classLabel)
	{
		//System.out.println("classLabel: "+classLabel);
    if (!classLabel.isBinary()) isBinary = false;
		int r = classIndexOf(classLabel.bestClassName());
		if (r < 0) {
			//System.out.println("extending");
			// extend the schema
			String[] currentNames = schema.validClassNames();
			String[] newNames = new String[currentNames.length+1];
			for (int i=0; i<currentNames.length; i++) newNames[i] = currentNames[i];
			newNames[currentNames.length] = classLabel.bestClassName();
		}
  }

  private void byBinaryScore()
  {
    Collections.sort(
        entryList,
        new Comparator() {
          public int compare(Object a, Object b) {
            return MathUtil.sign( ((Entry)b).predicted.posWeight() - ((Entry)a).predicted.posWeight() );
          }
        });
  }

  private void byOriginalPosition()
  {
    Collections.sort(
        entryList,
        new Comparator() {
          public int compare(Object a, Object b) {
            return ((Entry)a).index - ((Entry)b).index;
          }
        });
  }

  // table renderer
  public class MyTableCellRenderer extends DefaultTableCellRenderer
  {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if((row % 2) != 0)
      {
        label.setBackground(Color.lightGray);
        label.setOpaque(true);
      }
      else
      {
        label.setBackground(Color.white);
        label.setOpaque(true);
      }
      return label;
    }
  }

  //
  // test routine
  //
  static public void main(String[] args)
  {
    try {
      Evaluation v = Evaluation.load(new File(args[0]));
      if (args.length>1) v.save(new File(args[1]));
      ViewerFrame f = new ViewerFrame("From file "+args[0], v.toGUI());
    } catch (Exception e) {
      System.out.println("usage: Evaluation [serializedFile|evaluationFile] [evaluationFile]");
      e.printStackTrace();
    }
  }
}
