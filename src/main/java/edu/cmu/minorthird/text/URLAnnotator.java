package edu.cmu.minorthird.text;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Override
		protected void doAnnotate(MonotonicTextLabels labels)
    {
        for (Iterator<Span> i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
            Span docSpan = i.next();
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

    @Override
		public String explainAnnotation(TextLabels labels,Span documentSpan)
    {
        return "no explanation available";
    }

}
