package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.gui.*;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
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

	protected void initialize()
	{
		if (types==null) return;

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
		comparePanel.add(guessBox);
		comparePanel.add(truthBox);
		add(comparePanel);

		final ArrayList boxList = new ArrayList();
		for (int i=0; i<types.size(); i++) {
			JPanel boxPanel = new JPanel();
			JComboBox box = colorBox(i); 
			boxPanel.add( new JLabel(types.get(i).toString()) );
			boxPanel.add( box );
			boxList.add( box );
			add(boxPanel);
		}
		add( new JButton(new AbstractAction("Reset") {
				public void actionPerformed(ActionEvent e) {
					for (int i=0; i<boxList.size(); i++) {
						((JComboBox)boxList.get(i)).setSelectedIndex(0);
					}
				}
			}));
		addApplyButton();
	}
	

	public SimpleAttributeSet getColor(String type)
	{
		String s = (String)colorCode.get(type);
		if (s==null) return null;
		else return (SimpleAttributeSet)colorMap.get(s);
	}

	public SpanDifference getSpanDifference()
	{
		return sd;
	}

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
