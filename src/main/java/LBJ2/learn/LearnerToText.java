package LBJ2.learn;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import LBJ2.util.ClassUtils;


/**
  * This extremely simple class can be used to print a textual representation
  * of a trained learner to <code>STDOUT</code>.  This is achieved with the
  * following line of code:
  *
  * <blockquote><code> learner.write(System.out); </code></blockquote>
  *
  * <h4>Usage</h4>
  * <blockquote>
  *   <code>
  *     java LBJ2.learn.LearnerToText &lt;learner&gt;
  *   </code>
  * </blockquote>
  *
  * <h4>Input</h4>
  * The <code>&lt;learner&gt;</code> parameter must be a fully qualified class
  * name (e.g. <code>myPackage.myName</code>) referring to a class that
  * extends {@link Learner}.  Every learning classifier specified in an LBJ
  * source code satisfies this requirement.
  *
  * <h4>Output</h4>
  * A textual representation of the specified learning classifier is produced
  * on <code>STDOUT</code>.
  *
  * @author Nick Rizzolo
 **/
public class LearnerToText
{
  public static void main(String[] args) {
    String learnerName = null;

    try {
      learnerName = args[0];
      if (args.length > 1) throw new Exception();
    }
    catch (Exception e) {
      System.err.println("usage: java LBJ2.learn.LearnerToText <learner>");
      System.exit(1);
    }

    Learner learner = ClassUtils.getLearner(learnerName);
    learner.demandLexicon();
    PrintStream out = new PrintStream(new BufferedOutputStream(System.out));
    learner.write(out);
    out.close();
  }
}

