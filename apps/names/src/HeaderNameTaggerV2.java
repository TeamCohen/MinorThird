import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.text.learn.*;

import java.util.*;
import java.util.regex.*;
/**
 * Finds all tokens that appear in email addresses in the header, 
 * and marks every occurrence of these tokens in a document
 * with the "headerName" property.
 *
 * Version 2 is tuned for Enron data.
 */

public class HeaderNameTaggerV2 extends AbstractAnnotator
{
	static private final String HEADER_NAME_ANNOTATION = "headerNames_v2";
	static private final String HEADER_NAME_PROP = "headerName";

	protected void doAnnotate(MonotonicTextLabels labels)
	{
		System.out.println("Annotating with HeaderNameTaggerV2: labels size="+labels.getTextBase().size());

		Pattern emailRegex = Pattern.compile("\\b([a-z\\.]+)@");

		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {

			Span doc = i.nextSpan();

			// first find header areas
			String[] lines = doc.asString().split("\\n");
			int headerLen = 0;
			for (int j=0; j<lines.length; j++) {
				if (lines[j].length()>0) {
					Matcher matcher = emailRegex.matcher( lines[j] );
					while (matcher.find()) {
						Span email = doc.charIndexSubSpan( headerLen+matcher.start(1), headerLen+matcher.end(1) );
						labels.addToType( email, "emailNameSpan" );
					}
					headerLen += lines[j].length()+1;
				} else {
					Span header = doc.charIndexSubSpan( 0, headerLen );
					Span body = doc.charIndexSubSpan( headerLen, doc.getTextToken(doc.size()-1).getHi() );
					labels.addToType( header, "headerSection" ); 
					labels.addToType( body, "bodySection" );
					break;
				}
			}


			// collect tokens appearing in the email fields of this document
			Set headerNames = new HashSet();
			for (Span.Looper k=labels.instanceIterator("emailNameSpan", doc.getDocumentId()); k.hasNext(); ) {
				Span span = k.nextSpan();
				for (int h=0; h<span.size(); h++) {
					if (span.getToken(h).getValue().length()>1) {
						// don't bother saving one-token spans from email addresses, which are usually '.'
						headerNames.add( span.getToken(h).getValue().toLowerCase() );
					}
				}
			}

			// mark all occurrences of these words in the body of this document
			for (Span.Looper k=labels.instanceIterator("bodySection", doc.getDocumentId()); k.hasNext(); ) {
				Span body = k.nextSpan();
				for (int j=0; j<body.size(); j++) {
					Token token = body.getTextToken(j);
					if (headerNames.contains( token.getValue().toLowerCase() )) {
						labels.setProperty( token, HEADER_NAME_PROP, "t" );
					}
				}
			}
		}

		// provide the annotation
		labels.setAnnotatedBy( HEADER_NAME_ANNOTATION );
	}

	public String explainAnnotation(TextLabels e, Span s)
	{
		return "not implemented";
	}
}
