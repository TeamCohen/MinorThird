package LBJ2.IR;

import java.util.Iterator;
import java.util.LinkedList;

import LBJ2.Pass;
import LBJ2.parse.FoldParser;


/**
  * This class represents expressions that specify classifiers that learn.
  *
  * @author Nick Rizzolo
 **/
public class LearningClassifierExpression extends ClassifierExpression
{
  /**
    * If no learning algorithm is specified to learn a discrete classifier,
    * this learner is used.
   **/
  public static final InstanceCreationExpression defaultDiscreteLearner =
    new InstanceCreationExpression(
        new Name("SparseNetworkLearner"),
        new ExpressionList(
          new InstanceCreationExpression(
            new Name("SparseWinnow"),
            new ExpressionList(
              new Constant("1.35"),
              new ExpressionList(
                new Constant("0.8"),
                new ExpressionList(
                  new Constant("4"),
                  new ExpressionList(new Constant("1"))))),
            -1, -1)),
        -1, -1);

  /**
    * If no learning algorithm is specified to learn a real classifier, this
    * learner is used.
   **/
  public static final InstanceCreationExpression defaultRealLearner =
    new InstanceCreationExpression(
        new Name("StochasticGradientDescent"), -1, -1);

  /**
    * If no <code>alpha</code> clause appears during cross validation, this
    * constant is used.
   **/
  public static final double defaultAlpha = 0.05;

  /**
    * If no <code>preExtract</code> clause appears in the sources, this
    * constant is used.
   **/
  public static final String defaultPreExtract = "\"diskZip\"";


  /** The revision status of the LCE's features node. */
  public Integer featuresStatus;
  /** The revision status of the LCE's prune node. */
  public Integer pruneStatus;
  /** The revision status of the LCE's learning node. */
  public Integer learningStatus;
  /**
    * This flag is set <code>true</code> iff the changes to the learner's LBJ
    * specification require its Java translation to be regenerated and nothing
    * more.
   **/
  public boolean onlyCodeGeneration;

  /**
    * (&oslash;) The classifier this learning classifier gets its labels from.
   **/
  public ClassifierExpression labeler;

  /**
    * (&not;&oslash;) The classifier that does feature extraction for this
    * classifier; argument to <code>using</code>.
   **/
  public ClassifierExpression extractor;
  /** Counts the number of <code>using</code> clauses for error detection. */
  public int usingClauses;

  /**
    * (&oslash;) The encoding that the generated classifier will use when
    * storing string data in features.
   **/
  public Constant featureEncoding;
  /**
    * Counts the number of <code>encoding</code> clauses for error detection.
   **/
  public int encodingClauses;

  /**
    * (&oslash;) Tells this learning classifier how to get its training data;
    * argument to <code>from</code>.
   **/
  public InstanceCreationExpression parser;
  /** Counts the number of <code>from</code> clauses for error detection. */
  public int fromClauses;

  /**
    * (&oslash;) Represents the integer number of training repetitions;
    * augments the <code>from</code> clause.
   **/
  public Expression rounds;
  /** Training starts from this round number. */
  public int startingRound = 1;

  /** (&oslash;) Whether to use "global" or "perClass" feature pruning. */
  public Constant pruneCountType;
  /**
    * (&oslash;) Whether to use "count" or "percent" counting for feature
    * pruning.
   **/
  public Constant pruneThresholdType;
  /** (&oslash;) The feature pruning threshold. */
  public Constant pruneThreshold;
  /**
    * (&oslash;) The contents of {@link #pruneCountType} on the previous run
    * of the compiler, if any.
   **/
  public Constant previousPruneCountType;

  /**
    * (&oslash;) Tells this learning classifier how to construct its learning
    * algorithm; argument to <code>with</code>.  This variable should not
    * contain a non-<code>null</code> value if {@link #learnerName} and
    * {@link #learnerParameterBlock} contain non-<code>null</code> values.
   **/
  public InstanceCreationExpression learnerConstructor;
  /**
    * (&oslash;) The name of the learner for this classifier; first argument
    * to <code>with</code>.  If this variable contains a non-<code>null</code>
    * value, {@link #learnerParameterBlock} must also be
    * non-<code>null</code>, and {@link #learnerConstructor} must be
    * <code>null</code>.
   **/
  public Name learnerName;
  /**
    * (&oslash;) A block of statements that set parameters of the learner for
    * this classifier; second argument to <code>with</code>.  If this variable
    * contains a non-<code>null</code> value, {@link #learnerName} must also
    * be non-<code>null</code>, and {@link #learnerConstructor} must be
    * <code>null</code>.
   **/
  public Block learnerParameterBlock;
  /** Counts the number of <code>with</code> clauses for error detection. */
  public int withClauses;

