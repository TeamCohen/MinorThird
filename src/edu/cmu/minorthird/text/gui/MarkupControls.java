package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


/** A scheme for marking spans up in a labeling.
 *
 * @author William Cohen
 */

public class MarkupControls extends ViewerControls
{
	private static Map colorMap = new TreeMap();  // map color name -> SimpleAttributeSet
	static {
		colorMap.put("blue",HiliteColors.blue );
		colorMap.put("red",HiliteColors.red );
		colorMap.put("green",HiliteColors.green );
		colorMap.put("yellow",HiliteColors.yellow );
		colorMap.put("gray",HiliteColors.gray );
	}
	private static final int NUM_COLOR_BOXES=5;

	private TextLabels labels; // TextLabels being marked up
	private ArrayList types = new ArrayList(); // all types in the textLabels
	private Map typeColorCode = new HashMap(); // map types -> color name (eg "red")
	private Map propColorCode = new HashMap(); // map prop -> (map from value->color name)
	private JComboBox guessBox, truthBox; // used for spanDifferencesa
	private SpanDifference sd; // result of diffing two types
	private ArrayList boxList = new ArrayList(); // all combo boxes, for reset

	public MarkupControls(TextLabels labels)
	{
		super();
		this.labels = labels;
		types = new ArrayList(labels.getTypes());
		typeColorCode = new HashMap();
		initialize(); // initialize again, now that types are defined
	}

