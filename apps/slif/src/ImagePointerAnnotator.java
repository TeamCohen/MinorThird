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

// 'from' => ["Y-caption.txt"],
// 'produces' => ["proteinInPanelLabeled Y","cellInPanelLabeled Y", "Y-label.txt"],

public class ImagePointerAnnotator extends AbstractAnnotator implements Serializable
{

    /** The implementation for this method should explain how annotation
     * would be added to some part of the text base. */
    public String explainAnnotation(TextLabels labels,Span documentSpan)
    {
        return "no explanation";
    }
    

    protected void doAnnotate(MonotonicTextLabels labels)
    {
        MonotonicTextLabels tmpLabels = new NestedTextLabels(labels);
        //System.out.println("determine scopes");
        determineScopes(tmpLabels);
        //System.out.println("import scopes");
        importScopes(labels,tmpLabels);

        labels.setAnnotatedBy("imagePointer");
    }

    private Map imgPtrForScope; // scope span -> img Ptr span
    private List imagePtrList; // all imagePtr spans, local or regional
    private Map imagePtrDefinition; // imagePtr span -> set of strings that define the semantics
    private Set allLabels; // set of all imgPtr spans

    // define in labels:
    //  bulletStyle = regional
    //  citationStyle = local
    //  imgPtr = bulletStyle union citationStyle
    //  scope = bulletScope union citationScope
    //  labelSemantics(imgPtr||scope) = imgPtrDefinition
    //  labelSemantics(document) = allLabels

    private void importScopes(MonotonicTextLabels labels,TextLabels tmpLabels)
    {
        //System.out.println("importing img ptrs");
        defImgPtr("bulletStyle","regional",labels,tmpLabels);
        defImgPtr("citationStyle","local",labels,tmpLabels);
        //System.out.println("importing scopes");
        defScope("bulletScope","regionalScope",labels,tmpLabels);
        defScope("citationScope","localScope",labels,tmpLabels);
        defScope("globalScope","globalScope",labels,tmpLabels);
        //System.out.println("assigning global semantics");
        String sem = asString(allLabels);
        for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
            Span doc = i.nextSpan();
            labels.setProperty(doc,"semantics",sem);
        }
    }

    private void defScope(String type,String tmpType,MonotonicTextLabels labels,TextLabels tmpLabels)
    {
        //System.out.println("defScope: "+tmpType+" => "+type);
        for (Span.Looper i=tmpLabels.instanceIterator(tmpType); i.hasNext(); ) {
            Span s = i.nextSpan();
            labels.addToType(s, type);
            labels.addToType(s, "scope");
            Span t = (Span)imgPtrForScope.get(s);
            if (t!=null) {
                String sem = semanticsOf(t);
                if (sem!=null) labels.setProperty(s,"semantics",sem);
            }
        }
    }

    private String semanticsOf(Span s)
    {
        Set set = (Set)imagePtrDefinition.get(s);
        return set==null ? null : asString(set);
    }

    private void defImgPtr(String type,String tmpType,MonotonicTextLabels labels,TextLabels tmpLabels)
    {
        for (Span.Looper i=tmpLabels.instanceIterator(tmpType); i.hasNext(); ) {
            Span s = i.nextSpan();
            labels.addToType(s, type);
            labels.addToType(s, "imagePointer");
            String sem = semanticsOf(s);
            if (sem!=null) labels.setProperty(s,"semantics",sem);
        }
    }

    private String asString(Set set)
    {
        StringBuffer buf = new StringBuffer("");
        for (Iterator i=set.iterator(); i.hasNext(); ) {
            String s = (String)i.next();
            if (buf.length()==0) buf.append("\t");
            buf.append(s.toString());
        }
        return buf.toString();
    }

    private void determineScopes(MonotonicTextLabels labels)
    {
        // figure out which image pointer 'owns' which scope
        imgPtrForScope = new TreeMap(); 
        imagePtrList = new ArrayList();
        String[] ptrTypes = new String[] { "local", "regional" };
        for (int i=0; i<ptrTypes.length; i++) {
            ProgressCounter pc = new ProgressCounter("finding "+ptrTypes[i]+" scopes","image pointer",labels.getTextBase().size());
            for (Span.Looper j=labels.instanceIterator(ptrTypes[i]); j.hasNext(); ) {
                Span imgPtrSpan = j.nextSpan(); 
                imagePtrList.add(imgPtrSpan);
                //System.out.println("span for "+imgPtrSpan+" is ....");
                Span scopeSpan = findContainingSpan(imgPtrSpan, labels, ptrTypes[i]+"Scope");
                //System.out.println("span for "+imgPtrSpan+" is "+scopeSpan);
                if (scopeSpan!=null) imgPtrForScope.put( scopeSpan, imgPtrSpan );
                pc.progress();
            }
            pc.finished();
        }

        // expand out the 'definition' of the image pointers
        imagePtrDefinition = new TreeMap();
        allLabels = new TreeSet();
        Pattern p1 = Pattern.compile(".*\\b([A-Z])\\s*-\\s*([A-Z])\\b.*");
        Pattern p2 = Pattern.compile(".*\\b([a-z])\\s*-\\s*([a-z])\\b.*");
        Pattern p3 = Pattern.compile(".*\\b([A-Za-z])\\b.*");
        for (Iterator i=imagePtrList.iterator(); i.hasNext(); ) {
            Span span = (Span)i.next();
            //System.out.println("expanding: "+span);
            String string = span.asString();
            Matcher m1 = p1.matcher(string);
            while (m1.find()) defineRange(span,string,m1); 
            Matcher m2 = p2.matcher(string);
            while (m2.find()) defineRange(span,string,m2); 
            Matcher m3 = p3.matcher(string);
            while (m3.find()) defineLetter(span,string,m3);
        }
        //System.out.println("done with expanding");
    }

    // define the semantics of an imgPtrSpan of the form 'b-c'
    private void defineRange(Span span, String string, Matcher matcher)
    {
        char lo = string.charAt(matcher.start(1));
        char hi = string.charAt(matcher.start(2));
        TreeSet set = (TreeSet)imagePtrDefinition.get(span);
        if (set==null) imagePtrDefinition.put( span,  (set=new TreeSet()) );
        for (char ch=lo; ch<=hi; ch++) {
            StringBuffer buf = new StringBuffer("");
            buf.append(ch);
            set.add( buf.toString() );
            allLabels.add( buf.toString() );
        }
    }
    // define the semantics of an imgPtrSpan of the form 'a'
    private void defineLetter(Span span, String string, Matcher matcher)
    {
        char ch = string.charAt(matcher.start(1));
        TreeSet set = (TreeSet)imagePtrDefinition.get(span);
        if (set==null) imagePtrDefinition.put(span, (set=new TreeSet()) );
        StringBuffer buf = new StringBuffer("");
        buf.append(ch);
        set.add( buf.toString() );
    }

    // find a span of given type containing s
    private Span findContainingSpan(Span s,TextLabels labels,String type)
    {
        String id = s.getDocumentId();
        for (Span.Looper j=labels.instanceIterator(type,id); j.hasNext(); ) {
            Span t = j.nextSpan(); 
            if (t.contains(s)) return t;
        }
        return null;
    }

}    
