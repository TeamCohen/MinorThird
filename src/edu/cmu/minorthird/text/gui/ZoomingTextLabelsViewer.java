package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.util.gui.ControlledViewer;
import edu.cmu.minorthird.util.gui.VanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;
import edu.cmu.minorthird.util.gui.ZoomedViewer;

/** 
 * View the contents of a bunch of spans, using the util.gui.Viewer framework.
 *
 * <p> Hopefully this will evolve into a cleaner version of the
 * TextBaseViewer, TextBaseEditor, etc suite.  It replaces an earlier
 * attempt, the SpanLooperViewer.
 *
 * @author William Cohen
 */

public class ZoomingTextLabelsViewer extends ZoomedViewer{

	static final long serialVersionUID=20080202L;

	public ZoomingTextLabelsViewer(TextLabels labels){
		Viewer zoomedOut=new ControlledViewer(new TextLabelsViewer(labels),new MarkupControls(labels));
		Viewer zoomedIn=new VanillaViewer("[Empty TextBase]");
		if(labels.getTextBase().size()>0){
			zoomedIn=new SpanViewer(labels,labels.getTextBase().documentSpanIterator().next());
		}
		this.setSubViews(zoomedOut,zoomedIn);
	}

	// test case
	public static void main(String[] argv){
		try{
			final TextLabels labels=FancyLoader.loadTextLabels(argv[0]);
			new ViewerFrame(argv[0],new ZoomingTextLabelsViewer(labels));
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Usage: labelKey");
		}
	}

}
