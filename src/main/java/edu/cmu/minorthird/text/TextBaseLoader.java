package edu.cmu.minorthird.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.util.ProgressCounter;

/**
 * Configurable Text Loader.
 * <p>
 * Usage: Configure a loader object using the constructors. Call .load(File)
 * with the file object to your data (could be a directory) load(File) returns
 * the TextBase object for the data.
 * <p>
 * 
 * <pre>
 * Default: 
 * TextBaseLoader tbl = new TextBaseLoader();
 * Loads One Document per File and uses embedded labels 
 * ------------------------------------------------------
 * Specify Document Style
 * TextBaseLoader tbl = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE); // Loads One document per line
 * TextBaseLoader tbl = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE); // Loads One document per file
 * ------------------------------------------------------
 * Specify document type and whether to use embedded Labels
 * // ex: Loads one doc per line and ignores embedded labels
 * TextBaseLoader tbl = new TextBaseLoader(TextBaseLoader.DOC_PER_LINE, false); 
 * ------------------------------------------------------
 * Specify document type and whether to use embedded Labels
 * // ex: Loads one doc per file, uses embedded labels, and recurses directories
 * TextBaseLoader tbl = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE, true, true); 
 * <p>
 * In ALL cases use:
 * tbl.load(FILE);
 * </pre>
 * 
 * @author William Cohen
 * @author Kevin Steppe
 * @author Cameron Williams
 * @author Quinten Mercer
 */

public class TextBaseLoader{

	// style/location for IDs, groupID, Category of doc
	// Kept to support the old TextBaseLoader api
	public static final int NONE=0; // could be given as a param at some point

	public static final int DIRECTORY_NAME=1;

	public static final int FILE_NAME=2;

	public static final int IN_FILE=3;

	// document style
	public static final int DOC_PER_LINE=0;

	public static final int DOC_PER_FILE=1;

	// XML tags
	public static final boolean USE_XML=true;

	public static final boolean IGNORE_XML=false;

	// Parameters for loading
	// One document per line in a file or One document per file
	private int documentStyle=DOC_PER_FILE;

	// tagging -- whether to use embedded XML tags
	private boolean use_markup=USE_XML;

	// recursion -- if loading from a directory should subdirectories be loaded
	// too?
	private boolean recurseDirectories=false;

	// internal structure
	private static Logger log=Logger.getLogger(TextBaseLoader.class);

	private int closurePolicy=TextLabelsLoader.CLOSE_ALL_TYPES;

	private List<StackEntry> stack; // xml tag stack

	// saves labels associated with last set of files loaded
	private MutableTextLabels labels;

	private MutableTextBase textBase;

	// --------------------- Constructors
	// -----------------------------------------------------
	/**
	 * Default constructor. It will load each file as a single document, use XML
	 * markup, and NOT recurse recurse.
	 */
	public TextBaseLoader(){
	}

	/**
	 * Specifies the document style to use, but leaves all other properties to
	 * their defaults.
	 */
	public TextBaseLoader(int documentStyle){
		this.documentStyle=documentStyle;
	}

	public TextBaseLoader(int documentStyle,boolean use_markup){
		this.documentStyle=documentStyle;
		this.use_markup=use_markup;
	}

	public TextBaseLoader(int documentStyle,boolean use_markup,
			boolean recurseDirectories){
		this.documentStyle=documentStyle;
		this.use_markup=use_markup;
		this.recurseDirectories=recurseDirectories;
	}

	// --------------------- Constructors
	// -----------------------------------------------------

	// --------------------- Public methods
	// ---------------------------------------------------
	/**
	 * Load data from the given location according to configuration and whether
	 * location is a directory or not
	 * 
	 * Calling load a second time will load into the same text base (thus the
	 * second call returns documents from both the first and second locations).
	 * Use setTextBase(null) to reset the text base.
	 * 
	 * 
	 * @param dataLocation
	 *          File representation of location (single file or directory)
	 * @return the loaded TextBase
	 * @throws IOException -
	 *           problem reading the file
	 * @throws ParseException -
	 *           problem with xml of internal tagging
	 */
	public MutableTextBase load(File dataLocation) throws IOException,
			ParseException{
		// Create new TextBase and TextLabels to hold the data
		this.textBase=new BasicTextBase();
		this.labels=new BasicTextLabels(this.textBase);

		// check whether it's a dir or single dataLocation
		if(dataLocation.isDirectory())
			loadDirectory(dataLocation);
		else
			loadFile(dataLocation);

		return textBase;
	}

