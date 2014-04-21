package LBJ2.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import LBJ2.classify.Classifier;
import LBJ2.learn.Learner;
import LBJ2.parse.Parser;


/**
  * Utility methods for retrieving various classes that are part of the LBJ
  * class hierarchy by name.
  *
  * @author Nick Rizzolo
 **/
public class ClassUtils
{
  /**
    * Retrieves the <code>Class</code> object with the given name.  If there
    * is any exception thrown during retrieval, the program will print an
    * error message to <code>STDERR</code> and terminate via
    * <code>System.exit(1)</code>.
    *
    * @param name The fully qualified name of the class.
    * @return The class with the given name.
   **/
  public static Class getClass(String name) {
    return ClassUtils.getClass(name, true);
  }


  /**
    * Retrieves the <code>Class</code> object with the given name.
    *
    * @param name The fully qualified name of the class.
    * @param exit Whether or not to <code>System.exit(1)</code> on an
    *             exception.
    * @return The class with the given name, or <code>null</code> if an
    *         exception was caught.
   **/
  public static Class getClass(String name, boolean exit) {
    Class clazz = null;

    try { clazz = Class.forName(name); }
    catch (Exception e) {
      if (exit) {
        System.err.println("Can't get class for '" + name + "':");
        e.printStackTrace();
        System.exit(1);
      }
    }

    return clazz;
  }


  /**
    * Retrieve the constructor of the given class with the given parameter
    * types.  If there is any exception thrown during retrieval, the program
    * will print an error message to <code>STDERR</code> and terminate via
    * <code>System.exit(1)</code>.
    *
    * @param name       The fully qualified name of the class.
    * @param paramTypes The <code>Class</code>es representing the types of the
    *                   constructor's parameters.
    * @return The indicated constructor.
   **/
  public static Constructor getConstructor(String name, Class[] paramTypes) {
    return ClassUtils.getConstructor(name, paramTypes, true);
  }


  /**
    * Retrieve the constructor of the given class with the given parameter
    * type names.  If there is any exception thrown during retrieval, the
    * program will print an error message to <code>STDERR</code> and terminate
    * via <code>System.exit(1)</code>.
    *
    * @param name       The fully qualified name of the class.
    * @param paramNames The names of the types of the constructor's
    *                   parameters.
    * @return The indicated constructor.
   **/
  public static Constructor getConstructor(String name, String[] paramNames) {
    return ClassUtils.getConstructor(name, paramNames, true);
  }


  /**
    * Retrieve the constructor of the given class with the given parameter
    * type names.
    *
    * @param name       The fully qualified name of the class.
    * @param paramNames The names of the types of the constructor's
    *                   parameters.
    * @param exit       Whether or not to <code>System.exit(1)</code> on an
    *                   exception.
    * @return The indicated constructor, or <code>null</code> if an exception
    *         was caught.
   **/
  public static Constructor getConstructor(String name, String[] paramNames,
                                           boolean exit) {
    Class[] paramTypes = new Class[paramNames.length];
    for (int i = 0; i < paramNames.length; ++i)
      paramTypes[i] = ClassUtils.getClass(paramNames[i], exit);
    return getConstructor(name, paramTypes, exit);
  }


  /**
    * Retrieve the constructor of the given class with the given parameter
    * types.
    *
    * @param name       The fully qualified name of the class.
    * @param paramTypes The <code>Class</code>es representing the types of the
    *                   constructor's parameters.
    * @param exit       Whether or not to <code>System.exit(1)</code> on an
    *                   exception.
    * @return The indicated constructor, or <code>null</code> if an exception
    *         was caught.
   **/
  public static Constructor getConstructor(String name, Class[] paramTypes,
                                           boolean exit) {
    Class clazz = ClassUtils.getClass(name);
    Constructor constructor = null;

    try { constructor = clazz.getConstructor(paramTypes); }
    catch (Exception e) {
      if (exit) {
        System.err.print("Can't get the constructor with parameters ("
                         + paramTypes[0].getName());
        for (int i = 1; i < paramTypes.length; ++i)
          System.err.print(", " + paramTypes[i].getName());
        System.err.println(") for '" + name + "':");
        e.printStackTrace();
        System.exit(1);
      }
    }

    return constructor;
  }


