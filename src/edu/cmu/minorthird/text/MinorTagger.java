package edu.cmu.minorthird.text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import montylingua.JMontyLingua;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.text.mixup.MixupInterpreter;
import edu.cmu.minorthird.text.mixup.MixupProgram;

/**
 * Description:
 * An echo-like server that labels popular entities using XML tags in an input text
 *
 * Instruction:
 *
 * VERY IMPORTANT:
 * 1) Make sure you comment out the following line:
 * require 'pos';
 * in "lib/mixup/np.mixup" because this server already does POS tagging for you!
 * (otherwise MontyLingua will be invoked more than once)
 *
 * 2) If you encounter 'rcwangName.mixup' not found error, copy apps/names/lib/rcwangName.mixup
 * and apps/names/lib/newnames.txt to lib/mixup, or make sure rcwangName.mixup and newnames.txt
 * are in your classpath
 *
 * 3) To use Minorthird's createXMLmarkup function instead of mine, uncomment the line:
 * tagged = labelsLoader.createXMLmarkup(tempFile.getName(), labels);
 * and comment out the line:
 * tagged = createXML(in, labelsLoader.saveTypesAsXML(labels));
 * in the "tag" function. To switch back, do the opposite.
 *
 * 4) After connecting to the server, the client can send in any text. The server will
 * buffer the incoming text until it detects that a line is ended with TWO consecutive
 * dollar signs ($$). The server will then process the text in the buffer and echo back
 * the resulting text without disconnecting the client. However, if a line is ended
 * with THREE consecutive dollar signs ($$$), then after the server echos back the
 * resulting text, it will disconnect the connection automatically.
 *
 * @author Richard Wang <rcwang@cmu.edu>
 */
public class MinorTagger extends StringAnnotator{

	static Logger log=Logger.getLogger(MinorTagger.class);

	private static JMontyLingua montyLingua;

	private static final File NAMEMIXUP=new File("rcwangName.mixup");

	private static final File DATEMIXUP=new File("date.mixup");

//	private static final boolean DEBUG = false;
	private static final int DEFAULT_PORT=9998;

	public String explainAnnotation(TextLabels labels,Span documentSpan){
		return "no idea";
	}

	public static void main(String args[]){
		ServerSocket echoServer;
		Socket clientSocket;
		int port=DEFAULT_PORT;

		System.out.println("Starting MinorTagger v0.02b...");
		if(args.length>0&&args[0].matches("\\d+"))
			port=Integer.parseInt(args[0]);
		else{
			System.out
					.println("WARN: No listening port specified, using default port!");
			System.out
					.println("WARN: To specify, use the port number as the first argument.");
		}
		System.out.println("Loading Part-of-Speech Tagger...");
		montyLingua=new JMontyLingua();
		System.out.println("MinorTagger Started Successfully!");
		System.out.println("Waiting for connection on port "+port+"...");
		try{
			echoServer=new ServerSocket(port);
			while(true){
				clientSocket=echoServer.accept();
				System.out.println("["+(new Date())+"] Connected from "+
						clientSocket.getRemoteSocketAddress());
				(new MinorTaggerThread(clientSocket)).start();
			}
		}catch(IOException e){
			System.out.println(e);
		}
	}

	public MinorTagger(){
		montyLingua=new JMontyLingua();
	}

	protected CharAnnotation[] annotateString(String text){
		List<CharAnnotation> charAnnList=new ArrayList<CharAnnotation>();
		String tagged=montyLingua.tag_text(text);
		log.debug("Tagged: "+tagged);
		StringTokenizer tokeTagged=new StringTokenizer(tagged,"\n ",false);

		int index=0;
		while(tokeTagged.hasMoreTokens()){
			String strToken=tokeTagged.nextToken();
			int sep=strToken.lastIndexOf("/");
			String word=strToken.substring(0,sep);
			log.debug("word: "+word);
			String pos=strToken.substring(sep+1);
			log.debug("POS: "+pos);
			if(pos.endsWith("$"))
				pos=pos.replace('$','S');
			// look for alternative word
//      if (text.trim().toLowerCase())
			if(!text.trim().toLowerCase().startsWith(word.toLowerCase()))
				word=findAlternative(text,word);
			// ensure case are the same
			if(text.trim().toLowerCase().startsWith(word.toLowerCase()))
				word=text.trim().substring(0,word.length());
			CharAnnotation charAnn=makeCharAnnotation(text,word,pos,index,false);
			index=charAnn.getOffset()+charAnn.getLength();
		}
		return (CharAnnotation[])charAnnList.toArray(new CharAnnotation[0]);
	}

