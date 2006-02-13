
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.mixup.MixupProgram;
import montylingua.JMontyLingua;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MinorTagger {

	private static JMontyLingua montyLingua;
	private static final File NAMEMIXUP = new File("names.mixup");
	private static final File DATEMIXUP = new File("date.mixup");
	private static final boolean DEBUG = false;
	private static final int DEFAULT_PORT = 9998;

    private static String fileName = "TaggedFile";

    protected static void setFileName (String name) {
	fileName = name;
    }

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

    /* Outputs document marked up with sgml */
	private static String tag (String in) throws Exception {
		if (in.replaceAll("\\s+", "").length() == 0) return "";
		String tagged = in;

		// load text base
		TextBaseLoader baseLoader = new TextBaseLoader();
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
		tagged = labelsLoader.createXMLmarkup(tempFile.getName(), labels);

		String keepXML[] = {"S", "NP", "Name", "extracted_date", "extracted_time"};
		tagged = filterXML(tagged, keepXML);

		// fix the problem so that end-tag doesn't stick to the next begin-tag.
		tagged = tagged.replaceAll("(<[^<>]+>[^<>]+?)(\\s+)(</[^<>]+>)", "$1$3$2");

		return tagged;
	}

    /* Outputs minorthird stand off labels */
    private static String label (String in) throws Exception {
		if (in.replaceAll("\\s+", "").length() == 0) return "";
		String tagged = in;

		// load text base
		TextBaseLoader baseLoader = new TextBaseLoader();
		File tempFile = createFile(tagged);
		TextBase base = baseLoader.load(tempFile);
		tempFile.delete();

		// get XML labels
		MutableTextLabels labels = baseLoader.getLabels();

		// evaluate mixup
		MixupProgram p = new MixupProgram(NAMEMIXUP);
		p.eval(labels, base);
		p = new MixupProgram(DATEMIXUP);
		p.eval(labels, base);

		TextLabelsLoader labelsLoader = new TextLabelsLoader();

		tagged = labelsLoader.printTypesAsOps(labels);

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

	// create a temp file from a string
    private static File createFile (String content) {
		File temp = null;
		BufferedWriter bWriter;
		try {
		    temp =  new File(fileName);
		    //temp = File.createTempFile(fileName, "");
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
			String line, fileName;
			boolean label = false; //determines whether to output labels or sgml

			try {
				br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				os = new PrintStream(socket.getOutputStream());

				while ((line = br.readLine()) != null) {
					boolean end_close = false;
					boolean end_continue = false;
					if(line.equals("labels")) {
					    label = true;
					} else if(line.startsWith("***")) {
					    setFileName(line.substring(3));
					}else if (line.endsWith("$$$")) {
					    end_close = true;
					    line = line.substring(0, line.length() - 3);
					    buf.append(line + "\n");
					} else if (line.endsWith("$$")) {
					    end_continue = true;
					    line = line.substring(0, line.length() - 2);
					    buf.append(line + "\n");
					}
										
					if (end_close || end_continue) {
					    if (label)
						os.println(label(buf.toString()));
					    else os.println(tag(buf.toString()));
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
