/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.mixup;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.ProgressCounter;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Modify a textEnv using a series of mixup expressions.

<pre>
BNF:
  STATEMENT -> provide ID
  STATEMENT -> require ID,FILE
  STATEMENT -> defDict NAME = ID, ... , ID
  STATEMENT -> defTokenProp PROP:VALUE = GEN
	STATEMENT -> defSpanProp PROP:VALUE = GEN
	STATEMENT -> defSpanType TYPE2 = GEN
	STATEMENT -> declareSpanType TYPE
	
	GEN -> [TYPE]: MIXUP-EXPR
	GEN -> [TYPE]- MIXUP-EXPR
	GEN -> [TYPE]~ re 'REGEX',NUMBER
	GEN -> [TYPE]~ trie phrase1, phrase2, ... ;

	statements are semicolon-separated // and comments look like this

SEMANTICS:
  execute each command in order, saving spans/tokens as types, and asserting properties
	'=:' can be replaced with '=TYPE:', in which case the expr will be applied to
	 each span of the given type, rather than all top-level spans

</pre>
 *
 * @author William Cohen
*/

public class MixupProgram 
{
	private static Logger log = Logger.getLogger(MixupProgram.class);

	private ArrayList statementList = new ArrayList();
	// maps dictionary names to the sets they correspond to 
	private HashMap dictionaryMap = new HashMap();

	public MixupProgram() {;}

	/** Create a MixupProgram from an array of statements */
	public MixupProgram(String[] statements) throws Mixup.ParseException {
		for (int i=0; i<statements.length; i++) {
			addStatement( statements[i] );
		}
	}

	/** Create a MixupProgram from single string with a bunch of semicolon-separated statements. */
	public MixupProgram(String program) throws Mixup.ParseException {
		addStatements(program);
	}

	/** Create a MixupProgram from the contents of a file. */
	public MixupProgram(File file) throws Mixup.ParseException, FileNotFoundException, IOException {
		//LineNumberReader in = new LineNumberReader(new FileReader(file));
		LineNumberReader in = mixupReader(file);
		StringBuffer buf = new StringBuffer();
		String line;
		while ((line = in.readLine())!=null) {
//			int startComment = line.indexOf("//");
//			if (startComment>=0) line = line.substring(0,startComment);
			buf.append(line);
			buf.append("\n");
		}
		in.close();
		addStatements( buf.toString() );
	}

	/** Evaluate the program against an existing environment. */
	public void eval(MonotonicTextEnv env,TextBase textBase) {
		ProgressCounter pc = new ProgressCounter("mixup program","statement",statementList.size());
		for (int i=0; i<statementList.size(); i++) {
			((Statement)statementList.get(i)).eval(env,textBase);
			pc.progress();
		}
		pc.finished();
	}

	/** Add a single statement to the current mixup program. */
  public void addStatement(String statement) throws Mixup.ParseException
  {
//    log.debug("add statement: " + statement);
    statement = statement.trim();
    if (statement.length() > 0)
    {
      if (!statement.startsWith("//")) //skip comments
      {
//        log.debug("inserting: " + statement);
        statementList.add(new Statement(statement));
      }
    }
  }

	/** Add a bunch of ';'-separated statements
   *
   *
   * Splits into lines based on \n.
   * Add a line to the current
   * Removes anything after //
   * Checks if the current line has a ';'
   * If so split the line into statements on ';', send all statements to addStatement
   * If not, carry the current line forward
   */
  public void addStatements(String program) throws Mixup.ParseException
  {
    String[] lines = program.split("\\n");
    String line = "";
    for (int i = 0; i < lines.length; i++)
    {
      line += lines[i];
      int commentStart = line.indexOf("//");
      if (commentStart > -1)
        line = line.substring(0, commentStart);

      if (line.indexOf(';') > 0)
      {
        String[] statements = line.split(";");
        for (int j = 0; j < statements.length; j++)
        { addStatement(statements[j]); }

        line = "";
      }
    }

/*    int startStatement = 0;
    int endStatement = program.indexOf(';');
    if (endStatement < 0) endStatement = program.length();
    while (startStatement < program.length())
    {
      addStatement(program.substring(startStatement, endStatement));
      startStatement = endStatement + 1;
      endStatement = program.indexOf(';', startStatement);
      if (endStatement < 0) endStatement = program.length();
    }
*/
  }

	/** List the program **/
	public String toString() {
		StringBuffer buf = new StringBuffer("");
		for (int i=0; i<statementList.size(); i++) {
			buf.append(statementList.get(i).toString()+";\n");
		}
		return buf.toString();
	}
	