  /**
    * Retrieve a <code>Classifier</code> by name using the no-argument
    * constructor.  If there is any exception thrown during retrieval, the
    * program will print an error message to <code>STDERR</code> and terminate
    * via <code>System.exit(1)</code>.
    *
    * @param name The fully qualified name of the class.
    * @return An instance of the classifier.
   **/
  public static Classifier getClassifier(String name) {
    return ClassUtils.getClassifier(name, true);
  }


  /**
    * Retrieve a <code>Classifier</code> by name using the no-argument
    * constructor.
    *
    * @param name The fully qualified name of the class.
    * @param exit Whether or not to <code>System.exit(1)</code> on an
    *             exception.
    * @return An instance of the classifier, or <code>null</code> if an
    *         exception was caught.
   **/
  public static Classifier getClassifier(String name, boolean exit) {
    Class clazz = ClassUtils.getClass(name);
    Classifier classifier = null;

    try { classifier = (Classifier) clazz.newInstance(); }
    catch (Exception e) {
      if (exit) {
        System.err.println("Can't instantiate '" + name + "':");
        e.printStackTrace();
        System.exit(1);
      }
    }

    return classifier;
  }


  /**
    * Retrieve a <code>Classifier</code> by name using a constructor with
    * arguments.  If there is any exception thrown during retrieval, the
    * program will print an error message to <code>STDERR</code> and terminate
    * via <code>System.exit(1)</code>.
    *
    * @param name       The fully qualified name of the class.
    * @param paramTypes The <code>Class</code>es representing the types of the
    *                   constructor's parameters.
    * @param arguments  The arguments to send to the constructor.
    * @return An instance of the classifier.
   **/
  public static Classifier getClassifier(String name, Class[] paramTypes,
                                         Object[] arguments) {
    return ClassUtils.getClassifier(name, paramTypes, arguments, true);
  }


  /**
    * Retrieve a <code>Classifier</code> by name using a constructor with
    * arguments.
    *
    * @param name       The fully qualified name of the class.
    * @param paramTypes The <code>Class</code>es representing the types of the
    *                   constructor's parameters.
    * @param arguments  The arguments to send to the constructor.
    * @param exit       Whether or not to <code>System.exit(1)</code> on an
    *                   exception.
    * @return An instance of the classifier, or <code>null</code> if an
    *         exception was caught.
   **/
  public static Classifier getClassifier(String name, Class[] paramTypes,
                                         Object[] arguments, boolean exit) {
    Constructor constructor = ClassUtils.getConstructor(name, paramTypes);
    Classifier classifier = null;

    try { classifier = (Classifier) constructor.newInstance(arguments); }
    catch (InvocationTargetException e) {
      if (exit) {
        Throwable cause = e.getCause();
        System.err.println("Can't instantiate '" + name + "':");
        cause.printStackTrace();
        System.exit(1);
      }
    }
    catch (Exception e) {
      if (exit) {
        System.err.println("Can't instantiate '" + name + "':");
        e.printStackTrace();
        System.exit(1);
      }
    }

    return classifier;
  }


  /**
    * Retrieve a <code>Learner</code> by name using the no-argument
    * constructor.  If there is any exception thrown during retrieval, the
    * program will print an error message to <code>STDERR</code> and terminate
    * via <code>System.exit(1)</code>.
    *
    * @param name The fully qualified name of the class.
    * @return An instance of the learner.
   **/
  public static Learner getLearner(String name) {
    return ClassUtils.getLearner(name, true);
  }


  /**
    * Retrieve a <code>Learner</code> by name using the no-argument
    * constructor.
    *
    * @param name The fully qualified name of the class.
    * @param exit Whether or not to <code>System.exit(1)</code> on an
    *             exception.
    * @return An instance of the learner, or <code>null</code> if an exception
    *         was caught.
   **/
  public static Learner getLearner(String name, boolean exit) {
    Class clazz = ClassUtils.getClass(name);
    Learner learner = null;

    try { learner = (Learner) clazz.newInstance(); }
    catch (Exception e) {
      if (exit) {
        System.err.println("Can't instantiate '" + name + "':");
        e.printStackTrace();
        System.exit(1);
      }
    }

    return learner;
  }


  /**
    * Retrieve a <code>Learner</code> by name using a constructor with
    * arguments.  If there is any exception thrown during retrieval, the
    * program will print an error message to <code>STDERR</code> and terminate
    * via <code>System.exit(1)</code>.
    *
    * @param name       The fully qualified name of the class.
    * @param paramTypes The <code>Class</code>es representing the types of the
    *                   constructor's parameters.
    * @param arguments  The arguments to send to the constructor.
    * @return An instance of the learner.
   **/
  public static Learner getLearner(String name, Class[] paramTypes,
                                   Object[] arguments) {
    return ClassUtils.getLearner(name, paramTypes, arguments, true);
  }


