/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

/**
 * A Viewer coupled with a ViewerControls object.
 * 
 * @author William Cohen
 */

public class ControlledViewer extends Viewer{

	static final long serialVersionUID=20080517L;
	
	private Viewer viewer=null;

	private ViewerControls controls=null;

	public ControlledViewer(){
		super();
	}

	public ControlledViewer(Viewer viewer,ViewerControls controls){
		super();
		setComponents(viewer,controls);
	}

	public void setComponents(Viewer viewer,ViewerControls controls){
		if(!(viewer instanceof Controllable))
			throw new IllegalArgumentException("viewer must be controllable");
		this.controls=controls;
		this.viewer=viewer;
		controls.setControlledViewer(viewer);
		viewer.setSuperView(this);
		removeAll();

		if(controls.prefersToBeResized()){
			if(controls.preferredLocation()==ViewerControls.BOTTOM){
				JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
				splitPane.setResizeWeight(0.90);
				splitPane.setTopComponent(viewer);
				splitPane.setBottomComponent(controls);
				add(splitPane,fillerGBC());
			}else if(controls.preferredLocation()==ViewerControls.TOP){
				JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
				splitPane.setResizeWeight(0.10);
				splitPane.setTopComponent(controls);
				splitPane.setBottomComponent(viewer);
				add(splitPane,fillerGBC());
			}else if(controls.preferredLocation()==ViewerControls.RIGHT){
				JSplitPane splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
				splitPane.setResizeWeight(0.75);
				splitPane.setLeftComponent(viewer);
				splitPane.setRightComponent(controls);
				add(splitPane,fillerGBC());
			}else if(controls.preferredLocation()==ViewerControls.LEFT){
				JSplitPane splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
				splitPane.setResizeWeight(0.25);
				splitPane.setLeftComponent(controls);
				splitPane.setRightComponent(viewer);
				add(splitPane,fillerGBC());
			}else{
				throw new IllegalArgumentException(
						"controls has illegal preferred location "+
								controls.preferredLocation()+": "+controls);
			}
		}else{
			int x1,y1,x2,y2,part;
			int loc=controls.preferredLocation();
			if(loc==ViewerControls.BOTTOM){
				x1=0;
				y1=0;
				x2=0;
				y2=1;
				part=10;
			}else if(loc==ViewerControls.TOP){
				x1=0;
				y1=1;
				x2=0;
				y2=0;
				part=9;
			}else if(loc==ViewerControls.RIGHT){
				x1=0;
				y1=0;
				x2=1;
				y2=0;
				part=4;
			}else if(loc==ViewerControls.LEFT){
				x1=1;
				y1=0;
				x2=0;
				y2=0;
				part=1;
			}else{
				throw new IllegalArgumentException(
						"controls has illegal preferred location "+loc+": "+controls);
			}
			GridBagConstraints gbc=fillerGBC();
			gbc.gridx=x1;
			gbc.gridy=y1;
			add(viewer,gbc);
			gbc=fillerGBC();
			gbc.gridx=x2;
			gbc.gridy=y2;
			gbc.weightx/=part;
			gbc.weighty/=part;
			add(controls,gbc);
		}
	}

	public ViewerControls getControls(){
		return controls;
	}

	@Override
	protected void initialize(){
		setLayout(new GridBagLayout());
	}

	//
	// delegate signals & content to sub-viewer
	//

	@Override
	public void clearContent(){
		viewer.clearContent();
	}

	@Override
	public boolean canReceive(Object obj){
		return viewer.canReceive(obj);
	}

	@Override
	public void receiveContent(Object obj){
		viewer.setContent(obj);
	}

	@Override
	protected boolean canHandle(int signal,Object argument,List<Viewer> senders){
		return viewer.canHandle(signal,argument,senders);
	}

	@Override
	protected void handle(int signal,Object argument,List<Viewer> senders){
		viewer.handle(signal,argument,senders);
	}

	//
	// a very simple test case
	//

	/** a test case */
	public static void main(String[] argv){
		Viewer v=new ControlledViewer(new MyViewer(),new MyControls());
		v.setContent("William Cohen");
		new ViewerFrame("test",v);
	}

	// for test case
	static private class MyViewer extends TransformedVanillaViewer implements
			Controllable{

		static final long serialVersionUID=20080517L;
		
		private boolean uc;

		private String prefix;

		private Object lastObj;

		@Override
		public Object transform(Object o){
			lastObj=o;
			String s=o.toString();
			String result=uc?s.toUpperCase():s;
			if(prefix!=null)
				result=prefix+result;
			System.out.println("transform: "+o+" => "+result);
			return result;
		}

		@Override
		public void applyControls(ViewerControls c){
			System.out.println("controls: "+c);
			MyControls mc=(MyControls)c;
			uc=mc.ucBox.isSelected();
			prefix=mc.prefixField.getText();
			System.out
					.println("recieving: "+lastObj+" with uc="+uc+" prefix="+prefix);
			receiveContent(lastObj);
			revalidate();
		}
	}

	// for test case
	static private class MyControls extends ViewerControls{

		static final long serialVersionUID=20080517L;
		
		public JCheckBox ucBox;

		public JTextField prefixField;

		@Override
		public void initialize(){
			ucBox=new JCheckBox("uc");
			ucBox.addActionListener(this);
			add(ucBox);
			prefixField=new JTextField("the man: ");
			add(prefixField);
			addApplyButton();
		}

		@Override
		public String toString(){
			return "[uc: "+ucBox.isSelected()+" prefix: "+prefixField.getText()+"]";
		}
	}
}
