package edu.cmu.minorthird.text.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.text.SimpleAttributeSet;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanDifference;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Controllable;
import edu.cmu.minorthird.util.gui.ControlledViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerControls;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * View the contents of a bunch of spans, using the util.gui.Viewer framework.
 * 
 * <p>
 * Hopefully this will evolve into a cleaner version of the TextBaseViewer,
 * TextBaseEditor, etc suite. It replaces an earlier attempt, the
 * SpanLooperViewer.
 * 
 * @author William Cohen
 */

public class TextLabelsViewer extends ComponentViewer implements Controllable{

	static final long serialVersionUID=200803014L;
	
	static SimpleAttributeSet[] colorByStatus=
			new SimpleAttributeSet[SpanDifference.MAX_STATUS+1];
	static{
		colorByStatus[SpanDifference.FALSE_POS]=HiliteColors.red;
		colorByStatus[SpanDifference.FALSE_NEG]=HiliteColors.blue;
		colorByStatus[SpanDifference.TRUE_POS]=HiliteColors.green;
		colorByStatus[SpanDifference.UNKNOWN_POS]=HiliteColors.yellow;
	}

	private TextLabels labels;

	private Span[] spans;

	private SpanViewer.TextViewer[] viewers=null;

	private Map<String,SpanViewer.TextViewer> viewerForId=new HashMap<String,SpanViewer.TextViewer>();

	private JList jlist;

	public TextLabelsViewer(TextLabels labels){
		super();
		this.labels=labels;
		setContent(labels);
	}
	
	public TextLabels getLabels(){
		return labels;
	}

	@Override
	public void applyControls(ViewerControls viewerControls){
		MarkupControls controls=(MarkupControls)viewerControls;
		if(viewers!=null){
			for(int i=0;i<viewers.length;i++){
				viewers[i].applyControls(controls);
			}
			SpanDifference sd=controls.getSpanDifference();
			if(sd!=null){
				for(SpanDifference.Looper i=sd.differenceIterator();i.hasNext();){
					Span s=i.next();
					SpanViewer.TextViewer sv=
							viewerForId.get(s.getDocumentId());
					if(sv==null)
						throw new IllegalStateException("can't highlight span "+s);
					sv.highlight(s,colorByStatus[i.getStatus()]);
				}
			}
			jlist.repaint(10); // optional argument seems to be necessary
		}
	}

	@Override
	public JComponent componentFor(Object o){
		final TextLabels labels=(TextLabels)o;
		int n=labels.getTextBase().size();
		spans=new Span[n];
		viewers=new SpanViewer.TextViewer[n];
		int j=0;
		for(Iterator<Span> i=labels.getTextBase().documentSpanIterator();i
				.hasNext();){
			spans[j]=i.next();
			viewers[j]=new SpanViewer.TextViewer(labels,spans[j]);
			viewers[j].setContent(spans[j]);
			viewers[j].setSuperView(this);
			viewerForId.put(spans[j].getDocumentId(),viewers[j]);
			j++;
		}
		jlist=new JList(spans);
		jlist.setCellRenderer(new ListCellRenderer(){

			@Override
			public Component getListCellRendererComponent(JList el,Object v,
					int index,boolean sel,boolean focus){
				Color borderColor=sel?Color.blue:Color.black;
				viewers[index].setBorder(BorderFactory.createLineBorder(borderColor,2));
				return viewers[index];
			}
		});
		monitorSelections(jlist);
		// for (int i=0; i<viewers.length; i++)
		// System.out.println("viewers["+i+"].superView
		// "+viewers[i].getSuperView());
		JPanel panel=new JPanel();
		panel.setLayout(new GridBagLayout());
		JPanel subpanel=new JPanel();
		final JTextField fileField=new JTextField(20);
		subpanel.add(new JLabel("[ "+n+" documents "+labels.getTypes().size()+
				" types ]"));
		subpanel.add(new JButton(new AbstractAction("Save as... "){
			static final long serialVersionUID=20080314L;
			@Override
			public void actionPerformed(ActionEvent ev){
				try{
					new TextLabelsLoader().saveTypesAsOps(labels,new File(fileField
							.getText()));
				}catch(Exception ex){
					System.out.println("Error saving "+ex);
				}
			}
		}));
		subpanel.add(fileField);
		panel.add(jlist,fillerGBC());
		GridBagConstraints gbc=fillerGBC();
		gbc.gridy=1;
		gbc.weighty=0;
		gbc.fill=GridBagConstraints.HORIZONTAL;
		panel.add(subpanel,gbc);
		return new JScrollPane(panel);
	}

	// fancy test case
	public static void main(String[] argv){
		try{
			final TextLabels labels=FancyLoader.loadTextLabels(argv[0]);
			TextLabelsViewer sv;
			sv=new TextLabelsViewer(labels);
			MarkupControls mc=new MarkupControls(labels);
			Viewer v=new ControlledViewer(sv,mc);
			new ViewerFrame("TextLabelsViewer",v);
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("usage: labelKey");
		}
	}
}
