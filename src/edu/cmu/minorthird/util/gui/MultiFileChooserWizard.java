package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;


/** Wizard that picks a list of files.
 *
 * @author William Cohen
 */

abstract public class MultiFileChooserWizard extends NullWizardPanel
{
	// the file being operated on
	private File file = null;
	// list of chosen files
	private ArrayList fileList = new ArrayList();

	// to pass on to a WizardViewer
	protected Map viewerContext;
	protected String key;

	public MultiFileChooserWizard(
		String key,Map viewerContext,
		String titleString,String promptString,
		final JFileChooser chooser)
	{
		this.key = key;
		this.viewerContext = viewerContext;
		viewerContext.put(key, fileList);

		setBorder(new TitledBorder(titleString));
		add(new JLabel(promptString));

		final JTextField filePane = new JTextField(20);
		add(filePane);

		final JList jFileList = new JList();
		Dimension wide = new Dimension(600,100);
		jFileList.setPreferredSize(wide);
		add(new JButton(new AbstractAction("Browse") {
				public void actionPerformed(ActionEvent ev) {
					int returnVal = chooser.showOpenDialog(null);
					if (returnVal==JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						filePane.setText( file.getName() );
						fileList.add( file );
						jFileList.setListData( fileList.toArray() );
					}
				}
			}));
		JPanel listPanel = new JPanel();
		listPanel.setPreferredSize(wide);
		listPanel.setBorder(new TitledBorder("Selected Files"));
		JScrollPane scroller = new JScrollPane(jFileList);
		scroller.setPreferredSize(wide);
		listPanel.add(scroller);
		add(listPanel);
	}
	public boolean hasNext() { return true; }
	public boolean validateNext(java.util.List list) 
	{ 
		list.add("You need to pick at least one file!"); 
		return fileList.size() >= 1;
	}
	abstract public WizardPanel next();
}
