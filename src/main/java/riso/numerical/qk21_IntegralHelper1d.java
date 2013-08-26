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

public class qk21_IntegralHelper1d implements java.io.Serializable
{
	Callback_1d f1;

	boolean is_discrete;

	public double[] a, b;
	public int neval;

	public qk21_IntegralHelper1d( Callback_1d f1, double[][] intervals, boolean is_discrete )
	{
		// If the limits of integration are not yet established,
		// the caller must do so before calling do_integral().
		
		if ( intervals == null )
		{
			a = b = null;
		}
		else
		{
			a = new double[ intervals.length ];
			b = new double[ intervals.length ];
			for ( int i = 0; i < intervals.length; i++ )
			{
				a[i] = intervals[i][0];
				b[i] = intervals[i][1];
			}
		}

		this.f1 = f1;

		this.is_discrete = is_discrete;
	}

	public double do_integral() throws Exception
	{
		neval = 0;

		if ( is_discrete ) 
		{
			double sum = 0;

			for ( int j = 0; j < a.length; j++ )
			{
				int i0 = (a[j] < b[j] ? (int)a[j] : (int)b[j]);
				int i1 = (a[j] < b[j] ? (int)b[j] : (int)a[j]);

				for ( int i = i0; i <= i1; i++ )
					sum += f1.f( (double)i );

				neval += i1-i0+1;
			}

			return sum;
		}
		else
		{
			double[] result = new double[1];
			double sum = 0;

			for ( int j = 0; j < a.length; j++ )
			{
				// Last three arguments are don't-cares for us.
				qk21.do_qk21( f1, a[j], b[j], result, new double[1], new double[1], new double[1] );

				neval += 21;
				sum += result[0];
			}

			return sum;
		}
	}
}
