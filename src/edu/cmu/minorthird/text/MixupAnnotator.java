package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;

import java.io.*;

/**
 * Annotate labels using a mixup program.
 *
 * @author William Cohen
 */

public class MixupAnnotator extends AbstractAnnotator implements Serializable
{
    static private final long serialVersionUID = 1;
    private final int CURRENT_VERSION_NUMBER = 1;

    private MixupProgram program;

    public MixupAnnotator(MixupProgram program)
    { this.program = program; }

    protected void doAnnotate(MonotonicTextLabels labels)
    { program.eval(labels, labels.getTextBase()); }

    public String explainAnnotation(TextLabels labels, Span documentSpan)
    { return "annotated with mixup program"; }
}
