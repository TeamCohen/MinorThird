/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 *
 * @author William Cohen
 */

public class TestPackage extends TestSuite 
{
	public static final boolean DEBUG = false;

	public TestPackage(String name) { super(name); }

	public static TestSuite suite() {
		TestSuite suite = new TestSuite();
		suite.addTest( new ToXMLTest("doTest") );
		suite.addTest( new DiffTest("doTest") );
		suite.addTest( new TrieTest("doTest") );
		suite.addTest( new MixupTest("doTest") );
		suite.addTest( new LabelsTest("doTest") );
		suite.addTest( new TokenizationTest("doTest") );
		return suite;
	}

    public static class TokenizationTest extends TestCase
    {
		public TokenizationTest(String string)
		{
			super(string);
		}
		public void doTest() 
		{
			TextBaseLoader baseLoader = new TextBaseLoader();
			TextBase b = new BasicTextBase(), childTB = null;
			b.loadDocument("letters", "a b c d\ne f g h\ni j k l\nm n o p\nr s t u");
			Tokenizer tok = new Tokenizer(1, "\n");
			childTB = b.retokenize(tok);
			MutableTextLabels lab = new BasicTextLabels();
			try {
				MixupProgram p = 
				    new MixupProgram(new String[]{"defTokenProp token:first =: [any] ...",
								  "defSpanType first =: [token:first] ...",
								  "defTokenProp token:last =: ... [any]",
								  "defSpanType last =: ... [token:last]"});
				p.eval(lab,childTB);
				Span.Looper looper = lab.instanceIterator("first");
				assertTrue( looper.hasNext() );
				assertEquals( "a b c d", looper.nextSpan().asString()  ); 
				Span.Looper looper2 = lab.instanceIterator("last");
				assertTrue( looper2.hasNext() );
				assertEquals( "r s t u", looper2.nextSpan().asString()  ); 
			} catch (Mixup.ParseException e) {
				throw new IllegalStateException(e.toString());
			}
		}
    }
		
	public static class LabelsTest extends TestCase 
	{
		public LabelsTest(String string)
		{
			super(string);
		}
		public void doTest() 
		{
			TextBase b = new BasicTextBase();
			MutableTextLabels lab = new BasicTextLabels(b);
			b.loadDocument("d1", "a b c b d");
			try {
				MixupProgram p = 
					new MixupProgram(new String[]{"defSpanProp startsWith:b =: ... ['b' any]...",
																				"defSpanProp startsWith:c =: ... ['c' any]...",
																				"defSpanProp endsWith:b =: ... [any 'b']..."});
				p.eval(lab,b);
				Span.Looper looper = lab.getSpansWithProperty("startsWith");
				assertTrue( looper.hasNext() );
				assertEquals( "b c", looper.nextSpan().asString()  ); 
				assertTrue( looper.hasNext() );
				assertEquals( "c b", looper.nextSpan().asString()  ); 
				assertTrue( looper.hasNext() );
				assertEquals( "b d", looper.nextSpan().asString()  ); 
				assertTrue( !looper.hasNext() );
			} catch (Mixup.ParseException e) {
				throw new IllegalStateException(e.toString());
			}
		}
	}

