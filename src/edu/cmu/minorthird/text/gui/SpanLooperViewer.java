package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextEnv;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.SplitViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


/** View the contents of a bunch of spans, using the util.gui.Viewer framework.
 *
 * Hopefully this will evolve into a cleaner version of the TextBaseViewer,
 * TextBaseEditor, etc suite.
 *
 * @author William Cohen
 */

public class SpanLooperViewer extends ComponentViewer
{
	private TextEnv env;
	private Span[] spans;
	private SpanViewer.TextViewer[] viewers=null;
	private JList jlist;

	public SpanLooperViewer(TextEnv env,Span.Looper i)
	{
		super();
		this.env = env;
		setContent(i);
	}
	public void applyMarkup(MarkupPlan plan)
	{
		if (viewers!=null) {
			for (int i=0; i<viewers.length; i++) {
				viewers[i].applyMarkup(plan);
			}
			jlist.repaint(10); // optional argument seems to be necessary
		}
	}
	public JComponent componentFor(Object o)
	{																
		Span.Looper i = (Span.Looper)o;
		ArrayList tmp = new ArrayList();
		while (i.hasNext()) {
			tmp.add(i.next());
		}
		spans = new Span[tmp.size()];
		viewers = new SpanViewer.TextViewer[tmp.size()];
		for (int j=0; j<tmp.size(); j++) {
			spans[j] = (Span)tmp.get(j);
			viewers[j] = new SpanViewer.TextViewer(env,(Span)tmp.get(j));
			viewers[j].setContent(spans[j]);
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
			final TextEnv env = FancyLoader.loadTextEnv(argv[0]);
			SpanLooperViewer sv;
			if (argv.length==1) {
				sv = new SpanLooperViewer(env,env.getTextBase().documentSpanIterator());
			} else {
				sv = new SpanLooperViewer(env,env.instanceIterator(argv[1]));				
			}
			MarkupPlan mp = new MarkupPlan(env);
			SplitViewer v = new SplitViewer(sv,mp.toGUI()) {
					public void receiveContent(Object content) {
						viewer1.setContent((Span)content);
						viewer2.setContent(new MarkupPlan(env));
					}
					public boolean canReceive(Object content)	{
						return (content instanceof Span);
					}
					protected void handle(int signal,Object argument,ArrayList senders) {
						((SpanLooperViewer)viewer1).applyMarkup((MarkupPlan)argument);
					}
					protected boolean canHandle(int signal,Object argument,ArrayList senders) {
						return (signal==OBJECT_UPDATED && (argument instanceof MarkupPlan));
					}
				};
			v.setHorizontal();
			ViewerFrame f = new ViewerFrame("SpanLooperViewer", v);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: envKey [spanType]");
		}
	}
}
