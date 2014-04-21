package edu.cmu.minorthird.text;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;

/**
 * Loads and saves the contents of a TextLabels into a file.
 * 
 * Labels can be loaded from operations (see importOps) or from a serialized
 * TextLabels object. Labels can be serialized or types can be saved as
 * operations, xml, or plain lists.
 * 
 * @author William Cohen
 */

public class TextLabelsLoader{

	private static Logger log=Logger.getLogger(TextLabelsLoader.class);

	/**
	 * Spans in labels are a complete list of all spans.
	 */
	static final public int CLOSE_ALL_TYPES=1;

	/**
	 * If a document has been labeled for a type, assume all spans of that type
	 * are there.
	 */
	static final public int CLOSE_TYPES_IN_LABELED_DOCS=2;

	/** Make no assumptions about closure. */
	static final public int DONT_CLOSE_TYPES=3;

	static final public int CLOSE_BY_OPERATION=4;

	public static final String[] CLOSURE_NAMES=
	{"CLOSE_ALL_TYPES","CLOSE_TYPES_IN_LABELED_DOCS","DONT_CLOSE_TYPES",
	"CLOSE_BY_OPERATION"};

	private int closurePolicy=CLOSE_BY_OPERATION;

	private int warnings=0;

	static private final int MAX_WARNINGS=10;

	/**
	 * Set the closure policy.
	 * 
	 * @param policy
	 *          one of CLOSE_ALL_TYPES, CLOSE_TYPES_IN_LABELED_DOCS,
	 *          DONT_CLOSE_TYPES
	 */
	public void setClosurePolicy(int policy){
		this.closurePolicy=policy;
	}

	/**
	 * Create a new labeling by importing from a file with importOps.
	 */
	public MutableTextLabels loadOps(TextBase base,File file) throws IOException,
	FileNotFoundException{
		MutableTextLabels labels=new BasicTextLabels(base);
		importOps(labels,base,file);
		return labels;
	}

	/**
	 * Load lines modifying a TextLabels from a file. There are four allowed
	 * operations: addToType, closeType, closeAllTypes, setClosure
	 * 
	 * For addToType: The lines must be of the form:
	 * <code>addToType ID LOW LENGTH TYPE</code> where ID is a documentID in the
	 * given TextBase, LOW is a character index into that document, and LENGTH is
	 * the length in characters of the span that will be created as given type
	 * TYPE. If LENGTH==-1, then the created span will go to the end of the
	 * document.
	 * 
	 * For closeType: Lines must be <code>closeType ID TYPE</code> where ID is a
	 * documentID in the given TextBase and TYPE is the label type to close over
	 * that document.
	 * 
	 * For closeAllTypes: Lines must be <code>closeAllType ID</code> where ID is
	 * a documentID in the given TextBase. The document will be closed for all
	 * types present in the TextLabels <em>after all operations</em> are
	 * performed.
	 * 
	 * For setClosure: Lines must be <code>setClosure POLICY</code> where POLICY
	 * is one of the policy types defined in this class. It will immediately
	 * change the closure policy for the loader. This is best used at the
	 * beginning of the file to indicate one of the generic policies or the
	 * CLOSE_BY_OPERATION (default) policy.
	 */
	public void importOps(MutableTextLabels labels,TextBase base,File file)
	throws IOException,FileNotFoundException{
		base=labels.getTextBase();
		if(base==null)
			throw new IllegalStateException(
			"TextBase attached to labels must not be null");

		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line=null;
		List<String> docList=new ArrayList<String>();
		try{
			while((line=in.readLine())!=null){
				if(line.trim().length()==0)
					continue;
				if(line.startsWith("#"))
					continue;
				log.debug("read line #"+in.getLineNumber()+": "+line);
				StringTokenizer tok=new StringTokenizer(line);
				String op;
				try{
					op=advance(tok,in,file);
				}catch(IllegalArgumentException e){
					throw getNewException(e,", failed to find operation.");
				}
				if("addToType".equals(op)){
					addToType(tok,in,file,base,labels);
				}else if("setSpanProp".equals(op)){
					setSpanProp(tok,in,file,base,labels);
				}else if("closeType".equals(op)){
					String docId=advance(tok,in,file);
					String type=advance(tok,in,file);
					Span span=base.documentSpan(docId);
					if(span!=null){
						labels.closeTypeInside(type,span);
						log.debug("closed "+type+" on "+docId);
					}else{
						warnings++;
						if(warnings<MAX_WARNINGS){
							log.warn("unknown id '"+docId+"' in closeType");
						}else if(warnings==MAX_WARNINGS){
							log.warn("there will be no more warnings of this sort given");
						}
					}
				}else if("closeAllTypes".equalsIgnoreCase(op)){
					String docId=advance(tok,in,file);
					docList.add(docId);
				}else{
					throw new IllegalArgumentException("error on line "+
							in.getLineNumber()+" of "+file.getName());
				}
			}
			// close over the doc list for all types seen
			for(int i=0;i<docList.size();i++){
				String docId=(String)docList.get(i);
				Span span=base.documentSpan(docId);
				closeLabels(labels.getTypes(),labels,span);
			}

		}catch(IllegalArgumentException e){
			throw getNewException(e," on line: "+line);
		}
		in.close();
		closeLabels(labels,closurePolicy);
	}

