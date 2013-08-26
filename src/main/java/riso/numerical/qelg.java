package riso.numerical;

/** qagse.java, qags.java, qelg.java, qk21.java, and qpsrt.java are
  * derivative works (translations) of Fortran code by Robert Piessens
  * and Elise de Doncker. These five files are released under GPL
  * by permission of Robert Piessens. In response to my question,
  * <pre>
  *  >I would like to have your permission to distribute my
  *  >Java translation of your QUADPACK routines under the
  *  >terms of the GPL. Do I have your permission to do so?
  * </pre>
  * Robert Piessens writes:
  * <pre>
  *  Date:  Mon, 28 Jan 2002 14:41:58 +0100
  *  To: "Robert Dodier" <robert_dodier@yahoo.com>
  *  From: "Robert Piessens" <Robert.Piessens@cs.kuleuven.ac.be>
  *  Subject: Re: Permission to redistribute QUADPACK translation?
  *  
  *  OK, You have my permission.
  *  
  *  Robert Piessens
  * </pre>
  */
public class qelg implements java.io.Serializable
{

	public static void do_qelg ( int [ ] n , double [ ] epstab , double [ ] result , double [ ] abserr , double [ ] res3la , int [ ] nres )
	{
		double delta1, delta2, delta3, epmach, epsinf = -1, error, err1, err2, err3, e0, e1, e1abs, e2, e3, oflow, res, ss = -1, tol1, tol2, tol3;
		int i, ib, ib2, ie, indx, k1, k2, k3, limexp, newelm, num;
		epmach = qk21.D1MACH [ 4-1 ];
		oflow = qk21.D1MACH [ 2-1 ];
		nres[0] = nres[0] +1;
		abserr[0] = oflow;
// System.err.println( "do_qelg: assign epstab["+(n[0]-1)+"] == "+epstab[n[0]-1]+" tp result" );
		result[0] = epstab [ n[0] -1 ];
		if ( n[0] < 3 )
		{
			abserr[0] = Math.max ( abserr[0] , 5 * epmach * Math.abs ( result[0] ) );
			return;
		}
		limexp = 50;
		epstab [ n[0] + 2 -1 ] = epstab [ n[0] -1 ];
		newelm = ( n[0] - 1 ) / 2;
		epstab [ n[0] -1 ] = oflow;
		num = n[0];
		k1 = n[0];
		for ( i = 1 ; i <= newelm ; i += 1 )
		{
			k2 = k1-1;
			k3 = k1-2;
			res = epstab [ k1 + 2 -1 ];
			e0 = epstab [ k3 -1 ];
			e1 = epstab [ k2 -1 ];
			e2 = res;
			e1abs = Math.abs ( e1 );
			delta2 = e2-e1;
			err2 = Math.abs ( delta2 );
			tol2 = Math.max ( Math.abs ( e2 ) , e1abs ) * epmach;
			delta3 = e1-e0;
			err3 = Math.abs ( delta3 );
			tol3 = Math.max ( e1abs , Math.abs ( e0 ) ) * epmach;
			if ( ! ( err2 > tol2 || err3 > tol3 ) )
			{
// System.err.println( "do_qelg: assign res == "+res+" tp result" );
				result[0] = res;
				abserr[0] = err2+err3;
				abserr[0] = Math.max ( abserr[0] , 5 * epmach * Math.abs ( result[0] ) );
				return;
			}
			e3 = epstab [ k1 -1 ];
			epstab [ k1 -1 ] = e1;
			delta1 = e1-e3;
			err1 = Math.abs ( delta1 );
			tol1 = Math.max ( e1abs , Math.abs ( e3 ) ) * epmach;
			boolean goto20 = false;
			if ( err1 <= tol1 || err2 <= tol2 || err3 <= tol3 )
			{
				goto20 = true;
			}
			else
			{
				ss = 1/delta1+1/delta2-1/delta3;
				epsinf = Math.abs ( ss * e1 );
			}
			if ( goto20 || ! ( epsinf > 1e-4 ) )
			{
				n[0] = i+i-1;
			}
			else
			{
				res = e1+1/ss;
				epstab [ k1 -1 ] = res;
				k1 = k1-2;
				error = err2 + Math.abs ( res - e2 ) + err3;
				if ( error > abserr[0] ) continue;
				abserr[0] = error;
// System.err.println( "do_qelg: assign (#2) res == "+res+" tp result" );
				result[0] = res;
			}
		}
		if ( n[0] == limexp ) n[0] = 2 * ( limexp / 2 ) - 1;
		ib = 1;
		if ( ( num / 2 ) * 2 == num ) ib = 2;
		ie = newelm+1;
		for ( i = 1 ; i <= ie ; i += 1 )
		{
			ib2 = ib+2;
			epstab [ ib -1 ] = epstab [ ib2 -1 ];
			ib = ib2;
		}
		if ( ! ( num == n[0] ) )
		{
			indx = num-n[0] +1;
			for ( i = 1 ; i <= n[0] ; i += 1 )
			{
				epstab [ i -1 ] = epstab [ indx -1 ];
				indx = indx+1;
			}
		}
		if ( ! ( nres[0] >= 4 ) )
		{
			res3la [ nres[0] -1 ] = result[0];
			abserr[0] = oflow;
			abserr[0] = Math.max ( abserr[0] , 5 * epmach * Math.abs ( result[0] ) );
			return;
		}
		abserr[0] = Math.abs ( result[0] - res3la [ 3 -1 ] ) + Math.abs ( result[0] - res3la [ 2 -1 ] ) + Math.abs ( result[0] - res3la [ 1 -1 ] );
		res3la [ 1 -1 ] = res3la [ 2 -1 ];
		res3la [ 2 -1 ] = res3la [ 3 -1 ];
		res3la [ 3 -1 ] = result[0];
		abserr[0] = Math.max ( abserr[0] , 5 * epmach * Math.abs ( result[0] ) );
		return;
	}
}
