package edu.cmu.minorthird.ui;

import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

/** Interactively view TextLabels.
 */

public class ViewLabels 
{
	protected CommandLineUtil.BaseParams base = new CommandLineUtil.BaseParams();

	public static void main(String[] args)
	{
		ViewLabels m = new ViewLabels();
		try {
			m.base.processArguments(args);
			if (m.base.labels==null) throw new IllegalArgumentException("-labels must be specified");
			new ViewerFrame("ViewLabels"+StringUtil.toString(args),new SmartVanillaViewer(m.base.labels));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