	/**
	 * Load data from the given location according to configuration and whether
	 * location is a directory or not
	 * 
	 * Calling load a second time will load into the same text base (thus the
	 * second call returns documents from both the first and second locations).
	 * Use setTextBase(null) to reset the text base.
	 * 
	 * 
	 * @param dataLocation
	 *          File representation of location (single file or directory)
	 * @return the loaded TextBase
	 * @throws IOException -
	 *           problem reading the file
	 * @throws ParseException -
	 *           problem with xml of internal tagging
	 */
	public MutableTextBase load(File dataLocation,Tokenizer tok)
			throws IOException,ParseException{
		// Create new TextBase and TextLabels to hold the data
		this.textBase=new BasicTextBase(tok);
		this.labels=new BasicTextLabels(this.textBase);

		// check whether it's a dir or single dataLocation
		if(dataLocation.isDirectory())
			loadDirectory(dataLocation);
		else
			loadFile(dataLocation);

		return textBase;
	}

	/**
	 * Load a document where each word has it's own line and is follwed by three
	 * desscriptor words. The first item on each line is a word, the second a
	 * part-of-speech (POS) tag, the third a syntactic chunk tag and the fourth
	 * the named entity tag.
	 */
	public MutableTextBase loadWordPerLineFile(File file) throws IOException,
			FileNotFoundException{
		// Create the new TextBase and TextLabels that will contain this data.
		this.textBase=new BasicTextBase(new SplitTokenizer(" "));
		labels=new BasicTextLabels(this.textBase);

		// Buffer to temporarily hold the contents of each doc read in.
		StringBuffer buf=new StringBuffer("");

		// Each doc in the file needs a unique documentId
		String id=file.getName();
		int docNum=1;
		String curDocID=id+"-"+docNum;

		// Lists of spans and properties that are included in the data file
		List<CharSpan> spanList=new ArrayList<CharSpan>();
		List<String> tokenPropList=new ArrayList<String>();

		// Read in the file line by line
		String line;
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		int start=0,end=0;
		while((line=in.readLine())!=null){
			String[] words=line.split("\\s");

			// If we're in the middle of a doc, just keep adding to its buffer
			if(!(words[0].equals("-DOCSTART-"))){
				if(words.length>2){
					start=buf.length();
					buf.append(words[0]+" ");
					end=buf.length()-1;
					tokenPropList.add(words[1]);
					if(!words[3].equals("O"))
						spanList.add(new CharSpan(start,end,words[3],curDocID));
				}
			}
			// Otherwise we're at the end of a doc, so add it to the TextBase and
			// continue
			else{
				// Add the finished doc to the TextBase
				addDocument(buf.toString(),curDocID,spanList,tokenPropList);
				// Clear out the doc info variables
				spanList.clear();
				tokenPropList.clear();
				buf=new StringBuffer("");
				// Increment the document id.
				docNum++;
				curDocID=id+"-"+docNum;
			}
		}
		in.close();
		return this.textBase;
	}

	/**
	 * Sets whether the loader should use or ignore XML markup in the files. <br>
	 * <br>
	 * Valid values are: TextBaseLoader.IGNORE_XML and TextBaseLoader.USE_XML
	 */
	public void setLabelsInFile(boolean b){
		this.use_markup=b;
	}

	/**
	 * Sets the document style for loaded documents. <br>
	 * <br>
	 * Valid styles are: TextBaseLoader.DOC_PER_LINE and
	 * TextBaseLoader.DOC_PER_FILE
	 */
	public void setDocumentStyle(int style){
		this.documentStyle=style;
	}

	/** Sets whether the loader should recurse directories when loading docs. */
	public void setRecurseDirectories(boolean rec){
		this.recurseDirectories=rec;
	}

	/** get labeling generated by tags in data file */
	public MutableTextLabels getLabels(){
		return labels;
	}

	// ---------------Old Methods kept to support old
	// api-------------------------------
	// WARNING: These are all deprecated. How long have they been this way, should
	// we delete them?
//	/**
//	 * One document per file in a directory, labels are embedded in the data as
//	 * xml tags NB: Don't use this if the data isn't labbed - it will remove
//	 * things that look like <just a note> which could cause problems.
//	 * 
//	 * Returns the TextLabels object, the textbase is embedded
//	 * 
//	 * @deprecated
//	 */
//	public static MutableTextLabels loadDirOfTaggedFiles(File dir)
//			throws ParseException,IOException{
//		TextBaseLoader loader=new TextBaseLoader(DOC_PER_FILE,true);
//		loader.load(dir);
//
//		return loader.getLabels();
//	}
//
//	/** @deprecated */
//	public void loadTaggedFiles(TextBase base,File dir) throws IOException,
//			FileNotFoundException{
//		try{
//			TextBaseLoader loader=new TextBaseLoader(DOC_PER_FILE,true);
//			loader.load(dir);
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//	}
//
//	/** @deprecated */
//	public static TextBase loadDocPerLine(File file,boolean hasGroupID)
//			throws ParseException,IOException{
//		try{
//			TextBaseLoader loader=new TextBaseLoader(DOC_PER_LINE);
//			return loader.load(file);
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//		return null;
//	}

