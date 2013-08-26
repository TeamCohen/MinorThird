package LBJ2;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import LBJ2.IR.*;
import LBJ2.classify.Classifier;
import LBJ2.infer.ILPInference;
import LBJ2.infer.Inference;
import LBJ2.learn.Learner;
import LBJ2.learn.Normalizer;
import LBJ2.parse.Parser;


/**
  * The <code>SemanticAnalysis</code> pass builds useful tables, computes
  * classifier types and other useful information, and generally checks that
  * things appear only where they are expected.  More specifically, the
  * following data is arranged:
  *
  * <table cellspacing=8>
  *   <tr valign=top>
  *     <td align=right>A1</td>
  *     <td>
  *       The global symbol table is built.  It stores information about
  *       classifier, constraint, and inference declarations as well as
  *       symbols local to method bodies.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A2</td>
  *     <td>
  *       The classifier representation table is built.  It stores references
  *       to internal representations of source code implementing classifiers
  *       indexed by the classifiers' names.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A3</td>
  *     <td>
  *       Names for every {@link ClassifierExpression} are computed.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A4</td>
  *     <td>
  *       Type information is computed for classifiers,
  *       {@link InstanceCreationExpression}s creating outer classes, and
  *       {@link Name}s known to refer to classifiers, the latter two only to
  *       support the semantic checks performed over the various classifier
  *       specification syntaxes.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A5</td>
  *     <td>
  *       Method invocations that are actually classifier invocations are
  *       marked as such.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A6</td>
  *     <td>
  *       If a <code>sense</code> statement with a single argument appears in
  *       a generator, the argument expression is moved from the
  *       {@link SenseStatement#value value} to the
  *       {@link SenseStatement#name name} variable in the
  *       {@link SenseStatement} object, and the
  *       {@link SenseStatement#value value} variable gets a new
  *       {@link Constant} representing <code>"true"</code> if the generator
  *       is discrete and <code>"1"</code> if the generator is real.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A7</td>
  *     <td>
  *       If there are any <code>for</code>, <code>if</code>,
  *       <code>while</code>, or <code>do</code> statements that contain a
  *       single statement in their body, that statement is wrapped in a
  *       {@link Block}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A8</td>
  *     <td>
  *       The dependor graph, linking the names of {@link CodeGenerator}s with
  *       the names of other {@link CodeGenerator}s that depend on them, is
  *       built for use by {@link RevisionAnalysis}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A9</td>
  *     <td>
  *       The invoked graph, linking the names of {@link CodeGenerator}s with
  *       the names of other {@link CodeGenerator}s that are invoked by them,
  *       is built for use by {@link TranslateToJava}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A10</td>
  *     <td>
  *       If a method of the unique instance of a learning classifier is
  *       invoked using the learning classifier's name, code must be inserted
  *       to create an instance of that classifier ahead of time and then to
  *       call the method on that instance.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A11</td>
  *     <td>
  *       If a {@link LearningClassifierExpression} does not have a
  *       <code>with</code> clause, the default learning algorithm is
  *       substituted.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A12</td>
  *     <td>
  *       Flags are set in each {@link ConstraintEqualityExpression}
  *       indicating if its subexpressions are learner invocations.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A13</td>
  *     <td>
  *       {@link Name}s and every {@link ASTNode} that represents a new local
  *       scope gets a link to the symbol table representing its scope.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A14</td>
  *     <td>
  *       {@link Argument} types in arguments of quantifier expressions are
  *       marked as such.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A15</td>
  *     <td>
  *       Quantified {@link ConstraintEqualityExpression}s,
  *       {@link ConstraintInvocation}s, and
  *       {@link QuantifiedConstraintExpression}s are marked as such.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A16</td>
  *     <td>
  *       If a {@link InferenceDeclaration} does not have a <code>with</code>
  *       clause, the default inference algorithm is substituted.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A17</td>
  *     <td>
  *       When a {@link ClassifierName} is not alone on the right hand side of
  *       a {@link ClassifierAssignment}, its {@link ClassifierName#name name}
  *       is set equal to its {@link ClassifierName#referent referent}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A18</td>
  *     <td>
  *       The {@link ClassifierExpression#cacheIn} member variable is set when
  *       the containing {@link ClassifierAssignment} had a
  *       <code>cached</code> or <code>cachedin</code> modifier.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A19</td>
  *     <td>
  *       The {@link ClassifierExpression#comment} field of each top level
  *       classifier expression is set to the comment of the containing
  *       {@link ClassifierAssignment}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A20</td>
  *     <td>
  *       When a <code>with<code> clause is specified with an
  *       {@link InstanceCreationExpression} as an argument,
  *       {@link LearningClassifierExpression#learnerName} is set to the name
  *       of the class instantiated.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>A21</td>
  *     <td>
  *       The value of {@link ClassifierAssignment#singleExampleCache} is
  *       propagated from {@link ClassifierAssignment}s to
  *       {@link ClassifierExpression}s.
  *     </td>
  *   </tr>
  * </table>
  *
  * <p> And the following conditions are checked for:
  *
  * <table cellspacing=8>
  *   <tr valign=top>
  *     <td align=right>B1</td>
  *     <td>No named classifier is defined more than once.</td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B2</td>
  *     <td>
  *       Classifier and constraint invocations can only contain a single
  *       argument.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B3</td>
  *     <td>
  *       The output type of every classifier expression is checked for
  *       appropriateness in its context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B4</td>
  *     <td>
  *       The input type of a {@link ClassifierName} is checked for
  *       appropriateness in its context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B5</td>
  *     <td>
  *       The {@link InstanceCreationExpression} in the <code>from</code>
  *       clause of a {@link LearningClassifierExpression} instantiates a
  *       {@link Parser}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B6</td>
  *     <td>
  *       The {@link InstanceCreationExpression} in the <code>with</code>
  *       clause of a {@link LearningClassifierExpression} instantiates a
  *       {@link Learner}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B7</td>
  *     <td>
  *       The {@link Learner} specified in a
  *       {@link LearningClassifierExpression} must have input type
  *       assignable from the learning classifier expression's input type.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B8</td>
  *     <td>
  *       Classifiers with feature type <code>discrete</code>,
  *       <code>real</code>, or arrays of those may be invoked as if they were
  *       methods in any context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B9</td>
  *     <td>
  *       Any classifier other than one of feature return type
  *       <code>mixed%</code> may be invoked as a method when that invocation
  *       is the value argument of a <code>sense</code> statement inside a
  *       generator of the same basic type (<code>discrete</code> or
  *       <code>real</code>).  Generators may not be invoked in any other
  *       context.  Array producing classifiers may also be invoked as the
  *       only argument of a <code>sense</code> statement inside another array
  *       producing classifier of the same basic type.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B10</td>
  *     <td>
  *       <code>sense</code> statements may only appear in classifiers that
  *       are generators or that return arrays.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B11</td>
  *     <td>
  *       The <i>expression : expression</i> form of the <code>sense</code>
  *       statement may only appear in a generator.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B12</td>
  *     <td>
  *       <code>return</code> statements may not appear in classifiers that
  *       are generators or that return arrays or in constraints.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B13</td>
  *     <td>
  *       Every {@link ReferenceType} must successfully locate the Java
  *       <code>Class</code> object for the type it refers to.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B14</td>
  *     <td>
  *       The only "mixed" classifier return type is <code>mixed%</code>.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B15</td>
  *     <td>
  *       A {@link CodedClassifier} may not be declared as
  *       <code>mixed%</code>.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B16</td>
  *     <td>
  *       There can be no more than one <code>with</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B17</td>
  *     <td>
  *       There can be no more than one <code>from</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B18</td>
  *     <td>
  *       There must be exactly one <code>using</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B19</td>
  *     <td>
  *       Constraint statements may only appear in constraint declarations.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B20</td>
  *     <td>
  *       Constraint declarations must contain at least one constraint
  *       statement.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B21</td>
  *     <td> Names in classifier expressions must refer to classifiers. </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B22</td>
  *     <td>
  *       The name to the left of the parentheses in an
  *       {@link InferenceInvocation} must refer to an inference.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B23</td>
  *     <td>
  *       The name inside the parentheses of an
  *       {@link InferenceInvocation} must refer to a discrete learner.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B24</td>
  *     <td>
  *       The input type of the classifier inside the parentheses of an
  *       {@link InferenceInvocation} is checked for appropriateness in its
  *       context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B25</td>
  *     <td>
  *       The inference of an {@link InferenceInvocation} must contain an
  *       {@link LBJ2.IR.InferenceDeclaration.HeadFinder} whose input type is
  *       the same as the input type of the {@link InferenceInvocation}'s
  *       argument learner.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B26</td>
  *     <td>
  *       Only constraints can be invoked with the <code>&#64;</code> operator
  *       in a constraint statement.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B27</td>
  *     <td>
  *       The left hand side of the <code>normalizedby</code> operator must be
  *       the name of a {@link Learner}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B28</td>
  *     <td>
  *       The right hand side of the <code>normalizedby</code> operator must
  *       instantiate a {@link LBJ2.learn.Normalizer}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B29</td>
  *     <td>
  *       An {@link InferenceDeclaration} must contain at least one head
  *       finder method.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B30</td>
  *     <td>
  *       An {@link InferenceDeclaration} must contain exactly one
  *       <code>subjectto</code> clause.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B31</td>
  *     <td>
  *       An {@link InferenceDeclaration} may contain no more than one
  *       <code>with</code> clause.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B32</td>
  *     <td>
  *       The {@link InstanceCreationExpression} in the <code>with</code>
  *       clause of an {@link InferenceDeclaration} instantiates a
  *       {@link LBJ2.infer.Inference}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B33</td>
  *     <td>
  *       An inference may not be invoked anywhere other than classifier
  *       expression context.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B34</td>
  *     <td>
  *       Constraint expressions are only allowed to appear as part of their
  *       own separate expression statement.  (The only other place that the
  *       parser will allow them is in the head of a <code>for</code> loop.)
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B35</td>
  *     <td>
  *       The value supplied before the <code>rounds</code> keyword in a
  *       {@link LearningClassifierExpression}'s <code>from</code> clause must
  *       be an integer.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B36</td>
  *     <td>
  *       The <code>cachedin</code> and <code>cached</code> keywords can be
  *       used to cache the value(s) produced by classifiers returning either
  *       a single feature or an array of features in a member variable of a
  *       user's class or a <code>WeakHashMap</code> respectively.  The values
  *       of features produced by generators and conjunctions cannot be cached
  *       in this way.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B37</td>
  *     <td>
  *       There can be no more than one <code>evaluate</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B38</td>
  *     <td>
  *       In the body of a coded classifier, a method invocation with no
  *       parent object is assumed to be a classifier invocation.  As such,
  *       that classifier's definition must be accessible in one form or
  *       another.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B39</td>
  *     <td>
  *       LBJ must be properly <code>configure</code>d to use the selected
  *       inference algorithm.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B40</td>
  *     <td>
  *       The value supplied after the <code>cval</code> keyword in a
  *       {@link LearningClassifierExpression} must be an integer.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B41</td>
  *     <td>
  *       The value supplied after <code>preExtract</code> must be a Boolean
  *       or one of ("none"|"disk"|"diskZip"|"memory"|"memoryZip").
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B42</td>
  *     <td>
  *       The value supplied after <code>progressOutput</code> must be an
  *       integer.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B43</td>
  *     <td>
  *       The value supplied after the <code>alpha</code> keyword in a
  *       {@link LearningClassifierExpression} must be a double.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B44</td>
  *     <td>
  *       The input to any classifier must have either type
  *       {@link ReferenceType} or type {@link ArrayType}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B45</td>
  *     <td>
  *       The <code>alpha</code> keyword should not be used if the
  *       <code>cval</code> keyword is not being used.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B46</td>
  *     <td>
  *       The <code>testingMetric</code> keyword should not be used if both
  *       the <code>cval</code> and <code>testFrom</code> keywords are not
  *       present.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B47</td>
  *     <td>
  *       There can be no more than one <code>cval</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B48</td>
  *     <td>
  *       There can be no more than one <code>testingMetric</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B49</td>
  *     <td>
  *       There can be no more than one <code>alpha</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B50</td>
  *     <td>
  *       The {@link InstanceCreationExpression} in the <code>testFrom</code>
  *       clause of a {@link LearningClassifierExpression} instantiates a
  *       {@link Parser}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B51</td>
  *     <td>
  *       There can be no more than one <code>testFrom</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B52</td>
  *     <td>
  *       Parameter tuning can only be performed if either a <code>cval</code>
  *       clause or <code>testFrom</code> clause is supplied.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B53</td>
  *     <td>
  *       A parameter set must include only simple constant expressions.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B54</td>
  *     <td>
  *       A parameter range must be defined such that the enumerated list
  *       of values is finite.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B55</td>
  *     <td>
  *       A parameter range must be defined with numeric values.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B56</td>
  *     <td>
  *       A parameter set must be defined within a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B57</td>
  *     <td>
  *       There can be no more than one <code>preExtract</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B58</td>
  *     <td>
  *       The <code>prune</code> clause must be of the form
  *       <code>prune a b x</code> where <code>a</code> is one of
  *       ("global"|"perClass"), <code>b</code> is one of
  *       ("count"|"percent"), and <code>x</code> is numeric.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B59</td>
  *     <td>
  *       The prune threshold must be an integer when using the 'count' type
  *       or a real number in [0,1] when using the 'percent' type.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B60</td>
  *     <td>
  *       Feature pre-extraction should not be explicitly be explicitly
  *       enabled when there is no "from" clause (and thus no parser).
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B61</td>
  *     <td>
  *       The conjunction of a classifier with itself is not allowed if the
  *       classifier has return type <code>discrete</code> or
  *       <code>real</code>.
  *     </td>
  *   </tr>
  *   <tr valign=top>
  *     <td align=right>B62</td>
  *     <td>
  *       There can be no more than one <code>encoding</code> clause in a
  *       {@link LearningClassifierExpression}.
  *     </td>
  *   </tr>
  * </table>
  *
  * @see    LBJ2.RevisionAnalysis
  * @see    LBJ2.parse.Parser
  * @see    LBJ2.learn.Learner
  * @see    LBJ2.learn.Normalizer
  * @see    LBJ2.infer.Inference
  * @author Nick Rizzolo
 **/
