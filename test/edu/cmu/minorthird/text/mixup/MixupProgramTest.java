/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.mixup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.Document;
import edu.cmu.minorthird.text.EncapsulatingAnnotatorLoader;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextToken;
import edu.cmu.minorthird.util.gui.SmartVanillaViewer;
import edu.cmu.minorthird.util.gui.ViewerFrame;

/**
 *
 * @author William Cohen
 * @author Quinten Mercer
 */

public class MixupProgramTest extends TestSuite{

	private static Logger log=Logger.getLogger(MixupProgramTest.class);

	private static final boolean DEBUG=false;

	public MixupProgramTest(String name){
		super(name);
	}

	public static TestSuite suite(){
		TestSuite suite=new TestSuite();
		//        suite.addTest( new SimpleProgramTest() );
		//suite.addTest( new NestedProgramTest1() );
		//suite.addTest( new NestedProgramTest2() );
		suite.addTest(new NestedProgramTest3());
		//suite.addTest( new ImplicitDeclareTest() );
		//suite.addTest( new MultiLevelTest() );
		return suite;
	}

	public static class AbstractProgramTest extends TestCase{

		protected MonotonicTextLabels labels;

		protected final String testCaseDir=
				"edu/cmu/minorthird/text/mixup/testcases";

		protected final String sep=File.pathSeparator;

		public AbstractProgramTest(String string){
			super(string);
			BasicTextBase base=new BasicTextBase();
			String trialDoc=
					contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			labels=new BasicTextLabels(base);
		}

		protected void checkTime(TextLabels labels){
			// should be one time, "10:45 a.m."
			Iterator<Span> i=labels.instanceIterator("extracted_time");
			assertTrue(i.hasNext());
			assertEquals("10:45 a.m.",i.next().asString());
			assertTrue(!i.hasNext());
		}

		protected void checkRoom(TextLabels labels){
			// should be one time, "1112"
			Iterator<Span> i=labels.instanceIterator("extracted_room");
			assertTrue(i.hasNext());
			assertEquals("1112",i.next().asString());
			assertTrue(!i.hasNext());
		}

		protected void checkDate(TextLabels labels){
			//should contain two dates, "Tuesday", "Feb. 21" and one time, "10:45 a.m."
			Iterator<Span> i=labels.instanceIterator("extracted_date");
			assertTrue(i.hasNext());
			assertEquals("Tuesday",i.next().asString());
			assertTrue(i.hasNext());
			assertEquals("Feb. 21",i.next().asString());
			assertTrue(!i.hasNext());
		}

		protected void checkName(TextLabels labels){
			//should contain two names, Doherty Hall and Warren Baker
			Iterator<Span> i=labels.instanceIterator("extracted_name");
			assertTrue(i.hasNext());
			assertEquals("Doherty Hall",i.next().asString().replaceAll("\\s"," "));
			assertTrue(i.hasNext());
			assertEquals("Warren Baker",i.next().asString());
			assertTrue(!i.hasNext());
		}
	}

	/** make sure implicit declarations work */
	public static class ImplicitDeclareTest extends AbstractProgramTest{

		public ImplicitDeclareTest(){
			super("doTest");
		}

		public void doTest() throws Mixup.ParseException{
			String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";
			String progDef=
					contentsOfResourceFile(testCaseDir+"/implicitDeclare.mixup");
			MixupProgram prog=new MixupProgram(progDef);
			BasicTextBase base=new BasicTextBase();
			String trialDoc=
					contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			MonotonicTextLabels labels=new BasicTextLabels(base);
			MixupInterpreter interp=new MixupInterpreter(prog);
			interp.eval(labels); // just check there's no error raised
		}
	}

	/**  directly runs a mixup program which does not call anything else */
	public static class SimpleProgramTest extends AbstractProgramTest{

		public SimpleProgramTest(){
			super("doTest");
		}

		public void doTest() throws Mixup.ParseException{
			String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";
			String timeProgDef=contentsOfResourceFile(testCaseDir+"/xtime.mixup");
			MixupProgram timeProg=new MixupProgram(timeProgDef);
			BasicTextBase base=new BasicTextBase();
			String trialDoc=
					contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			MonotonicTextLabels labels=new BasicTextLabels(base);
			MixupInterpreter interp=new MixupInterpreter(timeProg);
			interp.eval(labels);
			if(DEBUG)
				new ViewerFrame("xtime result",new SmartVanillaViewer(labels));
			checkTime(labels);
		}
	}

