package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.*;
import java.io.*;
import java.util.*;

/** 
 * Wraps an ExtractorAnnotator with some code to implement
 * a simple API.
 */

public class ExtractorAnnotatorWrapper
{
	private ExtractorAnnotator annotator;

	/** Construct from a file containing a serialized extractor, as
	 * would be learned by TrainExtractor.
	 */
	public ExtractorAnnotatorWrapper(File file) throws IOException
	{
		annotator = (ExtractorAnnotator)IOUtil.loadSerialized(file);
	}

	/** Construct from an InputStream containing a serialized extractor, as
	 * would be learned by TrainExtractor.
	 */
	public ExtractorAnnotatorWrapper(InputStream stream) throws IOException
	{
		annotator = (ExtractorAnnotator)IOUtil.loadSerialized(stream);
	}

	/** Construct from an annotator that implements the
	 * ExtractorAnnotator interface.
	 */
	public ExtractorAnnotatorWrapper(ExtractorAnnotator annotator)
	{
		this.annotator = annotator;
	}

	/** Apply the wrapped annotator to a string, and return all
	 * substrings extracted. 
	 */
	public String[] extractFrom(String source)
	{
		TextBase base = new BasicTextBase();
		base.loadDocument("noSource", source);
		MutableTextLabels labels = new BasicTextLabels(base);
		annotator.annotate(labels);
		List accum = new ArrayList();
		for (Span.Looper i=labels.instanceIterator(annotator.getSpanType()); i.hasNext(); ) {
			Span s = i.nextSpan();
			accum.add( s.asString() );
		}
		return (String[])accum.toArray(new String[accum.size()]);
	}

	/** An interactive test routine to see if this thing works.
	 */
	public static void main(String[] args) throws Exception
	{
		if (args.length==0) {
			System.out.println("usage: annotator-file sample-string1 sample-string2 ..."); 
		} else {
			ExtractorAnnotatorWrapper w = new ExtractorAnnotatorWrapper(new File(args[0]));
			for (int i=1; i<args.length; i++) {
				String[] result = w.extractFrom(args[i]);
				System.out.println("Total of "+result.length+" extractions from arg"+i+":");
				for (int j=0; j<result.length; j++) {
					System.out.println("\t'"+result[j]+"'");
				}
			}
		}
	}
}
