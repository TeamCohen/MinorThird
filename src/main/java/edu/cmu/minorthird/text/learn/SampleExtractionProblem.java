package edu.cmu.minorthird.text.learn;

//import org.apache.log4j.Logger;

import java.util.Iterator;

import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.MonotonicTextLabels;
import edu.cmu.minorthird.text.NestedTextLabels;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Some sample inputs to facilitate testing.
 * 
 * @author William Cohen
 */

public class SampleExtractionProblem{

	// static private Logger log =
	// Logger.getLogger(SampleExtractionProblem.class);

	static private String[] trainStrings=
			{"Hello there, William Cohen, and welcome to CMU.",
					"William Clinton is a former US president.",
					"There's a new book by Hillary Clinton.",
					"George Washington was father of our country.",
					"Where in the world is Carmen Sandiego?",
					"Which George Bush was most damaging to the economy?",
					"I love books about Curious George the monkey."};

	static private String[] testStrings=
			{"Does William Cohen rock or what?",
					"Say what you like, William Clinton definitely had bad hair",
					"Who was George Mason anyway? did he invent jars?",
					"Don't blame me, I never voted for anyone named George Bush",};

	final static public String LABEL="trueName";

	static private String[] labelingProgram=
			{
					// useful features for learning
					"defTokenProp cap:t =: ... [re('^[A-Z][a-z]+')] ...",
					// "defDict +case fn = William, Carmen, George, Curious, Hillary",
					// "defTokenProp name:first =: ... [a(fn)] ... ",
					// "defTokenProp name:last =: ... a(fn) [any] ... ",
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
					"defSpanProp subject:other =top- [@political] || [...'William' 'Cohen'...]",};

	static public TextLabels trainLabels(){
		try{
			BasicTextBase base=trainBase();
			BasicTextLabels labels=new BasicTextLabels(base);
			MixupProgram prog=new MixupProgram(labelingProgram);
			MixupInterpreter interp=new MixupInterpreter(prog);
			interp.eval(labels);
			for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
				labels.closeTypeInside(LABEL,i.next());
			}
			new TextLabelsLoader().closeLabels(labels,
					TextLabelsLoader.CLOSE_ALL_TYPES);
			return labels;
		}catch(Mixup.ParseException e){
			throw new IllegalStateException("error: "+e);
		}
	}

	static public TextLabels taggerTrainLabels(){
		return tagNames(trainLabels());
	}

	static public TextLabels taggerTestLabels(){
		return tagNames(testLabels());
	}

	static private TextLabels tagNames(TextLabels labels){
		try{
			MonotonicTextLabels labels1=new NestedTextLabels(labels);
			MixupProgram p=
					new MixupProgram(
							new String[]{"defTokenProp partOfName:true =: ... [@trueName] ... "});
			MixupInterpreter interp=new MixupInterpreter(p);
			interp.eval(labels1);
			return labels1;
		}catch(Mixup.ParseException e){
			throw new IllegalStateException("error: "+e);
		}
	}

	static public BasicTextBase trainBase(){
		BasicTextBase base=new BasicTextBase();
		for(int i=0;i<trainStrings.length;i++){
			base.loadDocument("trainStrings["+i+"]",trainStrings[i]);
		}
		return base;
	}

	static public TextLabels testLabels(){
		try{
			BasicTextBase base=testBase();
			BasicTextLabels labels=new BasicTextLabels(base);
			MixupProgram prog=new MixupProgram(labelingProgram);
			MixupInterpreter interp=new MixupInterpreter(prog);
			interp.eval(labels);
			new TextLabelsLoader().closeLabels(labels,
					TextLabelsLoader.CLOSE_ALL_TYPES);
			return labels;
		}catch(Mixup.ParseException e){
			throw new IllegalStateException("error: "+e);
		}
	}

	static public BasicTextBase testBase(){
		BasicTextBase base=new BasicTextBase();
		for(int i=0;i<testStrings.length;i++){
			base.loadDocument("testStrings["+i+"]",testStrings[i]);
		}
		return base;
	}
}
