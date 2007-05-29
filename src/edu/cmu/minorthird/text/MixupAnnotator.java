package edu.cmu.minorthird.text;

import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import edu.cmu.minorthird.text.mixup.MixupInterpreter;
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

    //TODO: This class should be extended to allow the user to access different levels.  For 
    //     instance, we could create a method that allows them to specify the name of a 
    //     level to return.  Or they could specify a flag to return the level the program
    //     ended on.

    /**
     * Right now this method executes the mixup program associated with this annotator and the 
     * caller is expected to get the results directly out of the labels set that was passed in
     * originally.  This means that if the program creates new levels, it should also populate
     * any final results back to the root level using the importLabelsFromLevel method in
     * {@link edu.cmu.minorthird.text.mixup.MixupInterpreter}.  Otherwise these results will be lost.
     */
    protected void doAnnotate(MonotonicTextLabels labels) { 
        MixupInterpreter interp = new MixupInterpreter(program);
        interp.eval(labels); 
    }

    public String explainAnnotation(TextLabels labels, Span documentSpan)
    { return "annotated with mixup program"; }
}
