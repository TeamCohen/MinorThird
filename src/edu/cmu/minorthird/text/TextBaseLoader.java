package edu.cmu.minorthird.text;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;

import edu.cmu.minorthird.util.Loader;

/**
 * Configurable Loader.<br>
 * Configure the loader for the format of the data you have.  Then call load to return a TextBase<br.
 * <br>
 * There are 6 parameters:<br>
 * <ul>
 * <li>documentStyle - is a 'document' the full text of the file or from a single line?</li>
 * <li>docID - where should the id be taken from?  NONE (ie default), from the FILE_NAME, from the first word IN_FILE?</li>
 * <li>groupID - where should the document group be taken from?  NONE (no group ids), FILE_NAME, DIRECTORY_NAME, second word IN_FILE?</li>
 * <li>categoryID - add a category label to each document?  NONE (no label), DIRECTORY_NAME (ie by directory), FILE_NAME?</li>
 * <li>labelsInFile - are their xml style label tags on the data? (boolean)</li>
 * <li>recurseDirectories - search directories recursively for more data? (boolean) note that "CVS" directories will be omitted</li>
 * </ul>
 * <br>
 * These can be controlled with getter/setters or through constructors.<br>
 * load(File) is called to return a TextBase object.  The File object should be the root of your data (single file or directory of data)<br>
 * load throws exceptions if: 1) there is a problem reading the file (IOException), 2) xml tagging is not well formed (ParseException),
 *  or 3) parameter combination is not allowed (Exception)<br>
 * The validity of the parameter combination can be checked via <code>public boolean checkParameters</code>
 *
 *
 * @author William Cohen
 * @author Kevin Steppe
*/

public class TextBaseLoader implements Loader
{
  //style/location for IDs, groupID, Category of doc
  public static final int NONE = 0; //could be given as a param at some point
  public static final int DIRECTORY_NAME = 1;
  public static final int FILE_NAME = 2;
  public static final int IN_FILE = 3;

  //document style
  public static final int DOC_PER_LINE = 0;
  public static final int DOC_PER_FILE = 1;
//  public static final int DOC_PER_DIR = 2;  Not implemented


  /** Parameters for loading follow */
  /** One document per line in a file or One document per file */
  private int documentStyle = DOC_PER_FILE;

  /**
   * where is the string id for each document found?
   * if loading one doc per line then could be NONE or IN_FILE, others invalid
   * if loading one doc per file then must be FILE_NAME (assumed)
   */
  private int docIDsourceType = FILE_NAME; //TODO longer name

  /**
   * where do we find a groupID
   * if loading one doc per line then could any setting
   * if loading one doc per file then can be any except IN_FILE
   */
  private int groupIDsourceType = NONE;

  /**
   * Category to label documents
   * NOT SUPPORTED if loading one doc per line
   * if loading one doc per file then can be any except IN_FILE
   */
  private int categoryIDsourceType = NONE;

  //tagging -- are labels tagged with xml style?
  private boolean labelsInFile = false;

  //recursion -- if loading from a directory should subdirectories be loaded too?
  private boolean recurseDirectories = false;

  //legacy only
  private boolean firstWordIsDocumentId = false; /** docID = IN_FILE */
  private boolean secondWordIsGroupId = false; /** groupID = IN_FILE */

  //internal structure
 	private static Logger log = Logger.getLogger(TextBaseLoader.class);
	private int closurePolicy = TextLabelsLoader.CLOSE_ALL_TYPES;
  // saves labels associated with last set of files loaded
  private MutableTextLabels labels;
  private TextBase textBase;

  private String curDocID;
  private String curGrpID;
  private String curCatID;
  private Pattern markupPattern = Pattern.compile("</?([^ ><]+)( [^<>]+)?>");
  private ArrayList stack; //xml tag stack

