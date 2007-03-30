import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.text.learn.*;

import java.util.*;


public class QuotesTagger extends AbstractAnnotator
{
	static private final String QUOTED_ANNOTATION = "quoted";
	static private final String QUOTE_PROP = "quoted";

	protected void doAnnotate(MonotonicTextLabels labels)
	{
		
		// first mark single quotes that are used for abbreviations, to eliminate them later
		try {
			MixupProgram prog = new MixupProgram
			(new String[] { "defTokenProp notQuote:t =~ re '\\S(\\')\\S',1"});
                        MixupInterpreter interp = new MixupInterpreter(prog);
			interp.eval(labels);
		} catch (Mixup.ParseException e) {
			throw new IllegalStateException("mixup error: "+e);
		}		
		


		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) 
		{
			Span doc = i.nextSpan();
			
			double counter = 0;	
			

			for (int j=0; j<doc.size(); j++)
			{
				boolean isQuote = false;
				
				Token token = doc.getTextToken(j);
				
				if ((token.getValue().toLowerCase().equals("\"")) || 
				   ((token.getValue().toLowerCase().equals("\'")) && (labels.getProperty(token, "notQuote")==null))) 
				{
					isQuote = true;								
					counter++;
				}

				if (counter%2>0 && !isQuote)
				{
					labels.setProperty( token, QUOTE_PROP,"t" );
				}
				
			}


			// provide the annotation
			//labels.setAnnotatedBy( QUOTED_ANNOTATION );
		}
	}
	

	public String explainAnnotation(TextLabels e, Span s)
	{
		return "not implemented";
	}
}
