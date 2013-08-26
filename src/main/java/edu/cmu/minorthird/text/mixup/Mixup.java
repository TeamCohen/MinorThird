package edu.cmu.minorthird.text.mixup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.BoneheadStemmer;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.Token;
import edu.cmu.minorthird.util.ProgressCounter;

/** A simple pattern-matching and information extraction language.

 <pre>
 EXAMPLE:
 ... in('begin') @number? [ any{2,5} in('end') ] ... && [!in('begin')*] && [!in('end')*]

 BNF:
 simplePrim -> [!] simplePrim1
 simplePrim1 -> id | a(DICT) | ai(DICT) | eq(CONST) | eqi(CONST) | re(REGEX) 
 | any | ... | PROPERTY:VALUE  | PROPERTY:a(foo)  )
 prim -> < simplePrim [,simplePrim]* > | simplePrim
 repeatedPrim -> [L] prim [R] repeat | @type | @type?
 repeat -> {int,int} | {,int} | {int,} | {int} | ? | * | +
 pattern -> | repeatedPrim pattern
 basicExpr -> pattern [ pattern ] pattern 
 basicExpr -> (expr)
 expr -> basicExpr "||" expr 
 expr -> basicExpr "&&" expr

 SEMANTICS:
 basicExpr is pattern match - like a regex, but returns all matches, not just the longest one
 token-level tests:
 eq('foo') check token is exactly foo 
 'foo' is short for eq('foo')
 re('regex') checks if token matches the regex
 eqi('foo') check lowercase version of token is foo
 'foo' or eq('foo') checks a token is equal to 'foo'
 a(bar) checks a token is in dictionary 'bar'
 ai(bar) checks that the token is in dictionary 'bar', ignoring case
 color:red checks that the token has property 'color' set to 'red'
 color:a(primaryColor) checks that the token's  property 'color' is in the dictionary 'primaryColor'
 !test is negation of test
 <test1, test2, ... test3> conjoins token-level tests
 any is true for any token
 token-sequences:
 test? is 0 or 1 tokens matching test
 test+ is 1+ tokens matching test
 test* is 0+ tokens matching test
 test{3,7} is between 3 and 7 tokens matching test		
 ... is equal to any*
 <code>@foo</code> matches a span of type foo
 <code>@foo?</code> matches a span of type foo or the empty sequence
 L means sequence can't be extended to left and still match
 R means sequence can't be extended to right and still match
 expr || expr is union
 expr && expr is piping: generate with expr1, filter with expr2
 </pre>

 The name's an acronym for My Information eXtraction and Understanding Package.

 *
 * @author William Cohen
 */

public class Mixup implements Serializable{

	static private final long serialVersionUID=20080303L;

	/** Without constraints, the maximum number of times a mixup
	 * expression can extract something from a document of length N is
	 * O(N*N).  The maxNumberOfMatches... variables below constrain
	 * this behavior, for efficiency.  The variable below is a threshold
	 * after which these constraints kick in.
	 */
	public static int minMatchesToApplyConstraints=5000;

	/** Without constraints, the maximum number of times a mixup
	 * expression can extract something from a document of length N is
	 * O(N*N), since any token can be the begin or end of an extracted
	 * span.  The maxNumberOfMatchesPerToken value limits this to
	 * maxNumberOfMatchesPerToken*N.
	 */
	public static int maxNumberOfMatchesPerToken=5;

	/** Without constrains, the maximum number of times a mixup
	 * expression can extract something from a document of length N is
	 * O(N*N), since any token can be the begin or end of an extracted
	 * span.  This limits the number of matches to a fixed number.
	 */
	public static int maxNumberOfMatches=134217728; //2^27

	private static final boolean DEBUG=false;

	// tokenize: words, single-quoted strings, "&&", "||", "..." or single non-word chars
	public static final Pattern tokenizerPattern=Pattern.compile("\\s*((\\n)|(\\w+)|(\\/\\/)|('(\\\\'|[^\\'])*')|\\&\\&|\\|\\||\\.\\.\\.|\\\\\\;|\\W)\\s*");
	//Pattern.compile("\\s*(\\w+|'([^']|\\\\')*'|\\&\\&|\\|\\||\\.\\.\\.|\\W)\\s*");

	// legal functions
	private static Set<String> legalFunctions;
	static{
		legalFunctions=new HashSet<String>();
		String[] tmp=new String[]{"re","eq","eqi","a","ai","any","prop","propDict"};
		for(int i=0;i<tmp.length;i++)
			legalFunctions.add(tmp[i]);
	}

