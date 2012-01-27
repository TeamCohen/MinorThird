package edu.cmu.minorthird.util.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.log4j.Logger;

/**
 * Allows user to select among possible instantiations of a particular
 * type, and edit bean properties of these instantiations.
 *
 * Specifically, this lets the user recursively edit objects as
 * follows.  A "property" P of an object x is defined by the existence
 * two methods, a getter method <code>Type x.getP()</code> and a
 * setter method <code>x.setP(Type newValue).</code>.  Properties can
 * be edited by the user if <code>Type</code> is either
 * <code>boolean</code>, <code>int</code>, <code>double</code> or
 * <code>String</code>, or if <code>Type</code> is one of the
 * <code>validSubclasses</code> passed to the root constructor.
 *
 * <p>
 * Double-valued properties with names that contain the string
 * "Fraction" are visualized specially--it's assumed that their values
 * are between 0 and 1.0. String-valued properties with names that
 * contain "Filename" are also visualized specially.
 *
 * <p> If P is a String-valued property and a method
 * <code>x.getAllowedPValues()</code> exists, it will be used to
 * compute possible values for P.  The getAllowedPValues method should
 * return an Object array.
 * 
 */

public class TypeSelector extends ComponentViewer{

	static final long serialVersionUID=20080517L;

	static private boolean DEBUG=false;

	private static Logger log=Logger.getLogger(TypeSelector.class);

	private final Class<?> rootClass;

	private List<Class<?>> validSubclasses=new ArrayList<Class<?>>();

	private List<String> inLineClasses=new ArrayList<String>();

	private boolean inLine=false;

	private List<String> advancedParams=new ArrayList<String>();

	// maps classes to the objects which will be used to instantiate these classes
	private final Map<String,Object> instanceMap=new HashMap<String,Object>();

	private JComboBox classBox;

	private String name=null; // path of setters associated with this typeSelector

	public TypeSelector(Class<?>[] validSubclasses,Class<?> rootClass){
		this(validSubclasses,null,rootClass);
	}

	public TypeSelector(Class<?>[] validSubclasses,String configFilename,
			Class<?> rootClass){
		this(validSubclasses,null,null,configFilename,rootClass);
	}

	/**
	 * @param validSubclasses array of all classes that can be
	 * manipulated by (a) selecting them in a TypeSelector, and (b)
	 * editing their properties.  This array is inherited by all
	 * typeSelectors that are created, recursively, from this
	 * typeSelector.
	 * @param inLineClasses array of classes to be displayed inLine
	 * @param configFilename optional name of file containing names
	 * of additional classes to consider valid.  File may be on
	 * classpath.
	 * @param rootClass the class of objects that will be selected by
	 * this typeSelector.
	 */
	public TypeSelector(Class<?>[] validSubclasses,String[] inLineClasses,
			String[] advancedParams,String configFilename,Class<?> rootClass){
		this.validSubclasses.addAll(Arrays.asList(validSubclasses));
		if(inLineClasses!=null)
			this.inLineClasses.addAll(Arrays.asList(inLineClasses));
		else
			inLineClasses=null;
		if(advancedParams!=null){
			this.advancedParams.addAll(Arrays.asList(advancedParams));
		}else{
			advancedParams=null;
		}
		if(configFilename!=null)
			configureWith(configFilename);
		this.rootClass=rootClass;
		init(rootClass);
	}

//	/**
//	 * copies the given list as possible classes
//	 */
//	private TypeSelector(List<Class<?>> validClasses,Class rootClass){
//		this.validSubclasses=validClasses;
//		this.rootClass=rootClass;
//		init(rootClass);
//	}
	
	/**
	 * copies the given list as possible classes
	 */
	private TypeSelector(List<Class<?>> validClasses,List<String> inLineClasses,
			List<String> advancedParams,Class<?> rootClass,boolean inLine){
		this.validSubclasses=validClasses;
		this.inLineClasses=inLineClasses;
		this.advancedParams=advancedParams;
		this.rootClass=rootClass;
		this.inLine=inLine;
		init(rootClass);
	}

