package edu.cmu.minorthird.text.gui;

import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;

import edu.cmu.minorthird.text.EmptyLabels;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Controllable;
import edu.cmu.minorthird.util.gui.ControlledViewer;
import edu.cmu.minorthird.util.gui.ParallelViewer;
import edu.cmu.minorthird.util.gui.ViewerControls;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * View the contents of a span, using the util.gui.Viewer framework.
 * 
 * @author William Cohen
 */

public class SpanViewer extends ParallelViewer implements Controllable{

	static final long serialVersionUID=200803014L;

	private static final int DEFAULT_CONTEXT=10;

	private TextLabels labels;

	private Span span;

	private TextViewer textViewer;

	public SpanViewer(TextLabels labels,Span span){
		super();
		this.labels=labels;
		this.span=span;
		this.textViewer=new TextViewer(labels,span);
		addSubView("Text",textViewer);
		addSubView("Tokens",new TokenViewer(labels,span));
		addSubView("Ids",new IdViewer(labels,span));
		setContent(span);
	}
	
	public TextLabels getLabels(){
		return labels;
	}
	
	public Span getSpan(){
		return span;
	}

	@Override
	public void applyControls(ViewerControls controls){
		textViewer.applyControls(controls);
	}

	@Override
	public boolean canReceive(Object obj){
		return obj instanceof Span;
	}

	/** A text view of a span, plus controls */
	public static class ControlledTextViewer extends ControlledViewer{

		static final long serialVersionUID=200803014L;		
		
		private Span span;

		public ControlledTextViewer(Span span){
			this.span=span;
			TextControls controls=new TextControls();
			MyViewer viewer=new MyViewer();
			setComponents(viewer,controls);
			setContent(span);
		}

		private class MyViewer extends ComponentViewer implements Controllable{

			static final long serialVersionUID=200803014L;
			
			public TextControls controls;

			public Span span;

			@Override
			public JComponent componentFor(Object o){
				this.span=(Span)o;
				int contextWidth=
						controls==null?DEFAULT_CONTEXT:controls.contextSlider.getValue();
				SpanDocument doc=new SpanDocument(span,contextWidth);
				JTextPane textPane=new JTextPane(doc);
				return new JScrollPane(textPane);
			}

			@Override
			public void applyControls(ViewerControls controls){
				this.controls=(TextControls)controls;
				setContent(span,true);
				revalidate();
			}
		}

		private class TextControls extends ViewerControls{

			static final long serialVersionUID=200803014L;
			
			public JSlider contextSlider;

			@Override
			public void initialize(){
				contextSlider=new JSlider(0,100,DEFAULT_CONTEXT);
				add(new JLabel("Context:"));
				add(contextSlider);
				addApplyButton();
			}
		}

		
		public Span getSpan(){
			return span;
		}
	}

	/** A text view of a span. */
	public static class TextViewer extends ComponentViewer implements
			Controllable{

		static final long serialVersionUID=200803014L;
		
		private TextLabels labels;

		private Span span;

		private SpanDocument doc;

		private JTextPane textPane;

		private int context=DEFAULT_CONTEXT;

		public TextViewer(Span span){
			this(new EmptyLabels(),span,DEFAULT_CONTEXT);
		}

		public TextViewer(Span span,int context){
			this(new EmptyLabels(),span,context);
		}

		public TextViewer(TextLabels labels,Span span){
			this(labels,span,DEFAULT_CONTEXT);
		}

		public TextViewer(TextLabels labels,Span span,int context){
			super();
			this.labels=labels;
			this.span=span;
			this.context=context;
		}

		public void highlight(Span span,SimpleAttributeSet color){
			if(color!=null)
				doc.highlight(span,color);
		}

		@Override
		public JComponent componentFor(Object o){
			span=(Span)o;
			doc=new SpanDocument(span,context);
			textPane=new JTextPane(doc);
			textPane.setEditable(false);
			return new JScrollPane(textPane);
		}

		@Override
		public void applyControls(ViewerControls viewerControls){
			MarkupControls controls=(MarkupControls)viewerControls;
			doc.resetHighlights();
			for(Iterator<String> i=labels.getTypes().iterator();i.hasNext();){
				String type=i.next();
				SimpleAttributeSet color=controls.getColor(type);
				if(color!=null){
					for(Iterator<Span> j=
							labels.instanceIterator(type,span.getDocumentId());j.hasNext();){
						Span typeSpan=j.next();
						doc.highlight(typeSpan,color);
					}
				}
			}
			for(Iterator<String> i=controls.getColoredProperties().iterator();i.hasNext();){
				String prop=i.next();
				for(Iterator<Span> j=
						labels.getSpansWithProperty(prop,span.getDocumentId());j.hasNext();){
					Span span=j.next();
					String val=labels.getProperty(span,prop);
					SimpleAttributeSet color=controls.getColor(prop,val);
					if(color!=null)
						doc.highlight(span,color);
				}
			}
		}
	}