	private final static int RE=0;
	private final static int EQ=1;
	private final static int EQI=2;
	private final static int A=3;
	private final static int AI=4;
	private final static int ANY=5;
	private final static int PROP=6;
	private final static int PROPDICT=7;
	private final static int ELIPSE=9;

	private Expr expr;

	/** Create a new mixup query. */
	public Mixup(String pattern) throws ParseException{
		MixupTokenizer tok=new MixupTokenizer(pattern);
		if(tok.advance())
			expr=new MixupParser(tok).parseExpr();
	}

	public Mixup(MixupTokenizer tok) throws ParseException{
		expr=new MixupParser(tok).parseExpr();
	}

	/** Extract subspans from each generated span using the mixup expression.
	 */
	public Iterator<Span> extract(TextLabels labels,Iterator<Span> spanLooper){
		return expr.match(labels,spanLooper);
	}

	public String toString(){
		return expr.toString();
	}

	public static class MixupTokenizer{

		public String input;
		public Matcher matcher;
		private String token;
		public String nextToken;
		private int cursor;
		public int nextCursor=0;

		public MixupTokenizer(String input){
			this.input=input;
			this.matcher=tokenizerPattern.matcher(input);
		}

		public boolean advance(){
			if(matcher.find()){
				cursor=matcher.start(1);
				token=matcher.group(1);
				if((token.equals(";"))){
					token=null;
					return false;
				}
				return true;
			}else{
				token=null;
				return false;
			}
		}

		// advance to next token, and check that it's what's expected
		public String advance(Set<String> set) throws Mixup.ParseException{

			if(!matcher.find()){
				token=null;
				cursor=input.length();
				return null;
			}

			cursor=matcher.start(1);
			token=matcher.group(1);
			if((token.equals(";"))){
				token=null;
				return null;
			}
			if(set!=null&&!set.contains(token)){
				System.out.println("Token at Error: "+token);
				parseError("statement error: expected one of: "+setContents(set)+
						" in "+token);
			}

			return token;
		}

		private void parseError(String msg) throws ParseException{
			throw new ParseException(msg+": "+input.substring(0,cursor)+"^^^"+
					input.substring(cursor,input.length()));
		}

		/** convert a set to a string listing the elements */
		private String setContents(Set<String> set){
			StringBuffer buf=new StringBuffer("");
			for(Iterator<String> i=set.iterator();i.hasNext();){
				if(buf.length()>0)
					buf.append(" ");
				buf.append("'"+i.next().toString()+"'");
			}
			return buf.toString();
		}
	}

	//
	// recursive descent parser for the BNF above
	//
	private static class MixupParser{

		private MixupTokenizer tok;

		public MixupParser(MixupTokenizer tok){
			this.tok=tok;
		}

		private Expr parseExpr() throws ParseException{
//			Expr expr1=null;
			Expr expr2=null;
			String op=null;
			BasicExpr basic=parseBasicExpr();
			if("&&".equals(tok.token)||"||".equals(tok.token)){
				op=tok.token;
				tok.advance();
				expr2=parseExpr();
			}
			return new Expr(basic,expr2,op);
		}

		private BasicExpr parseBasicExpr() throws ParseException{
			List<RepeatedPrim> list=new ArrayList<RepeatedPrim>();
			int left=-1,right=-1;
			if("(".equals(tok.token)){
				tok.advance();
				Expr expr=parseExpr();
				if(!")".equals(tok.token))
					tok.parseError("expected close paren");
				tok.advance(); // past ')'
				return new BasicExpr(expr);
			}else{
				while(tok.token!=null&&!"||".equals(tok.token)&&
						!"&&".equals(tok.token)&&!")".equals(tok.token)){
					if("[".equals(tok.token)){
						left=list.size();
						tok.advance();
					}else if("]".equals(tok.token)){
						right=list.size();
						tok.advance();
					}else{
						list.add(parseRepeatedPrim());
					}
				}
				if(left<0)
					tok.parseError("no left bracket");
				if(right<0)
					tok.parseError("no right bracket");
				return new BasicExpr((RepeatedPrim[])list.toArray(new RepeatedPrim[list
						.size()]),left,right);
			}
		}

