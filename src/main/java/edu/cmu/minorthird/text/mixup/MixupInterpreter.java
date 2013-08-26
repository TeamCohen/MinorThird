package edu.cmu.minorthird.text.mixup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.RegexTokenizer;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.SpanTypeTokenizer;
import edu.cmu.minorthird.text.SplitTokenizer;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextBaseManager;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextToken;
import edu.cmu.minorthird.text.Tokenizer;

public class MixupInterpreter{

	private static Logger log=Logger.getLogger(MixupInterpreter.class);

	private MixupProgram program=null;

	private Map<String,MonotonicTextLabels> levelsToLabelsMap=
			new HashMap<String,MonotonicTextLabels>();

	private Stack<String> levelStack=new Stack<String>();

	private TextBaseManager tbManager;

	// Constructors
	public MixupInterpreter(){
		;
	}

	public MixupInterpreter(MixupProgram p){
		program=p;
	}

	public MixupInterpreter(MixupProgram p,MonotonicTextLabels rootLabels){
		program=p;
		tbManager=new TextBaseManager("root",rootLabels.getTextBase());
		levelsToLabelsMap.put("root",rootLabels);
		levelStack.push("root");
	}

	/** Effectively clears the current state and executes the current program on the
	 * specified TextLabels.  The specified labels will become the root level of the
	 * new labels hierarchy.
	 */
	public void eval(MonotonicTextLabels labels){
		// Check to make sure that there is an actual program to evaluate
		if(program==null)
			throw new IllegalStateException(
					"You must set the MixupProgram prior to calling eval.");

		// Clear out the state and start fresh with the specified labels
		tbManager=new TextBaseManager("root",labels.getTextBase());
		levelsToLabelsMap.clear();
		levelsToLabelsMap.put("root",labels);
		levelStack=new Stack<String>();
		levelStack.push("root");

		// Evaluate the program on this new state.
		this.eval();
	}

	/** Runs the current program on the current state of labels (current level).  This
	 * method is useful if you have already executed a program, called setProgram() and 
	 * want to run the new progam against the current state of the labels.
	 */
	public void eval(){
		// Check to make sure that there is an actual program to evaluate
		if(program==null)
			throw new IllegalStateException(
					"You must set the MixupProgram prior to calling eval.");

		// Make sure that at least a root level of labels has been specified
		if(this.getCurrentLevel()==null)
			throw new IllegalStateException(
					"There is no TextLabels heirarchy.  You must call eval(TextLabels) instead.");

		// If everything is in place, go ahead and evaluate the program statements on the current state.
		Statement[] statementList=program.getStatements();
		for(int i=0;i<statementList.length;i++){
			this.evaluate(statementList[i]);
		}
	}

	/** Sets the MixupProgram that this interpreter will execute when the eval method is called. */
	public void setProgram(MixupProgram p){
		program=p;
	}

	/** Returns the MixupProgram that this interpreter will execute if the eval method is called. */
	public MixupProgram getProgram(){
		return program;
	}

	/** Returns the TextLabels associated with the given level name or null if the level doesn't exist */
	public MonotonicTextLabels getLabelsForLevel(String level){
		return levelsToLabelsMap.get(level);
	}

	/** Returns the name of the current level or null if no TextLabels have been added or created because 
	 *  the program has not yet been evaluated. */
	public String getCurrentLevel(){
		if(levelStack.empty())
			return null;
		return levelStack.peek();
	}

	/** Returns the TextLabels associated with the current level */
	public MonotonicTextLabels getCurrentLabels(){
		return getLabelsForLevel(getCurrentLevel());
	}

	/** Makes the current level be the given level name.  Throws an exception if the given level name doesn't exist. */
	public void onLevel(String levelName){
		if(levelsToLabelsMap.get(levelName)==null)
			throw new IllegalArgumentException("There is no level named '"+levelName+
					"'");
		else
			levelStack.push(levelName);
	}

	/** Moves up one level in the stack of labels */
	public void offLevel(String levelName){
		if(levelStack.size()==1)
			throw new IllegalArgumentException("Already at the top level.");
		else if(!(levelStack.peek()).equals(levelName))
			throw new IllegalArgumentException("Not on level named '"+levelName+"'");
		else{
			levelStack.pop();
		}
	}

