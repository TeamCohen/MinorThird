
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import montylingua.JMontyLingua;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description:
 * An echo-like server that labels popular entities using XML tags in an input text
 *
 * Instruction:
 *
 * VERY IMPORTANT: Make sure you comment out the line
 * require 'pos';
 * in "np.mixup" because this server already does POS tagging for you!
 * If you don't want to change the original mixup file, you can copy "np.mixup"
 * to MinorTagger's directory and then comment out the above line.
 *
 * To use Minorthird's createXMLmarkup function instead of mine, uncomment the line:
 * tagged = labelsLoader.createXMLmarkup(tempFile.getName(), labels);
 * and comment out the line:
 * tagged = createXML(in, labelsLoader.saveTypesAsXML(labels));
 * in the "tag" function. To switch back, do the opposite.
 *
 * After connecting to the server, the client can send in any text. The server will
 * buffer the incoming text until it detects that a line is ended with TWO consecutive
 * dollar signs ($$). The server will then process the text in the buffer and echo back
 * the resulting text without disconnecting the client. However, if a line is ended
 * with THREE consecutive dollar signs ($$$), then after the server echos back the
 * resulting text, it will disconnect the connection automatically.
 *
 * @author Richard Wang <rcwang@cmu.edu>
 */
public class MinorTagger {

	private static JMontyLingua montyLingua;
	private static final File NAMEMIXUP = new File("names.mixup");
	private static final File DATEMIXUP = new File("date.mixup");
	private static final boolean DEBUG = false;
	private static final int DEFAULT_PORT = 9998;

