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

/** This class contains a method to compute the discrete convolution of two
  * sequences.
  */
public class Convolve
{
	/** Compute the discrete convolution of two sequences of data. 
	  * First <tt>-Nx</tt> data are read for the first sequence, then <tt>-Ny</tt> data
	  * are read for the second.
	  */
	public static void main( String[] args )
	{
		try
		{
			int Nx = 0, Ny = 0;

			for ( int i = 0; i < args.length; i++ )
			{
				if ( args[i].charAt(0) != '-' ) continue;
				switch (args[i].charAt(1))
				{
				case 'N':
					switch (args[i].charAt(2))
					{
					case 'x':
						Nx = Integer.parseInt( args[++i] );
						break;
					case 'y':
						Ny = Integer.parseInt( args[++i] );
						break;
					}
					break;
				}
			}

			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			double[] x = new double[Nx], y = new double[Ny];

			for ( int i = 0; i < Nx; i++ )
			{
				st.nextToken();
				x[i] = Double.parseDouble( st.sval );
			}

			for ( int i = 0; i < Ny; i++ )
			{
				st.nextToken();
				y[i] = Double.parseDouble( st.sval );
			}

			double[] cxy = convolve(x,y);

			System.err.println( "length of convolution: "+cxy.length );
			for ( int i = 0; i < cxy.length; i++ )
				System.out.println( cxy[i] );
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	/** Compute convolution of two sequences. The length of the two sequences need not be
	  * the same, and the lengths need not be powers of two. The length of the output array
	  * will be the length of <tt>x</tt> plus the length of <tt>y</tt>, less one.
	  */
	public static double[] convolve( double[] x, double[] y )
	{
		int Npadded = x.length+y.length, N = x.length+y.length-1;

		if ( (1 << FFT.ilog2(Npadded)) != Npadded )
			// Npadded is not a power of two; round up to next power of 2.
			Npadded = 1 << (FFT.ilog2(Npadded)+1);

		Complex[] xy = new Complex[ Npadded ];

		for ( int i = 0; i < Npadded; i++ ) xy[i] = new Complex();
		for ( int i = 0; i < x.length; i++ ) xy[i].real = x[i];
		for ( int i = 0; i < y.length; i++ ) xy[i].imag = y[i];

		FFT.fft(xy);		// compute FFT on both columns at once.

		// Compute product of FFT's of each column of data.

		Complex[] xx = new Complex[ Npadded ];

		for ( int i = 0; i < Npadded; i++ )
		{
			// The FFT coefficient of the first column is (R1,I1), and 
			// that of the second is (R2,-I2). This bit is taken from
			// Brigham (1974), The Fast Fourier Transform, Figure 10-9.
			
			int i_reflect = (i == 0? 0: Npadded-i);

			double R1 = (xy[i].real + xy[i_reflect].real)/2;
			double I1 = (xy[i].imag - xy[i_reflect].imag)/2;
			double R2 = (xy[i].imag + xy[i_reflect].imag)/2;
			double I2 = (xy[i].real - xy[i_reflect].real)/2;

			// Multiply the coefficients to obtain the convolution.

			xx[i] = new Complex();
			xx[i].real = R1*R2 + I1*I2;
			xx[i].imag = -R1*I2 + I1*R2;
		}

		FFT.invfft(xx);	// inverse transform to obtain convolution.

		double[] cxy = new double[N];

		for ( int i = 0; i < cxy.length; i++ )
			cxy[i] = xx[i].real;	// imaginary part is always zero.

		return cxy;
	}
}
