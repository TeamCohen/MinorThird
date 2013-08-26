/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999, Robert Dodier.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */
package riso.numerical;
import java.io.*;

public class QuasiMC_IntegralHelper implements IntegralHelper, Serializable
{
	Callback_nd fn;
	boolean[] is_discrete, skip_integration;
	int[] integration_index;
	double total_sum, volume;

	public double[] x, a, b;
	public int neval, N;
	static public int EVAL_PER_DIMENSION = 500;

	public QuasiMC_IntegralHelper( Callback_nd fn, double[] a, double[] b, boolean[] is_discrete, boolean[] skip_integration )
	{
		this.fn = fn;

		// If the limits of integration are not yet established,
		// the caller must do so before calling do_integral().

		this.a = (a == null ? null : (double[]) a.clone());
		this.b = (b == null ? null : (double[]) b.clone());

		int n = a.length;
System.err.println( "IntegralHelper: set up "+n+"-dimensional integral, fn: "+fn.getClass() );
		x = new double[n];

		this.is_discrete = (boolean[]) is_discrete.clone();
		this.skip_integration = (boolean[]) skip_integration.clone();

		// Count up the number of dimensions in which we are computing a
		// integration over a continuous variable.

		int nintegration = 0, ndiscrete = 0, nskip = 0;
		for ( int i = 0; i < n; i++ )
			if ( ! is_discrete[i] && ! skip_integration[i] )
				++nintegration;
			else
			{
				// "is discrete" and "skip" are not mutually exclusive.
				if ( is_discrete[i] ) ++ndiscrete;
				if ( skip_integration[i] ) ++nskip;
			}
System.err.println( "IntegralHelper: #integrations: "+nintegration+"; #discrete "+ndiscrete+", #skip: "+nskip );
		integration_index = new int[nintegration];
		for ( int i = 0, j = 0; i < n; i++ )
			if ( ! is_discrete[i] && ! skip_integration[i] )
				integration_index[j++] = i;

		// Let number of function evaluations grow like the square of 
		// the number of dimensions. THAT'S NOT AT ALL SCIENTIFIC !!!

		N = nintegration * nintegration * EVAL_PER_DIMENSION;
	}

	public double do_integral( double[] x_in ) throws Exception
	{
		System.arraycopy( x_in, 0, x, 0, x.length );
		return do_integral();
	}

	public double do_integral() throws Exception
	{
		neval = 0;
		volume = 1;
		for ( int i = 0; i < integration_index.length; i++ )
			volume *= (b[ integration_index[i] ]-a[ integration_index[i] ]);

		total_sum = 0;
		do_integral_recursion(0);
// System.err.println( "do_integral: total_sum: "+total_sum );
		return total_sum;
	}
		
	public void do_integral_recursion( int n ) throws Exception
	{
		if ( n == x.length )
		{
			// Recursion has bottomed out -- all discrete variables have been assigned a value.
// System.err.print( "do_integral_recursion: call do_qmc_integral w/ x=[" );
// for ( int i = 0; i < x.length; i++ )
// if ( is_discrete[i] ) System.err.print( x[i]+"(d) " );
// else if ( skip_integration[i] ) System.err.print( x[i]+"(s) " );
// else System.err.print( "xx " );
// System.err.println( "], N == "+N );
			if ( integration_index.length == 0 )
				// There are no variables to integrate over.
				total_sum += fn.f(x);
			else if ( integration_index.length == 1 )
				// There is exactly one variable to integrate over; can't handle by QMC !!!
				throw new Exception( "QuasiMC_IntegralHelper.do_integral_recursion: can't handle 1-dimensional integration." );
			else
				// General case -- integrate over two or more dimensions.
				total_sum += do_qmc_integral();
		}
		else
		{
			// Go on to next dimension. There's something to do here only if the n'th
			// variable is discrete, in which case we sum over it. (The sum is a class variable.)

			if ( is_discrete[n] ) 
			{
				int i0 = (a[n] < b[n] ? (int)a[n] : (int)b[n]);
				int i1 = (a[n] < b[n] ? (int)b[n] : (int)a[n]);

				for ( int i = i0; i <= i1; i++ )
				{
					x[n] = i;
					do_integral_recursion(n+1);
				}
			}
			else
				do_integral_recursion(n+1);
		}
	}

	/** Assume all discrete and skipped variables have been assigned values,
	  * and integrate over the remaining variables by a quasi Monte Carlo algorithm.
	  * The number of function evaluations within this method is <tt>N</tt>.
	  */
	public double do_qmc_integral() throws Exception
	{
		double sum = 0;
		double[] quasi = new double[ integration_index.length ];
		boolean[] flag = new boolean[2];
		LowDiscrepency.infaur( flag, quasi.length, N );
		if ( !flag[0] ) throw new Exception( "QuasiMC_IntegralHelper: "+quasi.length+" is a bad number of dimensions for CACM 659 low-discrepency sequence." );
		if ( !flag[1] ) throw new Exception( "QuasiMC_IntegralHelper: sequence length "+N+" is apparently too big for CACM 659 low-discrepency sequence." );
		
		for ( int i = 0; i < N; i++ )
		{
			LowDiscrepency.gofaur( quasi );
			for ( int j = 0; j < quasi.length; j++ )
			{
				int ii = integration_index[j];
				x[ii] = a[ii] + (b[ii]-a[ii])*quasi[j];
			}

			sum += fn.f(x);
		}

		neval += N;
		return (sum/N)*volume;
	}

	public static void main( String[] args )
	{
		try
		{
			double[] a = new double[3], b = new double[3];

			int i;

			for ( i = 0; i < 3; i++ )
			{
				a[i] = Double.parseDouble( args[i] );
				b[i] = Double.parseDouble( args[3+i] );
				System.err.println( "a["+i+"]: "+a[i]+"  b["+i+"]: "+b[i] );
			}
		
			boolean[] is_discrete = new boolean[3];
			boolean[] skip_integration = new boolean[3];

			String s1 = args[6];
			for ( i = 0; i < 3; i++) 
				is_discrete[i] = (s1.charAt(i) == 'y');

			String s2 = args[7];
			for ( i = 0; i < 3; i++) 
				skip_integration[i] = (s2.charAt(i) == 'y');

			if ( args.length > 8 ) QuasiMC_IntegralHelper.EVAL_PER_DIMENSION = Integer.parseInt( args[8] );
			QuasiMC_IntegralHelper ih = new QuasiMC_IntegralHelper( new ThreeD(), a, b, is_discrete, skip_integration );

			for ( i = 0; i < 3; i++ )
				if ( skip_integration[i] )
					ih.x[i] = (a[i]+b[i])/2;

			System.err.println( "ih.do_integral: "+ih.do_integral() );

			System.err.println( "neval: "+ih.neval );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
