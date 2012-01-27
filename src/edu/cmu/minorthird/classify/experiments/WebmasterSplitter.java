package edu.cmu.minorthird.classify.experiments;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.HasSubpopulationId;
import edu.cmu.minorthird.classify.Splitter;
import edu.cmu.minorthird.util.StringUtil;

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

public class WebmasterSplitter<T> implements Splitter<T>{

	static private Logger log=Logger.getLogger(WebmasterSplitter.class);

	// number of cross-validation splits for the r-th population 
	private int folds=3;

	// how to split the first r-1 populations
	private double fraction=0.7;

	// map subpopulationId -> user
	private Map<String,String> userMap=new HashMap<String,String>();

	// map subpopulationId -> request
	private Map<String,String> requestMap=new HashMap<String,String>();

	// map subpopulationId -> profile
	private Map<String,String> profileMap=new HashMap<String,String>();

	// map request -> profile
	private Map<String,String> req2ProfileMap=new HashMap<String,String>();

	// trainList[k] is training list for fold k
	private List<List<T>> trainList=null;

	// testList[k] is test list for fold k
	private List<List<T>> testList=null;

	public WebmasterSplitter(String constraintFileName,double fraction,int folds){
		this.folds=folds;
		this.fraction=fraction;
		loadFile(constraintFileName);
	}

	private void loadFile(String constraintFileName){
		try{
			// read in constraints
			LineNumberReader in=
					new LineNumberReader(new FileReader(new File(constraintFileName)));
			String line=null;
			while((line=in.readLine())!=null){
				if(!line.startsWith("#")){
					String[] f=line.split(" ");
					if(f.length!=4)
						badInput(line,constraintFileName,in);
					//System.out.println("userMap: '"+f[0]+"' -> "+f[1]);
					requestMap.put(f[0],f[2]);
					profileMap.put(f[0],f[3]);
					String oldProfForRequest=req2ProfileMap.get(f[2]);
					if(oldProfForRequest!=null&&!oldProfForRequest.equals(f[3])){
						log.error("request "+f[2]+" associated with two profiles: "+
								oldProfForRequest+" and "+f[3]);
						badInput(line,constraintFileName,in);
					}
					req2ProfileMap.put(f[2],f[3]);
				}
			}
			in.close();
		}catch(IOException ex){
			throw new IllegalArgumentException("can't load from "+constraintFileName+
					": "+ex.toString());
		}
	}

	private void badInput(String line,String fileName,LineNumberReader in){
		throw new IllegalStateException("Bad input at "+fileName+" line "+
				in.getLineNumber()+": "+line);
	}

	@Override
	public void split(Iterator<T> it){
		// collect set of users, and also set of requests
		// maintaining list of all examples with each request
		List<T> inputList=asList(it);
		Set<String> users=new HashSet<String>();
		Set<String> requests=new HashSet<String>();
		for(Iterator<T> i=inputList.iterator();i.hasNext();){
			T example=i.next();
			if(!(example instanceof HasSubpopulationId))
				badExample(example,"doesn't have a subpopulationId");
			HasSubpopulationId hsi=(HasSubpopulationId)example;
			String subpop=hsi.getSubpopulationId();
			String userId=userMap.get(subpop);
			if(userId==null){
				badExample(example,"no userId for "+subpop+" in the constraint file");
			}
			users.add(userId);
			String reqId=requestMap.get(subpop);
			requests.add(reqId);
		}

		//split users
		Splitter<String> userSplitter=new RandomSplitter<String>(fraction);
		userSplitter.split(users.iterator());
		Set<String> testUsers=asSet(userSplitter.getTest(0));
		if(log.isDebugEnabled())
			log.debug("testUsers = "+testUsers);

		// do cross-val split of requests stratified by profile
		List<String> requestList=new ArrayList<String>(requests.size());
		requestList.addAll(requests);
		Comparator<String> byProfile=new Comparator<String>(){
			@Override
			public int compare(String s1,String s2){
				//System.out.println("comparing "+o1+" and "+o2);
				String prof1=req2ProfileMap.get(s1);
				String prof2=req2ProfileMap.get(s2);
				return prof1.compareTo(prof2);
			}
		};
		Collections.shuffle(requestList);
		Collections.sort(requestList,byProfile);
		List<Set<String>> partition=new ArrayList<Set<String>>(folds);
		for(int k=0;k<folds;k++){
			partition.add(new HashSet<String>());
		}
		for(int i=0;i<requestList.size();i++){
			partition.get(i%folds).add(requestList.get(i));
		}
		if(log.isDebugEnabled()){
			for(int k=0;k<folds;k++){
				Set<String> profilesForPartition=new TreeSet<String>();
				for(Iterator<String> j=partition.get(k).iterator();j.hasNext();)
					profilesForPartition.add(req2ProfileMap.get(j.next()));
				log.debug("partition "+k+": "+partition.get(k)+" profiles: "+
						profilesForPartition);
			}
		}

		// allocate the test and training lists
		trainList=new ArrayList<List<T>>(folds);
		testList=new ArrayList<List<T>>(folds);
		for(int k=0;k<folds;k++){
			trainList.add(new ArrayList<T>());
			testList.add(new ArrayList<T>());
		}

		// populate them
		for(Iterator<T> i=inputList.iterator();i.hasNext();){
			T item=i.next();
			HasSubpopulationId hsi=(HasSubpopulationId)item;
			String subpop=hsi.getSubpopulationId();
			String userId=userMap.get(subpop);
			String reqId=requestMap.get(subpop);
			int k=partitionContaining(partition,reqId);
			if(testUsers.contains(userId)){
				testList.get(k).add(item);
			}else{
				for(int j=0;j<folds;j++){
					if(j!=k)
						trainList.get(j).add(item);
				}
			}
		}
		verifySplit();
	}

