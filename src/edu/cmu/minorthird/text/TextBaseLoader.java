package edu.cmu.minorthird.text;

import org.apache.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configurable Text Loader.
 * <p>
 * Usage:
 * Configure a loader object using the constructors.
 * Call .load(File) with the file object to your data (could be a directory)
 * load(File) returns the TextBase object for the data.
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
 */

public class TextBaseLoader
{
    //style/location for IDs, groupID, Category of doc
    //Kept to support the old TextBaseLoader api    
    public static final int NONE = 0; //could be given as a param at some point
    public static final int DIRECTORY_NAME = 1;
    public static final int FILE_NAME = 2;
    public static final int IN_FILE = 3;

    //document style
    public static final int DOC_PER_LINE = 0;
    public static final int DOC_PER_FILE = 1;

    //XML tags
    public static final boolean USE_XML = true;
    public static final boolean IGNORE_XML = false;

    /** Parameters for loading follow */
    /** One document per line in a file or One document per file */
    private int documentStyle = DOC_PER_FILE;

    //tagging -- whether to use embedded XML tags
    private boolean use_markup = USE_XML;

    //recursion -- if loading from a directory should subdirectories be loaded too?
    private boolean recurseDirectories = false;

    //internal structure
    private static Logger log = Logger.getLogger(TextBaseLoader.class);
    private int closurePolicy = TextLabelsLoader.CLOSE_ALL_TYPES;
    // saves labels associated with last set of files loaded
    private MutableTextLabels labels;
    private TextBase textBase;
    private Tokenizer tokenizer = null;

    private String curDocID;
    private Pattern markupPattern = Pattern.compile("</?([^ ><]+)( [^<>]+)?>");
    private ArrayList stack; //xml tag stack
    private List spanList;
    private List tokenPropList = null;

    //--------------------- Constructors -----------------------------------------------------
    public TextBaseLoader()
    {}

    public TextBaseLoader(int documentStyle)
    { this.documentStyle = documentStyle; }

    public TextBaseLoader(int documentStyle, boolean use_markup)
    {
	this.documentStyle = documentStyle;
	this.use_markup = use_markup;
    }
       
    public TextBaseLoader(int documentStyle, boolean use_markup, boolean recurseDirectories)
    {
	this.documentStyle = documentStyle;
	this.use_markup = use_markup;
	this.recurseDirectories = recurseDirectories;
    }

    /** @deprecated */
    public TextBaseLoader(int documentStyle, int docID)
    {
	this.documentStyle = documentStyle;
    }

    /** @deprecated */
    public TextBaseLoader(int documentStyle, int docID, boolean use_markup)
    {
	this.documentStyle = documentStyle;
	this.use_markup = use_markup;
    }

    /** @deprecated */
    public TextBaseLoader(int documentStyle, int docID, int groupID, int categoryID)
    {
	this.documentStyle = documentStyle;
    }

    /** @deprecated */
    public TextBaseLoader(int documentStyle, int docID, int groupID, int categoryID, boolean labelsInFile, boolean recurseDirectories)
    {
	this.documentStyle = documentStyle;
	this.use_markup = labelsInFile;
	this.recurseDirectories = recurseDirectories;
    }

    //--------------------- Constructors -----------------------------------------------------   

    //--------------------- Public methods ---------------------------------------------------
    /**
     * Load data from the given location according to configuration and whether location
     * is a directory or not
     *
     * Calling load a second time will load into the same text base (thus the second call returns
     * documents from both the first and second locations).  Use setTextBase(null) to reset the text base.
     *
     *
     * @param dataLocation File representation of location (single file or directory)
     * @return the loaded TextBase
     * @throws IOException - problem reading the file
     * @throws ParseException - problem with xml of internal tagging
     */
    public TextBase load(File dataLocation) throws IOException, ParseException
    {
	if (textBase == null)
	    textBase = new BasicTextBase();
	if (labels == null)
	    labels = new BasicTextLabels(textBase);

	clear();
	this.tokenizer = null;
	//check whether it's a dir or single dataLocation
	if (dataLocation.isDirectory())
	    loadDirectory(dataLocation);
	else
	    loadFile(dataLocation);

	return (TextBase)textBase;
    }