		private RepeatedPrim parseRepeatedPrim() throws ParseException{
			RepeatedPrim buf=new RepeatedPrim();
			if("@".equals(tok.token)){
				tok.advance();
				buf.type=tok.token;
				tok.advance();
				buf.maxCount=1;
				if("?".equals(tok.token)){
					buf.minCount=0;
					tok.advance();
				}else{
					buf.minCount=1;
				}
				return buf;
			}else{
				if("L".equals(tok.token)){
					buf.leftMost=true;
					tok.advance();
				}
				parsePrim(buf);
				parseRepeat(buf);
				if("R".equals(tok.token)){
					buf.rightMost=true;
					tok.advance();
				}
				buf.expandShortcuts();
				if(!buf.checkFunction())
					tok.parseError("syntax error");
				return buf;
			}
		}

		private void parsePrim(RepeatedPrim buf) throws ParseException{
			if("<".equals(tok.token)){
				tok.advance();
				parseSimplePrim(buf);
				while(",".equals(tok.token)){
					tok.advance();
					parseSimplePrim(buf);
				}
				if(">".equals(tok.token))
					tok.advance();
				else
					tok.parseError("expected '>'");
			}else{
				parseSimplePrim(buf);
			}
		}

		private void parseSimplePrim(RepeatedPrim buf) throws ParseException{
			Prim prim=new Prim();
			if("!".equals(tok.token)){
				prim.negated=true;
				tok.advance();
			}
			prim.funcString=tok.token;
//			int funcLength=tok.token.length();
//			char firstLetter=tok.token.charAt(0);
			if("a".equals(tok.token))
				prim.function=A;
			else if("eq".equals(tok.token))
				prim.function=EQ;
			else if("ai".equals(tok.token))
				prim.function=AI;
			else if("re".equals(tok.token))
				prim.function=RE;
			else if("any".equals(tok.token))
				prim.function=ANY;
			else if("eqi".equals(tok.token))
				prim.function=EQI;
			else if("...".equals(tok.token))
				prim.function=ELIPSE;
			else if("prop".equals(tok.token))
				prim.function=PROP;
			else if("propDict".equals(tok.token))
				prim.function=PROPDICT;
			tok.advance();
			if("(".equals(tok.token)){
				tok.advance(); // to argument
				prim.argument=tok.token;
				tok.advance(); // to ')' 
				if(!")".equals(tok.token))
					tok.parseError("expected close paren");
				tok.advance(); // past prim
			}else if(":".equals(tok.token)){
				prim.property=prim.funcString;
				prim.function=PROP;
				prim.funcString="prop";
				tok.advance(); // to property value
				if("a".equals(tok.token)){
					tok.advance(); // to '('
					if(!"(".equals(tok.token)){
						prim.value="a";
						tok.advance(); // past value
					}else{
						tok.advance(); // to dictionary name
						prim.function=PROPDICT;
						prim.funcString="propDict";
						prim.value=tok.token;
						tok.advance();
						if(!")".equals(tok.token))
							tok.parseError("expected close paren");
						tok.advance(); // past close paren
					}
				}else{
					prim.value=tok.token;
					tok.advance(); // past value
				}
			}
			prim.expandShortcuts();
			buf.primList.add(prim);
		}

		private void parseRepeat(RepeatedPrim buf) throws ParseException{
			String min=null,max=null;
			if("{".equals(tok.token)){
				tok.advance();
				if(!",".equals(tok.token)){
					min=tok.token;
					tok.advance(); // to "," 
				}else{
					min="0";
				}
				if("}".equals(tok.token)){
					max=min;
					tok.advance();
				}else{
					if(!",".equals(tok.token))
						tok.parseError("expected \",\"");
					tok.advance();
					if(!"}".equals(tok.token)){
						max=tok.token;
						tok.advance(); // to "}"
					}else{
						max="-1";
					}
					if(!"}".equals(tok.token))
						tok.parseError("expected \"}\"");
					tok.advance();
				}
			}else if("+".equals(tok.token)){
				min="1";
				max="-1";
				tok.advance();
			}else if("*".equals(tok.token)){
				min="0";
				max="-1";
				tok.advance();
			}else if("?".equals(tok.token)){
				min="0";
				max="1";
				tok.advance();
			}else{
				min=max="1";
			}
			try{
				buf.minCount=Integer.parseInt(min);
				buf.maxCount=Integer.parseInt(max);
			}catch(NumberFormatException e){
				tok.parseError("expected an integer: min = '"+min+"' max='"+max+"'");
			}
		}

	}

	/** Signals an error in parsing a mixup document. */
	public static class ParseException extends Exception{
		static final long serialVersionUID=20080303L;
		public ParseException(String s){
			super(s);
		}
	}

	//
	// encodes a pattern that matches a single TextToken
	//
	private static class Prim implements Serializable{

		static private final long serialVersionUID=20080303L;

		public boolean negated=false;

