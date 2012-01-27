package edu.cmu.minorthird.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.classify.sequential.SequenceDataset;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.learn.BeginContinueEndUniqueReduction;
import edu.cmu.minorthird.text.learn.Extraction2TaggingReduction;
import edu.cmu.minorthird.text.learn.SequenceAnnotatorLearner;
import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Preprocess extraction text data for sequential learning methods.
 * 
 * @author William Cohen
 */

public class PreprocessTextForExtractor extends PreprocessTextForClassifier{

	protected int historySize=1;

	protected Extraction2TaggingReduction reduction=
			new BeginContinueEndUniqueReduction();

	public PreprocessTextForExtractor(){
		super();
		this.fe=new Recommended.TokenFE();
	}

	public class ExtractionReductionParams extends BasicCommandLineProcessor{

		public void history(String s){
			historySize=StringUtil.atoi(s);
		}

		public void reduction(String s){
			reduction=
					(Extraction2TaggingReduction)CommandLineUtil.newObjectFromBSH(s,
							Extraction2TaggingReduction.class);
		}
	}

	public void usage(){
		System.out.println("extraction-related parameters:");
		System.out.println(" [-history N]               "
				+"number of previous classes to use as features");
		System.out.println(" [-reduction beanshell]     "
				+"how to map tokens to classes");
		System.out.println();
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{
				new LinkFileParams(),new ExtractionReductionParams(),gui,base,signal,
				save});
	}

	public int getHistorySize(){
		return historySize;
	}

	public void setHistorySize(int n){
		this.historySize=n;
	}

	public Extraction2TaggingReduction getReduction(){
		return reduction;
	}

	public void setReduction(Extraction2TaggingReduction r){
		this.reduction=r;
	}

	//
	// do it
	// 

	@Override
	public void doMain(){
		// check that inputs are valid
		if(signal.spanProp==null&&signal.spanType==null){
			throw new IllegalArgumentException(
					"one of -spanProp or -spanType must be specified");
		}
		if(signal.spanProp!=null&&signal.spanType!=null){
			throw new IllegalArgumentException(
					"only one of -spanProp or -spanType can be specified");
		}
		if(save.saveAs==null){
			throw new IllegalArgumentException("-saveAs must be specified");
		}

		dataset=
				SequenceAnnotatorLearner.prepareSequenceData(base.labels,
						signal.spanType,signal.spanProp,fe,historySize,reduction);

		try{
			DatasetLoader.saveSequence((SequenceDataset)dataset,save.saveAs);
		}catch(IOException ex){
			System.out.println("error saving sequential dataset to '"+save.saveAs+
					"': "+ex);
		}

		if(base.showResult){
			new ViewerFrame("Dataset",dataset.toGUI());
		}

		if(linkFileName!=null){
			try{
				saveLinkInfoSequence(new File(linkFileName),(SequenceDataset)dataset,
						save.getSaveAs());
			}catch(IOException ex){
				System.out.println("error saving link information to '"+linkFileName+
						"': "+ex);
			}
		}

	}

	private void saveLinkInfoSequence(File linkFile,SequenceDataset dataset,
			String datasetFileName) throws IOException{
		int lineNo=0;
		PrintStream out=new PrintStream(new FileOutputStream(linkFile));
		for(Iterator<Example[]> i=dataset.sequenceIterator();i.hasNext();){
			Example[] seq=i.next();
			for(int j=0;j<seq.length;j++){
				Example ex=seq[j];
				lineNo++;
				if(!(ex.getSource() instanceof Span)){
					throw new IllegalArgumentException(
							"example not associated with a span: "+ex);
				}
				Span span=(Span)ex.getSource();
				out.println(DatasetLoader.getSourceAssignedToExample(datasetFileName,
						lineNo)+
						" "+
						span.getDocumentId()+
						" "+
						span.getLoChar()+
						" "+
						(span.getHiChar()-span.getLoChar()));
			}
			lineNo++; // count a line for the sequence terminator '*'
		}
		out.close();
	}

	public static void main(String args[]){
		new PreprocessTextForExtractor().callMain(args);
	}
}
