/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify.experiments;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Classifier;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.ExampleSchema;
import edu.cmu.minorthird.classify.Instance;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.classify.SGMExample;
import edu.cmu.minorthird.classify.relational.RealRelationalDataset;
import edu.cmu.minorthird.classify.relational.StackedGraphicalLearner;
import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedClassifier;
import edu.cmu.minorthird.classify.semisupervised.SemiSupervisedDataset;
import edu.cmu.minorthird.classify.sequential.SequenceClassifier;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.util.MathUtil;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.LineCharter;
import edu.cmu.minorthird.util.gui.ParallelViewer;
import edu.cmu.minorthird.util.gui.VanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.Visible;

/** Stores some detailed results of evaluating a classifier on data.
 *
 * @author William Cohen
 */

public class Evaluation implements Visible,Serializable,Saveable{

	private static Logger log=Logger.getLogger(Evaluation.class);

	// serialization stuff
	static final long serialVersionUID=20080130L;

	// private data

	// all entries

	static public final int DEFAULT_PARTITION_ID=0;

	private List<Entry> entryList=new ArrayList<Entry>();

	// cached values
	transient private Matrix cachedPRCMatrix=null;

	transient private Matrix cachedTPFPMatrix=null;

	transient private Matrix cachedConfusionMatrix=null;

	// dataset schema
	private ExampleSchema schema;

	// properties
	private Properties properties=new Properties();

	private List<String> propertyKeyList=new ArrayList<String>();

	// are all classes binary?
	private boolean isBinary=true;

	/** Create an evaluation for databases with this schema */

	public Evaluation(ExampleSchema schema){
		this.schema=schema;
		isBinary=schema.equals(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
	}

//classification(RealRelationalDataset dataset)
	/** Test the classifier on the examples in the relational dataset and store the results. */
	public void extend4SGM(StackedGraphicalLearner.StackedGraphicalClassifier c,
			RealRelationalDataset d,int cvID){
		//ProgressCounter pc=new ProgressCounter("classifying","example",d.size());
		Map<String,ClassLabel> rlt=c.classification(d);

		for(Iterator<String> i=rlt.keySet().iterator();i.hasNext();){
			String ID=i.next();
			ClassLabel predicted=rlt.get(ID);
			SGMExample example=d.getExampleWithID(ID);

			if(predicted.bestClassName()==null)
				throw new IllegalArgumentException(
						"predicted can't be null! for example: "+example);
			if(example.getLabel()==null)
				throw new IllegalArgumentException("predicted can't be null!");
			if(log.isDebugEnabled()){
				String ok=predicted.isCorrect(example.getLabel())?"Y":"N";
				log.debug("ok: "+ok+"\tpredict: "+predicted+"\ton: "+example);
			}
			entryList.add(new Entry(example.asInstance(),predicted,
					example.getLabel(),entryList.size(),cvID));
			// calling these extends the schema to cover these classes
			extendSchema(example.getLabel());
			extendSchema(predicted);
			// clear caches
			cachedPRCMatrix=null;

		}

	}

	/** Test the classifier on the examples in the dataset and store the results. */
	public void extend(Classifier c,Dataset d,int cvID){
		ProgressCounter pc=new ProgressCounter("classifying","example",d.size());
		for(Iterator<Example> i=d.iterator();i.hasNext();){
			Example ex=i.next();
			ClassLabel p=c.classification(ex);
			extend(p,ex,cvID);
			pc.progress();
		}
		pc.finished();
	}

	/** Test the SequenceClassifier on the examples in the dataset and store the results. */
	public void extend(SequenceClassifier c,SequenceDataset d){
		for(Iterator<Example[]> i=d.sequenceIterator();i.hasNext();){
			Example[] seq=i.next();
			ClassLabel[] pred=c.classification(seq);
			for(int j=0;j<seq.length;j++){
				extend(pred[j],seq[j],DEFAULT_PARTITION_ID);
			}
		}
	}

	/** Test the classifier on the examples in the dataset and store the results. */
	public void extend(SemiSupervisedClassifier c,SemiSupervisedDataset d,int cvID){
		ProgressCounter pc=new ProgressCounter("classifying","example",d.size());
		for(Iterator<Example> i=d.iterator();i.hasNext();){
			Example ex=i.next();
			ClassLabel p=c.classification(ex);
			extend(p,ex,cvID);
			pc.progress();
		}
		pc.finished();
	}

	/** Record the result of predicting the give class label on the given example */
	public void extend(ClassLabel predicted,Example example,int cvID){
		if(predicted.bestClassName()==null){
//			for(String label:predicted.possibleLabels()){
//				log.info(label+"="+predicted.getWeight(label));
//			}
			throw new IllegalArgumentException("Best predicted class name is NULL: "+predicted);
		}
		if(example.getLabel()==null){
			throw new IllegalArgumentException("True label is NULL: "+example);
		}
		if(log.isDebugEnabled()){
			String ok=predicted.isCorrect(example.getLabel())?"Y":"N";
			log.debug("ok: "+ok+"\tpredict: "+predicted+"\ton: "+example);
		}
		entryList.add(new Entry(example.asInstance(),predicted,example.getLabel(),
				entryList.size(),cvID));
		// calling these extends the schema to cover these classes
		extendSchema(example.getLabel());
		extendSchema(predicted);
		// clear caches
		cachedPRCMatrix=null;
	}

	public void setProperty(String prop,String value){
		if(properties.getProperty(prop)==null){
			propertyKeyList.add(prop);
		}
		properties.setProperty(prop,value);
	}

	public String getProperty(String prop){
		return properties.getProperty(prop,"=unassigned=");
	}

	//
	// low-level access
	//
	public ClassLabel getPrediction(int i){
		return ((Entry)entryList.get(i)).predicted;
	}

	public ClassLabel getActual(int i){
		return ((Entry)entryList.get(i)).actual;
	}

	public boolean isCorrect(int i){
		return getPrediction(i).isCorrect(getActual(i));
	}

	//
	// simple statistics
	//

	/** Weighted total errors. */
	public double errors(){
		double errs=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if(e.actual.bestClassName()==null)
				throw new IllegalArgumentException("actual label is null?");
			errs+=e.predicted.isCorrect(e.actual)?0:e.w;
		}
		return errs;
	}

