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
		
		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) 
		{
			Span doc = i.nextSpan();
			
			double count = 0;	
			boolean flag = false;		


			for (int j=0; j<doc.size(); j++)
			{
				Token token = doc.getTextToken(j);

				if (token.getValue().toLowerCase().equals("\""))
 				{
					count++;
					flag = true;	
				}

				if (count%2>0)
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
