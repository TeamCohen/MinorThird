package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


/** View the contents of a bunch of spans, using the util.gui.Viewer framework.
 *
 * <p> Hopefully this will evolve into a cleaner version of the
 * TextBaseViewer, TextBaseEditor, etc suite.  It replaces an earlier
 * attempt, the SpanLooperViewer.
 *
 * @author William Cohen
 */

public class TextLabelsViewer extends ComponentViewer implements Controllable
{
	private TextLabels labels;
	private Span[] spans;
	private SpanViewer.TextViewer[] viewers=null;
	private JList jlist;

	public TextLabelsViewer(TextLabels labels)
	{
		super();
		this.labels = labels;
		setContent(labels);
	}
	public void applyControls(ViewerControls viewerControls)
	{
		MarkupControls controls = (MarkupControls)viewerControls;
		if (viewers!=null) {
			for (int i=0; i<viewers.length; i++) {
				viewers[i].applyControls(controls);
			}
			jlist.repaint(10); // optional argument seems to be necessary
		}
	}
	public JComponent componentFor(Object o)
	{																
		TextLabels labels = (TextLabels)o;
		int n = labels.getTextBase().size();
		spans = new Span[n];
		viewers = new SpanViewer.TextViewer[n];
		int j = 0;
		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
			spans[j] = i.nextSpan();
			viewers[j] = new SpanViewer.TextViewer(labels,spans[j]);
			viewers[j].setContent(spans[j]);
			j++;
		}
		jlist = new JList(spans);
		jlist.setCellRenderer(new ListCellRenderer() {
        public Component getListCellRendererComponent(JList el,Object v,int index,boolean sel,boolean focus){
					Color borderColor = sel ? Color.blue : Color.black;
					viewers[index].setBorder(BorderFactory.createLineBorder(borderColor, 2));
					return viewers[index];
				}
			});
		return new JScrollPane(jlist);
	}

	// fancy test case
	public static void main(String[] argv)
	{
		try {
			final TextLabels labels = FancyLoader.loadTextLabels(argv[0]);
			TextLabelsViewer sv;
			sv = new TextLabelsViewer(labels);
			MarkupControls mc = new MarkupControls(labels);
			Viewer v = new ControlledViewer(sv,mc);
			ViewerFrame f = new ViewerFrame("TextLabelsViewer", v);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: labelKey [spanType]");
		}
	}
}
