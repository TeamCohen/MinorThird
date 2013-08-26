package LBJ2.IR;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import LBJ2.classify.Classifier;
import LBJ2.learn.Learner;
import LBJ2.learn.Normalizer;
import LBJ2.infer.Inference;
import LBJ2.infer.ParameterizedConstraint;


/**
  * A symbol table is simply a <code>HashMap</code> associating names with
  * their types.  This class also assumes responsibility for determining type
  * information for externally defined names.
  *
  * <p> The global symbol table also keeps track of <code>package</code> and
  * <code>import</code> declarations so that when a name cannot be found
  * otherwise, it is searched for in the imported packages.
  *
  * @author Nick Rizzolo
 **/
public class SymbolTable
{
  /**
    * Given the name of a class, which may or may not be fully qualified, this
    * method returns the absolute path to the file with the same name and the
    * given extension.  This method was written for use by the LBJ compiler
    * and its generated code.  The user should not need to call it.
    *
    * @param name       The name of the class.
    * @param extension  The extension of the file to search for.
    * @param path       Paths in which to search for name.extension.
    * @return An object representing the file or <code>null</code> if it can't
    *         be located.
   **/
  protected static File findFile(String name, String extension, String path) {
    String fqName = name.replace('.', File.separatorChar) + "." + extension;
    String[] paths = path.split("\\" + File.pathSeparator);

    for (int i = 0; i < paths.length; ++i) {
      File file = new File(paths[i] + File.separator + fqName);
      if (file.exists()) return file;
    }

    return null;
  }


  /**
    * The parent of this symbol table, or <code>null</code> if there is none.
   **/
  private SymbolTable parent;
  /** The children of this symbol table. */
  private LinkedList children;
  /** Associates variable names with their types. */
  private HashMap table;
  /** Associates externally defined names with their types. */
  private HashMap external;
  /** The name representing the package of this source. */
  private String sourcePackage;
  /** The list of names representing packages that have been imported. */
  private HashSet imported;


  /** Initializes the member variables. */
  public SymbolTable() { this(null); }

  /**
    * Initializes the member variables.
    *
    * @param p  The parent of this table.
   **/
  public SymbolTable(SymbolTable p) {
    parent = p;
    table = new HashMap();
    children = new LinkedList();
    sourcePackage = "";

    if (parent == null) {
      external = new HashMap();
      imported = new HashSet();
    }
    else parent.addChild(this);
  }


  /** Retrieves the parent of this table. */
  public SymbolTable getParent() { return parent; }


  /**
    * Adds a child to this table.
    *
    * @param s  The child to add.
   **/
  public void addChild(SymbolTable s) { children.add(s); }


  /**
    * Adds a new entry to the table.
    *
    * @param a  An argument containing the name its associated type.
    * @return The type previously associated with the given name or
    *         <code>null</code> if no type was previously associated with it.
   **/
  public Type put(Argument a) { return put(a.getName(), a.getType()); }


  /**
    * Adds a new entry to the table.
    *
    * @param name The name to add to the table.
    * @param type The type to associate with the given name.
    * @return The type previously associated with the given name or
    *         <code>null</code> if no type was previously associated with it.
   **/
  public Type put(ClassifierName name, Type type) {
    return put(name.toString(), type);
  }


  /**
    * Adds a new entry to the table.
    *
    * @param name The name to add to the table.
    * @param type The type to associate with the given name.
    * @return The type previously associated with the given name or
    *         <code>null</code> if no type was previously associated with it.
   **/
  public Type put(Name name, Type type) { return put(name.toString(), type); }


  /**
    * Adds a new entry to the table.
    *
    * @param name The name to add to the table.
    * @param type The type to associate with the given name.
    * @return The type previously associated with the given name or
    *         <code>null</code> if no type was previously associated with it.
   **/
  public Type put(String name, Type type) {
    Type result = (Type) table.get(name);
    table.put(name, type);
    return result;
  }


  /**
    * Retrieves the type associated with the given name.  If this table does
    * not contain the name, imported packages are searched.
    *
    * @param name The name to retrieve type information for.
    * @return The type associated with the given name or <code>null</code> if
    *         no type information could be found.
   **/
  public Type get(ClassifierName name) {
    return get(name.referent.toString());
  }


  /**
    * Retrieves the type associated with the given name.  If this table does
    * not contain the name, imported packages are searched.
    *
    * @param name The name to retrieve type information for.
    * @return The type associated with the given name or <code>null</code> if
    *         no type information could be found.
   **/
  public Type get(Name name) { return get(name.toString()); }


