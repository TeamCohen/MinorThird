package edu.cmu.minorthird.util;

import java.io.Serializable;

/**
 * Math utilities.
 * 
 */
public class MathUtil{

	/** Sign function. */
	static public int sign(double x){
		if(x>0)
			return +1;
		else if(x<0)
			return -1;
		else
			return 0;
	}

	/** Absolute value function. */
	static public double abs(double x){
		if(x>0)
			return x;
		else
			return -x;
	}

	/** Logistic function. */
	static public double logistic(double x){
		return 1.0/(1.0+Math.exp(-x));
	}

	/**
	 * Accumulate a list of numbers, then report on mean, standard deviation, and
	 * other common statistics.
	 */
	static public class Accumulator implements Serializable{

		static private final long serialVersionUID=1;

		private double sum=0,cov=0,count=0;

		private boolean isBinomial=true;

            /** Clear the accumulator **/
        public void clear() {
            sum = cov = count = 0;
            isBinomial = true;
        }


		/** Add a new number to the accumulator. */
		public void add(double d){
			sum+=d;
			cov+=d*d;
			count++;
			if(d!=0&&d!=1)
				isBinomial=false;
		}

        /** Combine two accumulators. Result will be be as if every
            call b.add(x) had been followed by a call to this.add(x).
         **/
        public void addAll(Accumulator b) {
            sum += b.sum;
            cov += b.cov;
            count += b.count;
            isBinomial = isBinomial && b.isBinomial;
        }

		/** The mean of accumulated values. */
		public double mean(){
			return sum/count;
		}

		/** The number of accumulated values. */
		public double numberOfValues(){
			return count;
		}

		/** The variance of the accumulated values. */
		public double variance(){
			double avg=mean();
			return cov/count-avg*avg;
		}

		/** The population standard devation of the accumulated values. */
		public double populationStdDev(){
			return Math.sqrt(variance());
		}

		/** The sample standard devation of the accumulated values. */
		public double stdDev(){
			return populationStdDev()/Math.sqrt((count-1)/count);
		}

		/** The sample standard error of the accumulated values. */
		public double stdErr(){
			return stdDev()/Math.sqrt(count);
		}

		/** The standard error of binomially distributed values. */
		public double binomialStdErr(){
			if(!isBinomial)
				throw new IllegalArgumentException(
						"numbers in accumulator are not binomial!");
			double p=mean();
			return Math.sqrt(p*(1-p)/count);
		}

		/** The Z statistic. */
		public double z(double expected){
			return (mean()-expected)/stdErr();
		}
	}
}
