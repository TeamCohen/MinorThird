package edu.cmu.minorthird.util.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Wraps a viewer and adds a status message at the bottom.
 * 
 * @author William cohen
 */

public class MessageViewer extends Viewer
{
	private Viewer subViewer;
	private JTextField statusField;

	public MessageViewer()
	{
		super();
	}
	public MessageViewer(Object obj)
	{
		super(obj);
	}

	public MessageViewer(Viewer subViewer)
	{
		super();
		setSubView(subViewer);
	}

	public void setSubView(Viewer subViewer)
	{
		this.subViewer = subViewer;
		subViewer.setSuperView(this);
		removeAll();
		add( subViewer, fillerGBC() );
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = gbc.weighty = 0;
		gbc.gridx = 0; gbc.gridy = 1;
		statusField = new JTextField("");
		add( statusField, gbc );
	}

	//
	// delegate operations to subViewer
	//

	final public void receiveContent(Object obj)
	{
		subViewer.setContent(obj);
	}
	public void clearContent()
	{
		subViewer.clearContent();
	}
	final public boolean canReceive(Object obj)
	{
		return subViewer.canReceive(obj);
	}
	final protected boolean canHandle(int signal,Object argument,ArrayList senders)
	{
		// grab TEXT_MESSAGE signals
		if (signal==TEXT_MESSAGE) return true;
		else return subViewer.canHandle(signal,argument,senders);		
	}
	final protected void handle(int signal,Object argument,ArrayList senders)
	{
		if (signal==TEXT_MESSAGE) {
			statusField.setText(argument.toString());
		} else {
			subViewer.handle(signal,argument,senders);
		}
	}
	final protected void initialize()
	{
		setLayout(new GridBagLayout());
	}
}
