package riso.numerical;
import java.io.*;

/** Linear algebra computations. All the methods of this class are static
  * and operate on double[][] objects. This works for ordinary matrices;
  * however, it would be nice to (eventually) define special kinds of 
  * matrices, e.g. IdentityMatrix or BlockDiagonalMatrix, and have these
  * be interchangeable with ordinary matrices -- but this scheme won't allow
  * that. Objects of types derived from Matrix aren't anything like double[][].
  *
  * <p> This code (<tt>Matrix.java</tt>) is derived in large part, 
  * via translation from C to Java, from <tt>gauss_el.c</tt> and <tt>gauss_so.c</tt>
  * from <a href="http://www.math.ntnu.no/num/nnm/Program/Numlibc/">the NUMLIBC web site</a>.
  * The above web site states only "These routines [NUMLIBC] are free, but
  * for any damages caused by their use we do not have any responsibility."
  * (The source code does not mention any license terms.) So, as a derivative
  * work, this file is distributed under the same terms.
  *
  * <p> Attempts to reach the original author (Tore Haavie) by email failed;
  * each attempt, with some modification of <tt>haavie@imf.unit.no</tt>,
  * failed with "unknown user".
  */
public class Matrix implements java.io.Serializable
{
	/** This exception is thrown if a matrix should be positive definite
	  * but it is not.
	  */
	public static class NotPositiveDefiniteException extends IllegalArgumentException
	{
		public NotPositiveDefiniteException( String s ) { super(s); }
	}

	/** This exception is thrown if a matrix should not be singular but it is.
	  */
	public static class SingularMatrixException extends IllegalArgumentException
	{
		public SingularMatrixException( String s ) { super(s); }
	}

	/** Return a copy of a matrix. The clone() method for double[][] objects
	  * is only a shallow copy; the row bases are cloned, but not the rows.
	  * This function doesn't return a Matrix object, so it's not called 
	  * clone().
	  * @return A copy of the input matrix.
	  */
	public static double[][] copy( double[][] A )
	{
		double[][] copy = (double[][]) A.clone();
		for ( int i = 0; i < A.length; i++ )
			copy[i] = (double[]) A[i].clone();

		return copy;
	}

	/** Compute the product of a vector with a matrix, and return it.
	  * The vector is understood to be on the <i>right side</i> of the matrix.
	  * Neither the matrix nor the vector are stomped.
	  * @throws IllegalArgumentException If the matrix and vector don't have
	  *   compatible sizes.
	  */
	public static double[] multiply( double[][] A, double[] x )
	{
		if ( A[0].length != x.length )
			throw new IllegalArgumentException( "Matrix.multiply: matrix and vector have incompatible sizes." );

		int m = A.length, n = A[0].length;
		double[] Ax = new double[m];

		for ( int i = 0; i < m; i++ )
		{
			Ax[i] = 0;
			for ( int j = 0; j < n; j++ )
				Ax[i] += A[i][j] * x[j];
		}

		return Ax;
	}

	/** Compute the dot product of two vectors, and return it.
	  * @throws IllegalArgumentException If the vectors are of different lengths.
	  */
	public static double dot( double[] x, double[] y )
	{
		if ( x.length != y.length )
			throw new IllegalArgumentException( "Matrix.dot: vectors have different lengths." );

		double xy = 0;
		for ( int i = 0; i < x.length; i++ )
			xy += x[i] * y[i];

		return xy;
	}

	/** Add the second vector to the first. 
	  * @throws IllegalArgumentException If the vectors are of different lengths.
	  * @return Nothing; the first vector contains the sum.
	  */
	public static void add( double[] x, double[] y )
	{
		if ( x.length != y.length )
			throw new IllegalArgumentException( "Matrix.add: vectors have different lengths." );

		for ( int i = 0; i < x.length; i++ )
			x[i] += y[i];
	}

	/** Compute a scalar times a vector plus another scalar times a vector.
	  * If <tt>y</tt> is <tt>null</tt>, then ignore it; so 
	  * <pre>
	  *   Matrix.axpby( a, x, 0, null );
	  * </pre>
	  * is a way to calculate <tt>x *= a</tt>.
	  *
	  * @throws IllegalArgumentException If the vectors are of different lengths.
	  * @return Nothing; the first vector contains the result.
	  */
	public static void axpby( double a, double[] x, double b, double[] y )
	{
		if ( y != null && x.length != y.length )
			throw new IllegalArgumentException( "Matrix.axpy: vectors have different lengths." );
		
		for ( int i = 0; i < x.length; i++ )
		{
			x[i] *= a;
			if ( y != null ) x[i] += b*y[i];
		}
	}
	
