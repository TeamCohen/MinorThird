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

import java.lang.reflect.*;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class RemoteApp
{
	public static void main( String[] args )
	{
		String url = null, classname = null;
		Vector app_args_vec = new Vector();

		for ( int i = 0; i < args.length; i++ )
		{
			switch ( args[i].charAt(1) )
			{
			case 'u': url = args[++i]; break;
			case 'c': classname = args[++i]; break;
			case 'a':
				// Assume the remaining args are for the remote app.
				for ( i++; i < args.length; i++ )
					app_args_vec.addElement( args[i] );
				break;
			}
		}

		System.err.println( "RemoteApp: url: "+url+", classname: "+classname );

		String[] app_args = new String[ app_args_vec.size() ];
		app_args_vec.copyInto( app_args );

		try
		{
			System.setSecurityManager( new NullSecurityManager() );
			Class c;

			if ( url != null )
				c = RMIClassLoader.loadClass( new URL(url), classname );
			else
				// Use java.rmi.server.codebase property.
				c = RMIClassLoader.loadClass( classname );

			invokeMain( c, app_args );
		}
		catch (Exception e) { e.printStackTrace(); }

		System.err.println( "RemoteApp: complete." );
		// System.exit(0);
	}

	public static void invokeMain(Class c, String[] args)
	{
		try
		{
			Method m = c.getMethod ("main", new Class[] {String[].class} );

			// Since "main" is always a static method, supply null as the object.
			try { m.invoke (null, new Object[] { args }); }
			catch (InvocationTargetException ite)
			{
				System.err.println( "invokeMain: invocation failed; " );
				ite.getTargetException().printStackTrace();
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		catch (NoSuchMethodException e) { e.printStackTrace(); }
	}
}

class NullSecurityManager extends SecurityManager {
    public void checkCreateClassLoader() { } 
    public void checkAccess(Thread g) { }
    public void checkAccess(ThreadGroup g) { }
    public void checkExit(int status) { }
    public void checkExec(String cmd) { }
    public void checkLink(String lib) { }
    public void checkRead(FileDescriptor fd) { }
    public void checkRead(String file) { }
    public void checkRead(String file, Object context) { }
    public void checkWrite(FileDescriptor fd) { }
    public void checkWrite(String file) { }
    public void checkDelete(String file) { }
    public void checkConnect(String host, int port) { }
    public void checkConnect(String host, int port, Object context) { }
    public void checkListen(int port) { }
    public void checkAccept(String host, int port) { }
    public void checkMulticast(InetAddress maddr) { }
    public void checkMulticast(InetAddress maddr, byte ttl) { }
    public void checkPropertiesAccess() { }
    public void checkPropertyAccess(String key) { }
    public void checkPropertyAccess(String key, String def) { }
    public boolean checkTopLevelWindow(Object window) { return true; }
    public void checkPrintJobAccess() { }
    public void checkSystemClipboardAccess() { }
    public void checkAwtEventQueueAccess() { }
    public void checkPackageAccess(String pkg) { }
    public void checkPackageDefinition(String pkg) { }
    public void checkSetFactory() { }
    public void checkMemberAccess(Class clazz, int which) { }
    public void checkSecurityAccess(String provider) { }
}	
