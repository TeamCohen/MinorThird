/* RISO: an implementation of distributed belief networks.
 * This file (TopDownSplayTree.java) is a translation of splay tree
 * C code written by Danny Sleator, and it is redistributed as part
 * of the RISO project by permission of Danny Sleator, quoted below.
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

/* From: "Danny Sleator" <sleator+@cs.cmu.edu>
 * To: "Robert Dodier" <robert_dodier@yahoo.com>
 * Subject: Re: Permission to redistribute splay tree code?
 * Date: Mon, 31 Dec 2001 00:50:32 -0500
 * 
 * You're welcome to distribute your project under any license you want.
 * My splay tree code is unrestricted.  Do whatever you want with it.
 * Of course I'd prefer that you at least leave my name in the comments
 * somewhere, and perhaps even mention that you use splay trees to the
 * person using the software.  But these are only suggestions.
 * 
 *   Daniel Sleator, PhD
 *   Carnegie Mellon University
 *   Phones: 412-268-7563, 412-422-5377, 412-654-9585
 *   Email: sleator@cmu.edu
 */

/** <pre>
  *               An implementation of top-down splaying
  *                   D. Sleator <sleator@cs.cmu.edu>
  *     http://www.cs.cmu.edu/afs/cs.cmu.edu/user/sleator/www/home.html
  *                            March 1992
  * 
  *        [Java translation of splay(), insert(), and delete() by Robert Dodier,
  *         July 1998; max() and min() added by Robert Dodier. Permission to
  *         redistribute under GPL given by Danny Sleator to Robert Dodier.
  *         Following comments are from the original C code.]
  * 
  *   "Splay trees", or "self-adjusting search trees" are a simple and
  *   efficient data structure for storing an ordered set.  The data
  *   structure consists of a binary tree, without parent pointers, and no
  *   additional fields.  It allows searching, insertion, deletion,
  *   deletemin, deletemax, splitting, joining, and many other operations,
  *   all with amortized logarithmic performance.  Since the trees adapt to
  *   the sequence of requests, their performance on real access patterns is
  *   typically even better.  Splay trees are described in a number of texts
  *   and papers [1,2,3,4,5].
  * 
  *   The code here is adapted from simple top-down splay, at the bottom of
  *   page 669 of [3].  It can be obtained via anonymous ftp from
  *   ftp://ftp.cs.cmu.edu/user/sleator/.
  * 
  *   The chief modification here is that the splay operation works even if the
  *   key being splayed is not in the tree, and even if the tree root of the
  *   tree is null.  So the line:
  * 
  *                         t = splay(a, t);
  * 
  *   causes it to search for node with key a in the tree rooted at t.  If it's
  *   there, it is splayed to the root.  If it isn't there, then the node put
  *   at the root is the last one before null that would have been reached in a
  *   normal binary search for a.  (It's a neighbor of a in the tree.)  This
  *   allows many other operations to be easily implemented, as shown below.
  * 
  *   [1] "Fundamentals of data structures in C", Horowitz, Sahni,
  *        and Anderson-Freed, Computer Science Press, pp 542-547.
  *   [2] "Data Structures and Their Algorithms", Lewis and Denenberg,
  *        Harper Collins, 1991, pp 243-251.
  *   [3] "Self-adjusting Binary Search Trees" Sleator and Tarjan,
  *        JACM Volume 32, No 3, July 1985, pp 652-686.
  *   [4] "Data Structure and Algorithm Analysis", Mark Weiss,
  *        Benjamin Cummins, 1992, pp 119-130.
  *   [5] "Data Structures, Algorithms, and Performance", Derick Wood,
  *        Addison-Wesley, 1993, pp 367-375.
  * </pre>
  */
public class TopDownSplayTree implements java.io.Serializable
{
	/** Number of nodes in the tree; not needed for any of the operations.
	  */
	public int size;

	/** Root of this splay tree.
	  */
	public TreeNode root;

	public static class TreeNode implements java.io.Serializable
	{
		public TreeNode left, right;
		public double key, value;
	}

	/** Returns a reference to the node containing the minimum
	  * value of the subtree rooted on <tt>t</tt>.
	  * This is just the leftmost child of <tt>t</tt>.
	  */
	public static TreeNode min( TreeNode t )
	{
		if ( t == null ) return null;

		while ( t.left != null )
			t = t.left;
		
		return t;
	}

	/** Returns a reference to the node containing the maximum
	  * value of the subtree rooted on <tt>t</tt>.
	  * This is just the rightmost child of <tt>t</tt>.
	  */
	public static TreeNode max( TreeNode t )
	{
		if ( t == null ) return null;

		while ( t.right != null )
			t = t.right;
		
		return t;
	}

