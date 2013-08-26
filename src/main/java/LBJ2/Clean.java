package LBJ2;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;
import java.util.LinkedList;

import LBJ2.IR.AST;
import LBJ2.IR.ClassifierName;
import LBJ2.IR.CodedClassifier;
import LBJ2.IR.CompositeGenerator;
import LBJ2.IR.Conjunction;
import LBJ2.IR.ConstraintDeclaration;
import LBJ2.IR.InferenceDeclaration;
import LBJ2.IR.InferenceInvocation;
import LBJ2.IR.LearningClassifierExpression;
import LBJ2.IR.Name;


/**
  * To be run after <code>SemanticAnalysis</code>, this pass compiles the list
  * of files created by the LBJ2 compiler and removes them.
  *
  * @see    LBJ2.SemanticAnalysis
  * @author Nick Rizzolo
 **/
public class Clean extends Pass
{
  /** The list of files to be deleted. */
  private LinkedList files;
  /** The path at which a cleanable Java file will be found. */
  private String javaPath;
  /** The path at which a cleanable class file will be found. */
  private String classPath;


  /**
    * Instantiates a pass that runs on an entire <code>AST</code>.
    *
    * @param ast  The program to run this pass on.
   **/
  public Clean(AST ast) { super(ast); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ast  The node to process.
   **/
  public void run(AST ast) {
    files = new LinkedList();

    if (Main.generatedSourceDirectory != null)
      javaPath = Main.generatedSourceDirectory + File.separator;
    else javaPath = "";

    if (Main.classDirectory != null)
      classPath = Main.classDirectory + File.separator;
    else classPath = "";

    runOnChildren(ast);

    for (Iterator I = files.iterator(); I.hasNext(); ) {
      Object o = I.next();
      if (o instanceof String) {
        String name = (String) o;
        File f = new File(name);
        if (f.exists() && !f.delete())
          reportError(0, "Could not delete '" + name + "'.");
      }
      else {
        String[] names = (String[]) o;
        for (int i = 0; i < names.length; ++i) {
          File f = new File(names[i]);
          if (f.exists() && !f.delete())
            reportError(0, "Could not delete '" + names[i] + "'.");
        }
      }
    }
  }


  /**
    * Adds the default files (<code>*.java</code> and <code>*.class</code>)
    * to the remove list.
    *
    * @param n  The node to add files with respect to.
   **/
  protected void defaultFiles(CodeGenerator n) {
    files.add(javaPath + n.getName() + ".java");
    files.add(classPath + n.getName() + ".class");
  }


  /**
    * Adds files generated for anonymous classes associated with a particular
    * named classifier to the remove list.
    *
    * @param name The name of the classifier with which anonymous classes may
    *             be associated.
   **/
  protected void anonymousFiles(Name name) {
    final String prefix = name + "$";
    String directory = javaPath;
    if (directory.length() == 0) directory = System.getProperty("user.dir");
    else directory = directory.substring(0, directory.length() - 1);

    String[] toAdd =
      new File(directory).list(
        new FilenameFilter() {
          public boolean accept(File directory, String n) {
            return n.startsWith(prefix);
          }
        });

    for (int i = 0; i < toAdd.length; ++i)
      toAdd[i] = directory + File.separator + toAdd[i];
    files.add(toAdd);

    directory = classPath;
    if (directory.length() == 0) directory = System.getProperty("user.dir");
    else directory = directory.substring(0, directory.length() - 1);

    toAdd =
      new File(directory).list(
        new FilenameFilter() {
          public boolean accept(File directory, String n) {
            return n.startsWith(prefix);
          }
        });

    for (int i = 0; i < toAdd.length; ++i)
      toAdd[i] = directory + File.separator + toAdd[i];
    files.add(toAdd);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cn The node to process.
   **/
  public void run(ClassifierName cn) {
    if (cn.name == cn.referent) return;
    defaultFiles(cn);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc) { defaultFiles(cc); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cg The node to process.
   **/
  public void run(CompositeGenerator cg) {
    defaultFiles(cg);
    anonymousFiles(cg.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Conjunction c) {
    defaultFiles(c);
    anonymousFiles(c.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param i  The node to process.
   **/
  public void run(InferenceInvocation i) { defaultFiles(i); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce) {
    defaultFiles(lce);
    anonymousFiles(lce.name);
    files.add(classPath + lce.name + ".lc");
    files.add(javaPath + lce.name + ".ex");
    files.add(javaPath + lce.name + ".lex");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(ConstraintDeclaration c) { defaultFiles(c); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param i  The node to process.
   **/
  public void run(InferenceDeclaration i) {
    defaultFiles(i);
    defaultFiles(i.constraint);
  }
}

