/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

import edu.cmu.minorthird.classify.multi.*;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import org.apache.log4j.Logger;
import edu.cmu.minorthird.classify.relational.*;

import java.io.*;
import java.util.*;

/**
 * Dataset i/o.
 *
 * For ordinary datasets, format is one example per line, and the
 * format for a line is: 
 * <pre>
 * <code>type subpopid label feature1 feature2 ...</code>
 * </pre>where 
 * <ul>
 * <li>type=b or k (for binary or ordinary examples)
 * <li>subpopid is NUL or a string, naming the subpopulation from which the example was drawn
 * <li>label is +1 or -1 for binary
 * <li>features are a feature name (for binary features) and a featureName=value for numeric
 * features.
 * </ul>
 *
 * For example: <code>
 * k subpop1 2 f1=4 fOrange=1 fGreen=92 ...
 * k subpop1 1 f1=1 fBlue=10 fGreen=2 ...
 * k subpop2 3 f1=2 fYellow=1 fRed=42 ...
 * </code> or <code>
 * b NUL +1 f1=2 fOrange=1 fGreen=92 ...
 * b NUL -1 f1=1 fBlue=10 fGreen=2 ...
 * </code> or <code>
 * k subpop1 2 f1 fOrange fGreen ...
 * k subpop1 1 f1 fBlue fGreen ...
 * k subpop2 3 f1 fYellow fRed ...
 * </code>
 *
 * For SequenceDatasets, examples from a diffrerent sequence are separated by a single line
 * containing a "*".
 * 
 * @author William Cohen
 */

public class DatasetLoader{

	static private Logger log=Logger.getLogger(DatasetLoader.class);

	static private final StringEncoder stringCoder=new StringEncoder('%'," \t");

	static private final StringEncoder featureCoder=
			new StringEncoder('%',"=. \t");

	static private Map<String,ClassLabel> classLabelDict=new HashMap<String,ClassLabel>();
	
	static{
		classLabelDict.put(ExampleSchema.POS_CLASS_NAME,ClassLabel
				.positiveLabel(+1));
		classLabelDict.put(ExampleSchema.NEG_CLASS_NAME,ClassLabel
				.negativeLabel(-1));
	}

	/** Save a dataset to a file.  This should save each example in
	 * the order provided by the dataset.iterator() 
	 */
	static public void save(Dataset dataset,File file) throws IOException{
		PrintStream out=new PrintStream(new FileOutputStream(file));
		for(Iterator<Example> i=dataset.iterator();i.hasNext();){
			out.println(asParsableString(i.next()));
		}
	}

	/** Save a dataset that can be used for regression */
	static public void saveRegression(Dataset dataset,File file)
			throws IOException{
		PrintStream out=new PrintStream(new FileOutputStream(file));
		for(Iterator<Example> i=dataset.iterator();i.hasNext();){
			Example x=i.next();
			StringBuffer buf=new StringBuffer("");
			buf.append(x.getLabel().posWeight());
			buf.append('\t');
			buf.append(asParsableString(x));
			out.println(buf.toString());
		}
		out.close();
	}

	/** Save a dataset that can be used for regression */
	static public Dataset loadRegression(File file) throws IOException,
			NumberFormatException{
		Dataset dataset=new BasicDataset();
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null){
			int tab=line.indexOf('\t');
			Example x=parseLine(line.substring(tab+1),file,in);
			double score=StringUtil.atof(line.substring(0,tab));
			dataset.add(new Example(x.asInstance(),ClassLabel.positiveLabel(score)));
		}
		log.info("loaded "+dataset.size()+" examples from "+file.getName());
		in.close();
		return dataset;
	}

	/** Load a dataset from a file */
	static public Dataset loadFile(File file) throws IOException,
			NumberFormatException{
		Dataset dataset=new BasicDataset();
		ProgressCounter pc=
				new ProgressCounter("loading file "+file.getName(),"line");
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null){
			dataset.add(parseLine(line,file,in));
			pc.progress();
		}
		log.info("loaded "+dataset.size()+" examples from "+file.getName());
		in.close();
		pc.finished();
		return dataset;
	}

	/** Load a relational dataset from a file specifying objs */
	static public void loadRelFile(File file,RealRelationalDataset dataset)
			throws IOException,NumberFormatException{
//	Dataset dataset = new BasicDataset();
		ProgressCounter pc=
				new ProgressCounter("loading file "+file.getName(),"line");
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null){
			dataset.addSGM(RelparseLine(line,file,in));
//	    System.out.println(dataset);
			pc.progress();
		}
		log.info("loaded "+dataset.size()+" examples from "+file.getName());
		in.close();
		pc.finished();
