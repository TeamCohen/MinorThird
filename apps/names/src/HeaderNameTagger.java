import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.text.learn.*;

import java.util.*;

/**
 * Finds all tokens in a designated set of "header fields" for a
 * document, and marks every occurrence of these tokens in a document
 * as with the "headerName" property.
 */

public class HeaderNameTagger extends AbstractAnnotator
{
	static private final String HEADER_NAME_ANNOTATION = "headerNames";
	static private final String HEADER_NAME_PROP = "headerName";

	protected void doAnnotate(MonotonicTextLabels labels)
	{

		// first find header areas
		try {
			MixupProgram prog = new MixupProgram(	new String[] 
				{ "defSpanType _header =top~ re '\\n(From|To|Cc):\\s*\\S+\\s+([^\\n]+)',2" } );
                        MixupInterpreter interp = new MixupInterpreter(prog);
			interp.eval(labels);
		} catch (Mixup.ParseException e) {
			throw new IllegalStateException("mixup error: "+e);
		}

		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {

			Span doc = i.nextSpan();

			// collect tokens appearing in the HEADER_FIELDS of this document
			Set headerNames = new HashSet();
			for (Span.Looper k=labels.instanceIterator("_header", doc.getDocumentId()); k.hasNext(); ) {
				Span span = k.nextSpan();
				for (int h=0; h<span.size(); h++) {
					headerNames.add( span.getToken(h).getValue().toLowerCase() );
				}
			}

			// mark all occurrences of these words in this document 
			for (int j=0; j<doc.size(); j++) {
				Token token = doc.getTextToken(j);
				if (headerNames.contains( token.getValue().toLowerCase() )) {
					labels.setProperty( token, HEADER_NAME_PROP, "t" );
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