	private void badExample(Object o,String msg){
		throw new IllegalArgumentException(msg+" on input "+o);
	}

	private int partitionContaining(List<Set<String>> partition,String req){
		for(int i=0;i<partition.size();i++){
			if(partition.get(i).contains(req))
				return i;
		}
		throw new IllegalStateException("request id "+req+
				" not found in partition???");
	}

	// check correctness of split
	private void verifySplit(){
		for(int k=0;k<folds;k++){
			for(int i=0;i<trainList.get(k).size();i++){
				Object oi=trainList.get(k).get(i);
				for(int j=0;j<testList.get(k).size();j++){
					Object oj=testList.get(k).get(j);
					if(similarTo(oi,oj))
						throw new IllegalStateException("bad split for train/test "+oi+"/"+
								oj);
				}
			}
		}
		for(int k1=0;k1<folds;k1++){
			for(int k2=0;k2<folds;k2++){
				if(k2!=k1){
					for(int j1=0;j1<testList.get(k1).size();j1++){
						for(int j2=0;j2<testList.get(k2).size();j2++){
							if(testList.get(k1).get(j1)==testList.get(k2).get(j2)){
								throw new IllegalStateException(
										"overlapping test cases for lists "+k1+" and "+k2);
							}
						}
					}
				}
			}
		}
	}

	private boolean similarTo(Object o1,Object o2){
		String subpop1=((HasSubpopulationId)o1).getSubpopulationId();
		String subpop2=((HasSubpopulationId)o2).getSubpopulationId();
		if(userMap.get(subpop1).equals(userMap.get(subpop2)))
			return true;
		if(requestMap.get(subpop1).equals(requestMap.get(subpop2)))
			return true;
		return false;
	}

	@Override
	public int getNumPartitions(){
		return folds;
	}

	@Override
	public Iterator<T> getTrain(int k){
		return trainList.get(k).iterator();
	}

	@Override
	public Iterator<T> getTest(int k){
		return testList.get(k).iterator();
	}

	@Override
	public String toString(){
		return "[WebmasterSplitter "+folds+"]";
	}

	static public void main(String args[]){
		try{
			String file=args[0];

			WebmasterSplitter<HasSubpopulationId> splitter=
					new WebmasterSplitter<HasSubpopulationId>(file,0.7,StringUtil.atoi(args[1]));

			List<HasSubpopulationId> list=new ArrayList<HasSubpopulationId>();
			LineNumberReader in=
					new LineNumberReader(new FileReader(new File(args[0])));
			String line=null;
			while((line=in.readLine())!=null){
				if(!line.startsWith("#")){
					String[] f=line.split(" ");
					final String subpop=f[0];
					list.add(new HasSubpopulationId(){

						@Override
						public String toString(){
							return "[Ex "+subpop+"]";
						}

						@Override
						public String getSubpopulationId(){
							return subpop;
						}
					});
				}
			}
			splitter.split(list.iterator());
			int totTestSize=0;
			int totTrainSize=0;
			for(int k=0;k<splitter.getNumPartitions();k++){
				totTestSize+=splitter.asList(splitter.getTest(k)).size();
				totTrainSize+=splitter.asList(splitter.getTrain(k)).size();
				System.out.println("fold "+k+":");
				System.out.println("test: "+splitter.testList.get(k));
				System.out.println("train: "+splitter.trainList.get(k));
			}
			System.out.println("data.size = "+list.size());
			System.out.println("total test size="+totTestSize);
			System.out.println("total train size="+totTrainSize);

		}catch(Exception e){
			e.printStackTrace();
			System.out.println("usage: WebmasterSplitter constraint-file #folds");
		}
	}

	private List<T> asList(Iterator<T> i){
		List<T> accum=new ArrayList<T>();
		while(i.hasNext())
			accum.add(i.next());
		return accum;
	}

	private Set<String> asSet(Iterator<String> i){
		Set<String> accum=new HashSet<String>();
		while(i.hasNext())
			accum.add(i.next());
		return accum;
	}
}
