/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text.mixup;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.gui.*;
import junit.framework.*;
import org.apache.log4j.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author William Cohen
 */

public class MixupProgramTest extends TestSuite 
{
	private static Logger log = Logger.getLogger(MixupProgramTest.class);
	private static final boolean DEBUG = false;

	public MixupProgramTest(String name) { super(name); }

	public static TestSuite suite() {
		TestSuite suite = new TestSuite();
		suite.addTest( new SimpleProgramTest() );
		suite.addTest( new NestedProgramTest1() );
		suite.addTest( new NestedProgramTest2() );
		suite.addTest( new NestedProgramTest3() );
		return suite;
	}
		
	public static class AbstractProgramTest extends TestCase
	{
		protected MonotonicTextLabels labels;
		protected final String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";
		protected final String sep = File.pathSeparator;

		public AbstractProgramTest(String  string) 
		{ 
			super(string); 
			TextBase base = new BasicTextBase();
			String trialDoc = contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			labels = new BasicTextLabels(base);
		}
		protected void checkTime(TextLabels labels)
		{
			// should be one time, "10:45 a.m."
			Span.Looper i = labels.instanceIterator("extracted_time");
			assertTrue( i.hasNext() );
			assertEquals( "10:45 a.m.", i.nextSpan().asString() );
			assertTrue( !i.hasNext() );
		}
		protected void checkRoom(TextLabels labels)
		{
			// should be one time, "1112"
			Span.Looper i = labels.instanceIterator("extracted_room");
			assertTrue( i.hasNext() );
			assertEquals( "1112", i.nextSpan().asString() );
			assertTrue( !i.hasNext() );
		}
		protected void checkDate(TextLabels labels)
		{
			//should contain two dates, "Tuesday", "Feb. 21" and one time, "10:45 a.m."
			Span.Looper i = labels.instanceIterator("extracted_date");
			assertTrue( i.hasNext() );
			assertEquals( "Tuesday", i.nextSpan().asString() );
			assertTrue( i.hasNext() );
			assertEquals( "Feb. 21", i.nextSpan().asString() );
			assertTrue( !i.hasNext() );
		}
		protected void checkName(TextLabels labels)
		{
			//should contain two names, Doherty Hall and Warren Baker
			Span.Looper i = labels.instanceIterator("extracted_name");
			assertTrue( i.hasNext() );
			assertEquals( "Doherty\nHall", i.nextSpan().asString() );
			assertTrue( i.hasNext() );
			assertEquals( "Warren Baker", i.nextSpan().asString() );
			assertTrue( !i.hasNext() );
		}
	}

	/**  directly runs a mixup program which does not call anything else */
	public static class SimpleProgramTest extends AbstractProgramTest
	{
		public SimpleProgramTest() { super("doTest"); }
		public void doTest() throws Mixup.ParseException 
		{
			String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";
			String timeProgDef = contentsOfResourceFile(testCaseDir+"/xtime.mixup");
			MixupProgram timeProg = new MixupProgram(timeProgDef);
			TextBase base = new BasicTextBase();
			String trialDoc = contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			MonotonicTextLabels labels = new BasicTextLabels(base);
			timeProg.eval(labels,base);
			if (DEBUG) new ViewerFrame("xtime result", new SmartVanillaViewer(labels));
			checkTime(labels);
		}
	}

	/** tests a mixup program which requires another mixup program using
	 * an EncapsulatingAnnotatorLoader */
	public static class NestedProgramTest1 extends AbstractProgramTest
	{
		public NestedProgramTest1() { super("doTest"); }
		public void doTest() throws Mixup.ParseException 
		{
			String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";
			String sep = File.pathSeparator;
			TextBase base = new BasicTextBase();
			String trialDoc = contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			MonotonicTextLabels labels = new BasicTextLabels(base);
			EncapsulatingAnnotatorLoader eal = 
				new EncapsulatingAnnotatorLoader(false,testCaseDir+"/xtime.mixup" + sep + testCaseDir+"/xdate.mixup");
			labels.setAnnotatorLoader( eal );
			MixupProgram callingProgram = new MixupProgram("require 'xdate';");
			callingProgram.eval(labels,base);
			if (DEBUG) new ViewerFrame("xdate result", new SmartVanillaViewer(labels));
			checkTime(labels);
			checkDate(labels);
		}
	}

	/** tests a mixup program which requires another mixup program 
	 * that's loaded from the classpath.  This requires 'time.mixup'
	 * to be on the classpath.
	 */
	public static class NestedProgramTest2 extends AbstractProgramTest
	{
		public NestedProgramTest2() { super("doTest"); }
		public void doTest() throws Mixup.ParseException 
		{
			try {
				InputStream trialStream = this.getClass().getClassLoader().getResourceAsStream("time.mixup");
				if (trialStream==null) throw new IllegalStateException("null stream returned by getResourceAsStream");
			} catch (Exception e) {
				log.warn("NestedProgramTest2 not run because couldn't find time.mixup on classpath.\nReason was: "+e);
				return;
			}
			String testCaseDir="edu/cmu/minorthird/text/mixup/testcases";
			String sep = File.pathSeparator;
			TextBase base = new BasicTextBase();
			String trialDoc = contentsOfResourceFile(testCaseDir+"/seminar-official-news-2477.txt");
			base.loadDocument("2477",trialDoc);
			MonotonicTextLabels labels = new BasicTextLabels(base);
			EncapsulatingAnnotatorLoader eal = 
				new EncapsulatingAnnotatorLoader(false,testCaseDir+"/ydate.mixup");
			labels.setAnnotatorLoader( eal );
			MixupProgram callingProgram = new MixupProgram("require 'ydate';");
			callingProgram.eval(labels,base);
			if (DEBUG) new ViewerFrame("ydate result", new SmartVanillaViewer(labels));
			checkTime(labels);
			checkDate(labels);
		}
	}

	/** tests mixup zall that requires xdate (which requires xtime) and
	 * a java class RoomNumber which provides rooms.  the compiled class
	 * file for RoomNumber should be kept in the testcases directory.
	 */
	public static class NestedProgramTest3 extends AbstractProgramTest
	{
		public NestedProgramTest3() { super("doTest"); }
		public void doTest() throws Mixup.ParseException 
		{
			EncapsulatingAnnotatorLoader eal = 
				new EncapsulatingAnnotatorLoader(
					false,
					testCaseDir+"/zall.mixup" +sep +testCaseDir+"/xdate.mixup" +sep +testCaseDir+"/xtime.mixup"
					+sep +testCaseDir+"/RoomNumber.class");
			labels.setAnnotatorLoader( eal );
			labels.require("zall",null);
			if (DEBUG) new ViewerFrame("zall result", new SmartVanillaViewer(labels));
			checkTime(labels);
			checkDate(labels);
			checkRoom(labels);
			checkName(labels);
		}
	}

	static private String contentsOfResourceFile(String fileName)
	{
		try {
			InputStream s = MixupProgramTest.class.getClassLoader().getResourceAsStream(fileName);
			if (s==null) throw new IllegalStateException("can't find resouce "+fileName);
			byte[] buf = new byte[s.available()];
			s.read(buf);
			s.close();
			return new String(buf);
		} catch (IOException ex) {
			throw new IllegalStateException("couldn't find resouce '"+fileName+"': "+ex);
		}
	}


	static public void main(String[] argv) 
	{
		junit.textui.TestRunner.run(suite());
	}
}