  /**
    * Retrieve a <code>Learner</code> by name using a constructor with
    * arguments.
    *
    * @param name       The fully qualified name of the class.
    * @param paramTypes The <code>Class</code>es representing the types of the
    *                   constructor's parameters.
    * @param arguments  The arguments to send to the constructor.
    * @param exit       Whether or not to <code>System.exit(1)</code> on an
    *                   exception.
    * @return An instance of the learner, or <code>null</code> if an exception
    *         was caught.
   **/
  public static Learner getLearner(String name, Class[] paramTypes,
                                   Object[] arguments, boolean exit) {
    Constructor constructor = ClassUtils.getConstructor(name, paramTypes);
    Learner learner = null;

    try { learner = (Learner) constructor.newInstance(arguments); }
    catch (InvocationTargetException e) {
      if (exit) {
        Throwable cause = e.getCause();
        System.err.println("Can't instantiate '" + name + "':");
        cause.printStackTrace();
        System.exit(1);
      }
    }
    catch (Exception e) {
      if (exit) {
        System.err.println("Can't instantiate '" + name + "':");
        e.printStackTrace();
        System.exit(1);
      }
    }

    return learner;
  }


  /**
    * Retrieve a <code>Parser</code> by name using the no-argument
    * constructor.  If there is any exception thrown during retrieval, the
    * program will print an error message to <code>STDERR</code> and terminate
    * via <code>System.exit(1)</code>.
    *
    * @param name The fully qualified name of the class.
    * @return An instance of the parser.
   **/
  public static Parser getParser(String name) {
    return ClassUtils.getParser(name, true);
  }


  /**
    * Retrieve a <code>Parser</code> by name using the no-argument
    * constructor.
    *
    * @param name The fully qualified name of the class.
    * @param exit Whether or not to <code>System.exit(1)</code> on an
    *             exception.
    * @return An instance of the parser, or <code>null</code> if an exception
    *         was caught.
   **/
  public static Parser getParser(String name, boolean exit) {
    Class clazz = ClassUtils.getClass(name);
    Parser parser = null;

    try { parser = (Parser) clazz.newInstance(); }
    catch (Exception e) {
      if (exit) {
        System.err.println("Can't instantiate '" + name + "':");
        e.printStackTrace();
        System.exit(1);
      }
    }

    return parser;
  }


  /**
    * Retrieve a <code>Parser</code> by name using a constructor with
    * arguments.  If there is any exception thrown during retrieval, the
    * program will print an error message to <code>STDERR</code> and terminate
    * via <code>System.exit(1)</code>.
    *
    * @param name       The fully qualified name of the class.
    * @param paramTypes The <code>Class</code>es representing the types of the
    *                   constructor's parameters.
    * @param arguments  The arguments to send to the constructor.
    * @return An instance of the parser.
   **/
  public static Parser getParser(String name, Class[] paramTypes,
                                 Object[] arguments) {
    return ClassUtils.getParser(name, paramTypes, arguments, true);
  }


  /**
    * Retrieve a <code>Parser</code> by name using a constructor with
    * arguments.
    *
    * @param name       The fully qualified name of the class.
    * @param paramTypes The <code>Class</code>es representing the types of the
    *                   constructor's parameters.
    * @param arguments  The arguments to send to the constructor.
    * @param exit       Whether or not to <code>System.exit(1)</code> on an
    *                   exception.
    * @return An instance of the parser, or <code>null</code> if an exception
    *         was caught.
   **/
  public static Parser getParser(String name, Class[] paramTypes,
                                 Object[] arguments, boolean exit) {
    Constructor constructor = ClassUtils.getConstructor(name, paramTypes);
    Parser parser = null;

    try { parser = (Parser) constructor.newInstance(arguments); }
    catch (InvocationTargetException e) {
      if (exit) {
        Throwable cause = e.getCause();
        System.err.println("Can't instantiate '" + name + "':");
        cause.printStackTrace();
        System.exit(1);
      }
    }
    catch (Exception e) {
      if (exit) {
        System.err.println("Can't instantiate '" + name + "':");
        e.printStackTrace();
        System.exit(1);
      }
    }

    return parser;
  }
}

