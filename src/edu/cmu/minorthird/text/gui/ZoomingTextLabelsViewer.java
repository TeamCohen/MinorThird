package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/** View the contents of a bunch of spans, using the util.gui.Viewer framework.
 *
 * <p> Hopefully this will evolve into a cleaner version of the
 * TextBaseViewer, TextBaseEditor, etc suite.  It replaces an earlier
 * attempt, the SpanLooperViewer.
 *
 * @author William Cohen
 */

public class ZoomingTextLabelsViewer extends ZoomedViewer
{
	public ZoomingTextLabelsViewer(TextLabels labels)
	{
		Viewer zoomedOut = 
			new ControlledViewer(new TextLabelsViewer(labels), new MarkupControls(labels));
		Viewer zoomedIn = new VanillaViewer("[Empty TextBase]"); 
		if (labels.getTextBase().size()>0)  
			zoomedIn = new SpanViewer(labels,labels.getTextBase().documentSpanIterator().nextSpan());
		this.setSubViews(zoomedOut,zoomedIn);
	}

	// test case
	public static void main(String[] argv)
	{
		try {
			final TextLabels labels = FancyLoader.loadTextLabels(argv[0]);
			ViewerFrame f = new ViewerFrame(argv[0], new ZoomingTextLabelsViewer(labels));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: labelKey");
		}
	}

}
