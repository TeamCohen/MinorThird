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
    static private final boolean NO_ADJUSTMENT=false; // if true, suppress the 'adjustment' phase

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
        double totAdjustments = 0;

        for (Span.Looper i=plainTextLabels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
            Span plainDocSpan = i.nextSpan();
            String plainString = plainDocSpan.getDocumentContents();

            String plainDocId = plainDocSpan.getDocumentId();
            String markupDocId = 
                plainDocId.substring( 0, plainDocId.length()-plainTextExtension.length() ) + markupExtension;

            System.out.print("aligning "+plainDocId+" to "+markupDocId);

            Document markupDoc = markupLabels.getTextBase().getDocument(markupDocId);
            if (markupDoc==null) {
                //throw new IllegalStateException("can't find marked version of "+plainDocId);                
                System.out.println("WARNING: can't find marked version of "+plainDocId);                
                continue;
            }

            String markupString = markupDoc.getText();

            System.out.print(" string lengths: "+plainString.length()+","+markupString.length());
            long t0 = System.currentTimeMillis();
            double score = aligner.score(markupString,plainString);
            long tf = System.currentTimeMillis();
            System.out.println(" score = "+score+" runtime = "+((tf-t0)/1000.0)+" sec"); 
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
                            Alignment alignment = new Alignment(plainSpan,markupSpan,plainTextLabels,markupLabels);
                            totAdjustments += alignment.adjust();
                            alignment.commit(type);
                            totAlignments++;

                            //a check on quality
                            if (checkAlignments && !alignment.match()) {
                                //System.out.println(markupSpan+" aligned to "+plainSpan);
                                //System.out.println(type+" align: "+lo+","+hi+" => "+lo1+","+hi1);
                                //System.out.println("error? '"+markupString.substring(lo,hi)+"' => '"+plainString.substring(lo1,hi1)+"'");
                                //System.out.println("error? '"+markupSpan.asString()+"' => '"+plainSpan.asString()+"'");
                                totErrors++;
                                double errorDistance = -errorCounter.score( markupString.substring(lo,hi), plainString.substring(lo1,hi1) );
                                if (errorDistance>Math.max( hi-lo, hi1-lo1 )) {
                                    //totErrorDistance += Math.max( hi-lo, hi1-lo1 );
                                    System.out.println("WARNING: infinite error distance for possible mis-alignment?");
                                } else {
                                    totErrorDistance += errorDistance;
                                    //System.out.println("errorDistance: "+errorDistance+" totErrorDistance="+totErrorDistance);
                                }
                            } // end check
                        } // end alignment found
                        totDistance += Math.max( hi-lo, hi1-lo1 );
                    } // end if markupSpan.size>0
                } // for span k of type j
                plainTextLabels.closeTypeInside(type,plainDocSpan);
            } // for type j
        } // for document i
        if (totAlignments>0) System.out.println("adjustments:      "+totAdjustments+"/"+totAlignments+" = "+(totAdjustments/totAlignments));
        if (totAlignments>0) System.out.println("alignment errors: "+totErrors+"/"+totAlignments+" = "+(totErrors/totAlignments));
        if (totDistance>0) System.out.println("Error distance: "+totErrorDistance +"/"+ totDistance + " = "+(totErrorDistance/totDistance));
    }

    static private class Alignment
    {
        private static final int LO_DELTA1 = -3, LO_DELTA2 = +3;
        private static final int LEN_DELTA1 = -3, LEN_DELTA2 = +3; 

        Span plainSpan;
        final Span markupSpan;
        final MutableTextLabels plainLabels; 
        final TextLabels markupLabels;
        // save result of last doTokenMatch comparison
        private Boolean priorResult = null;

        public Alignment(Span plainSpan,Span markupSpan,MutableTextLabels plainLabels,TextLabels markupLabels)
        {
            this.plainSpan=plainSpan; this.markupSpan=markupSpan;
            this.plainLabels=plainLabels; this.markupLabels=markupLabels;
        }
        /** Change the plainTextLabels by adding the plainSpan to the type */
        public void commit(String type)
        {
            plainLabels.addToType(plainSpan,type);
        }
        /** Try and improve the local alignment by moving the
         * boundaries of the plainText span by a token or so in either
         * direction. Return 1 or 0, indicating if an adjustment was
         * made. */
        public int adjust()
        {
            if (NO_ADJUSTMENT) return 0;

            if (markupSpanMatch(plainSpan)) {
                priorResult = new Boolean(true);
                return 0; // none necessary
            }
            //System.out.println("adjusting plainSpan to match "+markupSpan);
            Span docSpan = plainSpan.documentSpan();
            for (int lo=plainSpan.documentSpanStartIndex()+LO_DELTA1; lo<=plainSpan.documentSpanStartIndex()+LO_DELTA2; lo++) {
                for (int len=plainSpan.size()+LEN_DELTA1; len<=plainSpan.size()+LEN_DELTA2; len++) {
                    if (lo>0 && lo+len<=docSpan.size()) {
                        //System.out.println("testing "+lo+":"+(lo+len));
                        Span adjustedPlainSpan = docSpan.subSpan( lo, len );
                        if (markupSpanMatch(adjustedPlainSpan)) {
                            //System.out.println("correcting plainSpan from "+plainSpan+" to "+adjustedPlainSpan);
                            plainSpan = adjustedPlainSpan;
                            priorResult = new Boolean(true);
                            return 1;
                        }
                    }
                }
            }
            //if (!mungedTokens()) System.out.println("adjustment fails for "+plainSpan+"==>"+markupSpan);
            priorResult = new Boolean(false); // no adjustment worked
            return 0;
        }

        // plainText/markupText results match token-by-token
        public boolean match()
        {
            if (priorResult==null) priorResult=new Boolean(markupSpanMatch(plainSpan));
            return priorResult.booleanValue() || mungedTokens();
        }

        // a likely explanation for apparent mis-alignments
        private boolean mungedTokens()
        {
            if (plainSpan.asString().indexOf("'t")>=0) return true; 
            if (markupSpan.asString().indexOf("-LBR-")>=0) return true; 
            if (markupSpan.asString().indexOf("-RBR-")>=0) return true; 
            if (markupSpan.asString().indexOf("--")>=0) return true; 
            return false;            
        }

        private boolean markupSpanMatch(Span span)
        {
            // token match is hopeless for these...
            boolean ok = true;
            String[] markupToks = markupLabels.getTextBase().getTokenizer().splitIntoTokens(markupSpan.asString());
            String[] plainToks = markupLabels.getTextBase().getTokenizer().splitIntoTokens(span.asString());
            if (markupToks.length!=plainToks.length) {
                ok = false;
            } else {
                for (int m=0; ok && m<markupToks.length; m++) {
                    if (!markupToks[m].equals(plainToks[m])) ok=false;
                }
            }
            priorResult = new Boolean(ok);
            return ok;
        }
    }
}
