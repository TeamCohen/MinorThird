package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;

/**
 * 
 * Top-level container for a Wizard.
 * 
 * @author William Cohen
 *
 */

public class WizardFrame extends JFrame
{
	public WizardFrame(String title,WizardPanel startPanel)
	{
		super(title);
		addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					System.exit(0);
				}
			});
		Wizard wizard = new Wizard();
		wizard.addWizardListener(new WizardAdapter() {
				public void wizardFinished(Wizard wizard) {	System.exit(0);	}
				public void wizardCancelled(Wizard wizard) {  System.exit(0); }
			});
    wizard.setPreferredSize(new Dimension(600,250));
		setContentPane(wizard);
		pack();
		setVisible(true);
		wizard.start( startPanel );
	}
}
