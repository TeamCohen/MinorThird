package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.AnnotatorTeacher;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.classify.experiments.Expt;
import org.apache.log4j.Logger;

/** Some sample inputs to facilitate testing.
 *
 * @author William Cohen
 */

public class SampleExtractionProblem
{
	static private Logger log = Logger.getLogger(SampleExtractionProblem.class);

	static private String[] trainStrings = {
		"Hello there, William Cohen, and welcome to CMU.", 
		"William Clinton is a former US president.",
		"There's a new book by Hillary Clinton.",
		"George Washington was father of our country.",
		"Where in the world is Carmen Sandiego?",
		"Which George Bush was most damaging to the economy?",
		"I love books about Curious George the monkey."
	};

	static private String[] testStrings = {
		"Does William Cohen rock or what?",
		"Say what you like, William Clinton definitely had bad hair",
		"Who was George Mason anyway? did he invent jars?",
		"Don't blame me, I never voted for anyone named George Bush",
	};

  final static public String LABEL = "trueName";
  static private String[] labelingProgram = {
		// useful features for learning
		"defDict fn = William, Carmen, George, Curious, Hillary",
		"defTokenProp cap:t =: ... [re('^[A-Z][a-z]+')] ...",
		"defTokenProp name:first =: ... [a(fn)] ... ",
		"defTokenProp name:last =: ... a(fn) [any] ... ",
		// used to label the true names
		"defSpanType trueName =~ trie William Cohen,William Clinton,Hillary Clinton,George Washington,"
		     +"Carmen Sandiego,George Bush,Curious George,George Mason",
		// used as candidates to filter
		"defSpanType bigram =: ... [any any] ... ",
		// used to test learning against a span property
		"defSpanProp inCapsBecause:name =: ... [@trueName] ...",
		"defSpanProp inCapsBecause:start =: [any]...@trueName...",
		"defSpanType inCapsBecauseStart =: [any]...@trueName...",
		// used for classification tests
		"defSpanType political =: [ ... 'Clinton' ... ] || [... 'George' 'Bush' ...]",
		"defSpanProp subject:politics =: [@political]",
		"defSpanProp subject:me =: [...'William' 'Cohen'...]",
		"defSpanProp subject:other =top- [@political] || [...'William' 'Cohen'...]",
	};
	
	static public TextLabels trainLabels() {
		try {
      BasicTextBase base = trainBase();
      BasicTextLabels labels = new BasicTextLabels(base);
			MixupProgram prog = new MixupProgram(labelingProgram);
			prog.eval(labels, base);
			for (Span.Looper i=base.documentSpanIterator(); i.hasNext(); ) {
				labels.closeTypeInside( LABEL, i.nextSpan() );
			}
			new TextLabelsLoader().closeLabels(labels,TextLabelsLoader.CLOSE_ALL_TYPES);
			return labels;
		} catch (Mixup.ParseException e) {
			throw new IllegalStateException("error: "+e);
		}
	}

	static public TextLabels taggerTrainLabels() { return tagNames(trainLabels()); }
	static public TextLabels taggerTestLabels() { return tagNames(testLabels()); }
		
	static private TextLabels tagNames(TextLabels labels) 
	{
		try {
			MonotonicTextLabels labels1 = new NestedTextLabels(labels);
			MixupProgram p = new MixupProgram(new String[]{"defTokenProp partOfName:true =: ... [@trueName] ... "});
			p.eval(labels1, labels1.getTextBase());
			return labels1; 
		} catch (Mixup.ParseException e) {
			throw new IllegalStateException("error: "+e);			
		}
	}

  static public BasicTextBase trainBase()
  {
    BasicTextBase base = new BasicTextBase();
    for (int i=0; i<trainStrings.length; i++) {
      base.loadDocument("trainStrings["+i+"]", trainStrings[i]);
    }
    return base;
  }

  static public TextLabels testLabels() {
		try {
      BasicTextBase base = testBase();
      BasicTextLabels labels = new BasicTextLabels(base);
			MixupProgram prog = new MixupProgram(labelingProgram);
			prog.eval(labels, base);
			new TextLabelsLoader().closeLabels(labels,TextLabelsLoader.CLOSE_ALL_TYPES);
			return labels;
		} catch (Mixup.ParseException e) {
			throw new IllegalStateException("error: "+e);
		}
	}

  static public BasicTextBase testBase()
  {
    BasicTextBase base = new BasicTextBase();
    for (int i=0; i<testStrings.length; i++) {
      base.loadDocument("testStrings["+i+"]", testStrings[i]);
    }
    return base;
  }
}
