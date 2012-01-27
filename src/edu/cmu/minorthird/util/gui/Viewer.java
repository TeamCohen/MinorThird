/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

/**
 * 
 * Visualize an object, and obey certain rules for propogating events.
 * 
 * <p>
 * Specifically, a Viewer has a superview, which is also a viewer, and displays
 * some 'content'. The canReceive(), clearContent(), and receiveContent()
 * methods are used for displaying content. Viewers also send signals via the
 * sendSignal() method. Signals are passed up to a superview, its superview, and
 * so on, until a Viewer is found that canHandle() that signal, at which point
 * the signal is trapped, and processed.
 * 
 * <p>
 * Viewers are intended to be used for linked display components.
 * 
 * @author William cohen
 * 
 */

public abstract class Viewer extends JPanel{
	
	static final long serialVersionUID=20081125L;

	static private Logger log=Logger.getLogger(Viewer.class);

	static private final boolean DEBUG=log.isDebugEnabled();

	static private final String ONLY_SUBVIEWER="*main*";

	static protected final int SET_CONTENT=1;

	static protected final int TEXT_MESSAGE=2;

	static protected final int OBJECT_SELECTED=3;

	static protected final int OBJECT_UPDATED=4;

	private Viewer superView=null;

	private Object content="empty view";

	/** Maps String 'names' of subviews to viewers. */
	protected Map<String,Viewer> namedSubViews=new TreeMap<String,Viewer>();

	public Viewer(){
		this(null);
	}

	public Viewer(Object content){
		initialize();
		if(content!=null)
			setContent(content);
	}

	//
	// setters & getters
	//

	/** Declare this viewer to be the only subview of superView. */
	final public void setSuperView(Viewer superView){
		// if (namedSubViews.keySet().size()>1)
		// throw new IllegalStateException("superview already has a subview");
		setSuperView(superView,ONLY_SUBVIEWER);
	}

	/** Declare this viewer to be a subview of superView. */
	final public void setSuperView(Viewer superView,String title){
		// if (namedSubViews.get(title)!=null)
		// throw new IllegalStateException("superview already has a subview named
		// "+title);
		this.superView=superView;
		superView.namedSubViews.put(title,this);
	}

	/** Get Viewer in which this viewer is contained. */
	final public Viewer getSuperView(){
		return superView;
	}

	/** Change the object being displayed by this viewer. */
	final public void setContent(Object content){
		setContent(content,false);
	}

	/**
	 * Change the object being displayed by this viewer. If forceUpdate is false,
	 * do not force the display to be changed.
	 */
	final public void setContent(Object content,boolean forceUpdate){
		if(content!=this.content||forceUpdate){
			this.content=content;
			receiveContent(content);
			sendSignal(SET_CONTENT,content);
		}
	}

	/**
	 * Get the object being displayed, as determined by the last call to
	 * setContent().
	 */
	final public Object getContent(){
		return content;
	}

	/** Get the object being displayed as the user sees it. */
	public Object getVisibleContent(){
		if(namedSubViews.size()==1&&namedSubViews.get(ONLY_SUBVIEWER)!=null){
			Object result=
					(namedSubViews.get(ONLY_SUBVIEWER)).getVisibleContent();
			return result;
		}else{
			return content;
		}
	}

	public Object getSerializableContent(){
		if(!(content instanceof Serializable)&&
				namedSubViews.get(ONLY_SUBVIEWER)!=null){
			Object result=
					(namedSubViews.get(ONLY_SUBVIEWER)).getSerializableContent();
			return result;
		}else
			return content;
	}

	//
	// signalling
	//

	/** Send a signal. */
	final protected void sendSignal(int signal,Object argument){
		if(DEBUG)
			log.debug("signal sent by "+this+": "+signal+","+argument);
		if(superView!=null){
			List<Viewer> senders=new ArrayList<Viewer>();
			senders.add(this);
			superView.hearBroadcast(signal,argument,senders);
		}
	}