	// --------------------- Public methods
	// ---------------------------------------------------

	// --------------------- Private methods
	// --------------------------------------------------
	private void loadDirectory(File directory) throws IOException,ParseException{
		// loop on files in directory or loop on directories?
		File[] files=directory.listFiles();
		Arrays.sort(files);
		if(files==null)
			throw new IllegalArgumentException("can't list directory "+
					directory.getName());

		ProgressCounter pc=
				new ProgressCounter("loading directory "+directory.getName(),"file",
						files.length);
		for(int i=0;i<files.length;i++){
			// skip CVS directories
			if("CVS".equals(files[i].getName()))
				continue;

			if(files[i].isDirectory()&&this.recurseDirectories)
				loadDirectory(files[i]);

			if(files[i].isFile())
				loadFile(files[i]);
			pc.progress();

		}
		pc.finished();
	}

	/**
	 * Load the given single file according the current settings
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void loadFile(File file) throws IOException,ParseException{

		log.debug("loadFile: "+file.getName());
		
		// build the correct reader

		BufferedReader in;
		if(documentStyle==DOC_PER_LINE){
			in=new LineNumberReader(new FileReader(file));
		}else{
			in=new BufferedReader(new FileReader(file));
		}

		// set the docid
		String curDocID=file.getName();

		// list of labeled spans if internally tagged
		List<CharSpan> spanList=new ArrayList<CharSpan>();

		// Clear the xml tag stack
		stack=new ArrayList<StackEntry>();

		// loop through the file
		StringBuffer buf=new StringBuffer();
		while(in.ready()){ // in.ready may cause problems on Macintosh
			String line=in.readLine();

			// BUG: THIS METHOD ADDS BLANK LINES AS DOCS FOR DOC_PER_LINE STYLE FILES

			// appends to the buffer internally
			if(this.use_markup){
				line=labelLine(line,buf,curDocID,spanList);
			}

			// If this reader is set to create a doc for each line then add the doc
			// now
			if(this.documentStyle==DOC_PER_LINE){
				if(line.trim().length()>0){
					curDocID=
							file.getName()+"@line:"+((LineNumberReader)in).getLineNumber();
					addDocument(line,curDocID,spanList,null);
					buf=new StringBuffer();
					spanList.clear();
				}
			}
			// Otherwise add the line to the buffer and continue reading
			else{
				if(!this.use_markup){
					buf.append(line);
					buf.append("\n"); // need line feed
				}
			}
		}

		if(this.documentStyle==DOC_PER_FILE)
			addDocument(buf.toString(),curDocID,spanList,null);

		in.close();
	}

	/**
	 * Add this text to the textBase as a new document, including group id and
	 * categorization
	 * 
	 * @param docText
	 *          String version of text
	 */
	private void addDocument(String docText,String documentId,List<CharSpan> spans,
			List<String> tokenProps){
		// Blank documents are dropped
		if(docText.length()==0){
			log
					.warn("Text for document "+documentId+
							" is length zero or all white space, it will not be added to the text base.");
			return;
		}

		if(log.isDebugEnabled())
			log.debug("add document "+documentId);

		// Add the document to the TextBase
		textBase.loadDocument(documentId,docText);

		// Now add all of the extracted spans to the labels set
		for(Iterator<CharSpan> j=spans.iterator();j.hasNext();){
			CharSpan charSpan=j.next();
			Span approxSpan; // =
												// textBase.documentSpan(documentId).subSpan(charSpan.lo,
												// charSpan.hi-charSpan.lo-1);
			boolean flag=false;
			for(int i=charSpan.lo;i<charSpan.hi;i++){
				if(docText.charAt(i)!=' '&&docText.charAt(i)!='\n')
					flag=true;
			}
			if(flag)
				approxSpan=
						textBase.documentSpan(documentId).charIndexSubSpan(charSpan.lo,
								charSpan.hi);
			else
				approxSpan=
						textBase.documentSpan(documentId).charIndexSubSpan(charSpan.lo,
								charSpan.hi).getLeftBoundary();

			if(log.isDebugEnabled()){
				int hi=charSpan.hi;
				if(hi>docText.length())
					hi=docText.length();

				log.debug("approximating "+charSpan.type+" span '"+
						docText.substring(charSpan.lo,hi)+"' with token span '"+approxSpan);
			}
			labels.addToType(approxSpan,charSpan.type);
		}

		// Next add all extracted token properties to the labels set
		if(tokenProps!=null&&tokenProps.size()>0){
			Document doc=textBase.getDocument(documentId);
			TextToken[] tokens=doc.getTokens();
			Iterator<String> itr=tokenProps.iterator();
			if(tokens.length>0){
				for(int x=0;x<tokens.length;x++){
					String nextPOS=itr.next();
					if(nextPOS!=null&&tokens[x]!=null){
						labels.setProperty(tokens[x],"POS",nextPOS);
					}
				}
			}
		}

		// Close the labels set
		new TextLabelsLoader().closeLabels(labels,closurePolicy);
	}