  /**
    * (&oslash;) Tells this learning classifier how to get its testing data;
    * argument to <code>testFrom</code>.
   **/
  public InstanceCreationExpression testParser;
  /**
    * Counts the number of <code>testFrom</code> clauses for error detection.
   **/
  public int testFromClauses;

  /**
    * (&oslash;) Tells this learning classifier how to produce a prediction
    * during evaluation; argument to <code>evaluate</code>.
   **/
  public Expression evaluation;
  /**
    * Counts the number of <code>evaluate</code> clauses for error detection.
   **/
  public int evaluateClauses;

  /**
    * A list of the {@link ParameterSet} objects that appear in
    * the argument list.
   **/
  public LinkedList parameterSets;

  /**
    * (&oslash;) Represents the integer number of subsets to be used in k-fold
    * cross validation; first argument to <code>cval</code>.
   **/
  public Constant K;
  /**
    * (&oslash;) Dictates how the training data will be split into subsets for
    * use by cross validation; second argument to <code>cval</code>.
   **/
  public FoldParser.SplitPolicy splitPolicy;
  /** Counts the number of <code>cval</code> clauses for error detection. */
  public int cvalClauses;

  /**
    * (&oslash;) Determines how the user wishes cross-validation to test its
    * performance; argument to <code>testingMetric</code>.
   **/
  public InstanceCreationExpression testingMetric;
  /**
    * Counts the number of <code>testingMetric</code> clauses, for error
    * detection.
   **/
  public int testingMetricClauses;

  /**
    * (&not;&oslash;) The desired confidence level for cross validation's
    * confidence interval output; argument to <code>alpha</code>, which can
    * only be specified when <code>cval</code> is also specified.
   **/
  public Constant alpha;
  /** Counts the number of <code>alpha</code> clauses, for error detection. */
  public int alphaClauses;

  /**
    * (&not;&oslash;) A Boolean or string value indicating how feature vectors
    * are to be pre-extracted; argument to <code>preExtract</code>.  Possible
    * values are <code>false</code>, <code>true</code>, <code>"false"</code>,
    * <code>"true"</code>, <code>"none"</code>, <code>"memory"</code>,
    * <code>"disk"</code>, and <code>"diskZip"</code>.
   **/
  public Constant preExtract;
  /**
    * Counts the number of <code>preExtract</code> clauses for error
    * detection.
   **/
  public int preExtractClauses;

  /**
    * (&oslash;) Integer specifying how often (in examples) to give the user a
    * progress update during training; argument to
    * <code>progressOutput</code>.
   **/
  public Constant progressOutput;
  /**
    * Counts the number of <code>progressOutput</code> clauses, for error
    * detection.
   **/
  public int progressOutputClauses;

  /**
    * The {@link LBJ2.SemanticAnalysis} pass will let this
    * <code>LearningClassifierExpression</code> know if the features it
    * generates need to be checked for appropriateness in the context of the
    * enclosing {@link ClassifierAssignment} by setting this flag.
   **/
  public boolean checkDiscreteValues;


  /**
    * A string representation of the return type information for each feature.
    * This information is crucial in the construction of WEKA classifiers.
    *
    * <p> Its format follows this convention:
    * "&lt;type&gt;_&lt;name&gt;[_&lt;value-list&gt;]:&lt;type&gt;_&lt;name&gt;[_&lt;value-list&gt;:[...]]"
    *
    * <p> &lt;type&gt; can be either "num", "str", or "nom", representing
    * numerical, string, and nominal attributes respectively.
    *
    * <p> Numerical and string attribute encodings do not need a value-list,
    * while Nominal attribute encodings are required to contain a value list.
    *
    * Examples:
    * <ul>
    *   <li>
    *     Just a numerical attribute named Dan: "num_Dan:"
    *   <li>
    *     A numerical attribute named Dan and a string attribute named Nick:
    *     "num_Dan:str_Nick:"
    *   <li>
    *     A numerical attribute named Dan, a string attribute named Nick, and
    *     a nominal attribute named Arindam which can take the values "Cool",
    *     "Uncool", or "Kinda Cool":
    *     "num_Dan:str_Nick:nom_Arindam_\"Cool\",\"Uncool\",\"Kinda Cool\":"
    * </ul>
    *
   **/
  public StringBuffer attributeString = new StringBuffer();