    /**
     * Load data from the given location according to configuration and whether location
     * is a directory or not
     *
     * Calling load a second time will load into the same text base (thus the second call returns
     * documents from both the first and second locations).  Use setTextBase(null) to reset the text base.
     *
     *
     * @param dataLocation File representation of location (single file or directory)
     * @return the loaded TextBase
     * @throws IOException - problem reading the file
     * @throws ParseException - problem with xml of internal tagging
     */
    public TextBase load(File dataLocation, Tokenizer tok) throws IOException, ParseException
    {
	if (textBase == null)
	    textBase = new BasicTextBase();
	if (labels == null)
	    labels = new BasicTextLabels(textBase);

	curDocID = curDocID + "2";

	clear();
	//check whether it's a dir or single dataLocation
	this.tokenizer = tok;
	if (dataLocation.isDirectory())
	    loadDirectory(dataLocation);
	else
	    loadFile(dataLocation);

	return (TextBase)textBase;
    }

    public void setLabelsInFile(boolean b) {
	this.use_markup = b;
    }

    public TextBase retokenize(TextBase tb, Tokenizer tokenizer) throws IOException, ParseException
    {
	if (labels == null)
	    labels = new BasicTextLabels(textBase);

	TextBase childTB = ((BasicTextBase)tb).retokenize(tokenizer);
	textBase = childTB;
	return textBase;
    }

    /**
     * Write the textTokenbase to a file.
     *
     * NB: ksteppe bug #
     */
    public void writeSerialized(TextBase base,File file) throws IOException {
	ObjectOutputStream out =
	    new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	out.writeObject(base);
	out.flush();
	out.close();
    }

    /** get labeling generated by tags in data file */
    public MutableTextLabels getLabels()
    { return labels; }

    //---------------Old Methods kept to support old api-------------------------------
    /**
     * One document per file in a directory, labels are embedded in the data as xml tags
     * NB: Don't use this if the data isn't labbed - it will remove things that look like <just a note>
     * which could cause problems.
     *
     * Returns the TextLabels object, the textbase is embedded
     * @deprecated
     */
    public static MutableTextLabels loadDirOfTaggedFiles(File dir) throws ParseException, IOException
    {
	TextBaseLoader loader = new TextBaseLoader(DOC_PER_FILE, true);
	loader.load(dir);

	return loader.getLabels();

    }

