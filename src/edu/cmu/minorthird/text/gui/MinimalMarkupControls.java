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


/** A compact control window for marking spans up in a labeling.
 *
 * @author William Cohen
 */

public class MinimalMarkupControls extends MarkupControls
{
  private static final String MENU_STRING = " -select type- ";
  private static final SimpleAttributeSet HIGHLIGHT_COLOR = HiliteColors.yellow;

  private JComboBox typeBox; // selects highlighted type
	private SpanDifference sd; // result of diffing two types

	public MinimalMarkupControls(TextLabels labels)
	{
		super(labels);
	}

	/** Lay out the controls - override the super class
	 */
	protected void initialize()
	{
		if (types==null) return; // will go back and initialize later
    typeBox = new JComboBox();
    typeBox.addItem(MENU_STRING);
    for (Iterator i=types.iterator(); i.hasNext(); ) {
      String type = (String)i.next();
      typeBox.addItem(type);
    }
    add(typeBox);
    typeBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
					MinimalMarkupControls.this.getControlledViewer().applyControls(MinimalMarkupControls.this);
        }
      });
    addApplyButton();
	}
	public int preferredLocation() { return ViewerControls.BOTTOM; }
	public boolean prefersToBeResized() { return true; }

	//
	// what's exported to the viewer...
	//

	/** Tell the ControlledViewer what color is associated with a type.
	 */
	public SimpleAttributeSet getColor(String type)
	{
    String selectedType = (String)typeBox.getSelectedItem();
    if (selectedType.equals(type)) return HIGHLIGHT_COLOR;
    else return null;
	}

	/** Tell the ControlledViewer what color is associated with a property/value pair
	 */
	public SimpleAttributeSet getColor(String prop,String value)
	{
    return null;
  }
	public Set getColoredProperties()
	{
		return Collections.EMPTY_SET;
	}
	public Set getColoredValues(String prop)
	{
		return Collections.EMPTY_SET;
  }

	/** Export a span difference to the controlled Span Viewer.
	 */
	public SpanDifference getSpanDifference()
	{
		return null;
	}

}
