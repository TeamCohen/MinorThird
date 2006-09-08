import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.apache.log4j.*;

/** Feature extractor used by the learners */
public class ImgPtrFE implements SpanFeatureExtractor, Serializable
{
    private int windowSize=3;
    public Instance extractInstance(Span s)	{
        throw new UnsupportedOperationException("can't!");
    }
    public Instance extractInstance(TextLabels labels, Span s)	{
        FeatureBuffer buf = new FeatureBuffer(labels,s);
        SpanFE.from(s,buf).tokens().emit(); 
        for (int i=0; i<windowSize; i++) {
            SpanFE.from(s,buf).tokens().emit();
            SpanFE.from(s,buf).tokens().prop("cap").emit();
            SpanFE.from(s,buf).left().token(-i-1).emit(); 
            SpanFE.from(s,buf).left().token(-i-1).prop("cap").emit(); 
            SpanFE.from(s,buf).right().token(i).emit(); 
            SpanFE.from(s,buf).right().token(i).prop("cap").emit(); 
        }
        return buf.getInstance();
    }
};