//	return dataset;
	}

	/** Load a link file */
	static public void loadLinkFile(File file,RealRelationalDataset dataset)
			throws IOException,NumberFormatException{
//	Dataset dataset = new RealRelationalDataset();
		ProgressCounter pc=
				new ProgressCounter("loading file "+file.getName(),"line");
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null){
			CoreRelationalDataset.addLink(LinkparseLine(line,file,in));
			pc.progress();
		}
		log.info("loaded "+dataset.size()+" examples from "+file.getName());
		in.close();
		pc.finished();
//	return dataset;
	}

	/** Load a relational template file */
	static public void loadRelTempFile(File file,RealRelationalDataset dataset)
			throws IOException,NumberFormatException{
//	Dataset dataset = new RealRelationalDataset();
		ProgressCounter pc=
				new ProgressCounter("loading file "+file.getName(),"line");
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null){

			String[] arr=line.split("\\s+");
			if(arr.length<3)
				throw new IllegalArgumentException("too few values at line#"+
						in.getLineNumber()+" of "+file.getName());
			if(!arr[1].equals("ON"))
				throw new IllegalArgumentException(
						"the format of the relational template is COUNT ON LEFT");

			RealRelationalDataset.addAggregator(arr[0],arr[2]);
			pc.progress();
		}
		log.info("loaded "+dataset.size()+" examples from "+file.getName());
		in.close();
		pc.finished();
