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
 * Hand-label some documents.
 *
 * @author William Cohen
 */

public class EditLabels extends UIMain
{
  private static Logger log = Logger.getLogger(EditLabels.class);

	// private data needed 
	private CommandLineUtil.EditParams edit = new CommandLineUtil.EditParams();

	public MutableTextLabels editorOutputLabels = null;
	public CommandLineUtil.EditParams getEditParameters() { return edit; }
	public void setEditParameters(CommandLineUtil.EditParams p) { edit=p; }

	public CommandLineProcessor getCLP()
	{
		return new JointCommandLineProcessor(new CommandLineProcessor[]{new GUIParams(),base,edit});
	}

	//
	// invoke the TextBaseLabeler 
	//

	public void doMain()
	{
		if (edit.editFile==null) throw new IllegalArgumentException("need to specify -edit");

		TextBase textBase = base.labels.getTextBase();
		try {
			editorOutputLabels = new TextLabelsLoader().loadOps(textBase, edit.editFile);
		} catch (IOException ex) {
			System.out.println("Can't load from "+edit.editFile+": "+ex);
			return;
		}
		TextBaseEditor editor = TextBaseEditor.edit( editorOutputLabels, edit.editFile);

		if (edit.extractedType!=null) 
			editor.getViewer().getGuessBox().setSelectedItem(edit.extractedType);
		if (edit.trueType!=null)
			editor.getViewer().getTruthBox().setSelectedItem(edit.trueType);

		if (base.showResult) {
			new ViewerFrame("Output of editing", new SmartVanillaViewer(editorOutputLabels));
		}

	}

	public Object getMainResult() { return editorOutputLabels; }

	public static void main(String args[])
	{
		new EditLabels().callMain(args);
	}
}
