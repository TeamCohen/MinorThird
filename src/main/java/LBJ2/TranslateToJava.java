package LBJ2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.zip.GZIPOutputStream;

import LBJ2.IR.*;
import LBJ2.classify.Classifier;
import LBJ2.infer.EqualityArgumentReplacer;
import LBJ2.infer.FirstOrderEquality;
import LBJ2.io.HexOutputStream;


/**
  * This pass generates Java code from an AST, but does not perform any
  * training.
  *
  * @author Nick Rizzolo
 **/
public class TranslateToJava extends Pass
{
  /** The commented message appearing at the top of all generated files. */
  public static final String disclaimer =
    "// Modifying this comment will cause the next execution of LBJ2 to "
    + "overwrite this file.";
  /**
    * This array contains string descriptions of methods that don't need to be
    * overridden when generating code for a learner.
   **/
  private static final String[] noOverride =
    {
      "native public int hashCode()",
      "public LBJ2.classify.Feature featureValue(java.lang.Object a0)",
      "public LBJ2.classify.FeatureVector classify(java.lang.Object a0)",
      "public LBJ2.classify.FeatureVector[] classify(java.lang.Object[] a0)",
      "public boolean equals(java.lang.Object a0)",
      "public double realValue(java.lang.Object a0)",
      "public double[] realValueArray(java.lang.Object a0)",
      "public java.lang.Object clone()",
      "public java.lang.String discreteValue(java.lang.Object a0)",
      "public java.lang.String getInputType()",
      "public java.lang.String getOutputType()",
      "public java.lang.String toString()",
      "public java.lang.String[] allowableValues()",
      "public java.lang.String[] discreteValueArray(java.lang.Object a0)",
      "public java.util.LinkedList getCompositeChildren()",
      "public short valueIndexOf(java.lang.String a0)",
      "public void learn(java.lang.Object a0)",
      "public void learn(java.lang.Object[] a0)",
      "public void unclone()"
    };
  /**
    * The prefix of the name of the temporary variable in which a constraint's
    * computed value should be stored.  This variable is only used when
    * {@link #constraintMode} is unset.
   **/
  private static final String constraintResult = "LBJ2$constraint$result$";


  /** Used for collecting the string representation of a method body. */
  private StringBuffer methodBody;
  /** The indent level when collecting the method body. */
  private int indent;
  /** Lets AST children know about the node they are contained in. */
  private CodeGenerator currentCG;
  /**
    * Lets {@link VariableDeclaration}s know if they are contained in the
    * initialization portion of the header of a <code>for</code> loop.
   **/
  private boolean forInit;
  /**
    * Filenames that have been generated during the processing of one
    * statement.
   **/
  private HashSet files;
  /**
    * When this flag is set, code generated for constraint expressions will
    * create {@link LBJ2.infer.Constraint} objects rather than computing the
    * value of the constraint expression.
   **/
  private boolean constraintMode;
  /**
    * This variable is appended to the {@link #constraintResult} variable to
    * form the name of a new temporary variable.
   **/
  private int constraintResultNumber;
  /** The current constraint result variable name. */
  private String constraintResultName;
  /**
    * Lets AST children know the index that a given quantification variable
    * occupies in an {@link EqualityArgumentReplacer}'s vector; the keys of
    * the map are names of quantification variables, and the values are
    * <code>Integer</code>s.
   **/
  private HashMap quantificationVariables;
  /**
    * Lets AST children know the index that a given context variable occupies
    * in an {@link EqualityArgumentReplacer}'s vector; the keys of the map are
    * names of context variables, and the values are <code>Integer</code>s.
   **/
  private HashMap contextVariables;
  /**
    * Lets AST nodes know how deeply nested inside
    * {@link QuantifiedConstraintExpression}s they are.
   **/
  private int quantifierNesting;

  /**
    * Associates an AST with this pass.
    *
    * @param ast  The AST to associate with this pass.
   **/
  public TranslateToJava(AST ast) {
    super(ast);
    methodBody = new StringBuffer();
    files = new HashSet();
  }


  /**
    * Uses the current value of {@link #indent} to append the appropriate
    * number of spaces to {@link #methodBody}.
   **/
  private void appendIndent() {
    for (int i = 0; i < indent; ++i) methodBody.append("  ");
  }


  /**
    * Uses the current value of {@link #indent} to append the appropriate
    * number of spaces to {@link #methodBody}, followed by the argument
    * string.
    *
    * @param text The text to append after the indent.
   **/
  private void appendIndent(String text) {
    appendIndent();
    methodBody.append(text);
  }


  /**
    * Appends the current indent via {@link #appendIndent()}, then appends the
    * argument string and a newline.
    *
    * @param text The text to append as a new line.
   **/
  private void appendLine(String text) {
    appendIndent(text);
    methodBody.append("\n");
  }


  /**
    * Sets the current code generator for this translator.
    *
    * @param cg The new current code generator.
   **/
  public void setCurrentCG(CodeGenerator cg) { currentCG = cg; }


  /**
    * Sets the indentation level.
    *
    * @param i  The new indentation level.
   **/
  public void setIndent(int i) { indent = i; }


  /**
    * Gives access to the {@link #methodBody} member variable so that this
    * pass can be invoked selectively on some subset of a method body.
    *
    * @return The contents of {@link #methodBody} in a <code>String</code>.
   **/
  public String getMethodBody() { return methodBody.toString(); }


  /**
    * Create a <code>PrintStream</code> that writes to a Java file
    * corresponding to the specified {@link CodeGenerator}.
    *
    * @param node The code producing node.
    * @return The stream, or <code>null</code> if it couldn't be created.
   **/
  public static PrintStream open(CodeGenerator node) {
    return open(node.getName() + ".java");
  }


  /**
    * Create a <code>PrintStream</code> that writes to the specified file.
    *
    * @param name The name of the file to open.
    * @return The stream, or <code>null</code> if it couldn't be created.
   **/
  public static PrintStream open(String name) {
    if (Main.generatedSourceDirectory != null) {
      name = Main.generatedSourceDirectory + File.separator + name;

      String[] directories = name.split("\\" + File.separator + "+");
      File directory = new File(directories[0]);

      for (int i = 1; i < directories.length - 1; ++i) {
        directory = new File(directory + File.separator + directories[i]);

        if (!directory.exists() && !directory.mkdir()) {
          System.err.println("Can't create directory '" + directory + "'.");
          return null;
        }
      }
    }
    else if (Main.sourceDirectory != null)
      name = Main.sourceDirectory + File.separator + name;

    Main.fileNames.add(name);

    PrintStream out = null;

    try { out = new PrintStream(new FileOutputStream(name)); }
    catch (Exception e) {
      System.err.println("Can't open '" + name + "' for output: " + e);
    }

    return out;
  }


  /**
    * Generate the code that overrides certain methods of
    * {@link LBJ2.learn.Learner} to check types and call themselves on the
    * unique instance; also declares other methods and fields of the
    * classifier's implementation.  The explicitly overridden methods are:
    * <ul>
    *   <li> <code>getInputType()</code> </li>
    *   <li> <code>getOutputType()</code> </li>
    *   <li> <code>allowableValues()</code> </li>
    *   <li> <code>learn(Object)</code> </li>
    *   <li> <code>learn(Object[])</code> </li>
    *   <li> <code>classify(Object)</code> </li>
    *   <li> <code>classify(Object[])</code> </li>
    * </ul>
    *
    * In addition, any methods defined by any subclass of
    * {@link LBJ2.learn.Learner} down to the super class of this learner are
    * overridden to call the super class's implementation on the unique
    * instance.
    *
    * @param out  The stream to write to.
    * @param lce  The {@link LearningClassifierExpression} representing the
    *             learner.
   **/
  public static void generateLearnerBody(PrintStream out,
                                         LearningClassifierExpression lce) {
    String lceName = lce.name.toString();
    String field = null;
    boolean cachedInMap = false;

    out.println("  public static boolean isTraining;");
    out.println("  public static " + lceName + " instance;\n");

    out.println("  public static " + lceName + " getInstance()");
    out.println("  {");
    out.println("    loadInstance();");
    out.println("    return instance;");
    out.println("  }\n");

    if (lce.cacheIn != null) {
      field = lce.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap)
        out.println("  private static final WeakHashMap __valueCache "
                    + "= new WeakHashMap();\n");
    }

    HashSet invoked = (HashSet) SemanticAnalysis.invokedGraph.get(lceName);

    if (invoked != null && invoked.size() > 0) {
      for (Iterator I = invoked.iterator(); I.hasNext(); ) {
        String name = (String) I.next();
        String nameNoDots = name.replace('.', '$');
        out.println("  private static final " + name + " __" + nameNoDots
                    + " = new " + name + "();");
      }

      out.println();
    }

    if (lce.parameterSets.size() == 0) {
      out.println("  private " + lceName + "(boolean b)");
      out.println("  {");
      out.print("    super(");

      if (lce.learnerParameterBlock != null) out.print("new Parameters()");
      else {
        if (lce.learnerConstructor.arguments.size() > 0) {
          out.print(lce.learnerConstructor.arguments);
          if (lce.attributeString.length() != 0) out.print(", ");
        }

        if (lce.attributeString.length() != 0) out.print("attributeString");
      }

      out.println(");");
      out.println("    containingPackage = \""
                  + AST.globalSymbolTable.getPackage() + "\";");
      out.println("    name = \"" + lceName + "\";");
      out.println("    setEncoding(" + lce.featureEncoding + ");");
      if (lce.labeler != null)
        out.println("    setLabeler(new " + lce.labeler.name + "());");
      out.println("    setExtractor(new " + lce.extractor.name + "());");
      out.println("    isClone = false;");
      out.println("  }\n");
    }

    out.println("  public static TestingMetric getTestingMetric() { return "
                + lce.testingMetric + "; }\n");

    if (lce.singleExampleCache) {
      out.println(
          "  private static ThreadLocal __cache = new ThreadLocal(){ };");
      out.println("  private static ThreadLocal __exampleCache = "
                  + "new ThreadLocal(){ };");
      out.println("  public static void clearCache() { __exampleCache = new "
                  + "ThreadLocal() { }; }");
    }

    if (lce.attributeString.length() != 0
        && lce.learnerParameterBlock == null)
      out.println("  private static final String attributeString = \""
                  + lce.attributeString + "\";");

    out.println("\n  private boolean isClone;\n");

    out.println("  public void unclone() { isClone = false; }\n");

    out.println("  public " + lceName + "()");
    out.println("  {");
    String fqName = AST.globalSymbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += lceName;
    out.println("    super(\"" + fqName + "\");");
    out.println("    isClone = true;");
    out.println("  }\n");

    out.println("  public " + lceName
                + "(String modelPath, String lexiconPath) { "
                + "this(new Parameters(), modelPath, lexiconPath); }");
    out.println("  public " + lceName
                + "(Parameters p, String modelPath, String lexiconPath)");
    out.println("  {");
    out.println("    super(p);");
    out.println("    try {");
    out.println("      lcFilePath = new java.net.URL(\"file:\" + "
                + "modelPath);");
    out.println("      lexFilePath = new java.net.URL(\"file:\" + "
                + "lexiconPath);");
    out.println("    }");
    out.println("    catch (Exception e) {");
    out.println("      System.err.println(\"ERROR: Can't create model or "
                + "lexicon URL: \" + e);");
    out.println("      e.printStackTrace();");
    out.println("      System.exit(1);");
    out.println("    }\n");

    out.println("    if (new java.io.File(modelPath).exists()) {");
    out.println("      readModel(lcFilePath);");
    out.println("      readLexiconOnDemand(lexFilePath);");
    out.println("    }");
    out.println("    else {");
    out.println("      containingPackage = \""
                + AST.globalSymbolTable.getPackage() + "\";");
    out.println("      name = \"" + lceName + "\";");
    if (lce.labeler != null)
      out.println("      setLabeler(new " + lce.labeler.name + "());");
    out.println("      setExtractor(new " + lce.extractor.name + "());");
    out.println("    }\n");

    out.println("    isClone = false;");
    out.println("  }\n");

    LBJ2.IR.Type input = lce.argument.getType();
    String inputString = input.toString();
    int line = lce.line + 1;

    typeReturningMethods(out, input, lce.returnType);

    out.println("\n  public void learn(Object example)");
    out.println("  {");
    out.println("    if (isClone)");
    out.println("    {");
    out.print(
        generateTypeChecking("      ", lceName, "Classifier", false,
                             inputString, lce.line, "example", true));
    out.println("      loadInstance();");
    out.println("      instance.learn(example);");
    out.println("      return;");
    out.println("    }\n");

    out.println("    if (example instanceof Object[])");
    out.println("    {");
    out.println("      Object[] a = (Object[]) example;");
    out.println("      if (a[0] instanceof int[])");
    out.println("      {");
    out.println("        super.learn((int[]) a[0], (double[]) a[1], (int[]) "
                + "a[2], (double[]) a[3]);");
    out.println("        return;");
    out.println("      }");
    out.println("    }\n");

    out.println("    super.learn(example);");
    out.println("  }\n");

    out.println("  public void learn(Object[] examples)");
    out.println("  {");
    out.println("    if (isClone)");
    out.println("    {");
    out.print(
        generateTypeChecking("      ", lceName, "Classifier", true,
                             inputString, lce.line, "examples", true));
    out.println("      loadInstance();");
    out.println("      instance.learn(examples);");
    out.println("      return;");
    out.println("    }\n");

    out.println("    super.learn(examples);");
    out.println("  }\n");

    StringBuffer preamble = new StringBuffer();
    StringBuffer body = new StringBuffer();
    StringBuffer post = null;

    preamble.append(  "    if (isClone)\n"
                    + "    {\n");
    preamble.append(
        generateTypeChecking("      ", lceName, "Classifier", false,
                             inputString, lce.line, "__example", true));
    preamble.append(  "      loadInstance();\n"
                    + "      return instance.$METHOD$(__example);\n"
                    + "    }\n\n");

    preamble.append(
          "    if (__example instanceof Object[])\n"
        + "    {\n"
        + "      Object[] a = (Object[]) __example;\n"
        + "      if (a[0] instanceof int[])\n"
        + "        return super.$METHOD$((int[]) a[0], (double[]) a[1]);\n"
        + "    }\n\n");

    boolean primitive =
      lce.returnType.type == ClassifierReturnType.DISCRETE
      || lce.returnType.type == ClassifierReturnType.REAL;

    if (lce.evaluation == null) {
      body.append("    __result = super.");
      body.append(primitive ? "featureValue" : "classify");
      body.append("(__example);\n");
    }
    else {
      TranslateToJava translator = new TranslateToJava(null);
      translator.setRoot(lce.evaluation);
      translator.setCurrentCG(lce);
      translator.run();
      body.append("    __result = ");
      body.append(translator.getMethodBody());
      body.append(";\n");
    }