	//
	// encodes a single program statement
	//
	private static class Statement {
		private static int REGEX=1, MIXUP=2, FILTER=3, PROVIDE=4, REQUIRE=5, DECLARE=6, TRIE=7;

		// encodes the statement properties
		private String keyword, property, type, startType, value;
		// set of words, for a dictionary
		private Set wordSet = null;
		// encode generator
		private int statementType;
		// for statementType = MIXUP or FILTER
		private Mixup mixupExpr = null;
		// for statementType = REGEX
		private String regex = null;
		private int regexGroup;
		// for statementType = TRIE
		private Trie trie = null;
		// for statementType=PROVIDE,REQUIRE
		private String annotationType,fileToLoad;
		// for parsing
		private Matcher matcher;
		private int lastTokenStart;
		private String input;
		private static Set generatorStart = new HashSet();
		private static Set legalKeywords = new HashSet(); 
		private static Set colonOrEquals = new HashSet();
		static { 
			legalKeywords.add("defTokenProp"); 
			legalKeywords.add("defSpanProp"); 
			legalKeywords.add("defSpanType"); 
			legalKeywords.add("defDict"); 
			legalKeywords.add("declareSpanType"); 
			legalKeywords.add("provide"); 
			legalKeywords.add("require"); 
		}
		static { colonOrEquals.add(":"); colonOrEquals.add("="); }
		static { generatorStart.add(":"); generatorStart.add("~"); generatorStart.add("-"); }
		//
		// constructor and parser
		//
		Statement(String input) throws Mixup.ParseException 
		{
			this.input = input;
			this.matcher = Mixup.tokenizerPattern.matcher(input);
			keyword = advance(legalKeywords); // read 
			if (keyword.equals("declareSpanType")) {
				statementType = DECLARE;
				type = advance(null);
				return;
			}
			if (keyword.equals("provide")) {
				statementType = PROVIDE;
				annotationType = advance(null);
				if (annotationType.charAt(0)=='\'') {
					annotationType = annotationType.substring(1,annotationType.length()-1);
				}
				return;
			}
			if (keyword.equals("require")) {
				statementType = REQUIRE;
				annotationType = advance(null);
				if (annotationType.charAt(0)=='\'') {
					annotationType = annotationType.substring(1,annotationType.length()-1);
				}
				String marker = advance(null); //Collections.singleton(","));
				log.debug("marker: " + marker);
        if (marker != null)
        {
          fileToLoad = advance(null);
          if (fileToLoad.charAt(0) == '\'')
            fileToLoad = fileToLoad.substring(1, fileToLoad.length() - 1);
        }
        return;
			}
			String propOrType = advance(null);  // read property or type
			String token = advance(colonOrEquals); // read ':' or '='
			if (":".equals(token)) {
				if (!"defSpanProp".equals(keyword) && !"defTokenProp".equals(keyword)) {
					parseError("can't define properties here");
				}
				property = propOrType; type = null;
				value = advance(null);
				advance(Collections.singleton("="));
			} else {
				if (!"defSpanType".equals(keyword) && !"defDict".equals(keyword)) {
					parseError("illegal keyword usage");
				}
				if (!"=".equals(token)) parseError("expected '='");
				type = propOrType; property = null;
			}

			if ("defDict".equals(keyword)) {
				type = propOrType;
				wordSet = new HashSet();
				while (true) {
					String w =  advance(null);
					// read in each line of the file name embraced by double quotes	
					if (w.equals("\"")) {
						StringBuffer defFile = new StringBuffer("");
						while (!(w = advance(null)).equals("\""))
							defFile.append(w);
						try {
							//BufferedReader bReader = new BufferedReader(new FileReader(defFile.toString()));
							LineNumberReader bReader = mixupReader(defFile.toString());
							String s = null;
							while ((s = bReader.readLine()) != null)
								wordSet.add( s.toLowerCase() );
							bReader.close();
						} catch (IOException ioe) {
							parseError("Error when reading " + defFile.toString() + ": " + ioe);
						}
					} else wordSet.add( w );
					String sep = advance(null);
					if (sep==null) break;
					else if (!",".equals(sep)) parseError("expected comma");
				}
			} else {
				// should be at '=' sign or starttype
				token = advance(null); 
				if (generatorStart.contains(token)) {
					startType = "top";
				} else {
					startType = token;
					token = advance( generatorStart );
				}
				if (token.equals(":")) {
					statementType = MIXUP;
					mixupExpr = new Mixup( input.substring(matcher.end(1),input.length()) );
				} else if (token.equals("-")) {
					statementType = FILTER;
					mixupExpr = new Mixup( input.substring(matcher.end(1),input.length()) );
				} else if (token.equals("~")) {
					token = advance(null);
					if ("re".equals(token)) {
						statementType = REGEX;
						regex = advance(null);
						if (regex.startsWith("'")) {
							regex = regex.substring(1,regex.length()-1);
						}
						token = advance(Collections.singleton(","));
						token = advance(null);
						try {
							regexGroup = Integer.parseInt(token);
						} catch (NumberFormatException e) {
							parseError("expected a regex group number and saw "+token);
						}
					} else if ("trie".equals(token)) {
						statementType = TRIE;
						String trieString = input.substring(matcher.end(1),input.length());
						String[] phrases = trieString.split("\\s*,\\s*");
						trie = new Trie();
						BasicTextBase tokenizerBase = new BasicTextBase();
						for (int i=0; i<phrases.length; i++) {
							String[] toks = tokenizerBase.splitIntoTokens(phrases[i]);
							if (toks.length<=2 || !"\"".equals(toks[0]) || !"\"".equals(toks[toks.length-1])) {
								trie.addWords( "phrase#"+i, toks );
							} else {
								StringBuffer defFile = new StringBuffer("");
								for (int j=1; j<toks.length-1; j++) defFile.append(toks[j]);
								try {
									//BufferedReader bReader = new BufferedReader(new FileReader(defFile.toString()));
									LineNumberReader bReader = mixupReader(defFile.toString());
									String s = null;
									int line=0;
									while ((s = bReader.readLine()) != null) {
										line++;
										String[] words = tokenizerBase.splitIntoTokens(s);
										trie.addWords(defFile+".line."+line, words);
									}
									bReader.close();
								} catch (IOException ioe) {
									parseError("Error when reading " + defFile.toString() + ": " + ioe);
								}
							} // file load 
						} // each phrase
					} else {
						parseError("expected 're' or 'trie'");
					}
				} else {
					throw new IllegalStateException("unexpected generatorStart '"+token+"'");
				}
			}
		}

