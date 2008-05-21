package edu.cmu.minorthird.text.mixup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class Statement implements Serializable{

	static private final long serialVersionUID=20080303L;

	private static Logger log=Logger.getLogger(Statement.class);

	public static int REGEX=1,MIXUP=2,FILTER=3,PROVIDE=4,REQUIRE=5,DECLARE=6,
			TRIE=7,ANNOTATE_WITH=8;

	//TODO: We should handle these properties better, possibly using a java properties object
	// encodes the statement properties
	private String keyword,property,type,startType,value;

	// set of words, for a dictionary
	private Set<String> wordSet=null;

	// file containing dictionary
//	private File dictFile=null;

	// Variable for whether to ignore case in dictionary
	private boolean ignoreCase;

	// split string for retokenizing textBase
	private String split,patt;

	// current tokenization level 
	private String level;

	// Variables that define the level and type to be imported to the current textBase
	private String importLevel,importType,oldType;

	// encode generator
	private int statementType;

	// for statementType = MIXUP or FILTER
	private Mixup mixupExpr=null;

	// for statementType TRIE
	private List<String> phraseList;

	// for statementType = REGEX
	private String regex=null;

	private int regexGroup;

	// for statementType=PROVIDE,REQUIRE,ANNOTATEWITH,DICTIONARY
	private String annotationType,fileToLoad;

	private List<String> filesToLoad;

	// for parsing
//	private Matcher matcher;

	// for TRIE
	private int lastTokenStart;

	private String input;

	private static Set<String> generatorStart=new HashSet<String>();

	private static Set<String> legalKeywords=new HashSet<String>();

	private static Set<String> colonEqualsOrCase=new HashSet<String>();

	private static Set<String> defLevelType=new HashSet<String>();
	static{
		legalKeywords.add("defTokenProp");
		legalKeywords.add("defSpanProp");
		legalKeywords.add("defSpanType");
		legalKeywords.add("defDict");
		legalKeywords.add("declareSpanType");
		legalKeywords.add("provide");
		legalKeywords.add("require");
		legalKeywords.add("defLevel");
		legalKeywords.add("onLevel");
		legalKeywords.add("offLevel");
		legalKeywords.add("importFromLevel");
	}
	static{
		colonEqualsOrCase.add(":");
		colonEqualsOrCase.add("=");
		colonEqualsOrCase.add("case");
	}
	static{
		generatorStart.add(":");
		generatorStart.add("~");
		generatorStart.add("-");
	}
	static{
		defLevelType.add("re");
		defLevelType.add("split");
		defLevelType.add("filter");
		defLevelType.add("pseudotoken");
	}

	//
	// constructor and parser
	//
	Statement(Mixup.MixupTokenizer tok,String firstTok)
			throws Mixup.ParseException{
		keyword=firstTok;
		if(keyword.equals("declareSpanType")){
			statementType=DECLARE;
			type=tok.advance(null);
			tok.advance(null); // advance to end-of-statement marker
			return;
		}
		if(keyword.equals("provide")){
			statementType=PROVIDE;
			annotationType=tok.advance(null);
			if(annotationType.charAt(0)=='\''){
				annotationType=annotationType.substring(1,annotationType.length()-1);
			}
			// added to parse ";" -frank
			tok.advance(null);
			return;
		}
		if(keyword.equals("annotateWith")){
			statementType=ANNOTATE_WITH;
			fileToLoad=tok.advance(null);
			if(fileToLoad.charAt(0)=='\''){
				fileToLoad=fileToLoad.substring(1,fileToLoad.length()-1);
			}
			tok.advance(null);
			return;
		}
		if(keyword.equals("require")){
			statementType=REQUIRE;
			annotationType=tok.advance(null);
			if(annotationType.charAt(0)=='\''){
				annotationType=annotationType.substring(1,annotationType.length()-1);
			}
			String marker=tok.advance(null); //Collections.singleton(","));
			log.debug("marker: "+marker);
			if(marker!=null){
				fileToLoad=tok.advance(null);
				if(fileToLoad.charAt(0)=='\'')
					fileToLoad=fileToLoad.substring(1,fileToLoad.length()-1);
				tok.advance(null);
			}
			return;
		}
		if("onLevel".equals(keyword)||"offLevel".equals(keyword)){
			level=tok.advance(null);
			tok.advance(null);
			return;
		}
		if("importFromLevel".equals(keyword)){
			importLevel=tok.advance(null);
			// continue to parse NEWTYPE = OLDTYPE
			importType=tok.advance(null); // read property or type
			tok.advance(Collections.singleton("="));
			oldType=tok.advance(null);
			tok.advance(null); // advance to end-of-statement marker
			return;
		}
		String propOrType=tok.advance(null); // read property or type
		//        importType = propOrType;
		String token=tok.advance(colonEqualsOrCase); // read ':' or '='
		if(":".equals(token)){
			if(!"defSpanProp".equals(keyword)&&!"defTokenProp".equals(keyword)){
				parseError("can't define properties here");
			}
			property=propOrType;
			type=null;
			value=tok.advance(null);
			tok.advance(Collections.singleton("="));
		}else if("case".equals(token)){
			if(!"defDict".equals(keyword))
				parseError("illegal keyword usage");
		}else{
			// token is '='
			if(!"defSpanType".equals(keyword)&&!"defDict".equals(keyword)&&
					!"defLevel".equals(keyword)){
				parseError("illegal keyword usage");
			}
			if(!"=".equals(token)){

				parseError("expected '='");
			}
			type=propOrType;
			property=null;
		}

		if("defDict".equals(keyword)){
			// syntax is "defDict [+case] dictName = ", so either
			// propOrType = dictName and token = '=', or else 
			// propOrType = + and token = 'case', or else 
			ignoreCase=true;
			if("case".equals(token)){
				ignoreCase=false;
				if(!"+".equals(propOrType))
					parseError("illegal defDict");
				type=tok.advance(null);
				tok.advance(Collections.singleton("="));
			}else{
				type=propOrType;
			}
			wordSet=new HashSet<String>();
			filesToLoad=new ArrayList<String>();
			while(true){
				String w=tok.advance(null);
				// read in each line of the file name embraced by double quotes	
				if(w.equals("\"")){
					StringBuffer defFile=new StringBuffer("");
					while(!(w=tok.advance(null)).equals("\""))
						defFile.append(w);
					fileToLoad=defFile.toString();
					filesToLoad.add(fileToLoad);
				}else{
					wordSet.add(ignoreCase?w.toLowerCase():w);
				}
				String sep=tok.advance(null);
				if(sep==null)
					break;
				else if(!",".equals(sep))
					parseError("expected comma");
			}
		}else if("defLevel".equals(keyword)){
			split=tok.advance(defLevelType);
			patt=tok.advance(null);
			if(patt.charAt(0)=='\''&&patt.charAt(patt.length()-1)=='\'')
				patt=patt.substring(1,patt.length()-1);
			tok.advance(null);
		}else{
			// GEN
			// should be at '=' sign or starttype
			token=tok.advance(null);
			if(generatorStart.contains(token)){
				startType="top";
			}else{
				startType=token;
				token=tok.advance(generatorStart);
			}
			if(token.equals(":")){
				statementType=MIXUP;
				//mixupExpr = new Mixup( tok.input.substring(tok.matcher.end(1),tok.input.length()) );
				//if(tok.advance())
				if(tok.advance())
					mixupExpr=new Mixup(tok);
			}else if(token.equals("-")){
				statementType=FILTER;
				//mixupExpr = new Mixup( tok.input.substring(tok.matcher.end(1),tok.input.length()) );
				//if(tok.advance())		    
				if(tok.advance())
					mixupExpr=new Mixup(tok);
			}else if(token.equals("~")){
				token=tok.advance(null);
				if("re".equals(token)){
					statementType=REGEX;
					regex=tok.advance(null);
					if(regex.startsWith("'")){
						regex=regex.substring(1,regex.length()-1);
						regex=regex.replaceAll("\\\\'","'");
					}
					token=tok.advance(Collections.singleton(","));
					token=tok.advance(null);
					try{
						regexGroup=Integer.parseInt(token);
						token=tok.advance(null);
					}catch(NumberFormatException e){
						parseError("expected a regex group number and saw "+token);
					}
				}else if("trie".equals(token)){
					statementType=TRIE;
					phraseList=new ArrayList<String>();
					String word=tok.advance(null);
					word.trim();
					String fullWord="";
					while(word!=null){
						if(!word.equals(",")){
							fullWord=fullWord+word+" ";
						}else{
							fullWord.trim();
							phraseList.add(fullWord);
							fullWord="";
						}
						word=tok.advance(null);
					}
					phraseList.add(fullWord);
					//String[] phrases = (String[])phraseList.toArray();
				}else{
					parseError("expected 're' or 'trie'");
				}
			}else{
				throw new IllegalStateException("unexpected generatorStart '"+token+"'");
			}
		}
	}

	/** convert a set to a string listing the elements */
//	private String setContents(Set set){
//		StringBuffer buf=new StringBuffer("");
//		for(Iterator i=set.iterator();i.hasNext();){
//			if(buf.length()>0)
//				buf.append(" ");
//			buf.append("'"+i.next().toString()+"'");
//		}
//		return buf.toString();
//	}

	// an error message
	private String parseError(String msg) throws Mixup.ParseException{
		throw new Mixup.ParseException("statement error at char "+lastTokenStart+
				": "+msg+"\nin '"+input+"'");
	}

	public String toString(){
		if("defDict".equals(keyword)||"defLevel".equals(keyword)){
			return keyword+" "+type+" = ... ";
		}else if("onLevel".equals(keyword)||"offLevel".equals(keyword)){
			return keyword+" "+level;
		}else if("importFromLevel".equals(keyword)){
			return keyword+" "+importLevel+" "+importType+" = "+oldType;
		}else if(statementType==DECLARE){
			return keyword+" "+type;
		}else if(statementType==PROVIDE){
			return keyword+" "+annotationType;
		}else if(statementType==REQUIRE){
			return keyword+" "+annotationType+","+fileToLoad;
		}else if(statementType==ANNOTATE_WITH){
			return keyword+" "+fileToLoad;
		}else{
			String genString="???";
			if(statementType==MIXUP){
				genString=": "+mixupExpr.toString();
			}else if(statementType==FILTER){
				genString="- "+mixupExpr.toString();
			}else if(statementType==REGEX){
				genString="~ re '"+regex+"' ,"+regexGroup;
			}else if(statementType==TRIE){
				genString="~ trie ...";
			}
			if(type!=null){
				return keyword+" "+type+" ="+startType+genString;
			}else{
				return keyword+" "+property+":"+value+" ="+startType+genString;
			}
		}
	}

	//
	// From here down are public accessors to the properties of this Statement.  In the future
	// this should be changed to use a better data store for less cumbersome access
	//

	/**
	 * Returns an integer representing the type this Statement is.  Valid types are:
	 * DECLARE, PROVIDE, REQUIRE, ANNOTATE_WITH, MIXUP, FILTER, REGEX, and TRIE.
	 */
	public int getStatementType(){
		return statementType;
	}

	/**
	 * Returns the keyword that defines what this Statement does.
	 */
	public String getKeyword(){
		return keyword;
	}

	/**
	 * Returns a list of the files that need to be loaded if this Statement
	 * defines a dictionary.
	 */
	public List<String> getFilesToLoad(){
		return filesToLoad;
	}

	/**
	 * Returns the file that needs to be loaded in this Statement is an
	 * ANNOTATE_WITH or REQUIRE statement.
	 */
	public String getFileToLoad(){
		return fileToLoad;
	}

	/**
	 * Returns the type that this Statement matches.
	 */
	public String getType(){
		return type;
	}

	/**
	 * Returns the property that this statement matches
	 */
	public String getProperty(){
		return property;
	}

	/**
	 * Returns the value that this statement will match.
	 */
	public String getValue(){
		return value;
	}

	/**
	 * Returns whether or not this statement will ignore case when defining a dictionary.
	 */
	public boolean getIgnoreCase(){
		return ignoreCase;
	}

	/**
	 * Returns the set of words defining a dictionary in the case that this statement
	 * defines a dictionary inline.
	 */
	public Set<String> getWordSet(){
		return wordSet;
	}

	/**
	 * Returns the type of level to create when this Statement is defining a level.
	 */
	public String getSplit(){
		return split;
	}

	/**
	 * Returns the pattern that is used to create a new level when this statement
	 * is defining a new level.
	 */
	public String getPatt(){
		return patt;
	}

	/**
	 * Returns the level name to be used when this statement is performing a level
	 * operation (onLevel, offLeve, defLevel, importFromLevel)
	 */
	public String getLevel(){
		return level;
	}

	/**
	 * Returns the type from the source level that should be imported when this statement
	 * executes an importFromLevel call.
	 */
	public String getOldType(){
		return oldType;
	}

	/**
	 * Returns the type that imported spans should be called when this statement 
	 * executes an importFromLevel call.
	 */
	public String getImportType(){
		return importType;
	}

	/**
	 * Returns the level that this statement will import from in a call to importFromLevel.
	 */
	public String getImportLevel(){
		return importLevel;
	}

	/**
	 * Returns the type that this statement either provides or requires.
	 */
	public String getAnnotationType(){
		return annotationType;
	}

	/**
	 * Returns the starting type in the case that this statement is a generator statement.
	 */
	public String getStartType(){
		return startType;
	}

	/**
	 * Returns the mixup expression that this statement will execute.
	 */
	public Mixup getMixupExpr(){
		return mixupExpr;
	}

	/**
	 * Returns the phrase list for when this statement will define a trie.
	 */
	public List<String> getPhraseList(){
		return phraseList;
	}

	/**
	 * Returns the regex string that will be executed by this statement.
	 */
	public String getRegex(){
		return regex;
	}

	/**
	 * Returns the regex group that will be returned when this statement executes.
	 */
	public int getRegexGroup(){
		return regexGroup;
	}
}