	private void addToType(StringTokenizer tok,LineNumberReader in,File file,
			TextBase base,MutableTextLabels labels){
		String id=advance(tok,in,file);
		String loStr=advance(tok,in,file);
		String lenStr=advance(tok,in,file);
		String type=advance(tok,in,file);
		String confidence=tok.hasMoreTokens()?advance(tok,in,file):null;
		int lo,len;
		try{
			lo=Integer.parseInt(loStr);
			len=Integer.parseInt(lenStr);
			Span span=base.documentSpan(id);
			if(span==null){
				warnings++;
				if(warnings<MAX_WARNINGS){
					log.warn("unknown id '"+id+"' in addToType "+lo+" "+len);
				}else if(warnings==MAX_WARNINGS){
					log.warn("there will be no more warnings of this sort given");
				}
			}else{
				Details details=null;
				if(confidence!=null)
					details=new Details(StringUtil.atof(confidence));
				if(lo==0&&len<0)
					labels.addToType(span,type,details);
				else{
					// shortcut: char offsets "0 -1" means the whole document
					if(len<0)
						len=span.asString().length()-lo;
					labels.addToType(span.charIndexSubSpan(lo,lo+len),type,details);
				}
			}
		}catch(NumberFormatException e){
			throw new IllegalArgumentException("bad number on line "+
					in.getLineNumber()+" of "+file.getName());
		}
	}

	private void setSpanProp(StringTokenizer tok,LineNumberReader in,File file,
			TextBase base,MutableTextLabels labels){
		String id=advance(tok,in,file);
		String loStr=advance(tok,in,file);
		String lenStr=advance(tok,in,file);
		String prop=advance(tok,in,file);
		String value=advance(tok,in,file);
		int lo,len;
		try{
			lo=Integer.parseInt(loStr);
			len=Integer.parseInt(lenStr);
			Span span=base.documentSpan(id);
			if(span==null){
				warnings++;
				if(warnings<MAX_WARNINGS){
					log.warn("unknown id '"+id+"'");
				}else if(warnings==MAX_WARNINGS){
					log.warn("there will be no more warnings of this sort given");
				}
			}else{
				if(lo==0&&len<0)
					labels.setProperty(span,prop,value);
				else{
					if(len<0)
						len=span.asString().length()-lo;
					labels.setProperty(span.charIndexSubSpan(lo,lo+len),prop,value);
				}
			}
		}catch(NumberFormatException e){
			throw new IllegalArgumentException("bad number on line "+
					in.getLineNumber()+" of "+file.getName());
		}
	}

	private static IllegalArgumentException getNewException(
			IllegalArgumentException e,String addToMsg){
		String msg=e.getMessage()+addToMsg;
		StackTraceElement[] trace=e.getStackTrace();
		IllegalArgumentException exception=new IllegalArgumentException(msg);
		exception.setStackTrace(trace);
		return exception;
	}

	private String advance(StringTokenizer tok,LineNumberReader in,File file){
		if(!tok.hasMoreTokens())
			throw new IllegalArgumentException("error on line "+in.getLineNumber()+
					" of "+file.getName()+" failed to find token");
		return tok.nextToken();
	}