  /**
    * Full constructor.  The line and byte offset are set to -1.
    *
    * @param l    The expression representing the labeler classifier.
    * @param ext  Representation of the extractor classifier.
    * @param p    The expression representing the parser applied to data.
    * @param r    The constant representing the number of training
    *             repetitions.
    * @param a    A learning algorithm instance creation expression.
    * @param ln   The name of a learning algorithm.
    * @param pb   A block of statements used to set learning algorithm
    *             parameters.
    * @param enc  The expression representing the feature encoding used in
    *             this learner's lexicon.
    * @param tp   The expression representing the test parser.
    * @param e    The expression used to compute a prediction.
    * @param k    The number of folds for cross validation.
    * @param prms The sets of parameter values used in tuning.
    * @param s    The method used to split the data for cross validation.
    * @param t    Determines how the user wishes cross-validation to test its
    *             performance.
    * @param al   The cross validation confidence interval width.
    * @param pre  A Boolean indicating whether example vectors will be
    *             pre-extracted.
    * @param pro  An integer indicating how often progress updates will be
    *             output.
    * @param pct  The prune type ("global" or "perClass").
    * @param ptt  The prune count type ("count" or "percent").
    * @param pt   The prune count threshold.
    * @param ls   The "learning status" node, set by RevisionAnalysis.
    * @param fs   The "features status" node, set by RevisionAnalysis.
    * @param ps   The "prune status" node, set by RevisionAnalysis.
    * @param at   The WEKA attribute string.
    * @param cdv  Value for {@link #checkDiscreteValues}.
   **/
  public LearningClassifierExpression(ClassifierExpression l,
                                      ClassifierExpression ext,
                                      InstanceCreationExpression p,
                                      Expression r,
                                      InstanceCreationExpression a, Name ln,
                                      Block pb, Constant enc,
                                      InstanceCreationExpression tp,
                                      Expression e, Constant k,
                                      LinkedList prms,
                                      FoldParser.SplitPolicy s,
                                      InstanceCreationExpression t,
                                      Constant al, Constant pre, Constant pro,
                                      Constant pct, Constant ptt, Constant pt,
                                      Integer ls, Integer fs, Integer ps,
                                      StringBuffer at, boolean cdv) {
    super(-1, -1);
    labeler = l;
    extractor = ext;
    usingClauses = extractor == null ? 0 : 1;
    parser = p;
    fromClauses = parser == null ? 0 : 1;
    rounds = r;
    learnerConstructor = a;
    learnerName = ln;
    learnerParameterBlock = pb;
    featureEncoding = enc;
    withClauses = learnerConstructor == null && learnerName == null ? 0 : 1;
    testParser = tp;
    testFromClauses = testParser == null ? 0 : 1;
    evaluation = e;
    evaluateClauses = evaluation == null ? 0 : 1;
    K = k;
    parameterSets = prms;
    splitPolicy = s;
    cvalClauses = K == null ? 0 : 1;
    testingMetric = t;
    testingMetricClauses = testingMetric == null ? 0 : 1;
    alpha = al;
    alphaClauses = al == null ? 0 : 1;
    if (alpha == null) alpha = new Constant("" + defaultAlpha);
    preExtract = pre;
    preExtractClauses = preExtract == null ? 0 : 1;
    if (preExtract == null)
      preExtract = new Constant(parser == null ? "false" : defaultPreExtract);
    progressOutput = pro;
    progressOutputClauses = progressOutput == null ? 0 : 1;
    pruneCountType = pct;
    pruneThresholdType = ptt;
    pruneThreshold = pt;
    learningStatus = ls;
    featuresStatus = fs;
    pruneStatus = ps;
    attributeString = at;
    checkDiscreteValues = cdv;
  }

