package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;


/** View the contents of a span, using the util.gui.Viewer framework.
 *
 * @author William Cohen
 */

public class SpanViewer extends ParallelViewer
{
	private static final int DEFAULT_CONTEXT=10;

	private TextEnv env;
	private Span span;
	private TextViewer textViewer; 

	public SpanViewer(TextEnv env,Span span)
	{
		super();
		this.env = env;
		this.span = span;
		this.textViewer = new TextViewer(env,span);
		addSubView("Text",textViewer);
		addSubView("Tokens",new TokenViewer(env,span));
		setContent(span);
	}
	public void applyMarkup(MarkupPlan plan)
	{
		textViewer.applyMarkup(plan);
	}
	
	/** A text view of a span, plus controls */
	public static class ControlledTextViewer extends ControlledViewer
	{
		private Span span;
		public ControlledTextViewer(Span span)
		{
			this.span = span;
			TextControls controls = new TextControls();
			MyViewer viewer = new MyViewer();
			setComponents(viewer,controls);
			setContent(span);
		}
		private class MyViewer extends ComponentViewer implements Controllable 
		{
			public TextControls controls;
			public Span span;
			public JComponent componentFor(Object o) {
				this.span = (Span)o;
				int contextWidth = controls==null ? DEFAULT_CONTEXT : controls.contextSlider.getValue();
				SpanDocument doc = new SpanDocument(span,contextWidth);
				JTextPane textPane = new JTextPane(doc); 
				return new JScrollPane(textPane);
			}
			public void applyControls(ViewerControls controls) {	
				this.controls = (TextControls)controls;
				setContent(span,true);
				revalidate();
			}
		}
		private class TextControls extends ViewerControls	{
			public JSlider contextSlider;
			public void initialize(){
				contextSlider = new JSlider(0,100,DEFAULT_CONTEXT);
				add(new JLabel("Context:"));
				add(contextSlider);
				addApplyButton();
			}
		}
	}

	/** A text view of a span. */
	public static class TextViewer extends ComponentViewer
	{																
		private TextEnv env;
		private Span span;
		private SpanDocument doc;
		private JTextPane textPane;
		private int context = DEFAULT_CONTEXT;
		public TextViewer(Span span)	{	this(new EmptyEnv(),span,DEFAULT_CONTEXT); }
		public TextViewer(Span span, int context)	{	this(new EmptyEnv(),span,context); }
		public TextViewer(TextEnv env, Span span)	{	this(env,span,DEFAULT_CONTEXT);	}
		public TextViewer(TextEnv env, Span span, int context)
		{
			super();
			this.env = env;
			this.span = span;
			this.context = context;
		}
		public JComponent componentFor(Object o)
		{
			span = (Span)o;
			doc = new SpanDocument(span,context);
			textPane = new JTextPane(doc); 
			textPane.setEditable(false);
			return new JScrollPane(textPane);
		}
		public void applyMarkup(MarkupPlan plan)
		{
			doc.resetHighlights(); 
			for (Iterator i=env.getTypes().iterator(); i.hasNext(); ) {
				String type = (String)i.next();
				SimpleAttributeSet color = plan.getColor(type);
				if (color!=null) {
					for (Span.Looper j=env.instanceIterator(type,span.getDocumentId()); j.hasNext(); ) {
						Span typeSpan = j.nextSpan();
						doc.highlight(typeSpan,color);
					}
				}
			}
		}
	}

	// note: this is rather expensive to build
	/** A tokenized view of a span. */
	public static class TokenViewer extends ComponentViewer
	{																
		private TextEnv env;
		private Span span;
		public TokenViewer(TextEnv env, Span span)
		{
			super();
			this.env = env;
			this.span = span;
		}
		public JComponent componentFor(Object o)
		{
			Span s = (Span)o;
			Set propSet = env.getTokenProperties();
			String[] props = (String[])propSet.toArray(new String[propSet.size()]);
			Object[][] table = new Object[span.size()][props.length+1];
			for (int i=0; i<s.size(); i++) {
				table[i][0] = s.getToken(i).getValue();
				for (int j=0; j<props.length; j++) {
					table[i][j+1] = env.getProperty( s.getToken(i), props[j] );
					if (table[i][j+1]==null) table[i][j+1] = "-"; 
				}
			}
			String[] colNames = new String[props.length+1];
			colNames[0] = "token";
			for (int j=0; j<props.length; j++) colNames[j+1] = props[j];
			return new JScrollPane(new JTable(table,colNames));
		}
	}

	// fancy test case
	public static void main(String[] argv)
	{
		try {
			TextEnv env0 = FancyLoader.loadTextEnv("env/seminar-subset");
			final MonotonicTextEnv env = new NestedTextEnv(env0);
			MixupProgram p = 
				new MixupProgram(
					new String[] { 
						"defTokenProp entity:time =top: ... [@stime] ...",
						"defTokenProp entity:speaker =top: ... [@speaker] ...",
						"defTokenProp entity:loc =top: ... [@location] ...",
						"defTokenProp freeText:true =top: ... [@sentence] ...",
					});
			p.eval(env,env.getTextBase());
			
			Span s = env.getTextBase().documentSpanIterator().nextSpan();
			SpanViewer sv = new SpanViewer(env,s);
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
						((SpanViewer)viewer1).applyMarkup((MarkupPlan)argument);
					}
					protected boolean canHandle(int signal,Object argument,ArrayList senders) {
						return (signal==OBJECT_UPDATED && (argument instanceof MarkupPlan));
					}
				};
			v.setHorizontal();
			//v.receiveContent(s);
			//ViewerFrame f = new ViewerFrame("SpanViewer", v);
			Span subspan = s.subSpan(10,5);
			Viewer u = new ControlledTextViewer(subspan);
			ViewerFrame f = new ViewerFrame("SpanViewer",u);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
