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
	private static Map colorMap = new TreeMap(); 
	static {
		colorMap.put("-select-",new SimpleAttributeSet());
		colorMap.put("blue",HiliteColors.blue );
		colorMap.put("red",HiliteColors.red );
		colorMap.put("green",HiliteColors.green );
		colorMap.put("yellow",HiliteColors.yellow );
		colorMap.put("gray",HiliteColors.gray );
	}
	private TextLabels labels;
	private ArrayList types = new ArrayList();
	private Map colorCode = new HashMap();
	private int numTypesWithColors;
	private JComboBox guessBox, truthBox;
	private SpanDifference sd;

	public MarkupControls(TextLabels labels)
	{
		super();
		this.labels = labels;
		types = new ArrayList(labels.getTypes());
		colorCode = new HashMap();
		initialize(); // initialize again!
	}

	/** Lay out the controls
	 */
	protected void initialize()
	{
		if (types==null) return;
		setLayout(new GridBagLayout());

		JPanel subpanel = new JPanel();
		subpanel.setLayout(new GridBagLayout());
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
		comparePanel.setLayout(new BoxLayout(comparePanel,BoxLayout.Y_AXIS));
		comparePanel.add(guessBox);
		comparePanel.add(truthBox);
		//subpanel.add(guessBox,gbc(0));
		//subpanel.add(truthBox,gbc(1));
		subpanel.add(comparePanel,gbc(0));

		JPanel hilitePanel = new JPanel();
		hilitePanel.setBorder(new TitledBorder("Highlight types"));
		hilitePanel.setLayout(new GridBagLayout());
		final ArrayList boxList = new ArrayList();
		for (int i=0; i<types.size(); i++) {
			JPanel boxPanel = new JPanel();
			JComboBox box = colorBox(i); 
			boxPanel.add( box );
			boxPanel.add( new JLabel(types.get(i).toString()) );
			boxList.add( box );
			hilitePanel.add(boxPanel,gbc(i));
		}
		
		subpanel.add(hilitePanel, gbc(1));

		JPanel applyPanel = new JPanel();
		applyPanel.setBorder(new TitledBorder("Update display"));
		applyPanel.add( new JButton(new AbstractAction("Reset") {
				public void actionPerformed(ActionEvent e) {
					for (int i=0; i<boxList.size(); i++) {
						((JComboBox)boxList.get(i)).setSelectedIndex(0);
					}
				}
			}));
		applyPanel.add(makeApplyButton());
		subpanel.add(applyPanel, gbc(2));
		// finally add the subpanel with a scroll pane and a desire to
		// fill some space..
		GridBagConstraints gbc = gbc(0);
		gbc.fill = GridBagConstraints.BOTH;
		add(new JScrollPane(subpanel),gbc);
	}
	
	public int preferredLocation() { return ViewerControls.LEFT; }

	// build a drop-down box to select a color
	private JComboBox colorBox(final int i) 
	{
		final String type = (String)types.get(i);
		final String color = (String)colorCode.get(type);
		final JComboBox box = new JComboBox();
		for (Iterator j = colorMap.keySet().iterator(); j.hasNext(); ) {
			box.addItem(j.next());
		}
		if (color!=null) box.setSelectedItem(color);
		else box.setSelectedIndex(0);
		box.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (box.getSelectedIndex()!=0) 
						colorCode.put(type,box.getSelectedItem());
					else
						colorCode.remove(type);
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

	//
	// what's exported to the viewer...
	//

	/** Tell the ControlledViewer what color is associated with a type.
	 */
	public SimpleAttributeSet getColor(String type)
	{
		String s = (String)colorCode.get(type);
		if (s==null) return null;
		else return (SimpleAttributeSet)colorMap.get(s);
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