  /**
    * Parser's unsupervised learning constructor.
    *
    * @param cl         A list of clauses.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public LearningClassifierExpression(LinkedList cl, int line, int byteOffset)
  {
    this(null, cl, line, byteOffset);
  }

  /**
    * Parser's supervised learning constructor.
    *
    * @param l          The expression representing the labeler classifier.
    * @param cl         A list of clauses.
    * @param line       The line on which the source code represented by this
    *                   node is found.
    * @param byteOffset The byte offset from the beginning of the source file
    *                   at which the source code represented by this node is
    *                   found.
   **/
  public LearningClassifierExpression(ClassifierExpression l, LinkedList cl,
                                      int line, int byteOffset) {
    super(line, byteOffset);

    labeler = l;
    extractor = null;
    parser = null;
    learnerConstructor = null;
    checkDiscreteValues = false;
    alpha = new Constant("" + defaultAlpha);
    parameterSets = new LinkedList();

    for (Iterator I = cl.iterator(); I.hasNext(); ) {
      Clause c = (Clause) I.next();

      if (c.type == Clause.USING) {
        extractor = (ClassifierExpression) c.argument;
        ++usingClauses;
      }
      else if (c.type == Clause.FROM) {
        parser = (InstanceCreationExpression) c.argument;
        rounds = c.rounds;
        ++fromClauses;
      }
      else if (c.type == Clause.WITH) {
        if (c.learnerParameterBlock == null)
          learnerConstructor = (InstanceCreationExpression) c.argument;
        else {
          learnerName = (Name) c.argument;
          learnerParameterBlock = c.learnerParameterBlock;
        }

        ++withClauses;
      }
      else if (c.type == Clause.ENCODING) {
        featureEncoding = (Constant) c.argument;
        ++encodingClauses;
      }
      else if (c.type == Clause.TESTFROM) {
        testParser = (InstanceCreationExpression) c.argument;
        ++testFromClauses;
      }
      else if (c.type == Clause.EVALUATE) {
        evaluation = (Expression) c.argument;
        ++evaluateClauses;
      }
      else if (c.type == Clause.CVAL) {
        if (c.argument != null)
          K = (Constant) c.argument;
        else
          // if there was a cval clause, but k was not given, set it to -1 so
          // cross validation gets invoked anyway, this occurs legally in the
          // case of manual separation
          K = new Constant("-1");

        // handle default action
        if (c.splitPolicy != null) splitPolicy = c.splitPolicy;
        else splitPolicy = FoldParser.SplitPolicy.sequential;

        ++cvalClauses;
      }
      else if (c.type == Clause.TESTINGMETRIC) {
        testingMetric = (InstanceCreationExpression) c.argument;
        ++testingMetricClauses;
      }
      else if (c.type == Clause.ALPHA) {
        alpha = (Constant) c.argument;
        ++alphaClauses;
      }
      else if (c.type == Clause.PREEXTRACT) {
        preExtract = (Constant) c.argument;
        ++preExtractClauses;
      }
      else if (c.type == Clause.PROGRESSOUTPUT) {
        progressOutput = (Constant) c.argument;
        ++progressOutputClauses;
      }
      else if (c.type == Clause.PRUNE) {
        pruneCountType = (Constant) c.pruneCountType;
        pruneThresholdType = (Constant) c.pruneThresholdType;
        pruneThreshold = (Constant) c.pruneThreshold;
      }
    }

    if (preExtract == null)
      preExtract = new Constant(parser == null ? "false" : defaultPreExtract);
  }


  /**
    * Returns a hash code value for java hash structures.
    *
    * @return A hash code for this object.
   **/
  public int hashCode() {
    int result = labeler == null ? 0 : labeler.hashCode();
    result += extractor.hashCode();
    if (parser != null) result += parser.hashCode();
    if (rounds != null) result += rounds.hashCode();
    if (learnerConstructor != null) result += learnerConstructor.hashCode();
    if (learnerName != null) result += learnerName.hashCode();
    if (learnerParameterBlock != null)
      result += learnerParameterBlock.hashCode();
    if (featureEncoding != null) result += featureEncoding.hashCode();
    if (testParser != null) result += testParser.hashCode();
    if (evaluation != null) result += evaluation.hashCode();
    if (K != null) result += K.hashCode();
    if (splitPolicy != null) result += splitPolicy.hashCode();
    if (testingMetric != null) result += testingMetric.hashCode();
    result += alpha.hashCode();
    result += preExtract.hashCode();
    if (progressOutput != null) result += progressOutput.hashCode();
    return result;
  }


