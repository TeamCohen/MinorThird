package edu.cmu.minorthird.util;

import java.io.*;

/**
 * Marker interface for classes that can be saved to disk
 * in some class-specific, human-readable format.
 */

public interface Saveable 
{
	/** Save this object to the given file. */
	public void saveAs(File file) throws IOException;
}