		public void eval(MonotonicTextEnv env,TextBase textBase) {
			log.info("Evaluating: "+this);
			long start = System.currentTimeMillis();
			if ("defDict".equals(keyword)) {
				env.defineDictionary( type, wordSet );
			} else if ("declareSpanType".equals(keyword)) {
				env.declareType( type );
			} else if (statementType==PROVIDE) {
				env.setAnnotatedBy(annotationType);
			} else if (statementType==REQUIRE) {
				if (!env.isAnnotatedBy(annotationType)) {
          Dependencies.runDependency(env, annotationType, fileToLoad);
        }
			} else {
				Span.Looper input = null;
				if ("top".equals(startType)) {
					input = textBase.documentSpanIterator();
				} else if (env.isType(startType)) {
					input = env.instanceIterator(startType);
				} else {
					throw new IllegalStateException("no type '"+startType+"' defined");
				}
				if (statementType==MIXUP) {
					for (Span.Looper i=mixupExpr.extract(env,input); i.hasNext(); ) {
						Span span = i.nextSpan();
						extendEnv( env, span );
					}
				} else if (statementType==FILTER) {
					TreeSet accum = new TreeSet();
					for (Span.Looper i=input; i.hasNext(); ) {
						Span span = i.nextSpan();
						if (!hasExtraction(mixupExpr,env,span)) {
							accum.add( span );
						}
					}
					for (Iterator i=accum.iterator(); i.hasNext(); ) {
						extendEnv( env, ((Span)i.next()) );
					}
				} else if (statementType==TRIE) {
					while (input.hasNext()) {
						Span span = input.nextSpan();
						Span.Looper output = trie.lookup( span );
						while (output.hasNext()) {
							extendEnv( env, output.nextSpan() );
						}
					}
				} else if (statementType==REGEX) {
					Pattern pattern = Pattern.compile(regex); 
					while (input.hasNext()) {
						Span span = input.nextSpan();
						Matcher matcher = pattern.matcher( span.asString() );
						while (matcher.find()) {
							extendEnv(
								env, 
								span.charIndexProperSubSpan( matcher.start(regexGroup),matcher.end(regexGroup)));
						}
					}
				} else {
					throw new IllegalStateException("illegal statement type "+statementType);
				}
			}
			long end = System.currentTimeMillis();
			log.info("time: "+((end-start)/1000.0)+" sec");
		}


