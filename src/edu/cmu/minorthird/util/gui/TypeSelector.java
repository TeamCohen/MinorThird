package edu.cmu.minorthird.util.gui;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Allows user to select among possible instantiations of a particular
 * type.
 */

public class TypeSelector extends ComponentViewer
{
	static private boolean DEBUG = false;
	Logger log = Logger.getLogger(TypeSelector.class);

	private final Class[] validSubclasses;
	private final Class rootClass;
	// maps classes to the objects which will be used to instantiate these classes
	private final Map instanceMap;
	private JComboBox classBox;
	private String name = null;

	/**
	 * @param validSubclasses array of all classes that can be
	 * manipulated by (a) selecting them in a TypeSelector, and (b)
	 * editing their properties.  This array is inherited by all
	 * typeSelectors that are created, recursively, from this
	 * typeSelector.
	 * @param rootClass the class of objects that will be selected by
	 * this typeSelector.
	 */
	public TypeSelector(Class[] validSubclasses,Class rootClass)
	{
		this.validSubclasses = validSubclasses;
		this.rootClass = rootClass;
		this.instanceMap = new HashMap();
		for (int i=0; i<validSubclasses.length; i++) {
			if (rootClass.isAssignableFrom(validSubclasses[i])) {
				try {
					Object instance = validSubclasses[i].newInstance();
					instanceMap.put( validSubclasses[i], instance);
				} catch (InstantiationException ex) {
					log.warn("can't create instance of "+validSubclasses[i]+": "+ex);
				} catch (IllegalAccessException ex) {
					log.warn("can't create instance of "+validSubclasses[i]+": "+ex);
				}
			}
		}
	}

