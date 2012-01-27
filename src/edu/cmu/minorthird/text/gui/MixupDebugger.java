package edu.cmu.minorthird.text.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.BoneheadStemmer;
import edu.cmu.minorthird.text.FancyLoader;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextBaseLoader;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * View results of executing a mixup program.
 * 
 * @author William Cohen
 */

public class MixupDebugger extends JComponent{

	static final long serialVersionUID=20080306L;

	private TextBaseEditor editor;

	private Logger log=Logger.getLogger(this.getClass());

	public TextBaseEditor getEditor(){
		return editor;
	}

	public MixupDebugger(TextBase base,File groundTruthLabelsFile,
			File mixupProgramFile,boolean readOnly,boolean stem) throws IOException{
		this(base,new BasicTextLabels(base),groundTruthLabelsFile,mixupProgramFile,
				readOnly,stem);
	}

	public MixupDebugger(TextBase base,TextLabels baseLabels,
			File groundTruthLabelsFile,File mixupProgramFile,boolean readOnly,
			boolean stem) throws IOException{
		super();
		if(mixupProgramFile==null){
			throw new IllegalArgumentException("mixup program must be specified");
		}
		MutableTextLabels truthLabels=null;
		MonotonicTextLabels programLabels;
		MixupProgram program=new MixupProgram();
		if(groundTruthLabelsFile!=null&&groundTruthLabelsFile.exists()){
			log
					.info("loading textLabels from "+groundTruthLabelsFile.getName()+
							"...");
			truthLabels=new BasicTextLabels(base);
			TextLabelsLoader labelsLoader=new TextLabelsLoader();
			labelsLoader
					.setClosurePolicy(TextLabelsLoader.CLOSE_TYPES_IN_LABELED_DOCS);
			labelsLoader.importOps(truthLabels,base,groundTruthLabelsFile);
		}else{
			if(truthLabels==null)
				truthLabels=new BasicTextLabels(base);
		}
		if(stem)
			new BoneheadStemmer().stem(base,truthLabels);

		String errorString="no mixup program specified";
		try{
			program=new MixupProgram(mixupProgramFile);
			errorString="Loaded "+mixupProgramFile.getName();
		}catch(Exception e){
			errorString=e.toString();
		}
		programLabels=
				new NestedTextLabels(new NestedTextLabels(baseLabels),truthLabels);
		System.out.println("evaluating program from "+mixupProgramFile.getName()+
				"...");
		MixupInterpreter interp=new MixupInterpreter(program);
		interp.eval(programLabels);
		new TextLabelsLoader()
				.saveTypesAsOps(programLabels,new File("test.labels"));
		StatusMessage statusMsg=new StatusMessage();
		JScrollPane errorPane=new JScrollPane(new JTextField(errorString));
		editor=
				new TextBaseEditor(base,programLabels,truthLabels,statusMsg,readOnly);
		JButton saveButton=null;
		if(groundTruthLabelsFile!=null){
			saveButton=
					new JButton(new SaveTruthLabelsAction("Save current to "+
							groundTruthLabelsFile.getName(),groundTruthLabelsFile,
							truthLabels,statusMsg));
		}else{
			saveButton=
					new JButton(new SaveTruthLabelsAction(
							"Save current to [no file specified]",groundTruthLabelsFile,
							truthLabels,statusMsg));
			saveButton.setEnabled(false);
		}
		editor.getViewerTracker().setSaveAs(groundTruthLabelsFile);
		JButton refreshButton=
				new JButton(new RefreshProgramAction("Reload program from "+
						mixupProgramFile.getName(),mixupProgramFile,base,baseLabels,
						truthLabels,editor.getViewer(),errorPane));
		JTextField newTypeField=new JTextField(10);
		JButton newTypeButton=
				new JButton(new NewTypeAction("New type:",truthLabels,editor
						.getViewer().getTruthBox(),newTypeField));

		//
		// layout stuff
		//
		setPreferredSize(new Dimension(800,600));
		setLayout(new GridBagLayout());

		JComponent top=new JPanel(new GridBagLayout());
		JComponent bottom=new JPanel(new GridBagLayout());
		GridBagConstraints gbc;

		int col=0;
		int row=0;

		// top panel
		++row;
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=1.0;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=row;
		top.add(refreshButton,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=1.0;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=row;
		top.add(newTypeButton,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=1.0;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=row;
		top.add(newTypeField,gbc);

		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=1.0;
		gbc.weighty=0.0;
		gbc.gridx=++col;
		gbc.gridy=row;
		top.add(saveButton,gbc);

		++row;
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.gridx=1;
		gbc.gridy=row;
		gbc.gridwidth=col;
		top.add(errorPane,gbc);

		// bottom panel
		row=col=0;

		++row;
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.gridx=1;
		gbc.gridy=row;
		bottom.add(editor,gbc);

		++row;
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=1.0;
		gbc.weighty=0.0;
		gbc.gridx=1;
		gbc.gridy=row;
		bottom.add(statusMsg,gbc);

		// a splitpane to size them

		top.setMinimumSize(new Dimension(100,50));
		bottom.setMinimumSize(new Dimension(100,100));
		JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT,top,bottom);
		splitPane.setDividerLocation(50);
		gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.gridx=1;
		gbc.gridy=1;
		add(splitPane,gbc);
	}

	static private class SaveTruthLabelsAction extends AbstractAction{

		static final long serialVersionUID=20080306L;

		private File saveFile;

		private MutableTextLabels labels;

		private StatusMessage statusMsg;

		public SaveTruthLabelsAction(String msg,File saveFile,
				MutableTextLabels labels,StatusMessage statusMsg){
			super(msg);
			this.saveFile=saveFile;
			this.labels=labels;
			this.statusMsg=statusMsg;
		}

		@Override
		public void actionPerformed(ActionEvent event){
			if(saveFile==null){
				statusMsg.display("no save file specified?");
				return;
			}
			statusMsg.display("saving to "+saveFile.getName());
			try{
				new TextLabelsLoader().saveTypesAsOps(labels,saveFile);
				statusMsg.display("saved to "+saveFile.getName());
			}catch(Exception e){
				statusMsg.display("can't save to "+saveFile.getName()+": "+e);
			}
		}
	}

	private static class RefreshProgramAction extends AbstractAction{

		static final long serialVersionUID=20080306L;

		private File mixupFile;

		private TextLabels initProgramLabels;

		private TextLabels truthLabels;

		//private TextBase base;

		private JScrollPane errorPane;

		private TextBaseViewer viewer;

		public RefreshProgramAction(String msg,File mixupFile,TextBase base,
				TextLabels initProgramLabels,TextLabels truthLabels,
				TextBaseViewer viewer,JScrollPane errorPane){
			super(msg);
			this.mixupFile=mixupFile;
			//this.base=base;
			this.initProgramLabels=initProgramLabels;
			this.truthLabels=truthLabels;
			this.viewer=viewer;
			this.errorPane=errorPane;
		}	
		
		/*
		public TextBase getBase(){
			return base;
		}
		
		public void setBase(TextBase base){
			this.base=base;
		}
		*/

		@Override
		public void actionPerformed(ActionEvent event){
			MixupProgram program=new MixupProgram();
			try{
				program=new MixupProgram(mixupFile);
			}catch(Exception e){
				errorPane.getViewport().setView(new JTextField(e.toString()));
				return;
			}
			// NestedTextLabels programLabels = new NestedTextLabels( truthLabels );
			MonotonicTextLabels programLabels=
					new NestedTextLabels(new NestedTextLabels(initProgramLabels),
							truthLabels);
			try{
				MixupInterpreter interp=new MixupInterpreter(program);
				interp.eval(programLabels);
			}catch(Exception e){
				errorPane.getViewport().setView(new JTextField(e.toString()));
				return;
			}
			viewer.updateTextLabels(programLabels);
			updateTypeBox(programLabels,viewer.getGuessBox());
			updateTypeBox(programLabels,viewer.getTruthBox());
			updateTypeBox(programLabels,viewer.getDisplayedTypeBox());
			errorPane.getViewport().setView(
					new JTextField("loaded "+mixupFile.getName()));
		}
	}

	static private void updateTypeBox(TextLabels labels,JComboBox box){
		Set<String> oldTypes=new HashSet<String>();
		for(int j=0;j<box.getItemCount();j++){
			oldTypes.add((String)box.getItemAt(j));
		}
		for(Iterator<String> i=labels.getTypes().iterator();i.hasNext();){
			String t=i.next();
			System.out.println("checking type "+t);
			if(!oldTypes.contains(t)){
				box.addItem(t);
				System.out.println("adding type "+t);
			}
		}
	}

	private static class NewTypeAction extends AbstractAction{

		static final long serialVersionUID=20080306L;
		
		private Set<String> truthTypeSet;

		private JComboBox truthBox;

		private JTextField newTypeField;

		public NewTypeAction(String msg,MutableTextLabels truthLabels,
				JComboBox truthBox,JTextField newTypeField){
			super(msg);
			truthTypeSet=new HashSet<String>();
			truthTypeSet.addAll(truthLabels.getTypes());
			this.truthBox=truthBox;
			this.newTypeField=newTypeField;
		}

		@Override
		public void actionPerformed(ActionEvent e){
			String newType=newTypeField.getText().trim();
			if(newType.length()>0&&truthTypeSet.add(newType)){
				truthBox.addItem(newType);
				truthBox.setSelectedItem(newType);
			}
		}
	}

	public static MixupDebugger debug(TextBase base,File groundTruthLabelsFile,
			File mixupProgramFile){
		MonotonicTextLabels baseLabels=new BasicTextLabels(base);
		return debug(base,baseLabels,groundTruthLabelsFile,mixupProgramFile);
	}

	public static MixupDebugger debug(TextBase base,TextLabels baseLabels,
			File groundTruthLabelsFile,File mixupProgramFile){
		try{
			JFrame frame=new JFrame("MixupDebugger");
			MixupDebugger debugger=
					new MixupDebugger(base,baseLabels,groundTruthLabelsFile,
							mixupProgramFile,false,false);
			frame.getContentPane().add(debugger,BorderLayout.CENTER);
			frame.addWindowListener(new WindowAdapter(){
				// public void windowClosing(WindowEvent e) { System.exit(0); }
			});
			frame.pack();
			frame.setVisible(true);
			return debugger;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args){
		String textBaseId=null;
		String fileName=null;
		File groundTruthLabelsFile=null;
		File mixupProgramFile=null;
		// String mainType = null;
		boolean readOnly=false,stem=false;

		// parse options
		if(args.length<4){
			System.out
					.println("Not enough arguments.  Must have at least {-file <dataFile> or -textBase <textBaseFile>} and -mixup <programFile>");
			usage();
		}

		for(int argp=0;argp<args.length;argp++){
			if("-textBase".equals(args[argp])){
				textBaseId=args[++argp];
			}else if("-file".equals(args[argp])){
				fileName=args[++argp];
			}else if("-truth".equals(args[argp])){
				groundTruthLabelsFile=new File(args[++argp]);
			}else if("-mixup".equals(args[argp])){
				mixupProgramFile=new File(args[++argp]);
			}else if("-readOnly".equals(args[argp])){
				readOnly=true;
			}else if("-stem".equals(args[argp])){
				stem=true;
			}else{
				System.out.println("illegal option "+args[argp]);
			}
		}

		if((textBaseId==null&&fileName==null)||mixupProgramFile==null||
				!mixupProgramFile.exists()||!mixupProgramFile.isFile()){

			System.out
					.println("Either can't data file not specified or can't find mixupProgram");
			usage();
			return;
		}

		try{

			JFrame frame=new JFrame("TextBaseEditor");
			TextBase base;

			if(textBaseId!=null)
				base=FancyLoader.loadTextLabels(textBaseId).getTextBase();
			else{ // this assumes directory of files

				File data=new File(fileName);
				if(data.isDirectory()){
					TextBaseLoader loader=
							new TextBaseLoader(TextBaseLoader.DOC_PER_FILE,false);
					base=loader.load(data);
				}else{
					TextBaseLoader loader=
							new TextBaseLoader(TextBaseLoader.DOC_PER_LINE,false);
					base=loader.load(data);
				}
			}

			MixupDebugger debugger=
					new MixupDebugger(base,groundTruthLabelsFile,mixupProgramFile,
							readOnly,stem);
			frame.getContentPane().add(debugger,BorderLayout.CENTER);
			frame.addWindowListener(new WindowAdapter(){
				// public void windowClosing(WindowEvent e) { System.exit(0); }
			});
			frame.pack();
			frame.setVisible(true);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private static void usage(){
		System.out
				.println("usage: MixupDebugger {-file <dataFile> or -textBase <textBaseFile>} -truth <truthLabels> -mixup <programFile> [options]");
		System.out.println("       options: -readOnly -stem");
		System.exit(0);
	}
}