	public static class ToXMLTest extends TestCase {
		private TextBase b;
		public ToXMLTest(String string) {
			super(string);
			b = new BasicTextBase();
			b.loadDocument("test", "a b c d e f g");
		}			
		public void doTest() {
			MutableTextLabels e1 = new BasicTextLabels(b);
			e1.addToType( testSpan(1,3), "x" );
			checkXML(e1, "<root>a <x>b c d </x>e f g</root>");
			e1.addToType( testSpan(2,1), "y" );
			checkXML(e1, "<root>a <x>b </x><overlap value=\"x,y\">c </overlap><x>d </x>e f g</root>");
			MutableTextLabels e2 = new BasicTextLabels(b);
			e2.addToType( testSpan(1,2), "x" );
			e2.addToType( testSpan(4,1), "y" );
			checkXML(e2, "<root>a <x>b c </x>d <y>e </y>f g</root>");
			MutableTextLabels e3 = new BasicTextLabels(b);
			e3.addToType( testSpan(0,3), "x" );
			e3.addToType( testSpan(3,1), "y" );
			checkXML(e3, "<root><x>a b c </x><y>d </y>e f g</root>");
			e3.addToType( testSpan(5,2), "z" );
			checkXML(e3, "<root><x>a b c </x><y>d </y>e <z>f g</z></root>");
			MutableTextLabels e4 = new BasicTextLabels(b);
			e4.addToType( testSpan(1,3), "x" );
			checkXML(e4, "<root>a <x>b c d </x>e f g</root>");
			e4.addToType( testSpan(2,3), "y" );
			checkXML(e4, "<root>a <x>b </x><overlap value=\"x,y\">c d </overlap><y>e </y>f g</root>");
		}
		private void checkXML(TextLabels e,String expected) {
			String actual = new TextLabelsLoader().markupDocumentSpan("test", e);
			//System.out.println("expected: '"+expected+"'");
			//System.out.println("actual:   '"+actual+"'");
			assertEquals(expected,actual);
		}
		private Span testSpan(int lo,int len) {
			return b.documentSpan("test").subSpan(lo,len);
		}
	}

	//
	// difference-testing code
	// to do: test on adjacent/overlapping spans 
	//
	public static class DiffTest extends TestCase {
		public DiffTest(String string) {
			super(string);
		}
		public void doTest() {
			TreeSet guess = new TreeSet();
			TreeSet truth = new TreeSet();
			TextBase b = new BasicTextBase();
			TextLabels e = new BasicTextLabels(b);
			b.loadDocument("a-d", "a b c d");
			b.loadDocument("e-h", "e f g h");
			b.loadDocument("i-l", "i j k l");
			b.loadDocument("m-p", "m n o p");
			b.loadDocument("r-u", "r s t u");
			try {
				truth.add( new Mixup("'a' ['b' 'c'] 'd'").extract(e, b.documentSpanIterator()).nextSpan() );
				truth.add( new Mixup("'e' 'f' ['g' 'h']").extract(e, b.documentSpanIterator()).nextSpan() );
				truth.add( new Mixup("'i' ['j' 'k'] 'l'").extract(e, b.documentSpanIterator()).nextSpan() );
				truth.add( new Mixup("'m' ['n' 'o'] 'p'").extract(e, b.documentSpanIterator()).nextSpan() );
				guess.add( new Mixup("'e' ['f' 'g'] 'h'").extract(e, b.documentSpanIterator()).nextSpan() );
				guess.add( new Mixup("'i' ['j' 'k'] 'l'").extract(e, b.documentSpanIterator()).nextSpan() );
				guess.add( new Mixup("'m' 'n' ['o' 'p']").extract(e, b.documentSpanIterator()).nextSpan() );
				guess.add( new Mixup("'r' ['s' 't'] 'u'").extract(e, b.documentSpanIterator()).nextSpan() );
			} catch (Mixup.ParseException ex) {
				ex.printStackTrace();
			}
			//System.out.println("guess = "+guess);
			//System.out.println("truth = "+truth);
			SpanDifference sd = new SpanDifference(new BasicSpanLooper(guess.iterator()),
																						 new BasicSpanLooper(truth.iterator()));
			DiffExpects[] expects = new DiffExpects[] {
				new DiffExpects("b c",SpanDifference.FALSE_NEG),
				new DiffExpects("f",SpanDifference.FALSE_POS),
				new DiffExpects("g",SpanDifference.TRUE_POS),
				new DiffExpects("h",SpanDifference.FALSE_NEG),
				new DiffExpects("j k",SpanDifference.TRUE_POS),
				new DiffExpects("n",SpanDifference.FALSE_NEG),
				new DiffExpects("o",SpanDifference.TRUE_POS),
				new DiffExpects("p",SpanDifference.FALSE_POS),
				new DiffExpects("s t",SpanDifference.FALSE_POS) };
			SpanDifference.Looper d = sd.differenceIterator(); 
			int k = 0;
			while (d.hasNext()) {
				Span s = d.nextSpan();
				int stat = d.getStatus();
				DiffExpects dx = expects[k++];
				assertEquals(dx.s, s.asString());
				assertEquals(dx.stat, stat);
			}
		}
		public static class DiffExpects {
			public String s;
			int stat;
			public DiffExpects(String s,int stat) { this.s = s; this.stat = stat; }
		}
	}

