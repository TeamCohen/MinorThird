import java.util.Iterator;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.text.Span;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.learn.SpanFE;

/**
 * A sample feature extractor, intended for identifying person names.
 * To try this out, compile it, put it on your classpath, and use the
 * command:
 *
 * <code>
 * <pre>
 * java edu.cmu.minorthird.ui.TrainTestClassifier -labels sample1.train -test sample1.test -fe "new MyFE()" -spanType trueName -candidateType bigram -showResult 
 * </pre>
 * </code>
 */

public class MyFE extends SpanFE
{

	static final long serialVersionUID=20080224L;

	// below, 'instance' is a MutableInstance that 
	// can be extended.

	public void extractFeatures(TextLabels labels,Span span) 
	{
		// some examples of the instance-extraction sublanguage

		// add features for lower-case versions of all tokens in the span
		from(span).tokens().eq().lc().emit();
		// add features for the tokens to the left and right
		from(span).left().token(-1).eq().emit(); // -1 is last token
		// -in the left()
		// span
		from(span).right().token(0).eq().emit(); // 0 is first token
		// in the right()
		// span
		// the 'charTypePattern' for the span itself
		from(span).eq().charTypePattern().emit(); 
		// the first token in the span
		from(span).token(0).eq().emit();

		// the capitalization of the tokens in the span
		from(span).tokens().prop("cap").emit();


		// an example of a numeric feature
		instance.addNumeric( new Feature("lengthInChars"), myFunction(span) );

		// a complex feature based on POS tags
		// ask for POS annotations
		labels.require("pos",null);  // null means use the default
		// tagger
		// POS annotations are stored as span types on one-token-long
		// spans.  This loop collects all the noun-like tags inside
		// the span into a single feature
		String interestingPosTags = "";
		for (int i=0; i<span.size(); i++) {
			Span tokenSpan = span.subSpan(i,1);
			boolean interestingTagFound=false;
			for (Iterator<String> j=labels.getTypes().iterator(); j.hasNext() && !interestingTagFound; ) {
				String posType = j.next();
				if (posType.startsWith("N") && labels.hasType(tokenSpan,posType)) {
					interestingPosTags = interestingPosTags + posType;
					interestingTagFound = true;
				}
			}
			if (!interestingTagFound) interestingPosTags = interestingPosTags+"-";
		}
		// now add a new feature based on this
		instance.addBinary( new Feature("nounTags."+interestingPosTags) );

		// another complex feature 
		if (span.size()==2) {
			String token1 = span.getToken(0).getValue();
			String token2 = span.getToken(1).getValue();
			if (Character.isUpperCase(token1.charAt(0)) && Character.isUpperCase(token2.charAt(0))) {
				instance.addBinary( new Feature("looksLikeName") );
			}
		}
	}

	private double myFunction(Span span)
	{
		return span.asString().length();
	}
}

