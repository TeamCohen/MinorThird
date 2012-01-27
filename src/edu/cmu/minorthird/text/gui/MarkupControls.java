package edu.cmu.minorthird.text.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.SimpleAttributeSet;

import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanDifference;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.util.gui.Controllable;
import edu.cmu.minorthird.util.gui.ControlledViewer;
import edu.cmu.minorthird.util.gui.VanillaViewer;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerControls;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * A scheme for marking spans up in a labeling.
 * 
 * @author William Cohen
 */

public class MarkupControls extends ViewerControls{

	static final long serialVersionUID=20080305L;

	private static Map<String,SimpleAttributeSet> colorMap=
			new TreeMap<String,SimpleAttributeSet>(); // map color name ->
	// SimpleAttributeSet
	static{
		colorMap.put("blue",HiliteColors.blue);
		colorMap.put("red",HiliteColors.red);
		colorMap.put("green",HiliteColors.green);
		colorMap.put("yellow",HiliteColors.yellow);
		colorMap.put("gray",HiliteColors.gray);
	}

	private static final int NUM_COLOR_BOXES=5;

	protected TextLabels labels; // TextLabels being marked up

	protected List<String> types=new ArrayList<String>(); // all types in the

	// textLabels

	private Map<String,String> typeColorCode=new HashMap<String,String>(); // map

	// types
	// ->
	// color
	// name
	// (eg
	// "red")

	private Map<String,Map<String,String>> propColorCode=
			new HashMap<String,Map<String,String>>(); // map prop -> (map from

	// value->color name)

	private JComboBox guessBox,truthBox; // used for spanDifferencesa

	private SpanDifference sd; // result of diffing two types

	private List<JComboBox> boxList=new ArrayList<JComboBox>(); // all combo

	// boxes, for
	// reset

	public MarkupControls(TextLabels labels){
		super();
		this.labels=labels;
		types=new ArrayList<String>(labels.getTypes());
		typeColorCode=new HashMap<String,String>();
		initialize(); // initialize again, now that types are defined
	}

