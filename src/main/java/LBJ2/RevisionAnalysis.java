package LBJ2;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import LBJ2.IR.AST;
import LBJ2.IR.ClassifierAssignment;
import LBJ2.IR.ClassifierName;
import LBJ2.IR.CodedClassifier;
import LBJ2.IR.CompositeGenerator;
import LBJ2.IR.Conjunction;
import LBJ2.IR.Constant;
import LBJ2.IR.ConstraintDeclaration;
import LBJ2.IR.DeclarationList;
import LBJ2.IR.InferenceDeclaration;
import LBJ2.IR.InferenceInvocation;
import LBJ2.IR.LearningClassifierExpression;
import LBJ2.frontend.parser;
import LBJ2.frontend.Yylex;
import LBJ2.io.HexOutputStream;
import LBJ2.io.HexStringInputStream;


/**
  * To be run after <code>SemanticAnalysis</code>, this pass determines which
  * <code>CodeGenerator</code>s need to have their code generated and which
  * classifiers need to be trained based on the revisions made to the LBJ
  * source file.
  *
  * <p> A hard coded classifier, a constraint, or an inference named
  * <code>foo</code> needs its code regenerated iff at least one of the
  * following is true:
  * <ul>
  *   <li> The file <code>foo.java</code> does not exist.
  *   <li>
  *     Using the comments at the top of <code>foo.java</code>, it is
  *     determined that the code specifying <code>foo</code> has been revised.
  * </ul>
  * If the comments at the top of <code>foo.java</code> do not exist, or if
  * they don't have the expected form, the file will not be overwritten and an
  * error will be generated.
  *
  * <p> All <code>CodeGenerator</code>s are also labeled as either "affected"
  * (by a revision) or "unaffected".  An <code>CodeGenerator</code> named
  * <code>foo</code> is labeled "affected" iff at least one of the following
  * is true:
  * <ul>
  *   <li>
  *     <code>foo</code> is a hard coded classifier, a constraint, or an
  *     inference and either:
  *     <ul>
  *       <li> its code needed to be regenerated as described above or
  *       <li> it invokes another "affected" <code>CodeGenerator</code>.
  *     </ul>
  *   <li>
  *     <code>foo</code> is a learning classifier and at least one of its
  *     label or extractor classifiers is "affected".
  * </ul>
  *
  * <p> A learning classifier named <code>foo</code> needs to have its code
  * regenerated and retrained iff at least one of the following is true:
  * <ul>
  *   <li> The file <code>foo.java</code> does not exist.
  *   <li>
  *     Using the comments at the top of <code>foo.java</code>, it is
  *     determined that the code specifying <code>foo</code> has been revised.
  *   <li> At least one of its label or extractor classifiers is "affected".
  * </ul>
  *
  * @see    LBJ2.SemanticAnalysis
  * @author Nick Rizzolo
 **/
public class RevisionAnalysis extends Pass
{
  /** Constant representing the "unaffected" revision status. */
  public static final Integer UNAFFECTED = new Integer(0);
  /** Constant representing the "affected" revision status. */
  public static final Integer AFFECTED = new Integer(1);
  /** Constant representing the "revised" revision status. */
  public static final Integer REVISED = new Integer(2);
  /** The names of the three revision states. */
  public static final String[] statusNames =
    { "unaffected", "affected", "revised" };

  /**
    * Keeps track of the names of classifiers whose revision status has been
    * resolved.
   **/
  public static HashMap revisionStatus;
  /**
    * Set to <code>true</code> iff no code has changed since the compiler was
    * last run.
   **/
  public static boolean noChanges;


  /**
    * Prints the contents of {@link #revisionStatus} to <code>STDOUT</code>.
   **/
  public static void printRevisionStatus() {
    if (revisionStatus == null) {
      System.out.println("No revision statuses.");
      return;
    }

    for (Iterator I = revisionStatus.entrySet().iterator(); I.hasNext(); ) {
      Map.Entry e = (Map.Entry) I.next();
      String name = (String) e.getKey();
      Integer status = (Integer) e.getValue();
      System.out.println(name + ": " + statusToString(status));

      Object classifierExpression =
        SemanticAnalysis.representationTable.get(name);
      if (classifierExpression instanceof LearningClassifierExpression) {
        LearningClassifierExpression lce =
          (LearningClassifierExpression) classifierExpression;
        System.out.println(
            "  features: " + statusToString(lce.featuresStatus));
        System.out.println("  pruning: " + statusToString(lce.pruneStatus));
        System.out.println(
            "  learning: " + statusToString(lce.learningStatus));
        System.out.println(
            "  only code generation: " + lce.onlyCodeGeneration);
      }
    }
  }


  /**
    * Returns the name of a revision status, or <code>"no status"</code> if
    * the status is <code>null</code>.
   **/
  public static String statusToString(Integer status) {
    if (status == null) return "no status";
    return statusNames[status.intValue()];
  }


