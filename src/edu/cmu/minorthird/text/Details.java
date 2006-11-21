package edu.cmu.minorthird.text;

import java.io.Serializable;

/**
 * Detailed information about assertions in a TextLabels object
 *
 * @author William Cohen
 */

public class Details implements Serializable
{
    static private final long serialVersionUID = 1;
    private final int CURRENT_VERSION_NUMBER = 1;

    /** Default 'details' record for facts. */
    static final public Details DEFAULT = new Details();

    private double confidence;
    private Object author;
	
    public Details() { this(1.0,"unknown");	}

    public Details(double confidence) { this(confidence,"unknown");	}

    public Details(double confidence,Object author) {
        this.confidence = confidence;
        this.author = author;
    }
		 
    public double getConfidence() { return confidence; }
    public Object author() { return author; }

}
