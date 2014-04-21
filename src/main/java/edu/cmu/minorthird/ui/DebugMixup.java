package edu.cmu.minorthird.ui;

import java.io.File;

import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.gui.MixupDebugger;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Run a mixup program.
 *
 * @author William Cohen
 */

public class DebugMixup extends UIMain{

	// private data needed 
	private CommandLineUtil.EditParams edit=new CommandLineUtil.EditParams();

	private CommandLineUtil.MixupParams mixup=new CommandLineUtil.MixupParams();

	public MonotonicTextLabels debuggerOutputLabels=null;

	public CommandLineUtil.MixupParams getMixupParameters(){
		return mixup;
	}

	public void setMixupParameters(CommandLineUtil.MixupParams p){
		mixup=p;
	}

	public CommandLineUtil.EditParams getEditParameters(){
		return edit;
	}

	public void setEditParameters(CommandLineUtil.EditParams p){
		edit=p;
	}

	public String getDebugMixupHelp(){
		return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/Mixup%20Tutorial.htm\">Mixup Tutorial</A></html>";
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,
				edit,mixup});
	}

	//
	// run the mixup program
	// 

	@Override
	public void doMain(){
		if(mixup.fileName==null)
			throw new IllegalArgumentException("need to specify -mixup");
		if(edit.editFile==null)
			throw new IllegalArgumentException("need to specify -edit");

		debuggerOutputLabels=new NestedTextLabels(base.labels);
		MixupDebugger debugger=
				MixupDebugger.debug(base.labels.getTextBase(),debuggerOutputLabels,
						edit.editFile,new File(mixup.fileName));

		if(edit.extractedType!=null)
			debugger.getEditor().getViewer().getGuessBox().setSelectedItem(
					edit.extractedType);
		if(edit.trueType!=null)
			debugger.getEditor().getViewer().getTruthBox().setSelectedItem(
					edit.trueType);

		if(base.showResult){
			new ViewerFrame("Output of "+mixup.fileName,new SmartVanillaViewer(
					debuggerOutputLabels));
		}
	}

	@Override
	public Object getMainResult(){
		return debuggerOutputLabels;
	}

	public static void main(String args[]){
		new DebugMixup().callMain(args);
	}
}
