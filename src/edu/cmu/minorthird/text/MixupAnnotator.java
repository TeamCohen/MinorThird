package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Annotate an environment using a mixup program.
 *
 * @author William Cohen
 */

public class MixupAnnotator extends AbstractAnnotator
{
	private MixupProgram program;

	public MixupAnnotator(MixupProgram program)
	{
		this.program = program;
	}
	protected void doAnnotate(MonotonicTextEnv env)
	{
		program.eval(env,env.getTextBase());
	}

	public String explainAnnotation(TextEnv env,Span documentSpan)
	{
		return "annotated with mixup program";
	}
}
