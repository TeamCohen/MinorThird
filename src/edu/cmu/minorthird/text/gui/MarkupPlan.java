package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.TextLabels;
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

public class MarkupPlan implements Visible
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

	private ArrayList types;
	private Map colorCode;
	private int numTypesWithColors;

	public MarkupPlan(TextLabels labels)
	{
		types = new ArrayList(labels.getTypes());
		colorCode = new HashMap();
	}
	public SimpleAttributeSet getColor(String type)
	{
		String s = (String)colorCode.get(type);
		if (s==null) return null;
		else return (SimpleAttributeSet)colorMap.get(s);
	}
	public Viewer toGUI()
	{
		PlanViewer v = new PlanViewer();
		v.setContent(this);
		return v;
	}
	public String toString()
	{
		return "[MarkupPlan: "+colorCode+"]";
	}
	
	// show the markup plan, and let the user edit it
	private static class PlanViewer extends ComponentViewer
	{																
		public JComponent componentFor(Object o)
		{
			final MarkupPlan plan = (MarkupPlan)o;
			final JPanel panel = new JPanel();
			final ArrayList boxList = new ArrayList();
			panel.setLayout(new GridBagLayout());
			GridBagConstraints gbc;
			for (int i=0; i<plan.types.size(); i++) {
				gbc = new GridBagConstraints();
				gbc.gridy = i; gbc.gridx = 0; 
				panel.add( new JLabel(plan.types.get(i).toString()), gbc );
				gbc = new GridBagConstraints();
				gbc.gridy = i; gbc.gridx = 1; 
				JComboBox box = colorBox( plan, i); 
				panel.add( box, gbc );
				boxList.add( box );
			}
			gbc = new GridBagConstraints();
			gbc.gridy = plan.types.size(); gbc.gridx=0;
			panel.add( new JButton(new AbstractAction("Reset") {
					public void actionPerformed(ActionEvent e) {
						for (int i=0; i<boxList.size(); i++) {
							((JComboBox)boxList.get(i)).setSelectedIndex(0);
						}
					}
				}), gbc);
			gbc = new GridBagConstraints();
			gbc.gridy = plan.types.size(); gbc.gridx=1;
			panel.add( new JButton(new AbstractAction("Update") {
					public void actionPerformed(ActionEvent e) {
						sendSignal(OBJECT_UPDATED,plan);
					}
				}), gbc);
			return panel;
		}
		private JComboBox colorBox(final MarkupPlan plan,final int i) 
		{
			final String type = (String)plan.types.get(i);
			final String color = (String)plan.colorCode.get(type);
			final JComboBox box = new JComboBox();
			for (Iterator j = colorMap.keySet().iterator(); j.hasNext(); ) {
				box.addItem(j.next());
			}
			if (color!=null) box.setSelectedItem(color);
			else box.setSelectedIndex(0);
			box.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (box.getSelectedIndex()!=0) 
							plan.colorCode.put(type,box.getSelectedItem());
						else
							plan.colorCode.remove(type);
					}
				});
			return box;
		}
	}

	public static void main(String[] argv)
	{
		try {
			TextLabels labels = FancyLoader.loadTextLabels("env/seminar-subset");
			Viewer v = new PlanViewer();
			v.setContent(new MarkupPlan(labels));
			//v.receiveContent(s);
			ViewerFrame f = new ViewerFrame("MarkupViewer", v);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
