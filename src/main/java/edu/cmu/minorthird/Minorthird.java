package edu.cmu.minorthird;

import edu.cmu.minorthird.ui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * A launch bar for Minorthird applications.
 */

public class Minorthird extends JFrame
{

	static final long serialVersionUID=20071015;

	private String[] defaultArgs;

	public Minorthird(String[] args) 
	{
		super();

		// copy the args, adding 'gui'
		defaultArgs = new String[args.length+1];
		defaultArgs[0] = "-gui";
		for (int i=0; i<args.length; i++) {
			defaultArgs[i+1] = args[i];
		}

		// build the content panel
		initContent();

		// pop the launcher window
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);
	}
	private void initContent()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2,2));
		panel.setBorder(new TitledBorder("Applications to Launch"));

		JPanel tcPanel = new JPanel();
		tcPanel.setBorder(new TitledBorder("Classify Text"));
		addUIButton(tcPanel,"Expt", new TrainTestClassifier());
		addUIButton(tcPanel,"Train", new TrainClassifier());
		addUIButton(tcPanel,"Test", new TestClassifier());
		panel.add(tcPanel);

		JPanel txPanel = new JPanel();
		txPanel.setBorder(new TitledBorder("Extract From Text"));
		addUIButton(txPanel,"Expt", new TrainTestExtractor());
		addUIButton(txPanel,"Train", new TrainExtractor());
		addUIButton(txPanel,"Test", new TestExtractor());
		panel.add(txPanel);

		JPanel cPanel = new JPanel();
		cPanel.setBorder(new TitledBorder("Non-Text Data"));
		cPanel.add(new JButton(new AbstractAction("Expt/Train/Test") {
			static final long serialVersionUID=20071015;
			@Override
			public void actionPerformed(ActionEvent ev) {
				new edu.cmu.minorthird.classify.UI.DataClassificationTask().callMain(defaultArgs);
			}
		}));
		panel.add(cPanel);

		JPanel oPanel = new JPanel();
		oPanel.setBorder(new TitledBorder("Execute"));
		addUIButton(oPanel,"Mixup",new RunMixup());
		addUIButton(oPanel,"Annotator",new ApplyAnnotator());
		panel.add(oPanel);

		//addHelpPane(panel);

		panel.setPreferredSize(new java.awt.Dimension(800,200));
		getContentPane().removeAll();
		getContentPane().add(panel, BorderLayout.CENTER);
		setTitle("Minorthird LaunchPad");
		panel.revalidate();
	}
	private void addUIButton(final JPanel panel,final String tag,final UIMain m)
	{
		panel.add(new JButton(new AbstractAction(tag) {
			static final long serialVersionUID=20071015;
			@Override
			public void actionPerformed(ActionEvent ev) {
				m.callMain(defaultArgs);
			}
		}));
	}

	/*
	private void addHelpPane(JPanel panel) 
	{
		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditable(false);
		try {
			java.net.URL helpURL = new java.net.URL("http://wcohen.com/index.html");
			editorPane.setPage(helpURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		panel.add(new JScrollPane(editorPane));
	}
	 */

	static public void main(String[] args)
	{
		new Minorthird(args);
	}
}