public class SemanticAnalysis extends Pass
{
  // Static variables.
  /**
    * The keys of this map are the names of {@link CodeGenerator}s; the values
    * are <code>HashSet</code>s of names of other locally defined
    * {@link CodeGenerator}s that depend on the {@link CodeGenerator} named by
    * the associated key.  The dependor graph has an entry for every
    * {@link CodeGenerator} in the source.
   **/
  public static HashMap dependorGraph;
  /**
    * The keys of this map are the names of {@link CodeGenerator}s; the values
    * are <code>HashSet</code>s of names of other (not necessarily locally
    * defined) {@link CodeGenerator}s that are invoked within the
    * {@link CodeGenerator} named by the associated key.  The invoked graph
    * does not necessarily have an entry for every {@link CodeGenerator} in
    * the source.
   **/
  public static HashMap invokedGraph;
  /**
    * The keys of this map are the names of {@link Classifier}s; the values
    * are {@link ASTNode}s representing the source code implementations of the
    * associated {@link Classifier}s.  This table has an entry for every
    * {@link Classifier} in the source.
   **/
  public static HashMap representationTable;


  // Utility methods.
  /** <!-- runAndRestore(AST) -->
    * Running an instance of this pass overwrites the static member variables;
    * use this method to run an instance of this pass and then restore the
    * static member variables to their states before the pass was run.
    *
    * @param ast  An abstract syntax tree to run semantic analysis on.
   **/
  public static void runAndRestore(AST ast) {
    HashMap dg = dependorGraph, ig = invokedGraph, rt = representationTable;
    Pass.canAddErrorsAndWarnings = false;
    new SemanticAnalysis(ast).run();
    Pass.canAddErrorsAndWarnings = true;
    dependorGraph = dg;
    invokedGraph = ig;
    representationTable = rt;
  }


  /** <!-- addDependor(String,String) -->
    * Adds an edge from dependency to dependor in the {@link #dependorGraph}.
    * If the dependor is <code>null</code>, no new list item is added, but the
    * <code>HashSet</code> associated with the dependency is still created if
    * it didn't already exist.
    *
    * @param dependency The name of the node depended on.
    * @param dependor   The name of the node doing the depending.
   **/
  public static void addDependor(String dependency, String dependor) {
    HashSet dependors = (HashSet) dependorGraph.get(dependency);

    if (dependors == null) {
      dependors = new HashSet();
      dependorGraph.put(dependency, dependors);
    }

    if (dependor != null) dependors.add(dependor);
  }


  /** <!-- isDependentOn(String,String) -->
    * Use this method to determine if one {@link CodeGenerator} depends on
    * another either directly or indirectly.
    *
    * @param c1 One {@link CodeGenerator}.
    * @param c2 The other {@link CodeGenerator}.
    * @return <code>true</code> iff <code>c1</code> depends on
    *         <code>c2</code>.
   **/
  public static boolean isDependentOn(String c1, String c2) {
    LinkedList queue = new LinkedList();
    queue.add(c2);

    HashSet visited = new HashSet();

    while (queue.size() > 0) {
      String c = (String) queue.removeFirst();
      if (c.equals(c1)) return true;

      visited.add(c);
      for (Iterator I = ((HashSet) dependorGraph.get(c)).iterator();
           I.hasNext(); ) {
        c = (String) I.next();
        if (!visited.contains(c)) queue.add(c);
      }
    }

    return false;
  }


  /** <!-- addInvokee(String,String) -->
    * Adds an edge from invoker to invokee in the {@link #invokedGraph}.
    *
    * @param invoker  The name of the node doing the invoking.
    * @param invokee  The name of the invoked node.
   **/
  private static void addInvokee(String invoker, String invokee) {
    HashSet invokees = (HashSet) invokedGraph.get(invoker);

    if (invokees == null) {
      invokees = new HashSet();
      invokedGraph.put(invoker, invokees);
    }

    invokees.add(invokee);
  }


  /** <!-- printDependorGraph() -->
    * Prints the contents of {@link #dependorGraph} to <code>STDOUT</code> in
    * a readable form.
   **/
  public static void printDependorGraph() { printGraph(dependorGraph); }


  /** <!-- printInvokedGraph() -->
    * Prints the contents of {@link #invokedGraph} to <code>STDOUT</code> in a
    * readable form.
   **/
  public static void printInvokedGraph() { printGraph(invokedGraph); }


  /** <!-- printGraph(HashMap) -->
    * Prints the contents of the specified graph to <code>STDOUT</code> in a
    * readable form.
    *
    * @param graph  The graph to print as a map of collections.
   **/
  private static void printGraph(HashMap graph) {
    String[] keys = (String[]) graph.keySet().toArray(new String[0]);
    Arrays.sort(keys);
    for (int i = 0; i < keys.length; ++i) {
      System.out.print(keys[i] + " ->");
      String[] edges =
        (String[]) ((Collection) graph.get(keys[i])).toArray(new String[0]);
      for (int j = 0; j < edges.length; ++j)
        System.out.print(" " + edges[j]);
      System.out.println();
    }
  }