	/**
	 * Close labels on the labels according to the policy. This applies the same
	 * policy to all documents and types in the labels. To get finer control of
	 * closure use closeLabels(Set, MutableTextLabels, Span) or
	 * MutableTextLabels.closeTypeInside(...)
	 * 
	 * @param labels
	 * @param policy
	 */
	public void closeLabels(MutableTextLabels labels,int policy){
		Set<String> types=labels.getTypes();
		TextBase base=labels.getTextBase();
		switch(policy){
		case CLOSE_ALL_TYPES:
			for(Iterator<Span> i=base.documentSpanIterator();i.hasNext();){
				Span document=i.next();
				closeLabels(types,labels,document);
			}
			break;
		case CLOSE_TYPES_IN_LABELED_DOCS:
			Set<Span> labeledDocs=new TreeSet<Span>();
			for(Iterator<String> j=types.iterator();j.hasNext();){
				String type=j.next();
				for(Iterator<Span> i=labels.instanceIterator(type);i.hasNext();){
					Span span=i.next();
					labeledDocs.add(span.documentSpan());
				}
			}
			for(Iterator<Span> i=labeledDocs.iterator();i.hasNext();){
				Span document=i.next();
				closeLabels(types,labels,document);
			}
			break;
		case DONT_CLOSE_TYPES: // do nothing for this
			break;
		case CLOSE_BY_OPERATION: // already closed in theory
			break;
		default:
			log.warn("closure policy("+policy+") not recognized");
		}
	}

	/**
	 * Close all types in the typeSet on the given document
	 * 
	 * @param typeSet
	 *          set of types to close for this document
	 * @param labels
	 *          TextLabels holding the types
	 * @param document
	 *          Span to close types over
	 */
	private void closeLabels(Set<String> types,MutableTextLabels labels,
			Span document){
		for(Iterator<String> j=types.iterator();j.hasNext();){
			String type=j.next();
			labels.closeTypeInside(type,document);
		}
	}

	/** Read in a serialized TextLabels. */
	public MutableTextLabels loadSerialized(File file,TextBase base)
	throws IOException,FileNotFoundException{
		try{
			ObjectInputStream in=
				new ObjectInputStream(new BufferedInputStream(new FileInputStream(
						file)));
			MutableTextLabels labels=(MutableTextLabels)in.readObject();
			labels.setTextBase(base);
			in.close();
			return labels;
		}catch(ClassNotFoundException e){
			throw new IllegalArgumentException("can't read TextLabels from "+file+
					": "+e);
		}
	}