		public int function=-1;

		public String funcString="";

		public String argument="";

		public String property="",value="";

		private Pattern pattern=null;

		/** See if the predicate for this pattern succeeds for this TextToken.  */
		public boolean matchesPrim(TextLabels labels,Token token){
			boolean status=matchesUnnegatedPrim(labels,token);
			return negated==!status;
		}

		private boolean matchesUnnegatedPrim(TextLabels labels,Token token){
			if(function==A)
				return labels.inDict(token,argument); //a 		
			if(function==EQ)
				return token.getValue().equals(argument); //eq
			else if(function==AI){ //ai
				final String lc=token.getValue().toLowerCase();
				Token lcToken=new Token(){

					public String toString(){
						return "[lcToken "+lc+"]";
					}

					public String getValue(){
						return lc;
					}

//					public int getIndex(){
//						return 0;
//					}
				};
				return labels.inDict(lcToken,argument);
			}else if(function==RE){ //re
				return pattern.matcher(token.getValue()).find();
			}else if(function==ANY)
				return true; //any	    	    	    	     
			else if(function==EQI)
				return token.getValue().equalsIgnoreCase(argument); //eqi	    
			else if(function==PROP){ //prop
				return value.equals(labels.getProperty(token,property));
			}else if(function==PROPDICT){ //propDict
				final String propVal=labels.getProperty(token,property);
				if(propVal==null)
					return false;
				Token propValToken=new Token(){

					public String toString(){
						return "[token:"+propVal+"]";
					}

					public String getValue(){
						return propVal;
					}

//					public int getIndex(){
//						return 0;
//					}
				};
				//System.out.println("testing "+propValToken+" for membership in dict "+value);
				return labels.inDict(propValToken,value);
			}else{
				throw new IllegalStateException("illegal function '"+funcString+"'");
			}
		}

		/** Expand some syntactic sugar-like abbreviations. */
		public void expandShortcuts(){
			// expand the 'const' abbreviation to eq('const')
			if(funcString.startsWith("'")&&funcString.endsWith("'")){
				argument=funcString;
				function=EQ;
				funcString="eq";
			}
			// unquote a quoted argument
			if(argument.startsWith("'")&&argument.endsWith("'")){
				argument=argument.substring(1,argument.length()-1);
				argument=argument.replaceAll("\\\\'","'");
			}
			// precompile a regex
			if(RE==function)
				pattern=Pattern.compile(argument);
			// check for correctness
		}

		/** is this a legal function? */
		public boolean checkFunction(){
			return legalFunctions.contains(funcString);
		}

		public String toString(){
			StringBuffer buf=new StringBuffer("");
			if(negated)
				buf.append("!");
			if(PROP!=function){
				buf.append(funcString);
				if(argument!=null)
					buf.append("("+argument+")");
			}else{
				buf.append(property+":"+value);
			}
			return buf.toString();
		}
	}

	// encodes a pattern matching a series of Token's
	private static class RepeatedPrim implements Serializable{

		static private final long serialVersionUID=20080303L;

		public boolean leftMost=false;

		public boolean rightMost=false;

		public List<Prim> primList=new ArrayList<Prim>();

		public boolean[] whereIMatch;

		public Span whatIIndexed=null;

		public int minCount;

		public int maxCount; // -1 indicates infinity

		String type=null; // non-null for @type and @type?

		/** Expand some syntactic sugar-like abbreviations. */
		public void expandShortcuts(){
			// expand the 'const' abbreviation to eq('const')
			if(primList.size()==1){
				Prim prim=primList.get(0);
				if(ELIPSE==prim.function){
					prim.function=ANY;
					prim.funcString="any";
					minCount=0;
					maxCount=-1;
					return;
				}
			}
		}

		public boolean checkFunction(){
			for(Iterator<Prim> i=primList.iterator();i.hasNext();){
				Prim prim=i.next();
				if("...".equals(prim.funcString)&&primList.size()!=1)
					return false;
				if(!prim.checkFunction())
					return false;
			}
			return true;
		}

		public String toString(){
			if(type!=null){
				if(minCount==0)
					return "@"+type+"?";
				else
					return "@"+type;
			}else{
				StringBuffer buf=new StringBuffer("");
				if(leftMost)
					buf.append("L ");
				if(primList.size()==1)
					buf.append((Prim)primList.get(0));
				else if(primList.size()==0)
					throw new IllegalStateException("empty prim list");
				else{
					buf.append("<"+primList.get(0).toString());
					for(int i=1;i<primList.size();i++){
						buf.append(", "+primList.get(i).toString());
					}
					buf.append(">");
				}
				buf.append("{"+minCount+","+maxCount+"}");
				if(rightMost)
					buf.append("R");
				return buf.toString();
			}
		}