	/** Add the second matrix to the first.
	  * @throws IllegalArgumentException If matrices don't have compatible sizes.
	  * @return Nothing; the result is stored in the first matrix.
	  */
	public static void add( double[][] A, double[][] B )
	{
		if ( A.length != B.length || A[0].length != B[0].length )
			throw new IllegalArgumentException( "Matrix.add: matrix sizes don't match" );

		for ( int i = 0; i < A.length; i++ )
			for ( int j = 0; j < A[0].length; j++ )
				A[i][j] += B[i][j];
	}

	/** Compute the product of two matrices, and return it. Neither matrix
	  * is stomped.
	  * @throws IllegalArgumentException If matrices don't have compatible sizes.
	  */
	public static double[][] multiply( double[][] A, double[][] B ) 
	{
		if ( A[0].length != B.length )
			throw new IllegalArgumentException( "Matrix.multiply: sizes don't match" );

		int i, j, k;
		double[][] AB = new double[A.length][B[0].length];

		for ( i = 0; i < A.length; i++ )
			for ( j = 0; j < B[0].length; j++ )
			{
				double s = 0;
				for ( k = 0; k < A[0].length; k++ )
					s += A[i][k] * B[k][j];
				AB[i][j] = s;
			}

		return AB;
	}

	/** Compute the Cholesky decomposition of a matrix, and return it.
	  * Given an input matrix <code>A</code>, the result is a lower-triangular
	  * matrix <code>L</code> such that <code>L L' == A</code>.
	  * The input matrix <code>A</code> is not stomped.
	  * Use algorithm of Stoer and Bulirsch, _Intro. to Numerical Analysis_,
	  * New York: Springer Verlag (1980), Sec. 4.3.
	  * @throws Matrix.NotPositiveDefiniteException If the input matrix is not positive
	  *   definite.
	  */
	public static double[][] cholesky( double[][] A )
	{
		int i, j, k, n = A.length;
		double[] p = new double[n];
		
		double[][] L = (double[][]) A.clone();
		for ( i = 0; i < n; i++ )
			L[i] = (double[]) A[i].clone();

		// The following for-loops compute the lower-diagonal elements of L.
		// The diagonal elements of L are stored in p.

		for ( i = 0; i < n; i++ )
		{
			for ( j = i; j < n; j++ )
			{
				double x = L[i][j];
				for ( k = i-1; k >= 0; k-- )
					x -= L[j][k] * L[i][k];
				if ( i == j )
				{
					if ( x <= 0 ) 
						throw new NotPositiveDefiniteException( "Matrix.cholesky: matrix is not positive definite." );
					p[i] = 1/Math.sqrt(x);
				}
				else
				{
					L[j][i] = x * p[i];
				}
			}
		}

		// Now copy p into the diagonal elements of L, and clear out the
		// upper-diagonal. p[i] contains 1/L[i][i].

		for ( i = 0; i < n; i++ )
			L[i][i] = 1/p[i];

		for ( i = 0; i < n; i++ )
			for ( j = i+1; j < n; j++ )
				L[i][j] = 0;

		return L;
	}

	/** This function performs Gauss elimination on a system of n linear   
	  * equations.
	  * The upper triangular matrix of the reduced system is computed    
	  * and the multipliers are stored in the lower triangular part of   
	  * the coefficient matrix. Pivoting and scaling are used.
	  *
	  * @see gauss_solve
	  *                                                                   
	  * @author Tore Haavie: original C version in NUMLIBC
	  * @author Robert Dodier: Java translation
	  *                                                                   
	  * @param A  Pointer to array storing coefficient matrix. This
	  *   matrix is not stomped.
	  * @param piv  Array storing indices of pivot elements; this is written
	  *   by <code>gauss_elim</code>.
	  * @param determinant_sign Sign of the determinant of <code>A</code>,
	  *   either 1 or -1.
	  * @return Decomposed matrix.
	  * @throws Matrix.SingularMatrixException If the input matrix is singular.
	  */
	public static double [ ] [ ] gauss_elim ( double [ ] [ ] A, int piv [ ] , int [ ] determinant_sign )
	{
		double temp, aik;
		int i, j, k, l;

		int mode = 3;
		int n = A.length;
		double [ ] scalfac = new double [ n ];

		double [ ] [ ] a = ( double [ ] [ ] ) A.clone ( );
		for ( i = 0 ; i < n ; i++ )
			// Default clone() is apparently always a shallow clone... argh!
			a [ i ] = ( double [ ] ) A [ i ] .clone ( );

		/* Start elimination process. k counts elimination steps. */
		determinant_sign [ 0 ] = 1;
		for ( k = 0 ; k <= n-2 ; k++ )
		{
			/* Compute pointer to pivot element. */
			if ( mode == 0 ) piv [ k ] = k;
			else piv [ k ] = pivcalc ( a, k, mode, scalfac );

			/* Test for singularity. */
			l = piv [ k ];
			if ( a [ l ] [ k ] == 0 )
				throw new SingularMatrixException ( "Matrix.gauss_elim: matrix is singular." );

			/* Interchange rows k and piv[k] if required. */
			if ( mode != 0 )
			{
				l = piv [ k ];
				if ( l != k )
				{
					determinant_sign [ 0 ] = -determinant_sign [ 0 ];
					rowchange ( a, k, l );
				}
			}

			/* Interchange scalfactors if required. */
			if ( mode >= 2 )
			{
				temp = scalfac [ l ];
				scalfac [ l ] = scalfac [ k ];
				scalfac [ k ] = temp;
			}

			/* Perform eliminations on the rows i = k+1(1)n-1. */
			eliminate ( a, k );
		}

		/* Test for singularity in last step. */
		if ( a [ n-1 ] [ n-1 ] == 0. )
			throw new SingularMatrixException( "Matrix.gauss_elim: matrix is singular." );

		return a;
	}

