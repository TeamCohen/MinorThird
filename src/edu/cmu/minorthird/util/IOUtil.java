/* Copyright 2003, Carnegie Mellon, All Rights Reserved */

package edu.cmu.minorthird.util;

import java.io.*;

/**
 * IO utilities.
 *
 */
public class IOUtil
{
	static public boolean saveSomehow(Object obj,File file)
	{
		return saveSomehow(obj,file,false);
	}

	static public boolean saveSomehow(Object obj,File file,boolean complainAboutProblems)
	{
		try {
			if (obj instanceof Saveable) {
				Saveable saveObj = (Saveable)obj;
				String defaultFormat = saveObj.getFormatNames()[0];
				saveObj.saveAs( file, defaultFormat );
				return true;
			} else if (obj instanceof Serializable) {
				IOUtil.saveSerialized((Serializable)obj,file);
				return true;
			} else {
				if (complainAboutProblems) {
					System.out.println("don't know how to save object of type "+obj.getClass());
				}
				return false;
			}
		} catch (IOException ex) {
			if (complainAboutProblems) {
				System.out.println("exception saving object of type "+obj.getClass());
				ex.printStackTrace();
			}
			return false;
		}
	}

	static public void saveSerialized(Serializable obj,File file) throws IOException {
		ObjectOutputStream out =
			new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
		out.writeObject(obj);
		out.close();
	}
	static public Serializable loadSerialized(File file) throws IOException
	{
		try {
			return loadSerialized(new FileInputStream(file));
			/*ObjectInputStream in =
				new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
				Object obj = in.readObject();
				in.close();
				return (Serializable)obj;
			 */
		}	catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("can't read serialized object from "+file+": "+e);
		}
	}

	public static Serializable loadSerialized(InputStream input) throws IOException
	{
		try
		{
			ObjectInputStream in =
				new ObjectInputStream(new BufferedInputStream(input));
			Object obj = in.readObject();
			in.close();
			return (Serializable)obj;
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalArgumentException("can't read serialized object from "+input+": "+e);
		}
	}

	/**
	 * Reads a file and converts it to a String via a byte array and inputStream.available()
	 * I'm not positive that inputStream.available() works the same under multi-threading
	 * @param in - File object to read - should be character data
	 * @return String a string version of the data
	 */
	public static String readFile(File in) throws IOException
	{
		/*
	  InputStream inputStream = new FileInputStream(in);
	  byte[] bytes = new byte[inputStream.available()];
	  inputStream.read(bytes);
	  inputStream.close();
	  return new String(bytes);
		 */

		//Richard's implementation, this may be more thread safe than using inputStream.available()
		String line = null;
		StringBuffer content = new StringBuffer("");
		//if (debug) System.out.println("Reading " + in);
		try
		{
			BufferedReader bReader = new BufferedReader(new FileReader(in));
			while ((line = bReader.readLine()) != null)
				content.append(line + "\n");
			bReader.close();
		}
		catch (IOException ioe)
		{
			System.err.println("Error reading " + in + ": " + ioe);
		}
		return content.toString();
	}

}