	/**
	 * Lay out the controls
	 */
	@Override
	protected void initialize(){
		if(types==null)
			return; // will go back and initialize later

		setLayout(new GridBagLayout());

		// subpanel will contain all content, and
		// will be wrapped in a JScrollPane
		JPanel subpanel=new JPanel();
		subpanel.setLayout(new GridBagLayout());

		//
		// configure the 'compare x to y' bit
		//
		guessBox=new JComboBox();
		guessBox.addItem("-compare- ");
		truthBox=new JComboBox();
		truthBox.addItem("-to-");
		for(int i=0;i<types.size();i++){
			guessBox.addItem(types.get(i).toString());
			truthBox.addItem(types.get(i).toString());
		}
		ActionListener diffListener=new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e){
				if(guessBox.getSelectedIndex()==0||truthBox.getSelectedIndex()==0){
					sd=null;
				}else{
					String guess=guessBox.getSelectedItem().toString();
					String truth=truthBox.getSelectedItem().toString();
					sd=
							new SpanDifference(labels.instanceIterator(guess),labels
									.instanceIterator(truth),labels.closureIterator(truth));
				}
			}
		};
		guessBox.addActionListener(diffListener);
		truthBox.addActionListener(diffListener);
		JPanel comparePanel=new JPanel();
		comparePanel.setBorder(new TitledBorder("Compare types"));
		// comparePanel.setLayout(new BoxLayout(comparePanel,BoxLayout.Y_AXIS));
		comparePanel.add(guessBox);
		comparePanel.add(truthBox);
		boxList.add(guessBox);
		boxList.add(truthBox);
		subpanel.add(comparePanel,gbc(0));

		//
		// configure the highlight types area
		// 
		JPanel typePanel=new JPanel();
		// typePanel.setBorder(new TitledBorder("Highlight types"));
		typePanel.setLayout(new GridBagLayout());
		for(int i=0;i<NUM_COLOR_BOXES;i++){
			JPanel boxPanel=new JPanel();
			JComboBox colorBox=makeBox("color",colorMap.keySet());
			JComboBox typeBox=makeBox("type",types);
			boxPanel.add(colorBox);
			boxPanel.add(typeBox);
			boxList.add(colorBox);
			boxList.add(typeBox);
			ActionListener boxListener=new ColorTypeBoxListener(colorBox,typeBox);
			colorBox.addActionListener(boxListener);
			typeBox.addActionListener(boxListener);
			typePanel.add(boxPanel,gbc(i));
		}

		//
		// configure the highlight properties area
		// 
		JPanel propPanel=new JPanel();
		// propPanel.setBorder(new TitledBorder("Highlight properties"));
		propPanel.setLayout(new GridBagLayout());
		for(int i=0;i<NUM_COLOR_BOXES;i++){
			JPanel boxPanel=new JPanel();
			JComboBox colorBox=makeBox("color",colorMap.keySet());
			JComboBox propBox=makeBox("property",propValuePairs());
			boxPanel.add(colorBox);
			boxPanel.add(propBox);
			boxList.add(colorBox);
			boxList.add(propBox);
			ActionListener boxListener=new ColorPropBoxListener(colorBox,propBox);
			colorBox.addActionListener(boxListener);
			propBox.addActionListener(boxListener);
			propPanel.add(boxPanel,gbc(i));
		}

		//
		// configure a summary window
		//
		JPanel summaryPanel=new JPanel();
		summaryPanel.setLayout(new GridBagLayout());
		summaryPanel.add(new JLabel("Documents:"),gbc(0,0));
		summaryPanel.add(new JLabel(Integer.toString(labels.getTextBase().size())),
				gbc(0,1));
		summaryPanel.add(new JLabel("Token Properties:"),gbc(1,0));
		summaryPanel.add(new JLabel(Integer.toString(labels.getTokenProperties()
				.size())),gbc(1,1));
		summaryPanel.add(new JLabel("Span Properties:"),gbc(2,0));
		summaryPanel.add(new JLabel(Integer.toString(labels.getSpanProperties()
				.size())),gbc(2,1));
		summaryPanel.add(new JLabel("Span Types:"),gbc(3,0));
		summaryPanel.add(new JLabel(Integer.toString(labels.getTypes().size())),
				gbc(3,1));

		JTabbedPane tabbedPane=new JTabbedPane();
		tabbedPane.add("Summary",summaryPanel);
		tabbedPane.add("SpanTypes",typePanel);
		tabbedPane.add("SpanProps",propPanel);

		subpanel.add(tabbedPane,gbc(1));

		JPanel applyPanel=new JPanel();
		applyPanel.setBorder(new TitledBorder("Update display"));
		applyPanel.add(makeApplyButton());
		applyPanel.add(makeResetButton());
		subpanel.add(applyPanel,gbc(2));
		// finally add the subpanel with a scroll pane and a desire to
		// fill some space..
		GridBagConstraints gbc=gbc(0);
		gbc.fill=GridBagConstraints.BOTH;
		add(new JScrollPane(subpanel),gbc);
	}

	@Override
	public int preferredLocation(){
		return ViewerControls.RIGHT;
	}

	@Override
	public boolean prefersToBeResized(){
		return true;
	}

	private Set<String> propValuePairs(){
		SortedSet<String> accum=new TreeSet<String>();
		Set<String> props=labels.getSpanProperties();
		for(Iterator<String> i=props.iterator();i.hasNext();){
			String prop=i.next();
			for(Iterator<Span> j=labels.getSpansWithProperty(prop);j.hasNext();){
				Span s=j.next();
				String val=labels.getProperty(s,prop);
				accum.add(packPropValue(prop,val));
			}
		}
		return accum;
	}

	private String packPropValue(String p,String v){
		return p+"="+v;
	}

	private String[] unpackPropValue(String pv){
		return pv.split("=");
	}

	private JButton makeResetButton(){
		return new JButton(new AbstractAction("-reset controls-"){
			static final long serialVersionUID=20080306L;
			@Override
			public void actionPerformed(ActionEvent e){
				for(int i=0;i<boxList.size();i++){
					(boxList.get(i)).setSelectedIndex(0);
				}
			}
		});
	}

	// a drop-down box of something's
	private JComboBox makeBox(String tag,Collection<?> c){
		final JComboBox box=new JComboBox();
		box.addItem("-select "+tag+"-");
		for(Iterator<?> j=c.iterator();j.hasNext();){
			box.addItem(j.next());
		}
		return box;
	}

	private class ColorTypeBoxListener implements ActionListener{

		private JComboBox colorBox,typeBox;

		private String type=null;

		public ColorTypeBoxListener(JComboBox colorBox,JComboBox typeBox){
			this.colorBox=colorBox;
			this.typeBox=typeBox;
		}

		@Override
		public void actionPerformed(ActionEvent ev){
			if(typeBox.getSelectedIndex()>0){
				type=(String)typeBox.getSelectedItem();
				if(colorBox.getSelectedIndex()!=0){
					typeColorCode.put(type,(String)colorBox.getSelectedItem());
				}else{
					typeColorCode.remove(type);
				}
			}else{
				if(type!=null)
					typeColorCode.remove(type);
				type=null;
			}
		}
	}

	private class ColorPropBoxListener implements ActionListener{

		private JComboBox colorBox,propValBox;

		private String[] propVal=null;

		public ColorPropBoxListener(JComboBox colorBox,JComboBox propValBox){
			this.colorBox=colorBox;
			this.propValBox=propValBox;
		}

		@Override
		public void actionPerformed(ActionEvent ev){
			if(propValBox.getSelectedIndex()>0){
				propVal=unpackPropValue((String)propValBox.getSelectedItem());
				if(colorBox.getSelectedIndex()!=0){
					putPropCode(propVal[0],propVal[1],(String)colorBox.getSelectedItem());
				}else{
					removePropCode(propVal[0],propVal[1]);
				}
			}else{
				if(propVal!=null)
					removePropCode(propVal[0],propVal[1]);
				propVal=null;
			}
		}
	}

	private void putPropCode(String p,String v,String color){
		Map<String,String> m=propColorCode.get(p);
		if(m==null)
			propColorCode.put(p,(m=new HashMap<String,String>()));
		m.put(v,color);
	}

	private void removePropCode(String p,String v){
		Map<String,String> m=propColorCode.get(p);
		if(m==null)
			propColorCode.put(p,(m=new HashMap<String,String>()));
		m.remove(v);
	}

	// build a drop-down box to select a color
