package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/** Wizard that picks from one of several options using radio buttons.
 *
 * @author William Cohen
 */

public class RadioWizard extends NullWizardPanel
{
	private Map wizardToStringMap = new HashMap();
	private Map buttonToWizardMap = new HashMap();
	private ButtonGroup buttonGroup = new ButtonGroup();

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
		add(new JLabel(promptString));

		buttonToWizardMap = new HashMap();
		wizardToStringMap = new HashMap();
		ButtonGroup group = new ButtonGroup();
	}
	public void addButton(String tag,WizardPanel wizardPanel,boolean isSelected)
	{
		JRadioButton button = new JRadioButton(tag, isSelected);
		buttonGroup.add(button);
		add(button);
		buttonToWizardMap.put(button,wizardPanel);
		wizardToStringMap.put(wizardPanel,tag);
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