	public JComponent componentFor(final Object o)
	{
		if (!rootClass.isAssignableFrom(o.getClass())) {
			throw new IllegalArgumentException("not instance of "+rootClass+": "+o);
		}
		JPanel panel = new JPanel();
		// replace the default instance for the appropriate class with o
		if (instanceMap.get(o.getClass())!=null) {
			instanceMap.put(o.getClass(), o);
		}
		classBox = new JComboBox();
		for (Iterator i=instanceMap.keySet().iterator(); i.hasNext(); ) {
			Class key = (Class)i.next();
			classBox.addItem( key );
		}
		classBox.setSelectedItem(o.getClass());
		classBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					sendSignal(OBJECT_SELECTED,instanceMap.get(classBox.getSelectedItem()));
				}
			});
		panel.add(classBox);
		if (DEBUG) {
			// print button, for debugging
			panel.add(new JButton(new AbstractAction("Print") {
					public void actionPerformed(ActionEvent e) {
						System.out.println("current selection: "+instanceMap.get(classBox.getSelectedItem()));
					}
				}));
		}
		// edit button - pops up an editor for the currently selected type 
		panel.add(new JButton(new AbstractAction("Edit") {
				public void actionPerformed(ActionEvent e) {
					PropertyEditor editor = new PropertyEditor();
					editor.setContent(instanceMap.get(classBox.getSelectedItem()));
					String title = name==null ? "Property Editor" : "Property Editor for "+name;
					//JFrame popupFrame = new JFrame(title);
					JOptionPane optionPane = new JOptionPane(new Object[]{title,editor});
					JDialog dialog = optionPane.createDialog(TypeSelector.this,title);
					dialog.show();
				}
			}));
		return panel;
	}

	//
	// inner class for editing properties
	//

	/**
	 * Allows properties of an object to be modified in a GUI.  For a
	 * property P to be modifiable, it must (a) have getP and setP
	 * methods defined, according to JavaBean conventions (b) have one
	 * of the types: int, String, boolean, double, or else have a type
	 * that is in the validSubclasses array.  Doubles must additionally
	 * have the substring Fraction in their name.
	 */
	public class PropertyEditor extends ComponentViewer
	{
		public JComponent componentFor(final Object o) 
		{
			JPanel panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			int row=0;
			try {
				BeanInfo info = Introspector.getBeanInfo(o.getClass());
				PropertyDescriptor[] props = info.getPropertyDescriptors();
				for (int i=0; i<props.length; i++) {
          log.setLevel(Level.DEBUG);
          log.debug("inspecting property: " + props[i].getShortDescription());
					final String pname = props[i].getDisplayName();
					final Class type = props[i].getPropertyType();
					final Method reader = props[i].getReadMethod();
					final Method writer = props[i].getWriteMethod();
					if (reader!=null & writer!=null) {
						// getter and setter methods exist - note the type of primitive objects
						// will be the corresponding wrapper class
						row++;
						Object value = reader.invoke(o,new Object[]{});
						panel.add(new JLabel(pname+":"), gbc(0,row));
						if (type.equals(int.class)) {
							// configure an int spinner
							final JSpinner spinner = new JSpinner();
							spinner.setValue(value);
							spinner.addChangeListener(new ChangeListener() {
									public void stateChanged(ChangeEvent e) {
										try {
											log.debug("change to "+spinner.getValue());
											writer.invoke(o, new Object[]{spinner.getValue()});
										} catch (IllegalAccessException ex) {
										}	catch (InvocationTargetException ex) {
										}
									}
								});
							panel.add(spinner, gbc(1,row));
						} else if (type.equals(double.class) && pname.indexOf("Fraction")>=0) {
							// configure an double spinner from 0 - 1
							final JSpinner spinner = 
								new JSpinner(new SpinnerNumberModel(((Double)value).doubleValue(),0,1.0,0.05));
							spinner.addChangeListener(new ChangeListener() {
									public void stateChanged(ChangeEvent e) {
										try {
											log.debug("change to "+spinner.getValue());
											writer.invoke(o, new Object[]{spinner.getValue()});
										} catch (IllegalAccessException ex) {
										}	catch (InvocationTargetException ex) {
										}
									}
								});
							panel.add(spinner, gbc(1,row));
						} else if (type.equals(double.class)) {
							// configure an text input for doubles
							final JTextField textField = new JTextField(10);
							textField.setText(value.toString());
							textField.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										try {
											log.debug("change to "+textField.getText());
											double d = Double.parseDouble(textField.getText().trim());
											writer.invoke(o, new Object[]{new Double(d)});
										} catch (IllegalAccessException ex) {
										}	catch (InvocationTargetException ex) {
										}	catch (NumberFormatException ex) {
											log.warn("Illegal number '"+textField.getText()+"'");
										}
									}
								});
							panel.add(textField, gbc(1,row));
						} else if (type.equals(String.class)) {
							// configure an text input
							final JTextField textField = new JTextField();
							textField.setText(value.toString());
							textField.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ev) {
										try {
											log.debug("change to "+textField.getText());
											writer.invoke(o, new Object[]{textField.getText()});
										} catch (IllegalAccessException ex) {
										}	catch (InvocationTargetException ex) {
										}
									}
								});
							panel.add(textField, gbc(1,row));
						} else if (type.equals(boolean.class)) {
							// configure a checkbox
							final JCheckBox checkbox = new JCheckBox();
							checkbox.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent ev) {
										try {
											log.debug("change to "+checkbox.isSelected());
											writer.invoke(o, new Object[]{new Boolean(checkbox.isSelected())});
										} catch (IllegalAccessException ex) {
										}	catch (InvocationTargetException ex) {
										}
									}
								});
							checkbox.setSelected(((Boolean)value).booleanValue());
							panel.add(checkbox, gbc(1,row));
						} else if (isValid(value.getClass())) {
							// configure a type selector for this class
							log.debug("type "+value.getClass()+" is editable");
							final TypeSelector selector = new TypeSelector(validSubclasses,type);
							selector.setContent(value);
							selector.name = name==null ? pname : name+"."+pname;
							selector.classBox.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										try {
											Object selected = selector.instanceMap.get(selector.classBox.getSelectedItem());
											writer.invoke(o, new Object[]{selected});
										} catch (IllegalAccessException ex) {
										}	catch (InvocationTargetException ex) {
										}
									}
								});
							panel.add(selector, gbc(1,row));
						} else {
							log.debug("type "+type+" is not editable");
							panel.add(new JLabel(value==null ? "null " : value.toString()), gbc(2,row));						
						}
						//panel.add(new JLabel(type.toString()), gbc(1,row));
						log.debug("property "+row+"\n  name: "+name+"\n  type: "+type+"\n  value: "+value);
						log.debug("class of value is: "+value.getClass());
					}
				} // for possible property
				if (row==0) {
					panel.add(new JLabel("No properties to edit for class "+o.getClass()));
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Editor on input "+o+": "+e.toString());
			}
			JScrollPane scroller = new JScrollPane(panel);
			scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			return scroller;
		}
		private GridBagConstraints gbc(int x,int y)
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(5,5,5,5);
			gbc.weightx = gbc.weighty = 1.0;
			gbc.gridx = x; gbc.gridy = y;
			gbc.ipadx = 10; 
			gbc.anchor = y==0 ? GridBagConstraints.EAST : GridBagConstraints.WEST;
			return gbc;
		}
		private GridBagConstraints gbc(int x,int y,int w)
		{
			GridBagConstraints gbc = gbc(x,y);
			gbc.weightx = gbc.weighty = 1.0;
			gbc.gridwidth = w;
			return gbc;
		}	
		private boolean isValid(Class c)
		{
			for (int i=0; i<validSubclasses.length; i++) {
				if (c.equals(validSubclasses[i])) return true;
			}
			return false;
		}
	}
	

	//
	// for testing
	//
	public static class AbstractSample {
		private int x=10;
		private double d=0.33;
		private String name="fooBar";
		private boolean flag=true;
		public String getName() { return name; }
		public void setName(String s) { name=s; }
		public int getX() { return x; }
		public void setX(int x) { this.x = x; }
		public double getDFraction() { return d; }
		public void setDFraction(double f) { d=f; }
		public boolean getFlag() { return flag; }
		public void setFlag(boolean flag) { this.flag = flag; }
	}
	public static class SampleOuter extends AbstractSample {
		private int y=0;
		private AbstractSample inner = new SampleInner();
		public int getY() { return y; }
		public void setY(int y) { this.y = y; }
		public AbstractSample getInner() { return inner; }
		public void setInner(AbstractSample inner) { this.inner = inner; }
		public String toString() { 
			return "[SampleOuter "+getName()+": flag,x,y,inner="+getFlag()+","+getX()+","+getY()+","+getInner()+"]";
		}
	}
	public static class SampleInner extends AbstractSample {
		private int z=0;
		public int getZ() { return z; }
		public void setZ(int z) { this.z = z; }
		public String toString() { 
			return "[SampleInner "+getName()+": x,z,inner="+getX()+","+getZ()+"]";
		}
	}

	public static void main(String[] args) 
	{
		try {
			TypeSelector t = new TypeSelector(new Class[]{SampleInner.class,SampleOuter.class},AbstractSample.class);
			SampleOuter s = new SampleOuter();
			s.setName("fred");
			t.setContent(s);
			ViewerFrame f = new ViewerFrame("test", t);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
