package edu.cmu.minorthird.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * A lightweight command-line processing tool.
 *
 * @author William Cohen
 */
public abstract class BasicCommandLineProcessor implements CommandLineProcessor,Saveable{
	
	private static Logger log=Logger.getLogger(BasicCommandLineProcessor.class);

	// cache args last processed for saving
	private String[] processedArgs=null;

	// cache values associated with args
	private Properties argValues=new Properties();

	@Override
	public final void processArguments(String[] args){
		processedArgs=args;
		int k=consumeArguments(args,0);
		if (k<args.length) throw new IllegalArgumentException("illegal argument "+args[k]);
	}
	
	@Override
	public boolean shouldTerminate(){
		return false;
	}

	@Override
	public final int consumeArguments(String[] args,int startPos){
		try{
			int pos=startPos;
			while(pos<args.length){
				String arg=args[pos];
				if(arg.startsWith("--")){
					arg=arg.substring(2);
				}else if(arg.startsWith("-")){
					arg=arg.substring(1);
				}else{
					return pos-startPos;
				}
				try{
					log.debug("Looking up method '"+arg+"' in "+getClass());
					Method m=getClass().getMethod(arg,new Class[]{});
					log.debug("Consuming '-"+arg+"' in "+getClass());
					System.out.println("Option: "+arg);
					Object result=m.invoke(this,new Object[]{});
					pos+=1;
					if(result instanceof CommandLineProcessor&&result!=null){
						pos+=((CommandLineProcessor)result).consumeArguments(args,pos);
					}
					//For when a label follows the argument
				}catch(NoSuchMethodException ex){
					try{
						Method ms=getClass().getMethod(arg,new Class[]{String.class});
						if(pos+1<args.length){
							log.debug("Consuming '-"+arg+"' '"+args[pos+1]+"' in "+getClass());
							System.out.println("Option: "+arg+"="+args[pos+1]);
							Object result=ms.invoke(this,new Object[]{args[pos+1]});
							pos+=2;
							if(result instanceof CommandLineProcessor){
								pos+=((CommandLineProcessor)result).consumeArguments(args,pos);
							}
						}else{
							throw new IllegalArgumentException(
									"no argument found to option '-"+arg+"'");
						}
					}catch(NoSuchMethodException ex2){
						return pos-startPos;
					}
				}
			}
			return pos-startPos;
		}catch(IllegalAccessException iax){
			iax.printStackTrace();
			throw new IllegalArgumentException("error: "+iax);
		}catch(InvocationTargetException itx){
			itx.printStackTrace();
			throw new IllegalArgumentException("error: "+itx);
		}
	}

	/** Implements the -config option.
	 */
	public void config(String fileName){
		config(fileName,this);
	}

	/** Implements the -config option for the given clp.  This is static
	 * so that JointCommandLineProcessor can use it also.
	 */
	static public void config(String fileName,CommandLineProcessor clp){
		try{
			String[] fileOptions=loadOptionsInPropertiesFormat(new File(fileName));
			clp.processArguments(fileOptions);
		}catch(IOException ex){
			throw new IllegalArgumentException("error opening "+fileName+": "+ex);
		}
	}

	@Override
	public void usage(String errorMessage){
		System.out.println(errorMessage);
		usage();
	}

	/** Override this to print a meaningful usage error. 
	 * Default will list all commands other than 'usage',
	 * 'help', 'getX', and 'setX'.
	 */
	@Override
	public void usage(){
		Method[] genericMethods=Object.class.getMethods();
		Set<String> stopList=new HashSet<String>();
		stopList.add("usage");
		stopList.add("help");
		for(int i=0;i<genericMethods.length;i++){
			Class<?>[] params=genericMethods[i].getParameterTypes();
			if(params.length==0||params.length==1&&params[0].equals(String.class)){
				stopList.add(genericMethods[i].getName());
			}
		}
		log.info("usage for "+getClass());
		Method[] methods=getClass().getMethods();
		for(int i=0;i<methods.length;i++){
			Class<?>[] params=methods[i].getParameterTypes();
			if(!(stopList.contains(methods[i].getName())||
					methods[i].getName().startsWith("get")||methods[i].getName()
					.startsWith("set"))){
				if(params.length==0){
					System.out.print(" [-"+methods[i].getName()+"]");
				}else if(params.length==1&&params[0].equals(String.class)){
					System.out.print(" [-"+methods[i].getName()+" foo]");
				}
			}
		}
		System.out.println();
	}

	/** Override this to make --help do something other than call usage */
	public void help(){
		usage();
	}

