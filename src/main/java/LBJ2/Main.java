package LBJ2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import java_cup.runtime.Symbol;

import LBJ2.IR.AST;
import LBJ2.frontend.parser;
import LBJ2.frontend.sym;
import LBJ2.frontend.SymbolNames;
import LBJ2.frontend.TokenValue;
import LBJ2.frontend.Yylex;


/**
  * LBJ2's command line interface.  Passing a source file to this class will
  * invoke LBJ2's frontend and optimization passes, resulting in the execution
  * of the source file's code including creation of java files that implement
  * the source file's semantics. <p>
  *
  * <font size=+2><b>LBJ</b></font> stands for
  * <font size=+1><b>L</b></font>earning <font size=+1><b>B</b></font>ased
  * <font size=+1><b>J</b></font>ava.  LBJ2 is a language for building systems
  * that learn. <p>
  *
  * <dl>
  *   <dt>Usage:</dt>
  *   <dd>
  *     <code>java LBJ2.Main [options] &lt;source file&gt;</code>
  *     <dl>
  *       <dt>where [options] is one or more of the following:</dt>
  *       <dd>
  *         <table>
  *           <tr>
  *             <td valign=top><code>-c</code></td>
  *             <td>
  *               Compile only: This option tells LBJ2 to translate the given
  *               source to Java, but not to compile the generated Java
  *               sources or do any training.
  *             </td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>-d &lt;directory&gt;</code></td>
  *             <td>
  *               Any class files generated during compilation will be written
  *               in the specified directory, just like <code>javac</code>'s
  *               <code>-d</code> command line parameter.
  *             </td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>-j &lt;a&gt;</code></td>
  *             <td>
  *               Sends the contents of <code>&lt;a&gt;</code> to
  *               <code>javac</code> as command line arguments while
  *               compiling.  Don't forget to put quotes around
  *               <code>&lt;a&gt;</code> if there is more than one such
  *               argument or if the argument has a parameter.
  *             </td>
  *           </tr>
  *           <!-- Doesn't work well because of interned features.
  *           <tr>
  *             <td valign=top><code>-p</code></td>
  *             <td>
  *               Train in parallel: When this option is enabled, learners
  *               that don't depend on each other are trained concurrently.
  *             </td>
  *           </tr>
  *           -->
  *           <tr>
  *             <td valign=top nowrap><code>-s</code></td>
  *             <td> Print the names of all declarations and quit. </td>
  *           </tr>
  *           <tr>
  *             <td valign=top nowrap><code>-t &lt;n&gt;</code></td>
  *             <td>
  *               Enables default progress output during training.  A message
  *               is printed every <code>&lt;n&gt;</code> examples while
  *               training any classifier whose <code>learn</code> expression
  *               doesn't contain a <code>progressOutput</code> clause.
  *             </td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>-v</code></td>
  *             <td> Prints the version number and exits. </td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>-w</code></td>
  *             <td>
  *               Disables the output of warning messages.  Currenlty, there
  *               are only two types of warnings.  A warning is reported if a
  *               constraint declaration does not contain any constraint
  *               statements, and a warning is reported if a learner's type is
  *               less specific than the declared type of the classifier it's
  *               being used in.
  *             </td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>-x</code></td>
  *             <td>
  *               Clean: Delete all files that would otherwise be generated.
  *               No code is generated and no training takes place.
  *             </td>
  *           </tr>
  *           <tr>
  *             <td valign=top nowrap>
  *               <code>-generatedsourcepath &lt;directory&gt;</code><br>
  *               <code>-gsp &lt;directory&gt;</code>
  *             </td>
  *             <td>
  *               LBJ will potentially generate many Java source files.  Use
  *               this option to have LBJ write them to the specified
  *               directory instead of the current directory.
  *               <code>&lt;directory&gt;</code> must already exist.  Note
  *               that LBJ will also compile these files which can result in
  *               even more class files than there were sources.  Those class
  *               files will also be written in <code>&lt;directory&gt;</code>
  *               unless the <code>-d</code> command line parameter is
  *               utilized as well.
  *             </td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>-sourcepath &lt;path&gt;</code></td>
  *             <td>
  *               If the LBJ source depends on classes whose source files
  *               cannot be found on the user's classpath, specify the
  *               directories where they can be found using this parameter.
  *               It works just like <code>javac</code>'s
  *               <code>-sourcepath</code> command line parameter.
  *             </td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>--parserDebug</code></td>
  *             <td>Debug: Debug output for parse phase only.</td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>--lexerOutput</code></td>
  *             <td>Lexer output: Print lexical token stream and quit.</td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>--parserOutput</code></td>
  *             <td>Parser output: Print the parsed AST and quit.</td>
  *           </tr>
  *           <tr>
  *             <td valign=top><code>--semanticOutput</code></td>
  *             <td>
  *               Semantic analysis output: Print semantic analysis
  *               information and quit.
  *             </td>
  *           </tr>
  *         </table>
  *       </dd>
  *     </dl>
  *   </dd>
  * </dl>
  *
  * @author Nick Rizzolo
 **/
