package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Annotate labels using a mixup program.
 *
 * @author William Cohen
 */

public class MixupAnnotator extends AbstractAnnotator
{
	private MixupProgram program;

	public MixupAnnotator(MixupProgram program)
	{ this.program = program; }

	protected void doAnnotate(MonotonicTextLabels labels)
	{ program.eval(labels, labels.getTextBase()); }

	public String explainAnnotation(TextLabels labels, Span documentSpan)
	{ return "annotated with mixup program"; }
}