    if (lce.checkDiscreteValues) {
      post = new StringBuffer();
      String variable = "__result";
      String indent = "";

      if (!primitive) {
        post.append(
              "    for (int __i = 0; __i < __result.featuresSize(); ++__i)\n"
            + "    {\n"
            + "      Feature __f = __result.getFeature(__i);\n");
        variable = "__f";
        indent = "  ";
      }

      post.append(indent);
      post.append("    if (");
      post.append(variable);
      post.append(".getValueIndex() == -1)\n");

      post.append(indent);
      post.append("    {\n");

      post.append(indent);
      post.append("      System.err.println(\"Classifier ");
      post.append(lceName);
      post.append(" defined on line ");
      post.append(line);
      post.append(" of ");
      post.append(Main.sourceFilename);
      post.append(" tried to produce a feature with value '\" + ");
      post.append(variable);
      post.append(".getStringValue() + \"' which is not allowable.\");\n");

      post.append(indent);
      post.append("      System.exit(1);\n");

      post.append(indent);
      post.append("    }\n");

      if (!primitive) post.append("    }\n");
    }

    generateClassificationMethods(
        out, lce, preamble.toString(), body.toString(), false,
        lce.evaluation != null, post == null ? null : post.toString());

    out.println("\n  public FeatureVector[] classify(Object[] examples)");
    out.println("  {");
    out.println("    if (isClone)");
    out.println("    {");
    out.print(
        generateTypeChecking("      ", lceName, "Classifier", true,
                             inputString, lce.line, "examples", true));
    out.println("      loadInstance();");
    out.println("      return instance.classify(examples);");
    out.println("    }\n");

    out.println("    FeatureVector[] result = super.classify(examples);");

    if (lce.checkDiscreteValues) {
      out.println("    for (int i = 0; i < result.length; ++i)");
      out.println("      for (int j = 0; j < result[i].featuresSize(); ++j)");
      out.println("      {");
      out.println("        Feature f = result[i].getFeature(j);");
      out.println("        if (f.getValueIndex() == -1)");
      out.println("        {");
      out.println("          System.err.println(\"Classifier " + lceName
                  + " defined on line " + line + " of " + Main.sourceFilename
                  + " tried to produce a feature with value '\" + "
                  + "f.getStringValue() + \"' which is not allowable.\");");
      out.println("          System.exit(1);");
      out.println("        }");
      out.println("      }\n");
    }

    out.println("    return result;");
    out.println("  }\n");

    String pack = AST.globalSymbolTable.getPackage();
    String fullName = pack + (pack.length() == 0 ? "" : ".") + lceName;
    out.println("  public static void main(String[] args)");
    out.println("  {");
    out.println("    String testParserName = null;");
    out.println("    String testFile = null;");
    out.println("    Parser testParser = getTestParser();\n");

    out.println("    try");
    out.println("    {");
    out.println("      if (!args[0].equals(\"null\"))");
    out.println("        testParserName = args[0];");
    out.println("      if (args.length > 1) testFile = args[1];\n");

    out.println("      if (testParserName == null && testParser == null)");
    out.println("      {");
    out.println("        System.err.println(\"The \\\"testFrom\\\" clause "
                + "was not used in the learning classifier expression "
                + "that\");");
    out.println("        System.err.println(\"generated this classifier, so "
                + "a parser and input file must be specified.\\n\");");
    out.println("        throw new Exception();");
    out.println("      }");
    out.println("    }");
    out.println("    catch (Exception e)");
    out.println("    {");
    out.println("      System.err.println(\"usage: " + fullName
                + " \\\\\");");
    out.println("      System.err.println(\"           <parser> <input "
                + "file> [<null label> [<null label> ...]]\\n\");");
    out.println("      System.err.println(\"     * <parser> must be the "
                + "fully qualified class name of a Parser, or "
                + "\\\"null\\\"\");");
    out.println("      System.err.println(\"       to use the default as "
                + "specified by the \\\"testFrom\\\" clause.\");");
    out.println("      System.err.println(\"     * <input file> is the "
                + "relative or absolute path of a file, or \\\"null\\\" "
                + "to\");");
    out.println("      System.err.println(\"       use the parser arguments "
                + "specified by the \\\"testFrom\\\" clause.  <input\");");
    out.println("      System.err.println(\"       file> can also be "
                + "non-\\\"null\\\" when <parser> is \\\"null\\\" (when the "
                + "parser\");");
    out.println("      System.err.println(\"       specified by the "
                + "\\\"testFrom\\\" clause has a single string argument\");");
    out.println("      System.err.println(\"       constructor) to use an "
                + "alternate file.\");");
    out.println("      System.err.println(\"     * A <null label> is a label "
                + "(or prediction) that should not count towards\");");
    out.println("      System.err.println(\"       overall precision and "
                + "recall assessments.\");");
    out.println("      System.exit(1);");
    out.println("    }\n");

    out.println("    if (testParserName == null && testFile != null && "
                + "!testFile.equals(\"null\"))");
    out.println("      testParserName = testParser.getClass().getName();");
    out.println("    if (testParserName != null)");
    out.println("      testParser = "
                + "LBJ2.util.ClassUtils.getParser(testParserName, new "
                + "Class[]{ String.class }, new String[]{ testFile });");
    out.println("    " + lceName + " classifier = new " + lceName + "();");
    out.println("    TestDiscrete tester = new TestDiscrete();");
    out.println("    for (int i = 2; i < args.length; ++i)");
    out.println("      tester.addNull(args[i]);");
    out.println("    TestDiscrete.testDiscrete(tester, classifier, "
                + "classifier.getLabeler(), testParser, true, 0);");

    out.println("  }\n");

    generateHashingMethods(out, lceName);

    Class lceClass =
      AST.globalSymbolTable.classForName(lce.learnerName.toString());
    if (lceClass == null) {
      reportError(lce.line, "Could not locate class for learner '"
                            + lce.learnerName + "'.");
      return;
    }

    Method[] methods = lceClass.getMethods();
    for (int i = 0; i < methods.length; ++i) {
      int modifiers = methods[i].getModifiers();
      if (Modifier.isFinal(modifiers) || Modifier.isPrivate(modifiers)
          || Modifier.isProtected(modifiers) || Modifier.isStatic(modifiers))
        continue;

      Class returned = methods[i].getReturnType();
      String name = methods[i].getName();
      Class[] parameters = methods[i].getParameterTypes();

      String sig =
        signature(methods[i], modifiers, returned, name, parameters);
      if (Arrays.binarySearch(noOverride, sig) >= 0) continue;

      out.println("\n  " + sig);
      out.println("  {");
      out.println("    if (isClone)");
      out.println("    {");
      out.println("      loadInstance();");

      out.print("      ");
      if (!returned.equals(void.class)) out.print("return ");
      out.print("instance." + name + "(");

      if (parameters.length > 0) {
        out.print("a0");
        for (int j = 1; j < parameters.length; ++j) out.print(", a" + j);
      }

      out.println(");");

      if (returned.equals(void.class)) out.println("      return;");
      out.println("    }\n");

      out.print("    ");
      if (!returned.equals(void.class)) out.print("return ");
      out.print("super." + name + "(");

      if (parameters.length > 0) {
        out.print("a0");
        for (int j = 1; j < parameters.length; ++j) out.print(", a" + j);
      }

      out.println(");");
      out.println("  }");
    }

