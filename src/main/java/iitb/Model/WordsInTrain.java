/** WordsInTrain.java
 * 
 * @author Sunita Sarawagi
 * @version 1.3
 */
package iitb.Model;
import java.util.*;
import java.io.*;
import iitb.CRF.*;

/**
 *
 * This is created by FeatureGenTypes and is available for any
 * featureTypes class to use. What it does is provide you counts of
 * the number of times a word occurs in a state.
 * 
 * @author Sunita Sarawagi
 * */


public class WordsInTrain implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 4743850971317817295L;
	class HEntry implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7173469259440218615L;
		int index;
        int cnt;
        int stateArray[];
        HEntry(int v) {
            index = v;
            cnt = 0;
        }
        HEntry(int v, int numStates) {
            index = v;
            cnt = 0;
            stateArray = new int[numStates];
        }
    };
    //TODO: Might be improved by generics.
    class Dictionary extends Hashtable<Object,HEntry> {
        /**
		 * 
		 */
		private static final long serialVersionUID = -6961655882827666386L;
		@Override
        public synchronized boolean containsKey(Object key) {
            return super.containsKey(getKey(key));
        }
        @Override
        public synchronized HEntry get(Object key) {
            return super.get(getKey(key));
        }

        @Override
        public synchronized HEntry put(Object key, HEntry value) {
            return super.put(getKey(key), value);
        }
        public Object getKey(Object w) {
            return tokenGenerator.getKey(w);
        }
    }
    private Dictionary dictionary;
    private int cntsArray[][];
    private int cntsOverAllWords[];
    private int allTotal;

    TokenGenerator tokenGenerator;
    public WordsInTrain() {
        this(new TokenGenerator());
    }
    public WordsInTrain(TokenGenerator tokenGen) {
        tokenGenerator = tokenGen;
        dictionary = new Dictionary(); 
    }
    public Object getKey(Object w) {
        return tokenGenerator.getKey(w);
    }
    int[] getStateArray(int pos) {
        return cntsArray[pos];
    }
    public int getIndex(Object w) {
        return ((dictionary.get(w))).index;
    }
   
    boolean inDictionary(Object w) {
        return (dictionary.get(w) != null);
    }
    public int count(Object w) {
        HEntry entry = dictionary.get(w);
        return ((entry != null)?entry.cnt:0);
    }
    public int count(int wordPos, int state) {
        return getStateArray(wordPos)[state];
    }
    public int count(int state) {
        return cntsOverAllWords[state];
    }
    public int totalCount() {return allTotal;}

    public int dictionaryLength() {return dictionary.size();}

    public int nextStateWithWord(Object w, int prev) {
        if (!inDictionary(w))
            return -1;
        int pos = getIndex(w);
        return nextStateWithWord(pos,prev);
    }
    public int nextStateWithWord(int pos, int prev) {
        int k = 0;
        if (prev >= 0)
            k = prev + 1;
        for (; k < getStateArray(pos).length; k++) {
            if (getStateArray(pos)[k] > 0)
                return k;
        }
        return -1;
    }
    public Enumeration allWords() {return dictionary.keys();}
    private void addDictElem(Object x, int y) {
        HEntry index = dictionary.get(x);
        if (index == null) {
            index = new HEntry(dictionary.size());
            dictionary.put(x, index);
        }
        index.cnt++;
    }
    protected void addDictElem(Object x, int y, int nelems) {
        HEntry index = dictionary.get(x);
        if (index == null) {
            index = new HEntry(dictionary.size(),nelems);
            dictionary.put(x, index);
        }
        index.cnt++;
        if (y >= 0) index.stateArray[y]++;
    }
    void setAggregateCnts(int numStates) {
        cntsOverAllWords = new int[numStates];
        for (int i = 0; i < numStates; i++) {
            cntsOverAllWords[i] = 0;
            for (int m = 0; m < cntsArray.length; m++)
                cntsOverAllWords[i] += getStateArray(m)[i];
            allTotal += cntsOverAllWords[i];
        }
    }
    protected void postProcess(int numStates){
        cntsArray = new int[dictionary.size()][0];
        for (Enumeration e = dictionary.keys() ; e.hasMoreElements() ;) {
            Object key = e.nextElement();
            HEntry entry = dictionary.get(key);
            cntsArray[entry.index] = entry.stateArray;
        }   
        setAggregateCnts(numStates);
    }
    public void train(DataIter trainData, int numStates) {
        for (trainData.startScan(); trainData.hasNext();) {
            DataSequence seq = trainData.next();
            for (int l = 0; l < seq.length(); l++) {
                for (tokenGenerator.startScan(seq.x(l)); tokenGenerator.hasNext();) {
                    addDictElem(tokenGenerator.next(),seq.y(l),numStates);
                }
            }
        }
        postProcess(numStates);
    }

    public void read(BufferedReader in, int numStates) throws IOException {
        int dictLen = Integer.parseInt(in.readLine());
        cntsArray = new int[dictLen][numStates];
        String line;
        for(int l = 0; (l < dictLen) && ((line=in.readLine())!=null); l++) {
            StringTokenizer entry = new StringTokenizer(line," ");
            String key = entry.nextToken();
            int pos = Integer.parseInt(entry.nextToken());
            HEntry hEntry = new HEntry(pos);
            dictionary.put(key,hEntry);
            while (entry.hasMoreTokens()) {
                StringTokenizer scp = new StringTokenizer(entry.nextToken(),":");
                int state = Integer.parseInt(scp.nextToken());
                int cnt = Integer.parseInt(scp.nextToken());
                getStateArray(pos)[state] = cnt;
                hEntry.cnt += cnt;
            }
        }
        setAggregateCnts(numStates);
    }
    public void write(PrintWriter out) throws IOException {
        out.println(dictionary.size());
        for (Enumeration e = dictionary.keys() ; e.hasMoreElements() ;) {
            Object key = e.nextElement();
            int pos = getIndex(key);
            out.print(key + " " + pos);
            for (int s = nextStateWithWord(pos,-1); s != -1; 
            s = nextStateWithWord(pos,s)) {
                out.print(" " + s + ":" + getStateArray(pos)[s]);
            }
            out.println("");
        }	
    }
    /*
    public Collection<String> wordSet() {
        return  dictionary.keySet();
    }
    */
};
