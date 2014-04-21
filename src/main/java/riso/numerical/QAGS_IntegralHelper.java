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

public class QAGS_IntegralHelper implements IntegralHelper, Callback_1d, Serializable
{
	int n;
	Callback_nd fn;

	boolean[] is_discrete, skip_integration;

	public double[] x, a, b;
	public double epsabs = 1e-6, epsrel = 1e-6;
	public int limit;
	public int[] neval;			// counts function evaluations in each dimension
	public int npanels = 10;	// split each interval into this many pieces

	qags[] q;					// one context for each level; don't share work variables!

	public QAGS_IntegralHelper( Callback_nd fn, double[] a, double[] b, boolean[] is_discrete, boolean[] skip_integration )
	{
		this.fn = fn;

		// If the limits of integration are not yet established,
		// the caller must do so before calling do_integral().

		this.a = (a == null ? null : (double[]) a.clone());
		this.b = (b == null ? null : (double[]) b.clone());

		n = a.length;
System.err.println( "QAGS_IntegralHelper: set up "+n+"-dimensional integral, fn: "+fn.getClass() );
		x = new double[n];
		neval = new int[n];

		this.is_discrete = (boolean[]) is_discrete.clone();
		this.skip_integration = (boolean[]) skip_integration.clone();

		// Count up the number of dimensions in which we are computing a
		// integration over a continuous variable.

		int i, nintegration = 0, ndiscrete = 0, nskip = 0;
		for ( i = 0; i < n; i++ )
			if ( ! is_discrete[i] && ! skip_integration[i] )
				++nintegration;
			else
			{
				// "is discrete" and "skip" are not mutually exclusive.
				if ( is_discrete[i] ) ++ndiscrete;
				if ( skip_integration[i] ) ++nskip;
			}
System.err.println( "QAGS_IntegralHelper: #integrations: "+nintegration+"; #discrete "+ndiscrete+", #skip: "+nskip );

		switch ( nintegration )
		{
		case 1: limit = 30; break;
		
		case 2: limit = 5; break;
		case 3: limit = 3; break;
		case 4: limit = 2; break;
		case 5: limit = 2; break;
		default: // anything beyond 5
			limit = 1;
		}

		q = new qags[n];
		for ( i = 0; i < n; i++ ) q[i] = new qags();

		--n;	// now n == next dimension to integrate over
	}

	public double f( double x1 ) throws Exception
	{
		x[n] = x1;

		if ( n == 0 ) 
		{
			// Recursion has bottomed out -- return integrand value.
			double fnx = fn.f(x);
			return fnx;
		}
		else
		{
			double fx;

			--n;
			fx = do_integral();
			++n;

			return fx;
		}
	}

	public double do_integral( double[] x_in ) throws Exception
	{
		if ( x_in != null ) System.arraycopy( x_in, 0, x, 0, x.length );
		return do_integral();
	}
		
	public double do_integral() throws Exception
	{
		if ( skip_integration[n] )
		{
			// Assume that x[n] was set by do_integral's caller.

			if ( n == 0 ) 
			{
				++neval[n];
				return fn.f(x);
			}
			else
			{
				double fx;

				--n;
				fx = do_integral();
				++n;

				++neval[n];
				return fx;
			}
		}

		if ( is_discrete[n] ) 
		{
			// Compute the summation over x[n].
			double sum = 0;
			int i0 = (a[n] < b[n] ? (int)a[n] : (int)b[n]);
			int i1 = (a[n] < b[n] ? (int)b[n] : (int)a[n]);

			for ( int i = i0; i <= i1; i++ )
				sum += f( (double)i );

			neval[n] += i1-i0+1;

			return sum;
		}
		else
		{
			double total_result = 0, h = (b[n]-a[n])/npanels, aa, bb;
			double[] result = new double[1], abserr = new double[1];
			int[] ier = new int[1];
			boolean some_ier = false;

			for ( int i = 0; i < npanels; i++ )
			{
				aa = a[n] + i*h;
				bb = aa + h;
				q[n].do_qags( this, aa, bb, epsabs/npanels, epsrel, result, abserr, ier, limit );
				total_result += result[0];
				neval[n] += q[n].neval[0];

				if ( ier[0] != 0 ) 
					some_ier = true;
			}

			if ( some_ier && q[n].verbose_errors )
				System.err.println( "QAGS_IntegralHelper.do_integral: integrate over variable "+n+". WARNING: ier != 0 for at least one of "+npanels+" panels." );

			return total_result;
		}
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

			QAGS_IntegralHelper ih = new QAGS_IntegralHelper( new ThreeD(), a, b, is_discrete, skip_integration );

			for ( i = 0; i < 3; i++ )
				if ( skip_integration[i] )
					ih.x[i] = (a[i]+b[i])/2;

			System.err.println( "ih.do_integral: "+ih.do_integral() );

			for ( i = 0; i < 3; i++ )
				System.err.println( "neval["+i+"]: "+ih.neval[i] );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

class ThreeD implements Callback_nd
{
	public double f( double[] x )
	{
		// double b0 = Bickley.bickley( x[0], 0 );
		// double b1 = Bickley.bickley( x[1], 0 );
		// double b2 = Bickley.bickley( x[2], 0 );
		// double fx = b0*b1*b2;

		double fx = x[0]*x[1]*x[2];
		return fx;
	}
}
