package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.awt.*;


/** Wizard that picks from one of several options using radio buttons.
 *
 * @author William Cohen
 */

public class RadioWizard extends NullWizardPanel
{
	private Map wizardToStringMap = new HashMap();
	private Map buttonToWizardMap = new HashMap();
	private ButtonGroup buttonGroup = new ButtonGroup();
  private JPanel buttonPanel = new JPanel(new GridLayout(0, 1));

	// to pass on to a WizardViewer
	protected Map viewerContext;
	protected String key;

	public RadioWizard(
		String aKey,Map aViewerContext,
		final String titleString,final String promptString)
	{
		this.key = aKey;
		this.viewerContext = aViewerContext;

		setBorder(new TitledBorder(titleString));
		this.setLayout(new BorderLayout());
    JPanel mainPanel = new JPanel(new GridLayout(0, 1));
    mainPanel.add(new JLabel(promptString));
    mainPanel.add(buttonPanel);
    add(mainPanel, BorderLayout.NORTH);

		buttonToWizardMap = new HashMap();
		wizardToStringMap = new HashMap();
	}

  /**
   * Add a Radiobutton to the panel
   * @param label text to display with the button
   * @param actionName name to be stored in the context map when selected
   * @param wizardPanel wizard panel to use next if this button is selected
   * @param isSelected whether it should appear selected
   */
  public void addButton(String label, String actionName, WizardPanel wizardPanel, boolean isSelected)
	{
		JRadioButton button = new JRadioButton(label, isSelected);
		buttonGroup.add(button);
		buttonPanel.add(button);
		buttonToWizardMap.put(button,wizardPanel);
		wizardToStringMap.put(wizardPanel,actionName);
	}

	public boolean canFinish() { return false; }
	public boolean validateFinish(java.util.List list) { return false; }
	public boolean hasNext() { return true; }
	public boolean validateNext(java.util.List list) { return true; }

	public WizardPanel next()
	{
		for (Iterator i=buttonToWizardMap.keySet().iterator(); i.hasNext(); ) {		
			JRadioButton button = (JRadioButton)i.next();
			if (button.isSelected()) {
				WizardPanel nextPanel = (WizardPanel)buttonToWizardMap.get(button);
				viewerContext.put(key, wizardToStringMap.get(nextPanel));
				//System.out.println("button: "+button+" panel: "+nextPanel+" viewerContext: "+viewerContext);
				return nextPanel;
			}
		}
		throw new IllegalStateException("nothing selected!");
	}
}
