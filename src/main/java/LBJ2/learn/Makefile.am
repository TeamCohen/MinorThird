
WEKAWRAPPER =
if WANT_WEKA
WEKAWRAPPER += WekaWrapper.java
endif

LIBLINEAR =
if WANT_LIBLINEAR
LIBLINEAR += SupportVectorMachine.java
endif

JAVA_FILES =  Accuracy.java AdaBoost.java BatchTrainer.java \
              BiasedRandomWeightVector.java BiasedWeightVector.java \
              BinaryMIRA.java ChildLexicon.java IdentityNormalizer.java \
              Learner.java LearnerToText.java Lexicon.java Log.java \
              LinearThresholdUnit.java MultiLabelLearner.java \
              MuxLearner.java NaiveBayes.java Normalizer.java \
              PassiveAggressive.java RandomWeightVector.java Sigmoid.java \
              Softmax.java SparseAveragedPerceptron.java SparseMIRA.java \
              SparseNetworkLearner.java SparseConfidenceWeighted.java \
              SparsePerceptron.java SparseWeightVector.java \
              SparseWinnow.java StochasticGradientDescent.java \
              TestingMetric.java \
              $(WEKAWRAPPER) $(LIBLINEAR)

if USING_JAVAC
AM_JAVACFLAGS = -source 1.4 @JAVA_DEBUG@
endif

include $(top_srcdir)/JavaMake.am

EXTRA_DIST =  $(JAVA_FILES) package.html
if WANT_WEKA
else
EXTRA_DIST += WekaWrapper.java
endif

if WANT_LIBLINEAR
else
EXTRA_DIST += SupportVectorMachine.java
endif

