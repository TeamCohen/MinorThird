package edu.cmu.minorthird.text.gui;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** View results of executing a mixup program.
 *
 * @author William Cohen
 */

public class MixupDebugger extends JComponent
{
	private TextBaseEditor editor;
	public TextBaseEditor getEditor() { return editor; }

	public 
	MixupDebugger(TextBase base,File groundTruthEnvFile,File mixupProgramFile,boolean readOnly,boolean stem)
		throws IOException
    {
	this(base,new BasicTextEnv(base),groundTruthEnvFile,mixupProgramFile,readOnly,stem);
    }

	public 
	MixupDebugger(TextBase base,MonotonicTextEnv baseEnv,File groundTruthEnvFile,File mixupProgramFile,boolean readOnly,boolean stem)
		throws IOException
	{
		super();
		MutableTextEnv truthEnv = null;
		MonotonicTextEnv programEnv;
		MixupProgram program = new MixupProgram();
		if (groundTruthEnvFile!=null && groundTruthEnvFile.exists()) {
			System.out.println("loading textEnv from "+groundTruthEnvFile.getName()+"...");
			truthEnv = new BasicTextEnv(base);
			TextEnvLoader envLoader = new TextEnvLoader();
			envLoader.setClosurePolicy( TextEnvLoader.CLOSE_TYPES_IN_LABELED_DOCS );
			envLoader.importOps( truthEnv, base, groundTruthEnvFile );
		} else {
			if (truthEnv==null)	truthEnv = new BasicTextEnv(base);
		}
		if (stem) new BoneheadStemmer().stem(base,truthEnv);

		String errorString = "no mixup program specified";
		try {
			program = new MixupProgram(mixupProgramFile);
			errorString = "Loaded "+mixupProgramFile.getName();
		} catch (Exception e) {
			errorString = e.toString();
		}
		programEnv = new NestedTextEnv( new NestedTextEnv( baseEnv, truthEnv) );
		System.out.println("evaluating program from "+mixupProgramFile.getName()+"...");
		program.eval( programEnv, base );
		if (mixupProgramFile==null) throw new IllegalArgumentException("mixup program must be specified");

		StatusMessage statusMsg = new StatusMessage();
		JScrollPane errorPane = new JScrollPane(new JTextField(errorString));
		editor = new TextBaseEditor(base,programEnv,truthEnv,statusMsg,readOnly);
		JButton saveButton = null;
		if (groundTruthEnvFile!=null) {
			saveButton = new JButton(new SaveTruthEnvAction(
																 "Save current to "+groundTruthEnvFile.getName(),
																 groundTruthEnvFile, truthEnv, statusMsg));
		} else {
			saveButton = new JButton(new SaveTruthEnvAction(
																 "Save current to [no file specified]",
																 groundTruthEnvFile, truthEnv, statusMsg));
			saveButton.setEnabled( false );
		}
		editor.getViewerTracker().setSaveAs( groundTruthEnvFile );
		JButton refreshButton =
			new JButton(new RefreshProgramAction(
										"Reload program from "+mixupProgramFile.getName(),
										mixupProgramFile,base,programEnv,truthEnv,editor.getViewer(),errorPane));
		JTextField newTypeField = new JTextField(10);
		JButton newTypeButton = 
			new JButton(new NewTypeAction("New type:",truthEnv,editor.getViewer().getTruthBox(),newTypeField));

		//
		// layout stuff
		//
		setPreferredSize(new Dimension(800,600));
    setLayout(new GridBagLayout());

		JComponent top = new JPanel(new GridBagLayout());
		JComponent bottom = new JPanel(new GridBagLayout());
		GridBagConstraints gbc;

		int col = 0;
		int row = 0;

		// top panel
		++row;
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.gridx = ++col; gbc.gridy = row;
		top.add( refreshButton, gbc );

		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.gridx = ++col; gbc.gridy = row;
		top.add( newTypeButton, gbc );

		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.gridx = ++col; gbc.gridy = row;
		top.add( newTypeField, gbc );

		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.gridx = ++col; gbc.gridy = row;
		top.add( saveButton, gbc );		

		++row;
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0; gbc.weighty = 1.0;
		gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = col;
		top.add( errorPane, gbc );

		// bottom panel
		row = col = 0;

		++row;
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0; gbc.weighty = 1.0;
		gbc.gridx = 1; gbc.gridy = row; 
		bottom.add( editor, gbc );

		++row;
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0; gbc.weighty = 0.0;
		gbc.gridx = 1; gbc.gridy = row; 
		bottom.add( statusMsg, gbc );

		// a splitpane to size them

		top.setMinimumSize(new Dimension(100,50));
		bottom.setMinimumSize(new Dimension(100,100));
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,top,bottom);
		splitPane.setDividerLocation(50);
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0; gbc.weighty = 1.0;
		gbc.gridx = 1; gbc.gridy = 1;
		add( splitPane, gbc );
	}

	static private class SaveTruthEnvAction extends AbstractAction {
		private File saveFile;
		private MutableTextEnv env;
		private StatusMessage statusMsg;
		public SaveTruthEnvAction(String msg, File saveFile, MutableTextEnv env, StatusMessage statusMsg) {
			super(msg);	
			this.saveFile = saveFile;	
			this.env = env;
			this.statusMsg = statusMsg;
		}
		public void actionPerformed(ActionEvent event) {
			if (saveFile==null) {
				statusMsg.display("no save file specified?");
				return;
			}
			statusMsg.display("saving to "+saveFile.getName());
			try {
				new TextEnvLoader().saveTypesAsOps(env, saveFile );
				statusMsg.display("saved to "+saveFile.getName());
			} catch (Exception e) {
				statusMsg.display("can't save to "+saveFile.getName()+": "+e);				
			}
		}
	}

	static private class RefreshProgramAction extends AbstractAction {
		private File mixupFile;
		private TextEnv initProgramEnv;
		private TextEnv truthEnv;
		private TextBase base;
		private JScrollPane errorPane;
		private TextBaseViewer viewer;
		public RefreshProgramAction(
			String msg,
			File mixupFile,
			TextBase base,
			TextEnv initProgramEnv,
			TextEnv truthEnv,
			TextBaseViewer viewer,
			JScrollPane errorPane) 
		{
			super(msg);	
			this.mixupFile = mixupFile;
			this.base = base;
			this.initProgramEnv = initProgramEnv;
			this.truthEnv = truthEnv;
			this.viewer = viewer;
			this.errorPane = errorPane;
		}
		public void actionPerformed(ActionEvent event) {
			MixupProgram program = new MixupProgram();
			try {
				program = new MixupProgram(mixupFile);
			} catch (Exception e) {
				errorPane.getViewport().setView( new JTextField(e.toString()) );				
				return;
			}
			NestedTextEnv programEnv = new NestedTextEnv( truthEnv );
			program.eval( programEnv, base );
			viewer.updateTextEnv( programEnv );
			updateTypeBox( programEnv, viewer.getGuessBox() );
			updateTypeBox( programEnv, viewer.getTruthBox() );
			updateTypeBox( programEnv, viewer.getDisplayedTypeBox() );
			errorPane.getViewport().setView( new JTextField("loaded "+mixupFile.getName() ));
		}
	}

	static private void updateTypeBox(TextEnv env, JComboBox box) {
		Set oldTypes = new HashSet();
		for (int j=0; j<box.getItemCount(); j++) {
			oldTypes.add( box.getItemAt(j) );
		}
		for (Iterator i=env.getTypes().iterator(); i.hasNext(); ) {
			String t = (String)i.next();
			System.out.println("checking type "+t);
			if (!oldTypes.contains(t)) {
				box.addItem(t);
				System.out.println("adding type "+t);
			}
		}
	}

	private static class NewTypeAction extends AbstractAction {
		private Set truthTypeSet;
		private JComboBox truthBox;
		private JTextField newTypeField;
		public NewTypeAction(String msg,
												 MutableTextEnv truthEnv,
												 JComboBox truthBox,
												 JTextField newTypeField) 
		{
			super(msg);
			truthTypeSet = new HashSet();
			truthTypeSet.addAll( truthEnv.getTypes() );
			this.truthBox = truthBox;
			this.newTypeField = newTypeField;
		}
		public void actionPerformed(ActionEvent e) {
			String newType = newTypeField.getText().trim();
			if (newType.length()>0 && truthTypeSet.add(newType)) {
				truthBox.addItem( newType );
				truthBox.setSelectedItem( newType );
			}
		}
	}

	public static MixupDebugger debug(TextBase base,File groundTruthEnvFile,File mixupProgramFile)
    {
	MonotonicTextEnv baseEnv = new BasicTextEnv(base);
	return debug(base,baseEnv,groundTruthEnvFile,mixupProgramFile);
    }

	public static MixupDebugger debug(TextBase base,MonotonicTextEnv baseEnv,File groundTruthEnvFile,File mixupProgramFile)
	{
		try {
			JFrame frame = new JFrame("MixupDebugger");
			MixupDebugger debugger = new MixupDebugger(base,baseEnv,groundTruthEnvFile,mixupProgramFile,false,false);
			frame.getContentPane().add( debugger, BorderLayout.CENTER );
			frame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) { System.exit(0); }
				});
			frame.pack();
			frame.setVisible(true);
			return debugger;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) 
	{
		String textBaseId = null;
		File groundTruthEnvFile = null;
		File mixupProgramFile = null;
		String mainType = null;
		boolean readOnly = false, stem = false;

		// parse options
		if (args.length==0) {
			System.out.println(
				"usage: MixupDebugger -textBase <textBaseFile> -truth <truthEnvironment> -mixup <programFile> [options]");
			System.out.println(
				"       options: -readOnly -stem");   
			System.exit(0);
		}
		int argp = 0;
		while (argp<args.length) {
			if ("-textBase".equals(args[argp])) {
				textBaseId = args[++argp];
				++argp;
			} else if ("-truth".equals(args[argp])) {
				groundTruthEnvFile = new File(args[++argp]);
				++argp;
			} else if ("-mixup".equals(args[argp])) {
				mixupProgramFile = new File(args[++argp]);
				++argp;
			} else if ("-readOnly".equals(args[argp])) {
				readOnly = true;
				++argp;
			} else if ("-stem".equals(args[argp])) {
				stem = true;
				++argp;
			} else {
				System.out.println("illegal option "+args[argp]);
				++argp;
			}
		}

		try {

			JFrame frame = new JFrame("TextBaseEditor");
			TextBase base = FancyLoader.loadTextBase(textBaseId);
			MixupDebugger debugger = 
				new MixupDebugger(base,groundTruthEnvFile,mixupProgramFile,readOnly,stem);
			frame.getContentPane().add( debugger, BorderLayout.CENTER );
			frame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) { System.exit(0); }
				});
			frame.pack();
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