//	private JComboBox colorBox(final int i){
//		final String type=(String)types.get(i);
//		final String color=(String)typeColorCode.get(type);
//		final JComboBox box=new JComboBox();
//		for(Iterator<String> j=colorMap.keySet().iterator();j.hasNext();){
//			box.addItem(j.next());
//		}
//		if(color!=null)
//			box.setSelectedItem(color);
//		else
//			box.setSelectedIndex(0);
//		box.addActionListener(new ActionListener(){
//
//			public void actionPerformed(ActionEvent e){
//				if(box.getSelectedIndex()!=0)
//					typeColorCode.put(type,(String)box.getSelectedItem());
//				else
//					typeColorCode.remove(type);
//			}
//		});
//		return box;
//	}

	// build GridBagConstraint with some default values
	private GridBagConstraints gbc(int i){
		GridBagConstraints gbc=new GridBagConstraints();
		gbc.anchor=GridBagConstraints.WEST;
		gbc.weightx=gbc.weighty=1.0;
		gbc.gridx=0;
		gbc.gridy=i;
		return gbc;
	}

	// build GridBagConstraint with some default values
	private GridBagConstraints gbc(int row,int col){
		GridBagConstraints gbc=new GridBagConstraints();
		gbc.ipadx=20;
		gbc.anchor=col==1?GridBagConstraints.WEST:GridBagConstraints.EAST;
		gbc.weightx=gbc.weighty=1.0;
		gbc.gridx=col;
		gbc.gridy=row;
		return gbc;
	}

	//
	// what's exported to the viewer...
	//

	/**
	 * Tell the ControlledViewer what color is associated with a type.
	 */
	public SimpleAttributeSet getColor(String type){
		String s=typeColorCode.get(type);
		if(s==null)
			return null;
		else
			return colorMap.get(s);
	}

	/**
	 * Tell the ControlledViewer what color is associated with a property/value
	 * pair
	 */
	public SimpleAttributeSet getColor(String prop,String value){
		Map<String,String> m=propColorCode.get(prop);
		if(m==null)
			return null;
		String s=m.get(value);
		if(s==null)
			return null;
		else
			return colorMap.get(s);
	}

	public Set<String> getColoredProperties(){
		return propColorCode.keySet();
	}

	public Set<String> getColoredValues(String prop){
		Map<String,String> m=propColorCode.get(prop);
		if(m==null)
			return Collections.<String>emptySet();
		return m.keySet();
	}

	/**
	 * Export a span difference to the controlled Span Viewer.
	 */
	public SpanDifference getSpanDifference(){
		return sd;
	}

	// for an interactive test, below
	private static class TestViewer extends VanillaViewer implements Controllable{

		static final long serialVersionUID=20080306L;
		
		public TestViewer(){
			super("yow");
		}

		@Override
		public void applyControls(ViewerControls controls){
			System.out.println("applied "+controls);
		}
	}

	public static void main(String[] argv){
		try{
			TextLabels labels=
					edu.cmu.minorthird.text.learn.SampleExtractionProblem.trainLabels();
			Viewer v=
					new ControlledViewer(new TestViewer(),new MarkupControls(labels));
			new ViewerFrame("MarkupControls",v);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
