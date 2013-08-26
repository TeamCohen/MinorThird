package edu.cmu.minorthird.text.learn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.util.IOUtil;

/** 
 * Wraps an ExtractorAnnotator with some code to implement
 * a simple API.
 */

public class ExtractorAnnotatorWrapper{

	private ExtractorAnnotator annotator;

	/** Construct from a file containing a serialized extractor, as
	 * would be learned by TrainExtractor.
	 */
	public ExtractorAnnotatorWrapper(File file) throws IOException{
		annotator=(ExtractorAnnotator)IOUtil.loadSerialized(file);
	}

	/** Construct from an InputStream containing a serialized extractor, as
	 * would be learned by TrainExtractor.
	 */
	public ExtractorAnnotatorWrapper(InputStream stream) throws IOException{
		annotator=(ExtractorAnnotator)IOUtil.loadSerialized(stream);
	}

	/** Construct from an annotator that implements the
	 * ExtractorAnnotator interface.
	 */
	public ExtractorAnnotatorWrapper(ExtractorAnnotator annotator){
		this.annotator=annotator;
	}

	public ExtractorAnnotator getAnnotator(){
		return annotator;
	}

	/** Apply the wrapped annotator to a string, and return all
	 * substrings extracted. 
	 */
	public String[] extractFrom(String source){
		BasicTextBase base=new BasicTextBase();
		base.loadDocument("noSource",source);
		MutableTextLabels labels=new BasicTextLabels(base);
		annotator.annotate(labels);
		List<String> accum=new ArrayList<String>();
		for(Iterator<Span> i=labels.instanceIterator(annotator.getSpanType());i
				.hasNext();){
			Span s=i.next();
			accum.add(s.asString());
		}
		return accum.toArray(new String[accum.size()]);
	}

	/** An interactive test routine to see if this thing works.
	 */
	public static void main(String[] args) throws Exception{
		if(args.length==0){
			System.out
					.println("usage: annotator-file sample-string1 sample-string2 ...");
		}else{
			ExtractorAnnotatorWrapper w=
					new ExtractorAnnotatorWrapper(new File(args[0]));
			for(int i=1;i<args.length;i++){
				String[] result=w.extractFrom(args[i]);
				System.out.println("Total of "+result.length+" extractions from arg"+i+
						":");
				for(int j=0;j<result.length;j++){
					System.out.println("\t'"+result[j]+"'");
				}
			}
		}
	}
}
