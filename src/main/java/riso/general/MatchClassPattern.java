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
import java.util.*;

public class MatchClassPattern
{
	// This one remains as an example.
	static SeqTriple[] sm3 = {
		new SeqTriple( "riso.distributions.AbstractConditionalDistribution", 1 ),
		new SeqTriple( "riso.distributions.Gamma", -1 ),
		new SeqTriple( "riso.distributions.Gaussian", 4 ),
		new SeqTriple( "riso.distributions.GaussianDelta", 4 )
	};

	public static void main( String[] args )
	{
		try
		{
			Vector sm = new Vector();
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( new FileInputStream( args[0] ) ) );
			st.nextToken();
			while ( st.ttype != StreamTokenizer.TT_EOF )
			{
				String s = st.sval;
				st.nextToken();
				int n = Integer.parseInt( st.sval );
				sm.addElement( new SeqTriple(s,n) );
				st.nextToken();
			}
	
			SeqTriple[] sm0 = new SeqTriple[ sm.size() ];
			sm.copyInto(sm0);

			for ( int i = 0; i < sm0.length; i++ )
				System.err.println( sm0[i] );

			Vector seq = new Vector();
			st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			st.nextToken();

			while ( st.ttype != StreamTokenizer.TT_EOF )
			{
				Class c = Class.forName( st.sval );
				System.err.println( "add: "+c.getName() );
				seq.addElement(c);
				st.nextToken();
			}

			int[] class_specific_score = new int[1], count_specific_score = new int[1];
			boolean does_match = matches(sm0,seq,class_specific_score,count_specific_score);
			System.err.println( "matches sm0: "+does_match+(does_match?(", class score: "+class_specific_score[0]+", count score: "+count_specific_score[0]):"") );
		}
		catch (Exception e) { System.err.println( "e: "+e ); }
	}

	public static boolean matches( SeqTriple[] sm, Vector seq, int[] class_specific_score, int[] count_specific_score )
	{
		int ii = 0, n = 0;
		class_specific_score[0] = 0;
		count_specific_score[0] = 0;

		for ( Enumeration e = seq.elements(); e.hasMoreElements(); )
		{
			Class seqc = (Class) e.nextElement();

			if ( ! sm[ii].c.isAssignableFrom(seqc) )
				if ( sm[ii].reps == -1 )
				{
					n = 0;
					if ( ++ii == sm.length )
					{
// System.err.println( "reached end of pattern w/ seqc "+seqc.getName() );
						return false;
					}
				}
				else
				{
// System.err.println( seqc.getName()+" not instance of "+sm[ii].c.getName()+"; found "+n+" before failing" );
					return false;
				}

// System.err.println( "add "+sm[ii].level+" to class-specific score; from "+sm[ii].c.getName() );
			class_specific_score[0] += sm[ii].level;

			if ( sm[ii].c.isAssignableFrom(seqc) )
			{
				if ( ++n == sm[ii].reps )
				{
// System.err.println( "reached end of "+sm[ii].reps+" "+sm[ii].c.getName() );
					n = 0;
					if ( ++ii == sm.length && e.hasMoreElements() )
					{
// System.err.println( "reached end of pattern w/ seqc "+seqc.getName() );
						return false;
					}
					++count_specific_score[0];
				}
			}
			else
			{
// System.err.println( seqc.getName()+" not instance of "+sm[ii].c.getName()+"; found "+n+" before failing" );
				return false;
			}
		}

// System.err.println( "fell out at bottom, ii: "+ii+", n: "+n );
		if ( ii == sm.length && n == sm[ii-1].reps )
			++count_specific_score[0];

		if ( ii < sm.length-1 )
		{
// System.err.println( "didn't reach end of pattern; ii: "+ii+", sm.length: "+sm.length );
			return false;
		}

		if ( ii < sm.length && n != sm[ii].reps && sm[ii].reps != -1 ) 
		{
// System.err.println( "reached end of pattern w/ n "+n+" (should be "+sm[ii].reps+")" );
			return false;
		}

		return true;
	}
}