public class Main
{
  /**
    * This flag is set to <code>true</code> if token printing is enabled on
    * the command line.  Tokens are the output from the scanner.
   **/
  private static boolean printTokens = false;
  /**
    * This flag is set to <code>true</code> if AST printing is enabled on the
    * command line.  The AST is the output from the parser.
   **/
  private static boolean printAST = false;
  /**
    * This flag is set to <code>true</code> if semantic analysis output is
    * enabled on the command line.
   **/
  private static boolean printSemantic = false;
  /**
    * This flag is set to <code>true</code> if revision analysis output is
    * enabled on the command line.
   **/
  private static boolean printRevisions = false;
  /**
    * This flag is set to <code>true</code> if the output of parser debugging
    * information is enabled on the command line.
   **/
  private static boolean parserDebug = false;
  /**
    * Set to the granularity at which progress messages should be printed
    * during training.
   **/
  private static int trainingOutput = 0;
  /**
    * This flag is set to <code>true</code> if cleaning has been enabled on
    * the command line.
   **/
  public static boolean clean = false;
  /**
    * This flag is set to <code>true</code> if the user has requested that the
    * source only be compiled to Java.
   **/
  private static boolean compileOnly = false;
  /** This flag is set if concurrent training has been enabled. */
  public static boolean concurrentTraining = false;
  /** This flag is set if warnings have been disabled on the command line. */
  public static boolean warningsDisabled = false;
  /** This flag is set if symbol printing is enabled on the command line. */
  public static boolean printSymbols = false;
  /** The relative path to the LBJ source file. */
  public static String sourceDirectory;
  /** The name of the LBJ2 source file as specified on the command line. */
  public static String sourceFilename;
  /** The source file's name without the <code>.lbj</code> extension. */
  public static String sourceFileBase;
  /**
    * Holds command line arguments to be sent to <code>javac</code> when
    * compiling.
   **/
  public static String javacArguments = "";
  /**
    * A list of names of files generated by the compiler, created as they are
    * generated.
   **/
  public static HashSet fileNames;
  /**
    * The directory in which Javac will place class files (with subdirectories
    * mimicing the package name included).
   **/
  public static String classDirectory;
  /**
    * The directory in which class files should be written, not including the
    * subdirectory structure that mimics the package.
   **/
  public static String classPackageDirectory;
  /** The directory in which to search for source files. */
  public static String classPath = System.getProperty("java.class.path");
  /** The directory in which to search for source files. */
  public static String sourcePath = System.getProperty("java.class.path");
  /**
    * The directory in which to write generated Java source files (with
    * subdirectories mimicing the package name included).
   **/
  public static String generatedSourceDirectory;
  /** The passes that will be executed. */
  private static LinkedList passes;


  /**
    * The main compiler driver.  This method parses command line options and
    * then calls all of LBJ2's components.
    *
    * @param args           The user's command line arguments are found here.
    * @exception Exception  An exception is thrown when any error occurs.
   **/
  public static void main(String[] args) throws Exception {
    AST ast = null;
    try { ast = frontend(ProcessCommandLine(args)); }
    catch (Exception e) {
      if ("version".equals(e.getMessage())) System.exit(0);

      if (e.getMessage() == null
          || !e.getMessage().equals("Incorrect arguments"))
        throw e;
      System.exit(1);
    }

    if (ast == null) return;
      // Happens if --lexerOutput, --parserOutput, or --semanticOutput is
      // enabled.

    fileNames = new HashSet();
    passes = new LinkedList();
    runSemanticAnalysis(ast);

    if (clean) passes.add(new Clean(ast));
    else {
      passes.add(new ClassifierCSE(ast));
      passes.add(new RevisionAnalysis(ast));
      passes.add(new TranslateToJava(ast));
      if (!compileOnly) passes.add(new Train(ast, trainingOutput));
    }

    for (Iterator I = passes.iterator(); I.hasNext() && !Pass.fatalError; ) {
      ((Pass) I.next()).run();
      Pass.printErrorsAndWarnings();
    }

    if (Pass.fatalError) System.exit(1);
  }


