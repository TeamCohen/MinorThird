/* RISO: an implementation of distributed belief networks.
 * Copyright (C) 1999-2001, Robert Dodier.
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

package riso.general;

import java.io.*;

/** This class contains a static method to sort a list of objects using Shell's algorithm.
  * This code is a translation of a C program, <tt>shl.txt</tt>, by Thomas Niemann, available at
  * <a href="http://epaperpress.com/sortsearch/index.html">http://epaperpress.com/sortsearch</a>.
  * This program is distributed with RISO under the terms of the GPL by permission 
  * of Thomas Niemann. In response to my question:
  * <pre>
  *  > Do I have your permission to redistribute the shell
  *  > sort code (which I've translated into Java) under
  *  > the GPL?
  * </pre>
  * Thomas Niemann wrote:
  * <pre>
  *  From: "Thomas Niemann" <thomasn@epaperpress.com>
  *  To: "Robert Dodier" <robert_dodier@yahoo.com>
  *  Subject: Re: Permission to redistribute shell sort code?
  *  Date: Tue, 1 Jan 2002 13:19:24 -0800
  *  
  *  Sure, it's okay. No need to reference me or my site.
  * </pre>
  *
  * @see Comparator
  * @param a List of objects -- must all be the same comparable by
  *   the argument <tt>cmp</tt>.
  * @param lb Lower bound -- set to 0 for top-level call.
  * @param ub Upper bound -- set to <tt>a.length-1</tt> for top-level call.
  * @param cmp Instance of a class which knows how to compare the objects
  *   in the list <tt>a</tt>.
  */
public class ShellSort
{
	public static void do_sort( Object[] a, int lb, int ub, Comparator cmp )
	{
		int n, h, i, j;
		Object t;

		/* compute largest increment */
		n = ub - lb + 1;
		h = 1;
		if (n < 14)
			h = 1;
		else {
			while (h < n) h = 3*h + 1;
			h /= 3;
			h /= 3;
		}

		while (h > 0) {

			/* sort-by-insertion in increments of h */
			for (i = lb + h; i <= ub; i++) {
				t = a[i];
				for (j = i-h; j >= lb && cmp.greater(a[j], t); j -= h)
					a[j+h] = a[j];
				a[j+h] = t;
			}

			/* compute next increment */
			h /= 3;
		}
	}

	/** This function is a test program for the sorting method;
	  * it asks for a list of integers and sorts them.
	  */
	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			System.err.print( "give number of numbers: " );
			st.nextToken();
			int i, N = Integer.parseInt( st.sval );
			System.err.println( "give "+N+" numbers: " );

			Integer[] x = new Integer[N];

			for ( i = 0; i < N; i++ )
			{
				st.nextToken();
				x[i] = new Integer( Integer.parseInt( st.sval ) );
			}

			do_sort( x, 0, x.length-1, new IntComparator() );

			System.err.println( "sorted: " );
			for ( i = 0; i < N; i++ )
				System.err.println( x[i] );

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
