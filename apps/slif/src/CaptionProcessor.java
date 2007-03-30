import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.learn.experiments.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.apache.log4j.*;

// 'from' => ["Y-caption.txt"],
// 'produces' => ["proteinInPanelLabeled Y","cellInPanelLabeled Y", "Y-label.txt"],

public class CaptionProcessor
{
	static final boolean DEBUG = true;
  static { Mixup.maxNumberOfMatchesPerToken = 20; }

	private static Annotator regionalAnnotator,localAnnotator;
	private static MixupProgram scopingProgram, cellTypeProgram, proteinProgram;
	static {
		try {
			System.out.println("load scope.mixup..");
			scopingProgram = new MixupProgram(new File("scope.mixup"));
			System.out.println("load cell.mixup...");
			cellTypeProgram = new MixupProgram(new File("cell.mixup"));
			System.out.println("load protein.mixup...");
			proteinProgram = new MixupProgram(new File("protein.mixup"));
			System.out.println("load learned annotators...");
			regionalAnnotator = loadAnnotator("regional");
			localAnnotator = loadAnnotator("local");
		} catch (Exception e) {
			throw new IllegalStateException("mixup or io error: "+e);
		}
	}

	//
	// private data
	//
	private Map imgPtrForScope;
	private List imagePtrList;
	private Map imagePtrDefinition;
	private Set allLabels;
	private List imagePtrEntityPairs;

	public void processCaption(String caption,String proteinFile,String cellFile, String labelFile)
	{
		BasicTextBase base = new BasicTextBase();
		base.loadDocument("theCaption",caption);
		MonotonicTextLabels labels = new BasicTextLabels(base);
		processLabels(labels,proteinFile,cellFile,labelFile);
	}