	/**
	 * Tag text with part-of-speech (plain text only)
	 * @param text
	 * @return
	 */
	public static String POSTag(String text){
		StringBuffer xmlTagged=new StringBuffer();
		text=text.replaceAll("<[^<>]+>","");
		String tagged=montyLingua.tag_text(text);
		log.debug("Tagged: "+tagged);
		StringTokenizer tokeTagged=new StringTokenizer(tagged,"\n ",false);

		while(tokeTagged.hasMoreTokens()){
			String strToken=tokeTagged.nextToken();
			int sep=strToken.lastIndexOf("/");
			String word=strToken.substring(0,sep);
			log.debug("word: "+word);
			String pos=strToken.substring(sep+1);
			log.debug("POS: "+pos);
			if(pos.endsWith("$"))
				pos=pos.replace('$','S');
			// look for alternative word
			if(!text.trim().toLowerCase().startsWith(word.toLowerCase()))
				word=findAlternative(text,word);
			// ensure case are the same
			if(text.trim().toLowerCase().startsWith(word.toLowerCase()))
				word=text.trim().substring(0,word.length());
			text=substFirst(text,word,"<"+pos+">"+word+"</"+pos+">",false);
			log.debug("WorkingString: "+text);
			int endPointer=text.lastIndexOf("</"+pos+">");
			if(endPointer!=-1)
				endPointer+=("</"+pos+">").length();
			log.debug("EndPointer: "+endPointer);
			if(endPointer==-1)
				break;
			xmlTagged.append(text.substring(0,endPointer));
			text=text.substring(endPointer); //in_ptr, working.length()
		}
		return xmlTagged.toString();
	}

	private static CharAnnotation makeCharAnnotation(String text,String word,
			String pos,int startIndex,boolean caseSensitive){
		int index=0;
		if(caseSensitive)
			index=text.indexOf(word,startIndex);
		else
			index=text.toLowerCase().indexOf(word.toLowerCase(),startIndex);
		if(index<0)
			return null;
		return new CharAnnotation(startIndex+index,word.length(),pos);
	}

	private static String substFirst(String in,String find,String newStr,
			boolean caseSensitive){
		char[] working=in.toCharArray();
		StringBuffer sb=new StringBuffer();
		int startIndex=0;
		if(caseSensitive)
			startIndex=in.indexOf(find);
		else
			startIndex=(in.toLowerCase()).indexOf(find.toLowerCase());
		if(startIndex<0)
			return in;
		int currIndex=0;
		for(int i=currIndex;i<startIndex;i++)
			sb.append(working[i]);
		currIndex=startIndex;
		sb.append(newStr);
		currIndex+=find.length();
		for(int i=currIndex;i<working.length;i++)
			sb.append(working[i]);
		return sb.toString();
	}

