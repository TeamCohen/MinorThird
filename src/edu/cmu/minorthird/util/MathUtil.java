package edu.cmu.minorthird.util;

/**
 * Math utilities.
 *
 */
public class MathUtil
{
	static public int sign(double x)
	{
		if (x>0) return +1;
		else if (x<0) return -1;
		else return 0;
	}

	static public double abs(double x)
	{
		if (x>0) return x;
		else return -x;
	}

	static public double logistic(double x)
	{
		return 1.0 / (1.0 + Math.exp(-x) );
	}
}