  //------------------ Statics ------------------------------------------------------------
  /**
   * One document per line, documentID as first word, optional groupID second
   * ex:
   *   msg1 group1 blah blahblah
   *   msg2 group1 blah blahblah
   *   msg3 group2 blah blahblah
   */
  public static TextBase loadDocPerLine(File file, boolean hasGroupID) throws Exception, ParseException, IOException
  {
    TextBaseLoader loader = new TextBaseLoader(DOC_PER_LINE, IN_FILE);
    if (hasGroupID)
      loader.setGroupIDsourceType(IN_FILE);

    return loader.load(file);
  }

  /**
   * One document per file in a directory, labels are embedded in the data
   *
   * Returns the TextLabels object, the textbase is embedded
   */
  public static MutableTextLabels loadDirOfTaggedFiles(File dir) throws Exception, ParseException, IOException
  {
    TextBaseLoader loader = new TextBaseLoader(DOC_PER_FILE, FILE_NAME, true);
    loader.load(dir);

    return loader.getLabels();

  }

  //------------------ End Statics ------------------------------------------------------------


  //------------------ Getters and Setters -------------------------------------------------
  public int getDocIDsourceType()
  { return docIDsourceType; }

  public void setDocIDsourceType(int docIDsourceType)
  {
    this.docIDsourceType = docIDsourceType;
    if (docIDsourceType == IN_FILE)
      this.firstWordIsDocumentId = true;
    else
      this.firstWordIsDocumentId = false;
  }

  public int getGroupIDsourceType()
  { return groupIDsourceType; }

  public void setGroupIDsourceType(int groupIDsourceType)
  {
    this.groupIDsourceType = groupIDsourceType;
    if (groupIDsourceType == IN_FILE)
      this.secondWordIsGroupId = true;
    else
      this.secondWordIsGroupId = false;
  }

  public Pattern getMarkupPattern()
  { return markupPattern; }

  public void setMarkupPattern(Pattern markupPattern)
  { this.markupPattern = markupPattern; }

  public boolean isRecurseDirectories()
  { return recurseDirectories; }

  public void setRecurseDirectories(boolean recurseDirectories)
  { this.recurseDirectories = recurseDirectories; }

  public int getDocumentStyle()
  { return documentStyle; }

  /** DOC_PER_FILE => docID = FILE_NAME */
  public void setDocumentStyle(int documentStyle)
  {
    this.documentStyle = documentStyle;
    if (documentStyle == DOC_PER_FILE)
      docIDsourceType = FILE_NAME;
  }

  /** loading labels from data file? */
  public boolean isLabelsInFile()
  { return labelsInFile; }

  /** set whether to load from tags in the file */
  public void setLabelsInFile(boolean labelsInFile)
  { this.labelsInFile = labelsInFile; }

	/** Set the closure policy.
	 * @param policy one of TextLabelsLoader.CLOSE_ALL_TYPES,
	 * TextLabelsLoader.CLOSE_TYPES_IN_LABELED_DOCS, TextLabelsLoader.DONT_CLOSE_TYPES
	 */
	public void setClosurePolicy(int policy) { this.closurePolicy = policy; }

  /** get labeling generated by tags in data file */
  public MutableTextLabels getLabels()
  { return labels; }

  /** For one doc per line, indicates if first word is the Id. */
  public boolean getFirstWordIsDocumentId() { return firstWordIsDocumentId; }

  /** For one doc per line, indicates if first word is the Id. */
  public void setFirstWordIsDocumentId(boolean flag) { firstWordIsDocumentId = flag; }

  /** For one doc per line, indicates if second word is the group Id. */
  public boolean getSecondWordIsGroupId() { return secondWordIsGroupId; }

  /** For one doc per line, indicates if second word is the group Id and also
   * sets first word to be document id */
  public void setSecondWordIsGroupId(boolean flag) {
    firstWordIsDocumentId = true;
    secondWordIsGroupId = flag;
  }
  //-------------------- End Getters and Setters-------------------------------------------------