	/**
	 * Load class names from this file into the validClasses list.
	 */
	private void configureWith(String filename){
		try{
			LineNumberReader r=null;
			File file=new File(filename);
			if(file.exists())
				r=new LineNumberReader(new BufferedReader(new FileReader(file)));
			else{
				InputStream s=ClassLoader.getSystemResourceAsStream(filename);
				if(s==null)
					log.error("No file named '"+filename+"' found on classpath");
				else
					r=new LineNumberReader(new BufferedReader(new InputStreamReader(s)));
			}
			if(r!=null){
				String line;
				while((line=r.readLine())!=null){
					if(!line.startsWith("#")){
						try{
							validSubclasses.add(Class.forName(line));
						}catch(ClassNotFoundException ex){
							log.warn(filename+":"+r.getLineNumber()+": No class named '"+
									line+"' found.");
						}
					}
				}
			}
		}catch(IOException ex){
			log.error("Exception reading "+filename+": "+ex);
			return;
		}
	}

	private void init(Class<?> rootClass){
		for(int i=0;i<validSubclasses.size();i++){
			if(rootClass.isAssignableFrom((Class<?>)validSubclasses.get(i))){
				try{
					Object instance=((Class<?>)validSubclasses.get(i)).newInstance();
					instanceMap.put(
							shortenedClassName((Class<?>)(validSubclasses.get(i))),instance);
				}catch(InstantiationException ex){
					ex.printStackTrace();
					log.warn("can't create instance of "+
							(Class<?>)validSubclasses.get(i)+": "+ex);
				}catch(IllegalAccessException ex){
					log.warn("can't create instance of "+
							(Class<?>)validSubclasses.get(i)+": "+ex);
				}
			}
		}
	}

	/** Returns the shortened version of the class name */
	public String shortenedClassName(Class<?> item){
		String fullName=item.getName();
		String[] splitName=fullName.split("\\W");
		String className=splitName[splitName.length-1];
		return className;
	}

//	/**
//	 * add a new class to the list of valid sub classes.
//	 * exiting UI components are NOT updated (so make additions first)
//	 * @param subClass
//	 */
//	private void addClass(Class<?> subClass){
//		try{
//			validSubclasses.add(subClass);
//			instanceMap.put(shortenedClassName(subClass),subClass.newInstance());
//		}catch(InstantiationException e){
//			log.warn("can't create instance of "+subClass+": "+e);
//		}catch(IllegalAccessException e){
//			log.warn("can't create instance of "+subClass+": "+e);
//		}
//	}

//	/**
//	 * remove a class from the list of valid sub classes.
//	 * exiting UI components are NOT updated (so make changes first or update
//	 * the component seperately)
//	 * @param subClass
//	 */
//	private void removeClass(Class subClass){
//		validSubclasses.remove(shortenedClassName(subClass));
//		instanceMap.remove(shortenedClassName(subClass));
//	}
//
//	private void setInLine(boolean b){
//		inLine=b;
//	}

	@Override
	public JComponent componentFor(final Object o){
		if(inLine)
			return InLineComponent(o);
		else
			return BoxComponent(o);
	}