	/**
	 * Takes a single line of text. Uses the markupPattern field to remove
	 * labelings (must be xml styled). These labelling are added to the span list
	 * 
	 * @param line -
	 *          String of a single line to have it's labels parsed
	 * @param spanList -
	 *          List of span labelings
	 * @return a String with the labelings removed
	 * @throws ParseException
	 *           improper xml format will cause a parse exception
	 */
	protected String labelLine(String line,StringBuffer docBuffer,String docId,
			List<CharSpan> spanList) throws ParseException{
		// stack of open tags
		if(stack==null)
			stack=new ArrayList<StackEntry>();

		// Create the matcher to find any XML marked up tags
		Pattern markupPattern=Pattern.compile("</?([^ ><]+)( [^<>]+)?>");
		Matcher matcher=markupPattern.matcher(line);

		int currentChar=0;
		while(matcher.find()){
			String tag=matcher.group(1);
			boolean isOpenTag=!matcher.group().startsWith("</");
			if(log.isDebugEnabled()){
				log.debug("matcher.group='"+matcher.group()+"'");
				log.debug("found '"+tag+"' tag ,open="+isOpenTag+", at "+
						matcher.start()+" in:\n"+line);
			}
			// copy stuff up to tag into buffer
			docBuffer.append(line.substring(currentChar,matcher.start()));
			currentChar=matcher.end();
			if(isOpenTag){
				stack.add(new StackEntry(docBuffer.length(),tag));
			}else{
				// pop the corresponding open off the stack
				StackEntry entry=null;
				for(int j=stack.size()-1;j>=0;j--){
					entry=stack.get(j);
					if(tag.equals(entry.markupTag)){
						stack.remove(j);
						break;
					}
				}
				if(entry==null)
					throw new ParseException(
							"close '"+tag+"' tag with no open in "+docId,0);
				if(!tag.equals(entry.markupTag))
					throw new ParseException("close '"+tag+"' tag paired with open '"+
							entry.markupTag+"'",entry.index);

				if(log.isDebugEnabled()){
					log.debug("adding a "+tag+" span from "+entry.index+" to "+
							docBuffer.length()+": '"+docBuffer.substring(entry.index)+"'");
				}
				// spanList.add( new CharSpan(entry.index, docBuffer.length()-1, tag) );
				spanList.add(new CharSpan(entry.index,docBuffer.length(),tag,docId));
			}
		}
		// append stuff from end of last tag to end of line into the buffer
		docBuffer.append(line.substring(currentChar,line.length()));
		// BUG: THIS IS CAUSING BLANK LINES IN FILES TO BE ADDED AS DOCUMENTS WHEN
		// LOADED in DOC_PER_LINE FORMAT
		// HOWEVER, SIMPLY REMOVING IT BREAKS BASIC FUNCTIONALITY
		docBuffer.append("\n");

		return docBuffer.toString();
	}

	private class StackEntry{

		public int index;

		public String markupTag;

		public StackEntry(int index,String markupTag){
			this.index=index;
			this.markupTag=markupTag;
		}
	}

	private class CharSpan{

		public int lo,hi;

		String type;
//		String docID;

		public CharSpan(int lo,int hi,String type,String docID){
			this.lo=lo;
			this.hi=hi;
			this.type=type;
//			this.docID=docID;
		}
	}

	// --------------------- End Private methods
	// --------------------------------------------------
}