  /** <!-- isAssignableFrom(Class,Class) -->
    * Calls the <code>Class#isAssignableFrom(Class)</code> method after making
    * sure that both classes involved aren't null.  The assumption made when
    * calling this method is that if either argument class is
    * <code>null</code>, an error has already been generated with respect to
    * it.
    *
    * @param c1 Class 1.
    * @param c2 Class 2.
    * @return <code>true</code> iff either class is null or c1 is assignable
    *         from c2.
   **/
  private static boolean isAssignableFrom(Class c1, Class c2) {
    return c1 == null || c2 == null || c1.isAssignableFrom(c2);
  }


  /** <!-- wekaIze(int,ClassifierReturnType,Name) -->
    * Called when analyzing the feature types for use by a WEKA classifier.
    * Writes the necessary attribute information from a
    * <code>ClassifierReturnType</code> to <code>lce.attributeString</code>.
    *
    * <p> <code>lce.attributeString</code> takes the form of a colon-separated
    * list of attribute specifications, each of which are formated in the
    * following way:
    * "<code>type</code>_<code>name</code>(_<code>value-list</code>)".
    *
    * <p> <code>value-list</code> takes the same format as it would in an lbj
    * source file.  i.e. <code>{"value1","value2",...}</code>
    *
    * <p> <code>type</code> can take the values <code>str</code> (string
    * attributes), <code>nom</code> (nominal attributes), or <code>num</code>
    * (numerical attributes).
    *
    * <p> The first attribute in this string is, by convention, considered to
    * be the class attribute.
   **/
  public void wekaIze(int line, ClassifierReturnType RT, Name name) {
    String typeName = RT.getTypeName();
    if (!typeName.equals("discrete") && !typeName.equals("real"))
      reportError(line, "Classifiers with return type " + typeName
                        + " are not usable with WEKA learning algorithms");

    // String attribute case
    if (typeName.equals("discrete")) {
      if (RT.values.size() == 0) {
        lceInQuestion.attributeString.append("str_");
        lceInQuestion.attributeString.append(name.toString());
        lceInQuestion.attributeString.append(':');
      }
      // Nominal attribute case
      else {
        lceInQuestion.attributeString.append("nom_");
        lceInQuestion.attributeString.append(name);
        lceInQuestion.attributeString.append('_');

        Constant[] constantList = RT.values.toArray();

        for (int i = 0; i < constantList.length; ++i) {
          String value = constantList[i].value;

          if (value.length() > 1 && value.charAt(0) == '"'
              && value.charAt(value.length() - 1) == '"')
            value = value.substring(1, value.length() - 1);

          lceInQuestion.attributeString.append(value);
          lceInQuestion.attributeString.append(',');
        }

        lceInQuestion.attributeString
          .deleteCharAt(lceInQuestion.attributeString.length() - 1);
        lceInQuestion.attributeString.append(':');
      }
    }
    // Numerical attribute case
    else {
      lceInQuestion.attributeString.append("num_");
      lceInQuestion.attributeString.append(name);
      lceInQuestion.attributeString.append(':');
    }
  }


  /** <!-- anonymousClassifier(String) -->
    * Creates a new anonymous classifier name.
    *
    * @param lastName The last part of the classifier's name as determined by
    *                 its parent's name.
    * @return The created name.
   **/
  public Name anonymousClassifier(String lastName) {  // A3
    int index = lastName.indexOf('$');
    if (lastName.indexOf('$', index + 1) >= 0) return new Name(lastName);
    return
      new Name(lastName.substring(0, index) + "$"
               + lastName.substring(index));
  }


  // Member variables.
  /**
    * Lets AST children know about the code producing node they are contained
    * in.
   **/
  private CodeGenerator currentCG;
  /**
    * Lets AST children know the return type of the
    * {@link ClassifierAssignment} they are contained in.
   **/
  private ClassifierReturnType currentRT;
  /**
    * Used when analyzing constraint declarations to determine if a constraint
    * statement appears within them.
   **/
  private boolean containsConstraintStatement;
  /** Lets all nodes know what symbol table represents their scope. */
  private SymbolTable currentSymbolTable;
  /**
    * Lets AST nodes know how deeply nested inside
    * {@link QuantifiedConstraintExpression}s they are.
   **/
  private int quantifierNesting;
  /**
    * A flag which indicates whether or not the compiler is in the process of
    * gathering attribute information for a WEKA learning algorithm.
   **/
  private boolean attributeAnalysis = false;
  /**
    * A reference to the <code>LearningClassifierExpression</code> which is
    * currently under analysis.
   **/
  private LearningClassifierExpression lceInQuestion;


  /** Default constructor. */
  public SemanticAnalysis() { }

