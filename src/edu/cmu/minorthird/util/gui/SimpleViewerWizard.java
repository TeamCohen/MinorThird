package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.Map;


/** Simple case for a viewer wizard.
 *
 * @author William Cohen
 */

public class SimpleViewerWizard extends ViewerWizard
{
	private String titleString,promptString;
	private WizardPanel nextWizardPanel;

	public SimpleViewerWizard(
		String key,Map viewerContext,
		String titleString,String promptString,
		WizardPanel nextWizardPanel)
	{
		super(key,viewerContext);
		this.titleString = titleString;
		this.promptString = promptString;
		this.nextWizardPanel = nextWizardPanel;
	}
	public WizardPanel buildWizardPanel() {	return new SimpleViewerPanel(); }
	private class SimpleViewerPanel extends NullWizardPanel
	{
		public SimpleViewerPanel() {
			setBorder(new TitledBorder(titleString));
			add(new JLabel(promptString));
		}
		public boolean hasNext() { return true; }
		public boolean validateNext(java.util.List list) { return true; }
		public WizardPanel next() { return nextWizardPanel; }
	}
	public void addViewer(Viewer viewer)	
	{ 
		viewer.setSuperView(SimpleViewerWizard.this);
		viewerContext.put(myKey, viewer.getContent());
		getWizardPanel().add(viewer); 
	}
}
