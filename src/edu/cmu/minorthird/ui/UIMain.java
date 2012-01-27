package edu.cmu.minorthird.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.gui.ComponentViewer;
import edu.cmu.minorthird.util.gui.Console;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.TypeSelector;
import edu.cmu.minorthird.util.gui.Viewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 * Main UI program. With enough support to make it configurable interactively,
 * by command lines, or by a file.
 * 
 */

public abstract class UIMain implements CommandLineProcessor.Configurable,
Console.Task{

	private JPanel errorPanel;

	public static JButton viewButton;

	public Console.Task main;

	//
	// some basic parameters and CommandLineProcessor items shared by everyone
	//

	protected CommandLineUtil.BaseParams base=new CommandLineUtil.BaseParams();

	protected CommandLineUtil.GUIParams gui=new CommandLineUtil.GUIParams();

	public CommandLineUtil.BaseParams getBaseParameters(){
		return base;
	}

	public void setBaseParameters(CommandLineUtil.BaseParams base){
		this.base=base;
	}

	public void helloWorld(){
		System.out.println("hello world");
	}

	/** Do the main action, after setting all parameters. */
	@Override
	abstract public void doMain();

	/** Return the result of the action. */
	@Override
	abstract public Object getMainResult();

	/** Returns whether base.labels exits */
	@Override
	public boolean getLabels(){
		return(base.labels!=null);
	}

	/** Helper to handle command-line processing, in either gui or text mode. */
	public void callMain(final String[] args){
		try{
			CommandLineProcessor clp=getCLP();
			clp.processArguments(args);
			if(clp.shouldTerminate()){
				return;
			}
			if(!gui.useGUI){
				if(base.labels==null)
					throw new IllegalArgumentException("-labels must be specified");
				if(base.showLabels)
					new ViewerFrame("Labeled TextBase",
							new SmartVanillaViewer(base.labels));
				doMain();
			}else{
				main=this;
				final Viewer v=new ComponentViewer(){

					static final long serialVersionUID=20071024L;

					@Override
					public JComponent componentFor(Object o){
						Viewer ts;
						if(base.classic)
							ts=
								new TypeSelector(SelectableTypes.CLASSES,
										"selectableTypes.txt",o.getClass());
						else
							ts=
								new TypeSelector(SelectableTypes.CLASSES,
										InLineSelectableTypes.CLASSES,AdvancedParameters.NAMES,
										"selectableTypes.txt",o.getClass());
						ts.setContent(o);

						// we'll put the type selector in a nice panel
						JPanel panel=new JPanel();
						panel.setBorder(new TitledBorder(StringUtil.toString(args,
								"Command line: ",""," ")));
						panel.setLayout(new GridBagLayout());

						GridBagConstraints gbc;
						// another panel to allow parameter modifications
						JPanel subpanel1=new JPanel();
						subpanel1.setBorder(new TitledBorder("Parameter modification"));
						subpanel1.add(ts);
						gbc=Viewer.fillerGBC();
						gbc.weighty=0;
						panel.add(subpanel1,gbc);
						// a control panel for controls
						JPanel subpanel2=new JPanel();
						subpanel2.setBorder(new TitledBorder("Execution controls"));
						// a button to show the results
						viewButton=new JButton(new AbstractAction("View results"){
							static final long serialVersionUID=20071024L;
							@Override
							public void actionPerformed(ActionEvent event){
								Viewer rv=new SmartVanillaViewer();
								rv.setContent(getMainResult());
								new ViewerFrame("Result",rv);
							}
						});
						viewButton.setEnabled(false);

						// another panel for error messages and other outputs

						errorPanel=new JPanel();
						errorPanel.setBorder(new TitledBorder("Error messages and output"));
						final Console console=
							new Console(main,base.labels!=null,viewButton);
						errorPanel.add(console.getMainComponent());

						// a button to start this thread
						JButton goButton=new JButton(new AbstractAction("Start Task"){
							static final long serialVersionUID=20071024L;
							@Override
							public void actionPerformed(ActionEvent event){
								console.start();
							}
						});

						// and a button to show the current labels
						JButton showLabelsButton=
							new JButton(new AbstractAction("Show labels"){
								static final long serialVersionUID=20071024L;
								@Override
								public void actionPerformed(ActionEvent ev){
									if(base.labels==null)
										noLabelsMessage(console);
									else
										new ViewerFrame("Labeled TextBase",
												new SmartVanillaViewer(base.labels));
								}
							});

						// and a button to clear the errorArea
						JButton clearButton=new JButton(new AbstractAction("Clear window"){
							static final long serialVersionUID=20071024L;
							@Override
							public void actionPerformed(ActionEvent ev){
								console.clear();
							}
						});

						// and a button for help
						JButton helpParamsButton=
							new JButton(new AbstractAction("Parameters"){
								static final long serialVersionUID=20071024L;
								@Override
								public void actionPerformed(ActionEvent ev){
									PrintStream oldSystemOut=System.out;
									ByteArrayOutputStream outBuffer=new ByteArrayOutputStream();
									System.setOut(new PrintStream(outBuffer));
									getCLP().usage();
									console.append(outBuffer.toString());
									System.setOut(oldSystemOut);
								}
							});

						// and another
						JButton helpRepositoryButton=
							new JButton(new AbstractAction("Repository"){
								static final long serialVersionUID=20071024L;
								@Override
								public void actionPerformed(ActionEvent ev){
									repositoryHelp(console);
								}
							});
						subpanel2.add(goButton);
						subpanel2.add(viewButton);
						subpanel2.add(showLabelsButton);
						subpanel2.add(clearButton);
						subpanel2.add(new JLabel("Help:"));
						subpanel2.add(helpParamsButton);
						subpanel2.add(helpRepositoryButton);
						gbc=Viewer.fillerGBC();
						gbc.weighty=0;
						gbc.gridy=1;
						panel.add(subpanel2,gbc);

						gbc=Viewer.fillerGBC();
						gbc.weighty=1;
						gbc.gridy=2;
						panel.add(errorPanel,gbc);

						// now some progress bars
						JProgressBar progressBar1=new JProgressBar();
						JProgressBar progressBar2=new JProgressBar();
						JProgressBar progressBar3=new JProgressBar();
						ProgressCounter.setGraphicContext(new JProgressBar[]{progressBar1,
								progressBar2,progressBar3});
						gbc=Viewer.fillerGBC();
						gbc.weighty=0;
						gbc.gridy=3;
						panel.add(progressBar1,gbc);
						gbc=Viewer.fillerGBC();
						gbc.weighty=0;
						gbc.gridy=4;
						panel.add(progressBar2,gbc);
						gbc=Viewer.fillerGBC();
						gbc.weighty=0;
						gbc.gridy=5;
						panel.add(progressBar3,gbc);

						return panel;
					}
				};
				v.setContent(this);
				String className=
					this.getClass().toString().substring("class ".length());
				ViewerFrame f=new ViewerFrame(className,v);
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Use option -help for help");
		}
	}

	public void noLabelsMessage(Console console){
		console.append("\nYou need to specify the labeled data you're using!\n"
				+"Modify the 'labels' parameters under base parameters section\n"
				+"of the parameter modification window.\n");
	}

	private void repositoryHelp(Console console){
		console
		.append("The Minorthird repository is a collection of data previously labeled for extraction\n"
				+"or classification learning. One version of the repository containing public data is on:\n"
				+"   /afs/cs/project/extract-learn/repository.\n"
				+"\n"
				+"Your repository is now configured as follows:\n"+"  "+
				FancyLoader.SCRIPTDIR_PROP+
				" => "+
				FancyLoader.getProperty(FancyLoader.SCRIPTDIR_PROP)+
				"\n"+
				"  "+
				FancyLoader.DATADIR_PROP+
				" => "+
				FancyLoader.getProperty(FancyLoader.DATADIR_PROP)+
				"\n"+
				"  "+
				FancyLoader.LABELDIR_PROP+
				" => "+
				FancyLoader.getProperty(FancyLoader.LABELDIR_PROP)+
				"\n"+
				"To change these parameters, put new values in a file \"data.properties\" on your classpath.\n"+
				"\n"+
				FancyLoader.SCRIPTDIR_PROP+
				" should contain bean shell scripts that return labeled datasets,\n"+
				"encoded as TextLabels objects. Usually these load documents from the directory pointed to by\n"+
				FancyLoader.DATADIR_PROP+
				" and load labels from the directory pointed to by\n"+
				FancyLoader.LABELDIR_PROP+
				"\n\n"+
				"Instead of using script names from repositories as the \"keys\" for the -labels options\n"+
				"you can also use the name of (a) a directory containing XML-marked up data (b) the common stem\n"+
				"\"foo\" of a pair of files foo.base and foo.labels or (c) the common stem of a pair foo.labels\n"+
		"and foo, where foo is a directory.\n");
	}
}