  /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @return <code>true</code> iff this object is the same as the argument.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof LearningClassifierExpression)) return false;
    LearningClassifierExpression c = (LearningClassifierExpression) o;
    return (labeler == null ? c.labeler == null : labeler.equals(c.labeler))
           && extractor.equals(c.extractor)
           && (parser == null ? c.parser == null : parser.equals(c.parser))
           && (rounds == null ? c.rounds == null : rounds.equals(c.rounds))
           && (learnerConstructor == null
                 ? c.learnerConstructor == null
                 : learnerConstructor.equals(c.learnerConstructor))
           && (learnerName == null ? c.learnerName == null
                                   : learnerName.equals(c.learnerName))
           && (learnerParameterBlock == null
                 ? c.learnerParameterBlock == null
                 : learnerParameterBlock.equals(c.learnerParameterBlock))
           && (featureEncoding == null
                 ? c.featureEncoding == null
                 : featureEncoding.equals(c.featureEncoding))
           && (testParser == null ? c.testParser == null
                                  : testParser.equals(c.testParser))
           && (evaluation == null ? c.evaluation == null
                                  : evaluation.equals(c.evaluation))
           && (K == null ? c.K == null : K.equals(c.K))
           && (splitPolicy == null ? c.splitPolicy == null
                                   : splitPolicy.equals(c.splitPolicy))
           && (testingMetric == null ? c.testingMetric == null
                                     : testingMetric.equals(c.testingMetric))
           && alpha.equals(c.alpha)
           && preExtract.equals(c.preExtract)
           && (progressOutput == null
                 ? c.progressOutput == null
                 : progressOutput.equals(c.progressOutput))
           && (pruneCountType == null
                 ? c.pruneCountType == null
                 : pruneCountType.equals(c.pruneCountType))
           && (pruneThresholdType == null
                 ? c.pruneThresholdType == null
                 : pruneThresholdType.equals(c.pruneThresholdType))
           && (pruneThreshold == null
                 ? c.pruneThreshold == null
                 : pruneThreshold.equals(c.pruneThreshold));
  }


  /**
    * Returns an iterator used to successively access the children of this
    * node.
    *
    * @return An iterator used to successively access the children of this
    *         node.
   **/
  public ASTNodeIterator iterator() {
    LinkedList children = new LinkedList();

    if (labeler != null) children.add(labeler);
    children.add(extractor);
    if (parser != null) children.add(parser);
    if (rounds != null) children.add(rounds);
    if (learnerConstructor != null) children.add(learnerConstructor);
    if (learnerName != null) children.add(learnerName);
    if (learnerParameterBlock != null) children.add(learnerParameterBlock);
    if (featureEncoding != null) children.add(featureEncoding);
    if (testParser != null) children.add(testParser);
    if (evaluation != null) children.add(evaluation);
    if (K != null) children.add(K);
    if (testingMetric != null) children.add(testingMetric);
    children.add(alpha);
    children.add(preExtract);
    if (progressOutput != null) children.add(progressOutput);
    if (pruneCountType != null) children.add(pruneCountType);
    if (pruneThresholdType != null) children.add(pruneThresholdType);
    if (pruneThreshold != null) children.add(pruneThreshold);

    ASTNodeIterator I = new ASTNodeIterator();
    I.children = (ASTNode[]) children.toArray(new ASTNode[children.size()]);
    return I;
  }


  /**
    * Creates a new object with the same primitive data, and recursively
    * creates new member data objects as well.
    *
    * @return The clone node.
   **/
  public Object clone() {
    ClassifierExpression l =
      labeler == null ? null : (ClassifierExpression) labeler.clone();
    ClassifierExpression ext = (ClassifierExpression) extractor.clone();
    InstanceCreationExpression p =
      parser == null ? null : (InstanceCreationExpression) parser.clone();
    Expression r = rounds == null ? null : (Expression) rounds.clone();
    InstanceCreationExpression a =
      learnerConstructor == null
        ? null
        : (InstanceCreationExpression) learnerConstructor.clone();
    Name ln = learnerName == null ? null : (Name) name.clone();
    Block pb =
      learnerParameterBlock == null ? null
                                    : (Block) learnerParameterBlock.clone();
    Constant enc =
      featureEncoding == null ? null : (Constant) featureEncoding.clone();
    InstanceCreationExpression tp =
      testParser == null ? null
                         : (InstanceCreationExpression) testParser.clone();
    Expression e =
      evaluation == null ? null : (Expression) evaluation.clone();
    Constant k = K == null ? null : (Constant) K.clone();
    LinkedList prms = null;
    if (parameterSets != null) {
      prms = new LinkedList();
      for (Iterator I = parameterSets.iterator(); I.hasNext(); )
        prms.add(((ParameterSet) I.next()).clone());
    }
    FoldParser.SplitPolicy s = splitPolicy;
    InstanceCreationExpression t =
      testingMetric == null
      ? null : (InstanceCreationExpression) testingMetric.clone();
    Constant al = (Constant) alpha.clone();
    Constant pre = (Constant) preExtract.clone();
    Constant pro =
      progressOutput == null ? null : (Constant) progressOutput.clone();
    Constant pct =
      pruneCountType == null ? null : (Constant) pruneCountType.clone();
    Constant ptt =
      pruneThresholdType == null ? null
                                 : (Constant) pruneThresholdType.clone();
    Constant pt =
      pruneThreshold == null ? null
                             : (Constant) pruneThreshold.clone();

    return
      new LearningClassifierExpression(
        l, ext, p, r, a, ln, pb, enc, tp, e, k, prms, s, t, al, pre, pro, pct,
        ptt, pt, learningStatus, featuresStatus, pruneStatus,
        new StringBuffer(attributeString.toString()), checkDiscreteValues);
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
    if (parenthesized) buffer.append("(");
    buffer.append("learn");

    if (labeler != null) {
      buffer.append(" ");
      labeler.write(buffer);
    }

    buffer.append(" using ");
    extractor.write(buffer);

    if (parser != null) {
      buffer.append(" from ");
      parser.write(buffer);

      if (rounds != null) {
        buffer.append(" ");
        rounds.write(buffer);
        buffer.append(" rounds");
      }
    }

    if (learnerConstructor != null) {
      buffer.append(" with ");
      learnerConstructor.write(buffer);
    }
    else if (learnerName != null) {
      buffer.append(" with ");
      learnerName.write(buffer);
      learnerParameterBlock.write(buffer);
    }

    if (featureEncoding != null) {
      buffer.append(" encoding ");
      featureEncoding.write(buffer);
    }

    if (evaluation != null) {
      buffer.append(" evaluate ");
      evaluation.write(buffer);
    }

    if (pruneCountType != null) {
      buffer.append(" prune ");
      pruneCountType.write(buffer);
      buffer.append(" ");
      pruneThresholdType.write(buffer);
      buffer.append(" ");
      pruneThreshold.write(buffer);
    }

    if (K != null) {
      buffer.append(" cval ");

      if (splitPolicy != FoldParser.SplitPolicy.manual) {
        K.write(buffer);
        buffer.append(" ");
      }

      buffer.append("\"" + splitPolicy + "\"");

      buffer.append(" alpha ");
      alpha.write(buffer);
    }

    if (testingMetric != null) {
      buffer.append(" testingMetric ");
      testingMetric.write(buffer);
    }

    if (testParser != null) {
      buffer.append(" testFrom ");
      testParser.write(buffer);
    }

    buffer.append(" preExtract ");
    preExtract.write(buffer);

    if (progressOutput != null) {
      buffer.append(" progressOutput ");
      progressOutput.write(buffer);
    }

    buffer.append(" end");
    if (parenthesized) buffer.append(")");
  }


  /**
    * Creates a <code>StringBuffer</code> containing a shallow representation
    * of this <code>ASTNode</code>.
    *
    * @return A <code>StringBuffer</code> containing a shallow text
    *         representation of the given node.
   **/
  public StringBuffer shallow() {
    StringBuffer buffer = new StringBuffer();
    if (comment != null) {
      buffer.append(comment);
      buffer.append(" ");
    }

    returnType.write(buffer);
    buffer.append(" ");
    name.write(buffer);
    buffer.append("(");
    argument.write(buffer);
    buffer.append(") ");

    if (singleExampleCache) buffer.append("cached ");

    if (cacheIn != null) {
      buffer.append("cachedin");

      if (cacheIn.toString().equals(ClassifierAssignment.mapCache))
        buffer.append("map");
      else {
        buffer.append(" ");
        cacheIn.write(buffer);
      }

      buffer.append(' ');
    }

    buffer.append("<- learn");

    if (labeler != null) buffer.append(" " + labeler.name);
    buffer.append(" using " + extractor.name);

    if (parser != null) {
      buffer.append(" from ");
      parser.write(buffer);

      if (rounds != null) {
        buffer.append(" ");
        rounds.write(buffer);
        buffer.append(" rounds");
      }
    }

    if (learnerConstructor != null) {
      buffer.append(" with ");
      learnerConstructor.write(buffer);
    }
    else if (learnerName != null) {
      buffer.append(" with ");
      learnerName.write(buffer);
      learnerParameterBlock.write(buffer);
    }

    if (featureEncoding != null) {
      buffer.append(" encoding ");
      featureEncoding.write(buffer);
    }

    if (evaluation != null) {
      buffer.append(" evaluate ");
      evaluation.write(buffer);
    }

    if (pruneCountType != null) {
      buffer.append(" prune ");
      pruneCountType.write(buffer);
      buffer.append(" ");
      pruneThresholdType.write(buffer);
      buffer.append(" ");
      pruneThreshold.write(buffer);
    }

    if (K != null) {
      buffer.append(" cval ");

      if (splitPolicy != FoldParser.SplitPolicy.manual) {
        K.write(buffer);
        buffer.append(" ");
      }

      buffer.append("\"" + splitPolicy + "\"");

      buffer.append(" alpha ");
      alpha.write(buffer);
    }

    if (testingMetric != null) {
      buffer.append(" testingMetric ");
      testingMetric.write(buffer);
    }

    if (testParser != null) {
      buffer.append(" testFrom ");
      testParser.write(buffer);
    }

    buffer.append(" preExtract ");
    preExtract.write(buffer);

    if (progressOutput != null) {
      buffer.append(" progressOutput ");
      progressOutput.write(buffer);
    }

    buffer.append(" end");
    return buffer;
  }


  /**
    * This class represents a clause in a
    * {@link LearningClassifierExpression}.  Note that this class is not an
    * {@link ASTNode} since it is only an intermediary used during parsing.
    *
    * @author Nick Rizzolo
  **/
  public static class Clause
  {
    /** Value of the <code>type</code> variable. */
    public static final int USING = 0;
    /** Value of the <code>type</code> variable. */
    public static final int FROM = 1;
    /** Value of the <code>type</code> variable. */
    public static final int WITH = 2;
    /** Value of the <code>type</code> variable. */
    public static final int TESTFROM = 3;
    /** Value of the <code>type</code> variable. */
    public static final int EVALUATE = 4;
    /** Value of the <code>type</code> variable. */
    public static final int CVAL = 5;
    /** Value of the <code>type</code> variable */
    public static final int PREEXTRACT = 6;
    /** Value of the <code>type</code> variable */
    public static final int PROGRESSOUTPUT = 7;
    /** Value of the <code>type</code> variable */
    public static final int TESTINGMETRIC = 8;
    /** Value of the <code>type</code> variable */
    public static final int ALPHA = 9;
    /** Value of the <code>type</code> variable */
    public static final int PRUNE = 10;
    /** Value of the <code>type</code> variable */
    public static final int ENCODING = 11;
    /** String representations of the type names. */
    public static final String[] typeNames =
      {
        "using", "from", "with", "testFrom", "evaluate", "cval", "preExtract",
        "progressOutput", "testingMetric", "alpha", "prune", "encoding"
      };


    /** The type of the clause. */
    public int type;
    /** The argument of the clause. */
    public ASTNode argument;
    /**
      * Represents the number training repetitions; used only by the
      * <code>from</code> clause.
     **/
    public Expression rounds;
    /**
      * A block of statements intended to be used to set learner parameters;
      * used only by the <code>with</code> clause.
     **/
    public Block learnerParameterBlock;
    /**
      * Dictates how cross-validation divides the training data; used only by
      * the <code>cval</code> clause.
     **/
    public Constant K;
    /** Whether to use "global" or "perClass" feature pruning. */
    public Constant pruneCountType;
    /** Whether to use "count" or "percent" counting for feature pruning. */
    public Constant pruneThresholdType;
    /** The feature pruning threshold. */
    public Constant pruneThreshold;
    /**
      * Dictates how the training data will be split into subsets for use by
      * cross validation.
     **/
    public FoldParser.SplitPolicy splitPolicy;
    /**
      * Determines the parameter with which the confidence interval is
      * calculated.  Takes the value .05 if the user does not specify
      * otherwise.
     **/
    public Constant alpha;
    /**
      * Determines how often to give the user status output during training.
     **/
    public Constant progressOutput;


    /**
      * Initializing constructor.
      *
      * @param t  The type.
      * @param a  The argument node.
    **/
    public Clause(int t, ASTNode a) { this(t, a, (Constant) null); }

    /**
      * Full constructor.
      *
      * @param t  The type.
      * @param a  The argument node.
      * @param r  Could represent the number of training repetitions or the
      *           split policy.
    **/
    public Clause(int t, ASTNode a, Expression r) {
      type = t;

      if (t == CVAL) {
        argument = a;

        if (r == null) splitPolicy = FoldParser.SplitPolicy.sequential;
        else {
          if (r instanceof Constant) {
            String s = ((Constant) r).value;
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
              s = s.substring(1, s.length() - 1);

            if (s.equals("random"))
              splitPolicy = FoldParser.SplitPolicy.random;
            else if (s.equals("sequential"))
              splitPolicy = FoldParser.SplitPolicy.sequential;
            else if (s.equals("kth"))
              splitPolicy = FoldParser.SplitPolicy.kth;
            else if (s.equals("manual"))
              splitPolicy = FoldParser.SplitPolicy.manual;
            else {
              System.err.println("Error: '" + s + "' is not a split policy.");
              System.exit(1);
            }
          }
        }
      }
      else {
        argument = a;
        rounds = r;
      }
    }

    /**
      * A constructor with 3 Constant parameters, used for the
      * <code>prune</code> clause.
      *
      * @param t  The type.
      * @param x  The first parameter.
      * @param y  The second parameter.
      * @param z  The third parameter.
    **/
    public Clause(int t, Constant x, Constant y, Constant z) {
      type = t;

      if (t == PRUNE) {
        pruneCountType = x;
        pruneThresholdType = y;
        pruneThreshold = z;
      }
    }

    /**
      * A constructor for a with clause with a parameter setting block.
      *
      * @param t  The type.
      * @param n  The name of the learner used by this learning classifier.
      * @param b  The parameter setting block.
     **/
    public Clause(int t, Name n, Block b) {
      type = t;
      argument = n;
      learnerParameterBlock = b;
    }

    /**
      * This constructor is only called by {@link #clone()}.
      *
      * @param t    The type of the clause.
      * @param a    The argument of the clause.
      * @param r    Represents the number of training repetitions.
      * @param b    A block of statements that set parameters of a learning
      *             algorithm.
      * @param k    The number of folds for cross validation.
      * @param s    The data splitting policy for cross validation.
      * @param al   The width of the confidence interval for cross validation
      *             output.
      * @param p    The frequency in examples of progress updates.
      * @param pct  The type of pruning.
      * @param ptt  The type of feature counting for pruning.
      * @param pt   The pruning threshold.
     **/
    protected Clause(int t, ASTNode a, Expression r, Block b, Constant k,
                     FoldParser.SplitPolicy s, Constant al, Constant p,
                     Constant pct, Constant ptt, Constant pt) {
      type = t;
      argument = a;
      rounds = r;
      learnerParameterBlock = b;
      K = k;
      splitPolicy = s;
      alpha = al;
      progressOutput = p;
      pruneCountType = pct;
      pruneThresholdType = ptt;
      pruneThreshold = pt;
    }


    /**
      * Creates a new object with the same primitive data, and recursively
      * creates new member data objects as well.
      *
      * @return The clone node.
    **/
    public Object clone() {
      ASTNode a = argument == null ? null : (ASTNode) argument.clone();
      Expression r = rounds == null ? null : (Expression) rounds.clone();
      Block b =
        learnerParameterBlock == null ? null
                                      : (Block) learnerParameterBlock.clone();
      Constant k = K == null ? null : (Constant) K.clone();
      Constant al = alpha == null ? null : (Constant) alpha.clone();
      Constant p =
        progressOutput == null ? null : (Constant) progressOutput.clone();
      Constant pct =
        pruneCountType == null ? null : (Constant) pruneCountType.clone();
      Constant ptt =
        pruneThresholdType == null ? null
                                   : (Constant) pruneThresholdType.clone();
      Constant pt =
        pruneThreshold == null ? null : (Constant) pruneThreshold.clone();
      return new Clause(type, a, r, b, k, splitPolicy, al, p, pct, ptt, pt);
    }


    /**
      * Debugging utility method.
      *
      * @return A textual representation of this expression.
    **/
    public String toString() { return typeNames[type] + " " + argument; }
  }
}

