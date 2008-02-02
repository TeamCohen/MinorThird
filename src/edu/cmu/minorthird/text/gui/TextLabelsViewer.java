package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;

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
	static SimpleAttributeSet[] colorByStatus = new SimpleAttributeSet[SpanDifference.MAX_STATUS + 1];
	static {
		colorByStatus[SpanDifference.FALSE_POS] = HiliteColors.red;
		colorByStatus[SpanDifference.FALSE_NEG] = HiliteColors.blue;
		colorByStatus[SpanDifference.TRUE_POS] = HiliteColors.green;
		colorByStatus[SpanDifference.UNKNOWN_POS] = HiliteColors.yellow;
	}

	private TextLabels labels;
	private Span[] spans;
	private SpanViewer.TextViewer[] viewers=null;
	private HashMap viewerForId=new HashMap();
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
			SpanDifference sd = controls.getSpanDifference();
			if (sd!=null) {
				for (SpanDifference.Looper i=sd.differenceIterator(); i.hasNext(); ) {
					Span s = i.nextSpan();
					SpanViewer.TextViewer sv = (SpanViewer.TextViewer)viewerForId.get(s.getDocumentId());
					if (sv==null) throw new IllegalStateException("can't highlight span "+s);
					sv.highlight(s, colorByStatus[i.getStatus()]);
				}
			}
			jlist.repaint(10); // optional argument seems to be necessary
		}
	}

	public JComponent componentFor(Object o)
	{																
		final TextLabels labels = (TextLabels)o;
		int n = labels.getTextBase().size();
		spans = new Span[n];
		viewers = new SpanViewer.TextViewer[n];
		int j = 0;
		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
			spans[j] = i.nextSpan();
			viewers[j] = new SpanViewer.TextViewer(labels,spans[j]);
			viewers[j].setContent(spans[j]);
			viewers[j].setSuperView(this);
			viewerForId.put( spans[j].getDocumentId(), viewers[j] );
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
		monitorSelections(jlist);
		//for (int i=0; i<viewers.length; i++) System.out.println("viewers["+i+"].superView  "+viewers[i].getSuperView());
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		JPanel subpanel = new JPanel();
		final JTextField fileField = new JTextField(20);
		subpanel.add( new JLabel("[ "+n+" documents "+labels.getTypes().size()+" types ]") );
		subpanel.add( new JButton(new AbstractAction("Save as... ") {
				public void actionPerformed(ActionEvent ev) {
					try {
						new TextLabelsLoader().saveTypesAsOps(labels,new File(fileField.getText()));
					} catch (Exception ex) {
						System.out.println("Error saving "+ex);
					}
				}
			}));
		subpanel.add(fileField);
		panel.add(jlist, fillerGBC());
		GridBagConstraints gbc = fillerGBC();
		gbc.gridy = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(subpanel,gbc);
		return new JScrollPane(panel);
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
			System.out.println("usage: labelKey");
		}
	}
}