    // subroutine of eval - check if a mixup expression matches
		private boolean hasExtraction(final Mixup mixupExpr,final TextEnv env,final Span span) {
			Span.Looper input = new BasicSpanLooper(Collections.singleton(span));
			Span.Looper output = mixupExpr.extract(env,input);
			return output.hasNext();
		}
		// subroutine of eval - label the span  
		private void extendEnv(MonotonicTextEnv env,Span span) {
			if ("defSpanType".equals(keyword)) env.addToType(span,type);
			else if ("defSpanProp".equals(keyword)) env.setProperty(span,property,value);
			else if ("defTokenProp".equals(keyword)) {
				for (int j=0; j<span.size(); j++) {
					TextToken token = span.getTextToken(j);
					if (property==null) throw new IllegalStateException("null property");
					env.setProperty(token,property,value);
				}
			}
		}

		// advance to next token, and check that it's what's expected
    private String advance(Set set) throws Mixup.ParseException
    {
      if (!matcher.find())
      {
        lastTokenStart = input.length();
        if (set != null && set.size() == 1)
        {
          parseError("incompete statement: expected " + setContents(set));
        }
        else if (set != null)
        {
          parseError("incompete statement: expected one of " + setContents(set));
        }
        else
        {
          return null;
        }
      }

      lastTokenStart = matcher.start(1);
      String result = matcher.group(1);
      if (set != null && !set.contains(result))
      {
        parseError("statement error: expected one of: " + setContents(set) + " in " + result);
      }
      return result;
    }

		/** convert a set to a string listing the elements */
		private String setContents(Set set) {
			StringBuffer buf = new StringBuffer("");
			for (Iterator i = set.iterator(); i.hasNext(); ) {
				if (buf.length()>0) buf.append(" ");
				buf.append("'"+i.next().toString()+"'");
			}
			return buf.toString();
		}
		// an error message
		private String parseError(String msg) throws Mixup.ParseException {
			throw new Mixup.ParseException("statement error at char "+lastTokenStart+": "+msg+"\nin '"+input+"'");
		}
		public String toString() {
			if ("defDict".equals(keyword)) {
				return keyword + " " +type + " = ... ";
			} else if (statementType==DECLARE) {
				return keyword + " " + type;
			} else if (statementType==PROVIDE) {
				return keyword+" "+annotationType;
			} else if (statementType==REQUIRE) {
				return keyword+" "+annotationType+","+fileToLoad;
			} else {
				String genString = "???";
				if (statementType==MIXUP) {
					genString = ": "+mixupExpr.toString();
				} else if (statementType==FILTER) {
					genString = "- "+mixupExpr.toString();
				} else if (statementType==REGEX) {
					genString = "~ re '"+regex+"' ,"+regexGroup;
				} else if (statementType==TRIE) {
					genString = "~ trie '"+trie;
				}
				if (type!=null) {
					return keyword+" "+type+" ="+startType+genString;
				} else {
					return keyword+" "+property+":"+value+" ="+startType+genString;
				}
			}
		}
	}

	/** Convert a string to an input stream, then a LineNumberReader. */
	static private LineNumberReader mixupReader(String fileName) throws IOException, FileNotFoundException
	{
		File file = new File(fileName);
		if (file.exists()) return new LineNumberReader(new BufferedReader(new FileReader(file)));
		else {
			InputStream s = ClassLoader.getSystemResourceAsStream(fileName);
			return new LineNumberReader(new BufferedReader(new InputStreamReader(s)));
		}
	}
	static private LineNumberReader mixupReader(File file) throws IOException, FileNotFoundException
	{
		return mixupReader(file.getName());
	}


	//
	// interactive test routine
	//
	public static void main(String[] args) {
		try {
			MixupProgram program = new MixupProgram(new File(args[0]));
			System.out.println("program:\n" + program.toString());
			if (args.length>1) {
				TextBase base = new BasicTextBase();
				TextBaseLoader loader = new TextBaseLoader();
				loader.loadFile(base, new File(args[1]));
				MonotonicTextEnv env = loader.getFileMarkup();
				program.eval(env,base);
				for (Iterator i=env.getTypes().iterator(); i.hasNext(); ) {
					String type = (String)i.next();
					System.out.println("Type "+type+":");
					for (Span.Looper j=env.instanceIterator(type); j.hasNext(); ) {
						Span span = j.nextSpan();
						System.out.println( "\t'"+span.asString()+"'" );
					}
				}
			}
		} catch (Exception e) {
			System.out.println("usage: programFile [textFile]");
			e.printStackTrace();
		}
	}
}
