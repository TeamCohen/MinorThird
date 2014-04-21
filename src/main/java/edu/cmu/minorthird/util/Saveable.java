package edu.cmu.minorthird.util;

import java.io.*;

/**
 * Interface for classes that can be saved to disk in one or more
 * class-specific, human-readable format.  The format is determined by
 * file extensions.
 */

public interface Saveable 
{
	/** List of formats in which the object can be saved. */
    public String[] getFormatNames();

	/** Recomended extension for the format with the given name. */
	public String getExtensionFor(String formatName);

	/** Save this object to the given file, in the given format. */
	public void saveAs(File file, String formatName) throws IOException;

	/** Restore the object from a file. */
	public Object restore(File file) throws IOException;
}