	// note: this is rather expensive to build
	/** A tokenized view of a span. */
	public static class TokenViewer extends ComponentViewer{

		static final long serialVersionUID=200803014L;
		
		private TextLabels labels;

		private Span span;

		public TokenViewer(TextLabels labels,Span span){
			super();
			this.labels=labels;
			this.span=span;
		}
		
		public Span getSpan(){
			return span;
		}

		@Override
		public JComponent componentFor(Object o){
			// bug in here somewhere...
			Span s=(Span)o;
			Set<String> propSet=labels.getTokenProperties();
			String[] props=propSet.toArray(new String[propSet.size()]);
			Object[][] table=new Object[s.size()][props.length+1];
			for(int i=0;i<s.size();i++){
				table[i][0]=s.getToken(i).getValue(); // bug here
				for(int j=0;j<props.length;j++){
					table[i][j+1]=labels.getProperty(s.getToken(i),props[j]);
					if(table[i][j+1]==null)
						table[i][j+1]="-";
				}
			}
			String[] colNames=new String[props.length+1];
			colNames[0]="token";
			for(int j=0;j<props.length;j++)
				colNames[j+1]=props[j];
			return new JScrollPane(new JTable(table,colNames));
		}
	}

	/** View of the span's documentId and documentGroupId */
	public static class IdViewer extends ComponentViewer{

		static final long serialVersionUID=200803014L;
		
		private TextLabels labels;

		private Span span;

		public IdViewer(TextLabels labels,Span span){
			super();
			this.labels=labels;
			this.span=span;
		}
		
		public TextLabels getLabels(){
			return labels;
		}
		
		public Span getSpan(){
			return span;
		}

		@Override
		public JComponent componentFor(Object o){
			Span s=(Span)o;
			Object[][] table=new Object[1][2];
			table[0][0]=s.getDocumentId();
			table[0][1]=s.getDocumentGroupId();
			String[] colNames=new String[]{"documentId","documentGroupId"};
			return new JScrollPane(new JTable(table,colNames));
		}
	}

	// fancy test case
	public static void main(String[] argv){
		try{
			TextLabels labels0=
					FancyLoader.loadTextLabels("demos/sampleData/seminar-subset");
			final MonotonicTextLabels labels=new NestedTextLabels(labels0);
			MixupProgram p=
					new MixupProgram(new String[]{
							"defTokenProp entity:time =top: ... [@stime] ...",
							"defTokenProp entity:speaker =top: ... [@speaker] ...",
							"defTokenProp entity:loc =top: ... [@location] ...",
							"defTokenProp freeText:true =top: ... [@sentence] ...",});
			MixupInterpreter interp=new MixupInterpreter(p);
			interp.eval(labels);
			Span s=labels.getTextBase().documentSpanIterator().next();
			SpanViewer sv=new SpanViewer(labels,s);
			MarkupControls mp=new MarkupControls(labels);
			ControlledViewer v=new ControlledViewer(sv,mp);
			v.setContent(s);
			// Span subspan = s.subSpan(10,5);
			// Viewer u = new ControlledTextViewer(subspan);
			new ViewerFrame("SpanViewer",v);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
