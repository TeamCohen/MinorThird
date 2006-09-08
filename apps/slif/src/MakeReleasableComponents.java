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
        File distDir = new File("dist"); // for things to distribute
        File helperDir = new File(distDir,"helper"); // temporary items
        
        // convert trained filters to encapsulated annotators
        for (int i=0; i<IMG_PTR_CLASSES.length; i++) {
            String ci = IMG_PTR_CLASSES[i];
            BinaryClassifier filter = (BinaryClassifier)IOUtil.loadSerialized(new File("lib/"+ci+"Filter.ser"));
            FinderAnnotator filterAnnotator = 
                new FinderAnnotator(new FilteredFinder(filter,new ImgPtrFE(),LearnImagePtrExtractor.candidateFinder),
                                    ci);
            String annotatorFileName = ci+"Filter.ann";
            IOUtil.saveSerialized(filterAnnotator,new File(helperDir,annotatorFileName));
            // this stuff is needed to encapsulate the localFilter.ann, regionalFilter.ann
            makeFilterHelper(annotatorFileName,ci);
            String pathi = "class/ImgPtrFE.class" +sep+ "dist/helper/"+annotatorFileName +sep+ "dist/helper/"+ci+".mixup";
            EncapsulatedAnnotator anni = new EncapsulatedAnnotator(ci,pathi);
            System.out.println("encapsulating "+ci+" with "+pathi);
            IOUtil.saveSerialized(anni,new File(helperDir,ci+"Filter.eann"));
        }

        // none of the stuff below needs to be tested just yet...
        if (true) return;

        // build the annotator for scopes, which uses the filter annotators
        String path0 = 
            "class/ImagePointerAnnotator.class" +sep+
            "class/ImgPtrFE.class" +sep+
            "dist/helper/scope.mixup" +sep+ "dist/helper/local.mixup" +sep+ "dist/helper/regional.mixup" +sep+ 
            "dist/helper/regionalFilter.eann" +sep+ "dist/helper/localFilter.eann" +sep+ "dist/helper/features.mixup" +sep+
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

    static private void makeFilterHelper(String annotatorFileName,String requiredAnnotation) throws IOException,FileNotFoundException
    {
        PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File("dist/helper/"+requiredAnnotation+".mixup"))));
        out.println("provide '"+requiredAnnotation+"';");
        out.println();
        out.println("annotateWith '"+annotatorFileName+"';");
        out.close();
    }
}