	//
	// trie-testing code
	//

	public static class TrieTest extends TestCase
	{
		private TextBase b = new BasicTextBase();
		private Trie trie = new Trie();
		public TrieTest(String string) {
			super(string);
		}
		public void doTest() {
			trie.addWords("wwc", b.splitIntoTokens("william cohen"));
			trie.addWords("wjc", b.splitIntoTokens("william clinton"));
			trie.addWords("pc", b.splitIntoTokens("paul cohen"));
			trie.addWords("j2p2", b.splitIntoTokens("pope john paul II"));
			trie.addWords("theMan", b.splitIntoTokens("william cohen"));
			b.loadDocument("t1","aint william cohen a great guy?");
			b.loadDocument("t2","men of the year: william william cohen ; william clinton ; and - bill gates??");
			b.loadDocument("t3","cohen & jensen was written by (a) william cohen (b) paul cohen (c) all of the above");
			b.loadDocument("t4","is the pope john paul II or not?");
			checkLookup( "t1", new TrieExpects[] { new TrieExpects(new String[]{"wwc", "theMan"},1,2) } );
			checkLookup( "t2", new TrieExpects[] { new TrieExpects(new String[]{"wwc", "theMan"},6,2),
																							 new TrieExpects(new String[]{"wjc"},9,2) });
			checkLookup( "t3", new TrieExpects[] { new TrieExpects(new String[]{"wwc", "theMan"},9,2),
																							 new TrieExpects(new String[]{"pc"},14,2) });
			checkLookup( "t4", new TrieExpects[] { new TrieExpects(new String[]{"j2p2"},2,4) } );
		}
		private void checkLookup( String documentId, TrieExpects[] expects) {
			Span span = b.documentSpan(documentId);
			ArrayList spanList = new ArrayList();
			ArrayList idList = new ArrayList();
			if (DEBUG) System.out.println("lookup in "+span);
			int k = 0;
			for (Trie.ResultLooper i = trie.lookup(span); i.hasNext(); ) {
				spanList.add( i.next() );
				idList.add ( i.getAssociatedIds() );
				k++;
				if (DEBUG) System.out.println("found "+spanList.get(k-1)+" ids: "+idList.get(k-1) );
			}
			assertEquals( expects.length, spanList.size() );
			for (int i=0; i<expects.length; i++) {
				Span s = (Span)spanList.get(i);
				assertEquals( expects[i].start, s.documentSpanStartIndex() );
				assertEquals( expects[i].length, s.size() );
				List ids = (List)idList.get(i);
				assertEquals( expects[i].ids.length, ids.size() );
				for (int j=0; j<expects[i].ids.length; j++) {
					//System.out.println("checking for "+expects[i].ids[j]+" in "+ids);
					assertTrue( ids.contains( expects[i].ids[j] ));
				}
			}
		}
		public static class TrieExpects {
			public String[] ids;
			int start,length;
			public TrieExpects(String[] ids, int start, int length) {
				this.ids = ids;	this.start = start;	this.length = length;
			}
		}
	}

	//
	// mixup-testing code
	//

	public static class MixupTest extends TestCase 
	{
		private TextBase b = new BasicTextBase();
		private TextLabels e = new BasicTextLabels(b);
		