  /**
    * Read the second line from the specified classifier's generated code.
    *
    * @param name The name of the classifier.
    * @param line The line number at which the classifier whose source we're
    *             reading is declared in its LBJ source file.
    * @return The second line from the classifier's generated code without the
    *         opening comment marker (//), or <code>null</code> if the
    *         generated code doesn't exist or the file doesn't appear to be
    *         generated code.
   **/
  private static String readSecondLine(String name, int line) {
    name += ".java";
    if (Main.generatedSourceDirectory != null)
      name = Main.generatedSourceDirectory + File.separator + name;

    File javaSource = new File(name);
    if (!javaSource.exists()) return null;

    BufferedReader in = null;
    try { in = new BufferedReader(new FileReader(javaSource)); }
    catch (Exception e) {
      System.err.println("Can't open '" + name + "' for input: " + e);
      System.exit(1);
    }

    String line1 = "";
    String line2 = "";
    try {
      line1 = in.readLine();
      line2 = in.readLine();
    }
    catch (Exception e) {
      System.err.println("Can't read from '" + name + "': " + e);
      System.exit(1);
    }

    try { in.close(); }
    catch (Exception e) {
      System.err.println("Can't close file '" + name + "': " + e);
      System.exit(1);
    }

    if (line1 == null || line2 == null || !line2.startsWith("// ")
        || !TranslateToJava.disclaimer.equals(line1)) {
      reportError(line,
          "The file '" + name + "' does not appear to have been generated by "
          + "LBJ2, but LBJ2 needs to overwrite it.  Either remove the file, "
          + "or change the name of the classifier in '" + Main.sourceFilename
          + "'.");
      return null;
    }

    return line2.substring(3);
  }


  /**
    * This method reads the comments at the top of the file containing the
    * code corresponding to the specified code generating node to determine if
    * the LBJ source describing that code generator has been modified since
    * the LBJ2 compiler was last executed.
    *
    * @param node     The code generating node.
    * @param convert  Whether or not the code is converted to hexadecimal
    *                 compressed format.
    * @return <code>true</code> iff the associated Java file did not exist or
    *         it contained the expected comments and those comments indicate
    *         that a revision has taken place.
   **/
  private static boolean codeRevision(CodeGenerator node, boolean convert) {
    String name = node.getName();
    String line2 = readSecondLine(name, node.getLine());
    if (line2 == null) return true;
    String expected = null;

    if (convert) {
      PrintStream converter = null;
      ByteArrayOutputStream converted = new ByteArrayOutputStream();
      try {
        converter = new PrintStream(
                      new GZIPOutputStream(
                        new HexOutputStream(converted)));
      }
      catch (Exception e) {
        System.err.println("Could not create converter stream.");
        System.exit(1);
      }

      converter.print(node.shallow().toString());
      converter.close();

      expected = converted.toString();
    }
    else expected = node.shallow().toString();

    return !line2.equals(expected);
  }


  /**
    * Recursively propagates the information about which nodes are "affected".
    *
    * @param name The name of an affected node.
   **/
  private static void propagateAffected(String name) {
    boolean isCompositeGenerator =
      SemanticAnalysis.representationTable.get(name)
      instanceof CompositeGenerator;
    boolean isRevised = revisionStatus.get(name) == REVISED;

    HashSet dependors = (HashSet) SemanticAnalysis.dependorGraph.get(name);

    assert dependors != null : "null entry in dependorGraph for " + name;

    for (Iterator I = dependors.iterator(); I.hasNext(); ) {
      String dependor = (String) I.next();

      if (SemanticAnalysis.representationTable.get(dependor)
          instanceof LearningClassifierExpression) {
        LearningClassifierExpression lce =
          (LearningClassifierExpression)
            SemanticAnalysis.representationTable.get(dependor);

        if (lce.featuresStatus == null || lce.featuresStatus != REVISED)
          lce.featuresStatus = AFFECTED;
        if (lce.pruneStatus == null || lce.pruneStatus != REVISED)
          lce.pruneStatus = AFFECTED;
        if (lce.learningStatus == null || lce.learningStatus != REVISED)
          lce.learningStatus = AFFECTED;
        lce.startingRound = 1;
      }

      if (!revisionStatus.containsKey(dependor)) {
        if (isCompositeGenerator && isRevised
            && SemanticAnalysis.representationTable.get(dependor)
               instanceof LearningClassifierExpression)
          revisionStatus.put(dependor, REVISED);
        else revisionStatus.put(dependor, AFFECTED);
        propagateAffected((String) dependor);
      }
    }
  }