	/** This function is used by gauss_elim() to compute the pivot element
	  * and perform scaling if wanted. As called from <code>gauss_elim</code>,
	  * scaling is always used (<code>mode==3</code>).
	  */
	static int pivcalc ( double [ ] [ ] a, int k, int mode, double scalfac [ ] )
	{
		double temp, cof;
		int i, l, n = a.length;
		temp = 0.;
		l = k;

		for ( i = k ; i <= n-1 ; i++ )
		{
			cof = Math.abs ( a [ i ] [ k ] );
			if ( mode >= 2 && k == 0 ) scalfac [ i ] =maxelement ( a, i, k );
			if ( ( mode == 2 ) || ( mode == 3 && k == 0 ) ) cof /= scalfac [ i ];
			if ( mode == 3 && k>0 ) cof /= maxelement ( a, i, k );
			if ( temp < cof )
			{
				temp = cof;
				l = i;
			}
		}
		return ( l );
	}

	/** This function is used by pivcalc() to compute the element largest
	  * in absolute value along the i'th row in the remaining system of equa-
	  * tions.
	  */
	static double maxelement ( double [ ] [ ] a, int i, int k )
	{
		double temp, cof;
		int l, n = a.length;
		temp = -1e9;

		for ( l = k ; l <= n-1 ; l++ )
		{
			cof = Math.abs ( a [ i ] [ l ] );
			if ( cof > temp ) temp = cof;
		}
		return ( temp );
	}

	/** This function is used by gauss_elim() for interchanging the k'th 
	  * and l'th row in the remaining coefficient matrix.
	  */
	static void rowchange ( double [ ] [ ] a, int k, int l )
	{
		double temp;
		int i, n = a.length;
		for ( i = k ; i <= n-1 ; i++ )
		{
			temp = a [ l ] [ i ];
			a [ l ] [ i ] = a [ k ] [ i ];
			a [ k ] [ i ] = temp;
		}
	}

	/** This function performs elimination on the rows i = k+1(1)n-1.  
	  */
	static void eliminate ( double [ ] [ ] a, int k )
	{
		double temp;
		int i, j, n = a.length;

		/* i counts the rows in the reduced system of equations, j counts the
		 elements in the row.
		 */
		for ( i = k+1 ; i <= n-1 ; i++ )
		{
			temp = a [ i ] [ k ] /a [ k ] [ k ];
			a [ i ] [ k ] = temp;

			/* Store new multiplier. */
			for ( j = k+1 ; j <= n-1 ; j++ )
				a [ i ] [ j ] = a [ i ] [ j ] - temp*a [ k ] [ j ];
		}
	}

	/**  This function, using the matrix decomposition computed by
	  *  <code>gauss_elim</code>, computes the solution for a given right-hand side.  
	  *  The calculation is performed in two steps:                       
	  *  <ol>                                                                
	  *  <li> The right-hand side <code>b</code> is modified using the      
	  *     Gaussian multipliers stored in the lower triangular part of   
	  *     the decomposed matrix.
	  *  <li> The solution vector <code>x</code> is computed by back
	  *     substitution using the coefficient matrix stored in the upper
	  *     triangular part of the decomposed matrix and the right-hand side,
	  *     computed in step 1, stored in <code>b</code>.
	  *  </ol>                                                                
	  *                                                                   
	  * @see gauss_elim
	  *
	  * @author Tore Haavie: original C version in NUMLIBC
	  * @author Robert Dodier: Java translation
	  *
	  * @param A_decomposed Decomposed matrix containing coefficients and multipliers.
	  * @param piv Array storing pointers to pivot elements.     
	  * @param b Array storing the right-hand side of the system of equations.
	  * @param x The computed solution of the system.
	  */
	public static void gauss_solve ( double [ ] [ ] A_decomposed, int piv [ ] , double b [ ] , double x [ ] )
	{
		// Modify right-hand side of system of equations. 
		modify ( A_decomposed, piv, b );

		// Perform back substitution. 
		solve ( A_decomposed, b, x );
	}

