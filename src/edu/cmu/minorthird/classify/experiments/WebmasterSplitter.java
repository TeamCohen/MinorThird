package edu.cmu.minorthird.classify.experiments;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.*;
import org.apache.log4j.Logger;
import java.util.*;
import java.io.*;

/**
 * 	A complicated splitter that stratifies samples according to an
 * 	arbitrary "profile" property, and restricts train/test splits to
 * 	not cross boundaries defined by "user" and "request" properties.
 * 	This will do a random split according to users, then a stratified
 * 	split according to requests (with the stratification done
 * 	according to profiles).
 *
 * <p>Constraints on splitting are defined by a file with
 * multiple lines of the form
 * <code><pre>
 * msgId userId requestId profileId
 * </pre></code>.
 * where each Id is a String.
 *
 * <p>The main purpose is of this is to split webmaster data, hence
 * the name.
 *
 * @author William Cohen
 */

public class WebmasterSplitter implements Splitter 
{
	static private Logger log = Logger.getLogger(WebmasterSplitter.class);
	// number of cross-validation splits for the r-th population 
	private int folds = 3;
	// how to split the first r-1 populations
	private double fraction = 0.7;
	// random seed
	private Random random = new Random();

	// map subpopulationId -> user
	private Map userMap = new HashMap(); 
	// map subpopulationId -> request
	private Map requestMap = new HashMap();
	// map subpopulationId -> profile
	private Map profileMap = new HashMap();
	// map request -> profile
	private Map req2ProfileMap = new HashMap();
	// trainList[k] is training list for fold k
	private List[] trainList = null;
	// testList[k] is test list for fold k
	private List[] testList = null;

	public WebmasterSplitter(String constraintFileName, double fraction, int folds) 
	{
		this.folds = folds;
		this.fraction = fraction;
		loadFile(constraintFileName);
	}

	private void loadFile(String constraintFileName) 
	{
		try {
			// read in constraints
			LineNumberReader in = new LineNumberReader(new FileReader(new File(constraintFileName)));
			String line = null;
			while ((line = in.readLine())!=null) {
				if (!line.startsWith("#")) {
					String[] f = line.split(" ");
					if (f.length!=4) badInput(line,constraintFileName,in);
					userMap.put(f[0],f[1]);
					//System.out.println("userMap: '"+f[0]+"' -> "+f[1]);
					requestMap.put(f[0],f[2]);
					profileMap.put(f[0], f[3]);
					String oldProfForRequest = (String)req2ProfileMap.get(f[2]);
					if (oldProfForRequest!=null && !oldProfForRequest.equals(f[3])) {
						log.error("request "+f[2]+" associated with two profiles: "+oldProfForRequest+" and "+f[3]);
						badInput(line,constraintFileName,in);
					}
					req2ProfileMap.put(f[2],f[3]);
				}
			}
			in.close();
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't load from "+constraintFileName+": "+ex.toString());
		}
	}
	private void badInput(String line,String fileName,LineNumberReader in)
	{
		throw new IllegalStateException("Bad input at "+fileName+" line "+in.getLineNumber()+": "+line);
	}

	public void split(Iterator it) 
	{
		// collect set of users, and also set of requests
		// maintaining list of all examples with each request
		List inputList = asList(it);
		Set users = new HashSet();
		Set requests = new HashSet();
		for (Iterator i=inputList.iterator(); i.hasNext(); ) {
			Object o = i.next();
			if (!(o instanceof HasSubpopulationId)) badExample(o,"doesn't have a subpopulationId");
			HasSubpopulationId hsi = (HasSubpopulationId)o;
			String subpop = hsi.getSubpopulationId();
			String userId = (String) userMap.get(subpop);
			if (userId==null) {
				badExample(o,"no userId for "+subpop+" in the constraint file");
			}
			users.add( userId ); 
			String reqId = (String)requestMap.get(subpop);
			requests.add( reqId );
		}

		//split users
		Splitter userSplitter = new RandomSplitter(fraction);
		userSplitter.split(users.iterator());
		Set testUsers = asSet(userSplitter.getTest(0));
		if (log.isDebugEnabled()) log.debug("testUsers = "+testUsers);
 		
		// do cross-val split of requests stratified by profile
		List requestList = new ArrayList( requests.size() );
		requestList.addAll( requests );
		Comparator byProfile = new Comparator() {
				public int compare(Object o1,Object o2) {
					//System.out.println("comparing "+o1+" and "+o2);
					String prof1 = (String)req2ProfileMap.get(o1);
					String prof2 = (String)req2ProfileMap.get(o2);
					return prof1.compareTo(prof2);
				}
			};
		Collections.shuffle(requestList);
		Collections.sort(requestList,byProfile);
		Set[] partition = new Set[folds];
		for (int k=0; k<folds; k++) {
			partition[k] = new HashSet();
		}
		for (int i=0; i<requestList.size(); i++) {
			partition[i%folds].add( requestList.get(i) );
		}
		if (log.isDebugEnabled()) {
			for (int k=0; k<folds; k++) {
				Set profilesForPartition = new TreeSet();
				for (Iterator j=partition[k].iterator(); j.hasNext(); ) 
					profilesForPartition.add( req2ProfileMap.get(j.next()) );
				log.debug("partition "+k+": "+partition[k]+" profiles: "+profilesForPartition);
			}
		}
		
		// allocate the test and training lists
		trainList = new List[folds];
		testList = new List[folds];
		for (int k=0; k<folds; k++) {
			trainList[k] = new ArrayList();
			testList[k] = new ArrayList();
		}

		// populate them
		for (Iterator i=inputList.iterator(); i.hasNext(); ) {
			HasSubpopulationId hsi = (HasSubpopulationId)i.next();
			String subpop = hsi.getSubpopulationId();
			String userId = (String) userMap.get(subpop);
			String reqId = (String)requestMap.get(subpop);
			int k = partitionContaining(partition,reqId);
			if (testUsers.contains(userId)) {
				testList[k].add( hsi );
			} else {
				for (int j=0; j<folds; j++) {
					if (j!=k) trainList[j].add( hsi );
				}
			}
		}
		verifySplit();
	}
	private void badExample(Object o,String msg)
	{
		throw new IllegalArgumentException(msg+" on input "+o);
	}
	private int partitionContaining(Set[] partition, String req)
	{
		for (int i=0; i<partition.length; i++) {
			if (partition[i].contains(req)) return i;
		}
		throw new IllegalStateException("request id "+req+" not found in partition???") ; 
	}

