package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.Map;
import java.awt.*;

//import edu.cmu.minorthird.ui.SimpleViewerPanel;


/** Simple case for a viewer wizard.
 *
 * @author William Cohen
 */

public class SimpleViewerWizard extends ViewerWizard
{
	private String titleString,promptString;
	private SimpleViewerPanel nextWizardPanel;

	public SimpleViewerWizard(
		String key,Map viewerContext,
		String titleString,String promptString,
		SimpleViewerPanel nextWizardPanel)
	{
		super(key,viewerContext);
		this.titleString = titleString;
		this.promptString = promptString;
		this.nextWizardPanel = nextWizardPanel;
    this.wizardPanel = buildWizardPanel();
    this.getWizardPanel().init();
    init();

	}
	public SimpleViewerPanel buildWizardPanel()
  {	return new SimpleViewerPanel(this); }

  public void addViewer(Viewer viewer)
	{
    addViewer(viewer, myKey);
	}

  public void addViewer(Viewer viewer, String key)
  {
    viewer.setSuperView(this);
    viewerToKeyMap.put(viewer, key);
    viewerContext.put(key, viewer.getContent());
    getWizardPanel().add(viewer);
  }

  /**
   * place holder allowing sub-classes to initialize the viewer panel
   */
  public void init()
  { }


//--------------------------------------------------------------------------------
  /**
   * A simple Panel for a wizard.
   *
   * important part is the init method which allows the panel to configure itself dynamicly
   * @author ksteppe
   */
  public class SimpleViewerPanel extends NullWizardPanel
  {
    private String title;
    private String prompt;
    private SimpleViewerPanel nextPanel;
    protected Map context;
    protected SimpleViewerWizard parent;

    public SimpleViewerPanel(SimpleViewerWizard parent)
    {
      this.nextPanel = nextWizardPanel;
      log.debug("SVP - set next: " + nextPanel);
      this.prompt = promptString;
      this.title = titleString;
      this.context = viewerContext;
      this.parent = parent;
    }

    /**
     * construct the panel
     */
    public void init()
    {
//      log.debug("init panel");
      this.removeAll();
      setBorder(new TitledBorder(title));
//      this.setLayout(new GridLayout(0, 1));
      add(new JLabel(prompt));
      if (parent !=null)
        parent.init();
      else
        log.warn("parent null");
    }

    public boolean hasNext()
    { return true; }

    public boolean validateNext(java.util.List list)
    { return true; }

    public WizardPanel next()
    {
      nextPanel.init();
      return nextPanel;
    }
  }

}