//	return dataset;
	}

	/** Load a dataset from a file */
	static public Dataset loadMulti(File file,int numDim) throws IOException,
			NumberFormatException{
		MultiDataset dataset=new MultiDataset();
		ProgressCounter pc=
				new ProgressCounter("loading file "+file.getName(),"line");
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null){
			dataset.addMulti(parseMultiLine(line,file,in,numDim));
			pc.progress();
		}
		log.info("loaded "+dataset.size()+" examples from "+file.getName());
		in.close();
		pc.finished();
		return dataset;
	}

	/**
	 * Save a SequenceDataset to a file
	 *
	 * Each Example in a sequence is saved on a seperate line.
	 * An asterix (*) alone on a line seperates the sequences.
	 *
	 * Example1,0
	 * Example1,1
	 * Example1,2
	 * ...
	 * *
	 * Example2,0
	 * Example2,1
	 * ...
	 * *
	 */
	static public void saveSequence(SequenceDataset dataset,File file)
			throws IOException{
		PrintStream out=new PrintStream(new FileOutputStream(file));
		for(Iterator<Example[]> i=dataset.sequenceIterator();i.hasNext();){
			Example[] seq=i.next();
			for(int j=0;j<seq.length;j++){
				out.println(asParsableString(seq[j]));
			}
			out.println("*");
		}
		out.close();
	}

	/**
	 * Load a SequenceDataset from a file
	 * @see #saveSequence for format
	 */
	static public SequenceDataset loadSequence(File file) throws IOException,
			NumberFormatException{
		SequenceDataset dataset=new SequenceDataset();
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		List<Example> list=new ArrayList<Example>();
		while((line=in.readLine())!=null){
			if("*".equals(line))
				clearBuffer(list,dataset);
			else
				list.add(parseLine(line,file,in));
		}
		if(list.size()>0){
			clearBuffer(list,dataset);
		}
		log.info("loaded "+dataset.size()+" examples from "+file.getName());
		in.close();
		return dataset;
	}

	static private void clearBuffer(List<Example> list,SequenceDataset dataset){
		Example[] seq=list.toArray(new Example[list.size()]);
		dataset.addSequence(seq);
		list.clear();
	}

	static private String asParsableString(Example x){
		StringBuffer buf=new StringBuffer("");
		//buf.append((x instanceof BinaryExample) ? "b" : "k" );
		buf.append('k'); // backward compatibility for binary examples
		buf.append(' ');
		buf.append(stringCoder.encode(x.getSubpopulationId()!=null?x
				.getSubpopulationId():"NUL"));
		buf.append(' ');
		buf.append(stringCoder.encode(x.getLabel().bestClassName()));
		buf.append(' ');
		appendParsableFeatures(buf,x);
		return buf.toString();
	}

	static void appendParsableFeatures(StringBuffer buf,Example x){
		for(Iterator<Feature> i=x.binaryFeatureIterator();i.hasNext();){
			Feature f=i.next();
			buf.append(' ');
			for(int j=0;j<f.size();j++){
				if(j>0)
					buf.append('.');
				buf.append(featureCoder.encode(f.getPart(j)));
			}
		}
		for(Iterator<Feature> i=x.numericFeatureIterator();i.hasNext();){
			Feature f=i.next();
			buf.append(' ');
			for(int j=0;j<f.size();j++){
				if(j>0)
					buf.append('.');
				buf.append(featureCoder.encode(f.getPart(j)));
			}
			buf.append("="+x.getWeight(f));
		}
	}

	/** The value that will be returned by example.getSource() for the example
	 * read in from the designated location.
	 */
	static public String getSourceAssignedToExample(String fileName,int lineNumber){
		return fileName+":"+lineNumber;
	}

	static private Example parseLine(String line,File file,LineNumberReader in){
		String[] arr=line.split("\\s+");
		if(arr.length<3)
			throw new IllegalArgumentException("too few values at line#"+
					in.getLineNumber()+" of "+file.getName());
		for(int i=0;i<3;i++)
			arr[i]=stringCoder.decode(arr[i]);
		String subpopulationId=arr[1];
		String source=getSourceAssignedToExample(file.getName(),in.getLineNumber());
		if("NUL".equals(arr[1]))
			subpopulationId=null;
		MutableInstance instance=new MutableInstance(source,subpopulationId);
		for(int i=3;i<arr.length;i++){
			int eqPos=arr[i].indexOf("=");
			if(eqPos>=0){
				try{
					String feature=arr[i].substring(0,eqPos);
					String value=arr[i].substring(eqPos+1);
					double weight=Double.parseDouble(value);
					instance.addNumeric(parseFeatureName(feature),weight);
				}catch(NumberFormatException e){
					throw new IllegalArgumentException("bad feature# "+i+" line#"+
							in.getLineNumber()+" of "+file.getName());
				}
			}else{
				instance.addBinary(parseFeatureName(arr[i]));
			}
		}
		ClassLabel label=classLabelDict.get(arr[2]);
		if(label==null){
			if("b".equals(arr[0])){
				throw new IllegalArgumentException("should be POS/NEG but label is '"+
						arr[2]+"' at line#"+in.getLineNumber()+" of "+file.getName());
			}
			classLabelDict.put(arr[2],(label=new ClassLabel(arr[2])));
		}
		return new Example(instance,label);
	}

	static private SGMExample RelparseLine(String line,File file,
			LineNumberReader in){
		String[] arr=line.split("\\s+");
		if(arr.length<4)
			throw new IllegalArgumentException("too few values at line#"+
					in.getLineNumber()+" of "+file.getName());
		String ID=arr[0];
		for(int i=1;i<4;i++)
			arr[i]=stringCoder.decode(arr[i]);
		String subpopulationId=arr[2];
		String source=getSourceAssignedToExample(file.getName(),in.getLineNumber());
		if("NUL".equals(arr[2]))
			subpopulationId=null;
		MutableInstance instance=new MutableInstance(source,subpopulationId);
		for(int i=4;i<arr.length;i++){
			int eqPos=arr[i].indexOf("=");
			if(eqPos>=0){
				try{
					String feature=arr[i].substring(0,eqPos);
					String value=arr[i].substring(eqPos+1);
					double weight=Double.parseDouble(value);
					instance.addNumeric(parseFeatureName(feature),weight);
				}catch(NumberFormatException e){
					throw new IllegalArgumentException("bad feature# "+i+" line#"+
							in.getLineNumber()+" of "+file.getName());
				}
			}else{
				instance.addBinary(parseFeatureName(arr[i]));
			}
		}
		ClassLabel label=classLabelDict.get(arr[3]);
		if(label==null){
			if("b".equals(arr[1])){
				throw new IllegalArgumentException("should be POS/NEG but label is '"+
						arr[3]+"' at line#"+in.getLineNumber()+" of "+file.getName());
			}
			classLabelDict.put(arr[3],(label=new ClassLabel(arr[3])));
		}
		return new SGMExample(instance,label,ID);
	}

	static private Link LinkparseLine(String line,File file,LineNumberReader in){
		String[] arr=line.split("\\s+");
		if(arr.length<3)
			throw new IllegalArgumentException("too few values at line#"+
					in.getLineNumber()+" of "+file.getName());
		return new Link(arr[0],arr[1],arr[2]);
	}

	static private MultiExample parseMultiLine(String line,File file,
			LineNumberReader in,int numDim){
		String[] arr=line.split("\\s+");
		if(arr.length<2+numDim)
			throw new IllegalArgumentException("too few values at line#"+
					in.getLineNumber()+" of "+file.getName());
		for(int i=0;i<2+numDim;i++)
			arr[i]=stringCoder.decode(arr[i]);
		String subpopulationId=arr[1];
		String source=file.getName()+":"+in.getLineNumber();
		if("NUL".equals(arr[1]))
			subpopulationId=null;
		MutableInstance instance=new MutableInstance(source,subpopulationId);
		for(int i=2+numDim;i<arr.length;i++){
			int eqPos=arr[i].indexOf("=");
			if(eqPos>=0){
				try{
					String feature=arr[i].substring(0,eqPos);
					String value=arr[i].substring(eqPos+1);
					double weight=Double.parseDouble(value);
					instance.addNumeric(parseFeatureName(feature),weight);
				}catch(NumberFormatException e){
					throw new IllegalArgumentException("bad feature# "+i+" line#"+
							in.getLineNumber()+" of "+file.getName());
				}
			}else{
				instance.addBinary(parseFeatureName(arr[i]));
			}
		}
		ClassLabel[] labels=new ClassLabel[numDim];
		for(int i=2;i<2+numDim;i++){
			ClassLabel label=classLabelDict.get(arr[i]);
			labels[i-2]=label;
		}
		for(int i=0;i<labels.length;i++){
			if(labels[i]==null){
				if("b".equals(arr[0])){
					throw new IllegalArgumentException(
							"should be POS/NEG but label is '"+arr[2+i]+"' at line#"+
									in.getLineNumber()+" of "+file.getName());
				}
				classLabelDict.put(arr[2+i],(labels[i]=new ClassLabel(arr[2+i])));
			}
		}
		//
		MultiClassLabel multiLabel=new MultiClassLabel(labels);
		return new MultiExample(instance,multiLabel);
	}

	static private Feature parseFeatureName(String string){
		String[] featureParts=string.split("\\.");
		for(int j=0;j<featureParts.length;j++)
			featureParts[j]=featureCoder.decode(featureParts[j]);
		return new Feature(featureParts);
	}

	public static Dataset loadSVMStyle(File file) throws IOException{
		Dataset dataset=new BasicDataset();
		BufferedReader in=new BufferedReader(new FileReader(file));
		while(in.ready()){
			String line=in.readLine();
			StringTokenizer st=new StringTokenizer(line," \t\n\r\f:");

			//label - yes this is necessary:
			//the original string representation and the reconstituted versions are different.
			//ex: (string)+1 => (double)1.0 => (string)1.0
			MutableInstance instance=new MutableInstance();
			String label=st.nextToken();
			double labelDouble=Double.parseDouble(label);
			label=""+labelDouble;

			//num features
			//      int numFeatures = st.countTokens()/2;
			while(st.hasMoreTokens()){
				//add features to instance
				// note for svm these should be numeric
				String featureName=st.nextToken();
				String featureValue=st.nextToken();
				instance.addNumeric(new Feature(featureName),Double
						.parseDouble(featureValue));
			}

			//build Example
			Example example=new Example(instance,ClassLabel.binaryLabel(labelDouble));

			dataset.add(example);
		}

		return dataset;
	}

	/**
	 * Calls loadFile.  The Dataset is temporarily swallowed.
	 * In other words, don't call this method.
	 * @param f
	 * @throws IOException
	 */
	public Object load(File f) throws IOException{
		return loadFile(f);
	}

	static public void main(String[] args){
		try{
			boolean sequential=args[0].startsWith("-seq");
			boolean regression=args[0].startsWith("-reg");
			String dbName=(sequential||regression)?args[1]:args[0];
			Dataset d=null;
			if(sequential)
				d=DatasetLoader.loadSequence(new File(dbName));
			else if(regression)
				d=DatasetLoader.loadRegression(new File(dbName));
			else
				d=DatasetLoader.loadFile(new File(dbName));
			new ViewerFrame("Data from "+dbName,d.toGUI());
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("usage: file");
		}
	}

}
