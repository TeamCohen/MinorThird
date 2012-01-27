package edu.cmu.minorthird.ui;

import java.io.IOException;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.Annotator;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Apply a serialized annotator.
 * 
 * @author William Cohen
 */

public class ApplyAnnotator extends UIMain{

	static Logger log=Logger.getLogger(ApplyAnnotator.class);

	// private data needed to test a classifier

	private CommandLineUtil.SaveParams save=new CommandLineUtil.SaveParams();

	private CommandLineUtil.LoadAnnotatorParams load=
			new CommandLineUtil.LoadAnnotatorParams();

	private CommandLineUtil.AnnotatorOutputParams output=
			new CommandLineUtil.AnnotatorOutputParams();

	private TextLabels annLabels=null;

	// for gui
	public CommandLineUtil.SaveParams getSaveParameters(){
		return save;
	}

	public void setSaveParameters(CommandLineUtil.SaveParams p){
		save=p;
	}

	public CommandLineUtil.LoadAnnotatorParams getLoadAnnotatorParameters(){
		return load;
	}

	public void setLoadAnnotatorParameters(CommandLineUtil.LoadAnnotatorParams p){
		load=p;
	}

	public CommandLineUtil.AnnotatorOutputParams getAnnotatorOutputParams(){
		return output;
	}

	public void setAnnotatorOutputParams(CommandLineUtil.AnnotatorOutputParams p){
		output=p;
	}

	public String getApplyAnnotatorHelp(){
		return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/ApplyAnnotator%20Tutorial.htm\">ApplyAnnotator Tutorial</A></html>";
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,
				save,load,output});
	}

	//
	// load and test a classifier
	// 

	@Override
	public void doMain(){
		
		// check that inputs are valid
		if(load.loadFrom==null){
			throw new IllegalArgumentException("-loadFrom must be specified");
		}

		// load the classifier
		Annotator ann=null;
		try{
			ann=(Annotator)IOUtil.loadSerialized(load.loadFrom);
		}catch(IOException ex){
			throw new IllegalArgumentException("Cannot load annotator "+load.loadFrom);
		}

		// do the annotation
		annLabels=ann.annotatedCopy(base.labels);

		// echo the annotated labels
		if(base.showResult){
			new ViewerFrame("Annotated Textbase",new SmartVanillaViewer(annLabels));
		}

		if(save.saveAs!=null){
			try{
				if("minorthird".equals(output.format)){
					new TextLabelsLoader().saveTypesAsOps(annLabels,save.saveAs);
				}else if("strings".equals(output.format)){
					new TextLabelsLoader().saveTypesAsStrings(annLabels,save.saveAs,true);
				}else if("xml".equals(output.format)){
					new TextLabelsLoader().saveDocsWithEmbeddedTypes(annLabels,save.saveAs);
				}else{
					throw new IllegalArgumentException("illegal output format "+
							output.format+" allowed values are "+
							StringUtil.toString(output.getAllowedOutputFormatValues()));
				}
			}catch(IOException e){
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
	}

	@Override
	public Object getMainResult(){
		return annLabels;
	}

	public static void main(String args[]){
		new ApplyAnnotator().callMain(args);
	}
}
