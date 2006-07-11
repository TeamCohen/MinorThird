import edu.cmu.minorthird.text.*;

/** Trivial annotator used as test case. */
public class RoomNumber extends AbstractAnnotator
{
	public String explainAnnotation(TextLabels labels,Span documentSpan) 
	{ 
		return "Not implemented"; 
	}
	protected void doAnnotate(MonotonicTextLabels labels)
	{
		for (Span.Looper i=labels.getTextBase().documentSpanIterator(); i.hasNext(); ) {
			Span s = i.nextSpan();
			for (int j=0; j<s.size(); j++) {
				if (s.getToken(j).getValue().equals("1112")) {
					Span r = s.subSpan(j,1);
					labels.addToType(r, "extracted_room");
				}
			}
		}
		labels.setAnnotatedBy("RoomNumber");
	}
}
