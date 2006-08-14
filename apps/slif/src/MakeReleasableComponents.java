import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.learn.experiments.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.apache.log4j.*;

/**
 * This creates encapsulated Minorthird annotators for all of the
 * text-based SLIF components.  The released components will be places
 * in the subdirectory 'dist'.  Certain tmp files are placed in
 * 'dist/helper'; these are not needed by the final encapsulated
 * annotators in 'dist'.
 *
*/

/* Currently, only the CRF-based extractors actually work.  The
 * DictHMMs need retrained, or else need a fix to SVM serialization. 
 * The semiCRFs have some other problem.
*/

public class MakeReleasableComponents
{
    final static String[] PROTEIN_ANNOTATORS = new String[]{
        "CRFonUt","CRFonYapex","CRFongenia",
        "DictHMMonGenia","DictHMMonUt","DictHMMonYapex",
        "semiCRFongenia","semiCRFonut","semiCRFonyapex"
    };
    final static String[] IMG_PTR_CLASSES = new String[]{
        "local","regional"
    };

    static public void main(String[] args) throws IOException
    {
        String sep = File.pathSeparator;
        String[] exportedTypes = new String[]{"protein","fillMeInLater"};
        File distDir = new File("dist");
        
        // convert trained filters to annotators
        for (int i=0; i<IMG_PTR_CLASSES.length; i++) {
            String ci = IMG_PTR_CLASSES[i];
            BinaryClassifier filter = (BinaryClassifier)IOUtil.loadSerialized(new File("lib/"+ci+"Filter.ser"));
            ImgPtrFilterAnnotator filterAnnotator = new ImgPtrFilterAnnotator(filter,ci);
            IOUtil.saveSerialized(filterAnnotator,new File("dist/helper/"+ci+"Filter.ann"));
        }
        // build the annotator for scopes
        String path0 = 
            "class/ImagePointerAnnotator.class" +sep+
            "dist/helper/scope.mixup" +sep+ "dist/helper/local.mixup" +sep+ "dist/helper/regional.mixup" +sep+ 
            "dist/helper/regionalFilter.ann" +sep+ "dist/helper/localFilter.ann" +sep+ "dist/helper/features.mixup" +sep+
            "dist/helper/caption.mixup";
        System.out.println("encapsulating caption with "+path0);
        EncapsulatedAnnotator ann0 = new EncapsulatedAnnotator("caption",path0);
        IOUtil.saveSerialized(ann0,new File(distDir,"caption.eann"));

        // build encapsulated versions of the various protein entity extractors
        for (int i=0; i<PROTEIN_ANNOTATORS.length; i++) {
            String pi = PROTEIN_ANNOTATORS[i];
            // fix annoying inconsistencies
            String cleanPi = pi.replaceAll("Ut","Texas").replaceAll("ut","Texas").replaceAll("yapex","Yapex").replaceAll("genia","Genia").replaceAll("semi","Semi");
            String req = "protein"+cleanPi;
            makeHelper(pi,cleanPi,req);
            String path = "lib/"+pi +sep+ "lib/proteinFeatures.mixup" +sep+ "dist/helper/protein"+cleanPi+".mixup";
            exportedTypes[1] = "proteinFrom"+cleanPi;
            System.out.println("encapsulating "+pi);
            EncapsulatedAnnotator ann = new EncapsulatedAnnotator(req,path,exportedTypes);
            IOUtil.saveSerialized(ann,new File(distDir,cleanPi+".eann"));
        }

        // copy over the encapsulated cell line annotator
        Annotator cann = (Annotator)IOUtil.loadSerialized(new File("lib/CellLine.eann"));
        IOUtil.saveSerialized((Serializable)cann,new File(distDir,"CellLine.eann"));
    }

    /**
     * A serializable annotator based on a learned filter of image pointer candidates. 
     */
    static private class ImgPtrFilterAnnotator extends AbstractAnnotator implements Serializable
    {
        private BinaryClassifier filter;
        private String className;
        private transient Annotator annotator = null;

        public ImgPtrFilterAnnotator(BinaryClassifier filter,String className) 
        { 
            this.filter=filter; this.className=className; 
        }

        public String explainAnnotation(TextLabels labels,Span span) { return "no explanation"; }

        public void doAnnotate(MonotonicTextLabels labels)
        {
            if (annotator==null) {
                SpanFeatureExtractor fe = new LearnImagePtrExtractor.ImgPtrFE();
                SpanFinder candidateFinder = LearnImagePtrExtractor.candidateFinder; 
                annotator = new FinderAnnotator( new FilteredFinder(filter,fe,candidateFinder), className);
            }
            annotator.annotate(labels);
        }
    }

    static private void makeHelper(String annotator,String cleanAnnotator,String requiredAnnotation) throws IOException,FileNotFoundException
    {
        PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File("dist/helper/"+requiredAnnotation+".mixup"))));
        out.println("provide '"+requiredAnnotation+"';");
        out.println();
        out.println("annotateWith "+annotator+";");
        out.println("defSpanType protein =_prediction: [...];");
        out.println("defSpanType proteinFrom"+cleanAnnotator+" =_prediction: [...];");
        out.close();
    }
}