		/** Indexes where tokens match in the PrimList */
		public void index(Span s,TextLabels labels){
			whatIIndexed=s;
			whereIMatch=new boolean[s.size()];
			for(int i=0;i<s.size();i++){
				whereIMatch[i]=matchesPrimList(labels,s.getToken(i));
			}
		}

		/** See if this pattern matches span.subSpan(lo,len). */
		public boolean matchesSubspan(TextLabels labels,Span span,int lo,int len){
			if(type!=null){
				if(minCount==1){
					return labels.hasType(span.subSpan(lo,len),type);
				}else{
					return len==0||labels.hasType(span.subSpan(lo,len),type);
				}
			}else{
				// check and see if this span has been indexed or not
				//String span1 = span.asString();
				//String span2 = "";
				//if(whatIIndexed != null) span2 = whatIIndexed.asString();
				//if(!span1.trim().equals(span2.trim())) index(span, labels);		    
				if(whatIIndexed==null||!whatIIndexed.equals(span))
					index(span,labels);

				if(len>maxCount&&maxCount>=0)
					return false;
				if(len<minCount)
					return false;
				int spanSize=span.size();
				for(int i=lo;i<lo+len;i++){
					if(i>=spanSize)
						return false;
					//if (!matchesPrimList(labels,span.getToken(i))) return false;
					if(!whereIMatch[i])
						return false;
				}
				if(leftMost&&(len<maxCount||maxCount<0)){
					if(lo>0&&
							/*matchesPrimList(labels,span.getToken(lo-1))*/whereIMatch[lo-1])
						return false;
				}
				if(rightMost&&(len<maxCount||maxCount<0)){
					if(lo+len<spanSize&&
							/*matchesPrimList(labels,span.getToken(lo+len))*/whereIMatch[lo+
									len])
						return false;
				}
				return true;
			}
		}

		private boolean matchesPrimList(TextLabels labels,Token token){
			for(Iterator<Prim> i=primList.iterator();i.hasNext();){
				Prim prim=i.next();
				if(!prim.matchesPrim(labels,token))
					return false;
			}
			return true;
		}
	}

	//
	// encodes a basicExpr in the BNF above
	//
	private static class BasicExpr implements Serializable{

		static private final long serialVersionUID=20080303L;

		public final Expr expr;

		public final RepeatedPrim[] repPrim;

		public final int leftBracket,rightBracket;

		private static Logger log=Logger.getLogger(BasicExpr.class);

		public BasicExpr(Expr expr){
			this.expr=expr;
			this.repPrim=null;
			this.leftBracket=this.rightBracket=-1;
		}

		public BasicExpr(RepeatedPrim[] repPrim,int leftBracket,int rightBracket){
			this.expr=null;
			this.repPrim=repPrim;
			this.leftBracket=leftBracket;
			this.rightBracket=rightBracket;
		}

		public String toString(){
			if(expr!=null){
				return "("+expr.toString()+")";
			}else{
				StringBuffer buf=new StringBuffer();
				for(int i=0;i<repPrim.length;i++){
					if(i==leftBracket)
						buf.append("[");
					buf.append(" "+repPrim[i].toString());
					if(i+1==rightBracket)
						buf.append("]");
				}
				return buf.toString();
			}
		}

		public Iterator<Span> match(TextLabels labels,Iterator<Span> spanLooper){
			if(expr!=null){
				return expr.match(labels,spanLooper);
			}else{
				ProgressCounter pc=
						new ProgressCounter("mixup","span");
				Set<Span> accum=new TreeSet<Span>();
				while(spanLooper.hasNext()){
					pc.progress();
					Span span=spanLooper.next();
					// match(labels,accum,span,new int[repPrim.length],new int[repPrim.length],1,0,0);
					fastMatch(labels,span,accum);
				}
				pc.finished();
				return accum.iterator();
			}
		}