	/**
	 * Creates a new level.  This specified level type indicates what kind of level to be created.  Allowed types are: 
	 * pseudotoken, filter, re, and split. <br>
	 * <br>
	 * pseudotoken - This type of level creates a new text base with a different tokenization scheme.  The new scheme is
	 * the same as the original scheme except that tokens in the span type specified in pattern are all merged into a single
	 * token. <br>
	 * <br>
	 * filter - This type of level creates a new text base that only contains the text inside instances of the span type
	 * specified in pattern.  Each instance is placed in a separate document in the new text base. <br>
	 * <br>
	 * re - This type of level creates a new text base with a new tokenization scheme.  In this case the tokenization 
	 * scheme is defined by the regular expression specified in pattern.  Only matches to this regex are considered 
	 * tokens in the new text base. <br>
	 * <br>
	 * split - This type of level is similar to re except that matches to the regex are used to separate the tokens.  That 
	 * is everything in between matches is considered a single token.
	 */
	public void createLevel(String newLevelName,String levelType,String pattern){
		TextBase newTextBase=null;
		BasicTextLabels newLabels=null;
		String currentLevel=this.getCurrentLevel();
		MonotonicTextLabels parentLabels=
				levelsToLabelsMap.get(currentLevel);

		// Create a textBase where spans of a certain type are combined into a sigle token 
		if("pseudotoken".equals(levelType)){
			// First create the tokenizer
			SpanTypeTokenizer tokenizer=new SpanTypeTokenizer(pattern,parentLabels);
			// Next create the retokenized text base
			newTextBase=tbManager.retokenize(tokenizer,currentLevel,newLevelName);
			// Finally create the labels and add in the pseudotoken token properties.
			newLabels=new BasicTextLabels(newTextBase);
			Iterator<Span> typeInstances=parentLabels.instanceIterator(pattern);
			while(typeInstances.hasNext()){
				Span currInstance=typeInstances.next();
				Span matchingChildSpan=
						tbManager.getMatchingSpan(currInstance,currentLevel,newLevelName);
				for(int i=0;i<matchingChildSpan.size();i++){
					newLabels.setProperty(matchingChildSpan.getTextToken(i),
							"Pseudotoken","1");
				}
			}
		}
		// creates a textBase which filters out all spans not of a certain spanType
		else if("filter".equals(levelType)){
			newTextBase=
					tbManager.filter(currentLevel,parentLabels,newLevelName,pattern);
			newLabels=new BasicTextLabels(newTextBase);
		}
		// Creates a new text base retokenized by the given pattern
		else if("re".equals(levelType)||"split".equals(levelType)){
			Tokenizer tokenizer;
			if(levelType.equals("split")) // creates a tokenizer that splits the textBase at a certain token (e.g. split at ".")
				tokenizer=new SplitTokenizer(pattern);
			else
				tokenizer=new RegexTokenizer(pattern); //only things matching pattern will be tokens.
			newTextBase=tbManager.retokenize(tokenizer,currentLevel,newLevelName);
			newLabels=new BasicTextLabels(newTextBase);
		}else{
			throw new IllegalArgumentException("No level type: "+levelType+
					" new level created with old textBase and Labels");
		}

		// Add the new TextLabels to the list of labels for each level
		levelsToLabelsMap.put(newLevelName,newLabels);
	}

	/** imports labels from specified level to the current level */
	public void importLabelsFromLevel(String importLevel,String oldType,
			String newType){
		if(!tbManager.containsLevel(importLevel)){
			throw new IllegalArgumentException("Level: "+importLevel+
					" not defined for importFromLevel");
		}

		MonotonicTextLabels oldLabels=levelsToLabelsMap.get(importLevel);
		MonotonicTextLabels currLabels=this.getCurrentLabels();
		Iterator<Span> instances=oldLabels.instanceIterator(oldType);
		while(instances.hasNext()){
			Span currInstance=instances.next();
			Span newSpan=
					tbManager.getMatchingSpan(currInstance,importLevel,this
							.getCurrentLevel());
			currLabels.addToType(newSpan,newType);
		}
	}