  /**
    * Runs the semantic analysis pass on the specified AST, then prints errors
    * and warnings if they exist, and finally sets the
    * {@link #generatedSourceDirectory} and {@link #classDirectory} variables.
    *
    * @param ast  The AST.
   **/
  public static void runSemanticAnalysis(AST ast) {
    new SemanticAnalysis(ast).run();
    Pass.printErrorsAndWarnings();

    if (generatedSourceDirectory != null) {
      if (AST.globalSymbolTable.getPackage().length() != 0)
        generatedSourceDirectory +=
          File.separator + AST.globalSymbolTable.getPackage()
                           .replace('.', File.separatorChar);
    }
    else if (sourceDirectory != null)
      generatedSourceDirectory = sourceDirectory;

    if (classPackageDirectory != null
        && AST.globalSymbolTable.getPackage().length() != 0)
      classDirectory =
        classPackageDirectory + File.separator
        + AST.globalSymbolTable.getPackage().replace('.', File.separatorChar);
    else classDirectory = classPackageDirectory;
  }


  /**
    * Sets all the internal flags that correspond to the specified command
    * line parameters, and checks the command line for errors.
    *
    * @param     args       The user's command line arguments are found here.
    * @exception Exception  An exception is thrown if an error is found.
    * @return A stream for the input source file.
   **/
  private static FileInputStream ProcessCommandLine(String[] args)
      throws Exception {
    if (args.length < 1) {
      PrintUsage();
      throw new Exception("Incorrect arguments");
    }

    boolean printVersion = false;
    int index;
    for (index = 0; index < args.length - 1; ++index) {
      if (args[index].equals("-t")) {
        try {
          trainingOutput = Integer.parseInt(args[++index]);
          if (trainingOutput < 0) throw new Exception();
        }
        catch (Exception e) {
          PrintUsage();
          throw
            new Exception("The -t argument must be followed by a "
                          + "non-negative integer.");
        }
      }
      else if (args[index].equals("-c")) compileOnly = true;
      else if (args[index].equals("-d"))
        classPackageDirectory = args[++index];
      else if (args[index].equals("-j"))
        javacArguments += " " + args[++index];
      else if (args[index].equals("-p")) concurrentTraining = true;
      else if (args[index].equals("-s")) printSymbols = true;
      else if (args[index].equals("-v")) printVersion = true;
      else if (args[index].equals("-w")) warningsDisabled = true;
      else if (args[index].equals("-x")) clean = true;
      else if (args[index].equals("-generatedsourcepath")
               || args[index].equals("-gsp"))
        generatedSourceDirectory = args[++index];
      else if (args[index].equals("-sourcepath")) sourcePath = args[++index];
      else if (args[index].equals("--parserDebug")) parserDebug = true;
      else if (args[index].equals("--lexerOutput")) printTokens = true;
      else if (args[index].equals("--parserOutput")) printAST = true;
      else if (args[index].equals("--revisionOutput")) printRevisions = true;
      else if (args[index].equals("--semanticOutput")) printSemantic = true;
      else {
        PrintUsage();
        throw new Exception("Unrecognized parameter: " + args[index]);
      }
    }

    if (printVersion || args.length == 1 && args[0].equals("-v")) {
      System.out.println("Learning Based Java (LBJ) "
                         + Configuration.packageVersion);
      System.out.println(
          "Copyright (C) 2011, Nicholas D. Rizzolo and Dan Roth.");
      System.out.println("Cognitive Computations Group");
      System.out.println("University of Illinois at Urbana-Champaign");
      System.out.println(Configuration.webSite);
      throw new Exception("version");
    }

    if (javacArguments.indexOf("-d ") != -1
        || javacArguments.indexOf("-sourcepath ") != -1
        || javacArguments.indexOf("-classpath ") != -1
        || javacArguments.indexOf("-cp ") != -1)
      throw new Exception(
          "None of the options '-d', '-sourcepath', or '-classpath' should "
          + "be specified inside LBJ's '-j' option.  Instead, specify '-d' "
          + "and '-sourcepath' directly as options to LBJ, and specify "
          + "-classpath to the JVM when executing LBJ.");

    if (clean
        && (compileOnly || printTokens || printAST || printSemantic
            || trainingOutput != 0)) {
      System.err.println(
          "The -x flag supercedes all other flags except --parserDebug and "
          + "the path related flags.");
      compileOnly = printTokens = printAST = printSemantic = false;
      trainingOutput = 0;
    }

    if (index >= args.length) {
      PrintUsage();
      throw new Exception("Error: No input filename specified.");
    }

    String file = args[index];
    if (!(file.length() > 4 && file.endsWith(".lbj"))) {
      PrintUsage();
      throw new Exception("Source file name must end with \".lbj\".");
    }

    int lastSlash = file.lastIndexOf(File.separatorChar);
    if (lastSlash != -1) {
      sourceDirectory = file.substring(0, lastSlash);
      sourceFilename = file.substring(lastSlash + 1);
    }
    else sourceFilename = file;

    sourceFileBase = sourceFilename.substring(0, sourceFilename.length() - 4);

    FileInputStream instream;
    try { instream = new FileInputStream(file); }
    catch (FileNotFoundException e) {
      System.err.println("Error: Unable to open input file " + file + ": "
                         + e.getMessage());
      throw e;
    }

    return instream;
  }