	/** Weighted total errors on examples with partitionID = ID.. */
	public double errors(int ID){
		double errs=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if(e.partitionID==ID){
				errs+=e.predicted.isCorrect(e.actual)?0:e.w;
			}
		}
		return errs;
	}

	/** Weighted total errors for classes 1 to K. */
	public double[] errorsByClass(){
		int K=schema.getNumberOfClasses();
		double[] err=new double[K];
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			String actualLabel=e.actual.bestClassName();
			int index=schema.getClassIndex(actualLabel);
			err[index]+=e.predicted.isCorrect(e.actual)?0:e.w;
		}
		return err;
	}

	/** Weighted total errors for classes 1 to K on examples with partitionID = ID. */
	public double[] errorsByClass(int ID){
		int K=schema.getNumberOfClasses();
		double[] err=new double[K];
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if(e.partitionID==ID){
				String actualLabel=e.actual.bestClassName();
				int index=schema.getClassIndex(actualLabel);
				err[index]+=e.predicted.isCorrect(e.actual)?0:e.w;
			}
		}
		return err;
	}

	/** Weighted total errors on POSITIVE examples. */
	public double errorsPos(){
		if(!isBinary)
			return -1;
		double errsPos=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if("POS".equals(e.actual.bestClassName())){
				errsPos+=e.predicted.isCorrect(e.actual)?0:e.w;
			}
		}
		return errsPos;
	}

	/** Weighted total errors on POSITIVE examples with partitionID = ID. */
	public double errorsPos(int ID){
		if(!isBinary)
			return -1;
		double errsPos=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if("POS".equals(e.actual.bestClassName())&e.partitionID==ID){
				errsPos+=e.predicted.isCorrect(e.actual)?0:e.w;
			}
		}
		return errsPos;
	}

	/** Weighted total errors on NEGATIVE examples. */
	public double errorsNeg(){
		double errsNeg=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if("NEG".equals(e.actual.bestClassName())){
				errsNeg+=e.predicted.isCorrect(e.actual)?0:e.w;
			}
		}
		return errsNeg;
	}

	/** Weighted total errors on NEGATIVE examples with partitionID = ID. */
	public double errorsNeg(int ID){
		double errsNeg=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if("NEG".equals(e.actual.bestClassName())&e.partitionID==ID){
				errsNeg+=e.predicted.isCorrect(e.actual)?0:e.w;
			}
		}
		return errsNeg;
	}

	/** standard deviation of total errors. */
	public double stDevErrors(){
		int cvFolds=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if(e.partitionID>cvFolds){
				cvFolds=e.partitionID+1;
			}
		}
		double mean=errorRate();
		double stDev=0.0;
		for(int k=0;k<cvFolds;k++){
			stDev+=Math.pow(errors(k)/numberOfInstances(k)-mean,2)/((double)cvFolds);
		}
		return Math.sqrt(stDev);
	}

	/** standard deviation of total errors for classes 1 to K. */
	public double[] stDevErrorsByClass(){
		int K=schema.getNumberOfClasses();
		int cvFolds=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if(e.partitionID>cvFolds){
				cvFolds=e.partitionID+1;
			}
		}
		double[] mean=errorRateByClass();
		double[] stdev=new double[K];
		for(int k=0;k<cvFolds;k++){
			double[] errorsByClass=errorsByClass(k);
			double[] numerOfExamplesByClass=numberOfExamplesByClass(k);
			for(int i=0;i<K;i++){
				stdev[i]+=
					Math.pow(errorsByClass[i]/numerOfExamplesByClass[i]-mean[i],2)/
					((double)cvFolds);
			}
		}
		for(int i=0;i<K;i++){
			stdev[i]=Math.sqrt(stdev[i]);
		}
		return stdev;
	}

	/** standard deviation of total errors on POSITIVE examples. */
	public double stDevErrorsPos(){
		if(!isBinary)
			return -1;
		int cvFolds=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if(e.partitionID>cvFolds){
				cvFolds=e.partitionID+1;
			}
		}
		double mean=errorsPos()/numberOfPositiveExamples();
		double variance=0.0;
		for(int k=0;k<cvFolds;k++){
			variance+=
				Math.pow(errorsPos(k)/numberOfPositiveExamples(k)-mean,2)/
				((double)cvFolds);
		}
		return Math.sqrt(variance);
	}

	/** standard deviation of total errors on NEGATIVE examples. */
	public double stDevErrorsNeg(){
		if(!isBinary)
			return -1;
		int cvFolds=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if(e.partitionID>cvFolds){
				cvFolds=e.partitionID+1;
			}
		}
		double mean=errorsNeg()/numberOfNegativeExamples();
		double variance=0.0;
		for(int k=0;k<cvFolds;k++){
			variance+=
				Math.pow(errorsNeg(k)/numberOfNegativeExamples(k)-mean,2)/
				((double)cvFolds);
		}
		return Math.sqrt(variance);
	}

	/** Total weight of all instances. */
	public double numberOfInstances(){
		double n=0;
		for(int i=0;i<entryList.size();i++){
			n+=getEntry(i).w;
		}
		return n;
	}

	/** Total weight of all instances. */
	public double numberOfInstances(int ID){
		double n=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if(e.partitionID==ID){
				n+=e.w;
			}
		}
		return n;
	}

	/** Total weight of examples in all classes 1 to K. */
	public double[] numberOfExamplesByClass(){
		int K=schema.getNumberOfClasses();
		double[] wgt=new double[K];
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			String actualLabel=e.actual.bestClassName();
			int index=schema.getClassIndex(actualLabel);
			wgt[index]+=e.w;
		}
		return wgt;
	}

	/** Total weight of examples in all classes 1 to K with partitionID = ID. */
	public double[] numberOfExamplesByClass(int ID){
		int K=schema.getNumberOfClasses();
		double[] wgt=new double[K];
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			String actualLabel=e.actual.bestClassName();
			int index=schema.getClassIndex(actualLabel);
			if(e.partitionID==ID){
				wgt[index]+=e.w;
			}
		}
		return wgt;
	}

	/** Total weight of all POSITIVE examples. */
	public double numberOfPositiveExamples(){
		if(!isBinary)
			return -1;
		double n=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if("POS".equals(e.actual.bestClassName())){
				n+=e.w;
			}
		}
		return n;
	}

	/** Total weight of all POSITIVE examples with partitionID = ID. */
	public double numberOfPositiveExamples(int ID){
		if(!isBinary)
			return -1;
		double n=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if("POS".equals(e.actual.bestClassName())&e.partitionID==ID){
				n+=e.w;
			}
		}
		return n;
	}

	/** Total weight of all NEGATIVE examples. */
	public double numberOfNegativeExamples(){
		double n=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if("NEG".equals(e.actual.bestClassName())){
				n+=e.w;
			}
		}
		return n;
	}

	/** Total weight of all NEGATIVE examples with partitionID = ID. */
	public double numberOfNegativeExamples(int ID){
		double n=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			if("NEG".equals(e.actual.bestClassName())&e.partitionID==ID){
				n+=e.w;
			}
		}
		return n;
	}

	/** Error rate. */
	public double errorRate(){
		if(numberOfInstances()>0){
			return errors()/numberOfInstances();
		}
		else{
			return 0.0;
		}
	}

	/** Error rate by Class. */
	public double[] errorRateByClass(){
		int K=schema.getNumberOfClasses();
		double[] errRate=new double[K];
		double[] err=errorsByClass();
		double[] wgt=numberOfExamplesByClass();
		for(int i=0;i<K;i++){
			if(wgt[i]==0){
				errRate[i]=0.0;
			}
			else{
				errRate[i]=err[i]/wgt[i];
			}
		}
		return errRate;
	}

	/** Error rate on Positive examples. */
	public double errorRatePos(){
		return errorsPos()/numberOfPositiveExamples();
	}

	/** Error rate on Negative examples. */
	public double errorRateNeg(){
		return errorsNeg()/numberOfNegativeExamples();
	}

	/** Balanced Error rate. */
	public double errorRateBalanced(){
		double errorBalanced=0.0;
		int K=schema.getNumberOfClasses();
		double[] errorsByClass=errorsByClass();
		double[] numberOfExamplesByClass=numberOfExamplesByClass();
		for(int i=0;i<K;i++){
			if(numberOfExamplesByClass[i]>0){
				errorBalanced+=1.0/(double)K*errorsByClass[i]/numberOfExamplesByClass[i];
			}
			else{
				errorBalanced+=0.0;
			}
		}
		return errorBalanced;
	}

	/** Recall in the top K, excluding items with score<threshold */
	public double recallTopK(int k,double minScore){
		if(!isBinary)
			return -1;
		if(numberOfPositiveExamples()==0)
			return 1.0; // special case
		double lastRecall=0; // detect a postive example
		double numPositiveExamplesInTopK=0;
		Matrix m=precisionRecallScore();
		for(int i=0;i<Math.min(m.values.length,k);i++){
			if(m.values[i][1]>lastRecall&&m.values[i][2]>minScore){
				numPositiveExamplesInTopK++;
			}
			lastRecall=m.values[i][1];
		}
		return numPositiveExamplesInTopK/numberOfPositiveExamples();
	}

	/** Non-interpolated average precision. */
	public double averagePrecision(){
		if(!isBinary)
			return -1;
		if(numberOfInstances()==0)
			return Double.NaN; // undefined!

		double total=0,n=0;
		Matrix m=precisionRecallScore();
		double lastRecall=0; // detect a postive example
		for(int i=0;i<m.values.length;i++){
			if(m.values[i][1]>lastRecall){
				n++;
				total+=m.values[i][0];
			}
			lastRecall=m.values[i][1];
		}
		return total/n;
	}

	/** Max f1 values at any cutoff. */
	public double maxF1(){
		return maxF1(Double.MIN_VALUE);
	}

	/** Max f1 values for any threshold above the specified cutoff. */
	public double maxF1(double minThreshold){
		if(!isBinary)
			return -1;
		if(numberOfPositiveExamples()==0)
			return 1.0;
		double maxF1=0;
		Matrix m=precisionRecallScore();
		for(int i=0;i<m.values.length;i++){
			double p=m.values[i][0];
			double r=m.values[i][1];
			if((p>0||r>0)&&m.values[i][2]>=minThreshold){
				double f1=(2*p*r)/(p+r);
				maxF1=Math.max(maxF1,f1);
			}
		}
		return maxF1;
	}

	public double kappa(){

		Matrix cm=confusionMatrix();
		double n=entryList.size();
		int k=schema.getNumberOfClasses();
		
		if(n<1){
			return 0.0;
		}

		double[] numActual=new double[k];
		double[] numPredicted=new double[k];
		double numAgree=0.0;
		for(int i=0;i<k;i++){
			numAgree+=cm.values[i][i];
			for(int j=0;j<k;j++){
				numActual[i]+=cm.values[i][j];
				numPredicted[i]+=cm.values[j][i];
			}
		}

		double randomAgreement=0.0;
		for(int i=0;i<k;i++){
			randomAgreement+=(numActual[i]/n)*(numPredicted[i]/n);
		}

		return (numAgree/n-randomAgreement)/(1.0-randomAgreement);

	}

	public int numExamples(){
		return entryList.size();
	}

	/** Average logloss on all examples. */
	public double averageLogLoss(){
		double tot=0;
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			double confidence=e.predicted.getWeight(e.actual.bestClassName());
			double error=e.predicted.isCorrect(e.actual)?+1:-1;
			tot+=Math.log(1.0+Math.exp(confidence*error));
		}
		return tot/entryList.size();
	}

	public double precision(){
		if(!isBinary)
			return -1;
		Matrix cm=confusionMatrix();
		int p=classIndexOf(ExampleSchema.POS_CLASS_NAME);
		int n=classIndexOf(ExampleSchema.NEG_CLASS_NAME);
		//cm is actual, predicted
		return cm.values[p][p]/(cm.values[p][p]+cm.values[n][p]);
	}

	public double recall(){
		if(!isBinary)
			return -1;
		Matrix cm=confusionMatrix();
		int p=classIndexOf(ExampleSchema.POS_CLASS_NAME);
		int n=classIndexOf(ExampleSchema.NEG_CLASS_NAME);
		//cm is actual, predicted
		return cm.values[p][p]/(cm.values[p][p]+cm.values[p][n]);
	}

	public double f1(){
		if(!isBinary)
			return -1;
		double p=precision();
		double r=recall();
		return (2*p*r)/(p+r);
	}

	public double[] summaryStatistics(){
		int K=schema.getNumberOfClasses();
		if(isBinary){
			double[] stats=new double[(10+2*K)];
			stats[0]=errorRate();
			stats[1]=stDevErrors();
			stats[2]=errorRateBalanced();
			double[] err=errorRateByClass();
			double[] sd=stDevErrorsByClass();
			for(int i=0;i<K;i++){
				stats[(2+2*i+1)]=err[i];
				stats[(2+2*i+2)]=sd[i];
			}
			stats[(3+2*K)]=averagePrecision();
			stats[(4+2*K)]=maxF1();
			stats[(5+2*K)]=averageLogLoss();
			stats[(6+2*K)]=recall();
			stats[(7+2*K)]=precision();
			stats[(8+2*K)]=f1();
			stats[(9+2*K)]=kappa();
			return stats;
		}else{
			double[] stats=new double[(4+2*K)];
			stats[0]=errorRate();
			stats[1]=stDevErrors();
			stats[2]=errorRateBalanced();
			double[] err=errorRateByClass();
			double[] sd=stDevErrorsByClass();
			for(int i=0;i<K;i++){
				stats[(2+2*i+1)]=err[i];
				stats[(2+2*i+2)]=sd[i];
			}
			stats[(3+2*K)]=kappa();
			return stats;
		}
	}

	public String[] summaryStatisticNames(){
		int K=schema.getNumberOfClasses();
		if(isBinary){
			String[] names=new String[(10+2*K)];
			names[0]="Error Rate";
			names[1]=". std. deviation error rate";
			names[2]="Balanced Error Rate";
			for(int i=0;i<K;i++){
				String classname=schema.getClassName(i);
				names[(2+2*i+1)]=new String(". error rate on "+classname);
				names[(2+2*i+2)]=new String(". std. deviation on "+classname);
			}
			names[(3+2*K)]="Average Precision";
			names[(4+2*K)]="Maximium F1";
			names[(5+2*K)]="Average Log Loss";
			names[(6+2*K)]="Recall";
			names[(7+2*K)]="Precision";
			names[(8+2*K)]="F1";
			names[(9+2*K)]="Kappa";
			return names;
		}else{
			String[] names=new String[(4+2*K)];
			names[0]="Error Rate";
			names[1]=". std. deviation error rate";
			names[2]="Balanced Error Rate";
			for(int i=0;i<K;i++){
				String classname=schema.getClassName(i);
				names[(2+2*i+1)]=new String(". error rate on "+classname);
				names[(2+2*i+2)]=new String(". std. deviation on "+classname);
			}
			names[(3+2*K)]="Kappa";
			return names;
		}
	}

	//
	// complex statistics, ie ones that are harder to visualize
	//
	public static class Matrix{

		public double[][] values;

		public Matrix(double[][] values){
			this.values=values;
		}

		@Override
		public String toString(){
			StringBuffer buf=new StringBuffer("");
			for(int i=0;i<values.length;i++){
				buf.append(StringUtil.toString(values[i])+"\n");
			}
			return buf.toString();
		}

		public double getValue(int row,int col){
			return values[row][col];
		}
	}

	/** Return a confusion matrix.
	 */
	public Matrix confusionMatrix(){
		if(cachedConfusionMatrix!=null)
			return cachedConfusionMatrix;

		String[] classes=getClasses();
		// count up the errors
		double[][] confused=new double[classes.length][classes.length];
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			confused[classIndexOf(e.actual)][classIndexOf(e.predicted)]++;
		}
		cachedConfusionMatrix=new Matrix(confused);
		return cachedConfusionMatrix;
	}

	public double numErrors(){
		Matrix m=confusionMatrix();
		double errors=m.getValue(0,1)+m.getValue(1,0);
		return errors;
	}

	public String[] getClasses(){
		return schema.validClassNames();
	}

	/** Return array of true positive,false positive,logitScore.
	 */
	public Matrix TPfractionFPfractionScore(){
		if(cachedTPFPMatrix!=null)
			return cachedTPFPMatrix;

		if(!isBinary)
			throw new IllegalArgumentException(
			"can't compute precisionRecallScore for non-binary data");
		byBinaryScore();
		int allActualPos=0;
		int allActualNeg=0;
		int lastIndexOfActualPos=0;
		int firstIndexOfActualNeg=0;
		boolean notFoundYet=true;
		ProgressCounter pc=
			new ProgressCounter("counting positive examples","examples",entryList
					.size());
		for(int i=0;i<entryList.size();i++){
			if(getEntry(i).actual.isPositive()){
				allActualPos++;
				lastIndexOfActualPos=i;
			}else{
				allActualNeg++;
				if(notFoundYet){
					firstIndexOfActualNeg=i;
					notFoundYet=false;
				}
			}
			pc.progress();
		}
		pc.finished();
		//System.out.println("all pos = "+allActualPos+" all neg = "+allActualNeg);

		int length=Math.abs(lastIndexOfActualPos-firstIndexOfActualNeg)+4;
		int min=Math.min(lastIndexOfActualPos,firstIndexOfActualNeg);
		int max=Math.max(lastIndexOfActualPos,firstIndexOfActualNeg);
		//System.out.println("min="+min+" max="+max+" length="+length);
		double truePosSoFar=0;
		double falsePosSoFar=0;
		double tpf=1,fpf=1,score=0;
		ProgressCounter pc2=
			new ProgressCounter("computing statistics","examples",entryList.size());
		double[][] result=new double[length][3];
		for(int i=0;i<entryList.size();i++){
			Entry e=getEntry(i);
			score=e.predicted.posWeight();
			if(e.actual.isPositive())
				truePosSoFar++;
			else
				falsePosSoFar++;
			if(allActualPos>0)
				tpf=truePosSoFar/allActualPos;
			if(allActualNeg>0)
				fpf=falsePosSoFar/allActualNeg;
			if(i==0){
				result[0][0]=0.0;
				result[0][1]=0.0;
				result[0][2]=score;

			}
			if(i>=(min-1)&i<=max){
				result[i-min+2][0]=tpf;
				result[i-min+2][1]=fpf;
				result[i-min+2][2]=score;
				//System.out.println("tpf="+tpf+" fpf="+fpf+" score="+score);
			}
			result[length-1][0]=1.0;
			result[length-1][1]=1.0;
			result[length-1][2]=score;
			pc2.progress();
		}
		pc2.finished();
		cachedTPFPMatrix=new Matrix(result);
		return cachedTPFPMatrix;
	}

	/** Return actual ROC curve.
	 *  At most 1000 points are kept.
	 *
	 */
	public Matrix thousandPointROC(){
		Matrix m=TPfractionFPfractionScore();
		int N=m.values.length-2;

		if(N>1000){
			double[][] v=new double[1002][3];
			int mod=N/1000;

			// add (0,0,score)
			v[0][0]=m.values[0][0];
			v[0][1]=m.values[0][1];
			v[0][2]=m.values[0][2];
			// fill in 1000 values
			for(int i=1;i<=1000;i++){
				int k=(i-1)*mod;
				v[i][0]=m.values[k+1][0];
				v[i][1]=m.values[k+1][1];
				v[i][2]=m.values[k+1][2];
			}
			// add (1,1,score)
			v[1001][0]=m.values[N+1][0];
			v[1001][1]=m.values[N+1][1];
			v[1001][2]=m.values[N+1][2];

			return new Matrix(v);
		}else{
			return m;
		}
	}

	/** Return array of precision,recall,logitScore.
	 */
	public Matrix precisionRecallScore(){
		if(cachedPRCMatrix!=null)
			return cachedPRCMatrix;

		if(!isBinary)
			throw new IllegalArgumentException(
			"can't compute precisionRecallScore for non-binary data");
		byBinaryScore();
		int allActualPos=0;
		int lastIndexOfActualPos=0;
		ProgressCounter pc=
			new ProgressCounter("counting positive examples","examples",entryList
					.size());
		for(int i=0;i<entryList.size();i++){
			if(getEntry(i).actual.isPositive()){
				allActualPos++;
				lastIndexOfActualPos=i;
			}
			pc.progress();
		}
		pc.finished();
		double truePosSoFar=0;
		double falsePosSoFar=0;
		double precision=1,recall=1,score=0;
		ProgressCounter pc2=
			new ProgressCounter("computing statistics","examples",
					lastIndexOfActualPos);
		double[][] result=new double[lastIndexOfActualPos+1][3];
		for(int i=0;i<=lastIndexOfActualPos;i++){
			Entry e=getEntry(i);
			score=e.predicted.posWeight();
			if(e.actual.isPositive())
				truePosSoFar++;
			else
				falsePosSoFar++;
			if(truePosSoFar+falsePosSoFar>0)
				precision=truePosSoFar/(truePosSoFar+falsePosSoFar);
			if(allActualPos>0)
				recall=truePosSoFar/allActualPos;
			result[i][0]=precision;
			result[i][1]=recall;
			result[i][2]=score;
			pc2.progress();
		}
		pc2.finished();
		cachedPRCMatrix=new Matrix(result);
		return cachedPRCMatrix;
	}

	/** Return eleven-point interpolated precision.
	 * Precisely, result is an array p[] of doubles
	 * such that p[i] is the maximal precision value
	 * for any point with recall>=i/10.
	 *
	 */
	public double[] elevenPointPrecision(){
		Matrix m=precisionRecallScore();
		//System.out.println("prs = "+m);
		double[] p=new double[11];
		p[0]=1.0;
		for(int i=0;i<m.values.length;i++){
			double r=m.values[i][1];
			//System.out.println("row "+i+", recall "+r+": "+StringUtil.toString(m.values[i]));
			for(int j=1;j<=10;j++){
				if(r>=j/10.0){
					p[j]=Math.max(p[j],m.values[i][0]);
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
	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer("");
		for(int i=0;i<entryList.size();i++){
			buf.append(getEntry(i)+"\n");
		}
		return buf.toString();
	}

	static public class PropertyViewer extends ComponentViewer{

		static final long serialVersionUID=20080130L;

		@Override
		public JComponent componentFor(Object o){
			final Evaluation e=(Evaluation)o;
			final JPanel panel=new JPanel();
			final JTextField propField=new JTextField(10);
			final JTextField valField=new JTextField(10);
			final JTable table=makePropertyTable(e);
			final JScrollPane tableScroller=new JScrollPane(table);
			final JButton addButton=
				new JButton(new AbstractAction("Insert Property"){
					static final long serialVersionUID=20080130L;
					@Override
					public void actionPerformed(ActionEvent event){
						e.setProperty(propField.getText(),valField.getText());
						tableScroller.getViewport().setView(makePropertyTable(e));
						tableScroller.revalidate();
						panel.revalidate();
					}
				});
			panel.setLayout(new GridBagLayout());
			GridBagConstraints gbc=fillerGBC();
			//gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth=3;
			panel.add(tableScroller,gbc);
			panel.add(addButton,myGBC(0));
			panel.add(propField,myGBC(1));
			panel.add(valField,myGBC(2));
			return panel;
		}

		private GridBagConstraints myGBC(int col){
			GridBagConstraints gbc=fillerGBC();
			gbc.fill=GridBagConstraints.HORIZONTAL;
			gbc.gridx=col;
			gbc.gridy=1;
			return gbc;
		}

		private JTable makePropertyTable(final Evaluation e){
			Object[][] table=new Object[e.propertyKeyList.size()][2];
			for(int i=0;i<e.propertyKeyList.size();i++){
				table[i][0]=e.propertyKeyList.get(i);
				table[i][1]=e.properties.get(e.propertyKeyList.get(i));
			}
			String[] colNames=new String[]{"Property","Property's Value"};
			return new JTable(table,colNames);
		}
	}

	public class SummaryViewer extends ComponentViewer{
		static final long serialVersionUID=20080130L;
		@Override
		public JComponent componentFor(Object o){
			Evaluation e=(Evaluation)o;
			double[] ss=e.summaryStatistics();
			String[] ssn=e.summaryStatisticNames();
			Object[][] oss=new Object[ss.length][2];
			for(int i=0;i<ss.length;i++){
				oss[i][0]=ssn[i];
				oss[i][1]=new Double(ss[i]);
			}
			JTable jtable=new JTable(oss,new String[]{"Statistic","Value"});
			jtable.setDefaultRenderer(Object.class,new MyTableCellRenderer());
			jtable.setVisible(true);
			return new JScrollPane(jtable);
		}
	}

	static public class ElevenPointPrecisionViewer extends ComponentViewer{
		static final long serialVersionUID=20080130L;
		@Override
		public JComponent componentFor(Object o){
			Evaluation e=(Evaluation)o;
			double[] p=e.elevenPointPrecision();
			LineCharter lc=new LineCharter();
			lc.startCurve("Interpolated Precision");
			for(int i=0;i<p.length;i++){
				lc.addPoint(i/10.0,p[i]);
			}
			return lc.getPanel("11-Pt Interpolated Precision vs. Recall","Recall",
			"Precision");
		}
	}

	static public class ROCViewer extends ComponentViewer{
		static final long serialVersionUID=20080130L;
		@Override
		public JComponent componentFor(Object o){
			Evaluation e=(Evaluation)o;

			Matrix p=e.thousandPointROC();
			LineCharter lc=new LineCharter();
			lc.startCurve("Actual ROC");

			for(int i=0;i<p.values.length;i++){
				lc.addPoint(p.values[i][1],p.values[i][0]);
				//System.out.println(p.values[i][0]+" "+p.values[i][1]);  // Uncomment for MATLAB
			}
			// compute area under the curve
			double area=0.0;
			for(int i=0;i<(p.values.length-1);i++){
				area+=
					(p.values[i][0]+p.values[i+1][0])*
					(p.values[i+1][1]-p.values[i][1])/2.0;
				//System.out.println("("+p.values[i][0]+"+"+p.values[i+1][0]+") * ("+p.values[i+1][1]+"-"+p.values[i][1]+") /2.0");
			}
			return lc.getPanel("Actual ROC Curve",
					"False Positive / All Negative   (AUC = "+area+")",
			"True Positive / All Positive");

		}
	}

	static public class ConfusionMatrixViewer extends ComponentViewer{
		static final long serialVersionUID=20080130L;
		@Override
		public JComponent componentFor(Object o){
			Evaluation e=(Evaluation)o;
			JPanel panel=new JPanel();
			Matrix m=e.confusionMatrix();
			String[] classes=e.getClasses();
			panel.setLayout(new GridBagLayout());
			//add( new JLabel("Actual class"), cmGBC(0,0) );
			GridBagConstraints gbc=cmGBC(0,1);
			gbc.gridwidth=classes.length;
			panel.add(new JLabel("Predicted Class"),gbc);
			for(int i=0;i<classes.length;i++){
				panel.add(new JLabel(classes[i]),cmGBC(1,i+1));
			}
			for(int i=0;i<classes.length;i++){
				panel.add(new JLabel(classes[i]),cmGBC(i+2,0));
				for(int j=0;j<classes.length;j++){
					panel.add(new JLabel(Double.toString(m.values[i][j])),cmGBC(i+2,j+1));
				}
			}
			return panel;
		}

		private GridBagConstraints cmGBC(int i,int j){
			GridBagConstraints gbc=new GridBagConstraints();
			//gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx=gbc.weighty=0;
			gbc.gridy=i;
			gbc.gridx=j;
			gbc.ipadx=gbc.ipady=20;
			return gbc;
		}
	}

	/** Print summary statistics
	 */
	public void summarize(){
		double[] stats=summaryStatistics();
		String[] statNames=summaryStatisticNames();
		int maxLen=0;
		for(int i=0;i<statNames.length;i++){
			maxLen=Math.max(statNames[i].length(),maxLen);
		}
		for(int i=0;i<statNames.length;i++){
			System.out.print(statNames[i]+": ");
			for(int j=0;j<maxLen-statNames[i].length();j++)
				System.out.print(" ");
			System.out.println(stats[i]);
		}
	}

	@Override
	public Viewer toGUI(){
		ParallelViewer main=new ParallelViewer();

		main.addSubView("Summary",new SummaryViewer());
		main.addSubView("Properties",new PropertyViewer());
		if(isBinary)
			main.addSubView("11Pt Precision/Recall",new ElevenPointPrecisionViewer());
		if(isBinary)
			main.addSubView(" ROC & AUC ",new ROCViewer());
		main.addSubView("Confusion Matrix",new ConfusionMatrixViewer());
		main.addSubView("Debug",new VanillaViewer());
		main.setContent(this);

		return main;
	}

	//
	// one entry in the evaluation
	//
	private static class Entry implements Serializable{

		private static final long serialVersionUID=-4069980043842319179L;

		transient public Instance instance=null;

		public int partitionID;

		public int index;

		public ClassLabel predicted,actual;

		//public int h;

		public double w=1.0;

		public Entry(Instance i,ClassLabel p,ClassLabel a,int k,int id){
			instance=i;
			predicted=p;
			actual=a;
			index=k;
			partitionID=id;
			//h=instance.hashCode();
		}

		@Override
		public String toString(){
			//double w=predicted.bestWeight();
			return predicted+"\t"+actual+"\t"+instance;
		}
	}

	//
	// implement Saveable
	//
	final static public String EVAL_FORMAT_NAME="Minorthird Evaluation";

	final static public String EVAL_EXT=".eval";

	@Override
	public String[] getFormatNames(){
		return new String[]{EVAL_FORMAT_NAME};
	}

	@Override
	public String getExtensionFor(String format){
		return EVAL_EXT;
	}

	@Override
	public void saveAs(File file,String formatName) throws IOException{
		save(file);
	}

	@Override
	public Object restore(File file) throws IOException{
		return load(file);
	}

	//
	//
	public void save(File file) throws IOException{
		PrintStream out=
			new PrintStream(new GZIPOutputStream(new FileOutputStream(file)));
		save(out);
	}

	public void save(PrintStream out) throws IOException{
		out.println(StringUtil.toString(schema.validClassNames()));
		for(Iterator<String> i=propertyKeyList.iterator();i.hasNext();){
			String prop=(String)i.next();
			String value=properties.getProperty(prop);
			out.println(prop+"="+value);
		}
		byOriginalPosition();
		for(Iterator<Entry> i=entryList.iterator();i.hasNext();){
			Entry e=(Entry)i.next();
			out.println(e.predicted.bestClassName()+" "+e.predicted.bestWeight()+" "+
					e.actual.bestClassName());
		}
		out.close();
	}

	static public Evaluation load(File file) throws IOException{
		// disabled to avoid looping, since this is how we now de-serialize
		// first try loading a serialized version
		//try {	return (Evaluation)IOUtil.loadSerialized(file); } catch (Exception ex) { ;  }

		LineNumberReader in=
			new LineNumberReader(new InputStreamReader(new GZIPInputStream(
					new FileInputStream(file))));
		String line=in.readLine();
		if(line==null)
			throw new IllegalArgumentException("no class list on line 1 of file "+
					file.getName());
		String[] classes=line.substring(1,line.length()-1).split(",");
		ExampleSchema schema=new ExampleSchema(classes);
		Evaluation result=new Evaluation(schema);
		while((line=in.readLine())!=null){
			if(line.indexOf('=')>=0){
				// property
				String[] propValue=line.split("=");
				if(propValue.length==2){
					result.setProperty(propValue[0],propValue[1]);
				}else if(propValue.length==1){
					result.setProperty(propValue[0],"");
				}else{
					throw new IllegalArgumentException(file.getName()+" line "+
							in.getLineNumber()+": illegal format");
				}
			}else{
				String[] words=line.split(" ");
				if(words.length<3)
					throw new IllegalArgumentException(file.getName()+" line "+
							in.getLineNumber()+": illegal format");
				ClassLabel predicted=new ClassLabel(words[0],StringUtil.atof(words[1]));
				ClassLabel actual=new ClassLabel(words[2]);
				//double instanceWeight = StringUtil.atof(words[3]);
				MutableInstance instance=new MutableInstance("dummy");
				//instance.setWeight( instanceWeight );
				Example example=new Example(instance,actual);
				result.extend(predicted,example,DEFAULT_PARTITION_ID);
			}
		}
		in.close();
		return result;
	}

	//
	// getters / setters
	//

	/** Returns whether this Evaluation refers to a binary classifier */
	public boolean isBinary(){
		return this.isBinary;
	}

	/** Returns whether the ExampleSchema this Evaluation is based upon */
	public ExampleSchema getSchema(){
		return this.schema;
	}

	//
	// convenience methods
	//
	private Entry getEntry(int i){
		return (Entry)entryList.get(i);
	}

	private int classIndexOf(ClassLabel classLabel){
		return classIndexOf(classLabel.bestClassName());
	}

	private int classIndexOf(String classLabelName){
		return schema.getClassIndex(classLabelName);
	}

	private void extendSchema(ClassLabel classLabel){
		//System.out.println("classLabel: "+classLabel);
		if(!classLabel.isBinary())
			isBinary=false;
		int r=classIndexOf(classLabel.bestClassName());
		if(r<0){
			//System.out.println("extending");
			// extend the schema

			//Add the provided label to the set of valid values 
			//for the class using the extend method on the
			//schema object
			schema.extend(classLabel.bestClassName());

			//commented old code 
			//String[] currentNames = schema.validClassNames();
			//String[] newNames = new String[currentNames.length+1];
			//for (int i=0; i<currentNames.length; i++) newNames[i] = currentNames[i];
			//newNames[currentNames.length] = classLabel.bestClassName();
		}
	}

	private void byBinaryScore(){
		Collections.sort(entryList,new Comparator<Entry>(){

			@Override
			public int compare(Entry a,Entry b){
				return MathUtil.sign(b.predicted.posWeight()-a.predicted.posWeight());
			}
		});
	}

	private void byOriginalPosition(){
		Collections.sort(entryList,new Comparator<Entry>(){

			@Override
			public int compare(Entry a,Entry b){
				return a.index-b.index;
			}
		});
	}

	// table renderer
	public class MyTableCellRenderer extends DefaultTableCellRenderer{

		static final long serialVersionUID=20080130L;

		@Override
		public Component getTableCellRendererComponent(JTable table,Object value,
				boolean isSelected,boolean hasFocus,int row,int column){
			JLabel label=
				(JLabel)super.getTableCellRendererComponent(table,value,isSelected,
						hasFocus,row,column);
			if((row%2)!=0){
				label.setBackground(Color.lightGray);
				label.setOpaque(true);
			}else{
				label.setBackground(Color.white);
				label.setOpaque(true);
			}
			return label;
		}
	}

	//
	// test routine
	//
	static public void main(String[] args){
		try{
			Evaluation v=Evaluation.load(new File(args[0]));
			if(args.length>1)
				v.save(new File(args[1]));
			new ViewerFrame("From file "+args[0],v.toGUI());
		}catch(Exception e){
			System.out
			.println("usage: Evaluation [serializedFile|evaluationFile] [evaluationFile]");
			e.printStackTrace();
		}
	}
}