	//
	// Evaluates a Statement instance against the current level's label set.
	//
	private void evaluate(Statement statement){
		log.info("Evaluating: "+statement);
		long start=System.currentTimeMillis();

		MonotonicTextLabels labels=this.getCurrentLabels();

		// The properties of this statement        
		int statementType=statement.getStatementType();
		String keyword=statement.getKeyword();
		List<String> filesToLoad=statement.getFilesToLoad();
		String fileToLoad=statement.getFileToLoad();
		String type=statement.getType();
		boolean ignoreCase=statement.getIgnoreCase();
		Set<String> wordSet=statement.getWordSet();
		String split=statement.getSplit();
		String patt=statement.getPatt();
		String level=statement.getLevel();
		String oldType=statement.getOldType();
		String importType=statement.getImportType();
		String importLevel=statement.getImportLevel();
		String annotationType=statement.getAnnotationType();
		String startType=statement.getStartType();
		Mixup mixupExpr=statement.getMixupExpr();
		List<String> phraseList=statement.getPhraseList();
		String regex=statement.getRegex();
		int regexGroup=statement.getRegexGroup();

		if("defDict".equals(keyword)){
			if(filesToLoad.size()>0){
				labels.defineDictionary(type,filesToLoad,ignoreCase);
				filesToLoad.clear();
			}else{
				log.debug("defining dictionary of: "+wordSet);
				labels.defineDictionary(type,wordSet);
			}
		}else if("defLevel".equals(keyword)){
			this.createLevel(type,split,patt);
		}else if("onLevel".equals(keyword)){
			this.onLevel(level);
		}else if("offLevel".equals(keyword)){
			this.offLevel(level);
		}else if("importFromLevel".equals(keyword)){
			this.importLabelsFromLevel(importLevel,oldType,importType);
		}else if("declareSpanType".equals(keyword)){
			labels.declareType(type);
		}else if(statementType==Statement.PROVIDE){
			labels.setAnnotatedBy(annotationType);
		}else if(statementType==Statement.REQUIRE){
			labels.require(annotationType,fileToLoad);
		}else if(statementType==Statement.ANNOTATE_WITH){
			labels.annotateWith(fileToLoad.substring(0,fileToLoad.length()-4),
					fileToLoad);
		}else{
			Iterator<Span> input=null;
			if("top".equals(startType)){
				input=labels.getTextBase().documentSpanIterator();
			}else if(labels.isType(startType)){
				input=labels.instanceIterator(startType);
			}else{
				throw new IllegalStateException("no type '"+startType+"' defined");
			}
			if(statementType==Statement.MIXUP){
				for(Iterator<Span> i=mixupExpr.extract(labels,input);i.hasNext();){
					Span span=i.next();
					extendLabels(labels,span,statement);
				}
				// make sure type is declared, even if nothing happened to be defined here
				if("defSpanType".equals(keyword)){
					labels.declareType(type);
				}
			}else if(statementType==Statement.FILTER){
				SortedSet<Span> accum=new TreeSet<Span>();
				for(Iterator<Span> i=input;i.hasNext();){
					Span span=i.next();
					if(!hasExtraction(mixupExpr,labels,span)){
						accum.add(span);
					}
				}
				for(Iterator<Span> i=accum.iterator();i.hasNext();){
					extendLabels(labels,i.next(),statement);
				}
			}else if(statementType==Statement.TRIE){
				labels.defineTrie(phraseList);
				while(input.hasNext()){
					Span span=input.next();
					Iterator<Span> output=labels.getTrie().lookup(span);
					while(output.hasNext()){
						extendLabels(labels,output.next(),statement);
					}
				}
			}else if(statementType==Statement.REGEX){
				Pattern pattern=Pattern.compile(regex);
				while(input.hasNext()){
					Span span=input.next();
					// Don't use this method as it drops leading and trailing spaces from the document text.
					//Matcher matcher = pattern.matcher( span.asString() );
					Matcher matcher=pattern.matcher(span.getDocumentContents());
					while(matcher.find()){
						try{
							Span subspan=
									span.charIndexProperSubSpan(matcher.start(regexGroup),matcher
											.end(regexGroup));
							extendLabels(labels,subspan,statement);
						}catch(IllegalArgumentException ex){
							/* there is no subspan that is properly contained by the regex match,
							   so don't add anything */
						}
					}
				}
			}else{
				throw new IllegalStateException("illegal statement type "+statementType);
			}
		}
		long end=System.currentTimeMillis();
		log.info("time: "+((end-start)/1000.0)+" sec");
	}

	// subroutine of eval - check if a mixup expression matches
	private boolean hasExtraction(final Mixup mixupExpr,final TextLabels labels,
			final Span span){
		Iterator<Span> input=Collections.singleton(span).iterator();
		Iterator<Span> output=mixupExpr.extract(labels,input);
		return output.hasNext();
	}

	// subroutine of eval - label the span  
	private void extendLabels(MonotonicTextLabels labels,Span span,
			Statement statement){
		String keyword=statement.getKeyword();
		String type=statement.getType();
		String property=statement.getProperty();
		String value=statement.getValue();

		if("defSpanType".equals(keyword))
			labels.addToType(span,type);
		else if("defSpanProp".equals(keyword))
			labels.setProperty(span,property,value);
		else if("defTokenProp".equals(keyword)){
			for(int j=0;j<span.size();j++){
				TextToken token=span.getTextToken(j);
				if(property==null)
					throw new IllegalStateException("null property");
				labels.setProperty(token,property,value);
			}
		}
	}
}
