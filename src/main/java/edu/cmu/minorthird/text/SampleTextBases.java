package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.mixup.Mixup;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;

import java.util.Iterator;

/** Some sample inputs to facilitate testing.
 *
 * @author William Cohen
 */

public class SampleTextBases {

	static private String[] testStrings = {
		"Zhone Technologies Acquires NEC eLUMINANT",
		"Reuters to Acquire Multex",
		"Tumbleweed and Valicert Announce Merger Agreement",
		"TruSecure Corporation Acquires Vigilinx"
	};

	static private String[] testProgram = {
		"defDict companyWord = Reuters, Multex, TruSecure, Vigilinx ",
		"defSpanType company =: ... [a(companyWord)] ... ",
		"defTokenProp vp:t =: ( ... ['to'? re('^Acquires?') ] ... || ... ['Announce'] ...)",
		"defSpanType subj =: [!vp:t+] vp:t ...",
		"defSpanType obj =: !vp:t+ vp:t+ [!vp:t+]",
		"defSpanType start =top: ( [any{5}] any+ || [any{,5}])",
	};
	
	static private BasicTextBase base;
	static private MutableTextLabels truthLabels;
	static private MonotonicTextLabels guessLabels;
	static {
		try {
			base = new BasicTextBase();
			for (int i=0; i<testStrings.length; i++) {
                            base.loadDocument("testStrings["+i+"]", testStrings[i]);
			}
			truthLabels = new BasicTextLabels(base);
			MixupProgram prog = new MixupProgram(testProgram);
                        MixupInterpreter interp = new MixupInterpreter(prog);
			interp.eval(truthLabels);
			guessLabels = new NestedTextLabels( truthLabels );
			MixupProgram guessProg = new MixupProgram(new String[] { "defSpanType guess =: [ any{2} ] ..." });
                        interp.setProgram(guessProg);
			interp.eval(guessLabels);
		} catch (Mixup.ParseException e) {
			e.printStackTrace();
		}
	}

	static public TextBase getTextBase() { return base; }
	static public MutableTextLabels getTruthLabels() { return truthLabels; }
	static public MonotonicTextLabels getGuessLabels() { return guessLabels; }

	static public void showLabels(TextLabels labels) {
		System.out.println("labels has "+labels.getTypes().size()+" types");
		for (Iterator<String> i = labels.getTypes().iterator(); i.hasNext(); ) {
			String type = i.next();
			for (Iterator<Span> j = labels.instanceIterator(type); j.hasNext(); ) {
				System.out.println(type+": "+j.next());
			}
		}
	}
}

