package edu.cmu.minorthird.util.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.Saveable;
import edu.cmu.minorthird.util.StringUtil;

/**
 * 
 * Top-level container for a Viewer.
 * 
 * @author William cohen
 *
 */

public class ViewerFrame extends JFrame{

	static final long serialVersionUID=20080517L;
	
	static private Logger log=Logger.getLogger(ViewerFrame.class);

	private Viewer myViewer=null;

	private String myName=null;

	private JMenuItem saveItem=null,zoomItem=null,openItem=null;

	private Object content=null;

	private ContentWrapper wrapper=null;

//	private static class StackFrame{
//		
//		public Viewer view;
//
//		public String name;
//
//		public StackFrame(String s,Viewer v){
//			this.view=v;
//			this.name=s;
//		}
//	}

	public ViewerFrame(String name,Viewer viewer){
		super();
		addMenu();
		setContent(name,viewer);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);
	}

	private void setContent(String name,Viewer viewer){
		this.myName=name;
		this.myViewer=viewer;
		this.content=myViewer.getSerializableContent();
		this.wrapper=new ContentWrapper(content);
		viewer.setPreferredSize(new java.awt.Dimension(800,600));
		getContentPane().removeAll();
		getContentPane().add(viewer,BorderLayout.CENTER);
		setTitle(name);
		myViewer.revalidate();
		zoomItem.setEnabled(myViewer.getSubViewNames().size()>0);
		saveItem.setEnabled(wrapper.isSaveable());
		openItem.setEnabled(wrapper.isSaveable());
		//repaint();
	}

	private void addMenu(){
		// build a menu bar
		JMenuBar menuBar=new JMenuBar();
		setJMenuBar(menuBar);
		JMenu menu=new JMenu("File");
		menuBar.add(menu);

		openItem=new JMenuItem("Open ...");
		menu.add(openItem);
		openItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent ev){
				JFileChooser chooser=wrapper.makeFileChooser(wrapper);
				int returnVal=chooser.showOpenDialog(ViewerFrame.this);
				if(returnVal==JFileChooser.APPROVE_OPTION){
					try{
						Object obj=wrapper.restore(chooser.getSelectedFile());
						if(!(obj instanceof Visible)){
							throw new RuntimeException(obj.getClass()+" is not Visible");
						}
						setContent(obj.getClass().toString(),((Visible)obj).toGUI());
					}catch(Exception ex){
						JOptionPane.showMessageDialog(ViewerFrame.this,"Error opening "+
								chooser.getSelectedFile().getName()+": "+ex,"Open File Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		saveItem=new JMenuItem("Save as ...");
		menu.add(saveItem);
		saveItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent ev){
				if(wrapper.isSaveable()){
					log.debug("Wrapper is saveable");
					JFileChooser chooser=wrapper.makeFileChooser(wrapper);
					int returnVal=chooser.showSaveDialog(ViewerFrame.this);
					if(returnVal==JFileChooser.APPROVE_OPTION){
						try{
							FileFilter filter=chooser.getFileFilter();
							String fmt=filter.getDescription();
							String ext=wrapper.getExtensionFor(fmt);
							File file0=chooser.getSelectedFile();
							File file=
									(file0.getName().endsWith(ext))?file0:new File(file0
											.getParentFile(),file0.getName()+ext);
							wrapper.saveAs(file,filter.getDescription());
						}catch(Exception ex){
							JOptionPane.showMessageDialog(ViewerFrame.this,"Error saving: "+
									ex,"Save File Error",JOptionPane.ERROR_MESSAGE);
						}
					}
				}else{
					JOptionPane.showMessageDialog(ViewerFrame.this,"You cannot save "+
							content.getClass(),"Error",JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		JMenuItem exitItem=new JMenuItem("Close");
		menu.add(exitItem);
		exitItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent ev){
				ViewerFrame.this.dispose();
			}
		});

		JMenu menu2=new JMenu("Go");
		menuBar.add(menu2);

		zoomItem=new JMenuItem("Zoom..");
		menu2.add(zoomItem);
		zoomItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent ev){
				final JDialog dialog=
						new JDialog(ViewerFrame.this,"Subpanes to zoom into",true);
				JPanel pane=new JPanel();
				dialog.getContentPane().add(pane);
				ButtonGroup group=new ButtonGroup();
				final JRadioButton[] buttons=
						new JRadioButton[myViewer.getSubViewNames().size()];
				int k=0;
				for(Iterator<String> i=myViewer.getSubViewNames().iterator();i.hasNext();){
					String name=i.next();
					JRadioButton button=new JRadioButton(name);
					pane.add(button);
					group.add(button);
					buttons[k++]=button;
				}
				JButton launchButton=new JButton(new AbstractAction("Zoom"){
					static final long serialVersionUID=20080517L;
					@Override
					public void actionPerformed(ActionEvent ev){
						for(int i=0;i<buttons.length;i++){
							if(buttons[i].isSelected()){
								final String name=buttons[i].getText();
								final Viewer subview=
										myViewer.getNamedSubView(buttons[i].getText());
								setContent(myName+" / "+name,subview);
								dialog.dispose();
							}
						}
					}
				});
				pane.add(launchButton);
				JButton cancelButton=new JButton(new AbstractAction("Cancel"){
					static final long serialVersionUID=20080517L;
					@Override
					public void actionPerformed(ActionEvent ev){
						dialog.dispose();
					}
				});
				pane.add(cancelButton);
				dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				dialog.pack();
				dialog.setLocationRelativeTo(ViewerFrame.this);
				dialog.setVisible(true);
			}
		});
	}

	public static void main(String[] args){
		try{
			File file=new File(args[0]);
			Object obj=IOUtil.loadSerialized(file);
			if(!(obj instanceof Visible)){
				System.out.println("Not visible object: "+obj.getClass());
			}
			new ViewerFrame(args[0],((Visible)obj).toGUI());
		}catch(Exception ex){
			ex.printStackTrace();
			System.out.println("usage: ViewerFrame [serializedVisableObjectFile]");
		}
	}

	private static class ContentWrapper implements Saveable{

		private Object obj;

		private String[] formats;

		public static final String SERIALIZED_FORMAT_NAME="Serialized Java Object";

		public static final String SERIALIZED_EXT=".serialized";

		public ContentWrapper(Object obj){
			this.obj=obj;
			int n=0;
			if(obj instanceof Serializable){
				n=1;
			}
			if(obj instanceof Saveable){
				String[] fs=((Saveable)obj).getFormatNames();
				formats=new String[fs.length+n];
				for(int i=0;i<fs.length;i++)
					formats[i+n]=fs[i];
			}else{
				formats=new String[n];
			}
			if(obj instanceof Serializable){
				formats[0]=SERIALIZED_FORMAT_NAME;
			}
			log.debug("ContentWrapper for "+obj.getClass()+" has "+formats.length+
					" save format(s): "+StringUtil.toString(formats));
		}

		public boolean isSaveable(){
			return formats.length>0;
		}

		@Override
		public String[] getFormatNames(){
			return formats;
		}

		@Override
		public String getExtensionFor(String formatName){
			if(formatName.equals(SERIALIZED_FORMAT_NAME))
				return SERIALIZED_EXT;
			else if(obj instanceof Saveable)
				return ((Saveable)obj).getExtensionFor(formatName);
			else
				return null;
		}

		public FileFilter getFilter(int i){
			final String ext=getExtensionFor(formats[i]);
			final String fmt=formats[i];
			return new FileFilter(){

				@Override
				public boolean accept(File f){
					return f.isDirectory()||f.getName().endsWith(ext);
				}

				@Override
				public String getDescription(){
					return fmt;
				}
			};
		}

		@Override
		public void saveAs(File file,String formatName) throws IOException{
			if(!isSaveable())
				throw new IllegalArgumentException("can't save "+obj);
			if(SERIALIZED_FORMAT_NAME.equals(formatName))
				IOUtil.saveSerialized((Serializable)obj,file);
			else
				((Saveable)obj).saveAs(file,formatName);
		}

		@Override
		public Object restore(File file) throws IOException{
			if(!isSaveable())
				throw new IllegalArgumentException("can't restore something like "+obj);
			if(file.getName().endsWith(SERIALIZED_EXT)){
				return IOUtil.loadSerialized(file);
			}else{
				return ((Saveable)obj).restore(file);
			}
		}

		public JFileChooser makeFileChooser(ContentWrapper w){
			JFileChooser chooser=new JFileChooser();
			for(int i=0;i<formats.length;i++){
				chooser.addChoosableFileFilter(w.getFilter(i));
			}
			return chooser;
		}
	}
}