	public static void main (String args[]) {
		ServerSocket echoServer = null;
		Socket clientSocket = null;
		int port = DEFAULT_PORT;

		System.out.println("Starting MinorTagger v0.01b...");
		if (args.length > 0 && args[0].matches("\\d+"))
			port = Integer.parseInt(args[0]);
		else {
			System.out.println("WARN: No listening port specified, using default port!");
			System.out.println("WARN: To specify, use the port number as the first argument.");
		}
		System.out.println("Loading Part-of-Speech Tagger...");
		montyLingua = new JMontyLingua();
		System.out.println("MinorTagger Started Successfully!");
		System.out.println("Waiting for connection on port " + port + "...");
		try {
			echoServer = new ServerSocket(port);
			while (true) {
				clientSocket = echoServer.accept();
				System.out.println("[" + (new Date()) + "] Connected from " + clientSocket.getRemoteSocketAddress());
				(new MinorTaggerThread(clientSocket)).start();
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public static String POSTag (String in) {
		String workingString = in.replaceAll("<[^<>]+>", "");
		String strToken = null;
		String pos = null;
		String word = null;
		StringBuffer XMLTagged = new StringBuffer();
		int sep = 0;
		int endPointer = 0;

		String tagged = montyLingua.tag_text(workingString);
		if (DEBUG) System.out.println("Tagged: " + tagged);
		StringTokenizer tokeTagged = new StringTokenizer(tagged, "\n ", false);

		while (tokeTagged.hasMoreTokens()) {
			strToken = tokeTagged.nextToken();
			sep = strToken.lastIndexOf("/");
			word = strToken.substring(0, sep);
			if (DEBUG) System.out.println("word: " + word);
			pos = strToken.substring(sep + 1);
			if (DEBUG) System.out.println("POS: " + pos);
			if (pos.endsWith("$"))
				pos = pos.replace('$', 'S');
			// look for alternative word
			if (!workingString.trim().toLowerCase().startsWith(word.toLowerCase()))
				word = findAlternative(workingString, word);
			// ensure case are the same
			if (workingString.trim().toLowerCase().startsWith(word.toLowerCase()))
				word = workingString.trim().substring(0, word.length());
			workingString = substFirst(workingString, word, "<" + pos + ">" + word + "</" + pos + ">", false);
			if (DEBUG) System.out.println("WorkingString: " + workingString);
			endPointer = workingString.lastIndexOf("</" + pos + ">") + ("</" + pos + ">").length();
			if (DEBUG) System.out.println("EndPointer: " + endPointer);
			XMLTagged.append(workingString.substring(0, endPointer));
			workingString = workingString.substring(endPointer); //in_ptr, working.length()
		}
		return XMLTagged.toString();
	}

	private static String substFirst (String in, String find, String newStr, boolean case_sensitive) {
		char[] working = in.toCharArray();
		StringBuffer sb = new StringBuffer();
		// int startindex =  in.indexOf(find);
		int startindex = 0;
		if (case_sensitive)
			startindex = in.indexOf(find);
		else
			startindex = (in.toLowerCase()).indexOf(find.toLowerCase());
		if (startindex < 0) return in;
		int currindex = 0;
		for (int i = currindex; i < startindex; i++)
			sb.append(working[i]);
		currindex = startindex;
		sb.append(newStr);
		currindex += find.length();
		for (int i = currindex; i < working.length; i++)
			sb.append(working[i]);
		return sb.toString();
	}

	private static String findAlternative (String in, String word) {
		String workingString = in.trim().toLowerCase();
		String workingWord = word.toLowerCase();
		String find = word;
		if (workingWord.equals("not")) {
			if (workingString.startsWith("n't"))
				find = "n't";
			else if (workingString.startsWith("'t"))
				find = "'t";
		} else if (workingWord.equals("-")) {
			if (workingString.startsWith("/"))
				find = "/";
		} else if (workingWord.equals("want")) {
			if (workingString.startsWith("wan"))
				find = "wan";
		} else if (workingWord.equals("going")) {
			if (workingString.startsWith("gon"))
				find = "gon";
		} else if (workingWord.equals("have")) {
			if (workingString.startsWith("haf"))
				find = "haf";
		} else if (workingWord.equals("to")) {
			if (workingString.startsWith("na"))
				find = "na";
			else if (workingString.startsWith("ta"))
				find = "ta";
		} else if (workingWord.equals("give")) {
			if (workingString.startsWith("gim"))
				find = "gim";
		} else if (workingWord.equals("let")) {
			if (workingString.startsWith("lem"))
				find = "lem";
		} else {
			System.err.println("POS Tagging error!");
			System.err.println("Working string:" + in);
			System.err.println("Current word:" + word);
		}
		return find;
	}

	private static String tag (String in) throws Exception {
		if (in.replaceAll("\\s+", "").length() == 0) return "";
		String tagged = POSTag(in);

		// load text base
		TextBaseLoader baseLoader = new TextBaseLoader(TextBaseLoader.DOC_PER_FILE, TextBaseLoader.FILE_NAME, true);
		File tempFile = createFile(tagged);
		TextBase base = baseLoader.load(tempFile);

		// get XML labels
		MutableTextLabels labels = baseLoader.getLabels();

		// evaluate mixup
		MixupProgram p = new MixupProgram(NAMEMIXUP);
		p.eval(labels, base);
		p = new MixupProgram(DATEMIXUP);
		p.eval(labels, base);

		TextLabelsLoader labelsLoader = new TextLabelsLoader();

		// Minorthird's version of marking up XML labels
		// tagged = labelsLoader.createXMLmarkup(tempFile.getName(), labels);

		// My own version of marking up XML labels; doesn't check for overlapping spans
		tagged = createXML(in, labelsLoader.saveTypesAsXML(labels));

		String keepXML[] = {
			"CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "POS", "PRP", "PRPS", "RB", "RBR", "RBS", "RP", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WPS", "WRB",
			"S", "NP", "name", "extracted_date", "extracted_time"
		};
		tagged = filterXML(tagged, keepXML);

		// fix the problem so that end-tag doesn't stick to the next begin-tag.
		// tagged = tagged.replaceAll("(<[^<>]+>[^<>]+?)(\\s+)(</[^<>]+>)", "$1$3$2");

		return tagged;
	}

	private static String filterXML (String tagged, String[] keepXML) {
		Matcher m = Pattern.compile("</?([^<>]+)>").matcher(tagged);
		HashMap delXML = new HashMap();
		while (m.find()) {
			if (delXML.containsKey(m.group(1))) continue;
			boolean keep = false;
			for (int i = 0; i < keepXML.length; i++) {
				if (keepXML[i].equals(m.group(1))) {
					keep = true;
					break;
				}
			}
			if (!keep) delXML.put(m.group(1), null);
		}
		for (Iterator i = delXML.keySet().iterator(); i.hasNext();) {
			String del = (String) i.next();
			tagged = tagged.replaceAll("</?\\Q" + del + "\\E>", "");
		}
		return tagged;
	}

	private static String createXML (String in, String XMLIndex) {
		HashMap hash = new HashMap();
		String temp;
		ArrayList spanList = new ArrayList();
		StringBuffer buf = new StringBuffer(in);
		Matcher m = Pattern.compile("[\r\n]  <(\\S+) lo=(\\d+) hi=(\\d+)>").matcher(XMLIndex);

		while (m.find()) {
			ArrayList span = new ArrayList();
			span.add(m.group(1));
			span.add(new Integer(m.group(2)));
			span.add(new Integer(m.group(3)));
			spanList.add(span);
		}

		Collections.sort(spanList, new Comparator() {
			public int compare (Object o1, Object o2) {
				Integer lo1 = (Integer) ((ArrayList) o1).get(1);
				Integer lo2 = (Integer) ((ArrayList) o2).get(1);
				Integer hi1 = (Integer) ((ArrayList) o1).get(2);
				Integer hi2 = (Integer) ((ArrayList) o2).get(2);
				if (lo1.compareTo(lo2) == 0)
					return hi2.compareTo(hi1);
				else return lo1.compareTo(lo2);
			}
		});

		for (Iterator i = spanList.iterator(); i.hasNext(); ) {
			ArrayList span = (ArrayList) i.next();
			String xml = (String) span.get(0);
			String start = "<" + xml + ">";
			String end = "</" + xml + ">";
			Integer lo = (Integer) span.get(1);
			Integer hi = (Integer) span.get(2);
			temp = (String) hash.get(lo);
			hash.put(lo, ((temp == null) ? "" : temp) + start);
			temp = (String) hash.get(hi);
			hash.put(hi, end + ((temp == null) ? "" : temp));
		}

		ArrayList al = new ArrayList(hash.keySet());
		Collections.sort(al);
		int counter = 0;
		for (Iterator i = al.iterator(); i.hasNext();) {
			Integer index = (Integer) i.next();
			String tags = (String) hash.get(index);
			buf.insert(index.intValue() + counter, tags);
			counter += tags.length();
		}
		return buf.toString();
	}

	// create a temp file from a string
	private static File createFile (String content) {
		File temp = null;
		BufferedWriter bWriter;
		try {
			temp = File.createTempFile("tmp", "");
			temp.deleteOnExit();
			bWriter = new BufferedWriter(new FileWriter(temp));
			bWriter.write(content);
			bWriter.close();
		} catch (IOException ioe) {
			System.err.println("Error creating temp file: " + ioe);
		}
		return temp;
	}

	private static class MinorTaggerThread extends Thread {
		private Socket socket = null;

		public MinorTaggerThread (Socket socket) {
			super("MinorTaggerThread");
			this.socket = socket;
		}

		public void run () {
			StringBuffer buf = new StringBuffer();
			BufferedReader br;
			PrintStream os;
			String line;

			try {
				br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				os = new PrintStream(socket.getOutputStream());

				while ((line = br.readLine()) != null) {
					boolean end_close = false;
					boolean end_continue = false;
					if (line.endsWith("$$$")) {
						end_close = true;
						line = line.substring(0, line.length() - 3);
					} else if (line.endsWith("$$")) {
						end_continue = true;
						line = line.substring(0, line.length() - 2);
					}
					buf.append(line + "\n");
					if (end_close || end_continue) {
						os.println(tag(buf.toString()));
						buf.setLength(0);
					}
					if (end_close) {
						os.close();
						br.close();
						socket.close();
						break;
					}
				}
				System.out.println("[" + (new Date()) + "] Disconnected from " + socket.getRemoteSocketAddress());
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}
}
