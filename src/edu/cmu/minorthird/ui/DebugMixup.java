package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;

/**
 * Run a mixup program.
 *
 * @author William Cohen
 */

public class DebugMixup extends UIMain
{
  private static Logger log = Logger.getLogger(DebugMixup.class);

	// private data needed 
	private CommandLineUtil.EditParams edit = new CommandLineUtil.EditParams();
	private CommandLineUtil.MixupParams mixup = new CommandLineUtil.MixupParams();
	public MonotonicTextLabels debuggerOutputLabels = null;
	public CommandLineUtil.MixupParams getMixupParameters() { return mixup; }
	public void setMixupParameters(CommandLineUtil.MixupParams p) { mixup=p; }
	public CommandLineUtil.EditParams getEditParameters() { return edit; }
	public void setEditParameters(CommandLineUtil.EditParams p) { edit=p; }

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,edit,mixup});
	}

	//
	// run the mixup program
	// 

	public void doMain()
	{
		if (mixup.fileName==null) throw new IllegalArgumentException("need to specify -mixup");
		if (edit.editFile==null) throw new IllegalArgumentException("need to specify -edit");

		debuggerOutputLabels = new NestedTextLabels(base.labels);
		MixupDebugger debugger = 
			MixupDebugger.debug(base.labels.getTextBase(),
													debuggerOutputLabels,
													edit.editFile,
													new File(mixup.fileName));

		if (edit.extractedType!=null) 
			debugger.getEditor().getViewer().getGuessBox().setSelectedItem(edit.extractedType);
		if (edit.trueType!=null)
			debugger.getEditor().getViewer().getTruthBox().setSelectedItem(edit.trueType);

		if (base.showResult) {
			new ViewerFrame("Output of "+mixup.fileName, new SmartVanillaViewer(debuggerOutputLabels));
		}
	}

	public Object getMainResult() { return debuggerOutputLabels; }

	public static void main(String args[])
	{
		new DebugMixup().callMain(args);
	}
}