    public void processLabels(MonotonicTextLabels labels,String proteinFile,String cellFile, String labelFile)
    {
        MixupInterpreter interp = new MixupInterpreter();
        if (DEBUG) System.out.println("feature construction...");
        interp.setProgram(LearnImagePtrExtractor.featureProgram);
        interp.eval(labels);
        if (DEBUG) System.out.println("regional annotator...");
        regionalAnnotator.annotate(labels);
        if (DEBUG) System.out.println("local annotator...");
        localAnnotator.annotate(labels);
        if (DEBUG) System.out.println("finding cells...");
        interp.setProgram(cellTypeProgram);
        interp.eval(labels);
        if (DEBUG) System.out.println("finding proteins...");
        interp.setProgram(proteinProgram);
        interp.eval(labels);
        if (DEBUG) System.out.println("scoping...");
        interp.setProgram(scopingProgram);
        interp.eval(labels);

        // figure out which image pointer 'owns' which scope
        imgPtrForScope = new TreeMap(); 
        imagePtrList = new ArrayList();
        String[] ptrTypes = new String[] { "local", "regional" };
        for (int i=0; i<ptrTypes.length; i++) {
            for (Span.Looper j=labels.instanceIterator(ptrTypes[i]); j.hasNext(); ) {
                Span imgPtrSpan = j.nextSpan(); 
                imagePtrList.add(imgPtrSpan);
                Span scopeSpan = findContainingSpan(imgPtrSpan, labels, ptrTypes[i]+"Scope");
                if (scopeSpan!=null) imgPtrForScope.put( scopeSpan, imgPtrSpan );
            }
        }

        
        // figure out which entities belong to which scopes
        imagePtrEntityPairs = new ArrayList();
        String[] entityTypes = new String[] { "protein", "cell" };
        for (int i=0; i<entityTypes.length; i++) {
            for (Span.Looper j=labels.instanceIterator(entityTypes[i]); j.hasNext(); ) {
                Span entitySpan = j.nextSpan(); 
                if (DEBUG) System.out.println("associating "+entitySpan);
                // find scope of each type containing span and associate imgPtrForScope(scope) & span
                for (int k=0; k<ptrTypes.length; k++) {
                    Span containingScope = findContainingSpan(entitySpan,labels,ptrTypes[k]+"Scope");
                    if (containingScope!=null && imgPtrForScope.get(containingScope)!=null) {
                        associate((Span)imgPtrForScope.get(containingScope), entityTypes[i], entitySpan);
                    } else {
                        if (DEBUG) System.out.println(" - not in "+ptrTypes[k]+" scope");
                    }
                }//ptrType k
                // stuff in global scope is associated with all img ptrs 
                Span globalScope = findContainingSpan(entitySpan,labels,"globalScope");
                if (globalScope!=null) {
                    if (DEBUG) System.out.println(" - in global scope");
                    String id = globalScope.getDocumentId();
                    for (int k=0; k<ptrTypes.length; k++) {
                        for (Span.Looper el=labels.instanceIterator(ptrTypes[k],id); el.hasNext(); ) {
                            Span imgPtrSpan = el.nextSpan();
                            associate(imgPtrSpan, entityTypes[i], entitySpan);
                        }
                    }
                }//globalScope
            } //entitySpan j
        } //entity type i
        
        // expand out the 'definition' of the image pointers
        imagePtrDefinition = new TreeMap();
        allLabels = new TreeSet();
        Pattern p1 = Pattern.compile(".*\\b([A-Z])\\s*-\\s*([A-Z])\\b.*");
        Pattern p2 = Pattern.compile(".*\\b([a-z])\\s*-\\s*([a-z])\\b.*");
        Pattern p3 = Pattern.compile(".*\\b([A-Za-z])\\b.*");
        for (Iterator i=imagePtrList.iterator(); i.hasNext(); ) {
            Span span = (Span)i.next();
            String string = span.asString();
            Matcher m1 = p1.matcher(string);
            while (m1.find()) defineRange(span,string,m1); 
            Matcher m2 = p2.matcher(string);
            while (m2.find()) defineRange(span,string,m2); 
            Matcher m3 = p3.matcher(string);
            while (m3.find()) defineLetter(span,string,m3);
        }
        
        try {
            writeFacts( "protein", proteinFile );
            writeFacts( "cell", cellFile );
            PrintStream s = new PrintStream(new FileOutputStream(new File(labelFile)));
            for (Iterator i=allLabels.iterator(); i.hasNext(); ) {
                s.println( (String) i.next() );
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: "+e.toString());
        }
        
    }

	// write out entity-related facts
	private void writeFacts(String entityType, String fileName) throws IOException
	{
		String myself = "caption/parseCaption.pl";
		// String myself = caption/CaptionProcessor.java;
		PrintStream s = new PrintStream(new FileOutputStream(new File(fileName)));
		for (Iterator i=imagePtrEntityPairs.iterator(); i.hasNext(); ) {
			ArrayList pair = (ArrayList)i.next();
			String type = (String)pair.get(0);
			if (entityType.equals(type)) {
				Span imgPtrSpan = (Span)pair.get(1);
				Span entitySpan = (Span)pair.get(2);
				String figureId = imgPtrSpan.getDocumentId(); // set by caller!
				for (Iterator j=((Set)imagePtrDefinition.get(imgPtrSpan)).iterator(); j.hasNext(); ) {
					String entityName = entitySpan.asString();
					String label = (String)j.next();
					s.println(entityType+"InPanelLabeled\t"+figureId+"\tstring://"+entityName+"\tstring://"+label
										+"\t"+myself+"\t1");
										 
				}
			}
		}
		s.close();
	}

	// define the semantics of an imgPtrSpan of the form 'b-c'
	private void defineRange(Span span, String string, Matcher matcher)
	{
		char lo = string.charAt(matcher.start(1));
		char hi = string.charAt(matcher.start(2));
		TreeSet set = (TreeSet)imagePtrDefinition.get(span);
		if (set==null) imagePtrDefinition.put( span,  (set=new TreeSet()) );
		for (char ch=lo; ch<=hi; ch++) {
			StringBuffer buf = new StringBuffer("");
			buf.append(ch);
			set.add( buf.toString() );
			allLabels.add( buf.toString() );
		}
	}
	// define the semantics of an imgPtrSpan of the form 'a'
	private void defineLetter(Span span, String string, Matcher matcher)
	{
		char ch = string.charAt(matcher.start(1));
		TreeSet set = (TreeSet)imagePtrDefinition.get(span);
		if (set==null) imagePtrDefinition.put(span, (set=new TreeSet()) );
		StringBuffer buf = new StringBuffer("");
		buf.append(ch);
		set.add( buf.toString() );
	}

	// associate an imgPtrSpan with an entity
	private void associate(Span imgPtrSpan,String entityType,Span entitySpan)
	{
		if (DEBUG) System.out.println("img ptr: "+imgPtrSpan+" entity: "+entitySpan);
		List pair = new ArrayList(3);
		pair.add(entityType);
		pair.add(imgPtrSpan);
		pair.add(entitySpan);
		imagePtrEntityPairs.add(pair);
	}

	// find a span of given type containing s
	private Span findContainingSpan(Span s,TextLabels labels,String type)
	{
		String id = s.getDocumentId();
		for (Span.Looper j=labels.instanceIterator(type,id); j.hasNext(); ) {
			Span t = j.nextSpan(); 
			if (t.contains(s)) return t;
		}
		return null;
	}

	// load classifier learned by LearnImagePtrExtractor and make it into an annotator
	private static Annotator loadAnnotator(String className) throws IOException
	{
		BinaryClassifier filter = (BinaryClassifier)IOUtil.loadSerialized(new File("lib/"+className+"Filter.ser"));
		SpanFeatureExtractor fe = new ImgPtrFE();
		SpanFinder candidateFinder = LearnImagePtrExtractor.candidateFinder; 
		String output = className;
		Annotator learnedAnnotator = new FinderAnnotator( new FilteredFinder(filter,fe,candidateFinder), output );
		return learnedAnnotator;
	}

	/**
	 * Main program.  Takes lines of the form
	 * <pre>
	 * X X-captionContent.txt X-proteinInPanelLabeled.FACTS X-cellInPanelLabeled.FACTS X-label.txt
	 *</pre>
	 * where each word is a file name, and creates the last three files from the first. 
	 */

	static public void main(String argv[]) throws IOException,Mixup.ParseException
	{
		boolean interactive = argv.length>0 && argv[0].startsWith("-interact");

		System.out.println("creating caption processor...");
		CaptionProcessor cp = new CaptionProcessor();
		System.out.println("caption processor created, interactive="+interactive);

		String line = null;
		LineNumberReader reader = new LineNumberReader(new BufferedReader(new InputStreamReader(System.in)));
		while ((line = reader.readLine())!=null) {
			String[] fileNames = line.split("\\s+");
			if (fileNames.length!=5) {
				System.out.println("Error: expected two input, three output files");
				continue;
			}
			String caption = loadFileContent( fileNames[1]) ;
			BasicTextBase base = new BasicTextBase();
			base.loadDocument( fileNames[0], caption );
			MutableTextLabels labels = new BasicTextLabels(base);
			for (int i=0; i<100; i++) {
				long start = System.currentTimeMillis();
				System.out.println("processing labels with "+base.size()+" docs");
				cp.processLabels(labels,fileNames[2],fileNames[3],fileNames[4]);
				long end = System.currentTimeMillis();
				System.out.println("processing time was "+((end-start)/1000.0)+" sec");
			}
			if (interactive) {
				System.out.println("launching viewer...");
				TextBaseEditor.edit( labels, null );
			}
		}
	}
	static private String loadFileContent(String fileName) {
		try {
			LineNumberReader bReader = new LineNumberReader(new BufferedReader(new FileReader(fileName)));
			StringBuffer buf = new StringBuffer("");
			String line = null;
			while ((line = bReader.readLine()) != null) {
				buf.append(line);
				buf.append("\n");
			}
			bReader.close();
			return buf.toString();
		} catch (Exception e) {			
			e.printStackTrace();
			System.out.println("Error: "+e.toString());
			return null;
		}
	}
}