    if (lce.parameterSets.size() == 0) {
      out.println();
      out.println("  public static class Parameters extends "
                  + lce.learnerName + ".Parameters");
      out.println("  {");

      if (lce.learnerParameterBlock != null) {
        TranslateToJava translator = new TranslateToJava(null);
        translator.setRoot(lce.learnerParameterBlock);
        translator.setCurrentCG(lce);
        translator.setIndent(3);
        translator.run();
        out.println("    public Parameters()");
        out.println(translator.getMethodBody());
      }
      else
        out.println("    public Parameters() { super((" + lce.learnerName
                    + ".Parameters) new " + lceName
                    + "(false).getParameters()); }");
      out.println("  }");
    }
  }


  /**
    * This method generates a string signature of the given method.  The
    * arguments other than <code>m</code> are supplied as arguments for
    * efficiency reasons, since this method is only called by one other
    * method.
    *
    * @see #generateLearnerBody(PrintStream,LearningClassifierExpression)
    * @param m          The method object.
    * @param modifiers  The integer representation of the method's modifiers.
    * @param returned   The return type of the method.
    * @param name       The name of the method.
    * @param parameters The parameter types of the method.
    * @return A string description of the method suitable for comparison with
    *         the elements of the {@link #noOverride} array.
   **/
  public static String signature(Method m, int modifiers, Class returned,
                                 String name, Class[] parameters) {
    Class[] thrown = m.getExceptionTypes();

    String result = "";
    if (Modifier.isAbstract(modifiers)) result += "abstract ";
    if (Modifier.isFinal(modifiers)) result += "final ";
    if (Modifier.isNative(modifiers)) result += "native ";
    if (Modifier.isPrivate(modifiers)) result += "private ";
    if (Modifier.isProtected(modifiers)) result += "protected ";
    if (Modifier.isPublic(modifiers)) result += "public ";
    if (Modifier.isStatic(modifiers)) result += "static ";
    if (Modifier.isStrict(modifiers)) result += "strictfp ";
    if (Modifier.isSynchronized(modifiers)) result += "synchronized ";

    result += makeTypeReadable(returned.getName()) + " " + name + "(";
    if (parameters.length > 0) {
      result += makeTypeReadable(parameters[0].getName()) + " a0";
      for (int j = 1; j < parameters.length; ++j)
        result += ", " + makeTypeReadable(parameters[j].getName()) + " a" + j;
    }

    result += ")";
    if (thrown.length > 0) {
      result += " throws " + thrown[0].getName();
      for (int j = 1; j < thrown.length; ++j)
        result += ", " + thrown[j].getName();
    }

    return result;
  }


  /**
    * The value returned by the <code>Class.getName()</code> method is not
    * recognizable as a type by <code>javac</code> if the given class is an
    * array; this method produces a representation that is recognizable by
    * <code>javac</code>.  This method also replaces '$' characters with '.'
    * characters, under the assumption that '$' only appears in the name of an
    * inner class.
    *
    * @param name The name of a class as produced by
    *             <code>Class.getName()</code>.
    * @return A string representation of the class recognizable by
    *         <code>javac</code>.
   **/
  public static String makeTypeReadable(String name) {
    for (int i = name.indexOf('$'); i != -1; i = name.indexOf('$', i + 1))
      name = name.substring(0, i) + '.' + name.substring(i + 1);

    if (name.charAt(0) != '[') return name;

    while (name.charAt(0) == '[') name = name.substring(1) + "[]";

    switch (name.charAt(0)) {
      case 'B': return "boolean" + name.substring(1);
      case 'C': return "char" + name.substring(1);
      case 'D': return "double" + name.substring(1);
      case 'F': return "float" + name.substring(1);
      case 'I': return "int" + name.substring(1);
      case 'J': return "long" + name.substring(1);
      case 'L':
        int colon = name.indexOf(';');
        return name.substring(1, colon) + name.substring(colon + 1);
      case 'S': return "short" + name.substring(1);
      case 'Z': return "boolean" + name.substring(1);
    }

    assert false : "Unrecognized type string: " + name;
    return null;
  }


  /**
    * Generate code that overrides the methods of {@link Classifier} that
    * return type information.  The methods overridden are:
    * <ul>
    *   <li> <code>getInputType()</code> </li>
    *   <li> <code>getOutputType()</code> </li>
    *   <li> <code>allowableValues()</code> </li>
    * </ul>
    *
    * @param out    The stream to write to.
    * @param input  The input type of the classifier whose code this is.
    * @param output The return type of the classifier whose code this is.
   **/
  public static void typeReturningMethods(PrintStream out,
                                          LBJ2.IR.Type input,
                                          ClassifierReturnType output) {
    out.println("  public String getInputType() { return \""
                + input.typeClass().getName() + "\"; }");
    out.println("  public String getOutputType() { return \""
                + output.getTypeName() + "\"; }");

    if (output.values.size() > 0) {
      ConstantList values = output.values;
      out.print("\n  private static String[] __allowableValues = ");
      boolean isBoolean = false;

      if (output.values.size() == 2) {
        ASTNodeIterator I = values.iterator();
        String v1 = I.next().toString();
        String v2 = I.next().toString();
        if ((v1.equals("false") || v1.equals("\"false\""))
            && (v2.equals("true") || v2.equals("\"true\""))) {
          isBoolean = true;
          out.println("DiscreteFeature.BooleanValues;");
        }
      }

      if (!isBoolean) {
        ASTNodeIterator I = values.iterator();
        String v = I.next().toString();
        if (v.charAt(0) != '"') v = "\"" + v + "\"";
        out.print("new String[]{ " + v);
        while (I.hasNext()) {
          v = I.next().toString();
          if (v.charAt(0) != '"') v = "\"" + v + "\"";
          out.print(", " + v);
        }
        out.println(" };");
      }

      out.println("  public static String[] getAllowableValues() { return "
                  + "__allowableValues; }");
      out.println("  public String[] allowableValues() { return "
                  + "__allowableValues; }");
    }
  }


  /**
    * Generates code that overrides the {@link Classifier#classify(Object[])}
    * method so that it checks the types of its arguments.
    *
    * @param out    The stream to write to.
    * @param name   The name of the classifier whose code this is.
    * @param input  The input type of the classifier whose code this is.
    * @param line   The line number on which this classifier is defined.
   **/
  public static void typeCheckClassifyArray(PrintStream out, String name,
                                            LBJ2.IR.Type input, int line) {
    out.println("  public FeatureVector[] classify(Object[] examples)");
    out.println("  {");
    out.print(
        generateTypeChecking("    ", name, "Classifier", true,
                             input.toString(), line, "examples", false));
    out.println("    return super.classify(examples);");
    out.println("  }");
  }


  /**
    * Generates the <code>equals(Object)</code> method, which evaluates to
    * <code>true</code> whenever the two objects are of the same type.  This
    * method should not be called when generating code for a
    * {@link InferenceDeclaration}.
    *
    * @param out  The stream to write to.
    * @param name The name of the node whose <code>equals(Object)</code>
    *             method is being generated.
   **/
  public static void generateHashingMethods(PrintStream out, String name) {
    out.println("  public int hashCode() { return \"" + name
                + "\".hashCode(); }");
    out.println("  public boolean equals(Object o) { return o instanceof "
                + name + "; }");
  }


  /**
    * Generates the code appearing at the beginning of, for example, many
    * classifiers' {@link Classifier#classify(Object)} methods that checks to
    * see if that input <code>Object</code> has the appropriate type.
    *
    * @param indent       The whitespace indentation in the generated code.
    * @param name         The name of the {@link CodeGenerator} whose input is
    *                     being checked.
    * @param type         The type of {@link CodeGenerator} whose input is
    *                     being checked (capitalized).
    * @param array        Whether or not the method being type checked takes
    *                     an array of the input type.
    * @param input        The correct input type of the {@link CodeGenerator}.
    * @param line         The line number on which the {@link CodeGenerator}
    *                     appears.
    * @param exampleName  The name of the example variable.
    * @param preExtracted Whether or not the generated code should allow
    *                     object arrays containing indexed features, as a
    *                     learner can take as input.
    * @return The generated code.
   **/
  public static StringBuffer generateTypeChecking(
      String indent, String name, String type, boolean array, String input,
      int line, String exampleName, boolean preExtracted) {
    StringBuffer result = new StringBuffer();

    result.append(indent);
    result.append("if (!(");
    result.append(exampleName);
    result.append(" instanceof ");
    result.append(input);
    if (array) result.append("[]");

    if (preExtracted) {
      result.append(" || ");
      result.append(exampleName);
      result.append(" instanceof Object[]");
      if (array) result.append("[]");
    }

    result.append("))\n");

    result.append(indent);
    result.append("{\n");

    result.append(indent);
    result.append("  String type = ");
    result.append(exampleName);
    result.append(" == null ? \"null\" : ");
    result.append(exampleName);
    result.append(".getClass().getName();\n");

    result.append(indent);
    result.append("  System.err.println(\"");
    result.append(type);
    result.append(" '");
    result.append(name);
    result.append("(");
    result.append(input);
    result.append(")' defined on line ");
    result.append(line + 1);
    result.append(" of ");
    result.append(Main.sourceFilename);
    result.append(" received '\" + type + \"' as input.\");\n");

    result.append(indent);
    result.append("  new Exception().printStackTrace();\n");

    result.append(indent);
    result.append("  System.exit(1);\n");

    result.append(indent);
    result.append("}\n\n");
    return result;
  }


  /**
    * Generates code that instantiates a primitive feature.
    *
    * @param discrete   Whether or not the feature is discrete.
    * @param array      Whether or not the feature comes from an array.
    * @param ref        Code referring to an instance of the classifier from
    *                   which package and name information will be taken for
    *                   this feature.
    * @param id         Code that evaluates to the value of the feature's
    *                   identifier.
    * @param value      Code that evaluates to the feature's value if
    *                   <code>array</code> is <code>false</code>, or the array
    *                   containing the feature's value if <code>array</code>
    *                   is <code>true</code>.
    * @param index      Code that evaluates to the array index of this feature.
    *                   This parameter is ignored if it is <code>null</code>
    *                   or <code>array</code> is <code>false</code>.
    * @param values     Code that evaluates to the number of possible values
    *                   that the feature can take.  If set to
    *                   <code>null</code>,
    *                   <code>"allowableValues().length"</code> is
    *                   substituted.  This parameter is ignored if
    *                   <code>array</code> is <code>false</code>.
    * @param arrayInfo  Code that evaluates to both the index and length
    *                   arguments in a feature constructor, separated by a
    *                   comma.  If this parameter is <code>null</code>, it
    *                   defaults to <code>index</code> followed by
    *                   <code>value</code> with <code>".length"</code>
    *                   appended.
   **/
  private static String primitiveFeatureConstructorInvocation(
      boolean discrete, boolean array, String ref, String id, String value,
      String index, String values, String arrayInfo) {
    StringBuffer buffer = new StringBuffer("new ");
    if (ref.length() > 0) ref += ".";

    buffer.append(discrete ? "Discrete" : "Real");
    buffer.append(array ? "Array" : "Primitive");
    buffer.append("StringFeature(");
    buffer.append(ref);
    buffer.append("containingPackage, ");
    buffer.append(ref);
    buffer.append("name, ");
    buffer.append(id);
    buffer.append(", ");
    buffer.append(value);

    if (array && index != null) {
      buffer.append('[');
      buffer.append(index);
      buffer.append(']');
    }

    if (discrete) {
      buffer.append(", valueIndexOf(");
      buffer.append(value);
      if (array && index != null) {
        buffer.append('[');
        buffer.append(index);
        buffer.append(']');
      }

      if (values == null) values = "allowableValues().length";
      buffer.append("), (short) ");
      buffer.append(values);
    }

    if (array) {
      buffer.append(", ");
      if (arrayInfo != null) buffer.append(arrayInfo);
      else {
        buffer.append(index);
        buffer.append(", ");
        buffer.append(value);
        buffer.append(".length");
      }
    }

    buffer.append(")");

    return buffer.toString();
  }


  /**
    * This method generates the methods that return the features and values
    * representing a classification.  Implementations generated here take care
    * of all caching we may want to take place.  The explicitly overridden
    * methods are a subset (depending on the classifier's type) of:
    *
    * <ul>
    *   <li> <code>classify(Object)</code> </li>
    *   <li> <code>featureValue(Object)</code> </li>
    *   <li> <code>discreteValue(Object)</code> </li>
    *   <li> <code>discreteValueArray(Object)</code> </li>
    *   <li> <code>realValue(Object)</code> </li>
    *   <li> <code>realValueArray(Object)</code> </li>
    * </ul>
    *
    * <p> If <code>bodyPrimitive</code> is <code>true</code>,
    * <code>body</code> should implement a method that the caller assumes
    * takes the same argument as the generated classifier and returns the
    * appropriate primitive type (<code>String</code> or <code>double</code>)
    * via a <code>return</code> statement.  Otherwise, <code>body</code>
    * should implement a method whose argument is <code>Object
    * __example</code> and which stores the result of its computation in a
    * variable named <code>__result</code> which has already been declared to
    * have either type {@link LBJ2.classify.Feature} or
    * {@link LBJ2.classify.FeatureVector} as appropriate.
    *
    * <p> If <code>post</code> is non-<code>null</code>, the code therein will
    * have access to the values computed by <code>body</code>.  If
    * <code>bodyPrimitive</code> is <code>true</code>, those values can be
    * accessed via a primitive variable named <code>__cachedValue</code>.
    * Otherwise, they must be accessed via the aforementioned
    * <code>__result</code> variable.
    *
    * @param out            The stream to write to.
    * @param classifierExp  The classifier for which code is being generated.
    * @param preamble       This code will be executed before any call to any
    *                       of the generated methods.  Any occurrences of the
    *                       string <code>"$METHOD$"</code> (without the
    *                       quotes) inside <code>preamble</code> will be
    *                       replaced with the name of the method inside which
    *                       code is currently being placed.  If left
    *                       <code>null</code>, it defaults to the code
    *                       generated by
    *                       {@link #generateTypeChecking(String,String,String,boolean,String,int,String,boolean)}.
    * @param body           Generated code that computes the classification
    *                       result (in a <code>FeatureVector</code>,
    *                       <code>Feature</code>, <code>String</code>, or
    *                       <code>double</code> as appropriate).
    * @param bodyPrimitive  Set to <code>true</code> if the code in
    *                       <code>body</code> computes a single
    *                       <code>String</code> or <code>double</code>.
    * @param bodyArgCast    Set to <code>true</code> if the code in
    *                       <code>body</code> assumes the classifier's
    *                       argument in the original source code will be in
    *                       scope.
    * @param post           Generated code that performs any post-processing
    *                       that may be necessary on the computed
    *                       classification result before it is finally
    *                       returned.  This parameter can be <code>null</code>
    *                       if post-processing is not necessary.
   **/
  private static void generateClassificationMethods(
      PrintStream out, ClassifierExpression classifierExp, String preamble,
      String body, boolean bodyPrimitive, boolean bodyArgCast, String post) {
    String name = classifierExp.name.toString();
    Argument arg = classifierExp.argument;
    String input = arg.getType().toString();
    String field =
      classifierExp.cacheIn == null ? null : classifierExp.cacheIn.toString();
    boolean cachedInMap = ClassifierAssignment.mapCache.equals(field);
    boolean anyCache = classifierExp.singleExampleCache || field != null;
    boolean discrete, array, generator;

  {
    ClassifierReturnType crt = classifierExp.returnType;
    discrete =
      crt.type == ClassifierReturnType.DISCRETE
      || crt.type == ClassifierReturnType.DISCRETE_ARRAY
      || crt.type == ClassifierReturnType.DISCRETE_GENERATOR;
    array =
      crt.type == ClassifierReturnType.DISCRETE_ARRAY
      || crt.type == ClassifierReturnType.REAL_ARRAY;
    generator =
      crt.type == ClassifierReturnType.DISCRETE_GENERATOR
      || crt.type == ClassifierReturnType.REAL_GENERATOR
      || crt.type == ClassifierReturnType.MIXED_GENERATOR;
  }

    String primitiveFeatureType = discrete ? "discrete" : "real";
    String primitiveType = discrete ? "String" : "double";
    String cachedValueType = primitiveType;
    if (array) cachedValueType += "[]";
    String valueMethodName = primitiveFeatureType + "Value";
    if (array) valueMethodName += "Array";
    String cachedMethodReturnType = "Feature";
    if (array || generator) cachedMethodReturnType += "Vector";

    if (preamble == null)
      preamble = 
        generateTypeChecking(
          "    ", name, "Classifier", false, input, classifierExp.line,
          "__example", false)
        .toString();

    if (anyCache)
      out.println("  private " + cachedMethodReturnType
                  + " cachedFeatureValue(Object __example)");
    else if (array || generator)
      out.println("  public FeatureVector classify(Object __example)");
    else if (!bodyPrimitive)
      out.println("  public Feature featureValue(Object __example)");

    if (anyCache || !bodyPrimitive) {
      out.println("  {");
      if (classifierExp.singleExampleCache) {
        out.println("    if (__example == __exampleCache.get()) return ("
                    + cachedMethodReturnType + ") __cache.get();");
        out.println("    __exampleCache.set(__example);");
      }

      if (field != null) {
        if (cachedInMap) {
          if (!array && !discrete)
            out.println("    Double __dValue = "
                        + "(Double) __valueCache.get(__example);");
        }
        else out.println("    " + arg + " = (" + input + ") __example;");

        out.print("    " + cachedValueType + " __cachedValue = ");

        if (cachedInMap) {
          if (!array && !discrete)
            out.println("__dValue == null ? Double.NaN : "
                        + "__dValue.doubleValue();");
          else
            out.println("(" + cachedValueType
                        + ") __valueCache.get(__example);");
        }
        else out.println(field + ";");

        out.print("\n    if (");
        if (!array && !discrete) out.print("Double.doubleToLongBits(");
        out.print("__cachedValue");
        if (!array && !discrete) out.print(")");
        out.print(" != ");
        if (!array && !discrete)
          out.print("Double.doubleToLongBits(Double.NaN)");
        else out.print("null");
        out.println(")");
        out.println("    {");
        out.print("      " + cachedMethodReturnType + " result = ");

        if (array) {
          out.println("new FeatureVector();");
          out.println("      for (int i = 0; i < __cachedValue.length; ++i)");
          out.print("        result.addFeature(");
        }

        out.print(
            primitiveFeatureConstructorInvocation(
                discrete, array, "", "\"\"", "__cachedValue", "i", null,
                null));

        if (array) out.print(")");
        out.println(";");
        if (classifierExp.singleExampleCache)
          out.println("      __cache.set(result);");
        out.println("      return result;");
        out.println("    }\n");
      }

      if (bodyPrimitive) {
        if (field != null) {
          out.print("    __cachedValue = ");
          if (!cachedInMap) out.print(field + " = ");
          out.println("_" + valueMethodName + "(__example);");

          if (cachedInMap) {
            out.print("    __valueCache.put(__example, ");
            if (!discrete) out.print("new Double(");
            out.print("__cachedValue");
            if (!discrete) out.print(")");
            out.println(");");
          }
        }
        else
          out.println("    " + primitiveType + " __cachedValue = _"
                      + valueMethodName + "(__example);");

        if (post != null) {
          out.println();
          out.println(post);
        }

        out.println("    Feature __result = "
                    + primitiveFeatureConstructorInvocation(
                        discrete, false, "", "\"\"", "__cachedValue", null,
                        null, null)
                    + ";");
      }
      else {
        if (!anyCache)
          out.print(
              preamble.replaceAll("\\$METHOD\\$",
                                  (array || generator ? "classify"
                                                      : "featureValue")));
        if (bodyArgCast && (field == null || cachedInMap))
          out.println("    " + arg + " = (" + input + ") __example;\n");
        out.println("    " + cachedMethodReturnType + " __result;");
        out.print(body);

        if (field != null) {
          out.print("    __cachedValue = ");
          if (!cachedInMap) out.print(field + " = ");
          out.println("__result."
                      + (array ? valueMethodName
                               : (discrete ? "getStringValue"
                                           : "getStrength"))
                      + "();");

          if (cachedInMap) {
            out.print("    __valueCache.put(__example, ");
            if (!discrete && !array) out.print("new Double(");
            out.print("__cachedValue");
            if (!discrete && !array) out.print(")");
            out.println(");");
          }
        }

        if (post != null) {
          out.println();
          out.println(post);
        }
      }

      if (classifierExp.singleExampleCache)
        out.println("    __cache.set(__result);");
      out.println("    return __result;");
      out.println("  }");
    }

    if (anyCache || !(array || generator)) {
      out.println("\n  public FeatureVector classify(Object __example)");
      out.println("  {");
      if (anyCache)
        out.print(preamble.replaceAll("\\$METHOD\\$", "classify"));
      out.print("    return ");
      if (!(array || generator)) out.print("new FeatureVector(");
      out.print((anyCache ? "cachedFeatureValue" : "featureValue")
                + "(__example)");
      if (!(array || generator)) out.print(")");
      out.println(";");
      out.println("  }");
    }

    if (!generator) {
      if (!array && (anyCache || bodyPrimitive)) {
        out.println("\n  public Feature featureValue(Object __example)");
        out.println("  {");
        if (anyCache)
          out.print(preamble.replaceAll("\\$METHOD\\$", "featureValue"));

        if (anyCache)
          out.println("    return cachedFeatureValue(__example);");
        else {
          out.println("    " + cachedValueType + " result = "
                      + valueMethodName + "(__example);");
          out.println(
              "    return "
              + primitiveFeatureConstructorInvocation(
                  discrete, false, "", "\"\"", "result", null, null, null)
              + ";");
        }

        out.println("  }");
      }

      if (anyCache || array || !bodyPrimitive || post != null) {
        out.println("\n  public " + cachedValueType + " " + valueMethodName
                    + "(Object __example)");
        out.println("  {");
        if (anyCache)
          out.print(preamble.replaceAll("\\$METHOD\\$", valueMethodName));

        if (array && field != null) {
          out.println("    cachedFeatureValue(__example);");
          if (!cachedInMap)
            out.println("    " + arg + " = (" + input + ") __example;");
          out.println("    return "
                      + (cachedInMap ? "(" + cachedValueType
                                       + ") __valueCache.get(__example)"
                                     : field)
                      + ";");
        }
        else if (!anyCache && bodyPrimitive && post != null) {
          out.print(preamble.replaceAll("\\$METHOD\\$", valueMethodName));
          out.println("    " + cachedValueType + " __cachedValue = _"
                      + valueMethodName + "(__example);\n");
          out.println(post);
          out.println("    return __cachedValue;");
        }
        else
          out.println("    return "
                      + (anyCache ? "cachedFeatureValue"
                                  : (array ? "classify" : "featureValue"))
                      + "(__example)."
                      + (array ? valueMethodName
                               : (discrete ? "getStringValue"
                                           : "getStrength"))
                      + "();");
        out.println("  }");
      }

      if (bodyPrimitive) {
        boolean helper = anyCache || post != null;
        out.println("\n  " + (helper ? "private" : "public") + " "
                    + cachedValueType + " " + (helper ? "_" : "")
                    + valueMethodName + "(Object __example)");
        out.println("  {");
        if (!helper)
          out.print(preamble.replaceAll("\\$METHOD\\$", valueMethodName));
        if (bodyArgCast)
          out.println("    " + arg + " = (" + input + ") __example;\n");
        out.print(body);
        out.println("  }");
      }
    }
  }


  /**
    * Compress the textual representation of an {@link ASTNode}, convert to
    * ASCII hexadecimal, and write the result to the specified stream.
    *
    * @param buffer The text representation to be written.
    * @param out    The stream to write to.
   **/
  public static void compressAndPrint(StringBuffer buffer, PrintStream out) {
    PrintStream converter = null;
    ByteArrayOutputStream converted = new ByteArrayOutputStream();
    try {
      converter =
        new PrintStream(new GZIPOutputStream(new HexOutputStream(converted)));
    }
    catch (Exception e) {
      System.err.println("Could not create converter stream.");
      System.exit(1);
    }

    converter.print(buffer.toString());
    converter.close();

    try { converted.writeTo(out); }
    catch (Exception e) {
      System.err.println("Could not write the converted stream.");
      System.exit(1);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ast  The node to process.
   **/
  public void run(AST ast) {
    if (!RevisionAnalysis.noChanges) {
      quantificationVariables = new HashMap();
      contextVariables = new HashMap();
      runOnChildren(ast);
    }
  }


  /**
    * Code is only generated for a {@link ClassifierName} when it is the only
    * {@link ClassifierExpression} on the right hand side of the arrow (and
    * there really shouldn't be a reason that a programmer would want to write
    * such a declaration, but if he does, it will work).
    *
    * @param cn The node to process.
   **/
  public void run(ClassifierName cn) {
    String cnName = cn.name.toString();
    if (cn.name == cn.referent
        || !RevisionAnalysis.revisionStatus.get(cnName)
            .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + cnName);

    PrintStream out = open(cn);
    if (out == null) return;

    out.println(disclaimer);
    out.println("// " + cn.shallow() + "\n");

    ast.symbolTable.generateHeader(out);

    String field = null;
    boolean cachedInMap = false;
    if (cn.cacheIn != null) {
      field = cn.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (cn.comment != null) out.println(cn.comment);

    out.println("public class " + cnName + " extends Classifier");
    out.println("{");

    if (cachedInMap)
      out.println("  private static final WeakHashMap __valueCache "
                  + "= new WeakHashMap();");

    String referentNoDots = cn.referent.toString().replace('.', '$');
    out.println("  private static final " + cn.referent + " __"
                + referentNoDots + " = new " + cn.referent + "();\n");

    if (cn.singleExampleCache) {
      out.println(
          "  private static ThreadLocal __cache = new ThreadLocal(){ };");
      out.println("  private static ThreadLocal __exampleCache = "
                  + "new ThreadLocal(){ };");
      out.println("  public static void clearCache() { __exampleCache = new "
                  + "ThreadLocal(){ }; }\n");
    }

    out.println("  public " + cnName + "()");
    out.println("  {");
    out.println("    containingPackage = \""
                + AST.globalSymbolTable.getPackage() + "\";");
    out.println("    name = \"" + cnName + "\";");
    out.println("  }\n");

    typeReturningMethods(out, cn.argument.getType(), cn.returnType);
    out.println();

    boolean array =
      cn.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
      || cn.returnType.type == ClassifierReturnType.REAL_ARRAY;
    boolean generator =
      cn.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
      || cn.returnType.type == ClassifierReturnType.REAL_GENERATOR
      || cn.returnType.type == ClassifierReturnType.MIXED_GENERATOR;

    StringBuffer body = new StringBuffer();
    body.append("    __result = __");
    body.append(referentNoDots);
    body.append(".");
    body.append(array || generator ? "classify" : "featureValue");
    body.append("(__example);\n");

    generateClassificationMethods(out, cn, null, body.toString(), false,
                                  false, null);
    out.println();
    typeCheckClassifyArray(out, cnName, cn.argument.getType(), cn.line);
    out.println();
    generateHashingMethods(out, cnName);

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc) {
    String ccName = cc.name.toString();
    String fileName = ccName + ".java";
    if (fileName.indexOf("$$") != -1) files.add(fileName);

    if (!RevisionAnalysis.revisionStatus.get(ccName)
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + ccName);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.print("// ");
    compressAndPrint(cc.shallow(), out);
    out.println("\n");

    ast.symbolTable.generateHeader(out);

    String field = null;
    boolean cachedInMap = false;
    if (cc.cacheIn != null) {
      field = cc.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (cc.comment != null) out.println(cc.comment);

    out.println("public class " + ccName + " extends Classifier");
    out.println("{");

    if (cachedInMap)
      out.println("  private static final WeakHashMap __valueCache "
                  + "= new WeakHashMap();");

    HashSet invoked =
      (HashSet) SemanticAnalysis.invokedGraph.get(ccName);
    if (invoked != null && invoked.size() > 0) {
      for (Iterator I = invoked.iterator(); I.hasNext(); ) {
        String name = (String) I.next();
        String nameNoDots = name.replace('.', '$');
        out.println("  private static final " + name + " __" + nameNoDots
                    + " = new " + name + "();");
      }

      out.println();
    }

    if (cc.singleExampleCache) {
      out.println(
          "  private static ThreadLocal __cache = new ThreadLocal(){ };");
      out.println("  private static ThreadLocal __exampleCache = "
                  + "new ThreadLocal(){ };");
      out.println("  public static void clearCache() { __exampleCache = new "
                  + "ThreadLocal(){ }; }\n");
    }

    out.println("  public " + ccName + "()");
    out.println("  {");
    out.println("    containingPackage = \""
                + AST.globalSymbolTable.getPackage() + "\";");
    out.println("    name = \"" + ccName + "\";");
    out.println("  }\n");

    LBJ2.IR.Type input = cc.argument.getType();
    typeReturningMethods(out, input, cc.returnType);
    out.println();

    indent = 2;
    forInit = false;
    constraintMode = false;
    methodBody.delete(0, methodBody.length());
    currentCG = cc;
    for (ASTNodeIterator I = cc.body.iterator(); I.hasNext(); ) {
      I.next().runPass(this);
      methodBody.append("\n");
    }

    StringBuffer body = new StringBuffer();
    StringBuffer post = null;
    boolean primitive = 
      cc.returnType.type == ClassifierReturnType.DISCRETE
      || cc.returnType.type == ClassifierReturnType.REAL;

    if (primitive) {
      boolean discrete = cc.returnType.type == ClassifierReturnType.DISCRETE;

      body = methodBody;
      if (discrete && cc.returnType.values.size() > 0) {
        post = new StringBuffer();
        post.append("    if (valueIndexOf(__cachedValue) == -1)\n"
                  + "    {\n"
                  + "      System.err.println(\"Classifier '");
        post.append(ccName);
        post.append("' defined on line ");
        post.append(cc.line + 1);
        post.append(" of ");
        post.append(Main.sourceFilename);
        post.append(" produced '\" + __cachedValue  + \"' as a feature "
                  + "value, which is not allowable.\");\n"
                  + "      System.exit(1);\n"
                  + "    }\n");
      }
    }
    else {
      body.append("    __result = new FeatureVector();\n");
      boolean array =
        cc.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
        || cc.returnType.type == ClassifierReturnType.REAL_ARRAY;
      if (array)
        body.append("    int __featureIndex = 0;\n");
      if (cc.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
          || cc.returnType.type == ClassifierReturnType.REAL_GENERATOR)
        body.append("    String __id;\n");
      if (cc.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
          || cc.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
          || cc.returnType.type == ClassifierReturnType.REAL_ARRAY
          || cc.returnType.type == ClassifierReturnType.REAL_GENERATOR) {
        body.append("    ");
        body.append(
            cc.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
            || cc.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
            ? "String" : "double");
        body.append(" __value;\n");
      }

      body.append("\n");
      body.append(methodBody);

      if (array) {
        post = new StringBuffer();
        post.append(
              "    for (int __i = 0; __i < __result.featuresSize(); ++__i)\n"
            + "      __result.getFeature(__i)"
            + ".setArrayLength(__featureIndex);\n");
      }
    }

    generateClassificationMethods(
        out, cc, null, body.toString(), primitive, true,
        post == null ? null : post.toString());
    out.println();
    typeCheckClassifyArray(out, ccName, input, cc.line);
    out.println();
    generateHashingMethods(out, ccName);

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param cg The node to process.
   **/
  public void run(CompositeGenerator cg) {
    String cgName = cg.name.toString();
    String fileName = cgName + ".java";
    if (fileName.indexOf("$$") != -1) {
      files.add(fileName);
      runOnChildren(cg);
    }
    else {
      files.clear();

      runOnChildren(cg);

      final String prefix = cgName + "$$";
      File[] leftOvers =
        new File(System.getProperty("user.dir")).listFiles(
          new FilenameFilter() {
            public boolean accept(File directory, String name) {
              int i = name.lastIndexOf('.');
              if (i == -1) return false;
              String javaFile = name.substring(0, i) + ".java";
              return name.startsWith(prefix) && !files.contains(javaFile);
            }
          });

      for (int i = 0; i < leftOvers.length; ++i)
        if (leftOvers[i].exists() && !leftOvers[i].delete())
          reportError(0, "Could not delete '" + leftOvers[i].getName()
                         + "'.");
    }

    if (!RevisionAnalysis.revisionStatus.get(cgName)
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + cgName);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.print("// ");
    compressAndPrint(cg.shallow(), out);
    out.println("\n");

    ast.symbolTable.generateHeader(out);

    out.println("\n");
    if (cg.comment != null) out.println(cg.comment);

    out.println("public class " + cgName + " extends Classifier");
    out.println("{");

    {
      HashSet declared = new HashSet();
      for (ClassifierExpressionList.ClassifierExpressionListIterator I =
             cg.components.listIterator();
           I.hasNext(); ) {
        String name = I.nextItem().name.toString();
        if (declared.add(name)) {
          String nameNoDots = name.replace('.', '$');
          out.println("  private static final " + name + " __" + nameNoDots
                      + " = new " + name + "();");
        }
      }
    }

    if (cg.singleExampleCache) {
      out.println(
          "\n  private static ThreadLocal __cache = new ThreadLocal(){ };");
      out.println("  private static ThreadLocal __exampleCache = "
                  + "new ThreadLocal(){ };");
      out.println("  public static void clearCache() { __exampleCache = new "
                  + "ThreadLocal() { }; }");
    }

    out.println("\n  public " + cgName + "()");
    out.println("  {");
    out.println("    containingPackage = \""
                + AST.globalSymbolTable.getPackage() + "\";");
    out.println("    name = \"" + cgName + "\";");
    out.println("  }\n");

    LBJ2.IR.Type input = cg.argument.getType();
    typeReturningMethods(out, input, cg.returnType);
    out.println();

    StringBuffer body = new StringBuffer();
    body.append("    __result = new FeatureVector();\n");

    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); ) {
      ClassifierExpression component = I.nextItem();
      String nameNoDots = ("__" + component.name).replace('.', '$');
      if (component.returnType.type == ClassifierReturnType.DISCRETE
          || component.returnType.type == ClassifierReturnType.REAL) {
        body.append("    __result.addFeature(");
        body.append(nameNoDots);
        body.append(".featureValue(__example));\n");
      }
      else {
        body.append("    __result.addFeatures(");
        body.append(nameNoDots);
        body.append(".classify(__example));\n");
      }
    }

    generateClassificationMethods(out, cg, null, body.toString(), false,
                                  false, null);
    out.println();
    typeCheckClassifyArray(out, cgName, input, cg.line);
    out.println();
    generateHashingMethods(out, cgName);

    out.println("\n  public java.util.LinkedList getCompositeChildren()");
    out.println("  {");
    out.println("    java.util.LinkedList result = new "
                + "java.util.LinkedList();");

    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); ) {
      String nameNoDots = ("__" + I.nextItem().name).replace('.', '$');
      out.println("    result.add(" + nameNoDots + ");");
    }

    out.println("    return result;");
    out.println("  }");

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param ii The node to process.
   **/
  public void run(InferenceInvocation ii) {
    String iiName = ii.name.toString();
    if (!RevisionAnalysis.revisionStatus.get(iiName)
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + iiName);

    PrintStream out = open(ii);
    if (out == null) return;

    out.println(disclaimer);
    out.println("// " + ii.shallow() + "\n");

    ast.symbolTable.generateHeader(out);

    String field = null;
    boolean cachedInMap = false;
    if (ii.cacheIn != null) {
      field = ii.cacheIn.toString();
      cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (ii.comment != null) out.println(ii.comment);

    out.println("public class " + iiName + " extends Classifier");
    out.println("{");

    if (cachedInMap)
      out.println("  private static final WeakHashMap __valueCache "
                  + "= new WeakHashMap();");

    String iiClassifierName = ii.classifier.toString();
    out.println("  private static final " + iiClassifierName + " __"
                + iiClassifierName + " = new " + iiClassifierName + "();\n");

    if (ii.singleExampleCache) {
      out.println(
          "  private static ThreadLocal __cache = new ThreadLocal(){ };");
      out.println("  private static ThreadLocal __exampleCache = "
                  + "new ThreadLocal(){ };");
      out.println("  public static void clearCache() { __exampleCache = new "
                  + "ThreadLocal(){ }; }\n");
    }

    out.println("  public " + iiName + "()");
    out.println("  {");
    out.println("    containingPackage = \""
                + AST.globalSymbolTable.getPackage() + "\";");
    out.println("    name = \"" + iiName + "\";");
    out.println("  }\n");

    ClassifierType iiType = (ClassifierType) ii.classifier.typeCache;
    LBJ2.IR.Type input = iiType.getInput();
    typeReturningMethods(out, input, iiType.getOutput());
    out.println();

    InferenceType inferenceType = (InferenceType) ii.inference.typeCache;

    String iiInference = ii.inference.toString();
    String fqInferenceName = iiInference;
    if (ast.symbolTable.containsKey(ii.inference)
        && ast.symbolTable.getPackage().length() != 0)
      fqInferenceName = ast.symbolTable.getPackage() + "." + fqInferenceName;

    StringBuffer body = new StringBuffer("    ");
    body.append(inferenceType.getHeadType().toString());
    body.append(" head = ");
    body.append(iiInference);
    body.append(".findHead((");
    body.append(input.toString());
    body.append(") __example);\n");

    body.append("    ");
    body.append(iiInference);
    body.append(" inference = (");
    body.append(iiInference);
    body.append(") InferenceManager.get(\"");
    body.append(fqInferenceName);
    body.append("\", head);\n\n");

    body.append(
          "    if (inference == null)\n"
        + "    {\n"
        + "      inference = new " + ii.inference + "(head);\n"
        + "      InferenceManager.put(inference);\n"
        + "    }\n\n"

        + "    String result = null;\n\n"

        + "    try { result = inference.valueOf(__");
    body.append(iiClassifierName);
    body.append(", __example); }\n"
        + "    catch (Exception e)\n"
        + "    {\n"
        + "      System.err.println(\"LBJ ERROR: Fatal error while "
        + "evaluating classifier ");
    body.append(iiName);
    body.append(": \" + e);\n"
                + "      e.printStackTrace();\n"
                + "      System.exit(1);\n"
                + "    }\n\n"

                + "    return result;\n");

    generateClassificationMethods(out, ii, null, body.toString(), true, false,
                                  null);
    out.println();
    typeCheckClassifyArray(out, iiName, input, ii.line);
    out.println();
    generateHashingMethods(out, iiName);

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce) {
    String lceName = lce.name.toString();
    String fileName = lceName + ".java";

    if (fileName.indexOf("$$") != -1) {
      files.add(fileName);
      runOnChildren(lce);
    }
    else {
      files.clear();

      runOnChildren(lce);

      final String prefix = lceName + "$$";
      File[] leftOvers =
        new File(System.getProperty("user.dir")).listFiles(
          new FilenameFilter() {
            public boolean accept(File directory, String name) {
              int i = name.lastIndexOf('.');
              if (i == -1) return false;
              String javaFile = name.substring(0, i) + ".java";
              return name.startsWith(prefix) && !files.contains(javaFile);
            }
          });

      for (int i = 0; i < leftOvers.length; ++i)
        if (leftOvers[i].exists() && !leftOvers[i].delete())
          reportError(0, "Could not delete '" + leftOvers[i].getName()
                         + "'.");
    }

    if ((lce.parser == null
         ? !RevisionAnalysis.revisionStatus.get(lceName)
            .equals(RevisionAnalysis.REVISED)
         : RevisionAnalysis.revisionStatus.get(lceName)
             .equals(RevisionAnalysis.UNAFFECTED)
           || lce.startingRound > 1)
        || lce.onlyCodeGeneration)
      // In the last condition above involving lce.startingRound, note that
      // before setting lce.startingRound > 1, RevisionAnalysis also ensures
      // that the lce is unaffected other than the number of rounds and that
      // there will be no parameter tuning or cross validation.
      return;

    System.out.println("Generating code for " + lceName);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.println("// \n");

    ast.symbolTable.generateHeader(out);

    if (lce.cacheIn != null) {
      String field = lce.cacheIn.toString();
      boolean cachedInMap = field.equals(ClassifierAssignment.mapCache);
      if (cachedInMap) out.println("import java.util.WeakHashMap;");
    }

    out.println("\n");
    if (lce.comment != null) out.println(lce.comment);

    out.println("public class " + lceName + " extends " + lce.learnerName);
    out.println("{");
    out.println("  private static void loadInstance()");
    out.println("  {");
    out.println("    if (instance == null) instance = new " + lceName
                + "(true);");
    out.println("  }\n");

    String formalParameterString = "";
    String firstArgumentsString = "";
    String argumentString = "";

    if (lce.parameterSets.size() > 0) {
      for (ListIterator I = lce.parameterSets.listIterator(); I.hasNext(); ) {
        ParameterSet e = (ParameterSet) I.next();

        StringBuffer typeStringB = new StringBuffer();
        e.type.write(typeStringB);

        formalParameterString += ", " + e.type + " " + e.getParameterName();
        firstArgumentsString += ", " + e.getFirst();
        argumentString += ", " + e.getParameterName();
      }

      formalParameterString = formalParameterString.substring(2);
      firstArgumentsString = firstArgumentsString.substring(2);
      argumentString = argumentString.substring(2);

      out.print("  private " + lceName + "(boolean b) { this(");
      if (lce.learnerParameterBlock == null) out.print(firstArgumentsString);
      else out.print("new Parameters()");
      out.println("); }");

      out.print("  private " + lceName + "(");
      if (lce.learnerParameterBlock == null) out.print(formalParameterString);
      else out.print("Parameters parameters");
      out.println(")");
      out.println("  {");
      out.print("    super(");

      if (lce.learnerParameterBlock == null) {
        if (lce.learnerConstructor.arguments.size() > 0) {
          out.print(argumentString);
          if (lce.attributeString.length() != 0) {
            out.print(", ");
          }
        }

        if (lce.attributeString.length() != 0) out.print("attributeString");
      }
      else out.print("parameters");

      out.println(");");
      out.println("    containingPackage = \""
                  + AST.globalSymbolTable.getPackage() + "\";");
      out.println("    name = \"" + lceName + "\";");
      out.println("    setEncoding(" + lce.featureEncoding + ");");
      if (lce.labeler != null)
        out.println("    setLabeler(new " + lce.labeler.name + "());");
      out.println("    setExtractor(new " + lce.extractor.name + "());");
      out.println("    isClone = false;");
      out.println("  }\n");
    }

    out.println("  public static Parser getParser() { return " + lce.parser
                + "; }");
    out.println("  public static Parser getTestParser() { return "
                + lce.testParser + "; }\n");

    generateLearnerBody(out, lce);

    if (lce.parameterSets.size() > 0) {
      out.println();
      out.println("  public static class Parameters extends "
                  + lce.learnerName + ".Parameters");
      out.println("  {");
      out.println("    public Parameters() { this(" + firstArgumentsString
                  + "); }");
      out.println("    public Parameters(" + formalParameterString + ")");

      if (lce.learnerParameterBlock != null) {
        TranslateToJava translator = new TranslateToJava(null);
        translator.setRoot(lce.learnerParameterBlock);
        translator.setCurrentCG(currentCG);
        translator.setIndent(3);
        translator.run();
        out.println(translator.getMethodBody());
      }
      else {
        out.println("    {");
        out.println("      super((" + lce.learnerName + ".Parameters) new "
                    + lceName + "(" + argumentString + ").getParameters());");
        out.println("    }");
      }
      out.println("  }");
    }

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Conjunction c) {
    String cName = c.name.toString();
    String fileName = cName + ".java";
    if (fileName.indexOf("$$") != -1) {
      files.add(fileName);
      runOnChildren(c);
    }
    else {
      files.clear();

      runOnChildren(c);

      final String prefix = cName + "$$";
      File[] leftOvers =
        new File(System.getProperty("user.dir")).listFiles(
          new FilenameFilter() {
            public boolean accept(File directory, String name) {
              int i = name.lastIndexOf('.');
              if (i == -1) return false;
              String javaFile = name.substring(0, i) + ".java";
              return name.startsWith(prefix) && !files.contains(javaFile);
            }
          });

      for (int i = 0; i < leftOvers.length; ++i)
        if (leftOvers[i].exists() && !leftOvers[i].delete())
          reportError(0, "Could not delete '" + leftOvers[i].getName()
                         + "'.");
    }

    if (!RevisionAnalysis.revisionStatus.get(cName)
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + cName);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.println("// " + c.shallow() + "\n");

    ast.symbolTable.generateHeader(out);

    out.println("\n");
    if (c.comment != null) out.println(c.comment);

    out.println("public class " + cName + " extends Classifier");
    out.println("{");

    String leftName = c.left.name.toString();
    String rightName = c.right.name.toString();
    out.println("  private static final " + leftName + " left = new "
                + leftName + "();");
    if (!leftName.equals(rightName))
      out.println("  private static final " + rightName + " right = new "
                  + c.right.name + "();\n");

    if (c.singleExampleCache) {
      out.println(
          "  private static ThreadLocal __cache = new ThreadLocal(){ };");
      out.println("  private static ThreadLocal __exampleCache = "
                  + "new ThreadLocal(){ };");
      out.println("  public static void clearCache() { __exampleCache = new "
                  + "ThreadLocal() { }; }\n");
    }

    out.println("  public " + cName + "()");
    out.println("  {");
    out.println("    containingPackage = \""
                + AST.globalSymbolTable.getPackage() + "\";");
    out.println("    name = \"" + cName + "\";");
    out.println("  }\n");

    LBJ2.IR.Type input = c.argument.getType();
    typeReturningMethods(out, input, c.returnType);
    out.println();

    boolean primitive =
      c.returnType.type == ClassifierReturnType.DISCRETE
      || c.returnType.type == ClassifierReturnType.REAL;
    boolean mixed = c.returnType.type == ClassifierReturnType.MIXED_GENERATOR;
    int leftType = c.left.returnType.type;
    int rightType = c.right.returnType.type;
    boolean sameType = leftType == rightType;
    boolean leftIsGenerator =
      leftType == ClassifierReturnType.DISCRETE_GENERATOR
      || rightType == ClassifierReturnType.REAL_GENERATOR;
    boolean leftIsPrimitive =
      leftType == ClassifierReturnType.DISCRETE
      || leftType == ClassifierReturnType.REAL;
    boolean rightIsPrimitive =
      rightType == ClassifierReturnType.DISCRETE
      || rightType == ClassifierReturnType.REAL;
    boolean bothMulti = !leftIsPrimitive && !rightIsPrimitive;

    StringBuffer body = new StringBuffer();

    if (primitive)
      body.append("    __result = left.featureValue(__example)"
                  + ".conjunction(right.featureValue(__example), this);\n");
    else {
      body.append("    __result = new FeatureVector();\n");
      if (leftIsPrimitive)
        body.append("    Feature lf = left.featureValue(__example);\n");
      else
        body.append(
              "    FeatureVector leftVector = left.classify(__example);\n"
            + "    int N = leftVector.featuresSize();\n");

      if (c.left.equals(c.right)) {
        // SemanticAnalysis ensures that neither classifier is primitive here.
        body.append(  "    for (int j = 1; j < N; ++j)\n"
                    + "    {\n");
        body.append(  "      Feature rf = leftVector.getFeature(j);\n"
                    + "      for (int i = 0; i < j; ++i)\n"
                    + "        __result.addFeature(leftVector.getFeature(i)"
                    + ".conjunction(rf, this));\n"
                    + "    }\n");
      }
      else {
        if (rightIsPrimitive)
          body.append("    Feature rf = right.featureValue(__example);\n");
        else
          body.append(
                "    FeatureVector rightVector = right.classify(__example);\n"
              + "    int M = rightVector.featuresSize();\n");

        String in = "";
        if (!leftIsPrimitive) {
          body.append(  "    for (int i = 0; i < N; ++i)\n"
                      + "    {\n"
                      + "      Feature lf = leftVector.getFeature(i);\n");
          in += "  ";
        }

        if (!rightIsPrimitive) {
          body.append(in);
          body.append("    for (int j = 0; j < M; ++j)\n");
          body.append(in);
          body.append("    {\n");
          body.append(in);
          body.append("      Feature rf = rightVector.getFeature(j);\n");
          in += "  ";
        }

        if (mixed || leftIsGenerator && sameType) {
          body.append(in);
          body.append("    if (lf.equals(rf)) continue;\n");
        }

        body.append(in);
        body.append("    __result.addFeature(lf.conjunction(rf, this));\n");

        if (!rightIsPrimitive) {
          in = in.substring(2);
          body.append(in);
          body.append("    }\n");
        }

        if (!leftIsPrimitive) body.append("    }\n");
        body.append("\n");
      }

      if (bothMulti) body.append("    __result.sort();\n");
    }

    generateClassificationMethods(out, c, null, body.toString(), false, false,
                                  null);
    out.println();
    typeCheckClassifyArray(out, cName, input, c.line);
    out.println();
    generateHashingMethods(out, cName);

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param cd The node to process.
   **/
  public void run(ConstraintDeclaration cd) {
    String cdName = cd.name.toString();
    String fileName = cdName + ".java";

    if (!RevisionAnalysis.revisionStatus.get(cdName)
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + cdName);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.print("// ");
    compressAndPrint(cd.shallow(), out);
    out.println("\n");

    ast.symbolTable.generateHeader(out);

    out.println("\n");
    if (cd.comment != null) out.println(cd.comment);

    out.println("public class " + cdName
                + " extends ParameterizedConstraint");
    out.println("{");

    HashSet invoked = (HashSet) SemanticAnalysis.invokedGraph.get(cdName);
    if (invoked != null && invoked.size() > 0) {
      for (Iterator I = invoked.iterator(); I.hasNext(); ) {
        String name = (String) I.next();
        String nameNoDots = name.replace('.', '$');
        out.println("  private static final " + name + " __" + nameNoDots
                    + " = new " + name + "();");
      }

      out.println();
    }

    String fqName = ast.symbolTable.getPackage();
    if (fqName.length() > 0) fqName += ".";
    fqName += cdName;
    out.println("  public " + cdName + "() { super(\"" + fqName + "\"); }\n");

    LBJ2.IR.Type input = cd.argument.getType();
    out.println("  public String getInputType() { return \""
                + input.typeClass().getName() + "\"; }\n");

    indent = 2;
    forInit = false;
    constraintMode = false;
    methodBody.delete(0, methodBody.length());
    currentCG = cd;
    for (ASTNodeIterator I = cd.body.iterator(); I.hasNext(); ) {
      I.next().runPass(this);
      methodBody.append("\n");
    }

    out.println("  public String discreteValue(Object __example)");
    out.println("  {");
    out.print(
        generateTypeChecking("    ", cdName, "Constraint", false,
                             input.toString(), cd.line, "__example", false));

    out.println("    " + cd.argument + " = (" + input + ") __example;\n");

    out.println(methodBody);

    out.println("    return \"true\";");
    out.println("  }");

    out.println();
    typeCheckClassifyArray(out, cdName, input, cd.line);
    out.println();
    generateHashingMethods(out, cdName);

    indent = 2;
    forInit = false;
    constraintMode = true;
    methodBody.delete(0, methodBody.length());
    for (ASTNodeIterator I = cd.body.iterator(); I.hasNext(); ) {
      I.next().runPass(this);
      methodBody.append("\n");
    }

    out.println("\n  public FirstOrderConstraint makeConstraint(Object "
                + "__example)");
    out.println("  {");
    out.print(
        generateTypeChecking("    ", cdName, "Constraint", false,
                             input.toString(), cd.line, "__example", false));

    out.println("    " + cd.argument + " = (" + input + ") __example;");
    out.println("    FirstOrderConstraint __result = new "
                + "FirstOrderConstant(true);\n");

    out.println(methodBody);

    out.println("    return __result;");
    out.println("  }");

    out.println("}\n");
    out.close();
  }


  /**
    * Generates code for all nodes of the indicated type.
    *
    * @param in The node to process.
   **/
  public void run(InferenceDeclaration in) {
    in.constraint.runPass(this);

    String inName = in.name.toString();
    String fileName = inName + ".java";

    if (!RevisionAnalysis.revisionStatus.get(inName)
         .equals(RevisionAnalysis.REVISED))
      return;

    System.out.println("Generating code for " + inName);

    PrintStream out = open(fileName);
    if (out == null) return;

    out.println(disclaimer);
    out.print("// ");
    compressAndPrint(in.shallow(), out);
    out.println("\n");

    ast.symbolTable.generateHeader(out);
    out.println("import java.util.*;\n\n");

    currentCG = in;
    String defaultNormalizer = "new IdentityNormalizer()";

    if (in.comment != null) out.println(in.comment);

    out.println("public class " + inName + " extends " + in.algorithm.name);
    out.println("{");

    if (in.containsTypeSpecificNormalizer()) {
      out.println("  private static final HashMap normalizers = new "
                  + "HashMap();");
      out.println("  static");
      out.println("  {");
      for (int i = 0; i < in.normalizerDeclarations.length; ++i) {
        if (in.normalizerDeclarations[i].learner != null)
          out.println("    normalizers.put(new "
                      + in.normalizerDeclarations[i].learner + "(), "
                      + in.normalizerDeclarations[i].normalizer + ");");
        else
          defaultNormalizer =
            in.normalizerDeclarations[i].normalizer.toString();
      }

      out.println("  }\n");
    }
    else
      for (int i = 0; i < in.normalizerDeclarations.length; ++i)
        defaultNormalizer =
          in.normalizerDeclarations[i].normalizer.toString();

    indent = 1;
    forInit = false;
    constraintMode = false;
    methodBody.delete(0, methodBody.length());
    for (int i = 0; i < in.headFinders.length; ++i)
      in.headFinders[i].runPass(this);
    out.println(methodBody);

    out.println("  public " + inName + "() { }");
    out.println("  public " + inName + "(" + in.head.getType() + " head)");
    out.println("  {");
    out.print("    super(head");
    if (in.algorithm.arguments.size() > 0)
      out.print(", " + in.algorithm.arguments);
    out.println(");");
    out.println("    constraint = new " + in.constraint.name
                + "().makeConstraint(head);");
    out.println("  }\n");

    out.println("  public String getHeadType() { return \""
                + in.head.getType().typeClass().getName() + "\"; }");
    out.println("  public String[] getHeadFinderTypes()");
    out.println("  {");
    out.print("    return new String[]{ \""
              + in.headFinders[0].argument.getType().typeClass().getName()
              + "\"");
    for (int i = 1; i < in.headFinders.length; ++i)
      out.print(", \""
                + in.headFinders[i].argument.getType().typeClass().getName()
                + "\"");
    out.println(" };");
    out.println("  }\n");

    out.println("  public Normalizer getNormalizer(Learner c)");
    out.println("  {");

    if (in.containsTypeSpecificNormalizer()) {
      out.println("    Normalizer result = (Normalizer) normalizers.get(c);");
      out.println("    if (result == null)");
      out.println("      result = " + defaultNormalizer + ";");
      out.println("    return result;");
    }
    else out.println("    return " + defaultNormalizer + ";");

    out.println("  }");

    out.println("}\n");
    out.close();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param h  The node to process.
   **/
  public void run(InferenceDeclaration.HeadFinder h) {
    appendIndent("public static ");
    methodBody.append(((InferenceDeclaration) currentCG).head.getType());
    methodBody.append(" findHead(" + h.argument + ")\n");
    ++indent;
    h.body.runPass(this);
    --indent;
    methodBody.append("\n\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param b  The node to process.
   **/
  public void run(Block b) {
    --indent;
    appendLine("{");

    ++indent;
    runOnChildren(b);
    methodBody.append("\n");
    --indent;

    appendIndent("}");
    ++indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(StatementList l) {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    if (I.hasNext()) I.next().runPass(this);
    while (I.hasNext()) {
      methodBody.append("\n");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(AssertStatement s) {
    appendIndent("assert ");
    s.condition.runPass(this);

    if (s.message != null) {
      methodBody.append(" : ");
      s.message.runPass(this);
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(BreakStatement s) {
    appendIndent("break");

    if (s.label != null) {
      methodBody.append(" ");
      methodBody.append(s.label);
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ContinueStatement s) {
    appendIndent("continue");

    if (s.label != null) {
      methodBody.append(" ");
      methodBody.append(s.label);
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ExpressionStatement s) {
    if (s.expression instanceof ConstraintStatementExpression)
      s.expression.runPass(this);
    else {
      appendIndent();
      s.expression.runPass(this);
      methodBody.append(";");
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ForStatement s) {
    appendIndent("for (");

    if (s.initializers != null) {
      s.initializers.runPass(this);
      methodBody.append("; ");
    }
    else if (s.initializer != null) {
      forInit = true;
      s.initializer.runPass(this);
      methodBody.append(" ");
      forInit = false;
    }
    else methodBody.append("; ");

    if (s.condition != null) s.condition.runPass(this);
    methodBody.append("; ");
    if (s.updaters != null) s.updaters.runPass(this);
    methodBody.append(")\n");
    ++indent;
    s.body.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(IfStatement s) {
    appendIndent("if (");
    s.condition.runPass(this);
    methodBody.append(")\n");
    ++indent;
    s.thenClause.runPass(this);
    --indent;

    if (s.elseClause != null) {
      methodBody.append("\n");
      appendLine("else");
      ++indent;
      s.elseClause.runPass(this);
      --indent;
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(LabeledStatement s) {
    appendIndent(s.label + ": ");
    s.statement.runPass(this);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ReturnStatement s) {
    appendIndent();

    if (currentCG instanceof CodedClassifier
        && ((CodedClassifier) currentCG).returnType.type
           == ClassifierReturnType.DISCRETE) {
      String literal = toStringLiteral(s.expression);
      methodBody.append("return ");
      if (literal != null) methodBody.append(literal);
      else {
        methodBody.append("\"\" + (");
        s.expression.runPass(this);
        methodBody.append(')');
      }
    }
    else {
      methodBody.append("return ");
      s.expression.runPass(this);
    }

    methodBody.append(";");
  }


  /**
    * If the given expression can be converted to a string at compile time,
    * this method returns that string.
    *
    * @param e  The given expression.
    * @return The compile time conversion of the expression to a string, or
    *         <code>null</code> if it wasn't possible to convert.
   **/
  private static String toStringLiteral(Expression e) {
    if (e instanceof Constant) {
      Constant c = (Constant) e;
      if (c.typeCache instanceof PrimitiveType) return "\"" + c.value + "\"";
      return c.value;
    }

    return null;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ss The node to process.
   **/
  public void run(SenseStatement ss) {
    CodedClassifier currentCC = (CodedClassifier) currentCG;

    if (ss.value instanceof MethodInvocation) {
      MethodInvocation m = (MethodInvocation) ss.value;
      if (m.isClassifierInvocation) {
        ClassifierReturnType invokedType =
          ((ClassifierType) m.name.typeCache).getOutput();
        int t = invokedType.type;

        if ((currentCC.returnType.type
               == ClassifierReturnType.DISCRETE_GENERATOR
             || currentCC.returnType.type
                == ClassifierReturnType.REAL_GENERATOR)
            && (t == ClassifierReturnType.DISCRETE_GENERATOR
                || t == ClassifierReturnType.REAL_GENERATOR
                || t == ClassifierReturnType.DISCRETE_ARRAY
                || t == ClassifierReturnType.REAL_ARRAY)) {
          appendIndent("__id = ");
          String s = toStringLiteral(ss.name);
          if (s != null) methodBody.append(s);
          else {
            methodBody.append("\"\" + (");
            ss.name.runPass(this);
            methodBody.append(")");
          }
          methodBody.append(";\n");

          appendLine("{");
          ++indent;

          appendIndent("FeatureVector __temp = ");
          ss.value.runPass(this);
          methodBody.append(";\n");

          appendLine("for (int __i = 0; __i < __temp.featuresSize(); ++__i)");
          ++indent;

          boolean isDiscrete = t == ClassifierReturnType.DISCRETE_GENERATOR
                               || t == ClassifierReturnType.DISCRETE_ARRAY;

          appendIndent("__result.addFeature(new ");
          methodBody.append(isDiscrete ? "Discrete" : "Real");
          methodBody.append("ReferringStringFeature");
          methodBody.append("(this, __id, (");
          methodBody.append(isDiscrete ? "Discrete" : "Real");
          methodBody.append("Feature) __temp.getFeature(__i)");
          if (currentCC.returnType.values.size() > 0
              && !currentCC.returnType.values.equals(invokedType.values)) {
            methodBody.append(", ");
            methodBody.append(m.name.toString());
            methodBody.append(".getAllowableValues()");
          }
          methodBody.append("));\n");

          indent -= 2;
          appendIndent("}");
          return;
        }
        else if ((currentCC.returnType.type
                    == ClassifierReturnType.DISCRETE_ARRAY
                  || currentCC.returnType.type
                     == ClassifierReturnType.REAL_ARRAY)
                 && (t == ClassifierReturnType.DISCRETE_ARRAY
                     || t == ClassifierReturnType.REAL_ARRAY)) {
          appendLine("{");
          ++indent;
          boolean isDiscrete = t == ClassifierReturnType.DISCRETE_ARRAY;

          appendIndent("FeatureVector __temp = ");
          ss.value.runPass(this);
          methodBody.append(";\n");

          appendLine("for (int __i = 0; __i < __temp.featuresSize(); ++__i)");
          appendLine("{");
          ++indent;
          appendLine("Feature __f = __temp.getFeature(__i);");
          appendIndent("__value = __f.");
          methodBody.append(isDiscrete ? "getStringValue" : "getStrength");
          methodBody.append("();\n");

          appendIndent("__result.addFeature(");
          methodBody.append(
              primitiveFeatureConstructorInvocation(
                  isDiscrete, true, "this", "\"\"", "__value", null,
                  "" + currentCC.returnType.values.size(),
                  "__featureIndex++, 0"));
          methodBody.append(");\n");

          --indent;
          appendLine("}");

          --indent;
          appendIndent("}");
          return;
        }
      }
    }

    boolean discrete =
      currentCC.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
      || currentCC.returnType.type == ClassifierReturnType.DISCRETE_ARRAY;

    if (ss.senseall) {
      if (ss.name != null) { // if we're inside a generator
        appendIndent("Object __values = ");
        ss.name.runPass(this);
        methodBody.append(";\n\n");

        appendLine("if (__values instanceof java.util.Collection)");
        appendLine("{");
        ++indent;

        appendLine(
            "for (java.util.Iterator __I = ((java.util.Collection) "
            + "__values).iterator(); __I.hasNext(); )");
        appendLine("{");
        ++indent;
        appendLine("__id = __I.next().toString();");
        senseFeature(ss, currentCC, discrete, discrete ? "\"true\"" : "1");
        methodBody.append("\n");

        --indent;
        appendLine("}");
        --indent;
        appendLine("}");

        appendLine("else");
        appendLine("{");
        ++indent;
        appendLine(
            "for (java.util.Iterator __I = ((java.util.Map) "
            + "__values).entrySet().iterator(); __I.hasNext(); )");
        appendLine("{");
        ++indent;
        appendLine(
            "java.util.Map.Entry __e = (java.util.Map.Entry) __I.next();");
        appendLine("__id = __e.getKey().toString();");
        appendIndent("__value = ");
        methodBody.append(
            discrete ? "__e.getValue().toString()"
                     : "((Double) __e.getValue()).doubleValue()");
        methodBody.append(";\n");

        senseFeature(ss, currentCC, discrete, null);
        methodBody.append("\n");
        --indent;
        appendLine("}");
        --indent;
        appendLine("}");
      }
      else {
        appendIndent("java.util.Collection __values = ");
        ss.value.runPass(this);
        methodBody.append(";\n\n");

        appendLine(
            "for (java.util.Iterator __I = ((java.util.Collection) "
            + "__values).iterator(); __I.hasNext(); )");
        appendLine("{");
        ++indent;

        appendIndent("__value = ");
        methodBody.append(
            discrete ? "__I.next().toString()"
                     : "((Double) __I.next()).doubleValue()");
        methodBody.append(";\n");

        senseFeature(ss, currentCC, discrete, null);
        methodBody.append("\n");
        --indent;
        appendIndent("}");
      }
    }
    else {
      if (ss.name != null) { // if we're inside a generator
        appendIndent("__id = ");
        String s = toStringLiteral(ss.name);
        if (s != null) methodBody.append(s);
        else {
          methodBody.append("\"\" + (");
          ss.name.runPass(this);
          methodBody.append(")");
        }
        methodBody.append(";\n");
      }

      appendIndent("__value = ");
      if (discrete) {
        String s = toStringLiteral(ss.value);
        if (s != null) methodBody.append(s);
        else {
          methodBody.append("\"\" + (");
          ss.value.runPass(this);
          methodBody.append(")");
        }
      }
      else ss.value.runPass(this);
      methodBody.append(";\n");

      senseFeature(ss, currentCC, discrete, null);
    }
  }


  /**
    * Generates the statement that adds a new feature of the appropriate type
    * to the returned <code>FeatureVector</code> when a <code>sense</code>
    * statement is executed.  The code generated by this method assumes the
    * following:
    * <ul>
    *   <li>
    *     if the containing classifier is a generator, code has already been
    *     generated to set the value of a string named <code>__id</code>
    *     representing the identifier of the new feature and
    *   <li>
    *     if <code>value</code> is <code>null</code>, code has already been
    *     generated to set the value of a string or double as appropriate
    *     named <code>__value</code> representing the value of the new
    *     feature.
    * </ul>
    *
    * @param s        The <code>sense</code> statement.
    * @param cc       The current coded classifier.
    * @param discrete Whether or not <code>cc</code> is discrete.
    * @param value    Generated code evaluating to the new feature's value.
    *                 If this parameter is <code>null</code>, it will default
    *                 to <code>"__value"</code>.
   **/
  private void senseFeature(SenseStatement s, CodedClassifier cc,
                            boolean discrete, String value) {
    appendIndent("__result.addFeature(");
    boolean array = s.name == null;
    String id = array ? "\"\"" : "__id";
    if (value == null) value = "__value";
    methodBody.append(
        primitiveFeatureConstructorInvocation(
            discrete, array, "this", id, value, null,
            "" + cc.returnType.values.size(), "__featureIndex++, 0"));
    methodBody.append(");");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(SwitchStatement s) {
    appendIndent("switch (");
    s.expression.runPass(this);
    methodBody.append(")\n");
    s.block.runPass(this);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(SynchronizedStatement s) {
    appendIndent("synchronized (");
    s.data.runPass(this);
    methodBody.append(")\n");
    ++indent;
    s.block.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ThrowStatement s) {
    appendIndent("throw ");
    s.exception.runPass(this);
    methodBody.append(";\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(TryStatement s) {
    appendLine("try");
    ++indent;
    s.block.runPass(this);
    --indent;
    s.catchList.runPass(this);
    if (s.finallyBlock != null) {
      appendLine("finally");
      s.finallyBlock.runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(VariableDeclaration s) {
    if (!forInit) appendIndent();
    if (s.isFinal) methodBody.append("final ");
    s.type.runPass(this);

    ASTNodeIterator N = s.names.iterator();
    methodBody.append(" " + N.next());
    ExpressionList.ExpressionListIterator I = s.initializers.listIterator();
    Expression i = I.nextItem();
    if (i != null) {
      methodBody.append(" = ");
      i.runPass(this);
    }

    while (N.hasNext()) {
      methodBody.append(", " + N.next());
      i = I.nextItem();
      if (i != null) {
        methodBody.append(" = ");
        i.runPass(this);
      }
    }

    methodBody.append(";");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(WhileStatement s) {
    appendIndent("while (");
    s.condition.runPass(this);
    methodBody.append(")\n");
    ++indent;
    s.body.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(DoStatement s) {
    appendLine("do");
    ++indent;
    s.body.runPass(this);
    --indent;

    appendIndent("while (");
    s.condition.runPass(this);
    methodBody.append(");\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(SwitchGroupList l) {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    I.next().runPass(this);
    while (I.hasNext()) {
      methodBody.append("\n");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param g  The node to process.
   **/
  public void run(SwitchGroup g) {
    appendIndent();
    g.labels.runPass(this);
    methodBody.append("\n");
    ++indent;
    g.statements.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(SwitchLabelList l) {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    I.next().runPass(this);
    while (I.hasNext()) {
      methodBody.append(" ");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(SwitchLabel l) {
    methodBody.append("case ");
    l.value.runPass(this);
    methodBody.append(":");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(CatchList l) {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    I.next().runPass(this);
    while (I.hasNext()) {
      methodBody.append("\n");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(CatchClause c) {
    appendIndent("catch (");
    c.argument.runPass(this);
    methodBody.append(")\n");
    ++indent;
    c.block.runPass(this);
    --indent;
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param a  The node to process.
   **/
  public void run(Argument a) {
    if (a.getFinal()) methodBody.append("final ");
    a.getType().runPass(this);
    methodBody.append(" " + a.getName());
  }


  /**
    * This method generates the code for a new temporary variable used when
    * translating constraints.
    *
    * @param name The name of the temporary variable.
   **/
  private void constraintTemporary(String name) {
    appendIndent();
    if (constraintMode) methodBody.append("FirstOrderConstraint ");
    else methodBody.append("boolean ");
    methodBody.append(name);
    if (constraintMode) methodBody.append(" = null;\n");
    else methodBody.append(";\n");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintStatementExpression e) {
    constraintResultNumber = 0;
    appendLine("{");
    ++indent;

    if (constraintMode && e.constraint.containsQuantifiedVariable()) {
      StringBuffer buffer = new StringBuffer();
      int i = 0;
      HashSet referenced = e.constraint.getVariableTypes();
      for (Iterator I = referenced.iterator(); I.hasNext(); ) {
        Argument a = (Argument) I.next();
        LBJ2.IR.Type t = a.getType();
        if (t.quantifierArgumentType) continue;

        for (int j = 0; j < indent; ++j) buffer.append("  ");
        buffer.append("LBJ$constraint$context[");
        buffer.append(i);
        buffer.append("] = ");
        if (t instanceof PrimitiveType) {
          String primitiveTypeName = null;
          if (((PrimitiveType) t).type == PrimitiveType.INT)
            primitiveTypeName = "Integer";
          else {
            primitiveTypeName = t.toString();
            primitiveTypeName =
              Character.toUpperCase(primitiveTypeName.charAt(0))
              + primitiveTypeName.substring(1);
          }

          buffer.append("new ");
          buffer.append(primitiveTypeName);
          buffer.append("(");
        }

        buffer.append(a.getName());
        if (t instanceof PrimitiveType) buffer.append(")");
        buffer.append(";\n");

        contextVariables.put(a.getName(), new Integer(i++));
      }

      appendIndent("Object[] LBJ$constraint$context = new Object[");
      methodBody.append(i);
      methodBody.append("];\n");
      methodBody.append(buffer);
    }

    String childResultName = constraintResult + constraintResultNumber;
    constraintResultName = childResultName;
    constraintTemporary(childResultName);
    quantifierNesting = 0;

    e.constraint.runPass(this);

    appendIndent();
    if (constraintMode) {
      methodBody.append("__result = new FirstOrderConjunction(__result, ");
      methodBody.append(childResultName);
      methodBody.append(");\n");
    }
    else {
      methodBody.append("if (!");
      methodBody.append(childResultName);
      methodBody.append(") return \"false\";\n");
    }

    --indent;
    appendIndent("}");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(BinaryConstraintExpression e) {
    String myResultName = constraintResultName;
    String leftResultName = constraintResult + ++constraintResultNumber;

    appendLine("{");
    ++indent;

    constraintTemporary(leftResultName);
    constraintResultName = leftResultName;
    e.left.runPass(this);

    if (constraintMode
        || e.operation.operation == Operator.DOUBLE_IMPLICATION) {
      String rightResultName = constraintResult + ++constraintResultNumber;
      constraintTemporary(rightResultName);
      constraintResultName = rightResultName;
      e.right.runPass(this);

      appendIndent(myResultName);

      if (constraintMode) {
        methodBody.append(" = new FirstOrder");
        if (e.operation.operation == Operator.LOGICAL_CONJUNCTION)
          methodBody.append("Conjunction");
        else if (e.operation.operation == Operator.LOGICAL_DISJUNCTION)
          methodBody.append("Disjunction");
        else if (e.operation.operation == Operator.IMPLICATION)
          methodBody.append("Implication");
        else methodBody.append("DoubleImplication");

        methodBody.append("(");
        methodBody.append(leftResultName);
        methodBody.append(", ");
        methodBody.append(rightResultName);
        methodBody.append(");\n");
      }
      else {
        methodBody.append(" = ");
        methodBody.append(leftResultName);
        methodBody.append(" == ");
        methodBody.append(rightResultName);
        methodBody.append(";\n");
      }
    }
    else {
      appendIndent("if (");
      if (e.operation.operation == Operator.LOGICAL_DISJUNCTION)
        methodBody.append("!");
      methodBody.append(leftResultName);
      methodBody.append(")\n");
      ++indent;

      constraintResultName = myResultName;
      e.right.runPass(this);

      --indent;
      appendIndent("else ");
      methodBody.append(myResultName);
      methodBody.append(" = ");
      methodBody.append(e.operation.operation == Operator.LOGICAL_DISJUNCTION
                        || e.operation.operation == Operator.IMPLICATION);
      methodBody.append(";\n");
    }

    --indent;
    appendLine("}");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(NegatedConstraintExpression e) {
    String myResultName = constraintResultName;
    String childResultName = constraintResult + ++constraintResultNumber;

    appendLine("{");
    ++indent;

    constraintTemporary(childResultName);
    constraintResultName = childResultName;
    e.constraint.runPass(this);

    appendIndent(myResultName);
    methodBody.append(" = ");
    if (constraintMode) methodBody.append("new FirstOrderNegation(");
    else methodBody.append("!");
    methodBody.append(childResultName);
    if (constraintMode) methodBody.append(")");
    methodBody.append(";\n");

    --indent;
    appendLine("}");
  }


  /**
    * Generates the code necessary at the top of a replacer method
    * implementation to declare the variables that will be used in the method.
    *
    * @param expression The expression to be evaluted in the replacer method.
   **/
  private void generateReplacerMethodEnvironment(Expression expression) {
    for (Iterator I = expression.getVariableTypes().iterator(); I.hasNext(); )
    {
      Argument a = (Argument) I.next();
      LBJ2.IR.Type type = a.getType();
      String primitiveTypeName = null;
      if (type instanceof PrimitiveType) {
        if (((PrimitiveType) type).type == PrimitiveType.INT)
          primitiveTypeName = "Integer";
        else {
          primitiveTypeName = type.toString();
          primitiveTypeName =
            Character.toUpperCase(primitiveTypeName.charAt(0))
            + primitiveTypeName.substring(1);
        }
      }

      appendIndent();
      a.runPass(this);
      methodBody.append(" = (");
      if (primitiveTypeName == null) type.runPass(this);
      else methodBody.append("(" + primitiveTypeName);
      methodBody.append(") ");

      if (type.quantifierArgumentType) {
        methodBody.append("quantificationVariables.get(");
        methodBody.append(
            ((Integer) quantificationVariables.get(a.getName()))
            .intValue());
        methodBody.append(")");
      }
      else {
        methodBody.append("context[");
        methodBody.append(
            ((Integer) contextVariables.get(a.getName())).intValue());
        methodBody.append("]");
      }

      if (primitiveTypeName != null)
        methodBody.append(")." + type + "Value()");
      methodBody.append(";\n");
    }
  }


  /**
    * Translates an expression from a quantified
    * {@link ConstraintEqualityExpression} into the appropriate method of an
    * {@link EqualityArgumentReplacer}.
    *
    * @param right              Indicates if <code>expression</code> comes
    *                           from the right hand side of the equality.
    * @param expression         The expression.
    * @param isDiscreteLearner  This flag is set if <code>expression</code>
    *                           represents a variable.
   **/
  private void generateEARMethod(boolean right, Expression expression,
                                 boolean isDiscreteLearner) {
    appendIndent("public ");
    methodBody.append(isDiscreteLearner ? "Object" : "String");
    methodBody.append(" get");
    methodBody.append(right ? "Right" : "Left");
    methodBody.append(isDiscreteLearner ? "Object" : "Value");
    methodBody.append("()\n");

    appendLine("{");
    ++indent;

    generateReplacerMethodEnvironment(expression);

    appendIndent("return ");
    if (isDiscreteLearner)
      ((MethodInvocation) expression).arguments.runPass(this);
    else {
      methodBody.append("\"\" + (");
      expression.runPass(this);
      methodBody.append(")");
    }

    methodBody.append(";\n");

    --indent;
    appendLine("}");
  }


  /**
    * Translates an unquantified expression not representing a first order
    * variable from a {@link ConstraintEqualityExpression} into an argument of
    * a {@link FirstOrderEquality}.
    *
    * @param left       This flag is set if <code>expression</code> came from
    *                   the left hand side of the equality.
    * @param expression The expression.
   **/
  private void generateNotVariable(boolean left, Expression expression) {
    if (left) methodBody.append("(");
    methodBody.append("\"\" + (");
    expression.runPass(this);
    methodBody.append(")");
    if (left) methodBody.append(")");
  }


  /**
    * Translates an expression representing a first order variable from a
    * {@link ConstraintEqualityExpression} into an argument of a
    * {@link FirstOrderEquality}.
    *
    * @param expression   The expression.
    * @param isQuantified This flag is set if <code>expression</code> contains
    *                     a quantified variable.
   **/
  private void generateVariable(Expression expression, boolean isQuantified) {
    MethodInvocation method = (MethodInvocation) expression;
    methodBody.append("new FirstOrderVariable(");
    methodBody.append(("__" + method.name).replace('.', '$'));

    if (isQuantified) methodBody.append(", null)");
    else {
      methodBody.append(", ");
      method.arguments.runPass(this);
      methodBody.append(")");
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintEqualityExpression e) {
    String myResultName = constraintResultName;

    boolean leftIsDiscreteLearner = e.leftIsDiscreteLearner;
    boolean rightIsDiscreteLearner = e.rightIsDiscreteLearner;
    boolean leftIsQuantified = e.leftIsQuantified;
    boolean rightIsQuantified = e.rightIsQuantified;
    Expression left = e.left;
    Expression right = e.right;

    if (!leftIsDiscreteLearner && rightIsDiscreteLearner) {
      leftIsDiscreteLearner = true;
      rightIsDiscreteLearner = false;

      leftIsQuantified ^= rightIsQuantified;
      rightIsQuantified ^= leftIsQuantified;
      leftIsQuantified ^= rightIsQuantified;

      Expression temp = left;
      left = right;
      right = temp;
    }

    if (!(constraintMode && (leftIsQuantified || rightIsQuantified))) {
      appendIndent(myResultName);
      methodBody.append(" = ");

      if (constraintMode) {
        methodBody.append("new FirstOrder");

        if (leftIsDiscreteLearner) {
          if (rightIsDiscreteLearner)
            methodBody.append("EqualityWithVariable");
          else methodBody.append("EqualityWithValue");
          methodBody.append("(");

          methodBody.append(e.operation.operation
                            == Operator.CONSTRAINT_EQUAL);
          methodBody.append(", ");
          generateVariable(left, false);
          methodBody.append(", ");
          if (rightIsDiscreteLearner) generateVariable(right, false);
          else generateNotVariable(false, right);

          methodBody.append(");\n");
          return;
        }

        methodBody.append("Constant(");
      }

      if (e.operation.operation == Operator.CONSTRAINT_NOT_EQUAL)
        methodBody.append("!");
      generateNotVariable(true, left);
      methodBody.append(".equals(");
      generateNotVariable(false, right);
      methodBody.append(")");

      if (constraintMode) methodBody.append(")");
      methodBody.append(";\n");
      return;
    }

    appendLine("{");
    ++indent;

    appendLine("EqualityArgumentReplacer LBJ$EAR =");
    ++indent;

    appendIndent("new EqualityArgumentReplacer(LBJ$constraint$context");
    if (!(leftIsQuantified && rightIsQuantified)) {
      methodBody.append(", ");
      methodBody.append(leftIsQuantified);
    }

    methodBody.append(")\n");
    appendLine("{");
    ++indent;

    if (leftIsQuantified)
      generateEARMethod(false, left, leftIsDiscreteLearner);

    if (rightIsQuantified) {
      if (leftIsQuantified) methodBody.append("\n");
      generateEARMethod(true, right, rightIsDiscreteLearner);
    }

    --indent;
    appendLine("};");
    --indent;

    appendIndent(myResultName);
    methodBody.append(" = new FirstOrderEquality");
    if (leftIsDiscreteLearner) {
      if (rightIsDiscreteLearner) methodBody.append("WithVariable");
      else methodBody.append("WithValue");
    }
    else methodBody.append("TwoValues");

    methodBody.append("(");
    methodBody.append(e.operation.operation == Operator.CONSTRAINT_EQUAL);
    methodBody.append(", ");
    if (leftIsDiscreteLearner) generateVariable(left, leftIsQuantified);
    else if (leftIsQuantified) methodBody.append("null");
    else generateNotVariable(true, left);
    methodBody.append(", ");
    if (rightIsDiscreteLearner) generateVariable(right, rightIsQuantified);
    else if (rightIsQuantified) methodBody.append("null");
    else generateNotVariable(false, right);
    methodBody.append(", LBJ$EAR);\n");

    --indent;
    appendLine("}");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintInvocation e) {
    String myResultName = constraintResultName;

    if (!(constraintMode && e.invocationIsQuantified)) {
      appendIndent(myResultName);
      methodBody.append((" = __" + e.invocation.name).replace('.', '$'));
      if (constraintMode) methodBody.append(".makeConstraint(");
      else methodBody.append(".discreteValue(");
      e.invocation.arguments.runPass(this);
      methodBody.append(")");
      if (!constraintMode) methodBody.append(".equals(\"true\")");
      methodBody.append(";\n");
      return;
    }

    appendLine("{");
    ++indent;

    appendLine("InvocationArgumentReplacer LBJ$IAR =");
    ++indent;

    appendLine("new InvocationArgumentReplacer(LBJ$constraint$context)");
    appendLine("{");
    ++indent;

    appendLine("public Object compute()");
    appendLine("{");
    ++indent;

    Expression argument = e.invocation.arguments.listIterator().nextItem();
    generateReplacerMethodEnvironment(argument);

    appendIndent("return ");
    argument.runPass(this);
    methodBody.append(";\n");

    --indent;
    appendLine("}");

    --indent;
    appendLine("};");
    --indent;

    appendIndent(myResultName);
    methodBody.append(" = new QuantifiedConstraintInvocation(");
    methodBody.append(("__" + e.invocation.name).replace('.', '$'));
    methodBody.append(", LBJ$IAR);\n");

    --indent;
    appendLine("}");
  }


  /**
    * {@link UniversalQuantifierExpression}s and
    * {@link ExistentialQuantifierExpression}s generate their code through
    * this method.
    *
    * @param e  The node to process.
   **/
  private void generateSimpleQuantifier(QuantifiedConstraintExpression e) {
    boolean universal = e instanceof UniversalQuantifierExpression;
    String myResultName = constraintResultName;

    if (!constraintMode) {
      String inductionVariable = "__I" + quantifierNesting;

      appendLine("{");
      ++indent;

      appendIndent(myResultName);
      methodBody.append(" = ");
      methodBody.append(universal);
      methodBody.append(";\n");

      appendIndent("for (java.util.Iterator ");
      methodBody.append(inductionVariable);
      methodBody.append(" = (");
      e.collection.runPass(this);
      methodBody.append(").iterator(); ");
      methodBody.append(inductionVariable);
      methodBody.append(".hasNext() && ");
      if (!universal) methodBody.append("!");
      methodBody.append(myResultName);
      methodBody.append("; )\n");

      appendLine("{");
      ++indent;

      appendIndent();
      e.argument.runPass(this);
      methodBody.append(" = (");
      e.argument.getType().runPass(this);
      methodBody.append(") ");
      methodBody.append(inductionVariable);
      methodBody.append(".next();\n");

      ++quantifierNesting;
      e.constraint.runPass(this);
      --quantifierNesting;

      --indent;
      appendLine("}");

      --indent;
      appendLine("}");
      return;
    }

    appendLine("{");
    ++indent;

    String childResultName = constraintResult + ++constraintResultNumber;
    constraintTemporary(childResultName);
    constraintResultName = childResultName;

    quantificationVariables.put(e.argument.getName(),
                                new Integer(quantifierNesting++));
    e.constraint.runPass(this);
    --quantifierNesting;

    if (!e.collectionIsQuantified) {
      appendIndent(myResultName);
      methodBody.append(" = new ");
      if (universal) methodBody.append("Universal");
      else methodBody.append("Existential");
      methodBody.append("Quantifier(\"");
      methodBody.append(e.argument.getName());
      methodBody.append("\", ");
      e.collection.runPass(this);
      methodBody.append(", ");
      methodBody.append(childResultName);
      methodBody.append(");\n");
    }
    else {
      appendLine("QuantifierArgumentReplacer LBJ$QAR =");
      ++indent;

      appendLine("new QuantifierArgumentReplacer(LBJ$constraint$context)");
      appendLine("{");
      ++indent;

      appendLine("public java.util.Collection getCollection()");
      appendLine("{");
      ++indent;

      generateReplacerMethodEnvironment(e.collection);

      appendIndent("return ");
      e.collection.runPass(this);
      methodBody.append(";\n");

      --indent;
      appendLine("}");

      --indent;
      appendLine("};");
      --indent;

      appendIndent(myResultName);
      methodBody.append(" = new ");
      if (universal) methodBody.append("Universal");
      else methodBody.append("Existential");
      methodBody.append("Quantifier(\"");
      methodBody.append(e.argument.getName());
      methodBody.append("\", null, ");
      methodBody.append(childResultName);
      methodBody.append(", LBJ$QAR);\n");
    }

    --indent;
    appendLine("}");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(UniversalQuantifierExpression e) {
    generateSimpleQuantifier(e);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ExistentialQuantifierExpression e) {
    generateSimpleQuantifier(e);
  }


  /**
    * {@link AtLeastQuantifierExpression}s and
    * {@link AtMostQuantifierExpression}s generate their code through this
    * method.
    *
    * @param e  The node to process.
   **/
  public void generateBoundedQuantifier(QuantifiedConstraintExpression e) {
    boolean atleast = e instanceof AtLeastQuantifierExpression;
    AtLeastQuantifierExpression ale = null;
    AtMostQuantifierExpression ame = null;
    if (atleast) ale = (AtLeastQuantifierExpression) e;
    else ame = (AtMostQuantifierExpression) e;

    String myResultName = constraintResultName;
    String childResultName = constraintResult + ++constraintResultNumber;

    if (!constraintMode) {
      appendLine("{");
      ++indent;

      String m = "LBJ$m$" + quantifierNesting;
      String bound = "LBJ$bound$" + quantifierNesting;

      appendIndent("int ");
      methodBody.append(m);
      methodBody.append(" = 0;\n");
      appendIndent("int ");
      methodBody.append(bound);
      methodBody.append(" = ");
      if (atleast) ale.lowerBound.runPass(this);
      else ame.upperBound.runPass(this);
      methodBody.append(";\n");

      String inductionVariable = "__I" + quantifierNesting;

      appendIndent("for (java.util.Iterator ");
      methodBody.append(inductionVariable);
      methodBody.append(" = (");
      e.collection.runPass(this);
      methodBody.append(").iterator(); ");
      methodBody.append(inductionVariable);
      methodBody.append(".hasNext() && ");
      methodBody.append(m);
      if (atleast) methodBody.append(" < ");
      else methodBody.append(" <= ");
      methodBody.append(bound);
      methodBody.append("; )\n");

      appendLine("{");
      ++indent;

      appendIndent();
      e.argument.runPass(this);
      methodBody.append(" = (");
      e.argument.getType().runPass(this);
      methodBody.append(") ");
      methodBody.append(inductionVariable);
      methodBody.append(".next();\n");

      constraintTemporary(childResultName);
      constraintResultName = childResultName;
      ++quantifierNesting;
      e.constraint.runPass(this);
      --quantifierNesting;

      appendIndent("if (");
      methodBody.append(childResultName);
      methodBody.append(") ++");
      methodBody.append(m);
      methodBody.append(";\n");

      --indent;
      appendLine("}");

      appendIndent(myResultName);
      methodBody.append(" = ");
      methodBody.append(m);
      if (atleast) methodBody.append(" >= ");
      else methodBody.append(" <= ");
      methodBody.append(bound);
      methodBody.append(";\n");

      --indent;
      appendLine("}");
      return;
    }

    appendLine("{");
    ++indent;

    constraintTemporary(childResultName);
    constraintResultName = childResultName;
    quantificationVariables.put(e.argument.getName(),
                                new Integer(quantifierNesting++));
    e.constraint.runPass(this);
    --quantifierNesting;

    if (!(e.collectionIsQuantified
          || atleast && ale.lowerBoundIsQuantified
          || !atleast && ame.upperBoundIsQuantified)) {
      appendIndent(myResultName);
      methodBody.append(" = new ");
      if (atleast) methodBody.append("AtLeast");
      else methodBody.append("AtMost");
      methodBody.append("Quantifier(\"");
      methodBody.append(e.argument.getName());
      methodBody.append("\", ");
      e.collection.runPass(this);
      methodBody.append(", ");
      methodBody.append(childResultName);
      methodBody.append(", ");
      if (atleast) methodBody.append(ale.lowerBound);
      else methodBody.append(ame.upperBound);
      methodBody.append(");\n");
    }
    else {
      appendLine("QuantifierArgumentReplacer LBJ$QAR =");
      ++indent;

      appendIndent("new QuantifierArgumentReplacer(LBJ$constraint$context");
      if (!(e.collectionIsQuantified
            && (atleast && ale.lowerBoundIsQuantified
                || !atleast && ame.upperBoundIsQuantified))) {
        methodBody.append(", ");
        methodBody.append(e.collectionIsQuantified);
      }

      methodBody.append(")\n");

      appendLine("{");
      ++indent;

      if (e.collectionIsQuantified) {
        appendLine("public java.util.Collection getCollection()");
        appendLine("{");
        ++indent;

        generateReplacerMethodEnvironment(e.collection);

        appendIndent("return ");
        e.collection.runPass(this);
        methodBody.append(";\n");

        --indent;
        appendLine("}");
      }

      if (atleast && ale.lowerBoundIsQuantified
          || !atleast && ame.upperBoundIsQuantified) {
        if (e.collectionIsQuantified) methodBody.append("\n");
        appendLine("public int getBound()");
        appendLine("{");
        ++indent;

        if (atleast) generateReplacerMethodEnvironment(ale.lowerBound);
        else generateReplacerMethodEnvironment(ame.upperBound);

        appendIndent("return ");
        if (atleast) ale.lowerBound.runPass(this);
        else ame.upperBound.runPass(this);
        methodBody.append(";\n");

        --indent;
        appendLine("}");
      }

      --indent;
      appendLine("};");
      --indent;

      appendIndent(myResultName);
      methodBody.append(" = new ");
      if (atleast) methodBody.append("AtLeast");
      else methodBody.append("AtMost");
      methodBody.append("Quantifier(\"");
      methodBody.append(e.argument.getName());
      methodBody.append("\", ");
      if (e.collectionIsQuantified) methodBody.append("null");
      else e.collection.runPass(this);
      methodBody.append(", ");
      methodBody.append(childResultName);
      methodBody.append(", ");
      if (atleast && ale.lowerBoundIsQuantified
          || !atleast && ame.upperBoundIsQuantified)
        methodBody.append("0");
      else if (atleast) ale.lowerBound.runPass(this);
      else ame.upperBound.runPass(this);
      methodBody.append(", LBJ$QAR);\n");
    }

    --indent;
    appendLine("}");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(AtLeastQuantifierExpression e) {
    generateBoundedQuantifier(e);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(AtMostQuantifierExpression e) {
    generateBoundedQuantifier(e);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param l  The node to process.
   **/
  public void run(ExpressionList l) {
    ASTNodeIterator I = l.iterator();
    if (!I.hasNext()) return;

    I.next().runPass(this);
    while (I.hasNext()) {
      methodBody.append(", ");
      I.next().runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ArrayCreationExpression e) {
    if (e.parenthesized) methodBody.append("(");

    methodBody.append("new ");
    e.elementType.runPass(this);

    int d = 0;
    for (ASTNodeIterator I = e.sizes.iterator(); I.hasNext(); ++d) {
      methodBody.append("[");
      I.next().runPass(this);
      methodBody.append("]");
    }

    for (; d < e.dimensions; ++d) methodBody.append("[]");

    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ArrayInitializer e) {
    if (e.parenthesized) methodBody.append("(");
    methodBody.append("{ ");
    e.values.runPass(this);
    methodBody.append(" }");
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(CastExpression e) {
    if (e.parenthesized) methodBody.append("(");
    methodBody.append("(");
    e.type.runPass(this);
    methodBody.append(") ");
    e.expression.runPass(this);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(Conditional e) {
    if (e.parenthesized) methodBody.append("(");
    e.condition.runPass(this);
    methodBody.append(" ? ");
    e.thenClause.runPass(this);
    methodBody.append(" : ");
    e.elseClause.runPass(this);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(Constant e) {
    if (e.parenthesized) methodBody.append("(");
    methodBody.append(e.value);
    if (e.parenthesized) methodBody.append(")");
  }

  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ParameterSet e) {
    methodBody.append(e.getParameterName());
  }

  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(InstanceofExpression e) {
    if (e.parenthesized) methodBody.append("(");
    e.left.runPass(this);
    methodBody.append(" instanceof ");
    e.right.runPass(this);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(Assignment e) {
    if (e.parenthesized) methodBody.append("(");
    e.left.runPass(this);
    methodBody.append(" " + e.operation + " ");
    e.right.runPass(this);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(IncrementExpression e) {
    if (e.parenthesized) methodBody.append("(");
    runOnChildren(e);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(InstanceCreationExpression e) {
    if (e.parenthesized) methodBody.append("(");

    if (e.parentObject != null) {
      e.parentObject.runPass(this);
      methodBody.append(".");
    }

    methodBody.append("new ");
    e.name.runPass(this);
    methodBody.append("(");
    e.arguments.runPass(this);
    methodBody.append(")");

    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(MethodInvocation e) {
    if (e.parenthesized) methodBody.append("(");

    if (e.isClassifierInvocation) {
      methodBody.append(("__" + e.name).replace('.', '$') + ".");

      ClassifierType invokedType = (ClassifierType) e.name.typeCache;
      int t = invokedType.getOutput().type;
      if (t == ClassifierReturnType.DISCRETE)
        methodBody.append("discreteValue(");
      else if (t == ClassifierReturnType.REAL)
        methodBody.append("realValue(");
      else if (!e.isSensedValue) {
        if (t == ClassifierReturnType.DISCRETE_ARRAY)
          methodBody.append("discreteValueArray(");
        else if (t == ClassifierReturnType.REAL_ARRAY)
          methodBody.append("realValueArray(");
      }
      else methodBody.append("classify(");

      if (invokedType.getInput() instanceof ArrayType)
        methodBody.append("(Object) ");
      e.arguments.runPass(this);
      methodBody.append(")");
    }
    else {
      if (e.parentObject != null) {
        e.parentObject.runPass(this);
        methodBody.append(".");
      }

      e.name.runPass(this);
      methodBody.append("(");
      e.arguments.runPass(this);
      methodBody.append(")");
    }

    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param be The node to process.
   **/
  public void run(BinaryExpression be) {
    if (be.parenthesized) methodBody.append("(");
    be.left.runPass(this);
    methodBody.append(" " + be.operation + " ");
    be.right.runPass(this);
    if (be.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(UnaryExpression e) {
    if (e.parenthesized) methodBody.append("(");
    runOnChildren(e);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(FieldAccess e) {
    if (e.parenthesized) methodBody.append("(");
    e.object.runPass(this);
    methodBody.append("." + e.name);
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(SubscriptVariable e) {
    if (e.parenthesized) methodBody.append("(");
    e.array.runPass(this);
    methodBody.append("[");
    e.subscript.runPass(this);
    methodBody.append("]");
    if (e.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param n  The node to process.
   **/
  public void run(Name n) {
    if (n.parenthesized) methodBody.append("(");
    boolean translated = false;

    if (currentCG != null && n.name.length > 1) {
      HashSet invoked =
        (HashSet) SemanticAnalysis.invokedGraph.get(currentCG.getName());
      if (invoked != null) {
        String className = n.toString();
        className = className.substring(0, className.lastIndexOf('.'));
        String fieldOrMethod = n.name[n.name.length - 1];

        if (invoked.contains(className)) {
          String nameNoDots = className.replace('.', '$');
          methodBody.append("__");
          methodBody.append(nameNoDots);
          methodBody.append(".");
          methodBody.append(fieldOrMethod);
          translated = true;
        }
      }
    }

    if (!translated) methodBody.append(n);
    if (n.parenthesized) methodBody.append(")");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(ArrayType t) {
    t.type.runPass(this);
    methodBody.append("[]");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(PrimitiveType t) { methodBody.append(t); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(ReferenceType t) { methodBody.append(t); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param o  The node to process.
   **/
  public void run(Operator o) { methodBody.append(o); }
}

