import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.text.learn.*;

import java.util.*;

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

	protected void doAnnotate(MonotonicTextEnv env)
	{
		System.out.println("Annotating with HeaderNameTaggerV2: environment size="+env.getTextBase().size());

		// first find header areas
		try {
			MixupProgram prog = new MixupProgram(	new String[] {
				"defSpanType _startWord =top~ re '\\n\\n\\s*(\\S+)',1",
				"defSpanType _headerSection =top: [...] @_startWord ... ",
				"defSpanType _emailNameWord =_headerSection: ... [L re('^[a-z\\.]+$')+  ] eq('@') ... ", 
			});
			prog.eval(env, env.getTextBase() );
		} catch (Mixup.ParseException e) {
			throw new IllegalStateException("mixup error: "+e);
		}

		for (Span.Looper i=env.getTextBase().documentSpanIterator(); i.hasNext(); ) {

			Span doc = i.nextSpan();

			// collect tokens appearing in the HEADER_FIELDS of this document
			Set headerNames = new HashSet();
			for (Span.Looper k=env.instanceIterator("_emailNameWord", doc.getDocumentId()); k.hasNext(); ) {
				Span span = k.nextSpan();
				for (int h=0; h<span.size(); h++) {
					headerNames.add( span.getToken(h).getValue() );
				}
			}

			// mark all occurrences of these words in this document 
			for (int j=0; j<doc.size(); j++) {
				Token token = doc.getTextToken(j);
				if (headerNames.contains( token.getValue() )) {
					env.setProperty( token, HEADER_NAME_PROP, "t" );
				}
			}
		}

		// provide the annotation
		env.setAnnotatedBy( HEADER_NAME_ANNOTATION );
	}

	public String explainAnnotation(TextEnv e, Span s)
	{
		return "not implemented";
	}
}
