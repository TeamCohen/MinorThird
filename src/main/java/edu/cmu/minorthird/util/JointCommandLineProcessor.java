package edu.cmu.minorthird.util;

import org.apache.log4j.Logger;

/**
 * A lightweight command-line processing tool.
 *
 * @author William Cohen
 */
public class JointCommandLineProcessor implements CommandLineProcessor{

	private static Logger log=Logger.getLogger(JointCommandLineProcessor.class);

	private boolean helpConsumed;

	private CommandLineProcessor[] subprocessor;

	public JointCommandLineProcessor(CommandLineProcessor[] subprocessor){
		this.subprocessor=subprocessor;
		helpConsumed=false;
	}

	@Override
	public boolean shouldTerminate(){
		return helpConsumed;
	}

	@Override
	final public void processArguments(String[] args){
		int k=0;
		while(k<args.length){
			int delta=consumeArguments(args,k);
			k+=delta;
			if(delta==0){
				// figure out how many args to skip
				delta++;
				if(k+1<args.length&&!args[k+1].startsWith("-")){
					delta++;
					log.warn("Unknown arguments "+args[k]+" "+args[k+1]+
							" will be ignored");
				}else{
					log.warn("Unknown argument "+args[k]+" will be ignored");
				}
				k+=delta;
			}
		}
	}

	@Override
	final public int consumeArguments(String[] args,int startPos){
		int pos=startPos;
		boolean somethingConsumed=true;
		while(pos<args.length&&somethingConsumed){
			if("-help".equals(args[pos])||"--help".equals(args[pos])){
				helpConsumed=true;
				help();
				pos++;
				continue;
			}
			if("-config".equals(args[pos])||"--config".equals(args[pos])){
				if(pos+1<args.length){
					BasicCommandLineProcessor.config(args[pos+1],this);
					pos+=2;
				}else{
					usage("missing argument for -config");
				}
				continue;
			}
			somethingConsumed=false;
			for(int i=0;!somethingConsumed&&i<subprocessor.length;i++){
				int k=subprocessor[i].consumeArguments(args,pos);
				//System.out.println("arg "+pos+" subprocessor "+i+" result "+k);
				if(k>0){
					log.debug("Subprocessor"+i+" consumed "+k+" args at pos="+pos);
					pos+=k;
					somethingConsumed=true;
				}
			}
		}
		return pos-startPos;
	}

	@Override
	final public void usage(String errorMessage){
		System.out.println(errorMessage);
		usage();
	}

	@Override
	final public void usage(){
		for(int i=0;i<subprocessor.length;i++){
			log.info("Subprocessor"+i+" usage invoked");
			subprocessor[i].usage();
		}
	}

	final public void help(){
		usage();
	}

//	static public void main(String[] args){
//		CommandLineProcessor cp1=new BasicCommandLineProcessor(){
//
//			public void one(){
//				System.out.println("one");
//			}
//		};
//		CommandLineProcessor cp2=new BasicCommandLineProcessor(){
//
//			public void two(){
//				System.out.println("two");
//			}
//
//			public void tree(){
//				System.out.println("three");
//			}
//		};
//		CommandLineProcessor jcp=
//				new JointCommandLineProcessor(new CommandLineProcessor[]{cp1,cp2});
//		jcp.processArguments(args);
//	}
}
