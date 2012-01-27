package edu.cmu.minorthird.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.DatasetLoader;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;
import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Preprocess text data for classification.
 *
 * @author William Cohen
 */

public class PreprocessTextForClassifier extends UIMain{

//	private static Logger log=Logger.getLogger(PreprocessTextForClassifier.class);

	protected String linkFileName=null;

	protected SpanFeatureExtractor fe=new Recommended.DocumentFE();

	protected CommandLineUtil.SaveParams save=new CommandLineUtil.SaveParams();

	protected CommandLineUtil.ClassificationSignalParams signal=
			new CommandLineUtil.ClassificationSignalParams(base);

	protected Dataset dataset;

	public class LinkFileParams extends BasicCommandLineProcessor{

		public String linkFileHelp=
				"file to save mapping between examples and spans they correspond to";

		public void linkFile(String s){
			linkFileName=s;
		}

		public CommandLineProcessor fe(String s){
			fe=
					(SpanFeatureExtractor)CommandLineUtil.newObjectFromBSH(s,
							SpanFeatureExtractor.class);
			return (fe instanceof CommandLineProcessor.Configurable)?tryToGetCLP(fe)
					:null;
		}

		public CommandLineProcessor feOp(){
			return tryToGetCLP(fe);
		}

		@Override
		public void usage(){
			System.out.println("special parameters:");
			System.out.println(" [-linkFile FILE]           "+linkFileHelp);
			System.out.println(" [-fe beanshell]            "+"feature extractor");
			System.out.println(" [-feOp opt1 ...]           "
					+"options for feature extractor");
			System.out.println();

		}

		public String getLinkFileHelp(){
			return linkFileHelp;
		}
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{
				new LinkFileParams(),gui,base,signal,save});
	}

	public String getLinkFile(){
		return linkFileName;
	}

	public void setLinkFile(String s){
		linkFileName=s;
	}

	public SpanFeatureExtractor getFeatureExtractor(){
		return fe;
	}

	public void setFeatureExtractor(SpanFeatureExtractor fe){
		this.fe=fe;
	}

	public CommandLineUtil.ClassificationSignalParams getSignalParameters(){
		return signal;
	}

	public void setSignalParameters(CommandLineUtil.ClassificationSignalParams p){
		signal=p;
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

		// construct the dataset and save it
		//if (tagDataFlag) {
		//	    dataset = 
		//SequenceAnnotatorLearner.prepareSequenceData(base.labels,signal.spanProp,signal.spanType,fe,historySize,reduction);

		dataset=
				CommandLineUtil.toDataset(base.labels,fe,signal.spanProp,
						signal.spanType,signal.candidateType);
		try{
			DatasetLoader.save(dataset,save.saveAs);
		}catch(IOException ex){
			System.out.println("error saving dataset to '"+save.saveAs+"': "+ex);
		}

		if(base.showResult){
			new ViewerFrame("Dataset",dataset.toGUI());
		}

		if(linkFileName!=null){
			try{
				saveLinkInfo(new File(linkFileName),dataset,save.getSaveAs());
			}catch(IOException ex){
				System.out.println("error saving link information to '"+linkFileName+
						"': "+ex);
			}
		}

	}

	private void saveLinkInfo(File linkFile,Dataset dataset,String datasetFileName)
			throws IOException{
		int lineNo=0;
		PrintStream out=new PrintStream(new FileOutputStream(linkFile));
		for(Iterator<Example> i=dataset.iterator();i.hasNext();){
			Example ex=i.next();
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
		out.close();
	}

	@Override
	public Object getMainResult(){
		return dataset;
	}

	public static void main(String args[]){
		new PreprocessTextForClassifier().callMain(args);
	}
}
