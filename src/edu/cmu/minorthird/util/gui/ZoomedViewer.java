package edu.cmu.minorthird.util.gui;

import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * When an object is selected in the zoomedOut view, it's displayed in
 * the zoomedIn view.  Used for getting a close up view of, say,
 * objects in a list.
 * 
 * @author William cohen
 */

public class ZoomedViewer extends SplitViewer
{
	static private Logger log = Logger.getLogger(ZoomedViewer.class);
	// rename viewer1,viewer2
	private Viewer zoomedOut, zoomedIn;

	public ZoomedViewer()
	{
		super();
	}
	public ZoomedViewer(Viewer zoomedOut,Viewer zoomedIn)
	{
		super(zoomedOut,zoomedIn);
		this.zoomedOut=viewer1;
		this.zoomedIn=viewer2;
	}
	public void setSubViews(Viewer zoomedOut,Viewer zoomedIn)
	{
		super.setSubViews(zoomedOut,zoomedIn);
		this.zoomedOut=viewer1;
		this.zoomedIn=viewer2;
	}

	public void receiveContent(Object content)
	{
		log.info("recieving content: "+content);
		zoomedOut.setContent(content);
		zoomedIn.clearContent();
	}

	public boolean canReceive(Object content)
	{
		return zoomedOut!=null && zoomedOut.canReceive(content);
	}

	protected void handle(int signal,Object argument,ArrayList senders) 
	{
		zoomedIn.setContent(argument);
		revalidate();
	}

	protected boolean canHandle(int signal,Object argument,ArrayList senders) 
	{
		if (signal==OBJECT_SELECTED && senders.contains(zoomedOut) && zoomedIn.canReceive(argument)) {
			return true;
		} else {
			return false;			
		}
	}

}
