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

public class FunctionCache extends TopDownSplayTree implements Callback_1d
{
	/** If the interval containing <tt>x</tt> is this small or
	  * smaller, we can carry out the interpolation.
	  */
	public double close_enough = 1e-1;

	/** If the estimated error from the interpolation is greater than this,
	  * then reject the interpolated value and compute a new one the hard way.
	  * IGNORED AT PRESENT; WILL USE WITH RATIONAL INTERPOLATION SCHEME !!!
	  */
	public double error_tolerance = 1e-4;

	/** This is the function cached by this object.
	  */
	public Callback_1d target;

	/** Sets the parameters for this function cache.
	  * @param close_enough Pass in -1 to use default value.
	  * @param error_tolerance Pass in -1 to use default value.
	  * @param target The function to approximate.
	  */
	public FunctionCache( double close_enough, double error_tolerance, Callback_1d target )
	{
		if ( close_enough > 0 ) this.close_enough = close_enough;
		if ( close_enough > 0 ) this.error_tolerance = error_tolerance;
		this.target = target;
	}

	/** Return a function value, either from the cache or newly computed.
	  * This method does nothing more than call <tt>lookup</tt>.
	  */
	public double f( double x ) throws Exception { return lookup(x); }

	/** Compute a new function value, cache it, and return it.
	  */
	public double cache_new_value( double x ) throws Exception
	{
		double fx = target.f( x );
		insert( x, fx );
// System.err.println( "FunctionCache.cache_new_value: x: "+x+" fx: "+fx );
if ( size % 400 == 0 ) System.err.println( "FunctionCache.cache_new_value: size now: "+size );
// if ( size % 1000 == 0 )
// { double[][] xy = dump();
// System.err.println( size+" pairs in FunctionCache: " );
// for ( int i = 0; i < xy.length; i++ )
// System.err.println( "x: "+xy[i][0]+"  y: "+xy[i][1] ); }
		return fx;
	}

	/** See if we can generate a value by interpolation;
	  * failing that, compute the function value, cache it,
	  * and return it.
	  */
	public double lookup( double x ) throws Exception
	{
		if ( root == null ) return cache_new_value( x );

		root = TopDownSplayTree.splay( x, root );
		TopDownSplayTree.TreeNode a, b;

		if ( x > root.key )
		{
			if ( root.right == null ) return cache_new_value( x );

			a = root;
			b = TopDownSplayTree.min( root.right );
		}
		else if ( x < root.key )
		{
			if ( root.left == null ) return cache_new_value( x );

			a = TopDownSplayTree.max( root.left );
			b = root;
		}
		else
		{
// System.err.println( "FunctionCache.lookup: exact match at "+x+"; return "+root.value );
			return root.value;
		}

		double da = x-a.key, dab = b.key-a.key;
if ( dab < 0 ) throw new RuntimeException( "FunctionCache.lookup: dab: "+dab+" < 0." );

		// If we're in a small interval (which should give us
		// an accurate interpolation) return interpolated value.

		if ( dab < close_enough )
		{
			double interpolated_value = (1-da/dab)*a.value + da/dab*b.value;
// System.err.println( "FunctionCache.lookup: interpolate at "+x+"; return "+interpolated_value );
			return interpolated_value;
		}
		else
		{
			return cache_new_value( x );
		}
	}
}