	/** Simple top down splay, not requiring <tt>a</tt> to be in the 
	  * subtree rooted on <tt>t</tt>.
	  * What it does is described in the header comment for the class.
	  */
	public static TreeNode splay( double a, TreeNode t )
	{
		if (t == null) return null;

		TreeNode N = new TreeNode(), l, r, y;

		N.left = N.right = null;
		l = r = N;

		for (;;)
		{
			if (a < t.key)
			{
				if (t.left == null) break;
				if (a < t.left.key)
				{
					y = t.left;						   /* rotate right */
					t.left = y.right;
					y.right = t;
					t = y;
					if (t.left == null) break;
				}
				r.left = t;							   /* link right */
				r = t;
				t = t.left;
			}
			else if (a > t.key)
			{
				if (t.right == null) break;
				if (a > t.right.key)
				{
					y = t.right;						  /* rotate left */
					t.right = y.left;
					y.left = t;
					t = y;
					if (t.right == null) break;
				}
				l.right = t;							  /* link left */
				l = t;
				t = t.right;
			}
			else
			{
				break;
			}
		}

		l.right = t.left;								/* assemble */
		r.left = t.right;
		t.left = N.right;
		t.right = N.left;

		return t;
	}

	/** Inserts <tt>new_key</tt> into this tree, unless it's already there.
	  */
	public void insert( double new_key, double value )
	{
		TreeNode t = root;
		TreeNode new_node = new TreeNode();
		
// System.err.print( "insert: new_key: "+new_key+", value: "+value+" ... " );

		new_node.key = new_key;
		new_node.value = value;

		if (t == null)
		{
			new_node.left = new_node.right = null;
			size = 1;
			root = new_node;
// System.err.println( "OK (now "+size+" nodes)." );
			return;
		}

		t = splay(new_key,t);
		if (new_key < t.key)
		{
			new_node.left = t.left;
			new_node.right = t;
			t.left = null;
			size ++;
			root = new_node;
// System.err.println( "OK (now "+size+" nodes)." );
			return;
		}
		else if (new_key > t.key)
		{
			new_node.right = t.right;
			new_node.left = t;
			t.right = null;
			size++;
			root = new_node;
// System.err.println( "OK (now "+size+" nodes)." );
			return;
		}
		else
		{
			/* We get here if it's already in the tree; don't add it again. */
// System.err.println( "oops, already in tree." );
			root = t;
			return;
		}
	}

	/** Deletes <tt>some_key</tt> from this tree, if it's there.
	  */
	public void delete( double some_key )
	{
		TreeNode t = root, x;

		if (t==null) return;

System.err.print( "delete: some_key: "+some_key+"... " );

		t = splay(some_key,t);

		if (some_key == t.key)
		{
			/* found it */
			if (t.left == null)
			{
				x = t.right;
			}
			else
			{
				x = splay(some_key, t.left);
				x.right = t.right;
			}
			size--;
			root = x;
System.err.println( "OK (now "+size+" nodes)." );
			return;
		}

System.err.println( "oops, not in tree." );
		root = t;						 /* It wasn't there */
	}

	/** Return the contents of the tree as an array with two columns and
	  * a number of rows equal to the number of keys in the tree.
	  * The first column corresponds to the keys and the second corresponds
	  * to the values.
	  */
	public double[][] dump()
	{
		double[][] xy = new double[size][2];

		int[] i = new int[1];	// pass an int by reference; initial value is 0

		traverse_inorder( root, xy, i );

		return xy;
	}

	/** Traverse the sub-tree rooted on <tt>t</tt> from least to greatest keys.
	  * Load up the array <tt>xy</tt> with key+value pairs. Increment the 
	  * index <tt>i</tt> with every pair stored.
	  */ 
	public static void traverse_inorder( TreeNode t, double[][] xy, int[] i )
	{
		if ( t == null ) return;

		traverse_inorder( t.left, xy, i );

		xy[ i[0] ][0] = t.key;
		xy[ i[0] ][1] = t.value;
		i[0] += 1;
	
		traverse_inorder( t.right, xy, i );
	}

	/** A sample use of these functions.  Start with the empty tree,
	  * insert some stuff into it, and then delete it.
	  */
	public static void main( String[] args )
	{
		int n = 1024;
		if ( args.length > 0 )
			n = Integer.parseInt( args[0] );

		TopDownSplayTree t = new TopDownSplayTree();

		t.root = null;			  /* the empty tree */
		t.size = 0;

		int i;

		for (i = 0; i < n; i++)
			t.insert( (double)((541*i) & (n-1)), (double)i );

		System.out.println("t.size = "+t.size);

		double[][] xy = t.dump();
		System.out.println( "contents: " );
		for ( i = 0; i < xy.length; i++ )
			System.out.println( "key: "+xy[i][0]+" value: "+xy[i][1] );

		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );

			System.out.print( "number? " );
			for ( st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; st.nextToken() )
			{
				double a = Double.parseDouble( st.sval );
				t.root = splay( a, t.root );
				if ( t.root.key == a )
					System.out.println( "found "+a+", value: "+t.root.value );
				else
				{
					System.out.print( a+" not found; " );
					if ( a > t.root.key )
						System.out.println( "key bracketed by "+t.root.key+" and "+min(t.root.right).key+"; bracket values: "+t.root.value+", "+min(t.root.right).value );
					else 
						System.out.println( "key bracketed by "+max(t.root.left).key+" and "+t.root.key+"; bracket values: "+max(t.root.left).value+", "+t.root.value );
				}

				System.out.print( "number? " );
			}
		}
		catch (Exception e)
		{
			System.err.println( "exception: "+e+"; stagger onward." );
		}

		for (i = 0; i < n; i++)
			t.delete( (double) ((541*i) & (n-1)) );

		System.out.println("t.size = "+t.size);
	}
}
