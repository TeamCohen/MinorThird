package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.Map;
import java.awt.*;


/** Simple case for a viewer wizard.
 *
 * @author William Cohen
 */

public class SimpleViewerWizard extends ViewerWizard
{
	private String titleString,promptString;
	protected WizardPanel nextWizardPanel;

	public SimpleViewerWizard(
		String key,Map viewerContext,
		String titleString,String promptString,
		WizardPanel nextWizardPanel)
	{
		super(key,viewerContext);
		this.titleString = titleString;
		this.promptString = promptString;
		this.nextWizardPanel = nextWizardPanel;
    ((SimpleViewerPanel) this.getWizardPanel()).init();

	}
	public WizardPanel buildWizardPanel() {	return new SimpleViewerPanel(); }
	protected class SimpleViewerPanel extends NullWizardPanel
	{
		public SimpleViewerPanel() {
    }

    private void init()
    {
      setBorder(new TitledBorder(titleString));
      this.setLayout(new GridLayout(0, 1));
      add(new JLabel(promptString));
    }

    public boolean hasNext() { return true; }
		public boolean validateNext(java.util.List list) { return true; }
		public WizardPanel next() { return nextWizardPanel; }
	}
	public void addViewer(Viewer viewer)	
	{
		viewer.setSuperView(this);
		viewerContext.put(myKey, viewer.getContent());
		getWizardPanel().add(viewer); 
	}
}
