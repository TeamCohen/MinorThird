package edu.cmu.minorthird.ui;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Run a mixup program.
 *
 * @author William Cohen
 */

public class RunMixup extends UIMain{

	private static Logger log=Logger.getLogger(RunMixup.class);

	// private data needed 
	private CommandLineUtil.SaveParams save=new CommandLineUtil.SaveParams();

	private CommandLineUtil.MixupParams mixup=new CommandLineUtil.MixupParams();

	private CommandLineUtil.AnnotatorOutputParams output=
			new CommandLineUtil.AnnotatorOutputParams();

	public TextLabels annotatedLabels=null;

	// for gui
	public CommandLineUtil.MixupParams getMixupParameters(){
		return mixup;
	}

	public void setMixupParameters(CommandLineUtil.MixupParams p){
		mixup=p;
	}

	public CommandLineUtil.SaveParams getSaveParameters(){
		return save;
	}

	public void setSaveParameters(CommandLineUtil.SaveParams p){
		save=p;
	}

	public CommandLineUtil.AnnotatorOutputParams getAnnotatorOutputParams(){
		return output;
	}

	public void setAnnotatorOutputParams(CommandLineUtil.AnnotatorOutputParams p){
		output=p;
	}

	public String getRunMixupHelp(){
		return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/Mixup%20Tutorial.htm\">Mixup Tutorial</A></html>";
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,
				save,mixup,output});
	}

	//
	// run the mixup program
	// 

	@Override
	public void doMain(){
		if(mixup.fileName==null)
			throw new IllegalArgumentException("need to specify -mixup");

		// load the mixup program
		MixupProgram program=null;
		try{
			program=new MixupProgram(new File(mixup.fileName));
		}catch(Mixup.ParseException ex){
			System.out.println("can't parse file "+mixup.fileName+": "+ex);
		}catch(IOException ex){
			System.out.println("can't load file "+mixup.fileName+": "+ex);
		}

		// check that inputs are valid
		if(program==null){
			log.error("Cannot runMixup unless a valid mixup program is specified.");
			return;
		}

		// echo the program
		System.out.println("Will execute this program:\n"+program);

		MixupInterpreter interpreter=new MixupInterpreter(program);

		// run the mixup on a copy
		annotatedLabels=new NestedTextLabels(base.labels);
		interpreter.eval((NestedTextLabels)annotatedLabels);

		if(base.showResult)
			new ViewerFrame("Result of "+mixup.fileName,new SmartVanillaViewer(
					annotatedLabels));

		if(save.saveAs!=null){
			try{
				if("minorthird".equals(output.format)){
					new TextLabelsLoader().saveTypesAsOps(annotatedLabels,save.saveAs);
				}else if("strings".equals(output.format)){
					new TextLabelsLoader().saveTypesAsStrings(annotatedLabels,
							save.saveAs,true);
				}else if("xml".equals(output.format)){
					new TextLabelsLoader().saveDocsWithEmbeddedTypes(annotatedLabels,
							save.saveAs);
				}else{
					throw new IllegalArgumentException("illegal output format "+
							output.format+" allowed values are "+
							StringUtil.toString(output.getAllowedOutputFormatValues()));
				}

				System.out.println("saved to "+save.saveAs);

			}catch(IOException e){
				throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
			}
		}
	}

	@Override
	public Object getMainResult(){
		return annotatedLabels;
	}

	public static void main(String args[]){
		new RunMixup().callMain(args);
	}
}