  /**
    * Retrieves the type associated with the given name.  If this table does
    * not contain the name, imported packages are searched.
    *
    * @param name The name to retrieve type information for.
    * @return The type associated with the given name or <code>null</code> if
    *         no type information could be found.
   **/
  public Type get(String name) {
    if (localContainsKey(name)) return (Type) table.get(name);
    if (parent != null) return parent.get(name);
    if (external.containsKey(name)) return (Type) external.get(name);

    Type result = null;
    Class c = classForName(name);

    if (c != null) {
      if (ParameterizedConstraint.class.isAssignableFrom(c)) {
        ParameterizedConstraint constraint = null;
        try { constraint = (ParameterizedConstraint) c.newInstance(); }
        catch (Exception e) {
          System.err.println("Can't instantiate parameterized constraint '"
              + c + "'.  Make sure there is a public, no argument "
              + "constructor defined:");
          e.printStackTrace();
          System.exit(1);
        }

        result =
          new ConstraintType(Type.parseType(constraint.getInputType()));
      }
      else if (Classifier.class.isAssignableFrom(c)) {
        Classifier classifier = null;
        try { classifier = (Classifier) c.newInstance(); }
        catch (Exception e) {
          System.err.println("Can't instantiate classifier '" + c
              + "'.  Make sure there is a public, no argument constructor "
              + "defined:");
          e.printStackTrace();
          System.exit(1);
        }

        result =
          new ClassifierType(
              Type.parseType(classifier.getInputType()),
              new ClassifierReturnType(
                classifier.getOutputType(),
                new ConstantList(classifier.allowableValues())),
              Learner.class.isAssignableFrom(c));
      }
      else if (Inference.class.isAssignableFrom(c)) {
        Inference inference = null;
        try { inference = (Inference) c.newInstance(); }
        catch (Exception e) {
          System.err.println("Can't instantiate inference '" + c
              + "'.  Make sure there is a public, no argument constructor "
              + "defined:");
          e.printStackTrace();
          System.exit(1);
        }

        Type headType = new ReferenceType(new Name(inference.getHeadType()));
        String[] headFinderTypeStrings = inference.getHeadFinderTypes();
        Type[] headFinderTypes = new Type[headFinderTypeStrings.length];
        for (int i = 0; i < headFinderTypeStrings.length; ++i)
          headFinderTypes[i] =
            new ReferenceType(new Name(headFinderTypeStrings[i]));

        result = new InferenceType(headType, headFinderTypes);
      }
      else if (Normalizer.class.isAssignableFrom(c)) {
        try { c.newInstance(); }
        catch (Exception e) {
          System.err.println("Can't instantiate normalizer '" + c
              + "'.  Make sure there is a public, no argument constructor "
              + "defined:");
          e.printStackTrace();
          System.exit(1);
        }

        result = new NormalizerType();
      }
    }

    external.put(name, result);

    return result;
  }


  /**
    * Attempts to locate the named class in the current package and any
    * imported packages.  If the corresponding Java source file is found and
    * either the class file does not exist or its time of last modification is
    * earlier than the java file's, it is recompiled.  If no class with the
    * specified name is found, <code>null</code> is returned.
    *
    * @param name The name of the class to search for.
    * @return The <code>Class</code> object representing that class.
   **/
  public Class classForName(ClassifierName name) {
    return classForName(name.referent);
  }


  /**
    * Attempts to locate the named class in the current package and any
    * imported packages.  If the corresponding Java source file is found and
    * either the class file does not exist or its time of last modification is
    * earlier than the java file's, it is recompiled.  If no class with the
    * specified name is found, <code>null</code> is returned.
    *
    * @param name The name of the class to search for.
    * @return The <code>Class</code> object representing that class.
   **/
  public Class classForName(String name) {
    return classForName(new Name(name));
  }


  /**
    * Attempts to locate the named class in the current package and any
    * imported packages.  If the corresponding Java source file is found and
    * either the class file does not exist or its time of last modification is
    * earlier than the java file's, it is recompiled.  If no class with the
    * specified name is found, <code>null</code> is returned.
    *
    * @param name The name of the class to search for.
    * @return The <code>Class</code> object representing that class.
   **/
  public Class classForName(Name name) {
    if (parent != null) return parent.classForName(name);

    Class result = null;

    LinkedList prefixes = new LinkedList();
    prefixes.add("");
    if (sourcePackage.length() != 0) prefixes.add(sourcePackage + ".");
    prefixes.add("java.lang.");

    for (Iterator I = imported.iterator(); I.hasNext(); ) {
      String s = (String) I.next();
      if (s.endsWith(".*")) prefixes.add(s.substring(0, s.length() - 1));
      else if (s.endsWith("." + name.name[0]))
        prefixes.add(s.substring(0, s.length() - name.name[0].length()));
    }

    for (Iterator I = prefixes.iterator(); I.hasNext() && result == null; ) {
      String prefix = (String) I.next();
      String fqName = prefix + name;
      File javaFile = findFile(fqName, "java", LBJ2.Main.sourcePath);

      if (javaFile != null) {
        File classFile = findFile(fqName, "class", LBJ2.Main.classPath);

        if ((classFile == null
             || javaFile.lastModified() > classFile.lastModified())
            && LBJ2.Train.runJavac(javaFile.toString()))
          System.exit(1);
      }

      try { result = Class.forName(fqName); }
      catch (Exception e) { }
      catch (NoClassDefFoundError e) { }

      for (int i = 0; i < name.name.length - 1 && result == null; ++i) {
        fqName = prefix + name.name[0];
        for (int j = 1; j <= i; ++j) fqName += "." + name.name[j];
        javaFile = findFile(fqName, "java", LBJ2.Main.sourcePath);

        if (javaFile != null) {
          File classFile = findFile(fqName, "class", LBJ2.Main.classPath);

          if ((classFile == null
               || javaFile.lastModified() > classFile.lastModified())
              && LBJ2.Train.runJavac(javaFile.toString()))
            System.exit(1);
        }

        for (int j = i + 1; j < name.name.length; ++j)
          fqName += "$" + name.name[j];

        try { result = Class.forName(fqName); }
        catch (Exception e) { }
        catch (NoClassDefFoundError e) { }
      }
    }

    return result;
  }