  /**
    * Instantiates a pass that runs on an entire <code>AST</code>.
    *
    * @param ast  The program to run this pass on.
   **/
  public RevisionAnalysis(AST ast) {
    super(ast);
    revisionStatus = new HashMap();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param list The node to process.
   **/
  public void run(DeclarationList list) {
    noChanges = true;
    if (list.size() == 0) return;

    runOnChildren(list);

    noChanges = revisionStatus.size() == 0;
    String[] revised =
      (String[]) revisionStatus.keySet().toArray(new String[0]);
    for (int i = 0; i < revised.length; ++i)
      propagateAffected(revised[i]);

    for (Iterator I = SemanticAnalysis.dependorGraph.keySet().iterator();
         I.hasNext(); ) {
      Object name = I.next();
      if (!revisionStatus.containsKey(name)) {
        revisionStatus.put(name, UNAFFECTED);

        if (SemanticAnalysis.representationTable.get(name)
            instanceof LearningClassifierExpression) {
          LearningClassifierExpression lce =
            (LearningClassifierExpression)
            SemanticAnalysis.representationTable.get(name);

          if (lce.featuresStatus == null) lce.featuresStatus = UNAFFECTED;
          if (lce.pruneStatus == null) lce.pruneStatus = UNAFFECTED;
          if (lce.learningStatus == null) lce.learningStatus = UNAFFECTED;
        }
      }
    }
  }


  /**
    * Parses a learning classifier expression out of an encoded string using
    * the automatically generated scanner and parser.
    *
    * @param s  The string out of which the learning classifier expression
    *           will be parsed.
    * @return The parsed learning classifier expression.
   **/
  private static LearningClassifierExpression parseLCE(String s) {
    Reader reader = null;
    try {
      reader =
        new BufferedReader(
            new InputStreamReader(
                new GZIPInputStream(
                    new HexStringInputStream(s))));
    }
    catch (Exception e) {
      System.err.println(
          "LBJ ERROR: Can't instantiate string parser for LCE:");
      e.printStackTrace();
      System.exit(1);
    }

    AST ast = null;
    try { ast = (AST) new parser(new Yylex(reader)).parse().value; }
    catch (Exception e) {
      System.err.println("LBJ ERROR: Can't parse LCE from string:");
      e.printStackTrace();
      System.exit(1);
    }

    SemanticAnalysis.runAndRestore(ast);
    ClassifierAssignment ca =
      (ClassifierAssignment) ast.declarations.iterator().next();
    return (LearningClassifierExpression) ca.expression;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce) {
    runOnChildren(lce);
    String lceName = lce.name.toString();

    String line2 = readSecondLine(lce.getName(), lce.getLine());
    if (line2 == null || line2.length() == 0 || line2.equals("rebuild")) {
      revisionStatus.put(lceName, REVISED);
      lce.featuresStatus = lce.pruneStatus = lce.learningStatus = AFFECTED;
      return;
    }

    LearningClassifierExpression oldLCE = parseLCE(line2);
    String exFilePath = lceName + ".ex";
    if (Main.generatedSourceDirectory != null)
      exFilePath =
        Main.generatedSourceDirectory + File.separator + exFilePath;
    String lexFilePath = lceName + ".lex";
    String lcFilePath = lceName + ".lc";
    if (Main.classDirectory != null) {
      String prefix = Main.classDirectory + File.separator;
      lexFilePath = prefix + lexFilePath;
      lcFilePath = prefix + lcFilePath;
    }
    File exFile = new File(exFilePath);
    File lexFile = new File(lexFilePath);
    File lcFile = new File(lcFilePath);

    boolean preExtractToDisk =
      lce.preExtract.value.startsWith("\"disk")
      || lce.preExtract.value.equals("true")
      || lce.preExtract.value.equals("\"true\"");
    boolean previousPreExtractToDisk =
      oldLCE.preExtract.value.startsWith("\"disk")
      || oldLCE.preExtract.value.equals("true")
      || oldLCE.preExtract.value.equals("\"true\"");

    if (!oldLCE.returnType.equals(lce.returnType)
        || !oldLCE.name.equals(lce.name)
        || !oldLCE.argument.equals(lce.argument)
        || (oldLCE.labeler == null
            ? lce.labeler != null
            : lce.labeler == null
              || !oldLCE.labeler.name.equals(lce.labeler.name))
        || !oldLCE.extractor.name.equals(lce.extractor.name)
        || (oldLCE.parser == null ? lce.parser != null
                                  : !oldLCE.parser.equals(lce.parser))
        || (oldLCE.featureEncoding == null
            ? lce.featureEncoding != null
            : lce.featureEncoding == null
              || !oldLCE.featureEncoding.value
                  .equals(lce.featureEncoding.value))
        || preExtractToDisk && !previousPreExtractToDisk
        || (preExtractToDisk ? !exFile.exists() : !lcFile.exists())
        || !lexFile.exists()) {
      revisionStatus.put(lceName, REVISED);
      lce.featuresStatus = lce.pruneStatus = lce.learningStatus = AFFECTED;
      return;
    }

    if ((oldLCE.pruneCountType == null
         ? lce.pruneCountType != null
         : !oldLCE.pruneCountType.equals(lce.pruneCountType))
        || (oldLCE.pruneThresholdType == null
            ? lce.pruneThresholdType != null
            : !oldLCE.pruneThresholdType.equals(lce.pruneThresholdType))
        || (oldLCE.pruneThreshold == null
            ? lce.pruneThreshold != null
            : !oldLCE.pruneThreshold.equals(lce.pruneThreshold))) {
      lce.featuresStatus = preExtractToDisk ? UNAFFECTED : REVISED;
      lce.pruneStatus = REVISED;
      lce.learningStatus = AFFECTED;
      lce.previousPruneCountType = oldLCE.pruneCountType;
      revisionStatus.put(lceName, AFFECTED);
      return;
    }

    if ((oldLCE.learnerName == null
         ? lce.learnerName != null
         : !oldLCE.learnerName.equals(lce.learnerName))
        || (oldLCE.learnerConstructor == null
            ? lce.learnerConstructor != null
            : !oldLCE.learnerConstructor.equals(lce.learnerConstructor))
        || (oldLCE.learnerParameterBlock == null
            ? lce.learnerParameterBlock != null
            : !oldLCE.learnerParameterBlock.toString()
               .equals(lce.learnerParameterBlock.toString()))
        || (oldLCE.K == null ? lce.K != null : !oldLCE.K.equals(lce.K))
        || oldLCE.splitPolicy != lce.splitPolicy
        || (oldLCE.testingMetric == null
            ? lce.testingMetric != null
            : !oldLCE.testingMetric.equals(lce.testingMetric))
        || !oldLCE.alpha.equals(lce.alpha)
        || !lcFile.exists()) {
      lce.featuresStatus = lce.pruneStatus =
        preExtractToDisk ? UNAFFECTED : REVISED;
      lce.learningStatus = REVISED;
      revisionStatus.put(lceName, AFFECTED);
      return;
    }

    if (oldLCE.rounds == null ? lce.rounds != null
                              : !oldLCE.rounds.equals(lce.rounds)) {
      lce.featuresStatus = lce.pruneStatus =
        preExtractToDisk ? UNAFFECTED : REVISED;
      lce.learningStatus = REVISED;
      revisionStatus.put(lceName, AFFECTED);

      if (lce.K == null && lce.parameterSets.size() == 0
          && lce.rounds instanceof Constant
          && oldLCE.rounds instanceof Constant) {
        int rounds =
          lce.rounds == null
          ? 1 : Integer.parseInt(((Constant) lce.rounds).value);
        int oldRounds =
          oldLCE.rounds == null
          ? 1 : Integer.parseInt(((Constant) oldLCE.rounds).value);
        if (rounds > oldRounds) lce.startingRound = oldRounds + 1;
      }

      return;
    }

    lce.onlyCodeGeneration =
      (oldLCE.comment == null ? lce.comment != null
                              : !oldLCE.comment.equals(lce.comment))
      || (oldLCE.cacheIn == null ? lce.cacheIn != null
                                 : !oldLCE.cacheIn.equals(lce.cacheIn))
      || oldLCE.singleExampleCache != lce.singleExampleCache
      || (oldLCE.evaluation == null
          ? lce.evaluation != null
          : !oldLCE.evaluation.equals(lce.evaluation));
    if (lce.onlyCodeGeneration) {
      revisionStatus.put(lceName, REVISED);
      lce.featuresStatus = lce.pruneStatus = lce.learningStatus = UNAFFECTED;
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cn The node to process.
   **/
  public void run(ClassifierName cn) {
    if (cn.referent == cn.name) return;
    if (codeRevision(cn, false))
      revisionStatus.put(cn.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc) {
    if (codeRevision(cc, true))
      revisionStatus.put(cc.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cg The node to process.
   **/
  public void run(CompositeGenerator cg) {
    runOnChildren(cg);
    if (codeRevision(cg, true))
      revisionStatus.put(cg.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Conjunction c) {
    runOnChildren(c);
    if (codeRevision(c, false))
      revisionStatus.put(c.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param i  The node to process.
   **/
  public void run(InferenceInvocation i) {
    if (codeRevision(i, false))
      revisionStatus.put(i.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cd The node to process.
   **/
  public void run(ConstraintDeclaration cd) {
    if (codeRevision(cd, true))
      revisionStatus.put(cd.name.toString(), REVISED);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param id The node to process.
   **/
  public void run(InferenceDeclaration id) {
    if (codeRevision(id, true))
      revisionStatus.put(id.name.toString(), REVISED);
    run(id.constraint);
  }
}