		public MixupTest(String string) { 
			super(string);
		}
		public void doTest() {

			b.loadDocument("test1", "aa bb ccc dd ee ff");
			//
			// test basic mixup commands
			//
			checkExpr(e,"[any+] any{2,3}", new String[] { "aa bb ccc", "aa bb ccc dd" });
			checkExpr(e,"... [any 'ccc'?] re('[cdef]')*", new String[] { "bb", "bb ccc", "ccc", "dd", "ee", "ff" });
			checkExpr(e,"[any] ...", new String[] { "aa" });
			checkExpr(e,"... [any]", new String[] { "ff" });
			checkExpr(e,"any{1} [any] ...", new String[] { "bb" });
			checkExpr(e,"any{2,3} [any] ...", new String[] { "ccc", "dd" });
			checkExpr(e,"any{,2} [any] ...", new String[] { "aa", "bb", "ccc" });
			checkExpr(e,"any{3,} [any] ...", new String[] { "dd", "ee", "ff" });
			checkExpr(e,"[any{2,3}] ...", new String[] { "aa bb", "aa bb ccc" });
			checkExpr(e,"... 'bb' [any] ...", new String[] { "ccc" });
			checkExpr(e,"... !'bb' [!'bb'] ...", new String[] { "dd", "ee", "ff"});
			checkExpr(e,"... [re('...') any] ...", new String[] { "ccc dd" }); 
			checkExpr(e,"... [!re('^..$') any] ...", new String[] { "ccc dd" }); 
			checkExpr(e,"... [eq('ccc') any] ...", new String[] { "ccc dd" }); 
			checkExpr(e,"... [any any] ... && [re('[bcd]') any]", new String[] { "bb ccc", "ccc dd", "dd ee" });
			checkExpr(e,"... [re('[bc]')] ... || ... [re('[cd]')] ...", new String[] { "bb", "ccc", "dd" });
			checkExpr(e,"(... [re('[bc]')] ... || ... [re('[cd]')] ...) && [re('...')]", new String[] { "ccc" });
			checkExpr(e,"...[re('...')]... && ( [re('[bc]')] || [re('[cd]')] ) ", new String[] { "ccc" });
			checkExpr(e,"...[L re('^..$')+ R]...", new String[] { "aa bb", "dd ee ff" });
			checkExpr(e,"...<re('[bc]'),re('...')>[any]...", new String[] { "dd" });
			//
			// test program - out is the output tested against the 'expected' strings
			//
			checkProg(new String[] { "defTokenProp trigram:t =: ... [re('^...$')] ... ",
																 "defTokenProp bigram:t =: ... [re('^..$')] ... ",
																 "defSpanType out =: ... [trigram:t any] ..." },
								new String[] { "ccc dd"});
			checkProg(new String[] { "defTokenProp trigram:t =: ... [re('^...$')*] ...",
																 "defTokenProp bigram:t =: ... [re('^..$')*] ...",
																 "defSpanType out =: ... [trigram:t any] ..." },
								new String[] { "ccc dd"});
			checkProg(new String[] { "defTokenProp trigram:t =: ... [re('^...$')] ... ",
																 "defTokenProp bigram:t =: ... [re('^..$')] ... ",
																 "defSpanType trispan =: ... [trigram:t] ...",
																 "defSpanType bispan=: ... [bigram:t bigram:t] ...",
																 "defSpanType out =: ... [ @bispan @trispan ] ..." },
								new String[] { "aa bb ccc"});
			checkProg(new String[] { "defTokenProp trigram:t =: ... [re('^...$')] ... ",
																 "defTokenProp bigram:t =: ... [re('^..$')] ... ",
																 "defSpanType trispan =: ... [trigram:t] ...",
																 "defSpanType bispan=: ... [bigram:t bigram:t] ...",
															 "defSpanType out =: ... [ @bispan? @trispan ] ..." },
								new String[] { "aa bb ccc", "ccc" });
			checkProg(new String[] { "defSpanType out =~ re 'b+ (c+)', 1'"},
								new String[] { "ccc" });
			checkProg(new String[] { "defSpanType out =~ re 'b+ (c+\\s+)', 1'"},
			new String[] { "ccc" });
			checkProg(new String[] { "defSpanType pair =: ... [any any] ...", 
															 "defSpanType out =pair- ... ['ccc'] ... " }, 
								new String[] { "aa bb", "dd ee", "ee ff" });
			checkProg(new String[] { "defSpanType out =~ trie aa bb,bb ccc,bb ccc dd" },
			  new String[] { "aa bb", "bb ccc", "bb ccc dd" });
			//
			// test dictionaries and multiple documents
			//
			MutableTextLabels numLabels = new BasicTextLabels(b);
			String[] nums = new String[] { "one", "two", "three", "four", "five" };
			TreeSet numSet = new TreeSet();
			for (int i=0; i<nums.length; i++) numSet.add(nums[i]);
			numLabels.defineDictionary( "num", numSet );
			b.loadDocument("test2", "one fish, two fish");
			b.loadDocument("test3", "red fish, blue fish");
			b.loadDocument("test4", "one, two, three strikes you're out");
			b.loadDocument("test5", "Three phish");
			try {
				Mixup numExpr = new Mixup("... [a(num) <!a(num),re('[a-z]')>] ...");
				checkLooper( new String[] { "one fish", "two fish", "three strikes" },
										 numExpr.extract(numLabels, b.documentSpanIterator()) );
				new BoneheadStemmer().stem(b,numLabels);
				Mixup stemExpr = new Mixup(" ... [stem:a(num) stem:strik] ... ");
				checkLooper( new String[] { "three strikes" },
										 stemExpr.extract(numLabels, b.documentSpanIterator()) );
				Mixup aiExpr = new Mixup("[ai(num) any]"); 
				checkLooper( new String[] { "Three phish" },
										 aiExpr.extract( numLabels, b.documentSpanIterator() ) );
			} catch (Mixup.ParseException e) {
				throw new IllegalStateException("parse error "+e);
			}
		}
		private void checkProg(String[] statements, String[] expected) {
			try {
				MixupProgram program = new MixupProgram(statements);
				MutableTextLabels labels = new BasicTextLabels(b);
				if (DEBUG) System.out.println("checking program "+program);
				program.eval(labels, b);
				checkLooper( expected, labels.instanceIterator("out") );
			} catch (Mixup.ParseException e) {
				throw new IllegalStateException("parse error"+e);
			}
		}
		private void checkExpr(TextLabels e,String pattern,String[] expected) {
			if (DEBUG) System.out.println("checking "+pattern);
			
			try {
				Mixup mixup = new Mixup(pattern);
				checkLooper( expected, mixup.extract(e, b.documentSpanIterator()) );
			} catch (Mixup.ParseException ex) {
				throw new IllegalStateException("parse error"+ex);
			}
		}
		private void checkLooper( String[] expected, Span.Looper looper ) {
			ArrayList list = new ArrayList();
			while (looper.hasNext()) {
				Span s = looper.nextSpan();
				if (DEBUG) System.out.println(" - result '"+s+"'");
				list.add( s );
			}
			assertEquals( expected.length, list.size() );
			for (int i=0; i<list.size(); i++) {
				String[] toks = b.splitIntoTokens( expected[i] );
				Span span = (Span)list.get(i);
				assertEquals( toks.length, span.size() );
				if (DEBUG) System.out.print( "checking '"+span.toString()+"' vs expected '"+expected[i]+"'");
				for (int j=0; j<span.size(); j++) {
					assertEquals( toks[j], span.getToken(j).getValue() ); 
				}
				if (DEBUG) System.out.println("- passed.");
			}
		}
	}

	static public void main(String[] argv) {
		junit.textui.TestRunner.run(suite());
	}
}