	/** Serialize a TextLabels. */
	public void saveSerialized(MutableTextLabels labels,File file)
	throws IOException{
		ObjectOutputStream out=
			new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
					file)));
		out.writeObject(labels);
		out.flush();
		out.close();
	}

	/** Save extracted data in a format readable with loadOps. */
	public String printTypesAsOps(TextLabels labels){
		StringBuffer out=new StringBuffer();
		ProgressCounter pc=
			new ProgressCounter("saving labels","type",labels.getTypes().size());
		for(Iterator<String> i=labels.getTypes().iterator();i.hasNext();){
			String type=i.next();
			ProgressCounter pc2=new ProgressCounter("saving type "+type,"span");
			for(Iterator<Span> j=labels.instanceIterator(type);j.hasNext();){
				Span s=j.next();
				if(s.size()>0){
					int lo=s.getTextToken(0).getLo();
					int hi=s.getTextToken(s.size()-1).getHi();
					Details details=labels.getDetails(s,type);
					if(details==null||details==Details.DEFAULT){
						out.append("addToType "+s.getDocumentId()+" "+lo+" "+(hi-lo)+" "+
								type+"\n");
					}else{
						out.append("addToType "+s.getDocumentId()+" "+lo+" "+(hi-lo)+" "+
								type+" "+details.getConfidence()+"\n");
					}
				}else{
					warnings++;
					if(warnings<MAX_WARNINGS){
						log.warn("forgetting label on empty span type "+type+": "+s);
					}else if(warnings==MAX_WARNINGS){
						log.warn("there will be no more warnings of this sort given");
					}
				}
				pc2.progress();
			}
			pc2.finished();
			Iterator<Span> it=labels.closureIterator(type);
			while(it.hasNext()){
				Span s=it.next();
				Span doc=s.documentSpan();
				if(s.size()!=doc.size()){
					throw new UnsupportedOperationException(
					"can't save environment with closureSpans!=docSpans");
				}
				out.append("closeType "+s.getDocumentId()+" "+type+"\n");
			}
			pc.progress();
		}
		pc.finished();
		ProgressCounter pc3=
			new ProgressCounter("saving labels","property",labels
					.getSpanProperties().size());
		for(Iterator<String> i=labels.getSpanProperties().iterator();i.hasNext();){
			String prop=i.next();
			for(Iterator<Span> j=labels.getSpansWithProperty(prop);j.hasNext();){
				Span s=j.next();
				if(s.size()>0){
					String val=labels.getProperty(s,prop);
					int lo=s.getTextToken(0).getLo();
					int hi=s.getTextToken(s.size()-1).getHi();
					out.append("setSpanProp "+s.getDocumentId()+"  "+lo+" "+(hi-lo)+" "+
							prop+" "+val+"\n");
				}
			}
			pc3.progress();
		}
		pc3.finished();
		return out.toString();
	}

	/** Save extracted data in a format readable with loadOps. */
	public void saveTypesAsOps(TextLabels labels,File file) throws IOException{
		PrintStream out=new PrintStream(new FileOutputStream(file));
		out.println(printTypesAsOps(labels));
		out.close();
	}

	/**
	 * Save spans of given type into the file, one per line. Linefeeds in strings
	 * are replaced with spaces.
	 */
	public void saveTypesAsStrings(TextLabels labels,File file,
			boolean includeOffset) throws IOException{
		PrintStream out=new PrintStream(new FileOutputStream(file));
		// Do types
		for(Iterator<String> j=labels.getTypes().iterator();j.hasNext();){
			String type=j.next();
			for(Iterator<Span> i=labels.instanceIterator(type);i.hasNext();){
				Span span=i.next();
				if(span.size()>0){
					out.print(type);
					if(includeOffset){
						out.print(":"+span.getDocumentId()+":"+span.getTextToken(0).getLo()+
								":"+span.getTextToken(span.size()-1).getHi());
					}
					out.println("\t"+span.asString().replace('\n',' '));
				}
			}
		}
		// Do props
		for(Iterator<String> i=labels.getSpanProperties().iterator();i.hasNext();){
			String prop=i.next();
			for(Iterator<Span> j=labels.getSpansWithProperty(prop);j.hasNext();){
				Span span=j.next();
				if(span.size()>0){
					String val=labels.getProperty(span,prop);
					if(!prop.equals("_prediction")){
						out.print(prop);
						out.print("=");
					}
					out.print(val);
					if(includeOffset){
						out.print(":"+span.getDocumentId()+":"+span.getTextToken(0).getLo()+
								":"+span.getTextToken(span.size()-1).getHi());
					}
					out.println("\t"+span.asString().replace('\n',' '));
				}
			}
		}
		out.close();
	}

	/**
	 * Save documents to specified directory with extracted types embedded as xml.
	 */
	public void saveDocsWithEmbeddedTypes(TextLabels labels,File dir)
	throws IOException{
		Span currDoc;
		Iterator<Span> looper=labels.getTextBase().documentSpanIterator();
		PrintStream out;

		if(dir.exists()){
			log.warn(dir+" already exists, some files may be overwritten.");
		}
		else if(!dir.mkdir()){
			throw new IOException("Could not create directory named: "+dir);
		}

		while(looper.hasNext()){
			// this call returns the entire document with all labels embedded as xml
			currDoc=looper.next();
			out=
				new PrintStream(new FileOutputStream(new File(dir+"/"+
						currDoc.getDocumentId())));
			out.println(createXMLmarkup(currDoc.getDocumentId(),labels));
			out.close();
		}
	}

	/**
	 * Save extracted data in an XML format. Convert to string
	 * &lt;root>..&lt;type>...&lt;/type>..&lt;/root>. <br>
	 * <br>
	 * In the even that labels overlap such as [A (B C] D)E an
	 * IllegalArgumentException is thrown because a well-formed XML document
	 * cannot be created.
	 */
	public String createXMLmarkup(String documentId,TextLabels labels){
		Span docSpan=labels.getTextBase().documentSpan(documentId);
		String docString=
			labels.getTextBase().documentSpan(documentId).getDocumentContents();

		// Put all labels and their info in a list
		List<LabelInfo> unsortedLabels=new ArrayList<LabelInfo>();

		// Do types
		for(Iterator<String> i=labels.getTypes().iterator();i.hasNext();){
			String type=i.next();			
			for(Iterator<Span> j=labels.instanceIterator(type,documentId);j.hasNext();){
				Span s=j.next();
				int start=s.documentSpanStartIndex();
				int end=start+s.size()-1;
				unsortedLabels.add(new LabelInfo(s,type,start,end));
			}
		}

		// Do props
		for(Iterator<String> i=labels.getSpanProperties().iterator();i.hasNext();){
			String prop=i.next();
			for(Iterator<Span> j=labels.getSpansWithProperty(prop,documentId);j.hasNext();){
				Span s=j.next();
				String val=labels.getProperty(s,prop);
				int start=s.documentSpanStartIndex();
				int end=start+s.size()-1;
				if(prop.equals("_prediction")){
					unsortedLabels.add(new LabelInfo(s,val,start,end));
				}
				else{
					unsortedLabels.add(new LabelInfo(s,prop+"."+val,start,end));
				}
			}
		}

		// Sort the labels. If two spans are overlapping then throw an exception
		List<LabelInfo> sortedLabels=
			new ArrayList<LabelInfo>(unsortedLabels.size());
		while(unsortedLabels.size()>0){
			LabelInfo curLabel=unsortedLabels.remove(0);
			int position=-1;

			boolean overlap=false;

			// Iterate through sortedLabels
			for(int j=0;j<sortedLabels.size();j++){
				LabelInfo compLabel=(LabelInfo)sortedLabels.get(j);
				// Find if there is an overlap
				if((curLabel.start<compLabel.start&&curLabel.end>compLabel.start)&&
						(curLabel.end<compLabel.end))
					overlap=true;
				else if((curLabel.start>compLabel.start&&curLabel.start<compLabel.end)&&
						(curLabel.end>compLabel.end))
					overlap=true;
				// Find position
				if((curLabel.start<compLabel.start)||
						((curLabel.start==compLabel.start)&&(curLabel.end>=compLabel.end))){
					position=j;
					break;
				}
			}

			// If the label overlapped with another label, then throw an exception
			if(overlap)
				throw new IllegalArgumentException(
				"Labels contain overalpping spans, cannot save as XML format.");

			// Otherwise add the label to the proper position in the sorted list.
			if(position>-1)
				sortedLabels.add(position,curLabel);
			else
				sortedLabels.add(curLabel);
		}

		// Create sorted list of tags
		List<TagInfo> sortedTags=new ArrayList<TagInfo>(sortedLabels.size()*2);
		for(int i=0;i<sortedLabels.size();i++){
			LabelInfo label=sortedLabels.get(i);
			sortedTags.add(new TagInfo(label.start,"<"+label.type+">",true));
		}
		boolean added=false;
		while(sortedLabels.size()>0){
			LabelInfo label=sortedLabels.remove(0);
			added=false;
			for(int y=0;y<sortedTags.size();y++){
				TagInfo tag=(TagInfo)sortedTags.get(y);
				if(label.end<tag.pos){
					sortedTags.add(y,new TagInfo(label.end,"</"+label.type+">",false));
					added=true;
					break;
				}
			}
			if(!added){
				sortedTags.add(new TagInfo(label.end,"</"+label.type+">",false));
			}
		}

		// Create markedup StringBuffer
		StringBuffer buffer=new StringBuffer();

		buffer.append("<root>");
		int docPos=0,pos=0;
		while(sortedTags.size()>0){
			TagInfo curTag=sortedTags.remove(0);

			if(curTag.pos<docSpan.size()){
				if(curTag.isOpenTag)
					pos=docSpan.subSpan(curTag.pos,1).getTextToken(0).getLo();
				else
					pos=docSpan.subSpan(curTag.pos,1).getTextToken(0).getHi();
			}else
				pos=docString.length();

			buffer.append(docString.substring(docPos,pos));
			buffer.append(curTag.tag);
			docPos=pos;
		}
		buffer.append(docString.substring(docPos,docString.length()));
		buffer.append("</root>");
		return buffer.toString();

	}

	private class TagInfo{

		public int pos;

		public String tag;

		public boolean isOpenTag;

		public TagInfo(int pos,String tag,boolean isOpenTag){
			this.pos=pos;
			this.tag=tag;
			this.isOpenTag=isOpenTag;
		}
	}

	private class LabelInfo{

//		public Span span;

		public String type;

		public int start;

		public int end;

		public LabelInfo(Span span,String type,int start,int end){
//			this.span=span;
			this.type=type;
			this.start=start;
			this.end=end;
		}
	}

//	// Helper method used to maintain a set of tag boundaries
//	private void setBoundary(SortedMap<Span,Set<String[]>> boundaries,
//			String beginOrEnd,String type,Span s){
//		Set<String[]> ops=boundaries.get(s);
//		if(ops==null)
//			boundaries.put(s,(ops=new HashSet<String[]>()));
//		ops.add(new String[]{beginOrEnd,type});
//	}

	/** Save extracted data in an XML format */
	public String saveTypesAsXML(TextLabels labels){
		StringBuffer buf=new StringBuffer("<extractions>\n");
		for(Iterator<String> i=labels.getTypes().iterator();i.hasNext();){
			String type=i.next();
			for(Iterator<Span> j=labels.instanceIterator(type);j.hasNext();){
				Span s=j.next();
				int lo=s.getTextToken(0).getLo();
				int hi=s.getTextToken(s.size()-1).getHi();
				buf.append("  <"+type+" lo="+lo+" hi="+hi+">"+s.asString()+"</"+type+
				">\n");
			}
		}
		buf.append("</extractions>\n");
		return buf.toString();
	}
}