	private static String findAlternative(String in,String word){
		String workingString=in.trim().toLowerCase();
		String workingWord=word.toLowerCase();
		String find=word;
		if(workingWord.equals("do")){
			if(workingString.startsWith("dunno"))
				find="du";
		}else if(workingWord.equals("not")){
			if(workingString.startsWith("n't"))
				find="n't";
			else if(workingString.startsWith("'t"))
				find="'t";
			else if(workingString.startsWith("nno"))
				find="n";
		}else if(workingWord.equals("know")){
			if(workingString.startsWith("no"))
				find="no";
		}else if(workingWord.equals("your")){
			if(workingString.startsWith("ur"))
				find="ur";
		}else if(workingWord.equals("am")){
			if(workingString.startsWith("'m"))
				find="'m";
		}else if(workingWord.equals("are")){
			if(workingString.startsWith("'re"))
				find="'re";
		}else if(workingWord.equals("is")){
			if(workingString.startsWith("'s"))
				find="'s";
		}else if(workingWord.equals("would")){
			if(workingString.startsWith("'d"))
				find="'d";
		}else if(workingWord.equals("had")){
			if(workingString.startsWith("'d"))
				find="'d";
		}else if(workingWord.equals("will")){
			if(workingString.startsWith("'ll"))
				find="'ll";
			else if(workingString.startsWith("wo"))
				find="wo";
		}else if(workingWord.equals("come")){
			if(workingString.startsWith("c'm"))
				find="c'm";
		}else if(workingWord.equals("you")){
			if(workingString.startsWith("y'"))
				find="y'";
			else if(workingString.startsWith("ya"))
				find="ya";
		}else if(workingWord.equals("it")){
			if(workingString.startsWith("'t"))
				find="'t";
		}else if(workingWord.equals("-")){
			if(workingString.startsWith("/"))
				find="/";
		}else if(workingWord.equals("want")){
			if(workingString.startsWith("wan"))
				find="wan";
		}else if(workingWord.equals("going")){
			if(workingString.startsWith("gon"))
				find="gon";
		}else if(workingWord.equals("have")){
			if(workingString.startsWith("haf"))
				find="haf";
			else if(workingString.startsWith("'ve"))
				find="'ve";
		}else if(workingWord.equals("to")){
			if(workingString.startsWith("na"))
				find="na";
			else if(workingString.startsWith("ta"))
				find="ta";
		}else if(workingWord.equals("give")){
			if(workingString.startsWith("gim"))
				find="gim";
		}else if(workingWord.equals("let")){
			if(workingString.startsWith("lem"))
				find="lem";
		}else{
			System.err.println("POS Tagging error!");
			System.err.println("Current word:"+word);
			System.err.println("Working string:"+in);
		}
		return find;
	}

	private static String tag(String in){
		if(in.replaceAll("\\s+","").length()==0)
			return "";
		String tagged=POSTag(in);

		// load text base
		TextBaseLoader baseLoader=
				new TextBaseLoader(TextBaseLoader.DOC_PER_FILE,true);
		File tempFile=createFile(tagged);
		
		try{
			baseLoader.load(tempFile);
		}catch(Exception e){
			System.err.println(e);
		}

		// get XML labels
		MutableTextLabels labels=baseLoader.getLabels();

		// evaluate mixup
		MixupProgram p;
		try{
			p=new MixupProgram(NAMEMIXUP);
			(new MixupInterpreter(p)).eval(labels);
		}catch(Exception e){
			System.err.println(e);
		}
		try{
			p=new MixupProgram(DATEMIXUP);
			(new MixupInterpreter(p)).eval(labels);
		}catch(Exception e){
			System.err.println(e);
		}

		TextLabelsLoader labelsLoader=new TextLabelsLoader();

		// Minorthird's version of marking up XML labels
		// tagged = labelsLoader.createXMLmarkup(tempFile.getName(), labels);

		// My own version of marking up XML labels; doesn't check for overlapping spans
		tagged=createXML(in,labelsLoader.saveTypesAsXML(labels));

		String keepXML[]=
				{"CC","CD","DT","EX","FW","IN","JJ","JJR","JJS","LS","MD","NN","NNS",
						"NNP","NNPS","PDT","POS","PRP","PRPS","RB","RBR","RBS","RP","TO",
						"UH","VB","VBD","VBG","VBN","VBP","VBZ","WDT","WP","WPS","WRB","S",
						"NP","name","extracted_date","extracted_time"};
		tagged=filterXML(tagged,keepXML);

		// fix the problem so that end-tag doesn't stick to the next begin-tag.
		// tagged = tagged.replaceAll("(<[^<>]+>[^<>]+?)(\\s+)(</[^<>]+>)", "$1$3$2");

		return tagged;
	}