	// check correctness of split
	private void verifySplit()
	{
		for (int k=0; k<folds; k++) {
			for (int i=0; i<trainList[k].size(); i++) {
				Object oi = trainList[k].get(i);
				for (int j=0; j<testList[k].size(); j++) {
					Object oj = testList[k].get(j);
					if (similarTo(oi,oj)) throw new IllegalStateException("bad split for train/test "+oi+"/"+oj);
				}
			}
		}
		for (int k1=0; k1<folds; k1++) {
			for (int k2=0; k2<folds; k2++) {
				if (k2!=k1) {
					for (int j1=0; j1<testList[k1].size(); j1++) {
						for (int j2=0; j2<testList[k2].size(); j2++) {						
							if (testList[k1].get(j1) == testList[k2].get(j2)) {
								throw new IllegalStateException("overlapping test cases for lists "+k1+" and "+k2);
							}
						}
					}
				}
			}
		}
	}
	private boolean similarTo(Object o1,Object o2)
	{
		String subpop1 = ((HasSubpopulationId)o1).getSubpopulationId();
		String subpop2 = ((HasSubpopulationId)o2).getSubpopulationId();
		if (userMap.get(subpop1).equals(userMap.get(subpop2))) return true;
		if (requestMap.get(subpop1).equals(requestMap.get(subpop2))) return true;		
		return false;
	}

	public int getNumPartitions() { return folds; }

	public Iterator getTrain(int k) 
	{
		return trainList[k].iterator();
	}

	public Iterator getTest(int k) 
	{
		return testList[k].iterator();
	}

	public String toString() { return "[WebmasterSplitter "+folds+"]"; }

	static public void main(String args[]) {
		try {
			String file = args[0];

			WebmasterSplitter splitter = new WebmasterSplitter(file,0.7,StringUtil.atoi(args[1]));

			List list = new ArrayList();
			LineNumberReader in = new LineNumberReader(new FileReader(new File(args[0])));
			String line = null;
			while ((line = in.readLine())!=null) {
				if (!line.startsWith("#")) {
					String[] f = line.split(" ");
					final String subpop =  f[0];
					list.add( new HasSubpopulationId() {
							public String toString() { return "[Ex "+subpop+"]"; }
							public String getSubpopulationId() { return subpop; }
						});
				}
			}
			splitter.split( list.iterator() );
			int totTestSize = 0;
			int totTrainSize = 0;
			for (int k=0; k<splitter.getNumPartitions(); k++) {
				totTestSize += asList( splitter.getTest(k)).size();
				totTrainSize += asList( splitter.getTrain(k)).size();
				System.out.println("fold "+k+":");
				System.out.println("test: "+splitter.testList[k]);
				System.out.println("train: "+splitter.trainList[k]);
			}
			System.out.println("data.size = "+list.size());
			System.out.println("total test size="+totTestSize);
			System.out.println("total train size="+totTrainSize);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: WebmasterSplitter constraint-file #folds");
		}
	}
	static private List asList(Iterator i) 
	{
		List accum = new ArrayList();
		while (i.hasNext()) accum.add(i.next());
		return accum;
	}
	static private Set asSet(Iterator i) 
	{
		Set accum = new HashSet();
		while (i.hasNext()) accum.add(i.next());
		return accum;
	}
}