	/** 
	 * A list of arguments from the command line, in order. 
	 * For instance, if the command line includes -foo bar,
	 * then "foo" will appear on the propertyList. 
	 */
	protected List<String> propertyList(){
		List<String> result=new ArrayList<String>();
		argValues.clear();
		int k=0;
		while(k<processedArgs.length){
			String opt=processedArgs[k];
			String val="";
			int delta=1;
			if(k+1<processedArgs.length&&!processedArgs[k+1].startsWith("-")){
				val=processedArgs[k+1];
				delta=2;
			}
			String prop=opt.substring(1);
			result.add(prop);
			argValues.setProperty(prop,val);
			k+=delta;
		}
		return result;
	}

	/** 
	 * The value assigned to a property from the command line.
	 * For instance, if the command line includes -foo bar,
	 * then propertyValue("foo") will return "bar".
	 * This can only be called after propertyList has been
	 * called.
	 */
	protected String propertyValue(String property){
		return argValues.getProperty(property);
	}

	// 
	// implements Saveable
	// 
	private static final String CONFIG_FORMAT_NAME="Configuration file";

	private static final String CONFIG_FORMAT_EXT=".config";

	@Override
	public String[] getFormatNames(){
		return new String[]{CONFIG_FORMAT_NAME};
	};

	@Override
	public String getExtensionFor(String format){
		return CONFIG_FORMAT_EXT;
	}

	@Override
	public void saveAs(File file,String format) throws IOException{
		if(!format.equals(CONFIG_FORMAT_NAME))
			throw new IllegalArgumentException("illegal format "+format);
		PrintStream s=new PrintStream(new FileOutputStream(file));
		for(Iterator<String> i=propertyList().iterator();i.hasNext();){
			String prop=i.next();
			s.println(prop+"="+propertyValue(prop));
		}
		s.close();
		//Properties props = new Properties();
		//props.store(new FileOutputStream(file), "auto-saved configuration file");
	}

	@Override
	public Object restore(File file) throws IOException{
		throw new UnsupportedOperationException(
				"Can't restore a command line processor");
	}

//	/**
//	 * Test and/or example code.
//	 * For sample output, invoke this with arguments -scoff you -laff -family -mom gerbil  -taunt
//	 */
//	static public void main(String[] args){
//		CommandLineProcessor p=new BasicCommandLineProcessor(){
//
//			String mother="hamster";
//
//			String father="elderberries";
//
//			String laffter="bwa ha ha ha ha ha!";
//
//			public void laff(){
//				System.out.println("bwa ha ha ha ha ha!");
//			}
//
//			public void scoff(String atWhat){
//				System.out.println("I scoff derisively at "+atWhat+"!");
//			}
//
//			// test that usage doesn't show getters/setters
//			public String getLaff(){
//				return laffter;
//			}
//
//			public void setLaff(String s){
//				laffter=s;
//			}
//
//			public CommandLineProcessor family(){
//				return new BasicCommandLineProcessor(){
//
//					public void mom(String s){
//						mother=s;
//					}
//
//					public void dad(String s){
//						father=s;
//					}
//				};
//			}
//
//			public void taunt(){
//				System.out.println("Your mother was a "+mother+
//						" and your father smelled of "+father+"!!!");
//			}
//			//public void usage() { System.out.println("usage: [-laff] [-scoff foo]"); }
//		};
//		p.processArguments(args);
//	}

	protected CommandLineProcessor tryToGetCLP(Object o){
		if(o instanceof CommandLineProcessor.Configurable){
			return ((CommandLineProcessor.Configurable)o).getCLP();
		}
		throw new IllegalArgumentException(o+
				" can't be configured from the command line");
	}

	static private String[] loadOptionsInPropertiesFormat(File file)
			throws IOException{
		List<String> accum=new ArrayList<String>();
		LineNumberReader in=new LineNumberReader(new FileReader(file));
		String line;
		while((line=in.readLine())!=null){
			if(line.trim().length()>0&&line.charAt(0)!='#'){
				int eqPos=line.indexOf('=');
				if(eqPos<0)
					inputError(in,line,file,"no equal-sign (=) in line");
				else{
					String option=line.substring(0,eqPos).trim();
					String value=line.substring(eqPos+1).trim();
					accum.add("-"+option);
					if(value.length()>0)
						accum.add(value);
				}
			}
		}
		return accum.toArray(new String[accum.size()]);
	}

	static private void inputError(LineNumberReader in,String line,File file,
			String msg){
		log.warn(file+", line "+in.getLineNumber()+": "+msg);
		log.warn(" - incorrect line is '"+line+"'");
	}

}
