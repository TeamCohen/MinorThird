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

	public void doAnnotate(MonotonicTextLabels labels)
	{
            MixupInterpreter interp = new MixupInterpreter(featureProgram);
            interp.eval(labels);
            learnedAnnotator.annotate( labels );
	}

	public String explainAnnotation(TextLabels labels,Span span)
	{
		return "just because";
	}

	public static void main(String[] args)
	{
		try {
			PersonNameTagger tagger = new PersonNameTagger(args[0]);
			TextBaseLoader baseLoader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE, TextBaseLoader.FILE_NAME);
			TextBase base = baseLoader.load(new File(args[1]));

			MonotonicTextLabels labels = new BasicTextLabels( base );
			tagger.annotate( labels );
			saveType(labels, "predicted_name", new File(args[2]));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: annotatorFile mailDirectory tags");
		}
	}
	private static void saveType(TextLabels labels, String type, File file) throws FileNotFoundException
	{
		PrintStream out = new PrintStream(new FileOutputStream(file));
		for (Span.Looper j=labels.instanceIterator(type); j.hasNext(); ) {
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
