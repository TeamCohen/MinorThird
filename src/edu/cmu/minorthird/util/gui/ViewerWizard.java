package edu.cmu.minorthird.util.gui;

import jwf.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/** Wraps a WizardPanel (from the Java Wizard Framework, jwf) in a
 * viewer construct.  Whatever is selected by the viewer is stored in
 * the viewerContext map, under the given key.
 *
 * @author William Cohen
 */

abstract public class ViewerWizard extends ComponentViewer 
{
	final protected String myKey;
	final protected Map viewerContext;
	final private WizardPanel wizardPanel;

	/**
	 * Construct a ViewerWizard.  
	 * @param viewerContext scratchPad for information passed between wizards.
	 * @param key if an object "obj" is selected in some viewer contained
	 * in the WizardPanel, then this object will be stored in the viewerContext,
	 * under this key.
	 */
	public ViewerWizard(String key,Map viewerContext)	
	{	
		this.myKey = key; 
		this.viewerContext = viewerContext;	
		this.wizardPanel = buildWizardPanel();
	}

	public ViewerWizard(String key)	
	{	
		this(key,new HashMap()); 
	}

	/** Return the WizardPanel which contains the viewer.
	 */
	public WizardPanel getWizardPanel()	{	return wizardPanel;	}

	public JComponent componentFor(Object o) 	{ return wizardPanel;	}

	public boolean canHandle(int signal,Object argument,ArrayList senders) { return (signal==OBJECT_SELECTED);}

	public void handle(int signal,Object argument,ArrayList senders) 
	{
		if (signal==OBJECT_SELECTED) {
			System.out.println("selected "+argument);
			viewerContext.put(myKey,argument);
		}
	}

	/** Construct a WizardPanel, which contains some Viewer.
	 */
	abstract public WizardPanel buildWizardPanel();
}
