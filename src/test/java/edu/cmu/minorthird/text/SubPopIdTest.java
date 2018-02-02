/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.text;

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BasicDataset;
import edu.cmu.minorthird.classify.ClassLabel;
import edu.cmu.minorthird.classify.Dataset;
import edu.cmu.minorthird.classify.Example;
import edu.cmu.minorthird.text.learn.CVSplitterTest;
import edu.cmu.minorthird.text.learn.SampleFE;
import edu.cmu.minorthird.text.learn.SpanFeatureExtractor;

/**
 *
 * @author William Cohen
 */

public class SubPopIdTest extends TestCase
{
	private static Logger log = Logger.getLogger(CVSplitterTest.class);

	public SubPopIdTest(String name) { super(name); }
	public SubPopIdTest() { super("SubPopIdTest"); }
  public static Test suite() { return new TestSuite(SubPopIdTest.class); }
	
	public void testSubPop()
	{
		BasicTextBase base = new BasicTextBase();

		base.loadDocument("b1","Mud Club");
		base.setDocumentGroupId("b1","bar");
		base.loadDocument("b2","CBGB's");
		base.setDocumentGroupId("b2","bar");

		base.loadDocument("f1","Mud Pie");
		base.setDocumentGroupId("f1","foo");
		base.loadDocument("f2","PBJ's");
		base.setDocumentGroupId("f2","foo");

		SpanFeatureExtractor fe = SampleFE.BAG_OF_WORDS;

		Dataset data1 = new BasicDataset();
		for (Iterator<Span> i=base.documentSpanIterator(); i.hasNext(); ) {
			Span s = i.next();
			data1.add( new Example( fe.extractInstance(new EmptyLabels(), s), ClassLabel.binaryLabel(+1 ) ));
		}
		
		TextLabels labels = new BasicTextLabels(base);
		Dataset data2 = new BasicDataset();
		for (Iterator<Span> i=base.documentSpanIterator(); i.hasNext(); ) {
			Span s = i.next();
			data2.add( new Example( fe.extractInstance(labels,s), ClassLabel.binaryLabel(+1) ) );
		}

		checkSubPopIds(data1);
		checkSubPopIds(data2);
	}
	public void checkSubPopIds(Dataset d)
	{
		for (Iterator<Example> i=d.iterator(); i.hasNext(); ) {
			Example e = i.next();
			Span span = (Span)e.getSource();
			// make sure bi is in 'bar', and fi is in 'food'
			assertEquals( span.getDocumentId().substring(0,1),  e.getSubpopulationId().substring(0,1) );
			assertTrue( !span.getDocumentId().equals(e.getSubpopulationId()) );
			log.debug( "id: "+span.getDocumentId()+" subpop: "+e.getSubpopulationId() );
		}
	}
}

