package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Annotate substrings that are legal URLs.
 *
 *
 * @author William Cohen
 */

public class URLAnnotator extends AbstractAnnotator
{
    static final Pattern URL_CANDIDATE = Pattern.compile("\\b(\\w+:)?/[/\\w;:\\@\\$\\-~#%\\?\\&\\+=\\.]+");
    static final String URL_SPANTYPE = "URL";
    static final String URL_ANNOTATION_TYPE = "URL";

    protected void doAnnotate(MonotonicTextLabels labels)
    {
        for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
            Span docSpan = i.nextSpan();
            String docString = docSpan.getDocumentContents();
            Matcher m = URL_CANDIDATE.matcher(docString);
            while (m.find()) {
                int lo = m.start();
                int hi = m.end();
                if (validURL( docString.substring(lo,hi) )) {
                    labels.addToType( docSpan.charIndexSubSpan(lo,hi), URL_SPANTYPE );
                }
            }
        }
        labels.setAnnotatedBy("URL");
    }
    
    private boolean validURL(String s) 
    {
        try {
            new URL(s);
            return true;
        } catch (MalformedURLException ex) {
        }
        return false;
    }

    public String explainAnnotation(TextLabels labels,Span documentSpan)
    {
        return "no explanation available";
    }

}
