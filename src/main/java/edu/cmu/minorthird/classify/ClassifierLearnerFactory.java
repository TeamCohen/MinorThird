/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.classify;

/**
 * Generate many copies of a ClassifierLearner.
 *
 * @author William Cohen
 */

public class ClassifierLearnerFactory
{
	private String learnerName;
	public ClassifierLearnerFactory(String learnerName)
	{
		this.learnerName = learnerName;
	}
	/** Build a new copy of the learner produced by this factory. */
	public ClassifierLearner getLearner()
	{
		try {
			bsh.Interpreter interp = new bsh.Interpreter();
			interp.eval("import edu.cmu.minorthird.classify.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.linear.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.trees.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.knn.*;");
			interp.eval("import edu.cmu.minorthird.classify.algorithms.svm.*;");
			interp.eval("import edu.cmu.minorthird.classify.transform.*;");
			return (ClassifierLearner)interp.eval(learnerName);
		} catch (bsh.EvalError e) {
			throw new IllegalArgumentException("error parsing learnerName '"+learnerName+"':\n"+e);
		}
	}
}