  /**
    * This method scans and then parses the input.
    *
    * @param in             A stream for the source input file.
    * @exception Exception  Thrown when any error occurs.
    * @return The AST that results from parsing.
   **/
  private static AST frontend(FileInputStream in) throws Exception {
    Yylex scanner = new Yylex(in);
    scanner.sourceFilename = sourceFilename;
    if (printTokens) {
      dumpTokenStream(scanner);
      return null;
    }

    AST ast = null;
    parser LBJ2parser = new parser(scanner);
    if (parserDebug) ast = (AST) LBJ2parser.debug_parse().value;
    else ast = (AST) LBJ2parser.parse().value;

    if (ast == null)
      throw new InternalError("Parser returned null abstract syntax tree.");

    AST result = ast;
    if (printAST) {
      new PrintAST(ast).run();
      result = null;
    }

    if (printSemantic) {
      runSemanticAnalysis(ast);
      System.out.println("\nGlobal symbol table:");
      System.out.println("--------------------");
      ast.symbolTable.print();

      System.out.println("\nDependor graph:");
      System.out.println("--------------------");
      SemanticAnalysis.printDependorGraph();

      System.out.println("\nInvoked graph:");
      System.out.println("--------------------");
      SemanticAnalysis.printInvokedGraph();
      System.out.println();

      result = null;
    }

    if (printSymbols) {
      new DeclarationNames(ast).run();
      result = null;
    }

    if (printRevisions) {
      if (!printSemantic) runSemanticAnalysis(ast);
      new RevisionAnalysis(ast).run();
      System.out.println("\nRevision statuses:");
      System.out.println("--------------------");
      RevisionAnalysis.printRevisionStatus();
      result = null;
    }

    return result;
  }


  /**
   * Dump the token stream produced by the given scanner to standard output.
   * Returns when the end-of-file token is returned from the scanner, or if an
   * exception is thrown by the scanner's next_token() method.
   *
   * <p> Tokens are output as follows: the name of the token (as provided by
   * the array symNames.nameTable[]), followed by a tab, followed by the
   * token's semantic value (see TokenValue.toString()), followed by a tab,
   * followed by the line and the byte offset in the file where the token
   * began (the last two separated by a colon).
   *
   * <p> Error tokens are printed specially; an error message is printed with
   * only the line number listed.
   *
   * @param scanner A reference to the JLex generated scanner object.
   */
  private static void dumpTokenStream(Yylex scanner) {
    Symbol t;
    TokenValue tValue;

    while (true) {
      try { t = scanner.next_token(); }
      catch (IOException e) {
        System.err.println(e);
        return;
      }

      tValue = (TokenValue)t.value;
      switch (t.sym) {
        case sym.EOF: return;
        case sym.error:
          System.out.println("Scanner returned error token at "
                              + tValue.line);
          break;
        default:
          System.out.println(SymbolNames.nameTable[t.sym] + "\t" + tValue
                              + "\t" + (tValue.line + 1) + ":"
                              + tValue.byteOffset);
      }
    }
  }


  /**
    * Print a usage message.  This method is called when the user's command
    * line cannot be interpreted.
   **/
  public static void PrintUsage() {
    System.err.print(
  "Usage: java LBJ2.Main [options] <filename.lbj>\n"
+ "  where [options] is one or more of the following:\n"
+ "    -c               Compile to Java only\n"
+ "    -d <dir>         Write generated class files to <dir>\n"
+ "    -j <a>           Send the specified arguments to javac\n"
//+ "    -p               Train in parallel\n"
+ "    -s               Print the names of all declarations and quit\n"
+ "    -t <n>           Enables default progress output during training\n"
+ "    -v               Print the version number and quit\n"
+ "    -w               Disables the output of warning messages\n"
+ "    -x               Delete all files that would have been generated\n\n"

+ "    -generatedsourcepath <dir>\n"
+ "    -gsp <dir>\n"
+ "                     Write generated Java source files to <dir>\n"
+ "    -sourcepath <path>\n"
+ "                     Search for Java source files in <path>\n\n"

+ "    --parserDebug    Debug output for parse phase only\n"
+ "    --lexerOutput    Print lexical token stream and quit\n"
+ "    --parserOutput   Print the parsed AST and quit\n"
+ "    --revisionOutput Print revision analysis information and quit\n"
+ "    --semanticOutput Print semantic analysis information and quit\n");
  }
}