	public JComponent BoxComponent(final Object o){
		if(!rootClass.isAssignableFrom(o.getClass())){
			throw new IllegalArgumentException("not instance of "+rootClass+": "+o);
		}
		JPanel panel=new JPanel();
		// replace the default instance for the appropriate class with o
		if(instanceMap.get(shortenedClassName(o.getClass()))!=null){
			instanceMap.put(shortenedClassName(o.getClass()),o);
		}
		classBox=new JComboBox();
		for(Iterator<String> i=instanceMap.keySet().iterator();i.hasNext();){
			String key=i.next();
			log.debug("adding to classBox "+key);
			classBox.addItem(key);
		}
		String currentName=shortenedClassName(o.getClass());
		classBox.setSelectedItem(currentName);
		classBox.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent ev){
				sendSignal(OBJECT_SELECTED,instanceMap.get(classBox.getSelectedItem()));
			}
		});
		panel.add(classBox);

		if(DEBUG){
			// print button, for debugging
			panel.add(new JButton(new AbstractAction("Print"){
				static final long serialVersionUID=20080517L;
				@Override
				public void actionPerformed(ActionEvent e){
					System.out.println("current selection: "+
							instanceMap.get(classBox.getSelectedItem()));
				}
			}));
		}
		// edit button - pops up an editor for the currently selected type 
		panel.add(new JButton(new AbstractAction("Edit"){
			static final long serialVersionUID=20080517L;
			@Override
			public void actionPerformed(ActionEvent e){
				log.debug("pressed edit");
				PropertyEditor editor=new PropertyEditor(true,false);
				editor.setContent(instanceMap.get(classBox.getSelectedItem()));
				String title=name==null?"Property Editor":"Property Editor for "+name;
				//editor.setWarning();
				//JFrame popupFrame = new JFrame(title);
				JOptionPane optionPane=new JOptionPane(new Object[]{title,editor});
				JDialog dialog=optionPane.createDialog(TypeSelector.this,title);
				dialog.setVisible(true);
			}
		}));
		//Possible Help Button
		try{
			BeanInfo info=Introspector.getBeanInfo(o.getClass());
			PropertyDescriptor[] props=info.getPropertyDescriptors();
			for(int i=0;i<props.length;i++){
				log.debug("inspecting property: "+props[i].getShortDescription());
				final String pname=props[i].getDisplayName();
				//final Class<?> type=props[i].getPropertyType();
				final Method reader=props[i].getReadMethod();
				final String[] classes=o.getClass().getName().split("\\.");
				final String name=classes[classes.length-1];
				if(reader!=null&&pname.equalsIgnoreCase(name+"Help")){
					final Object value=reader.invoke(o,new Object[]{});
					panel.add(new JButton(new AbstractAction("?"){
						static final long serialVersionUID=20080517L;
						@Override
						public void actionPerformed(ActionEvent e){
							log.debug("pressed help");
							HelpViewer editor=new HelpViewer();
							editor.setContent(value);
							String title=
									pname==null?"Help":"Help for "+o.getClass().getName();
							//JFrame popupFrame = new JFrame(title);
							JOptionPane optionPane=new JOptionPane(new Object[]{editor});
							JDialog dialog=optionPane.createDialog(TypeSelector.this,title);
							dialog.setVisible(true);
						}
					}));
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Help does not work");
		}

		//panel.add(new JLabel("[Reminder: text will not be saved unless you use ENTER]"));
		//return (new JScrollPane(panel));
		return panel;
	}

	public JComponent InLineComponent(final Object o){
		if(!rootClass.isAssignableFrom(o.getClass())){
			throw new IllegalArgumentException("not instance of "+rootClass+": "+o);
		}
		JPanel panel=new JPanel();
		// replace the default instance for the appropriate class with o
		if(instanceMap.get(shortenedClassName(o.getClass()))!=null){
			instanceMap.put(shortenedClassName(o.getClass()),o);
		}
		classBox=new JComboBox();
		for(Iterator<String> i=instanceMap.keySet().iterator();i.hasNext();){
			String key=i.next();
			log.debug("adding to classBox "+key);
			classBox.addItem(key);
		}
		String currentName=shortenedClassName(o.getClass());
		classBox.setSelectedItem(currentName);
		classBox.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent ev){
				sendSignal(OBJECT_SELECTED,instanceMap.get(classBox.getSelectedItem()));
			}
		});
		if(instanceMap.size()>1){
			panel.add(classBox);
		}
		if(DEBUG){
			// print button, for debugging
			panel.add(new JButton(new AbstractAction("Print"){
				static final long serialVersionUID=20080517L;
				@Override
				public void actionPerformed(ActionEvent e){
					System.out.println("current selection: "+
							instanceMap.get(classBox.getSelectedItem()));
				}
			}));
		}
		// property editor displayed inLine

		PropertyEditor editor=new PropertyEditor();
		editor.setContent(instanceMap.get(classBox.getSelectedItem()));
		panel.add(editor);
		//return (new JScrollPane(panel));
		return panel;
	}

	public void createPE(Class<?> type){
		PropertyEditor editor=new PropertyEditor(true,true);
		editor.setContent(instanceMap.get(shortenedClassName(type)));
		String title=name==null?"Property Editor":"Property Editor for "+name;
		//JFrame popupFrame = new JFrame(title);
		JOptionPane optionPane=new JOptionPane(new Object[]{title,editor});
		JDialog dialog=optionPane.createDialog(TypeSelector.this,title);
		dialog.setVisible(true);
	}

	private boolean isInLine(String c){
		for(int i=0;i<inLineClasses.size();i++){
			if(c.equals(inLineClasses.get(i)))
				return true;
		}
		return false;
	}

	private boolean isAdvanced(String s){
		if(advancedParams==null)
			return false;
		for(int i=0;i<advancedParams.size();i++){
			if(s.equals(advancedParams.get(i)))
				return true;
		}
		return false;
	}

	public class HelpViewer extends ComponentViewer implements HyperlinkListener{
		static final long serialVersionUID=20080517L;
		@Override
		public JComponent componentFor(final Object o){
			try{
				JPanel helpPanel=new JPanel();
				JEditorPane ep=new JEditorPane();
				ep.setEditorKit(new HTMLEditorKit());
				ep.setText((String)o);
				ep.setEditable(false);
				ep.addHyperlinkListener(this);
				helpPanel.add(ep);
				return helpPanel;
			}catch(Exception e){
				e.printStackTrace();
				throw new IllegalArgumentException("Must give the help viewer a String");
			}
		}

		@Override
		public void hyperlinkUpdate(HyperlinkEvent he){
			if(he.getEventType()==HyperlinkEvent.EventType.ACTIVATED){

				try{
					String osName=System.getProperty("os.name");
					//String[] cmd=new String[5];
					String url=he.getURL().toString();

					if(osName.startsWith("Mac OS")){
						Class<?> fileMgr=Class.forName("com.apple.eio.FileManager");
						Method openURL=
								fileMgr.getDeclaredMethod("openURL",new Class[]{String.class});
						openURL.invoke(null,new Object[]{url});
					}else if(osName.startsWith("Windows")){
						Runtime.getRuntime().exec(
								"rundll32 url.dll,FileProtocolHandler "+url);
					}else{ //assume Unix or Linux 
						String[] browsers=
								{"firefox","opera","konqueror","epiphany","mozilla","netscape"};
						String browser=null;
						for(int count=0;count<browsers.length&&browser==null;count++)
							if(Runtime.getRuntime().exec(
									new String[]{"which",browsers[count]}).waitFor()==0)
								browser=browsers[count];
						if(browser==null)
							throw new Exception("Could not find web browser");
						else
							Runtime.getRuntime().exec(new String[]{browser,url});
					}
				}catch(Exception e){
					e.printStackTrace();
					System.out.println("Cannot open web page: "+e);
				}
			}
		}
	}

	//
	// inner class for editing properties
	//

	/**
	 * Allows properties of an object to be modified in a GUI.  For a
	 * property P to be modifiable, it must (a) have getP and setP
	 * methods defined, according to JavaBean conventions (b) have one
	 * of the types: int, String, boolean, double, or else have a type
	 * that is in the validSubclasses array.  Doubles with the substring
	 * Fraction in their name are treated specially.
	 */
	public class PropertyEditor extends ComponentViewer{
		
		static final long serialVersionUID=20080517L;
		
		boolean warning=false; //whether or not to print the warning message for TextFields

		boolean useAdvanced=false;

		public PropertyEditor(){
			super();
		}

		public PropertyEditor(boolean warn,boolean useAdv){
			super();
			warning=warn;
			useAdvanced=useAdv;
		}

		public class Selection{

			public Object itemSelected=null;

			public void setSelected(Object selected){
				itemSelected=selected;
			}
		}

		@Override
		public JComponent componentFor(final Object o){
			//      if (o instanceof Configurable) //custom property editor
			//        return ((Configurable)(o)).guiConfigure();

			//log.setLevel(Level.DEBUG);
			JPanel wholePanel=new JPanel();
			wholePanel.setLayout(new GridBagLayout());
			int numTextFields=0,numTypeSelectors=0,numAdvancedParams=0;
			int row=0;
			try{
				BeanInfo info=Introspector.getBeanInfo(o.getClass());
				PropertyDescriptor[] props=info.getPropertyDescriptors();
				int i=0;
				while(i<props.length){
					log.debug("inspecting property: "+props[i].getShortDescription());
					final String pname=props[i].getDisplayName();
					final Class<?> type=props[i].getPropertyType();
					final Method reader=props[i].getReadMethod();
					final Method writer=props[i].getWriteMethod();
					i++;
					final String pname2=i<props.length?props[i].getDisplayName():null;
					final Method reader2=i<props.length?props[i].getReadMethod():null;
					boolean isHelp;
					if(reader2!=null&&
							pname2!=null&&
							pname2.endsWith("Help")&&
							pname.charAt(0)==pname2.charAt(0)&&
							((!isAdvanced(pname2)&&!useAdvanced)||(useAdvanced&&isAdvanced(pname2)))){
						i++;
						isHelp=true;
					}else
						isHelp=false;
					final Selection selection=new Selection();

					JPanel panel=new JPanel();
					panel.setBorder(new EmptyBorder(0,0,0,0));
					//JButton helpButton=new JButton("?");
					if(reader!=null&&writer!=null){
						final Object value=reader.invoke(o,new Object[]{});
						if((!isAdvanced(pname)&&!useAdvanced)||
								(useAdvanced&&isAdvanced(pname))){
							// getter and setter methods exist - note the type of primitive objects
							// will be the corresponding wrapper class
							row++;
							// checks whether type should be displayed inLine
							if((isInLine(pname)))
								inLine=true;
							log.debug("reader: "+reader.getName());

							if(!(isInLine(pname)))
								panel.add(new JLabel(pname+":"),gbc(0,row));
							if(type.equals(String.class)){
								// configure an text input
								try{
									// method to find allowed values for property, eg
									// getAllowedSpanTypeValues for property spanType
									String allowedValueMethodName=
											"getAllowed"+pname.substring(0,1).toUpperCase()+
													pname.substring(1)+"Values";
									Method allowedValueMethod=
											o.getClass().getMethod(allowedValueMethodName,
													new Class[]{});
									//System.out.println("allowedValueMethod="+allowedValueMethod);
									Object[] allowedValues=
											(Object[])allowedValueMethod.invoke(o,new Object[]{});
									final JComboBox theBox=new JComboBox();
									theBox.addItem("-choose a value-");
									for(int j=0;j<allowedValues.length;j++){
										theBox.addItem(allowedValues[j]);
									}
									theBox.setSelectedItem(value);
									theBox.addActionListener(new ActionListener(){

										@Override
										public void actionPerformed(ActionEvent ev){
											try{
												if(theBox.getSelectedIndex()>0){
													writer.invoke(o,
															new Object[]{theBox.getSelectedItem()});
												}else{
													writer.invoke(o,new Object[]{null});
												}
											}catch(IllegalAccessException ex){
												log.error(ex.toString());
											}catch(InvocationTargetException ex){
												log.error(ex.toString());
											}
										}
									});
									panel.add(theBox,gbc(1,row));
								}catch(NoSuchMethodException ex){
									final JTextField textField=new JTextField(10);
									textField.setText(value==null?"":value.toString());
									textField.addActionListener(new ActionListener(){

										@Override
										public void actionPerformed(ActionEvent ev){
											try{
												log.debug("change to "+textField.getText());
												writer.invoke(o,new Object[]{textField.getText()});
											}catch(IllegalAccessException ex){
												log.error(ex.toString());
											}catch(InvocationTargetException ex){
												log.error(ex.toString());
											}
										}
									});

									if(pname.indexOf("Filename")<0){
										panel.add(textField,gbc(1,row));
									}else{
										// couple input field with a browse button
										final JFileChooser chooser=new JFileChooser();
										chooser
												.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
										JButton browseButton=
												new JButton(new AbstractAction("Browse"){
													static final long serialVersionUID=20080517L;
													@Override
													public void actionPerformed(ActionEvent ev){
														int returnVal=chooser.showOpenDialog(null);
														if(returnVal==JFileChooser.APPROVE_OPTION){
															String filename=
																	chooser.getSelectedFile().getAbsolutePath();
															textField.setText(filename);
															try{
																log.debug("change to "+textField.getText());
																writer.invoke(o,new Object[]{filename});
															}catch(IllegalAccessException ex){
																log.error(ex.toString());
															}catch(InvocationTargetException ex){
																log.error(ex.toString());
															}
														}
													}
												});
										JPanel typeOrBrowsePanel=new JPanel();
										typeOrBrowsePanel.add(textField);
										typeOrBrowsePanel.add(browseButton);
										panel.add(typeOrBrowsePanel,gbc(1,row));
									}
									numTextFields++;
								}

							}else if(type.equals(int.class)){
								// configure an int spinner
								final JSpinner spinner=
										new JSpinner(new SpinnerNumberModel(((Integer)value)
												.intValue(),-1,100,1));
								spinner.addChangeListener(new ChangeListener(){

									@Override
									public void stateChanged(ChangeEvent e){
										try{
											log.debug("change to "+spinner.getValue());
											writer.invoke(o,new Object[]{spinner.getValue()});
										}catch(IllegalAccessException ex){
										}catch(InvocationTargetException ex){
										}
									}
								});
								panel.add(spinner,gbc(1,row));
							}else if(type.equals(double.class)&&pname.indexOf("Fraction")>=0){
								// configure an double spinner from 0 - 1
								final JSpinner spinner=
										new JSpinner(new SpinnerNumberModel(((Double)value)
												.doubleValue(),0,100,0.05));
								spinner.addChangeListener(new ChangeListener(){

									@Override
									public void stateChanged(ChangeEvent e){
										try{
											log.debug("change to "+spinner.getValue());
											writer.invoke(o,new Object[]{spinner.getValue()});
										}catch(IllegalAccessException ex){
										}catch(InvocationTargetException ex){
										}
									}
								});
								panel.add(spinner,gbc(1,row,10));
							}else if(type.equals(double.class)){
								// configure an text input for doubles
								final JTextField textField=new JTextField(10);
								textField.setText(value.toString());
								textField.addActionListener(new ActionListener(){

									@Override
									public void actionPerformed(ActionEvent e){
										try{
											log.debug("change to "+textField.getText());
											double d=Double.parseDouble(textField.getText().trim());
											writer.invoke(o,new Object[]{new Double(d)});
										}catch(IllegalAccessException ex){
										}catch(InvocationTargetException ex){
										}catch(NumberFormatException ex){
											log.warn("Illegal number '"+textField.getText()+"'");
										}
									}
								});
								panel.add(textField,gbc(1,row));
								numTextFields++;
							}else if(type.equals(boolean.class)){
								// configure a checkbox
								final JCheckBox checkbox=new JCheckBox();
								checkbox.addActionListener(new ActionListener(){

									@Override
									public void actionPerformed(ActionEvent ev){
										try{
											log.debug("change to "+checkbox.isSelected());
											writer.invoke(o,new Object[]{new Boolean(checkbox
													.isSelected())});
										}catch(IllegalAccessException ex){
										}catch(InvocationTargetException ex){
										}
									}
								});
								checkbox.setSelected(((Boolean)value).booleanValue());
								panel.add(checkbox,gbc(1,row));
							}else if(value!=null&&isValid(value.getClass())){
								// configure a type selector for this class
								if(isInLine(pname))
									panel.setBorder(new TitledBorder(pname));
								log.debug("type "+value.getClass()+" is editable");
								log.debug("add selector on type "+type+" of "+validSubclasses);
								final TypeSelector selector=
										new TypeSelector(validSubclasses,inLineClasses,
												advancedParams,type,isInLine(pname));
								numTypeSelectors++;
								//selector.setInLine(isInLine(pname));
								selector.setContent(value);
								selector.name=name==null?pname:name+"."+pname;
								Object selected=
										selector.instanceMap.get(selector.classBox
												.getSelectedItem());
								selection.setSelected(selected);
								selector.classBox.addActionListener(new ActionListener(){

									@Override
									public void actionPerformed(ActionEvent e){
										try{
											Object selected=
													selector.instanceMap.get(selector.classBox
															.getSelectedItem());
											selection.setSelected(selected);
											writer.invoke(o,new Object[]{selected});
										}catch(IllegalAccessException ex){
										}catch(InvocationTargetException ex){
										}
									}
								});
								panel.add(selector,gbc(1,row));

							}else{
								log.debug("type "+type+" is not editable");
								panel.add(new JLabel(value==null?"null ":value.toString()),gbc(
										1,row));
							}
							//panel.add(new JLabel(type.toString()), gbc(1,row));
							log.debug("property "+row+"\n  name: "+name+"\n  type: "+type+
									"\n  value: "+value);
							if(value!=null)
								log.debug("class of value is: "+value.getClass());
							else
								log.debug("null value, no class");

						}else
							numAdvancedParams++;
						wholePanel.add(panel,gbc(1,row));
					}
					if(isHelp){
						final Object value=reader2.invoke(o,new Object[]{});
						panel.add(new JButton(new AbstractAction("?"){
							static final long serialVersionUID=20080517L;
							@Override
							public void actionPerformed(ActionEvent e){
								log.debug("pressed help");
								HelpViewer editor=new HelpViewer();
								String helpString=value.toString();
								if(selection.itemSelected!=null){
									String selectedItem=selection.itemSelected.toString();
									if(selectedItem.indexOf('@')>0){
										selectedItem=
												selectedItem.substring(0,selectedItem.indexOf('@'));
									}
									String javadocLink=selectedItem;
									javadocLink=javadocLink.replaceAll("\\.","\\/");
									if(javadocLink.indexOf('$')>0){
										javadocLink=javadocLink.replaceAll("\\$","\\.");
									}
									javadocLink=
											"http://minorthird.sourceforge.net/javadoc/"+javadocLink;
									helpString=
											helpString+"<br>Your current selection is:  <a href=\""+
													javadocLink+".html\">"+selectedItem+"</a>";
								}
								editor.setContent(helpString);
								String title=pname==null?"Help":"Help for "+pname;
								//JFrame popupFrame = new JFrame(title);
								JOptionPane optionPane=new JOptionPane(new Object[]{editor});
								JDialog dialog=optionPane.createDialog(TypeSelector.this,title);
								dialog.setVisible(true);
							}
						}),gbc(1,row));
					}
				} // for possible property
				if(numAdvancedParams>0&&inLine){
					JButton advancedOptions=new JButton("advanced options");
					advancedOptions.addActionListener(new ActionListener(){

						@Override
						public void actionPerformed(ActionEvent e){
							final TypeSelector selector=
									new TypeSelector(validSubclasses,inLineClasses,
											advancedParams,o.getClass(),false);
							selector.setContent(o);
							//selector.name = name==null ? pname : name+"."+pname;
							selector.classBox.addActionListener(new ActionListener(){

								@Override
								public void actionPerformed(ActionEvent e){
									//try {
//									Object selected=
//											selector.instanceMap.get(selector.classBox
//													.getSelectedItem());
									//writer.invoke(o, new Object[]{selected});
									//} catch (IllegalAccessException ex) {
									//}	catch (InvocationTargetException ex) {
									//}
								}
							});
							selector.createPE(o.getClass());
						}
					});
					row++;
					wholePanel.add(advancedOptions,gbc(1,row));
				}

				if(row==0){
					wholePanel.add(new JLabel("No properties to edit for class "+
							o.getClass()));
				}
				if(warning&&(numTypeSelectors>0||numTextFields>0)){
					wholePanel.add(new JLabel(
							"[Reminder: text will not be saved unless you use ENTER]"),gbc(0,
							row+1,2));
				}
			}catch(Exception e){
				e.printStackTrace();
				throw new IllegalArgumentException("Editor on input "+o+": "+
						e.toString());
			}

			JScrollPane scroller=new JScrollPane(wholePanel);
			scroller
					.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			return scroller;
			//wholePanel.setBorder(new EmptyBorder(0,0,0,0));
			//return wholePanel;
		}

		private GridBagConstraints gbc(int x,int y){
			GridBagConstraints gbc=new GridBagConstraints();
			gbc.insets=new Insets(5,5,5,5);
			gbc.weightx=gbc.weighty=1.0;
			gbc.gridx=x;
			gbc.gridy=y;
			gbc.ipadx=10;
			gbc.anchor=y==0?GridBagConstraints.EAST:GridBagConstraints.WEST;
			return gbc;
		}

		private GridBagConstraints gbc(int x,int y,int w){
			GridBagConstraints gbc=gbc(x,y);
			gbc.weightx=gbc.weighty=1.0;
			gbc.gridwidth=w;
			return gbc;
		}

		private boolean isValid(Class<?> c){
			for(int i=0;i<validSubclasses.size();i++){
				if(c.equals(validSubclasses.get(i)))
					return true;
			}
			return false;
		}

	}

	//
	// for testing
	//
	public static class AbstractSample{

		private int x=10;

		private double d=0.33;

		private String name="fooBar";

		private boolean flag=true;

		public String getName(){
			return name;
		}

		public void setName(String s){
			name=s;
		}

		public int getX(){
			return x;
		}

		public void setX(int x){
			this.x=x;
		}

		public double getDFraction(){
			return d;
		}

		public void setDFraction(double f){
			d=f;
		}

		public boolean getFlag(){
			return flag;
		}

		public void setFlag(boolean flag){
			this.flag=flag;
		}
	}

	public static class SampleOuter extends AbstractSample{

		private int y=0;

		private AbstractSample inner=new SampleInner();

		public int getY(){
			return y;
		}

		public void setY(int y){
			this.y=y;
		}

		public AbstractSample getInner(){
			return inner;
		}

		public void setInner(AbstractSample inner){
			this.inner=inner;
		}

		@Override
		public String toString(){
			return "[SampleOuter "+getName()+": flag,x,y,inner="+getFlag()+","+
					getX()+","+getY()+","+getInner()+"]";
		}
	}

	public static class SampleInner extends AbstractSample{

		private int z=0;

		public int getZ(){
			return z;
		}

		public void setZ(int z){
			this.z=z;
		}

		@Override
		public String toString(){
			return "[SampleInner "+getName()+": x,z,inner="+getX()+","+getZ()+"]";
		}
	}

	public static void main(String[] args){
		try{
			TypeSelector t=
					new TypeSelector(new Class[]{SampleInner.class,SampleOuter.class},
							AbstractSample.class);
			SampleOuter s=new SampleOuter();
			s.setName("fred");
			t.setContent(s);
			new ViewerFrame("test",t);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}

/*
 public class InLineTypeSelector extends TypeSelector
 {
 public JComponent componentFor(Object o) {
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
 log.debug("adding to classBox " + key);
 classBox.addItem( key );
 }
 classBox.setSelectedItem(o.getClass());
 classBox.addActionListener(new ActionListener() {
 public void actionPerformed(ActionEvent ev) {
 sendSignal(OBJECT_SELECTED,instanceMap.get(classBox.getSelectedItem()));
 }
 });
 if(instanceMap.size() > 1) {	
 panel.add(classBox);
 }
 if (DEBUG) {
 // print button, for debugging
 panel.add(new JButton(new AbstractAction("Print") {
 public void actionPerformed(ActionEvent e) {
 System.out.println("current selection: "+instanceMap.get(classBox.getSelectedItem()));
 }
 }));
 }
 // property editor displayed inLine

 PropertyEditor editor = new PropertyEditor();
 editor.setContent(instanceMap.get(classBox.getSelectedItem()));
 panel.add(editor);
 return (new JScrollPane(panel));
 }
 }
 */
