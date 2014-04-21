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
public class qpsrt implements java.io.Serializable
{
	public static void do_qpsrt ( int limit, int last, int[] maxerr, double[] ermax, double[] elist, int[] iord, int[] nrmax )
	{
		double errmax, errmin;
		int i = 0, ibeg, ido, isucc, j, jbnd, jupbn, k;
		if ( ! ( last > 2 ) )
		{
			iord [ 1 -1 ] = 1;
			iord [ 2 -1 ] = 2;
			maxerr[0] = iord [ nrmax[0] -1 ];
			ermax[0] = elist [ maxerr[0] -1 ];
			return;
		}
		errmax = elist [ maxerr[0] -1 ];
		if ( ! ( nrmax[0] == 1 ) )
		{
			ido = nrmax[0]-1;
			for ( i = 1 ; i <= ido ; i += 1 )
			{
				isucc = iord [ nrmax[0] - 1 -1 ];
				if ( errmax <= elist [ isucc -1 ] ) break;
				iord [ nrmax[0] -1 ] = isucc;
				nrmax[0] = nrmax[0]-1;
			}
		}
		jupbn = last;
		if ( last > ( limit / 2 + 2 ) ) jupbn = limit + 3 - last;
		errmin = elist [ last -1 ];
		jbnd = jupbn-1;
		ibeg = nrmax[0]+1;
		boolean goto60 = false;
		if ( ! ( ibeg > jbnd ) )
		{
			for ( i = ibeg ; i <= jbnd ; i += 1 )
			{
				isucc = iord [ i -1 ];
				if ( errmax >= elist [ isucc -1 ] )
				{
					goto60 = true;
					break;
				}
				iord [ i - 1 -1 ] = isucc;
			}
		}
		if ( !goto60 )
		{
			iord [ jbnd -1 ] = maxerr[0];
			iord [ jupbn -1 ] = last;
			maxerr[0] = iord [ nrmax[0] -1 ];
			ermax[0] = elist [ maxerr[0] -1 ];
			return;
		}
		iord [ i - 1 -1 ] = maxerr[0];
		k = jbnd;
		boolean goto80 = false;
		for ( j = i ; j <= jbnd ; j += 1 )
		{
			isucc = iord [ k -1 ];
			if ( errmin < elist [ isucc -1 ] )
			{
				goto80 = true;
				break;
			}
			iord [ k + 1 -1 ] = isucc;
			k = k-1;
		}
		if ( !goto80 )
		{
			iord [ i -1 ] = last;
			maxerr[0] = iord [ nrmax[0] -1 ];
			ermax[0] = elist [ maxerr[0] -1 ];
			return;
		}
		iord [ k + 1 -1 ] = last;
		maxerr[0] = iord [ nrmax[0] -1 ];
		ermax[0] = elist [ maxerr[0] -1 ];
		return;
	}
}