		// most time taken here
		private void fastMatch(TextLabels labels,Span span,Set<Span> accum){
			//      log.debug("span size: " + span.size() + " - " + span.asString());
			// there are at most span.length^2 matches of every repeated primitive
			log.debug("matching span id/size="+span.getDocumentId()+"/"+span.size());
			log.debug("before alloc: max/free="+Runtime.getRuntime().maxMemory()+"/"+
					Runtime.getRuntime().freeMemory());
			// We may overflow the int datatype if there are too many tokens in the span, in which case we should use 
			// the largest available int as it is highly unlikely that there will *actually* be anywhere near that
			// many matches to store.
			//int maxRepeatedPrimMatches = span.size() * (span.size()+1);
			int maxRepeatedPrimMatches;
			if(span.size()>(Integer.MAX_VALUE/(span.size()+1)))
				maxRepeatedPrimMatches=Integer.MAX_VALUE; // overflow
			else
				maxRepeatedPrimMatches=span.size()*(span.size()+1);
			// Now apply any constraints that may further limit the number of possible matches
			if(maxRepeatedPrimMatches>minMatchesToApplyConstraints){
				if(maxNumberOfMatchesPerToken>0){
					// If the span is large enough (ie has more than Integer.MAX_VALUE/maxNumberOfMatchesPerToken tokens) then we will
					// overflow int again here so check for that and only attempt to adjust for the constraint if it will
					// succeed.  Otherwise stick with the maximum int value.
					if(span.size()>(Integer.MAX_VALUE/maxNumberOfMatchesPerToken))
						maxRepeatedPrimMatches=
								Math.min(maxNumberOfMatchesPerToken*span.size(),
										maxRepeatedPrimMatches);
				}
				// Now we can arbitrarily set a limit to the number of matches so if this is the case, then we should 
				// use that limit if it is larger than the number of possible matches we computed.
				if((maxNumberOfMatches>0)&&(maxNumberOfMatches<maxRepeatedPrimMatches)){
					maxRepeatedPrimMatches=maxNumberOfMatches;
				}
			}
			int[] loIndexBuffer=new int[maxRepeatedPrimMatches];
			int[] lengthBuffer=new int[maxRepeatedPrimMatches];
			log.debug("alloc hi-lo: max/free="+Runtime.getRuntime().maxMemory()+"/"+
					Runtime.getRuntime().freeMemory());
			// store possible places that repPrim[i] can match
			int[][] possibleLos=new int[repPrim.length][];
			int[][] possibleLens=new int[repPrim.length][];
			// also record min/max length 
			int[] minLen=new int[repPrim.length];
			int[] maxLen=new int[repPrim.length];
			boolean[] isAny=new boolean[repPrim.length];
			log.debug("after alloc: max/free="+Runtime.getRuntime().maxMemory()+"/"+
					Runtime.getRuntime().freeMemory());
			for(int i=0;i<repPrim.length;i++){
				// work out possible lengths for repPrim[i]
				RepeatedPrim rp=repPrim[i];
				minLen[i]=rp.minCount;
				maxLen[i]=span.size();
				if(rp.maxCount>=0&&rp.maxCount<maxLen[i])
					maxLen[i]=rp.maxCount;
				// see if repPrim[i] is "any"
				if(rp.primList.size()==1){
					Prim prim=(Prim)rp.primList.get(0);
					isAny[i]=
							(ANY==prim.function&&!prim.negated&&!rp.leftMost&&!rp.rightMost);
				}
				if(!isAny[i]){
					// find all places this matches
					int numMatches=0;
					if(rp.type!=null){
						// look up matches from the labels for a spantype repPrim, eg @foo
						for(Iterator<Span> el=
								labels.instanceIterator(rp.type,span.getDocumentId());el
								.hasNext();){
							if(numMatches>=maxRepeatedPrimMatches){
								overflowWarning(numMatches,maxRepeatedPrimMatches,span,i);
								return;
							}
							Span s=el.next();
							if(span.contains(s)){
								if(numMatches>=maxRepeatedPrimMatches){
									overflowWarning(numMatches,maxRepeatedPrimMatches,span,i);
									return;
								}
								loIndexBuffer[numMatches]=
										s.documentSpanStartIndex()-span.documentSpanStartIndex();
								lengthBuffer[numMatches]=s.size();
								numMatches++;
							}
						}
					}
					if(rp.type==null||(rp.type!=null&&rp.minCount==0)){
						// something besides @foo or @foo?
						// check all possible subspans
						for(int j=0;j<=span.size();j++){
							int topLen=Math.min(maxLen[i],span.size()-j);
							for(int k=minLen[i];k<=topLen;k++){
								if(numMatches>=maxRepeatedPrimMatches){
									overflowWarning(numMatches,maxRepeatedPrimMatches,span,i);
									return;
								}
								//84% time taken in matchesSubspan
								if(rp.matchesSubspan(labels,span,j,k)){
									loIndexBuffer[numMatches]=j;
									lengthBuffer[numMatches]=k;
									numMatches++;
								}
							}
						}
					}
					// save matches from buffer into loIndices, lengths
					possibleLos[i]=new int[numMatches];
					possibleLens[i]=new int[numMatches];
					for(int m=0;m<numMatches;m++){
						possibleLos[i][m]=loIndexBuffer[m];
						possibleLens[i][m]=lengthBuffer[m];
					}
				}
			}
			//
			// now find a good series of loIndex/length pairs
			//
			int[] lows=new int[repPrim.length];
			int[] highs=new int[repPrim.length];
			fastMatch(labels,accum,span,lows,highs,1,0,0,possibleLos,possibleLens,
					isAny,minLen,maxLen);
		}