  /**
    * Instantiates a pass that runs on an entire {@link AST}.
    *
    * @param ast  The program to run this pass on.
   **/
  public SemanticAnalysis(AST ast) { super(ast); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ast The node to process.
   **/
  public void run(AST ast) {
    currentSymbolTable = ast.symbolTable;

    if (ast.symbolTable.importedSize() == 0) {  // A1
      ast.symbolTable.addImported("LBJ2.classify.*");
      ast.symbolTable.addImported("LBJ2.learn.*");
      ast.symbolTable.addImported("LBJ2.parse.*");
      ast.symbolTable.addImported("LBJ2.infer.*");
      if (LBJ2.Configuration.GLPKLinked)
        ast.symbolTable.addImported("LBJ2.jni.*");
    }

    dependorGraph = new HashMap();
    invokedGraph = new HashMap();
    representationTable = new HashMap();
    quantifierNesting = 0;

    runOnChildren(ast);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param decl The node to process.
   **/
  public void run(PackageDeclaration decl) {
    ast.symbolTable.setPackage(decl.name.toString()); // A1
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param decl The node to process.
   **/
  public void run(ImportDeclaration decl) {
    ast.symbolTable.addImported(decl.name.toString());  // A1
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param list The node to process.
   **/
  public void run(DeclarationList list) {
    if (list.size() == 0) return;

    for (DeclarationList.DeclarationListIterator I = list.listIterator();
         I.hasNext(); ) {
      Declaration d = I.nextItem();
      if (ast.symbolTable.containsKey(d.name))  // B1
        reportError(d.line,
                    "A declaration named '" + d.name + "' already exists.");
      ast.symbolTable.put(d.name, d.getType()); // A1
    }

    currentCG = null;
    runOnChildren(list);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ca The node to process.
   **/
  public void run(ClassifierAssignment ca) {
    LBJ2.IR.Type inputType = ca.argument.getType();
    if (!(inputType instanceof ReferenceType  // B44
          || inputType instanceof ArrayType))
      reportError(ca.line,
          "The input to a classifier must be a single object reference.");

    ca.expression.name = (Name) ca.name.clone();  // A3

    ca.expression.returnType = (ClassifierReturnType) ca.returnType.clone();
      // B3
    ca.expression.argument = (Argument) ca.argument.clone();  // A4

    ca.expression.singleExampleCache = ca.singleExampleCache; // A21

    if (ca.cacheIn != null) {
      // B36
      if (ca.returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
          || ca.returnType.type == ClassifierReturnType.REAL_GENERATOR
          || ca.returnType.type == ClassifierReturnType.MIXED_GENERATOR)
        reportError(ca.line,
                    "Generators' outputs cannot be cached (in a member "
                    + "variable or otherwise).");
      if (ca.expression instanceof Conjunction)
        reportError(ca.line,
                    "Conjunctive classifiers' outputs cannot be cached (in a "
                    + "member variable or otherwise).");

      ca.expression.setCacheIn(ca.cacheIn); // A18
    }

    currentRT = (ClassifierReturnType) ca.returnType.clone(); // A4
    currentSymbolTable = ca.symbolTable = new SymbolTable(currentSymbolTable);
      // A13
    runOnChildren(ca);
    currentSymbolTable = currentSymbolTable.getParent();
    ca.expression.returnType = (ClassifierReturnType) ca.returnType.clone();

    ca.expression.comment = ca.comment; // A19
    representationTable.put(ca.name.toString(), ca.expression); // A2
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cce  The node to process.
   **/
  public void run(ClassifierCastExpression cce) {
    if (!cce.castType.isContainableIn(cce.returnType))  // B3
      reportError(cce.line,
          "Found classifier expression of return type '" + cce.castType
          + "' when '" + cce.returnType + "' was expected.");

    cce.expression.name = (Name) cce.name.clone();  // A3

    cce.expression.returnType = (ClassifierReturnType) cce.castType.clone();
      // B3
    cce.expression.argument = (Argument) cce.argument.clone();  // A4

    cce.expression.singleExampleCache = cce.singleExampleCache; // A21

    ClassifierReturnType saveRT = currentRT;
    currentRT = (ClassifierReturnType) cce.castType.clone();  // A4
    boolean saveAttributeAnalysis = attributeAnalysis;
    attributeAnalysis = false;

    runOnChildren(cce);

    attributeAnalysis = saveAttributeAnalysis;
    currentRT = saveRT;

    representationTable.put(cce.name.toString(), cce);  // A2
    cce.expression.returnType = (ClassifierReturnType) cce.castType.clone();

    if (attributeAnalysis) wekaIze(cce.line, cce.returnType, cce.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cn The node to process.
   **/
  public void run(ClassifierName cn) {
    if (cn.name.toString().indexOf("$$") != -1) // A3 A17
      cn.name = cn.referent;
    else addDependor(cn.name.toString(), null); // A8

    LBJ2.IR.Type t = ast.symbolTable.get(cn);
    if (!(t instanceof ClassifierType)) { // B21
      reportError(cn.line, "'" + cn + "' is not known to be a classifier.");
      cn.returnType = null;
      return;
    }

    ClassifierType type = (ClassifierType) t;

    LBJ2.IR.Type input = type.getInput();
    if (!isAssignableFrom(input.typeClass(),
                          cn.argument.getType().typeClass()))  // B4
      reportError(cn.line,
          "Classifier '" + cn + "' has input type '" + input + "' when '"
          + cn.argument.getType() + "' was expected.");

    ClassifierReturnType output = type.getOutput();
    if (!output.isContainableIn(cn.returnType)) // B3
      reportError(cn.line,
          "Classifier '" + cn + "' has return type '" + output + "' when '"
          + cn.returnType + "' was expected.");
    else cn.returnType = output;  // A4

    if (attributeAnalysis) wekaIze(cn.line, cn.returnType, cn.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cc The node to process.
   **/
  public void run(CodedClassifier cc) {
    addDependor(cc.name.toString(), null);  // A8

    cc.returnType = (ClassifierReturnType) currentRT.clone(); // A4

    if (cc.returnType.type == ClassifierReturnType.MIXED_GENERATOR) // B15
      reportError(cc.line,
          "A coded classifier may not have return type 'mixed%'.");

    // A13
    currentSymbolTable = cc.symbolTable = cc.body.symbolTable =
      new SymbolTable(currentSymbolTable);

    CodeGenerator saveCG = currentCG;
    currentCG = cc;
    run(cc.argument); // A1
    runOnChildren(cc);
    currentCG = saveCG;

    representationTable.put(cc.name.toString(), cc);  // A2
    currentSymbolTable = currentSymbolTable.getParent();

    if (attributeAnalysis) wekaIze(cc.line, cc.returnType, cc.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param cg The node to process.
   **/
  public void run(CompositeGenerator cg) {
    addDependor(cg.name.toString(), null);  // A8

    int i = 0;
    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); ) {
      ClassifierExpression e = I.nextItem();

      e.name = anonymousClassifier(cg.name + "$" + i++);  // A3
      e.returnType =
        new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
      e.argument = (Argument) cg.argument.clone();  // A4
      e.singleExampleCache = cg.singleExampleCache; // A21

      e.runPass(this);

      addDependor(e.name.toString(), cg.name.toString()); // A8
    }

    String cgReturnType = null;
    ConstantList values = null;
    for (ClassifierExpressionList.ClassifierExpressionListIterator I =
           cg.components.listIterator();
         I.hasNext(); ) { // A4
      ClassifierExpression component = I.nextItem();

      if (component.returnType == null) return;
      String componentReturnType = component.returnType.toString();
      if (cgReturnType == null) {
        cgReturnType = componentReturnType;
        values = component.returnType.values;
      }
      else {
        if (cgReturnType.startsWith("discrete")
              && !componentReturnType.startsWith("discrete")
            || cgReturnType.startsWith("real")
               && !componentReturnType.startsWith("real"))
          cgReturnType = "mixed";
        if (values.size() > 0 && !values.equals(component.returnType.values))
          values = new ConstantList();
      }
    }

    assert cgReturnType != null : "Empty component list";

    // A4
    ClassifierReturnType output = null;
    if (cgReturnType.startsWith("discrete"))
      output =
        new ClassifierReturnType(ClassifierReturnType.DISCRETE_GENERATOR,
                                 values);
    else if (cgReturnType.startsWith("real"))
      output = new ClassifierReturnType(ClassifierReturnType.REAL_GENERATOR);
    else
      output = new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR);

    if (!output.isContainableIn(cg.returnType)) // B3
      reportError(cg.line,
          "Found a classifier expression of return type '" + output
          + "' when '" + cg.returnType + "' was expected.");
    else cg.returnType = output;

    representationTable.put(cg.name.toString(), cg);  // A2
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Conjunction c) {
    addDependor(c.name.toString(), null); // A8

    c.left.name = anonymousClassifier(c.name + "$0"); // A3
    c.left.returnType =
      new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
    c.left.argument = (Argument) c.argument.clone();  // A4
    c.left.singleExampleCache = c.singleExampleCache; // A21

    c.right.name = anonymousClassifier(c.name + "$1");  // A3
    c.right.returnType =
      new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
    c.right.argument = (Argument) c.argument.clone(); // A4
    c.right.singleExampleCache = c.singleExampleCache; // A21

    boolean saveAttributeAnalysis = attributeAnalysis;
    attributeAnalysis = false;

    runOnChildren(c);

    attributeAnalysis = saveAttributeAnalysis;

    if (c.left.returnType == null || c.right.returnType == null) return;

    addDependor(c.left.name.toString(), c.name.toString()); // A8
    addDependor(c.right.name.toString(), c.name.toString());  // A8

    // A4
    LBJ2.IR.Type inputType = c.right.argument.getType();
    Class inputRight = inputType.typeClass();
    LBJ2.IR.Type leftType = c.left.argument.getType();
    Class inputLeft = leftType.typeClass();
    if (!isAssignableFrom(inputLeft, inputRight)) inputType = leftType;

    c.argument =
      new Argument(inputType, c.argument.getName(), c.argument.getFinal());

    ConstantList valuesLeft = c.left.returnType.values;
    ConstantList valuesRight = c.right.returnType.values;
    ConstantList values = new ConstantList();
    if (valuesLeft.size() > 0 && valuesRight.size() > 0)
      for (ConstantList.ConstantListIterator I = valuesLeft.listIterator();
           I.hasNext(); ) {
        Constant valueLeft = I.nextItem();
        for (ConstantList.ConstantListIterator J = valuesRight.listIterator();
             J.hasNext(); )
          values.add(
              new Constant(valueLeft.noQuotes() + "&"
                           + J.nextItem().noQuotes()));
      }

    int rt1 = c.left.returnType.type;
    int rt2 = c.right.returnType.type;
    if (rt2 < rt1) {
      int temp = rt1;
      rt1 = rt2;
      rt2 = temp;
    }

    ClassifierReturnType output = null;
    switch (10 * rt1 + rt2) {
      case 0:
        output =
          new ClassifierReturnType(ClassifierReturnType.DISCRETE, values);
        break;

      case 11:
        output = new ClassifierReturnType(ClassifierReturnType.REAL);
        break;

      case 3: case 33:
        output =
          new ClassifierReturnType(ClassifierReturnType.DISCRETE_ARRAY,
                                   values);
        break;

      case 14: case 44:
        output = new ClassifierReturnType(ClassifierReturnType.REAL_ARRAY);
        break;

      case 6: case 36: case 66:
        output =
          new ClassifierReturnType(ClassifierReturnType.DISCRETE_GENERATOR,
                                   values);
        break;

      case 1: case 4: case 7: case 13: case 16: case 17: case 34: case 37:
      case 46: case 47: case 67: case 77:
        output =
          new ClassifierReturnType(ClassifierReturnType.REAL_GENERATOR);
        break;

      case 8: case 18: case 38: case 48: case 68: case 78: case 88:
        output =
          new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR);
        break;
    }

    assert output != null
           : "Unexpected conjunction types: "
             + ClassifierReturnType.typeName(rt1) + ", "
             + ClassifierReturnType.typeName(rt2);

    if (!output.isContainableIn(c.returnType))  // B3
      reportError(c.line,
          "Found a classifier expression of return type '" + output
          + "' when '" + c.returnType + "' was expected.");
    else if ((output.type == ClassifierReturnType.DISCRETE
              || output.type == ClassifierReturnType.REAL)
             && c.left.equals(c.right)) // B61
      reportError(c.line,
          "A classifier cannot be conjuncted with itself unless it returns "
          + "multiple features.");
    else c.returnType = output;

    representationTable.put(c.name.toString(), c);  // A2

    if (attributeAnalysis) wekaIze(c.line, c.returnType, c.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ii  The node to process.
   **/
  public void run(InferenceInvocation ii) {
    // A8
    addDependor(ii.name.toString(), null);
    addDependor(ii.inference.toString(), ii.name.toString());
    addDependor(ii.classifier.toString(), ii.name.toString());

    runOnChildren(ii);

    if (!(ii.inference.typeCache instanceof InferenceType)) { // B22
      reportError(ii.inference.line,
          "'" + ii.inference + "' is not known to be a inference.");
      return;
    }

    if (!(ii.classifier.typeCache instanceof ClassifierType)) { // B23
      reportError(ii.classifier.line,
          "'" + ii.classifier + "' is not known to be a learner.");
      return;
    }

    ClassifierType argumentType = (ClassifierType) ii.classifier.typeCache;
    ClassifierReturnType output = argumentType.getOutput();
    if (output.type != ClassifierReturnType.DISCRETE
        || !argumentType.isLearner()) // B23
      reportError(ii.classifier.line,
          "'" + ii.classifier + "' is not a discrete learner.");

    LBJ2.IR.Type input = argumentType.getInput();
    if (!isAssignableFrom(input.typeClass(),
                          ii.argument.getType().typeClass()))  // B24
      reportError(ii.line,
          "Classifier '" + ii + "' has input type '" + input + "' when '"
          + ii.argument.getType() + "' was expected.");

    if (!output.isContainableIn(ii.returnType)) // B3
      reportError(ii.line,
          "Classifier '" + ii + "' has return type '" + output + "' when '"
          + ii.returnType + "' was expected.");
    else ii.returnType = output; // A4

    InferenceType type = (InferenceType) ii.inference.typeCache;
    boolean found = false;
    for (int i = 0; i < type.getFindersLength() && !found; ++i)
      found = type.getFinderType(i).equals(input);

    if (!found) // B25
      reportError(ii.line,
          "Inference '" + ii.inference + "' does not contain a head finder "
          + " method for class '" + input + "'.");

    representationTable.put(ii.name.toString(), ii);  // A2

    if (attributeAnalysis) wekaIze(ii.line, ii.returnType, ii.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param lce  The node to process.
   **/
  public void run(LearningClassifierExpression lce) {
    String lceName = lce.name.toString();
    addDependor(lceName, null); // A8

    // Setting up signatures for labeler and extractor.
    int i = 0;
    if (lce.labeler != null) {
      lce.labeler.name = anonymousClassifier(lceName + "$" + i++); // A3
      lce.labeler.returnType =
        new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
      lce.labeler.argument = (Argument) lce.argument.clone(); // A4
      lce.labeler.singleExampleCache = lce.singleExampleCache; // A21
    }

    lce.extractor.name = anonymousClassifier(lceName + "$" + i); // A3
    lce.extractor.returnType =
      new ClassifierReturnType(ClassifierReturnType.MIXED_GENERATOR); // B3
    lce.extractor.argument = (Argument) lce.argument.clone(); // A4
    lce.extractor.singleExampleCache = lce.singleExampleCache; // A21

    // Making sure an appropriate quantity of each clause is present.
    if (lce.usingClauses != 1) {  // B18
      reportError(lce.line,
          "A learning classifier expression must contain exactly one 'using' "
          + "clause.");
      return;
    }

    if (lce.fromClauses > 1) {  // B17
      reportError(lce.line,
          "A learning classifier expression can have no more than one 'from' "
          + "clause.");
      return;
    }

    if (lce.withClauses > 1) {  // B16
      reportError(lce.line,
          "A learning classifier expression can have no more than one 'with' "
          + "clause.");
      return;
    }

    if (lce.encodingClauses > 1) {  // B62
      reportError(lce.line,
          "A learning classifier expression can have no more than one "
          + "'encoding' clause.");
      return;
    }

    if (lce.testFromClauses > 1) {  // B51
      reportError(lce.line,
          "A learning classifier expression can have no more than one "
          + "'testFrom' clause.");
      return;
    }

    if (lce.evaluateClauses > 1) {  // B37
      reportError(lce.line,
          "A learning classifier expression can have no more than one "
          + "'evaluate' clause.");
    }

    if (lce.cvalClauses == 0) {
      if (lce.alphaClauses > 0) { // B45
        reportError(lce.line,
            "The alpha keyword is meaningful only if the cval keyword is "
            + "also being used, and should not be used otherwise.");
      }

      if (lce.testFromClauses == 0 && lce.testingMetric != null) {  // B46
        reportError(lce.testingMetric.line,
            "The 'testingMetric' keyword is meaningful only if one of 'cval' "
            + "or 'testFrom' is also present, and should not be used "
            + "otherwise.");
      }
    }

    if (lce.cvalClauses > 1) {  // B47
      reportError(lce.line,
          "A learning classifier expression can have no more than one 'cval'"
          + " clause.");
    }

    if (lce.testingMetricClauses > 1) { // B48
      reportError(lce.line,
          "A learning classifier expression can have no more than one "
          + "'testingMetric' clause.");
    }

    if (lce.alphaClauses > 1) { // B49
      reportError(lce.line,
          "A learning classifier expression can have no more than one 'alpha'"
          + " clause.");
    }

    if (lce.preExtractClauses > 1) {  // B57
      reportError(lce.line,
          "A learning classifier expression can have no more than one "
          + "'preExtract' clause.");
    }

    if (lce.evaluation != null && lce.evaluation instanceof MethodInvocation)
      ((MethodInvocation) lce.evaluation).isEvaluateArgument = true;
    if (lce.rounds != null && lce.rounds instanceof ParameterSet)
      ((ParameterSet) lce.rounds).inRounds = true;

    // Make sure we have a learning algorithm.
    if (lce.learnerName == null) {
      if (lce.learnerConstructor == null) { // A11
        if (lce.returnType.toString().charAt(0) == 'd')
          lce.learnerConstructor =
            LearningClassifierExpression.defaultDiscreteLearner;
        else
          lce.learnerConstructor =
            LearningClassifierExpression.defaultRealLearner;
        //lce.learnerConstructor.runPass(this);
      }

      lce.learnerName = lce.learnerConstructor.name;  // A20
    }

    //lce.learnerName.runPass(this);

    // Make sure third party learning algorithms are linked.
    boolean liblinear = false;
    liblinear =
      lce.learnerName.equals(new Name("SupportVectorMachine"))
      || lce.learnerName.equals(new Name("LBJ2.learn.SupportVectorMachine"));
    if (liblinear && !LBJ2.Configuration.LiblinearLinked)
      reportError(lce.learnerName.line,
          "LBJ has not been configured properly to use the liblinear library."
          + "  See the Users's Manual for more details.");

    boolean weka = false;
    weka = lce.learnerName.equals(new Name("WekaWrapper"))
           || lce.learnerName.equals(new Name("LBJ2.learn.WekaWrapper"));
    if (weka && !LBJ2.Configuration.WekaLinked) {
      reportError(lce.learnerName.line,
          "LBJ has not been configured properly to use the WEKA library.  "
          + "See the Users's Manual for more details.");
    }

    boolean saveAttributeAnalysis = attributeAnalysis;
    LearningClassifierExpression saveLCE = null;

    // Weka specific pre-processing.
    if (weka) {
      attributeAnalysis = true;
      lce.attributeString = new StringBuffer();

      // Identify which learning classifier expression we are gathering
      // feature information for.
      saveLCE = lceInQuestion;
      lceInQuestion = lce;
    }

    CodeGenerator saveCG = currentCG;
    currentCG = lce;
    runOnChildren(lce);
    currentCG = saveCG;

    // Weka specific post-processing.
    if (weka) {
      attributeAnalysis = saveAttributeAnalysis;
      lceInQuestion = saveLCE;
      if (lce.attributeString.length() != 0)
        lce.attributeString.deleteCharAt(lce.attributeString.length() - 1);

      if (lce.learnerParameterBlock != null)
        lce.learnerParameterBlock.statementList().add(
            new ExpressionStatement(
                new Assignment(
                    new Operator(Operator.ASSIGN),
                    new Name("attributeString"),
                    new Constant('"' + lce.attributeString.toString()
                                 + '"'))));
    }

    if (lce.labeler != null)
      addDependor(lce.labeler.name.toString(), lceName);  // A8
    addDependor(lce.extractor.name.toString(), lceName);  // A8

    // Check the "rounds" clause (if any) for semantic errors.
    if (lce.rounds != null) {
      if (lce.rounds instanceof Constant) {
        try { Integer.parseInt(((Constant) lce.rounds).value); }
        catch (Exception e) { // B35
          reportError(lce.rounds.line,
              "The value supplied before 'rounds' must be an integer.");
        }
      }
      else if (!(lce.rounds instanceof ParameterSet)) {
        reportError(lce.rounds.line,
            "The value supplied before 'rounds' must be an integer.");
      }
    }

    // Check CV clauses for appropriate argument types.
    if (lce.K != null) {
      try { Integer.parseInt(lce.K.value); }
      catch (Exception e) { // B40
        reportError(lce.K.line,
            "The value supplied after 'cval' must be an integer.");
      }
    }

    if (lce.alpha != null) {
      try { Double.parseDouble(lce.alpha.value); }
      catch (Exception e) { // B43
        reportError(lce.alpha.line,
            "The value supplied after 'alpha' must be an double.");
      }
    }

    // Check "preExtract" clause for appropriate argument type.
    if (!(lce.preExtract.value.equals("\"none\"")
          || lce.preExtract.value.equals("\"disk\"")
          || lce.preExtract.value.equals("\"diskZip\"")
          || lce.preExtract.value.equals("\"memory\"")
          || lce.preExtract.value.equals("\"memoryZip\"")
          || lce.preExtract.value.equals("\"true\"")
          || lce.preExtract.value.equals("\"false\"")
          || lce.preExtract.value.equals("true")
          || lce.preExtract.value.equals("false"))) { // B41
      reportError(lce.preExtract.line,
          "The value supplied after 'preExtract' must be a boolean or one of "
          + "(\"none\"|\"disk\"|\"diskZip\"|\"memory\"|\"memoryZip\").");
    }

    // Check that pre-extraction has not been enabled without a from clause.
    if (!(lce.preExtract.value.equals("\"none\"")
          || lce.preExtract.value.equals("\"false\"")
          || lce.preExtract.value.equals("false"))
        && lce.parser == null) {  // B60
      reportWarning(lce.preExtract.line,
          "Feature pre-extraction will be disabled since there is no "
          + "\"from\" clause.");
      lce.preExtract = new Constant("false");
    }

    // Check "progressOutput" clause for appropriate argument type.
    if (lce.progressOutput != null) {
      try { Integer.parseInt(lce.progressOutput.value); }
      catch (Exception e) { // B42
        reportError(lce.progressOutput.line,
            "The value supplied after 'progressOutput' must be an integer.");
      }
    }

    // Check "prune" clause for appropriate argument types.
    // Only certain keywords are legal.
    if (lce.pruneCountType != null) {  // B58
      if (!(lce.pruneCountType.value.equals("\"global\"")
            || lce.pruneCountType.value.equals("\"perClass\""))
          || !(lce.pruneThresholdType.value.equals("\"count\"")
               || lce.pruneThresholdType.value.equals("\"percent\""))) {
        reportError(lce.pruneCountType.line,
          "The prune clause must take the form "
          + "'prune (\"global\"|\"perClass\") (\"count\"|\"percent\") X' "
          + "where X is numeric.");
      }

      if (lce.preExtract.value.equals("\"none\"")
          || lce.preExtract.value.equals("\"false\"")
          || lce.preExtract.value.equals("false")) {
        reportError(lce.preExtract.line,
          "Feature pruning cannot be performed unless pre-extraction is "
          + "enabled.");
      }
    }

    // The theshold must have the right type for the given keywords.
    if (lce.pruneThresholdType != null) { // B59
      if (lce.pruneThresholdType.value.equals("\"percent\"")) {
        try {
          double p = Double.parseDouble(lce.pruneThreshold.value);
          if (p < 0 || p > 1) throw new Exception();
        }
        catch (Exception e) {
          reportError(lce.pruneThresholdType.line,
            "The prune threshold must be a real number in [0,1] when using "
            + "the 'percent' type.");
        }
      }
      else {
        try { Integer.parseInt(lce.pruneThreshold.value); }
        catch (Exception e) {
          reportError(lce.pruneThresholdType.line,
            "The prune threshold must be an integer when using the 'count' "
            + "type.");
        }
      }
    }

    // Pruning implies pre-extraction.
    if (lce.pruneCountType != null
        && (lce.preExtract == null || lce.preExtract.value.equals("\"none\"")
            || lce.preExtract.value.equals("\"false\"")
            || lce.preExtract.value.equals("false"))) {
      lce.preExtract =
        new Constant(LearningClassifierExpression.defaultPreExtract);
      reportWarning(lce.pruneCountType.line,
        "Pruning cannot be performed without pre-extraction.  Setting "
        + "'preExtract " + lce.preExtract + "'.");
    }

    // Check "from" clause for appropriate argument type.
    if (lce.parser != null) { // B5
      if (!(lce.parser.typeCache instanceof ReferenceType))
        reportError(lce.parser.line,
            "The 'from' clause of a learning classifier expression must "
            + "instantiate a LBJ2.parse.Parser.");
      else {
        Class iceClass = lce.parser.typeCache.typeClass();
        if (!isAssignableFrom(Parser.class, iceClass))
          reportError(lce.parser.line,
              "The 'from' clause of a learning classifier expression must "
              + "instantiate a LBJ2.parse.Parser.");
      }
    }

    // Check "with" clause for appropriate argument types.
    LBJ2.IR.Type input = lce.argument.getType();
    Class inputClass = input.typeClass();
    ClassifierReturnType output = null;

    // Check that the specified algorithm accepts our input.
    if (!(lce.learnerName.typeCache instanceof ClassifierType)
        || !((ClassifierType) lce.learnerName.typeCache).isLearner()) { // B6
      reportError(lce.learnerName.line,
          "The 'with' clause of a learning classifier expression must "
          + "instantiate a LBJ2.learn.Learner.");
    }
    else {
      Class iceClass = AST.globalSymbolTable.classForName(lce.learnerName);

      if (iceClass != null) {
        if (!isAssignableFrom(Learner.class, iceClass)) // B6
          reportError(lce.learnerName.line,
              "The 'with' clause of a learning classifier expression must "
              + "instantiate a LBJ2.learn.Learner.");
        else {  // A4
          ClassifierType learnerType =
            (ClassifierType) lce.learnerName.typeCache;
          LBJ2.IR.Type learnerInputType = learnerType.getInput();
          if (!isAssignableFrom(learnerInputType.typeClass(), inputClass))
            reportError(lce.learnerName.line,  // B7
                "A learning classifier with input type '" + input
                + "' cannot use a Learner with input type '"
                + learnerInputType + "'.");

          output = learnerType.getOutput();
        }
      }
    }

    // Check that the specified algorithm can produce our output.
    if (output != null && !output.isContainableIn(lce.returnType)) {  // B3
      if (output.toString().charAt(0) != 'd'
          || lce.returnType.toString().charAt(0) != 'd')
        reportError(lce.line,
            "Learner " + lce.learnerName + " returns '" + output
            + "' which conflicts with the declared return type '"
            + lce.returnType + "'.");
      else {
        lce.checkDiscreteValues = true;
        reportWarning(lce.line,
            "Learner " + lce.learnerName + " returns '" + output
            + "' which may conflict with the declared return type '"
            + lce.returnType + "'.  A run-time error will be reported if a "
            + "conflict is detected.");
      }
    }
    else lce.returnType = output;

    if (output != null && lce.labeler != null
        && lce.labeler.returnType != null
        && !lce.labeler.returnType.isContainableIn(output)) { // B3
      if (output.toString().charAt(0) == 'd'
          && lce.labeler.returnType.toString().charAt(0) == 'd')
        reportWarning(lce.line,
            "The labeler for learner " + lceName + " may return more labels "
            + "than the learner is designed to deal with.  A run-time error "
            + "will be reported if a conflict is detected.");
      else
        reportWarning(lce.line,
            "The labeler for learner " + lceName + " may return labels that "
            + "the learner is not designed to deal with.  A run-time error "
            + "will be reported if a conflict is detected.");
    }

    // Check "testFrom" clause for appropriate argument type.
    if (lce.testParser != null) { // B50
      if (!(lce.testParser.typeCache instanceof ReferenceType))
        reportError(lce.testParser.line,
            "The 'testFrom' clause of a learning classifier expression must "
            + "instantiate a LBJ2.parse.Parser.");
      else {
        Class iceClass = lce.testParser.typeCache.typeClass();
        if (!isAssignableFrom(Parser.class, iceClass))
          reportError(lce.testParser.line,
              "The 'testFrom' clause of a learning classifier expression must"
              + " instantiate a LBJ2.parse.Parser.");
      }
    }

    representationTable.put(lceName, lce);  // A2

    if (attributeAnalysis) wekaIze(lce.line, lce.returnType, lce.name);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param p  The node to process.
   **/
  public void run(ParameterSet p) {
    if (!(currentCG instanceof LearningClassifierExpression)) { // B56
      reportError(p.line,
                  "Parameter sets cannot be defined outside of a "
                  + "LearningClassifierExpression.");
      return;
    }
    else {
      LearningClassifierExpression lce =
        (LearningClassifierExpression) currentCG;
      if (lce.K == null && lce.testParser == null) {  // B52
        reportError(lce.line,
          "Parameter tuning can only be performed if either a 'cval' "
          + "clause or a 'testFrom' clause is supplied.");
        return;
      }

      if (!p.inRounds) lce.parameterSets.add(p);
    }

    runOnChildren(p);

    // Make sure the values in the parameter set specification make sense.
    ExpressionList.ExpressionListIterator PI = null;

    if (p.isRange()) {
      ExpressionList rangeList = new ExpressionList();
      rangeList.add(p.start);
      rangeList.add(p.end);
      rangeList.add(p.increment);

      PI = rangeList.listIterator();
    }
    else {
      PI = p.listIterator();
    }

    for (int PIindex = 0; PI.hasNext(); ++PIindex) {
      Expression pe = PI.nextItem();

      // Replace unary negation expressions with negative constants.
      if (pe instanceof UnaryExpression) {
        if (p.inRounds) {
          reportError(p.line,
                      "The number of rounds must be a positive integer.");
          return;
        }

        UnaryExpression upe = (UnaryExpression) pe;
        if (upe.operation.operation != Operator.MINUS) {  // B53
          reportError(p.line, "A parameter set must include only literals.");
          return;
        }

        Expression subpe = upe.subexpression;
        if (!(subpe instanceof Constant)) { // B53
          reportError(p.line, "A parameter set must include only literals.");
          return;
        }

        pe =
          new Constant(pe.line, pe.byteOffset,
                       "-" + ((Constant) subpe).value);
        run((Constant) pe);

        if (p.isRange()) {
          switch (PIindex) {
            case 0: p.start = pe; break;
            case 1: p.end = pe; break;
            case 2: p.increment = pe; break;
          }
        }
        else PI.set(pe);
      }
      else if (!(pe instanceof Constant)) { // B53
        reportError(p.line,
            "A parameter set must include only simple constant expressions.");
        return;
      }
      else if (p.inRounds) {
        try { Integer.parseInt(((Constant) pe).value); }
        catch (Exception ex) {  // B35
          reportError(p.line,
                      "The number of rounds must be a positive integer.");
          return;
        }
      }

      // Determine the type of the parameter set.
      if (!pe.typeCache.typeClass().equals(String.class)
          && !(pe.typeCache instanceof PrimitiveType)) {
        reportError(p.line,
            "Parameter sets must include only primitive constants or "
            + "strings.");
        return;
      }
      else if (p.isRange()
               && (!(pe.typeCache instanceof PrimitiveType)
                   || ((PrimitiveType) pe.typeCache).type
                      == PrimitiveType.BOOLEAN)) {
        reportError(p.line,
            "Parameter set ranges must involve primitive values that aren't"
            + "booleans.");
        return;
      }

      else if (p.type == null) p.type = (Type) pe.typeCache.clone();

      else if (p.type.typeClass().equals(String.class)
               != pe.typeCache.typeClass().equals(String.class)) {
        reportError(p.line,
            "Strings cannot appear in a parameter set with any other type of "
            + "value.");
        return;
      }
      else if (p.type instanceof PrimitiveType) {
        PrimitiveType pt = (PrimitiveType) p.type;
        PrimitiveType pet = (PrimitiveType) pe.typeCache;
        if ((pt.type == PrimitiveType.BOOLEAN)
            != (pet.type == PrimitiveType.BOOLEAN)) {
          reportError(p.line,
              "booleans cannot appear in a parameter set with any other type "
              + "of value.");
          return;
        }

        if (p.isRange() && PIindex == 2) {
          if (pt.type == PrimitiveType.CHAR) {
            if (!pet.isWholeNumber()) {
              reportError(p.line,
                  "The increment of a character parameter set should be an "
                  + "integer.");
              return;
            }
          }
          else pt.type = Math.max(pt.type, pet.type);
        }
        else pt.type = Math.max(pt.type, pet.type);
      }
    }

    // If a range, make sure it's not infinite, and convert it.
    if (p.isRange()) {
      PrimitiveType pt = (PrimitiveType) p.type;
      double s =
        pt.type == PrimitiveType.CHAR
        ? (double) ((Constant) p.start).value.charAt(1)
        : Double.parseDouble(((Constant) p.start).value);
      double e =
        pt.type == PrimitiveType.CHAR
        ? (double) ((Constant) p.start).value.charAt(1)
        : Double.parseDouble(((Constant) p.end).value);
      double i = Double.parseDouble(((Constant) p.increment).value);

      // B54
      if (i == 0.0 || e - s != 0 && (e - s > 0) != (i > 0))
        reportError(p.line,
          "Infinite loop detected in parameter set range specification.");
      else p.convertRange();
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param b  The node to process.
   **/
  public void run(Block b) {
    boolean needLocalTable = b.symbolTable == null;
    if (needLocalTable)
      currentSymbolTable = b.symbolTable =
        new SymbolTable(currentSymbolTable);  // A13

    runOnChildren(b);

    if (needLocalTable) currentSymbolTable = currentSymbolTable.getParent();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param m  The node to process.
   **/
  public void run(MethodInvocation m) {
    runOnChildren(m);

    if (m.name.typeCache instanceof ClassifierType
        && m.parentObject == null) {
      if (m.arguments.size() != 1)  // B2
        reportError(m.line, "Classifiers can only take a single argument.");
      else {
        ClassifierReturnType returnType =
          ((ClassifierType) m.name.typeCache).getOutput();
        m.isClassifierInvocation = true;  // A5 B8

        if (m.isSensedValue // B9
            && !returnType
                .isContainableIn(((CodedClassifier) currentCG).returnType))
          reportError(m.line,
              "Classifier " + currentCG.getName() + " with return type '"
              + ((CodedClassifier) currentCG).returnType
              + "' cannot sense classifier " + m.name + " with return type '"
              + returnType + "'.");
        else
          if (!m.isSensedValue
              && (returnType.type == ClassifierReturnType.DISCRETE_GENERATOR
                  || returnType.type == ClassifierReturnType.REAL_GENERATOR
                  || returnType.type == ClassifierReturnType.MIXED_GENERATOR))
            reportError(m.line,
                "Feature generators may only be invoked as the value "
                + "argument of a sense statement in another generator.");
        else if (currentCG != null) // A9
          addInvokee(currentCG.getName(), m.name.toString());
      }
    }
    else if (m.name.typeCache instanceof InferenceType
             && m.parentObject == null) // B33
      reportError(m.line,
          "Inferences may only be invoked to create a new classifier in "
          + "classifier expression context.");
    else if (m.parentObject == null && m.name.name.length == 1
             && !m.isEvaluateArgument) // B38
      reportError(m.line, "Unrecognized classifier name: '" + m.name + "'");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param ice  The node to process.
   **/
  public void run(InstanceCreationExpression ice) {
    runOnChildren(ice);

    if (ice.parentObject == null) { // A4
      ice.typeCache = new ReferenceType(ice.name);
      ice.typeCache.runPass(this);
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param n  The node to process.
   **/
  public void run(Name n) {
    n.symbolTable = currentSymbolTable; // A13

    n.typeCache = n.symbolTable.get(n); // A4

    if (currentCG == null) return;

    if (n.typeCache instanceof ClassifierType) {
      if (ast.symbolTable.containsKey(n)) // A8
        addDependor(n.toString(), currentCG.getName());
    }
    else if (n.name.length > 1) {
      String className = n.toString();
      className = className.substring(0, className.lastIndexOf('.'));
      String fieldOrMethod = n.name[n.name.length - 1];

      if (ast.symbolTable.containsKey(className)
          && !fieldOrMethod.equals("isTraining")) {
        String currentCGName = currentCG.getName();
        addDependor(className, currentCGName);  // A8

        // A10
        if (ast.symbolTable.get(className) instanceof ClassifierType
            && !fieldOrMethod.equals("getInstance"))
          addInvokee(currentCGName, className);
      }
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ForStatement s) {
    if (s.initializers != null) // B34
      for (ASTNodeIterator I = s.initializers.iterator(); I.hasNext(); ) {
        ASTNode statementExpression = I.next();
        if (statementExpression instanceof ConstraintStatementExpression)
          reportError(statementExpression.line,
              "Constraint expressions are only allowed to appear as part of "
              + "their own separate expression statement.");
      }

    if (s.updaters != null) // B34
      for (ASTNodeIterator I = s.updaters.iterator(); I.hasNext(); ) {
        ASTNode statementExpression = I.next();
        if (statementExpression instanceof ConstraintStatementExpression)
          reportError(statementExpression.line,
              "Constraint expressions are only allowed to appear as part of "
              + "their own separate expression statement.");
      }

    if (!(s.body instanceof Block)) // A7
      s.body = new Block(new StatementList(s.body));

    currentSymbolTable = s.symbolTable = s.body.symbolTable =
      new SymbolTable(currentSymbolTable);  // A13

    runOnChildren(s);

    currentSymbolTable = currentSymbolTable.getParent();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(IfStatement s) {
    if (!(s.thenClause instanceof Block)) // A7
      s.thenClause = new Block(new StatementList(s.thenClause));
    if (s.elseClause != null && !(s.elseClause instanceof Block)) // A7
      s.elseClause = new Block(new StatementList(s.elseClause));
    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(ReturnStatement s) {
    if (currentCG instanceof ConstraintDeclaration
        || currentCG instanceof LearningClassifierExpression
        || currentCG instanceof CodedClassifier
           && ((CodedClassifier) currentCG).returnType.type
              != ClassifierReturnType.DISCRETE
           && ((CodedClassifier) currentCG).returnType.type
              != ClassifierReturnType.REAL) // B12
      reportError(s.line,
          "return statements may only appear in classifers of type discrete "
          + "or real, not in an array returner, a generator, or a "
          + "constraint.");

    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(SenseStatement s) {
    if (!(currentCG instanceof CodedClassifier)
        || ((CodedClassifier) currentCG).returnType.type
           == ClassifierReturnType.DISCRETE
        || ((CodedClassifier) currentCG).returnType.type
           == ClassifierReturnType.REAL) {  // B10
      reportError(s.line,
          "sense statements may only appear in an array returning classifier "
          + "or a generator.");
      return;
    }

    CodedClassifier currentCC = (CodedClassifier) currentCG;
    if (s.name != null) { // B11
      if (currentCC.returnType.type == ClassifierReturnType.DISCRETE_ARRAY
          || currentCC.returnType.type == ClassifierReturnType.REAL_ARRAY)
        reportError(s.line,
            "The names of features need not be sensed in an array returning "
            + "classifier.  (Use sense <expression>; instead of sense "
            + "<expression> : <expression>;)");
    }
    else if (currentCC.returnType.type
             == ClassifierReturnType.DISCRETE_GENERATOR) {  // A6
      s.name = s.value;
      s.value = new Constant("true");
    }
    else if (currentCC.returnType.type == ClassifierReturnType.REAL_GENERATOR)
    { // A6
      s.name = s.value;
      s.value = new Constant("1");
    }

    s.value.senseValueChild();  // B9

    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(WhileStatement s) {
    if (!(s.body instanceof Block)) // A7
      s.body = new Block(new StatementList(s.body));
    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param s  The node to process.
   **/
  public void run(DoStatement s) {
    if (!(s.body instanceof Block)) // A7
      s.body = new Block(new StatementList(s.body));
    runOnChildren(s);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param v  The node to process.
   **/
  public void run(VariableDeclaration v) {
    for (NameList.NameListIterator I = v.names.listIterator(); I.hasNext(); )
      currentSymbolTable.put(I.nextItem(), v.type); // A1
    runOnChildren(v);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param a  The node to process.
   **/
  public void run(Argument a) {
    currentSymbolTable.put(a);  // A1
    runOnChildren(a);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(Constant c) {
    int cType;

    String value = c.value;

    if (value.equals("true") || value.equals("false"))
      cType = PrimitiveType.BOOLEAN;
    else if (value.charAt(0) == '\'') cType = PrimitiveType.CHAR;
    else if (value.charAt(0) == '.') {
      if (value.matches(".*[fF].*")) {
        cType = PrimitiveType.FLOAT;
      }
      else {
        cType = PrimitiveType.DOUBLE;
      }
    }
    else if (value.substring(0, 1).matches("[0-9\\-]")) {
      if (value.matches(".*[fF].*")) {
        cType = PrimitiveType.FLOAT;
      }
      else if (value.matches(".*[\\.dD].*")) {
        cType = PrimitiveType.DOUBLE;
      }
      else if (value.matches(".*[lL].*")) {
        cType = PrimitiveType.LONG;
      }
      else {
        cType = PrimitiveType.INT;
      }
    }
    else {
      cType = -1; // is a string
    }

    if (cType == -1)
      c.typeCache = new ReferenceType(new Name("java.lang.String"));
    else c.typeCache = new PrimitiveType(cType);
    c.typeCache.runPass(this);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(ReferenceType t) {
    runOnChildren(t);
    if (t.typeClass() == null)  // B13
      reportError(t.line, "Cannot locate class '" + t + "'.");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param t  The node to process.
   **/
  public void run(ClassifierReturnType t) {
    // B14
    if (t.type == ClassifierReturnType.MIXED)
      reportError(t.line,
                  "There is no such type as mixed.  (There is only mixed%.)");
    else if (t.type == ClassifierReturnType.MIXED_ARRAY)
      reportError(t.line,
          "There is no such type as mixed[].  (There is only mixed%.)");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param c  The node to process.
   **/
  public void run(ConstraintDeclaration c) {
    addDependor(c.getName(), null); // A8

    currentSymbolTable = c.symbolTable = c.body.symbolTable
      = new SymbolTable(currentSymbolTable);  // A13

    containsConstraintStatement = false;
    CodeGenerator saveCG = currentCG;
    currentCG = c;
    runOnChildren(c);
    currentCG = saveCG;

    currentSymbolTable = currentSymbolTable.getParent();

    if (!containsConstraintStatement) // B20
      reportWarning(c.line,
          "Constraint '" + c.name
          + "' does not contain any constraint statements.");
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintStatementExpression e) {
    if (!(currentCG instanceof ConstraintDeclaration)) {  // B19
      reportError(e.line,
          "Constraint statements may only appear in constraint "
          + "declarations.");
      return;
    }

    containsConstraintStatement = true;
    runOnChildren(e);
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param q  The node to process.
   **/
  public void run(UniversalQuantifierExpression q) {
    q.argument.getType().quantifierArgumentType = true; // A14
    // A13
    currentSymbolTable = q.symbolTable = new SymbolTable(currentSymbolTable);

    ++quantifierNesting;
    runOnChildren(q);
    --quantifierNesting;

    currentSymbolTable = currentSymbolTable.getParent();

    // A15
    q.collectionIsQuantified =
      quantifierNesting > 0 && q.collection.containsQuantifiedVariable();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param q  The node to process.
   **/
  public void run(ExistentialQuantifierExpression q) {
    q.argument.getType().quantifierArgumentType = true; // A14
    // A13
    currentSymbolTable = q.symbolTable = new SymbolTable(currentSymbolTable);

    ++quantifierNesting;
    runOnChildren(q);
    --quantifierNesting;

    currentSymbolTable = currentSymbolTable.getParent();

    // A15
    q.collectionIsQuantified =
      quantifierNesting > 0 && q.collection.containsQuantifiedVariable();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param q  The node to process.
   **/
  public void run(AtLeastQuantifierExpression q) {
    q.argument.getType().quantifierArgumentType = true; // A14
    // A13
    currentSymbolTable = q.symbolTable = new SymbolTable(currentSymbolTable);

    ++quantifierNesting;
    runOnChildren(q);
    --quantifierNesting;

    currentSymbolTable = currentSymbolTable.getParent();

    // A15
    if (quantifierNesting > 0) {
      q.collectionIsQuantified = q.collection.containsQuantifiedVariable();
      q.lowerBoundIsQuantified = q.lowerBound.containsQuantifiedVariable();
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param q  The node to process.
   **/
  public void run(AtMostQuantifierExpression q) {
    q.argument.getType().quantifierArgumentType = true; // A14
    // A13
    currentSymbolTable = q.symbolTable = new SymbolTable(currentSymbolTable);

    ++quantifierNesting;
    runOnChildren(q);
    --quantifierNesting;

    currentSymbolTable = currentSymbolTable.getParent();

    // A15
    if (quantifierNesting > 0) {
      q.collectionIsQuantified = q.collection.containsQuantifiedVariable();
      q.upperBoundIsQuantified = q.upperBound.containsQuantifiedVariable();
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param i  The node to process.
   **/
  public void run(ConstraintInvocation i) {
    if (i.invocation.arguments.size() != 1)  // B2
      reportError(i.line, "Constraints can only take a single argument.");

    runOnChildren(i); // A9

    if (!(i.invocation.name.typeCache instanceof ConstraintType)) // B26
      reportError(i.line,
          "Only constraints can be invoked with the '@' operator.");

    // A15
    i.invocationIsQuantified =
      quantifierNesting > 0 && i.invocation.containsQuantifiedVariable();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param e  The node to process.
   **/
  public void run(ConstraintEqualityExpression e) {
    runOnChildren(e);

    e.leftIsDiscreteLearner = e.rightIsDiscreteLearner = false;
    if (e.left instanceof MethodInvocation) {
      MethodInvocation m = (MethodInvocation) e.left;
      // A12
      e.leftIsDiscreteLearner = m.name.typeCache instanceof ClassifierType;
      if (e.leftIsDiscreteLearner) {
        ClassifierType type = (ClassifierType) m.name.typeCache;
        e.leftIsDiscreteLearner =
          type.getOutput().type == ClassifierReturnType.DISCRETE
          && type.isLearner();
      }
    }

    if (e.right instanceof MethodInvocation) {
      MethodInvocation m = (MethodInvocation) e.right;
      // A12
      e.rightIsDiscreteLearner = m.name.typeCache instanceof ClassifierType;
      if (e.rightIsDiscreteLearner) {
        ClassifierType type = (ClassifierType) m.name.typeCache;
        e.rightIsDiscreteLearner =
          type.getOutput().type == ClassifierReturnType.DISCRETE
          && type.isLearner();
      }
    }

    // A15
    if (quantifierNesting > 0) {
      e.leftIsQuantified = e.left.containsQuantifiedVariable();
      e.rightIsQuantified = e.right.containsQuantifiedVariable();
    }
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param d  The node to process.
   **/
  public void run(InferenceDeclaration d) {
    addDependor(d.getName(), null); // A8

    if (d.headFinders.length == 0)  // B29
      reportError(d.line,
          "An inference with no head finder methods can never be applied to "
          + "a learner.");

    if (d.subjecttoClauses != 1)  // B30
      reportError(d.line,
          "Every inference must contain exactly one 'subjectto' clause "
          + "specifying a constraint. " + d.subjecttoClauses);

    if (d.withClauses > 1)  // B31
      reportError(d.line,
          "An inference may contain no more than one 'with' clause "
          + "specifying an inference algorithm.");

    currentCG = d;
    currentSymbolTable = d.symbolTable = new SymbolTable(currentSymbolTable);
    runOnChildren(d);
    currentSymbolTable = currentSymbolTable.getParent();
    currentCG = null;

    if (d.algorithm != null) {
      Class iceClass = d.algorithm.typeCache.typeClass();
      if (!isAssignableFrom(Inference.class, iceClass)) // B32
        reportError(d.algorithm.line,
            "The 'with' clause of an inference must instantiate an "
            + "LBJ2.infer.Inference.");

      if (iceClass != null && iceClass.equals(ILPInference.class)) {
        Expression[] arguments = d.algorithm.arguments.toArray();

        if (arguments[0] instanceof InstanceCreationExpression) { // B39
          InstanceCreationExpression ice =
            (InstanceCreationExpression) arguments[0];

          if ((ice.name.toString().equals("GLPKHook")
               || ice.name.toString().equals("LBJ2.infer.GLPKHook"))
              && !LBJ2.Configuration.GLPKLinked)
            reportError(ice.line,
                "LBJ has not been configured properly to use the GLPK "
                + "library.  See the Users's Manual for more details.");

          if ((ice.name.toString().equals("XpressMPHook")
               || ice.name.toString().equals("LBJ2.infer.XpressMPHook"))
              && !LBJ2.Configuration.XpressMPLinked)
            reportError(ice.line,
                "LBJ has not been configured properly to use the Xpress-MP "
                + "library.  Make sure the jar file is on your CLASSPATH "
                + "environment variable and re-run configure.");

          if ((ice.name.toString().equals("GurobiHook")
               || ice.name.toString().equals("LBJ2.infer.GurobiHook"))
              && !LBJ2.Configuration.GurobiLinked)
            reportError(ice.line,
                "LBJ has not been configured properly to use the Gurobi "
                + "library.  Make sure the jar file is on your CLASSPATH "
                + "environment variable and re-run configure.");
        }
      }
    }
    else
      d.algorithm = InferenceDeclaration.defaultInferenceConstructor; // A16
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param h  The node to process.
   **/
  public void run(InferenceDeclaration.HeadFinder h) {
    currentSymbolTable = h.symbolTable = h.body.symbolTable =
      new SymbolTable(currentSymbolTable); // A13
    runOnChildren(h);
    currentSymbolTable = currentSymbolTable.getParent();
  }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param n  The node to process.
   **/
  public void run(InferenceDeclaration.NormalizerDeclaration n) {
    runOnChildren(n);

    if (n.learner != null
        && !(n.learner.typeCache instanceof ClassifierType
             && ((ClassifierType) n.learner.typeCache).isLearner()))  // B27
      reportError(n.line,
          "The left hand side of the 'normalizedby' operator must be the "
          + "name of a LBJ2.learn.Learner.");

    if (!(n.normalizer.typeCache instanceof ReferenceType)
        || !isAssignableFrom(Normalizer.class,
                ((ReferenceType) n.normalizer.typeCache).typeClass()))  // B28
      reportError(n.line,
          "The right hand side of the 'normalizedby' operator must "
          + "instantiate a LBJ2.learn.Normalizer.");
  }
}