    /** @deprecated */
    public void loadTaggedFiles(TextBase base,File dir) throws IOException,FileNotFoundException
    {
	try {
	    TextBaseLoader loader = new TextBaseLoader(DOC_PER_FILE, true);
	    loader.load(dir);
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }

    /** @deprecated */
    public static TextBase loadDocPerLine(File file, boolean hasGroupID) throws ParseException, IOException
    {
	try {
	    TextBaseLoader loader = new TextBaseLoader(DOC_PER_LINE);
	    return loader.load(file);
	} catch(Exception e) {
	    e.printStackTrace();
	}  
	return null;
    }
    //--------------------- Public methods ---------------------------------------------------

    //--------------------- Private methods --------------------------------------------------
    private void loadDirectory(File directory) throws IOException, ParseException
    {
	{
	    //loop on files in directory or loop on directories?
	    File[] files = directory.listFiles();
	    if (files==null) throw new IllegalArgumentException("can't list directory "+directory.getName());

	    for (int i=0; i<files.length; i++)
		{
		    // skip CVS directories
		    if ("CVS".equals(files[i].getName()))
			continue;

		    if (files[i].isDirectory() && this.recurseDirectories)
			loadDirectory(files[i]);

		    if (files[i].isFile())
			loadFile(files[i]);

		}
	}
    }

    /**
     * Load the given single file according the current settings
     * @param file
     * @throws IOException
     */
    private void loadFile(File file) throws IOException, ParseException
    {
	log.debug("loadFile: " + file.getName());

	//build the correct reader
	BufferedReader in;
	if (documentStyle == DOC_PER_LINE)
	    in = new LineNumberReader(new FileReader(file));
	else
	    in = new BufferedReader(new FileReader(file));

	curDocID = file.getName(); //set the docid

	spanList = new ArrayList(); //list of labeled spans if internally tagged

	//buffer of lines in file
	StringBuffer buf = new StringBuffer();

	//loop through the file
	while (in.ready()) //in.ready may cause problems on Macintosh
	    {
		String line = in.readLine();

		if (this.use_markup) {
		    line = labelLine(line, buf, spanList); // appends to the buffer internally
		}

		if (this.documentStyle == DOC_PER_LINE)
		    {   
			//get ids
			//make doc
			curDocID = file.getName() + "@line:" + ((LineNumberReader)in).getLineNumber();
	  
			addDocument(line); //we don't really care about the buffer, it's fluf
			buf = new StringBuffer();
		    }
		else
		    if (!this.use_markup) //better append to the buffer if it wasn't done before
			{
			    buf.append(line);
			    buf.append("\n"); //need line feed <---
			}
	    }

	if (this.documentStyle == DOC_PER_FILE)
	    addDocument(buf.toString()); //still need to set ids and such

	in.close();
    }

    /**
     *  Load a document where each word has it's own line and is follwed by three desscriptor words.
     *  The first item on each line is a word, the second a part-of-speech (POS) tag, the third a 
     *  syntactic chunk tag and the fourth the named entity tag.
     */
    public void loadWordPerLineFile(TextBase base, File file) throws IOException, FileNotFoundException
    {
	this.tokenizer = new Tokenizer(Tokenizer.SPLIT, " ");
	if (labels == null)
	    labels = new BasicTextLabels(base);
	String id = file.getName();
	LineNumberReader in = new LineNumberReader(new FileReader(file));
	String line;
	this.textBase = base;
	StringBuffer buf = new StringBuffer("");
	int docNum = 1, start = 0, end = 0;
	curDocID = id + "-" + docNum;
	spanList = new ArrayList();
	tokenPropList = new ArrayList();

	while((line = in.readLine()) != null) {
	   String[] words = line.split("\\s");
	   if(!(words[0].equals("-DOCSTART-"))) {
	       if(words.length > 2) {
		   start = buf.length();
		   buf.append(words[0]+" ");
		   end = buf.length()-1;
		   		  
		   tokenPropList.add(words[1]);
		   if(!words[3].equals("O"))
		       spanList.add(new CharSpan(start,end,words[3],curDocID)); 
	       }
	       
	   }else {
	       this.tokenizer = new Tokenizer(Tokenizer.SPLIT, " ");
	       addDocument(buf.toString());
	       spanList = new ArrayList();
	       tokenPropList = new ArrayList();
	       buf = new StringBuffer("");
	       docNum++;
	       curDocID = id + "-" + docNum;
	   }
	}
	in.close();
    }

    /**
     * Add this text to the textBase as a new document, including group id and categorization
     * @param docText String version of text
     */
    private void addDocument(String docText)
    {
	//Blank documents are dropped
	if (docText.length() == 0)
	    {
		log.warn("Text for document " + curDocID 
			 + " is length zero or all white space, it will not be added to the text base.");
		return;
	    }

	if (log.isDebugEnabled())
	    log.debug("add document " + curDocID);
	if(tokenizer == null)
	    textBase.loadDocument(curDocID, docText);
	else textBase.loadDocument(curDocID, docText, tokenizer);

	for (Iterator j=spanList.iterator(); j.hasNext(); ) {
	    CharSpan charSpan = (CharSpan)j.next();
	    Span approxSpan; // = textBase.documentSpan(curDocID).subSpan(charSpan.lo, charSpan.hi-charSpan.lo-1); 	   
	    boolean flag = false;
	    for(int i = charSpan.lo; i<charSpan.hi; i++) {
		if(docText.charAt(i) != ' ' && docText.charAt(i) != '\n')
		    flag = true;
	    }
	    if(flag)
		approxSpan = textBase.documentSpan(curDocID).charIndexSubSpan(charSpan.lo, charSpan.hi);	      
	    else approxSpan = textBase.documentSpan(curDocID).charIndexSubSpan(charSpan.lo, charSpan.hi).getLeftBoundary();

	    if (log.isDebugEnabled())
		{
		    int hi = charSpan.hi;
		    if (hi > docText.length())
			hi = docText.length();

		    log.debug("approximating "+charSpan.type+" span '"
			      + docText.substring(charSpan.lo,hi)
			      +"' with token span '"+approxSpan);
		}
	    labels.addToType( approxSpan, charSpan.type );

	}
	if(tokenPropList.size() > 0) {	    	    
	    Document doc = textBase.getDocument(curDocID);
	    TextToken[] tokens = tokenizer.splitIntoTokens(doc,docText);
	    Iterator itr = tokenPropList.iterator();
	    if(tokens.length > 0) {		
		for(int x=0; x<tokens.length; x++) {
		    String nextPOS = (String)itr.next();
		    if(nextPOS != null && tokens[x] != null) {
			labels.setProperty(tokens[x], "POS", nextPOS);
		    }
		}
	    }
	}
	new TextLabelsLoader().closeLabels( labels, closurePolicy );
	spanList = new ArrayList();
    }

    /**
     * Takes a single line of text.
     * Uses the markupPattern field to remove labelings (must be xml styled).
     * These labelling are added to the span list
     *
     * @param line - String of a single line to have it's labels parsed
     * @param spanList - List of span labelings
     * @return a String with the labelings removed
     * @throws ParseException improper xml format will cause a parse exception
     */
    protected String labelLine(String line, StringBuffer docBuffer, List spanList) throws ParseException
    {
	// stack of open tags
	if (stack == null)
	    stack = new ArrayList();

	int currentChar = 0;
	Matcher matcher = markupPattern.matcher(line);
	while (matcher.find()) {
	    String tag = matcher.group(1);
	    boolean isOpenTag = !matcher.group().startsWith("</");
	    if (log.isDebugEnabled()) {
		log.debug("matcher.group='"+matcher.group()+"'");
		log.debug("found '"+tag+"' tag ,open="+isOpenTag+", at "+matcher.start()+" in:\n"+line);
	    }
	    //copy stuff up to tag into buffer
	    docBuffer.append( line.substring(currentChar, matcher.start()) );
	    currentChar = matcher.end();
	    if (isOpenTag) {
		stack.add( new StackEntry(docBuffer.length(), tag) );
	    } else {
		// pop the corresponding open off the stack
		StackEntry entry = null;
		for (int j=stack.size()-1; j>=0; j--) {
		    entry = (StackEntry)stack.get(j);
		    if (tag.equals(entry.markupTag)) {
			stack.remove(j);
			break;
		    }
		}
		if (entry==null)
		    throw new ParseException("close '"+tag+"' tag with no open", entry.index);
		if (!tag.equals(entry.markupTag))
		    throw new ParseException("close '"+tag+"' tag paired with open '" +entry.markupTag+"'", entry.index);

		if (log.isDebugEnabled()) {
		    log.debug("adding a "+tag+" span from "+entry.index+" to "+docBuffer.length()
			      +": '"+docBuffer.substring(entry.index)+"'");
		}
		//spanList.add( new CharSpan(entry.index, docBuffer.length()-1, tag) );
		spanList.add( new CharSpan(entry.index, docBuffer.length(), tag, curDocID) );
	    }
	}
	// append stuff from end of last tag to end of line into the buffer
	docBuffer.append( line.substring(currentChar, line.length()) );
	docBuffer.append( "\n" );

	return docBuffer.toString();
    }

    /**
     * Clears the state of current ids.
     * Good to do before each document
     */
    private void clear()
    {
	curDocID = null;
    }


    private class StackEntry {
	public int index;
	public String markupTag;
	public StackEntry(int index,String markupTag) {
	    this.index=index; this.markupTag=markupTag;
	}
    }
    private class CharSpan {
	public int lo,hi;
	String type, docID;
	public CharSpan(int lo,int hi,String type, String docID) {
	    this.lo=lo; this.hi=hi; this.type = type; this.docID=docID;
	}
    }

    //--------------------- End Private methods --------------------------------------------------


}