	/** Lay out the controls
	 */
	protected void initialize()
	{
		if (types==null) return; // will go back and initialize later

		setLayout(new GridBagLayout());

		// subpanel will contain all content, and
		// will be wrapped in a JScrollPane
		JPanel subpanel = new JPanel();
		subpanel.setLayout(new GridBagLayout());

		//
		// configure the 'compare x to y' bit
		//
		guessBox = new JComboBox();
		guessBox.addItem( "-compare- ");
		truthBox = new JComboBox();
		truthBox.addItem( "-to-" );
		for (int i=0; i<types.size(); i++) {
			guessBox.addItem( types.get(i).toString() );
			truthBox.addItem( types.get(i).toString() );
		}
		ActionListener diffListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (guessBox.getSelectedIndex()==0 || truthBox.getSelectedIndex()==0) {
						sd = null;
					} else {
						String guess = guessBox.getSelectedItem().toString();
						String truth = truthBox.getSelectedItem().toString();
						sd = new SpanDifference( 
							labels.instanceIterator(guess), 
							labels.instanceIterator(truth), 
							labels.closureIterator(truth));
					}
				}
			};
		guessBox.addActionListener(diffListener);
		truthBox.addActionListener(diffListener);
		JPanel comparePanel = new JPanel();
		comparePanel.setBorder(new TitledBorder("Compare types"));
		//comparePanel.setLayout(new BoxLayout(comparePanel,BoxLayout.Y_AXIS));
		comparePanel.add(guessBox);
		comparePanel.add(truthBox);
		boxList.add(guessBox);
		boxList.add(truthBox);
		subpanel.add(comparePanel,gbc(0));

		//
		// configure the highlight types area
		// 
		JPanel typePanel = new JPanel();
		//typePanel.setBorder(new TitledBorder("Highlight types"));
		typePanel.setLayout(new GridBagLayout());
		for (int i=0; i<NUM_COLOR_BOXES; i++) {
			JPanel boxPanel = new JPanel();
			JComboBox colorBox = makeBox("color",colorMap.keySet());
			JComboBox typeBox = makeBox("type",types);
			boxPanel.add( colorBox );
			boxPanel.add( typeBox );
			boxList.add( colorBox );
			boxList.add( typeBox );
			ActionListener boxListener = new ColorTypeBoxListener(colorBox,typeBox);
			colorBox.addActionListener( boxListener );
			typeBox.addActionListener( boxListener );
			typePanel.add(boxPanel,gbc(i));
		}

		//
		// configure the highlight properties area
		// 
		JPanel propPanel = new JPanel();
		//propPanel.setBorder(new TitledBorder("Highlight properties"));
		propPanel.setLayout(new GridBagLayout());
		for (int i=0; i<NUM_COLOR_BOXES; i++) {
			JPanel boxPanel = new JPanel();
			JComboBox colorBox = makeBox("color",colorMap.keySet());
			JComboBox propBox = makeBox("property",propValuePairs());
			boxPanel.add( colorBox );
			boxPanel.add( propBox );
			boxList.add( colorBox );
			boxList.add( propBox );
			ActionListener boxListener = new ColorPropBoxListener(colorBox,propBox);
			colorBox.addActionListener( boxListener );
			propBox.addActionListener( boxListener );
			propPanel.add(boxPanel,gbc(i));
		}

		//
		// configure a summary window
		//
		JPanel summaryPanel = new JPanel();
		summaryPanel.setLayout(new GridBagLayout());
		summaryPanel.add(new JLabel("Documents:"), gbc(0,0));
		summaryPanel.add(new JLabel(Integer.toString(labels.getTextBase().size())), gbc(0,1));
		summaryPanel.add(new JLabel("Token Properties:"), gbc(1,0));
		summaryPanel.add(new JLabel(Integer.toString(labels.getTokenProperties().size())), gbc(1,1));
		summaryPanel.add(new JLabel("Span Properties:"), gbc(2,0));
		summaryPanel.add(new JLabel(Integer.toString(labels.getSpanProperties().size())), gbc(2,1));
		summaryPanel.add(new JLabel("Span Types:"), gbc(3,0));
		summaryPanel.add(new JLabel(Integer.toString(labels.getTypes().size())), gbc(3,1));

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Summary",summaryPanel);
		tabbedPane.add("SpanTypes",typePanel);
		tabbedPane.add("SpanProps",propPanel);

		subpanel.add(tabbedPane, gbc(1));

		JPanel applyPanel = new JPanel();
		applyPanel.setBorder(new TitledBorder("Update display"));
		applyPanel.add(makeApplyButton());
		applyPanel.add(makeResetButton());
		subpanel.add(applyPanel, gbc(2));
		// finally add the subpanel with a scroll pane and a desire to
		// fill some space..
		GridBagConstraints gbc = gbc(0);
		gbc.fill = GridBagConstraints.BOTH;
		add(new JScrollPane(subpanel),gbc);
	}
	
	public int preferredLocation() { return ViewerControls.RIGHT; }
	public boolean prefersToBeResized() { return true; }

	public Set propValuePairs()
	{
		TreeSet accum = new TreeSet();
		Set props = labels.getSpanProperties();
		for (Iterator i=props.iterator(); i.hasNext(); ) {
			String prop = (String)i.next();
			for (Span.Looper j=labels.getSpansWithProperty(prop); j.hasNext(); ) {
				Span s = j.nextSpan();
				String val = labels.getProperty(s, prop);
				accum.add( packPropValue(prop,val) );
			}
		}
		return accum;
	}
	private String packPropValue(String p,String v) { return p+"="+v; }
	private String[] unpackPropValue(String pv) { return pv.split("="); }

	public JButton makeResetButton()
	{
		return new JButton(new AbstractAction("-reset controls-") {
				public void actionPerformed(ActionEvent e) {
					for (int i=0; i<boxList.size(); i++) {
						((JComboBox)boxList.get(i)).setSelectedIndex(0);
					}
				}
			});
	}

	// a drop-down box of something's
	public JComboBox makeBox(String tag,Collection c)
	{
		final JComboBox box = new JComboBox();	
		box.addItem("-select "+tag+"-");
		for (Iterator j = c.iterator(); j.hasNext(); ) {
			box.addItem(j.next());
		}
		return box;
	}

	private class ColorTypeBoxListener implements ActionListener
	{
		private JComboBox colorBox,typeBox;
		private String type=null;
		public ColorTypeBoxListener(JComboBox colorBox,JComboBox typeBox) 
		{
			this.colorBox = colorBox; this.typeBox = typeBox;
		}
		public void actionPerformed(ActionEvent ev) 
		{
			if (typeBox.getSelectedIndex()>0) {
				type = (String) typeBox.getSelectedItem();
				if (colorBox.getSelectedIndex()!=0) { 
					typeColorCode.put(type,colorBox.getSelectedItem());
				} else {
					typeColorCode.remove(type);					
				}
			} else {
				if (type!=null) typeColorCode.remove(type);
				type = null;
			}
		}
	}

	private class ColorPropBoxListener implements ActionListener
	{
		private JComboBox colorBox,propValBox;
		private String[] propVal=null;
		public ColorPropBoxListener(JComboBox colorBox,JComboBox propValBox) 
		{
			this.colorBox = colorBox; this.propValBox = propValBox;
		}
		public void actionPerformed(ActionEvent ev) 
		{
			if (propValBox.getSelectedIndex()>0) {
				propVal = unpackPropValue((String)propValBox.getSelectedItem());
				if (colorBox.getSelectedIndex()!=0) { 
					putPropCode(propVal[0],propVal[1],(String)colorBox.getSelectedItem());
				} else {
					removePropCode(propVal[0],propVal[1]);
				}
			} else {
				if (propVal!=null) removePropCode(propVal[0],propVal[1]);
				propVal = null;
			}
		}
	}
	private void putPropCode(String p,String v,String color)
	{
		Map m = (Map)propColorCode.get(p);
		if (m==null) propColorCode.put(p, (m=new HashMap()));
		m.put(v, color);
	}
	private void removePropCode(String p,String v)
	{
		Map m = (Map)propColorCode.get(p);
		if (m==null) propColorCode.put(p, (m=new HashMap()));
		m.remove(v);
	}

	// build a drop-down box to select a color
	private JComboBox colorBox(final int i) 
	{
		final String type = (String)types.get(i);
		final String color = (String)typeColorCode.get(type);
		final JComboBox box = new JComboBox();	
		for (Iterator j = colorMap.keySet().iterator(); j.hasNext(); ) {
			box.addItem(j.next());
		}
		if (color!=null) box.setSelectedItem(color);
		else box.setSelectedIndex(0);
		box.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (box.getSelectedIndex()!=0) 
						typeColorCode.put(type,box.getSelectedItem());
					else
						typeColorCode.remove(type);
				}
			});
		return box;
	}
	// build GridBagConstraint with some default values
	private GridBagConstraints gbc(int i)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor=GridBagConstraints.WEST;
		gbc.weightx = gbc.weighty = 1.0;
		gbc.gridx = 0; gbc.gridy = i;
		return gbc;
	}

	// build GridBagConstraint with some default values
	private GridBagConstraints gbc(int row,int col)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.ipadx = 20;
		gbc.anchor=col==1?GridBagConstraints.WEST:GridBagConstraints.EAST;
		gbc.weightx = gbc.weighty = 1.0;
		gbc.gridx = col; gbc.gridy = row;
		return gbc;
	}

	//
	// what's exported to the viewer...
	//

	/** Tell the ControlledViewer what color is associated with a type.
	 */
	public SimpleAttributeSet getColor(String type)
	{
		String s = (String)typeColorCode.get(type);
		if (s==null) return null;
		else return (SimpleAttributeSet)colorMap.get(s);
	}

	/** Tell the ControlledViewer what color is associated with a property/value pair
	 */
	public SimpleAttributeSet getColor(String prop,String value)
	{
		Map m = (Map)propColorCode.get(prop);
		if (m==null) return null;
		String s = (String)m.get(value);
		if (s==null) return null;
		else return (SimpleAttributeSet)colorMap.get(s);
	}
	public Set getColoredProperties()
	{
		return propColorCode.keySet();
	}
	public Set getColoredValues(String prop)
	{
		Map m = (Map)propColorCode.get(prop);
		if (m==null) return Collections.EMPTY_SET;
		return m.keySet();
	}

	/** Export a span difference to the controlled Span Viewer.
	 */
	public SpanDifference getSpanDifference()
	{
		return sd;
	}

  // for an interactive test, below
	private static class TestViewer extends VanillaViewer implements Controllable
	{
		public TestViewer() { super("yow"); }
		public void applyControls(ViewerControls controls) { System.out.println("applied "+controls); }
	}

	public static void main(String[] argv)
	{
		try {
			TextLabels labels = edu.cmu.minorthird.text.learn.SampleExtractionProblem.trainLabels();
			Viewer v = new ControlledViewer( new TestViewer(), new MarkupControls(labels) );
			ViewerFrame f = new ViewerFrame("MarkupControls", v);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