  //--------------------- Constructors -----------------------------------------------------
  public TextBaseLoader()
  {}

  public TextBaseLoader(int documentSytle)
  { this.documentStyle = documentSytle; }

  public TextBaseLoader(int documentSytle, int docID)
  {
    this.documentStyle = documentSytle;
    this.docIDsourceType = docID;
  }

  public TextBaseLoader(int documentSytle, int docID, boolean labelsInFile)
  {
    this.documentStyle = documentSytle;
    this.docIDsourceType = docID;
    this.labelsInFile = labelsInFile;
  }

  public TextBaseLoader(int documentSytle, int docID, int groupID, int categoryID)
  {
    this.categoryIDsourceType = categoryID;
    this.docIDsourceType = docID;
    this.documentStyle = documentSytle;
    this.groupIDsourceType = groupID;
  }

  public TextBaseLoader(int documentSytle, int docID, int groupID, int categoryID, boolean labelsInFile, boolean recurseDirectories)
  {
    this.documentStyle = documentSytle;
    this.docIDsourceType = docID;
    this.groupIDsourceType = groupID;
    this.categoryIDsourceType = categoryID;
    this.labelsInFile = labelsInFile;
    this.recurseDirectories = recurseDirectories;
  }

  //--------------------- Constructors -----------------------------------------------------

  //--------------------- Public methods ---------------------------------------------------
  /**
   * Load data from the given location according to configuration and whether location
   * is a directory or not
   * @param dataLocation File representation of location (single file or directory)
   * @return the loaded TextBase
   * @throws IOException - problem reading the file
   * @throws ParseException - problem with xml of internal tagging
   * @throws Exception - problem with parameter configuration
   */
  public TextBase load(File dataLocation) throws IOException, ParseException, Exception
  {
    if (!checkParameters())
      throw new Exception("Parameter combination is not allowed.  Please see documentation for TextBaseLoader.");

    if (textBase == null)
      textBase = new BasicTextBase();
    if (labels == null)
      labels = new BasicTextLabels(textBase);

    clear();
    //check whether it's a dir or single dataLocation
    if (dataLocation.isDirectory())
      loadDirectory(dataLocation);
    else
      loadFile(dataLocation);

    return textBase;
  }

