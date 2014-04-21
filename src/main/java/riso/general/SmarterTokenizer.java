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

/** Set up a tokenizer the way we like it.
  * In particular, ignore comments beginning with "<tt>%</tt>"; do not
  * parse numbers (use <tt>Double.parseDouble</tt> or <tt>Integer.parseInt</tt> for that); make the special chars
  * <tt>+-./#$?@[\]_:</tt> word characters; and use <tt>"</tt> for
  * quoted strings.
  */
public class SmarterTokenizer extends StreamTokenizer
{
	protected Reader reader;
	public Hashtable string_to_numeric = null;

	// Quick reference: ascii character set.
	// 32= !"#$%&'()*+,-./0123456789:;<=>?@=64
	// 65=ABCDEFGHIJKLMNOPQRSTUVWXYZ=90
	// 91=[\]^_`=96
	// 97=abcdefghijklmnopqrstuvwxyz=122
	// 123={|}~=126

	public SmarterTokenizer( Reader r )
	{
		super(r);
		reader = r;

		// Set tokenizer so all printable ascii chars are `ordinary.'
		// This means that stream becomes a sequence of 1-char tokens.

		ordinaryChars( ' ', '~' );

		// Now assign all of the letters, digits, and some of the
		// special characters to be word parts.
		// Use parse* methods of Double, Int, or Long to parse numbers;
		// don't rely on StreamTokenizer.

		wordChars( 'A', 'Z' );
		wordChars( 'a', 'z' );
		wordChars( '0', '9' );
		wordChars( '-', '/' );	// '-', '.', '/'
		wordChars( '#', '$' );	// '#', '$'
		wordChars( '?', '@' );	// '?', '@'
		wordChars( '[', ']' );	// '[', '\', ']'
		wordChars( '_', '_' );	
		wordChars( ':', ':' );	
		wordChars( '+', '+' );	

		commentChar( '%' );

		quoteChar( '"' );

		whitespaceChars( ' ', ' ' );
		whitespaceChars( '\t', '\t' );
		whitespaceChars( '\n', '\n' );
		whitespaceChars( '\r', '\r' );
	}

	/** Parse the next token in the input stream as a number.
	  * Note that in this class, the function of parsing a number is separate
	  * from that of parsing a string; the two functions are combined in
	  * <tt>StreamTokenizer</tt>, but are separated here (1) so as to avoid
	  * breaking existing code which parses strings and then calls <tt>Double.parseDouble</tt>,
	  * and (2) so that string to numeric lookups are only called when needed, instead
	  * of calling <tt>Hashtable.get</tt> on every token; <tt>nextToken</tt> is slow
	  * enough as it is.
	  */
	public int nextNumber() throws IOException
	{
		nextToken();

		if ( string_to_numeric != null )
		{
			Double x;
			if ( (x = (Double) string_to_numeric.get( sval )) != null )
			{
				nval = x.doubleValue();
				ttype = TT_NUMBER;
				return ttype;
			}
		}

		nval = Double.parseDouble( sval );
		ttype = TT_NUMBER;
		return ttype;
	}

	/** Parses the next block of input as a string. The <tt>ttype</tt>
	  * is <tt>TT_WORD</tt> and <tt>sval</tt> is set to the string.
	  * A ``block'' is a sequence of tokens between matching curly braces,
	  * and includes the braces.
	  */
	public int nextBlock() throws IOException
	{
		MultipleBuffer multi_buffer = new MultipleBuffer();
		int bracket_level = 0;
		int nchar = 0;
		int c;

		do
		{
			c = reader.read();
			if ( c == -1 ) break;
			multi_buffer.store_to_buffer( (char)c );
			++nchar;
			if ( c == '{' )
				++bracket_level;
			else if ( c == '}' )
				--bracket_level;
		}
		while ( c != '}' || bracket_level > 0 );

		if ( nchar > 0 )
		{
			sval = multi_buffer.toString();
			ttype = TT_WORD;
			return ttype;
		}
		else
		{
			sval = null;
			ttype = TT_EOF;
			return ttype;
		}
	}

	public static void main( String[] args )
	{
		try
		{
			SmarterTokenizer st = new SmarterTokenizer( new InputStreamReader( System.in ) );
			for ( st.nextBlock(); st.ttype != StreamTokenizer.TT_EOF; st.nextBlock() )
				System.out.println( "tokenizer: "+st );
		}
		catch (Exception e) { e.printStackTrace(); }
	}
}

class MultipleBuffer
{
	SimpleBuffer current_buffer = null;
	Vector buffer_list = new Vector();

	MultipleBuffer()
	{
		current_buffer = new SimpleBuffer();
		buffer_list.addElement( current_buffer );
	}

	void store_to_buffer( char c )
	{
		if ( ! current_buffer.store_to_buffer( c ) )
		{
			current_buffer = new SimpleBuffer();
			buffer_list.addElement( current_buffer );
			current_buffer.store_to_buffer( c );
		}
	}

	public String toString()
	{
		int i, ii, total_size = (buffer_list.size()-1)*SimpleBuffer.BUFFER_SIZE + current_buffer.count;
		char[] total_buffer = new char[ total_size ];

		for ( i = 0, ii = 0; i < buffer_list.size(); i++ )
		{
			SimpleBuffer sb = (SimpleBuffer) buffer_list.elementAt(i);
			System.arraycopy( sb.buffer, 0, total_buffer, ii, sb.count );
			ii += sb.count;
		}

		return new String( total_buffer );
	}
}

class SimpleBuffer
{
	static final int BUFFER_SIZE = 4096;

	int count = 0;
	char[] buffer = new char[ BUFFER_SIZE ];

	boolean store_to_buffer( char c )
	{
		if ( count < BUFFER_SIZE )
		{
			buffer[ count++ ] = c;
			return true;
		}
		else
			return false;
	}
}