	/** This function is used by gauss_solve() to modify the right-hand    
	  * side of the system of equations using the Gaussian multipliers    
	  * stored in the lower triangular part of the a-matrix.              
	  */
	static void modify ( double [ ] [ ] a, int piv [ ] , double b [ ] )
	{
		double temp;
		int k, i, integ, n = a.length;

		for ( k = 0 ; k <= n-2 ; k++ )
		{
			integ = piv [ k ];
			if ( k != integ )
			{
				temp = b [ k ];
				b [ k ] = b [ integ ];
				b [ integ ] = temp;
			}

			for ( i = k+1 ; i <= n-1 ; i++ )
				b [ i ] -= a [ i ] [ k ] *b [ k ];
		}
	}

	/** This function is used by gauss_solve() to compute the solution     
	  * vector x of the upper triangular system of
	  * equations with the coefficient matrix stored in the upper trangu- 
	  * lar part of the a-matrix and with the right hand side stored in   
	  * in the b-vector.                                                  
	  */
	static void solve ( double [ ] [ ] a, double b [ ] , double x [ ] )
	{
		int i, k, n = a.length;
		double temp;

		for ( i = n-1 ; i >= 0 ; i-- )
		{
			temp = b [ i ];
			if ( i < n-1 )
			{
				for ( k = i+1 ; k <= n-1 ; k++ )
					temp -= a [ i ] [ k ] *x [ k ];
			}
			x [ i ] = temp/a [ i ] [ i ];
		}
	}

	/** Compute the inverse of a matrix, and return it.
	  * The input matrix <code>A</code> is not stomped.
	  * @throws Matrix.SingularMatrixException If the input matrix is singular.
	  */
	public static double[][] inverse( double[][] A )
	{
		int	i, j, n = A.length;
		int[] pivot = new int[n];
		double[] column = new double[n], ej = new double[n];

		int[] determinant_sign = new int[1];
		double[][] A_decomposed = gauss_elim( A, pivot, determinant_sign );
		double[][] A_inverse = new double[n][n];

		for ( j = 0; j < n; j++ )
		{
			for ( i = 0; i < n; i++ )
				ej[i] = 0;
			ej[j] = 1;
			gauss_solve( A_decomposed, pivot, ej, column );
			for ( i = 0; i < n; i++ )
				A_inverse[i][j] = column[i];
		}

		return A_inverse;
	}

	/** Return the determinant of a matrix. The matrix is not stomped.
	  * This function first computes the Gaussian decomposition of the
	  * matrix <code>A</code>, then computes the determinant.
	  * @throws Matrix.SingularMatrixException If the matrix is singular.
	  */
	public static double determinant( double[][] A )
	{
		int n = A.length;
		int[] pivots = new int[n];
		int[] determinant_sign = new int[1];

		double[][] A_decomposed = Matrix.gauss_elim( A, pivots, determinant_sign );

		double det = determinant_sign[0];
		for ( int i = 0; i < n; i++ )
			det *= A_decomposed[i][i];

		return det;
	}

	/** Return the determinant of a matrix which has already been decomposed.
	  * It is assumed that <code>A_decomposed</code> is the Gaussian
	  * decomposition of the matrix for which we want the determinant.
	  * The other argument <code>determinant_sign</code> is an output of
	  * <code>gauss_elim</code>.
	  * @see gauss_elim
	  */
	public static double determinant( double[][] A_decomposed, int[] determinant_sign )
	{
		int n = A_decomposed.length;
		double det = determinant_sign[0];
		for ( int i = 0; i < n; i++ )
			det *= A_decomposed[i][i];

		return det;
	}

	/** Output a vector in a nice format.
	  */
	public static void pretty_output( double[] x, OutputStream os, String ws )
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );
		for ( int i = 0; i < x.length; i++ )
			dest.print( x[i]+ws );
	}

	/** Output a matrix in a nice format.
	  */
	public static void pretty_output( double[][] A, OutputStream os, String leading_ws )
	{
		PrintStream dest = new PrintStream( new DataOutputStream(os) );

		for ( int i = 0; i < A.length; i++ )
		{
			dest.print( leading_ws );
			for ( int j = 0; j < A[i].length; j++ )
				dest.print( A[i][j]+" " );
			dest.println("");
		}
	}
}

