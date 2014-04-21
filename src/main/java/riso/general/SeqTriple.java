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

public class SeqTriple implements java.io.Serializable
{
	int level, reps;
	Class c;

	public SeqTriple( String s, int reps )
	{
		try
		{
			c = Class.forName(s);
			level = nsuperclasses(c);
			this.reps = reps;
		}
		catch (Exception e) { e.printStackTrace(); System.exit(1); } // !!!
	}

	public static int nsuperclasses( Class c ) throws ClassNotFoundException
	{
		int n = 0;
		while ( (c = c.getSuperclass()) != null )
			++n;
		return n;
	}

	public String toString()
	{
		return "["+level+","+c.getName()+","+reps+"]";
	}
}
