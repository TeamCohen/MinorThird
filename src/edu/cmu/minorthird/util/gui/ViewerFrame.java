package edu.cmu.minorthird.util.gui;

import edu.cmu.minorthird.util.IOUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Stack;


/**
 * 
 * Top-level container for a Viewer.
 * 
 * @author William cohen
 *
 */

public class ViewerFrame extends JFrame
{
	private Viewer myViewer = null;
	private String myName = null;
	private static class StackFrame {
		public Viewer view; 
		public String name;
		public StackFrame(String s,Viewer v) { this.view=v; this.name=s; }
	}
	private Stack history = new Stack();

	public ViewerFrame(String name,Viewer viewer)
	{
		super();
		pushContent(name,viewer);
		addMenu();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    pack();
		setVisible(true);
	}

	private void pushContent(String name,Viewer viewer)
	{
		if (myName!=null) history.push(new StackFrame(myName,myViewer));
		setContent(name,viewer);
	}
	private void popContent()
	{
		if (!history.empty()) {
			StackFrame sf = (StackFrame)history.pop();
			setContent(sf.name,sf.view);
		}
	}
	private void setContent(String name,Viewer viewer)
	{
		this.myName = name;
		this.myViewer = viewer;
		viewer.setPreferredSize(new java.awt.Dimension(800,600));
		getContentPane().removeAll();
		getContentPane().add(viewer, BorderLayout.CENTER);
		setTitle(name);
		myViewer.revalidate();
		//repaint();
	}

	private void addMenu()
	{
		// build a menu bar
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu menu = new JMenu("File");
		menuBar.add(menu);

		JMenuItem openItem = new JMenuItem("Open ...");
		menu.add(openItem);
		openItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					JFileChooser chooser = new JFileChooser();
					int returnVal = chooser.showOpenDialog(ViewerFrame.this);
					if (returnVal==JFileChooser.APPROVE_OPTION) {
						try {
							Object obj = IOUtil.loadSerialized(chooser.getSelectedFile());
							if (!(obj instanceof Visible)) {
								throw new RuntimeException(obj.getClass()+" is not Visible");
							}
							pushContent(obj.getClass().toString(), ((Visible)obj).toGUI());
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(
								ViewerFrame.this,
								"Error opening "+chooser.getSelectedFile().getName()+": "+ex,
								"Open File Error",
								JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			});


		JMenuItem saveItem = new JMenuItem("Save as ...");
		menu.add(saveItem);
		saveItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					Object content = myViewer.getContent();
					//System.out.println("saving object of type "+content.getClass());
					if (content instanceof Serializable) {
						JFileChooser chooser = new JFileChooser();
						int returnVal = chooser.showSaveDialog(ViewerFrame.this);
						if (returnVal==JFileChooser.APPROVE_OPTION) {
							try {
								IOUtil.saveSerialized((Serializable)content, chooser.getSelectedFile());
							} catch (Exception ex) {
								JOptionPane.showMessageDialog(
									ViewerFrame.this,
									"Error saving: "+ex,"Save File Error",JOptionPane.ERROR_MESSAGE);
							}
						}
					} else {
						JOptionPane.showMessageDialog(
							ViewerFrame.this,
							"You cannot save "+content.getClass(),"Serializability Error",JOptionPane.ERROR_MESSAGE);
					}
				}
			});

		JMenuItem exitItem = new JMenuItem("Close");
		menu.add(exitItem);
		exitItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					ViewerFrame.this.dispose();
				}
			});

		JMenu menu2 = new JMenu("Go");
		menuBar.add(menu2);
		JMenuItem backItem = new JMenuItem("Back");
		menu2.add(backItem);
		backItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					if (!history.empty()) {
						popContent();
					} else {
						JOptionPane.showMessageDialog(
							ViewerFrame.this,"You can't go back","Navigation Error",JOptionPane.ERROR_MESSAGE);
					}
				}
			});
		backItem.setEnabled(false); // this doesn't seem to work 

		JMenuItem zoomItem = new JMenuItem("Zoom..");
		menu2.add(zoomItem);
		zoomItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					final JDialog dialog = new JDialog(ViewerFrame.this,"Subpanes to zoom into",true);
					JPanel pane = new JPanel();
					dialog.getContentPane().add(pane);
					ButtonGroup group = new ButtonGroup();
					final JRadioButton[] buttons = new JRadioButton[myViewer.getSubViewNames().size()];
					int k=0;
					for (Iterator i=myViewer.getSubViewNames().iterator(); i.hasNext(); ) {
						String name = (String)i.next();
						JRadioButton button = new JRadioButton(name);
						pane.add(button);
						group.add(button);
						buttons[k++] = button;
					}
					JButton launchButton = new JButton(new AbstractAction("Zoom") {
							public void actionPerformed(ActionEvent ev) {
								for (int i=0; i<buttons.length; i++) {
									if (buttons[i].isSelected()) {
										final String name = buttons[i].getText();
										final Viewer subview = myViewer.getNamedSubView(buttons[i].getText());
										pushContent(myName+" / "+name,subview);
										dialog.dispose();
									}
								}
							}
						});
					pane.add(launchButton);
					JButton cancelButton = new JButton(new AbstractAction("Cancel") {
							public void actionPerformed(ActionEvent ev) {
								dialog.dispose();
							}
						});
					pane.add(cancelButton);
					dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
					dialog.pack();
					dialog.setLocationRelativeTo(ViewerFrame.this);
					dialog.setVisible(true);
				}
			});
	}

	public static void main(String[] args)
	{
		try {
			File file = new File(args[0]);
			Object obj = (Visible)IOUtil.loadSerialized(file);
			if (!(obj instanceof Visible)) {
				System.out.println("Not visible object: "+obj.getClass());
			}
			ViewerFrame f = new ViewerFrame(args[0], ((Visible)obj).toGUI());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("usage: ViewerFrame [serializedVisableObjectFile]");
		}
	}
}