	/** tests a mixup program which requires another mixup program using
	 * an EncapsulatingAnnotatorLoader */
	public static class NestedProgramTest1 extends AbstractProgramTest{

		public NestedProgramTest1(){
			super("doTest");
		}

		public void doTest() throws Mixup.ParseException{
			String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";
			String sep=File.pathSeparator;
			BasicTextBase base=new BasicTextBase();
			String trialDoc=
					contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			MonotonicTextLabels labels=new BasicTextLabels(base);
			EncapsulatingAnnotatorLoader eal=
					new EncapsulatingAnnotatorLoader(false,testCaseDir+"/xtime.mixup"+
							sep+testCaseDir+"/xdate.mixup");
			labels.setAnnotatorLoader(eal);
			MixupProgram callingProgram=new MixupProgram("require 'xdate';");
			MixupInterpreter interp=new MixupInterpreter(callingProgram);
			interp.eval(labels);
			if(DEBUG)
				new ViewerFrame("xdate result",new SmartVanillaViewer(labels));
			checkTime(labels);
			checkDate(labels);
		}
	}

	/** tests a mixup program which requires another mixup program 
	 * that's loaded from the classpath.  This requires 'time.mixup'
	 * to be on the classpath.
	 */
	public static class NestedProgramTest2 extends AbstractProgramTest{

		public NestedProgramTest2(){
			super("doTest");
		}

		public void doTest() throws Mixup.ParseException{
			try{
				InputStream trialStream=
						this.getClass().getClassLoader().getResourceAsStream("time.mixup");
				if(trialStream==null)
					throw new IllegalStateException(
							"null stream returned by getResourceAsStream");
			}catch(Exception e){
				log
						.warn("NestedProgramTest2 not run because couldn't find time.mixup on classpath.\nReason was: "+
								e);
				return;
			}
			String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";
			BasicTextBase base=new BasicTextBase();
			String trialDoc=
					contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			MonotonicTextLabels labels=new BasicTextLabels(base);
			EncapsulatingAnnotatorLoader eal=
					new EncapsulatingAnnotatorLoader(false,testCaseDir+"/ydate.mixup");
			labels.setAnnotatorLoader(eal);
			MixupProgram callingProgram=new MixupProgram("require 'ydate';");
			MixupInterpreter interp=new MixupInterpreter(callingProgram);
			interp.eval(labels);
			if(DEBUG)
				new ViewerFrame("ydate result",new SmartVanillaViewer(labels));
			checkTime(labels);
			checkDate(labels);
		}
	}

	/** 
	 * Tests mixup zall that requires xdate (which requires xtime) and
	 * a java class RoomNumber which provides rooms.  the compiled class
	 * file for RoomNumber should be kept in the testcases directory.
	 */
	public static class NestedProgramTest3 extends AbstractProgramTest{

		public NestedProgramTest3(){
			super("doTest");
		}

		public void doTest() throws Mixup.ParseException{
			EncapsulatingAnnotatorLoader eal=
					new EncapsulatingAnnotatorLoader(false,testCaseDir+"/zall.mixup"+sep+
							testCaseDir+"/xdate.mixup"+sep+testCaseDir+"/xtime.mixup"+sep+
							testCaseDir+"/RoomNumber.class");
			labels.setAnnotatorLoader(eal);
			labels.require("zall",null);
			if(DEBUG)
				new ViewerFrame("zall result",new SmartVanillaViewer(labels));
			checkTime(labels);
			checkDate(labels);
			checkRoom(labels);
			checkName(labels);
		}
	}

	public static class MultiLevelTest extends AbstractProgramTest{

		public MultiLevelTest(){
			super("doTest");
		}

		public void doTest() throws Mixup.ParseException{
			String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";

			// Load the trial document into a text base
			BasicTextBase base=new BasicTextBase();
			String trialDoc=
					contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);