  /**
   * Checks the consistency of parameters
   * TODO: need a table of allowed values
   * @return true if the paremeter combination is supported, else false
   */
  public boolean checkParameters()
  {
    switch (this.documentStyle)
    {
      case DOC_PER_LINE:
        if ( (docIDsourceType == NONE || docIDsourceType == IN_FILE) && (categoryIDsourceType == NONE) )
          return true;
        break;

      case DOC_PER_FILE:
        if ( (docIDsourceType == NONE || docIDsourceType == FILE_NAME ) && (groupIDsourceType != IN_FILE) && (categoryIDsourceType != IN_FILE))
          return true;
        break;
    }

    return false;
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

  //--------------------- Public methods ---------------------------------------------------

  //--------------------- Private methods --------------------------------------------------
  private void loadDirectory(File directory) throws IOException, ParseException
  {
    {
      //loop on files in directory or loop on directories?
      File[] files = directory.listFiles();
      if (files==null) throw new IllegalArgumentException("can't list directory "+directory.getName());

      if (categoryIDsourceType == DIRECTORY_NAME)
        curCatID = directory.getName();
      if (groupIDsourceType == DIRECTORY_NAME)
        curGrpID = directory.getName();

      for (int i=0; i<files.length; i++)
      {
        // skip CVS directories
        if ("CVS".equals(files[i].getName()))
          continue;

        if (files[i].isDirectory() && isRecurseDirectories())
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
    //get input reader
    BufferedReader in;
    //build the correct reader
    if (documentStyle == DOC_PER_LINE)
      in = new LineNumberReader(new FileReader(file));
    else
      in = new BufferedReader(new FileReader(file));

    //set the docid
    if (docIDsourceType == FILE_NAME)
      curDocID = file.getName();

    //select the categoryID properly
    if (categoryIDsourceType == FILE_NAME)
      curCatID = file.getName();

    //set the groupID
    if (groupIDsourceType == FILE_NAME)
      curGrpID = file.getName();

    //list of labeled spans if internally tagged
    List spanList = new ArrayList();

    //buffer of lines in file
    StringBuffer buf = new StringBuffer();


    //loop through the file
    while (in.ready()) //in.ready may cause problems on Macintosh
    {
      String line = in.readLine();
      if (this.isLabelsInFile())
        line = labelLine(line, buf, spanList); // appends to the buffer internally

      if (this.documentStyle == DOC_PER_LINE)
      {   //get ids
        //make doc
        if (docIDsourceType == NONE)
          curDocID = file.getName() + "@line:" + ((LineNumberReader)in).getLineNumber();
        else
          line = getIDsFromLine(line);

        addDocument(line); //we don't really care about the buffer, it's fluf
        buf = new StringBuffer();
      }
      else
        if (!this.isLabelsInFile()) //better append to the buffer if it wasn't done before
          buf.append(line);
    }

    if (this.documentStyle == DOC_PER_FILE)
      addDocument(buf.toString()); //still need to set ids and such

    in.close();
  }

  /**
   * Add this text to the textBase as a new document, including group id and categorization
   * @param docText String version of text
   */
  private void addDocument(String docText)
  {
    if (log.isDebugEnabled())
      log.debug("add doc (" + curDocID + ") " + docText);

    textBase.loadDocument(curDocID, docText);
    if (curGrpID != null)
      textBase.setDocumentGroupId(curDocID, curGrpID);

    //label document as this category
    if (curCatID != null)
      labels.addToType(textBase.documentSpan(curDocID), curCatID);
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
  private String labelLine(String line, StringBuffer docBuffer, List spanList) throws ParseException
  {
    // stack of open tags
    if (stack == null)
      stack = new ArrayList();

    int currentChar = 0;
    Matcher matcher = markupPattern.matcher(line);
    while (matcher.find()) {
      String tag = matcher.group(1);
      boolean isOpenTag = !matcher.group().startsWith("</");
      log.debug("matcher.group='"+matcher.group()+"'");
      log.debug("found '"+tag+"' tag ,open="+isOpenTag+", at "+matcher.start()+" in:\n"+line);
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

        log.debug("adding a "+tag+" span from "+entry.index+" to "+docBuffer.length()
                  +": '"+docBuffer.substring(entry.index)+"'");
        spanList.add( new CharSpan(entry.index, docBuffer.length(), tag) );
      }
    }
    // append stuff from end of last tag to end of line into the buffer
    docBuffer.append( line.substring(currentChar, line.length()) );
    docBuffer.append( "\n" );

    return docBuffer.toString();
  }

  /**
   * parse id values out of the given line.
   * Return the rest of the line
   * @param line
   * @return
   */
  private String getIDsFromLine(String line)
  {
    int spaceIndex = line.indexOf(' ');
    if (spaceIndex < 0)
    {
      curDocID = line;
      line = "";
    }
    else
    {
      curDocID = line.substring(0, spaceIndex);
      if (!secondWordIsGroupId)
      {
        line = line.substring(spaceIndex + 1, line.length());
      }
      else
      {
        int spaceIndex2 = line.indexOf(' ', spaceIndex + 1);
        if (spaceIndex < 0)
        {
          curGrpID = line.substring(spaceIndex + 1, line.length());
          line = "";
        }
        else
        {
          curGrpID = line.substring(spaceIndex + 1, spaceIndex2);
          line = line.substring(spaceIndex2 + 1, line.length());
        }
      }
    }
    return line;
  }

  /**
   * Clears the state of current ids.
   * Good to do before each document
   */
  private void clear()
  {
    curCatID = null;
    curDocID = null;
    curGrpID = null;
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
    String type;
    public CharSpan(int lo,int hi,String type) {
      this.lo=lo; this.hi=hi; this.type = type;
    }
  }

  //--------------------- End Private methods --------------------------------------------------

  //--------------------- Deprecated methods -----------------------------------------------
  //--------------------- Old Methods -------------------------------------------------------
	/**
   * Load from either a file (one document per line) or a directory (one document per file)
   * Directory is assumed to be tagged files
   * Single file assumed not to be tagged
   *
   * @deprecated; to be removed at end of February
   */
	public void loadFile(TextBase base,File file) throws IOException,FileNotFoundException
	{
		if (file.isDirectory())
      loadTaggedFiles(base,file);
		else {
			loadLines(base,file);
			labels = new BasicTextLabels(base);
		}
	}

	/** Load files from a directory, stripping out any XML/SGML tags.
   *
   * @deprecated; to be removed at end of February
   *
   */
	public void loadTaggedFiles(TextBase base,File dir) throws IOException,FileNotFoundException
	{
		labels = new BasicTextLabels(base);

		File[] files = dir.listFiles();
		if (files==null) throw new IllegalArgumentException("can't list directory "+dir.getName());

		for (int i=0; i<files.length; i++) {

			// skip CVS directories
			if ("CVS".equals(files[i].getName())) continue;

      loadTaggedFile(files[i], markupPattern, base);

    }
	}

  /**
   * @deprecated; to be removed at end of February
   */
  public void loadTaggedFile(File file, Pattern markupPattern, TextBase base) throws IOException
  {
    if (labels == null)
      labels = new BasicTextLabels(base);

    if (markupPattern == null)
      markupPattern = Pattern.compile("</?([^ ><]+)( [^<>]+)?>");

    // list of constructed spans
    List spanList = new ArrayList();
    // file name used as ID
    String id = file.getName();
    // holds a string representation of the file with xml tags removed
    StringBuffer buf = new StringBuffer("");

    LineNumberReader in = new LineNumberReader(new FileReader(file));
    String line;
    while ((line = in.readLine())!=null) {
      try
      { labelLine(line, buf, spanList); }
      catch (ParseException e)
      {
        IllegalStateException ex = new IllegalStateException("in " + id + " @" + in.getLineNumber() + ":" + e.getMessage());
        ex.setStackTrace(e.getStackTrace());
        throw ex;
      }
    }
    in.close();
    // add the document to the textbase
    base.loadDocument(id, buf.toString() );
    // add the markup to the labels
    Set types = new TreeSet();
    for (Iterator j=spanList.iterator(); j.hasNext(); ) {
      CharSpan charSpan = (CharSpan)j.next();
      types.add( charSpan.type );
      Span approxSpan = base.documentSpan(id).charIndexSubSpan(charSpan.lo, charSpan.hi);
      log.debug("approximating "+charSpan.type+" span '"
                +buf.toString().substring(charSpan.lo,charSpan.hi)
                +"' with token span '"+approxSpan);
      labels.addToType( approxSpan, charSpan.type );
    }
    new TextLabelsLoader().closeLabels( labels, closurePolicy );
  }


  //
	// loadLines code
	//

	/** Load each line of the file as a separate 'document'.
	 * If firstWordIsDocumentId is set to be true, then the first token on
	 * a line is the documentId.
   *
   * @deprecated; to be removed at end of February
	 */
  public void loadLines(TextBase base, File file) throws IOException, FileNotFoundException
  {
    LineNumberReader in = new LineNumberReader(new FileReader(file));
    String line;
    this.textBase = base;
    while ((line = in.readLine()) != null)
    {
      clear();
      if (!firstWordIsDocumentId)
      {
        curDocID = file.getName() + "@line:" + in.getLineNumber(); // default
      }
      else
        line = getIDsFromLine(line);

      textBase.loadDocument(curDocID, line);
      if (curGrpID != null) {
				textBase.setDocumentGroupId(curDocID, curGrpID);
			}
    }
    in.close();
  }

	/**
   * Read a serialized BasicTextBase from a file.
   *
   * will soon be deprecated; to be removed at end of February
   */
	public TextBase readSerialized(File file) throws IOException {
		try {
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
			TextBase b = (TextBase)in.readObject();
			in.close();
			return b;
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("can't read BasicTextBase from "+file+": "+e);
		}
	}

	/**
	 * Takes a base directory.  Each file is a different doc to load.
	 * @param base TextBase to load into
	 * @param directory File representation of directory
   *
   * @deprecated; to be removed at end of February
   *
	 */
	public void loadDir(TextBase base, File directory)
	{
		if (directory.isDirectory())
		{
//			String categoryLabel = directory.getName();
//			log.debug("found directory for type: " + categoryLabel);
			//load everything in the directory
			try
			{
				File[] files = directory.listFiles();
				for (int j = 0; j < files.length; j++)
				{
					// skip CVS directories
					if ("CVS".equals(files[j].getName())) continue;
					File file = files[j];
					this.loadFileWithID(base, file, file.getName());
				}
			}
			catch (IOException ioe)
			{ log.error(ioe, ioe); }
		}
		else
			log.error("loadDir found a file instead of directory label: "
								+ directory.getPath() + File.pathSeparator + directory.getName());
	}

	/**
	 * Takes a base directory.  Each subdirectory is a label for the category
	 * of the files in that directory.  Each file is a different doc
	 * @param base TextBase to load into
	 * @param dir File representation of dir to use as the base
   *
   * @deprecated; to be removed at end of February
   *
	 */
	public void loadLabeledDir(TextBase base, File dir)
	{
		labels = new BasicTextLabels(base);
		//cycle through the directories
		//these should all be directories
		File[] dirs = dir.listFiles();
		for (int i = 0; i < dirs.length; i++)
		{
			File directory = dirs[i];
			if (directory.isDirectory())
			{
				String categoryLabel = directory.getName();
				log.debug("found directory for type: " + categoryLabel);
				//load everything in the directory
				try
				{
					File[] files = directory.listFiles();
					for (int j = 0; j < files.length; j++)
					{
						File file = files[j];
						this.loadFileWithID(base, file, file.getName());
						//label the new span
						labels.addToType(base.documentSpan(file.getName()), categoryLabel);
					}
				}
				catch (IOException ioe)
				{ log.error(ioe, ioe); }
			}
			else
				log.error("loadLabeledDir found a file instead of directory label: "
									+ directory.getPath() + File.pathSeparator + directory.getName());
		}
	}


	/**
	 * the given file is treated as a single document
	 * @param base TextBase to load into
	 * @param file File to load from
   * @param id ID to be given to the document
   *
   * @deprecated; to be removed at end of February
   *
	 */
	public void loadFileWithID(TextBase base, File file, String id) throws IOException
	{
		log.debug("loadFileWithID: " + file);
		if (!file.isFile())
			throw new IllegalArgumentException("loadFileWithID must be given a file, not a directory");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String allLines = new String();
		while (in.ready())
		{
			allLines += in.readLine() + "\n";
		}
		
		base.loadDocument(id, allLines);
		in.close();
	}

	// test routine
	static public void main(String[] args) {
		if (args.length<2)
			throw new IllegalArgumentException("usage: TextBaseLoader [file|dir] output.seqbase");
		try {
			TextBase b; // = new BasicTextBase();
			TextBaseLoader loader = new TextBaseLoader();
			File f = new File(args[0]);
			if (f.isDirectory()) {
				b = TextBaseLoader.loadDirOfTaggedFiles(f).getTextBase();
			} else {
				b = TextBaseLoader.loadDocPerLine(f, false);
			}
			loader.writeSerialized(b, new File(args[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

  //--------------------- Deprecated methods -----------------------------------------------

}
