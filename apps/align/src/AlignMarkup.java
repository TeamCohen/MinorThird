import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import com.wcohen.ss.api.*;
import com.wcohen.ss.*;

import java.io.*;
import java.util.*;

/** Takes as input two directories, one containing documents with
 * XML-based markup, and one with similar plain-text documents, and
 * tries to import the XML markup to the plain-text documents.
 *
 * Specifically, this will try to align the each plain-text document
 * from dirWithPlainText with a similarly-named marked-up document
 * from the dirWithMarkup directory. ("Similarly-named" means that the
 * .plainTextExtension, by default ".txt", is replaced with the
 * markupExtension, by default ".xml").  Alignment is done with a fast
 * (linear time and space) variant of NeedlemanWunch edit distance.
 * If an alignment is found, then each marked-span in the marked-up
 * document (i.e., each span that has been assigned a span type) will
 * be aligned to the corresponding span in the plain-text document,
 * and the span Type will be given to the corresponding span.  The
 * 'imported' labels for the documents in the dirWithPlainText
 * directory will be written out to 'labelFile'.
 */

public class AlignMarkup
{
    static private boolean checkAlignments = true;

    static public void main(String[] args) throws Exception
    {
        if (args.length<3) {
            System.out.println("usage: dirWithMarkup dirWithPlainText labelFile [.plainTextExtension .markupExtension]");
            System.exit(-1);
        }
        String dirWithMarkup = args[0];
        String dirWithPlainText = args[1];
        String labelFileName = args[2];
        String markupExtension = args.length>3 ? args[3] : ".xml";
        String plainTextExtension = args.length>4 ? args[4] : ".txt";

        TextBaseLoader markupLoader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE,TextBaseLoader.USE_XML);
        TextBase markupBase = markupLoader.load(new File(dirWithMarkup));
        TextLabels markupLabels = markupLoader.getLabels();
        
        TextBaseLoader plainTextLoader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE,TextBaseLoader.USE_XML);
        TextBase plainTextBase = plainTextLoader.load(new File(dirWithPlainText));
        MutableTextLabels plainTextLabels = new BasicTextLabels(plainTextBase);

        alignMarkup(markupLabels,plainTextLabels,markupExtension,plainTextExtension);

        new TextLabelsLoader().saveTypesAsOps(plainTextLabels,new File(labelFileName));

        //new ViewerFrame("original markup",new SmartVanillaViewer(markupLabels));
        //new ViewerFrame("imported markup",new SmartVanillaViewer(plainTextLabels));
    }

    static private void alignMarkup(TextLabels markupLabels,MutableTextLabels plainTextLabels,
                                    String markupExtension,String plainTextExtension)
    {
        ApproxNeedlemanWunsch aligner = new ApproxNeedlemanWunsch(CharMatchScore.DIST_01, 1.0);
        ApproxNeedlemanWunsch errorCounter = new ApproxNeedlemanWunsch(CharMatchScore.DIST_01, 1.0);
        aligner.setWidth(200);
        double totErrors = 0;
        double totErrorDistance = 0;
        double totDistance = 0;
        double totAlignments = 0;

        for (Span.Looper i=plainTextLabels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
            Span plainDocSpan = i.nextSpan();
            String plainString = plainDocSpan.getDocumentContents();

            String plainDocId = plainDocSpan.getDocumentId();
            String markupDocId = 
                plainDocId.substring( 0, plainDocId.length()-plainTextExtension.length() ) + markupExtension;

            System.out.println("aligning "+plainDocId+" to "+markupDocId);

            Document markupDoc = markupLabels.getTextBase().getDocument(markupDocId);
            if (markupDoc==null) throw new IllegalStateException("can't find marked version of "+plainDocId);
            String markupString = markupDoc.getText();

            System.out.println("string lengths: "+plainString.length()+","+markupString.length());
            long t0 = System.currentTimeMillis();
            double score = aligner.score(markupString,plainString);
            long tf = System.currentTimeMillis();
            System.out.println("score = "+score+" runtime = "+((tf-t0)/1000.0)+" sec"); 
            //System.out.println( aligner.explainScore(markupString,plainString) );

            for (Iterator j=markupLabels.getTypes().iterator(); j.hasNext(); ) {
                String type = (String)j.next();
                for (Span.Looper k=markupLabels.instanceIterator(type,markupDocId); k.hasNext(); ) {
                    Span markupSpan = k.nextSpan();
                    if (markupSpan.size()>0) {
                        int lo = markupSpan.getLoChar();
                        int hi = markupSpan.getHiChar();

                        // align first char of span to plaintext
                        int lo1 = aligner.getAlignedChar(lo,false);
                        // align last char of span to plaintext, add one for the limit
                        int hi1 = aligner.getAlignedChar(hi-1,true)+1;

                        if (lo1<0 || hi1<0 || lo1>plainString.length() || hi1>plainString.length() || lo1>=hi1) {
                            //System.out.println(type+" align failure: "+lo+","+hi+" => "+lo1+","+hi1);
                            totErrors++;
                            totErrorDistance += hi-lo;
                            totDistance += hi-lo;
                        } else {

                            Span plainSpan = plainDocSpan.charIndexSubSpan(lo1,hi1);
                            plainTextLabels.addToType( plainSpan, type );
                            totAlignments++;

                            //a check on quality
                            if (checkAlignments) {
                                boolean ok = true;
                                String[] markupToks = markupLabels.getTextBase().splitIntoTokens(markupSpan.asString());
                                String[] plainToks = markupLabels.getTextBase().splitIntoTokens(plainSpan.asString());
                                if (markupToks.length!=plainToks.length) {
                                    ok = false;
                                } else {
                                    for (int m=0; ok && m<markupToks.length; m++) {
                                        if (!markupToks[m].equals(plainToks[m])) ok=false;
                                    }
                                }
                                if (!ok) {
                                    //System.out.println(markupSpan+" aligned to "+plainSpan);
                                    //System.out.println(type+" align: "+lo+","+hi+" => "+lo1+","+hi1);
                                    //System.out.println("error? '"+markupString.substring(lo,hi)+"' => '"+plainString.substring(lo1,hi1)+"'");
                                    //System.out.println("error? '"+markupSpan.asString()+"' => '"+plainSpan.asString()+"'");
                                    totErrors++;
                                    totErrorDistance += -errorCounter.score( markupString.substring(lo,hi), plainString.substring(lo1,hi1) );
                                    totDistance += Math.max( hi-lo, hi1-lo1 );
                                }
                            } // end check
                        } // end alignment found
                    } // end if markupSpan.size>0
                } // for span k of type j
                plainTextLabels.closeTypeInside(type,plainDocSpan);
            } // for type j
        } // for document i
        if (totAlignments>0) System.out.println("alignment errors: "+totErrors+"/"+totAlignments+" = "+(totErrors/totAlignments));
        if (totDistance>0) System.out.println("Error distance: "+totErrorDistance +"/"+ totDistance + " = "+(totErrorDistance/totDistance));
    }
}