		private void overflowWarning(int numMatches,int maxRepeatedPrimMatches,
				Span span,int i){
			log.warn("mixup warning at pattern #"+(i+1)+" "+repPrim[i]+") on "+span);
			log.warn("not enough room to store all matches: adjust Mixup.maxNumberOfMatches(PerToken)");
			log.warn("size="+span.size()+" numMatches="+numMatches+" max="+
							maxRepeatedPrimMatches+" minConstraint="+
							minMatchesToApplyConstraints);
		}

		private void fastMatch(TextLabels labels, // passed along to subroutines
				Set<Span> accum, // accumulate matches
				Span span, // span being matched
				int[] lows, // lows[i] is lo index of match to repPrim[i] 
				int[] highs, // highs[i] is high index of match to repPrim[i] 
				int tab, // for debugging
				int spanCursor, // index into the span being matched
				int patternCursor, // index into the repPrim's being matched
				int[][] possibleLos, // loIndices[i] is all places repPrim[i] might match
				int[][] possibleLens, // lengths[i] is parallel-to-loIndices array of lengths 
				boolean[] isAny, // true if repPrim[i] is "any"
				int[] minLen, // min lengths of subseq matching an isAny==true repPrim[i]
				int[] maxLen) // max lengths of subseq matching an isAny==true repPrim[i]
		{
			if(patternCursor==repPrim.length){
				if(spanCursor==span.size()){
					// a complete, successful match
					if(DEBUG)
						showMatch(tab,"complete",span,lows,highs,patternCursor);
					int lo=lows[leftBracket];
					int hi=highs[rightBracket-1];
					accum.add(span.subSpan(lo,hi-lo));
				}else{
					// a deadend
					if(DEBUG)
						showMatch(tab,"failed",span,lows,highs,patternCursor);
				}
			}else{
				// continue a partial match
				if(isAny[patternCursor]){
					if(patternCursor+1<repPrim.length&&!isAny[patternCursor+1]){
						// trick to handle something like '...' followed by a specific pattern 
						for(int i=0;i<possibleLos[patternCursor+1].length;i++){
							int nextSpanCursor=possibleLos[patternCursor+1][i];
							int len=nextSpanCursor-spanCursor;
							if(len>=minLen[patternCursor]&&len<=maxLen[patternCursor]){
								lows[patternCursor]=spanCursor;
								highs[patternCursor]=spanCursor+len;
								if(DEBUG)
									showMatch(tab,"partial",span,lows,highs,patternCursor+1);
								fastMatch(labels,accum,span,lows,highs,tab+1,spanCursor+len,
										patternCursor+1,possibleLos,possibleLens,isAny,minLen,
										maxLen);
							}
						}
					}else{
						int topLen=Math.min(maxLen[patternCursor],span.size()-spanCursor);
						for(int len=minLen[patternCursor];len<=topLen;len++){
							lows[patternCursor]=spanCursor;
							highs[patternCursor]=spanCursor+len;
							if(DEBUG)
								showMatch(tab,"partial",span,lows,highs,patternCursor+1);
							fastMatch(labels,accum,span,lows,highs,tab+1,spanCursor+len,
									patternCursor+1,possibleLos,possibleLens,isAny,minLen,maxLen);
						}
					}
				}else{
					int topLen=span.size()-spanCursor;
					for(int i=0;i<possibleLos[patternCursor].length;i++){
						if(possibleLos[patternCursor][i]==spanCursor&&
								possibleLens[patternCursor][i]<=topLen){
							int len=possibleLens[patternCursor][i];
							lows[patternCursor]=spanCursor;
							highs[patternCursor]=spanCursor+len;
							if(DEBUG)
								showMatch(tab,"partial",span,lows,highs,patternCursor+1);
							fastMatch(labels,accum,span,lows,highs,tab+1,spanCursor+len,
									patternCursor+1,possibleLos,possibleLens,isAny,minLen,maxLen);
						}
					}
				}
			}
		}