  /**
    * Determines whether the specified name has been used as a key in this
    * table or any of its parents.
    *
    * @param key  The name.
    * @return <code>true</code> iff <code>key</code> is already a key in this
    *         table or any of its parents.
   **/
  public boolean containsKey(ClassifierName key) {
    return containsKey(key.toString());
  }


  /**
    * Determines whether the specified name has been used as a key in this
    * table or any of its parents.
    *
    * @param key  The name.
    * @return <code>true</code> iff <code>key</code> is already a key in this
    *         table or any of its parents.
   **/
  public boolean containsKey(Name key) { return containsKey(key.toString()); }


  /**
    * Determines whether the specified name has been used as a key in this
    * table or any of its parents.
    *
    * @param key  The name.
    * @return <code>true</code> iff <code>key</code> is already a key in this
    *         table or any of its parents.
   **/
  public boolean containsKey(String key) {
    if (!table.containsKey(key)) {
      if (parent != null) return parent.containsKey(key);
      return false;
    }

    return true;
  }


  /**
    * Determines whether the specified name has been used as a key in this
    * table.
    *
    * @param key  The name.
    * @return <code>true</code> iff <code>key</code> is already a key in this
    *         table.
   **/
  public boolean localContainsKey(ClassifierName key) {
    return localContainsKey(key.toString());
  }


  /**
    * Determines whether the specified name has been used as a key in this
    * table.
    *
    * @param key  The name.
    * @return <code>true</code> iff <code>key</code> is already a key in this
    *         table.
   **/
  public boolean localContainsKey(Name key) {
    return localContainsKey(key.toString());
  }


  /**
    * Determines whether the specified name has been used as a key in this
    * table.
    *
    * @param key  The name.
    * @return <code>true</code> iff <code>key</code> is already a key in this
    *         table.
   **/
  public boolean localContainsKey(String key) {
    return table.containsKey(key);
  }


  /**
    * Adds a name to the list of imported names in the top level table.
    *
    * @param name The name of a new imported package.
   **/
  public void addImported(String name) {
    if (parent == null) imported.add(name);
    else parent.addImported(name);
  }


  /**
    * Returns the size of the list of imported items.
    *
    * @return The size of the list of imported items.
   **/
  public int importedSize() {
    if (parent == null) return imported.size();
    return parent.importedSize();
  }


  /**
    * Sets the package name in the top level table.
    *
    * @param name The package name.
   **/
  public void setPackage(String name) {
    if (parent == null) sourcePackage = name;
    else parent.setPackage(name);
  }


  /**
    * Gets the package name.
    *
    * @return The package name.
   **/
  public String getPackage() {
    if (parent == null) return sourcePackage;
    return parent.getPackage();
  }


  /**
    * Generates <code>package</code> and <code>import</code> statements from
    * the names in the member variable <code>imported</code>.
    *
    * @param out  The stream to write to.
   **/
  public void generateHeader(java.io.PrintStream out) {
    if (parent != null) {
      parent.generateHeader(out);
      return;
    }

    if (sourcePackage.length() != 0)
      out.println("package " + sourcePackage + ";\n");

    String[] names = (String[]) imported.toArray(new String[0]);
    Arrays.sort(names);
    for (int i = 0; i < names.length; ++i)
      out.println("import " + names[i] + ";");
  }


  /** Returns the names of the symbols in this (local) table. */
  public String[] getSymbols() {
    return (String[]) table.keySet().toArray(new String[0]);
  }


  /**
    * Prints this table and all its children recursively to
    * <code>STDOUT</code>.
   **/
  public void print() { print(""); }


  /**
    * Prints this table and all its children recursively to
    * <code>STDOUT</code>.
    *
    * @param indent The level of indentation.
   **/
  public void print(String indent) {
    if (parent == null) {
      if (sourcePackage.length() == 0)
        System.out.println("Package: " + sourcePackage);
      System.out.println("Imported:");
      for (Iterator I = imported.iterator(); I.hasNext(); )
        System.out.println("  " + I.next());
    }

    System.out.println(indent + "Symbols:");
    String[] symbols = (String[]) table.keySet().toArray(new String[0]);
    Arrays.sort(symbols);
    for (int i = 0; i < symbols.length; ++i)
      System.out.println(indent + "  " + symbols[i] + " -> "
                         + table.get(symbols[i]));

    if (children.size() > 0) {
      for (Iterator I = children.iterator(); I.hasNext(); ) {
        System.out.println();
        ((SymbolTable) I.next()).print(indent + "  ");
      }
    }
  }
}

