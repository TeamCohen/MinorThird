package edu.cmu.minorthird.text.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A panel including a status message on the bottom.
 * 
 * @author William Cohen
 */

public class StatusMessagePanel extends JPanel{

	static final long serialVersionUID=200803014L;
	
	public StatusMessagePanel(JComponent component,StatusMessage statusMsg){
		super();
		setLayout(new GridBagLayout());
		GridBagConstraints gbc;
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.gridx=1;
		gbc.gridy=1;
		add(component,gbc);
		gbc=new GridBagConstraints();
		gbc.weightx=1.0;
		gbc.weighty=0.0;
		gbc.gridx=1;
		gbc.gridy=2;
		gbc.anchor=GridBagConstraints.WEST;
		add(statusMsg,gbc);
	}
}
