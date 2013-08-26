package LBJ2.IR;

import LBJ2.Pass;


/**
  * The root node of LBJ2's AST.  The parser will create only one of these and
  * return it as the ultimate result of parsing.  It currently contains a
  * single (optional) <code>PackageDeclaration</code>, a <code>List</code> of
  * <code>ImportDeclaration</code>s (also optional), the global
  * <code>SymbolTable</code>, and a <code>List</code> containing
  * <code>ClassifierAssignment</code>s, <code>ConstraintDeclaration</code>s,
  * and <code>InferenceDeclaration</code>s.
  *
  * <p>
  * The <code>PackageDeclaration</code> specifies what package will contain
  * the generated classes.  The <code>ImportDeclaration</code>s work similarly
  * to <code>import</code> declarations in regular Java code: they allow the
  * user to name classes in other packages without using their full package
  * names.  The list of <code>Declaration</code>s comprises the LBJ2 program.
  * The global <code>SymbolTable</code> simply associates variable names with
  * their type information in the program.
  *
  * @author Nick Rizzolo
 **/
public class AST extends ASTNode
{
  /** The <code>symbolTable</code> variable mirrors this variable. */
  public static final SymbolTable globalSymbolTable = new SymbolTable();


  /**
    * (&oslash;) An optional declaration of the package that generated classes
    * should be a part of.
   **/
  public PackageDeclaration packageDeclaration;
  /**
    * (&not;&oslash;) The list of import statements at the top of the source
    * file.
   **/
  public ImportList imports;
  /**
    * (&not;&oslash;) The list of classifier, constraint, and inference
    * declarations representing the LBJ2 program.
   **/
  public DeclarationList declarations;


  /**
    * Initializes just the statement list.  Line and byte offset information
    * are taken from the statement list.
    *
    * @param d  The declarations comprising the program.
   **/
  public AST(DeclarationList d) {
    this(null, new ImportList(), d, d.line, d.byteOffset);
  }

  /**
    * Initializes both lists.  Line and byte offset information are taken from
    * the <code>import</code> list.
    *
    * @param i  The <code>import</code> declarations.
    * @param d  The declarations comprising the program.
   **/
  public AST(ImportList i, DeclarationList d) {
    this(null, i, d, i.line, i.byteOffset);
  }

  /**
    * Initializes package declaration and statement list.  Line and byte
    * offset information are taken from the <code>package</code> declaration.
    *
    * @param p  The <code>package</code> declaration.
    * @param d  The declarations comprising the program.
   **/
  public AST(PackageDeclaration p, DeclarationList d) {
    this(p, new ImportList(), d, p.line, p.byteOffset);
  }

  /**
    * Initializes all member variables.  Line and byte offset information are
    * taken from the <code>package</code> declaration.
    *
    * @param p  The <code>package</code> declaration.
    * @param i  The <code>import</code> declarations.
    * @param d  The declarations comprising the program.
   **/
  public AST(PackageDeclaration p, ImportList i, DeclarationList d) {
    this(p, i, d, p.line, p.byteOffset);
  }

  /**
    * Full constructor.
    *
    * @param p          The <code>package</code> declaration.
    * @param i          The <code>import</code> declarations.
    * @param d          The declarations comprising the program.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public AST(PackageDeclaration p, ImportList i, DeclarationList d, int line,
             int byteOffset) {
    super(line, byteOffset);
    packageDeclaration = p;
    imports = i;
    declarations = d;
    symbolTable = globalSymbolTable;
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    ASTNodeIterator I =
      new ASTNodeIterator(packageDeclaration == null ? 2 : 3);
    if (packageDeclaration != null) I.children[0] = packageDeclaration;
    I.children[I.children.length - 2] = imports;
    I.children[I.children.length - 1] = declarations;
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    PackageDeclaration p = null;
    if (packageDeclaration != null)
      p = (PackageDeclaration) packageDeclaration.clone();

    return new AST(p, (ImportList) imports.clone(),
                   (DeclarationList) declarations.clone(), -1, -1);
  }


  /**
    * Ensures that the correct <code>run()</code> method is called for this
    * type of node.
    *
    * @param pass The pass whose <code>run()</code> method should be called.
   **/
  public void runPass(Pass pass) { pass.run(this); }


  /**
    * Writes a string representation of this <code>ASTNode</code> to the
    * specified buffer.  The representation written is parsable by the LBJ2
    * compiler, but not very readable.
    *
    * @param buffer The buffer to write to.
   **/
  public void write(StringBuffer buffer) {
    if (packageDeclaration != null) {
      packageDeclaration.write(buffer);
      buffer.append(" ");
    }

    imports.write(buffer);
    buffer.append(" ");
    declarations.write(buffer);
  }
}