	/**
	 * Listen to passing signals, and either handle them, or propogare them upward
	 * to the superViewer.
	 */
	final private void hearBroadcast(int signal,Object argument,
			List<Viewer> senders){
		if(canHandle(signal,argument,senders)){
			if(DEBUG)
				log.debug("signal claimed by "+this+": "+signal+","+argument+","+
						senders);
			handle(signal,argument,senders);
		}else if(superView!=null){
			if(DEBUG)
				log.debug("signal forwarded to "+superView+": "+signal+","+argument+
						","+senders);
			senders.add(this);
			superView.hearBroadcast(signal,argument,senders);
		}else if(superView==null){
			if(DEBUG)
				log.debug("no superview set for "+this);
		}
	}

	//
	// abstract actions
	//

	/** Called at creation time. Initialize the layout of this view. */
	abstract protected void initialize();

	/** Called when new content obtained. So load it into the viewer. */
	abstract public void receiveContent(Object obj);

	/** Called when no content should be displayed. */
	abstract public void clearContent();

	/** See if proposed content is displayable. */
	abstract public boolean canReceive(Object obj);

	/** Handle a signal from a subview. */
	abstract protected void handle(int signal,Object argument,List<Viewer> senders);

	/** Offer to handle a signal. */
	abstract protected boolean canHandle(int signal,Object argument,
			List<Viewer> senders);

	/** Provide a set of subview names */
	public Set<String> getSubViewNames(){
		Viewer onlySubviewer=namedSubViews.get(ONLY_SUBVIEWER);
		if(onlySubviewer!=null)
			return onlySubviewer.getSubViewNames();
		else
			return namedSubViews.keySet();
	}

	/** Retrieve a subview by name. */
	public Viewer getNamedSubView(String name){
		Viewer onlySubviewer=namedSubViews.get(ONLY_SUBVIEWER);
		if(onlySubviewer!=null)
			return onlySubviewer.getNamedSubView(name);
		else
			return namedSubViews.get(name);
	}

	//
	// utility functions
	//
	/**
	 * Useful default case for a GridBagConstraint.
	 */
	protected static GridBagConstraints fillerGBC(){
		GridBagConstraints gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=gbc.weighty=1.0;
		gbc.gridx=gbc.gridy=0;
		return gbc;
	}

	/**
	 * Add a list selection listener which sends the appropriate viewer signal.
	 * The transformer is used to re-map the selected value.
	 */
	protected void monitorSelections(final JList jlist,final Transform transformer){
		jlist.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e){
				int index=jlist.getSelectedIndex();
				sendSignal(OBJECT_SELECTED,transformer.transform(jlist.getModel().getElementAt(index)));
			}
		});
	}

	/**
	 * Add a list selection listener which sends the appropriate viewer signal.
	 */
	protected void monitorSelections(final JList jlist){
		monitorSelections(jlist,IDENTITY_TRANSFORM);
	}

	/**
	 * Add a list selection listener which sends the appropriate viewer signal.
	 * The transformer is used to re-map the selected value.
	 */
	protected void monitorSelections(final JTable jtable,final int colIndex,
			final Transform transformer){
		jtable.addMouseListener(new MouseAdapter(){

			@Override
			public void mouseClicked(MouseEvent e){
				int rowIndex=jtable.rowAtPoint(e.getPoint());
				sendSignal(OBJECT_SELECTED,transformer.transform(jtable.getModel()
						.getValueAt(rowIndex,colIndex)));
			}
		});
	}

	/**
	 * Add a list selection listener which sends the appropriate viewer signal.
	 */
	protected void monitorSelections(final JTable jtable,final int colIndex){
		monitorSelections(jtable,colIndex,IDENTITY_TRANSFORM);
	}

	protected interface Transform{

		public Object transform(Object o);
	}

	final private Transform IDENTITY_TRANSFORM=new Transform(){

		@Override
		public Object transform(Object o){
			return o;
		}
	};
}
