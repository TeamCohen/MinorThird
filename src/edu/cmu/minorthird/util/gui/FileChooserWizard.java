package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Map;


/** Wizard that picks a file.
 *
 * @author William Cohen
 */

abstract public class FileChooserWizard extends NullWizardPanel
{
	// the file being operated on
	private File file = null;
	// if true, open a file, else save a file
	private boolean openFile = true;
	// indicate if the file was saved or loaded 
	private boolean complete = false;
	// file chooser used in action
	private static final JFileChooser chooser;
	// pane to hold chosen file
	private JTextField filePane;
  // FileChooser Dialog
  protected WizardPanel nextWizardPanel;

  static {
    chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
  };

	// to pass on to a WizardViewer
	protected Map viewerContext;
	protected String key;

	public FileChooserWizard(
		String aKey,Map aViewerContext,
		final boolean openFile,final String titleString,final String promptString)
	{
		this.key = aKey;
		this.viewerContext = aViewerContext;
    this.openFile = openFile;

		setBorder(new TitledBorder(titleString));
		add(new JLabel(promptString));
		filePane = new JTextField(20);
		add(filePane);
		//add(chooser);
		//chooser.addActionListener(new BrowseAction());
		add(new JButton(new BrowseAction()));
	}

	private class BrowseAction extends AbstractAction
	{
		public BrowseAction() { super("Browse"); }
		synchronized public void actionPerformed(ActionEvent ev) {
			int returnVal = openFile ? chooser.showOpenDialog(null) : chooser.showSaveDialog(null);
			if (returnVal==JFileChooser.APPROVE_OPTION) {
				file = chooser.getSelectedFile();
				filePane.setText( file.getName() );
				viewerContext.put(key, file);
				complete = true;
			}
		}
	}

  public void setNextWizardPanel(WizardPanel nextWizardPanel)
  {
    this.nextWizardPanel = nextWizardPanel;
  }

	public boolean canFinish() { return false; }
	public boolean validateFinish(java.util.List list) { 	return false; }
	public boolean hasNext() { return true; }
	public boolean validateNext(java.util.List list) { list.add("You need to pick a file"); return file!=null; }
	public WizardPanel next() { return nextWizardPanel; }
}
