package edu.cmu.minorthird.util.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JTextField;

/**
 * Wraps a viewer and adds a status message at the bottom.  Status
 * messages are sent by a sub-viewer by sending a message with a class
 * to sendSignal(TEXT_MESSAGE, "content of message");
 * 
 * @author William cohen
 */

public class MessageViewer extends Viewer{
	
	static final long serialVersionUID=20080517L;

	private Viewer subViewer;

	private JTextField statusField;

	public MessageViewer(){
		super();
	}

	public MessageViewer(Object obj){
		super(obj);
	}

	public MessageViewer(Viewer subViewer){
		super();
		setSubView(subViewer);
	}

	public void setSubView(Viewer subViewer){
		this.subViewer=subViewer;
		subViewer.setSuperView(this);
		removeAll();
		add(subViewer,fillerGBC());
		GridBagConstraints gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=gbc.weighty=0;
		gbc.gridx=0;
		gbc.gridy=1;
		statusField=new JTextField("");
		add(statusField,gbc);
	}

	//
	// delegate operations to subViewer
	//

	@Override
	final public void receiveContent(Object obj){
		subViewer.setContent(obj);
	}

	@Override
	public void clearContent(){
		subViewer.clearContent();
	}

	@Override
	final public boolean canReceive(Object obj){
		return subViewer.canReceive(obj);
	}

	@Override
	final protected boolean canHandle(int signal,Object argument,
			List<Viewer> senders){
		// grab TEXT_MESSAGE signals
		if(signal==TEXT_MESSAGE)
			return true;
		else
			return subViewer.canHandle(signal,argument,senders);
	}

	@Override
	final protected void handle(int signal,Object argument,List<Viewer> senders){
		if(signal==TEXT_MESSAGE){
			statusField.setText(argument.toString());
		}else{
			subViewer.handle(signal,argument,senders);
		}
	}

	@Override
	final protected void initialize(){
		setLayout(new GridBagLayout());
	}
}
