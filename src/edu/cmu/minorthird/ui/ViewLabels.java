package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/** 
 * Interactively view TextLabels.
 */

public class ViewLabels{

	protected CommandLineUtil.BaseParams base=new CommandLineUtil.BaseParams();

	protected CommandLineUtil.ViewLabelsParams viewLabels=new CommandLineUtil.ViewLabelsParams();

	public static void main(String[] args){
		ViewLabels m=new ViewLabels();
		try{
			m.base.processArguments(args);
			//m.viewLabels.processArguments(args);
			if(m.base.labels!=null){
				new ViewerFrame("ViewLabels"+StringUtil.toString(args),new SmartVanillaViewer(m.base.labels));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
