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
import riso.general.*;

public class FFT
{
	public static final int FORWARD = 1; 	// do forward (ordinary) transform
	public static final int INVERSE = 2;	// do inverse transform		

	/** Compute complex discrete fast Fourier transform of input
	  * array y[]. Transform is done in-place. Algorithm is taken from
	  * C. Balogh's class notes, Mth 553, Spr 1984.
	  * The forward transform computes the following sum:
	  *
	  * <pre>
	  *     Y[n] = sum_{k=0}^{N-1} y[k] exp( -2 PI n k i /N ),  n=0, 1, ...N-1.
	  * </pre>
	  *
	  * where N is the number of data and i is the imaginary unit. A
	  * straightforward encoding of this summation leads to the "slow" transform.
	  *
	  * @param y on entry: data points; on exit: Fourier coefficients.
	  * @param N number of data points --assume it's a power of 2.
	  */
	public static void fft( Complex[] y )
	{
		int	g, l, k, CTRLDNP, count, N = y.length;

		g = ilog2( N );	/* 2^g == N */
		count = N;		/* remember N before changing it */
		l = 1;
		k = 0;

		while ( l <= g )
		{
			do {
				CTRLDNP = 0;
				do { 
					++CTRLDNP;
					/* compute dual-node pair */
					dual( FORWARD, y, g, N, k, l, count );
					++k;
				}
				while ( CTRLDNP < N/2 );
				/* Skip */
				k += N/2;
			}
			while ( k < count -1 );	/* k == count -1 is end of column */

			/* get ready for next column */
			++l;
			N /= 2;
			k  = 0;
		}

		/* unscramble results */
		unscram( y, count, g );

		/* y[] now contains Fourier coefficients */
	}

	/** Compute inverse complex discrete fast Fourier transform of input
	  * array y[]. Transform is done in-place. Algorithm is taken from
	  * C. Balogh's class notes, Mth 553, Spr 1984.
	  * The inverse transform computes the following sum:
	  * 
	  * <pre>
	  *	    y[n] = (1/N) sum_{k=0}^{N-1} Y[k] exp( 2 PI n k i /N ),  n=0, 1, ...N-1.
	  * </pre>
	  *
	  * where N is the number of data and i is the imaginary unit. A
	  * straightforward encoding of this summation leads to the "slow" transform.
	  *
	  * @param Y on entry: Fourier coefficients; on exit: data points.
	  * @param N number of data points --assume it's a power of 2.
	  */
	public static void invfft( Complex[] Y )
	{
		int	g, l, k, CTRLDNP, count, N = Y.length;

		g = ilog2( N );	/* 2^g == N */
		count = N;		/* remember N before changing it */
		l = 1;
		k = 0;

		while ( l <= g ) {
			do {
				CTRLDNP = 0;
				do { 
					++CTRLDNP;
					/* compute dual-node pair */
					dual( INVERSE, Y, g, N, k, l, count );
					++k;
				}
				while ( CTRLDNP < N/2 );
				/* Skip */
				k += N/2;
			}
			while ( k < count -1 );	/* k == count -1 is end of column */

			/* get ready for next column */
			++l;
			N /= 2;
			k  = 0;
		}

		/* unscramble results */
		unscram( Y, count, g );

		/* ...and scale by factor of 1/N */
		for ( k =0; k < count; ++k ) {
			Y[k].real /= count;
			Y[k].imag /= count;
		}

		/* Y[] now contains signal data */
	}

	/** Compute the dual node function.
	  * @param flag Is this forward or inverse transform?
	  * @param x Array in process of being transformed.
	  * @param g Bits used for indexes, ie 2^g = count of data.
	  * @param N ???
	  * @param k ???
	  * @param l ???
	  * @param count Number of data.
	  */
	public static void dual( int flag, Complex[] x, int g, int N, int k, int l, int count )
	{
		Complex Wp = new Complex(), temp = new Complex();
		double twopin;
		int	m, p;

		twopin = 2 * Math.PI / (double) count;

		/* compute W^p, where W is exp(-i2PI/n) or exp(i2PI/n), n is count */
		m = k >> (g -l);
		p = bitrev( m, g );
		Wp.real = Math.cos( twopin * p );

		switch ( flag )
		{
		case FORWARD: Wp.imag = -Math.sin( twopin * p ); break;
		case INVERSE: Wp.imag =  Math.sin( twopin * p ); break;
		default: throw new RuntimeException( "dual: what is flag "+flag+" ?" );
		}

		/* now compute node pair */
		Complex.mul( Wp,   x[k+N/2], temp );
		Complex.sub( x[k], temp,     x[k+N/2] );
		Complex.add( x[k], temp,     x[k] );
	}

	public static void unscram( Complex[] x, int N, int g )
	{
		Complex temp = new Complex();
		int	k, BR;

		k = 0;
		do
		{
			BR = bitrev( k, g );
			if ( BR > k )
			{
				/* swap */
				temp.real = x[k].real;
				temp.imag = x[k].imag;
				x[k].real = x[BR].real;
				x[k].imag = x[BR].imag;
				x[BR].real= temp.real;
				x[BR].imag= temp.imag;
			}
			++k;
		}
		while ( k < N );
	}

	static int bitrev( int n, int len )
	{
		int	rev, j;

		rev = 0;
		for ( j =0; j < len; ++j ) {
			rev = (rev << 1) + (n % 2);
			n  /= 2;
		}
		return( rev );
	}

	static int ilog2( int k )
	{
		int	pow;

		pow = 0;
		while ( k > 1 ) {
			++pow;
			k >>= 1;
		}

		return pow;
	}

	/** Read some data and apply the FFT or inverse FFT, as specified.
	  */
	public static void main( String[] args )
	{
		try
		{
			int N = 0;
			boolean do_inverse = false, complex_input = false;

			for ( int i = 0; i < args.length; i++ )
			{
				if ( args[i].charAt(0) != '-' ) continue;
				switch (args[i].charAt(1))
				{
				case 'N':
					N = Integer.parseInt( args[++i] );
					break;
				case 'c':
					complex_input = true;
					break;
				case 'i':
					do_inverse = true;
					break;
				}
			}

			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			Complex[] x = new Complex[N];
			for ( int i = 0; i < N; i++ )
			{
				x[i] = new Complex();

				st.nextToken();
				x[i].real = Double.parseDouble( st.sval );

				if ( complex_input ) 
				{
					st.nextToken();
					x[i].imag = Double.parseDouble( st.sval );
				}
				// else imaginary part is zero.
			}

			if ( do_inverse )
				invfft(x);
			else
				fft(x);

			for ( int i = 0; i < N; i++ )
			{
				System.out.println( x[i].real+"  "+x[i].imag );
			}
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}