	private static String filterXML(String tagged,String[] keepXML){
		Matcher m=Pattern.compile("</?([^<>]+)>").matcher(tagged);
		Set<String> delXML=new HashSet<String>();
		while(m.find()){
			if(delXML.contains(m.group(1)))
				continue;
			boolean keep=false;
			for(int i=0;i<keepXML.length;i++){
				if(keepXML[i].equals(m.group(1))){
					keep=true;
					break;
				}
			}
			if(!keep)
				delXML.add(m.group(1));
		}
		for(Iterator<String> i=delXML.iterator();i.hasNext();){
			String del=i.next();
			tagged=tagged.replaceAll("</?\\Q"+del+"\\E>","");
		}
		return tagged;
	}

	private static String createXML(String in,String XMLIndex){
		Map<Integer,String> hash=new HashMap<Integer,String>();
		String temp;
		List<List<Object>> spanList=new ArrayList<List<Object>>();
		StringBuffer buf=new StringBuffer(in);
		Matcher m=
				Pattern.compile("[\r\n]  <(\\S+) lo=(\\d+) hi=(\\d+)>").matcher(
						XMLIndex);

		while(m.find()){
			List<Object> span=new ArrayList<Object>();
			span.add(m.group(1));
			span.add(new Integer(m.group(2)));
			span.add(new Integer(m.group(3)));
			spanList.add(span);
		}

		Collections.sort(spanList,new Comparator<List<Object>>(){
			public int compare(List<Object> o1,List<Object> o2){
				Integer lo1=(Integer)o1.get(1);
				Integer lo2=(Integer)o2.get(1);
				Integer hi1=(Integer)o1.get(2);
				Integer hi2=(Integer)o2.get(2);
				if(lo1.compareTo(lo2)==0)
					return hi2.compareTo(hi1);
				else
					return lo1.compareTo(lo2);
			}
		});

		for(Iterator<List<Object>> i=spanList.iterator();i.hasNext();){
			List<Object> span=i.next();
			String xml=(String)span.get(0);
			String start="<"+xml+">";
			String end="</"+xml+">";
			Integer lo=(Integer)span.get(1);
			Integer hi=(Integer)span.get(2);
			temp=(String)hash.get(lo);
			hash.put(lo,((temp==null)?"":temp)+start);
			temp=(String)hash.get(hi);
			hash.put(hi,end+((temp==null)?"":temp));
		}

		List<Integer> al=new ArrayList<Integer>(hash.keySet());
		Collections.sort(al);
		int counter=0;
		for(Iterator<Integer> i=al.iterator();i.hasNext();){
			Integer index=i.next();
			String tags=hash.get(index);
			buf.insert(index.intValue()+counter,tags);
			counter+=tags.length();
		}
		return buf.toString();
	}

	// create a temp file from a string
	private static File createFile(String content){
		File temp=null;
		BufferedWriter bWriter;
		try{
			temp=File.createTempFile("tmp","");
			temp.deleteOnExit();
			bWriter=new BufferedWriter(new FileWriter(temp));
			bWriter.write(content);
			bWriter.close();
		}catch(IOException ioe){
			System.err.println("Error creating temp file: "+ioe);
		}
		return temp;
	}

	private static class MinorTaggerThread extends Thread{

		private Socket socket=null;

		public MinorTaggerThread(Socket socket){
			super("MinorTaggerThread");
			this.socket=socket;
		}

		public void run(){
			StringBuffer buf=new StringBuffer();
			BufferedReader br;
			PrintStream os;
			String line;

			try{
				br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
				os=new PrintStream(socket.getOutputStream());

				while((line=br.readLine())!=null){
					boolean end_close=false;
					boolean end_continue=false;
					if(line.endsWith("$$$")){
						end_close=true;
						line=line.substring(0,line.length()-3);
					}else if(line.endsWith("$$")){
						end_continue=true;
						line=line.substring(0,line.length()-2);
					}
					buf.append(line+"\n");
					if(end_close||end_continue){
						os.println(tag(buf.toString()));
						buf.setLength(0);
					}
					if(end_close){
						os.close();
						br.close();
						socket.close();
						break;
					}
				}
				System.out.println("["+(new Date())+"] Disconnected from "+
						socket.getRemoteSocketAddress());
			}catch(Exception e){
				System.err.println(e);
			}
		}
	}
}
