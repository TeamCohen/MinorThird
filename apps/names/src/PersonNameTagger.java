import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;
import edu.cmu.minorthird.classify.algorithms.svm.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.sequential.*;
import java.util.*;
import java.io.*;

public class PersonNameTagger extends AbstractAnnotator
{
	private Annotator learnedAnnotator;
	private MixupProgram featureProgram;

	public PersonNameTagger(String learnedAnnotatorFile) throws IOException,Mixup.ParseException
	{
		learnedAnnotator = (Annotator)IOUtil.loadSerialized(new File(learnedAnnotatorFile));
		featureProgram = new MixupProgram(new File("nameFeatures.mixup")); 
	}

	public void doAnnotate(MonotonicTextEnv env)
	{
		featureProgram.eval( env, env.getTextBase() );
		learnedAnnotator.annotate( env );
	}

	public String explainAnnotation(TextEnv env,Span span)
	{
		return "just because";
	}

	public static void main(String[] args)
	{
		try {
			PersonNameTagger tagger = new PersonNameTagger(args[0]);
			TextBaseLoader baseLoader = new TextBaseLoader();
			TextBase base = new BasicTextBase();
			baseLoader.loadDir(base, new File(args[1]));
			MonotonicTextEnv env = new BasicTextEnv( base );
			tagger.annotate( env );
			saveType(env, "predicted_name", new File(args[2]));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: annotatorFile mailDirectory tags");
		}
	}
	private static void saveType(TextEnv env, String type, File file) throws FileNotFoundException
	{
		PrintStream out = new PrintStream(new FileOutputStream(file));
		for (Span.Looper j=env.instanceIterator(type); j.hasNext(); ) {
			Span s = j.nextSpan();
			if (s.size()>0) {
				int lo = s.getTextToken(0).getLo();
				int hi = s.getTextToken(s.size()-1).getHi();
				out.println("addToType "+s.getDocumentId()+" "+lo+" "+(hi-lo)+" "+type);
			}
		}
		out.close();
	}
}
