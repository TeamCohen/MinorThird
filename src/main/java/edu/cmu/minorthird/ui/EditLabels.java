package edu.cmu.minorthird.ui;

import java.io.IOException;

import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.gui.TextBaseEditor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.JointCommandLineProcessor;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Hand-label some documents.
 *
 * @author William Cohen
 */

public class EditLabels extends UIMain{

	// private data needed 
	private CommandLineUtil.EditParams edit=new CommandLineUtil.EditParams();

	public MutableTextLabels editorOutputLabels=null;

	public CommandLineUtil.EditParams getEditParameters(){
		return edit;
	}

	public void setEditParameters(CommandLineUtil.EditParams p){
		edit=p;
	}

	public String getEditLabelsHelp(){
		return "<A HREF=\"http://minorthird.sourceforge.net/tutorials/EditLabels%20Tutorial.htm\">EditLabels Tutorial</A></html>";
	}

	@Override
	public CommandLineProcessor getCLP(){
		return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,
				edit});
	}

	//
	// invoke the TextBaseLabeler 
	//

	@Override
	public void doMain(){
		if(edit.editFile==null)
			throw new IllegalArgumentException("need to specify -edit");

		TextBase textBase=base.labels.getTextBase();
		try{
			editorOutputLabels=new TextLabelsLoader().loadOps(textBase,edit.editFile);
		}catch(IOException ex){
			System.out.println("Can't load from "+edit.editFile+": "+ex);
			return;
		}
		if(edit.trueType!=null){
			editorOutputLabels.declareType(edit.trueType);
		}
		TextBaseEditor editor=TextBaseEditor.edit(editorOutputLabels,edit.editFile);
		if(edit.extractedType!=null)
			editor.getViewer().getGuessBox().setSelectedItem(edit.extractedType);
		if(edit.trueType!=null){
			editor.getViewer().getTruthBox().setSelectedItem(edit.trueType);
		}

		if(base.showResult){
			new ViewerFrame("Output of editing",new SmartVanillaViewer(
					editorOutputLabels));
		}

	}

	@Override
	public Object getMainResult(){
		return editorOutputLabels;
	}

	public static void main(String args[]){
		new EditLabels().callMain(args);
	}
}
