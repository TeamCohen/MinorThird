package edu.cmu.minorthird.text.learn;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.AnnotatorLearner;
import edu.cmu.minorthird.text.learn.AnnotatorTeacher;
import edu.cmu.minorthird.text.gui.TextBaseViewer;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupProgram;
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
  static private String capDef = "defTokenProp cap:t =: ... [re('^[A-Z][a-z]+')] ...";
  static private String[] labelingProgram = {
		capDef,
		"defDict fn = William, Carmen, George, Curious, Hillary",
		"defTokenProp name:first =: ... [a(fn)] ... ",
		"defTokenProp name:last =: ... a(fn) [any] ... ",
		"defSpanType trueName =~ trie William Cohen,William Clinton,Hillary Clinton,George Washington,"
		                         +"Carmen Sandiego,George Bush,Curious George,George Mason"
	};
	
	static public TextEnv trainEnv() {
		try {
      BasicTextBase base = trainBase();
      BasicTextEnv env = new BasicTextEnv(base);
			MixupProgram prog = new MixupProgram(labelingProgram);
			prog.eval(env, base);
			for (Span.Looper i=base.documentSpanIterator(); i.hasNext(); ) {
				env.closeTypeInside( LABEL, i.nextSpan() );
			}
			new TextEnvLoader().closeEnvironment(env,TextEnvLoader.CLOSE_ALL_TYPES);
			return env;
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

  static public TextEnv testEnv() {
		try {
      BasicTextBase base = testBase();
      BasicTextEnv env = new BasicTextEnv(base);
			MixupProgram prog = new MixupProgram(labelingProgram);
			prog.eval(env, base);
			new TextEnvLoader().closeEnvironment(env,TextEnvLoader.CLOSE_ALL_TYPES);
			return env;
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

  public static void main(String[] args)
	{
		try {
			TextEnv trainEnv = trainEnv();
			AnnotatorTeacher annotatorTeacher = new TextEnvAnnotatorTeacher( trainEnv, LABEL );
			AnnotatorLearner annotatorLearner = FancyLoader.loadAnnotatorLearner( args[0] );
			annotatorLearner.setAnnotationType( "predictedName" );
			Annotator learnedAnnotator = annotatorTeacher.train( annotatorLearner );
			System.out.println("Learned concept: "+learnedAnnotator);
			System.out.println("Viewing annotated training text base...");
			TextEnv trainEnv1 = learnedAnnotator.annotatedCopy( trainEnv );
			//System.out.println( trainEnv1 );
			TextBaseViewer.view( trainEnv1 );
			for (Span.Looper i=trainEnv1.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan();
				log.debug("extraction from train doc "+s+":\n" 
									+ learnedAnnotator.explainAnnotation( trainEnv1, s ));
			}
			System.out.println("Viewing annotated test text base...");
			TextEnv testEnv1 = learnedAnnotator.annotatedCopy( testEnv() );
			for (Span.Looper i=testEnv1.getTextBase().documentSpanIterator(); i.hasNext(); ) {
				Span s = i.nextSpan();
				log.debug("extraction from test doc "+s+":\n" 
									+ learnedAnnotator.explainAnnotation( testEnv1, s ));
			}
			TextBaseViewer.view( testEnv1 );
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: annotator-learner");
		}
	}

}
