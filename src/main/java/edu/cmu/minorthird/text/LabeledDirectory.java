/* Copyright 2006, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.minorthird.util.BasicCommandLineProcessor;
import edu.cmu.minorthird.util.CommandLineProcessor;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/*
 * A more scalable version of a document-per-file TextBase with
 * standoff annotation.  In a LabeledDirectory, annotations are
 * generated document-by-document, and each set of annotations is
 * saved in a .labels file for that document alone, rather than by
 * reading the entire collection into memory and annotating it at
 * once.
 *
 * @author William Cohen
 */

public class LabeledDirectory implements CommandLineProcessor.Configurable{

	private static final FileFilter TEXT_FILE_FILTER=new FileFilter(){
		@Override
		public boolean accept(File file){
			return file.getName().endsWith(".txt");
		}
	};

	private File dir;

	private List<Annotator> annotatorList=new ArrayList<Annotator>();

	private List<String> requireList=new ArrayList<String>();

	private boolean resetAll=false;

	private String nameOfFileToView=null;

	/** A CommandLineProcessor for LabeledDirectory. */
	public class MyCLP extends BasicCommandLineProcessor{

		public void dir(String s){
			dir=getDirectory(s);
		}

		public void annotate(String s){
			annotatorList.add(getAnnotator(s));
		}

		public void require(String s){
			requireList.add(s);
		}

		public void reset(){
			resetAll=true;
		}

		public void view(String s){
			nameOfFileToView=s;
		}
	}

	/** Return a CommandLineProcessor that be used to configure a LabeledDirectory */
	@Override
	public CommandLineProcessor getCLP(){
		return new MyCLP();
	}

	public LabeledDirectory(){
		;
	}

	public LabeledDirectory(String s){
		dir=getDirectory(s);
	}

	public LabeledDirectory(File dir){
		this.dir=dir;
	}

	/**
	 * Get the TextLabels which annotates this file.
	 */
	public MonotonicTextLabels getTextLabels(File f) throws IOException{
		TextBase textBase=getTextBase(f);
		String stem=f.getName().substring(0,f.getName().length()-".txt".length());
		File labelFile=new File(f.getParentFile(),stem+".labels");
		if(labelFile.exists())
			return new TextLabelsLoader().loadOps(textBase,labelFile);
		else
			return new BasicTextLabels(textBase);
	}

	/**
	 * Get a TextBase that contains exactly this file.
	 */
	private TextBase getTextBase(File f) throws IOException{
		String contents=IOUtil.readFile(f);
		BasicTextBase base=new BasicTextBase();
		base.loadDocument("someFile",contents);
		return base;
	}

	/**
	 * Re-label the text files in the directory.  This uses lists of
	 * annotators and 'require' calls that have been set up
	 * previously.
	 */
	public void reLabelText() throws IOException{
		File[] textFiles=dir.listFiles(TEXT_FILE_FILTER);
		if(textFiles==null)
			throw new IllegalArgumentException("can't list directory "+dir);
		ProgressCounter filePC=
				new ProgressCounter("labeling","file",textFiles.length);
		TextLabelsLoader loader=new TextLabelsLoader();
		for(int j=0;j<textFiles.length;j++){
			File fileJ=textFiles[j];
			MonotonicTextLabels labels=getTextLabels(fileJ);
			if(resetAll)
				labels=new BasicTextLabels(labels.getTextBase());
			for(Iterator<Annotator> k=annotatorList.iterator();k.hasNext();){
				Annotator ann=k.next();
				ann.annotate(labels);
			}
			for(Iterator<String> k=requireList.iterator();k.hasNext();){
				String req=k.next();
				labels.require(req,null);
			}
			String stem=
					fileJ.getName().substring(0,fileJ.getName().length()-".txt".length());
			File labelFile=new File(dir,stem+".labels");
			loader.saveTypesAsOps(labels,labelFile);
			filePC.progress();
		}
		filePC.finished();
	}

	// convert to a file and check that it is an existing directory
	private File getDirectory(String dirName){
		File dir=new File(dirName);
		if(!dir.exists()||!dir.isDirectory()){
			throw new IllegalArgumentException("not a directory: "+dirName);
		}
		return dir;
	}

	// load an annotator from a file
	private Annotator getAnnotator(String annotatorName){
		File annFile=new File(annotatorName);
		try{
			return (Annotator)IOUtil.loadSerialized(annFile);
		}catch(Exception ex){
			throw new IllegalArgumentException("can't load annotator "+annotatorName+
					": "+ex);
		}
	}

	/**
	 * A simple main program that allows you to add annotations to a directory of text files.
	 */
	public static void main(String[] args) throws IOException{
		LabeledDirectory ld=new LabeledDirectory();
		ld.getCLP().processArguments(args);
		if(ld.nameOfFileToView==null){
			ld.reLabelText();
		}else{
			TextLabels labels=ld.getTextLabels(new File(ld.nameOfFileToView));
			new ViewerFrame(ld.nameOfFileToView,new SmartVanillaViewer(labels));
		}
	}
}