		// 
		// obsolete slower match routine, kept around as a reference implementation for debugging
		// 
//		private void match(TextLabels env,Set accum,Span span,int[] lows,
//				int[] highs,int tab,int spanCursor,int patternCursor){
//			if(patternCursor==repPrim.length){
//				if(spanCursor==span.size()){
//					// a complete, successful match
//					if(DEBUG)
//						showMatch(tab,"complete",span,lows,highs,patternCursor);
//					int lo=lows[leftBracket];
//					int hi=highs[rightBracket-1];
//					accum.add(span.subSpan(lo,hi-lo));
//				}else{
//					// a deadend
//					if(DEBUG)
//						showMatch(tab,"failed",span,lows,highs,patternCursor);
//				}
//			}else{
//				// continue a partial match
//				RepeatedPrim nextPattern=repPrim[patternCursor];
//				int maxLen=span.size()-spanCursor;
//				if(nextPattern.maxCount>=0&&nextPattern.maxCount<maxLen)
//					maxLen=nextPattern.maxCount;
//				for(int len=nextPattern.minCount;len<=maxLen;len++){
//					// 84% time taken in matchesSubspan
//					boolean lenOk=nextPattern.matchesSubspan(env,span,spanCursor,len);
//					if(lenOk){
//						lows[patternCursor]=spanCursor;
//						highs[patternCursor]=spanCursor+len;
//						if(DEBUG)
//							showMatch(tab,"partial",span,lows,highs,patternCursor+1);
//						match(env,accum,span,lows,highs,tab+1,spanCursor+len,
//								patternCursor+1);
//					}
//				}
//			}
//		}

		// for debugging
		private void showMatch(int tab,String msg,Span span,int[] lows,int[] highs,
				int patternCursor){
			for(int i=0;i<tab;i++){
				System.out.print("| ");
			}
			System.out.print(msg+":");
			for(int i=0;i<patternCursor;i++){
				System.out.print(" "+repPrim[i].toString()+"["+lows[i]+":"+highs[i]+
						"]<");
				for(int j=lows[i];j<highs[i];j++){
					if(j>lows[i])
						System.out.print(" ");
					System.out.print(span.getToken(j).getValue());
				}
				System.out.print(">");
			}
			System.out.println();
		}
	}

	//
	// encodes an expression in the BNF above
	//
	private static class Expr implements Serializable{

		static private final long serialVersionUID=20080303L;

		private BasicExpr expr1;

		private Expr expr2;

		private String op;

		public Expr(BasicExpr expr1,Expr expr2,String op){
			this.expr1=expr1;
			this.expr2=expr2;
			this.op=op;
		}

		public Iterator<Span> match(TextLabels labels,Iterator<Span> spanIt){
			if(expr2==null){
				return expr1.match(labels,spanIt);
			}else if("&&".equals(op)){
				return expr2.match(labels,expr1.match(labels,spanIt));
			}else{
				if(!"||".equals(op))
					throw new IllegalStateException("illegal operator '"+op+"'");
				// copy the input looper
				SortedSet<Span> save=new TreeSet<Span>();
				while(spanIt.hasNext())
					save.add(spanIt.next());
				// union the outputs of expr1 and expr2
				Iterator<Span> a=expr1.match(labels,save.iterator());
				Iterator<Span> b=expr2.match(labels,save.iterator());
				SortedSet<Span> union=new TreeSet<Span>();
				while(a.hasNext())
					union.add(a.next());
				while(b.hasNext())
					union.add(b.next());
				return union.iterator();
			}
		}

		public String toString(){
			StringBuffer buf=new StringBuffer();
			buf.append(expr1.toString());
			if(expr2!=null)
				buf.append(" "+op+" "+expr2.toString());
			return buf.toString();
		}
	}

	//
	// interactive test routine
	//
	public static void main(String[] args){
		try{
			Mixup mixup=new Mixup(args[0]);
			System.out.println("normalized expression = "+mixup);
			BasicTextBase b=new BasicTextBase();
			MonotonicTextLabels labels=new BasicTextLabels(b);
			for(int i=1;i<args.length;i++){
				b.loadDocument("arg_"+i,args[i]);
			}
			new BoneheadStemmer().stem(b,labels);
			//System.out.println("labels="+labels);
			//labels.addWord("the", "det");
			//labels.addWord("thi", "det");
			for(Iterator<Span> i=mixup.extract(labels,b.documentSpanIterator());i
					.hasNext();){
				System.out.println(i.next());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