			// Load and execute a couple mixup programs to create some labels in this document.
			String timeProgDef=contentsOfResourceFile(testCaseDir+"/xtime.mixup");
			MixupProgram timeProg=new MixupProgram(timeProgDef);
			MonotonicTextLabels labels=new BasicTextLabels(base);
			MixupInterpreter interp=new MixupInterpreter(timeProg);
			interp.eval(labels);
			String dateProgDef=contentsOfResourceFile(testCaseDir+"/xdate.mixup");
			MixupProgram dateProg=new MixupProgram(dateProgDef);
			interp=new MixupInterpreter(dateProg);
			interp.eval(labels);

			// Now test the Multi-Level functions

			// Pseudotokens
			String pseudotokenProgDef=
					"defLevel newLevel = pseudotoken extracted_date;\n";
			pseudotokenProgDef+="onLevel newLevel;\n";
			pseudotokenProgDef+=
					"importFromLevel root extracted_date = extracted_date;\n";
			MixupProgram pseudotokenProg=new MixupProgram(pseudotokenProgDef);
			interp=new MixupInterpreter(pseudotokenProg);
			interp.eval(labels);
			MonotonicTextLabels annotatedLabels=interp.getCurrentLabels();

			//should contain two dates, "Tuesday", "Feb. 21" both should be made up of a single token 
			Iterator<Span> i=annotatedLabels.instanceIterator("extracted_date");
			// WARNING: There doesn't seem to be a way to get a list of tokens or spans that have a token property set.
			//Span.Looper i = annotatedLabels.getSpansWithProperty("Pseudotoken");
			assertTrue(i.hasNext());
			Span s=i.next();
			assertEquals("Tuesday",s.asString());
			assertEquals(1,s.size());
			assertTrue(i.hasNext());
			s=i.next();
			assertEquals("Feb. 21",s.asString());
			assertEquals(1,s.size());
			assertTrue(!i.hasNext());

			// Split
			String splitProgDef="defLevel newLevel = split \'\\.\';\n";
			splitProgDef+="onLevel newLevel;\n";
			MixupProgram splitProg=new MixupProgram(splitProgDef);
			interp=new MixupInterpreter(splitProg);
			interp.eval(labels);
			annotatedLabels=interp.getCurrentLabels();

			// Should have tokenized the document up so that everything in between instances of '.' are single tokens.
			// For our example document this means that we should have 5 docs.

			Document d=annotatedLabels.getTextBase().getDocument("2477");
			assertNotNull(d);
			TextToken[] tokens=d.getTokens();
			assertEquals(6,tokens.length);
			assertEquals(
					"\nA seminar, entitled \"Graft Modification of Polymers in Twin Screw\nExtruders,\" will be given at 10:45 a",
					tokens[0].getValue());
			assertEquals("m",tokens[1].getValue());
			assertEquals(", Tuesday, Feb",tokens[2].getValue());
			assertEquals(" 21, in Doherty\nHall 1112",tokens[3].getValue());
			assertEquals(
					" Professor Warren Baker from the Department of Chemistry at\nQueen\'s University, Kingston, Canada, will conduct the seminar",
					tokens[4].getValue());
			assertEquals("\n",tokens[5].getValue());

			// Regex

			// Filter
			String filterProgDef="defLevel newLevel = filter extracted_date;\n";
			filterProgDef+="onLevel newLevel;\n";
			MixupProgram filterProg=new MixupProgram(filterProgDef);
			interp=new MixupInterpreter(filterProg);
			interp.eval(labels);
			annotatedLabels=interp.getCurrentLabels();

			// There should now be two documents, one for each instance of extracted_date
			i=annotatedLabels.getTextBase().documentSpanIterator();
			assertTrue(i.hasNext());
			assertEquals("Tuesday",i.next().asString());
			assertTrue(i.hasNext());
			assertEquals("Feb. 21",i.next().asString());
			assertTrue(!i.hasNext());
		}
	}

	static private String contentsOfResourceFile(String fileName){
		try{
			InputStream s=
					MixupProgramTest.class.getClassLoader().getResourceAsStream(fileName);
			if(s==null)
				throw new IllegalStateException("can't find resouce "+fileName);
			byte[] buf=new byte[s.available()];
			s.read(buf);
			s.close();
			return new String(buf);
		}catch(IOException ex){
			throw new IllegalStateException("couldn't find resouce '"+fileName+"': "+
					ex);
		}
	}

	static public void main(String[] argv){
		junit.textui.TestRunner.run(suite());
	}
}
