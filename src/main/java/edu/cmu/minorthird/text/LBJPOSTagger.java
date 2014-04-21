package edu.cmu.minorthird.text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import LBJ2.nlp.SentenceSplitter;
import LBJ2.nlp.WordSplitter;
import LBJ2.nlp.seg.PlainToTokenParser;
import LBJ2.nlp.seg.Token;
import edu.cmu.minorthird.util.IOUtil;
import edu.illinois.cs.cogcomp.lbj.pos.POSTagger;

/**
 * A wrapper for UIUC CCG's LBJ POS tagger
 * 
 * http://cogcomp.cs.illinois.edu/page/software_view/3
 *
 * @author frank
 */

public class LBJPOSTagger extends StringAnnotator{
	
  static Logger log=Logger.getLogger(LBJPOSTagger.class);

	public LBJPOSTagger(){
		// tell superclass what type of annotation is being provided
		providedAnnotation = "pos";
	}

  /**
   * Returns char based stand-off annotations for POS in the given string
   *
   * @param in String to tag
   * @return tagged String
   */
	
	@Override
  protected CharAnnotation[] annotateString(String content){
		
		String[] input=new String[]{content};

    List<CharAnnotation> tags=new ArrayList<CharAnnotation>();
    
    POSTagger tagger=new POSTagger();
    PlainToTokenParser parser=new PlainToTokenParser(new WordSplitter(new SentenceSplitter(input)));
    for(Token word;(word=(Token)parser.next())!=null;){
    	tags.add(new CharAnnotation(word.start,word.end-word.start,tagger.discreteValue(word)));
    }

    return tags.toArray(new CharAnnotation[tags.size()]);

  }
	
  @Override
	public String explainAnnotation(TextLabels labels,Span documentSpan){
		return "See: http://cogcomp.cs.illinois.edu/page/software_view/3";
	}

  public static void main(String[] args) throws IOException{
  	
    if(args.length!=2){
      System.out.println("Usage: <input_file> <output_file>");
      return;
    }

    File inFile=new File(args[0]);
    File outFile=new File(args[1]);
    
    if(!inFile.exists()||!inFile.isFile()){
      log.fatal("Error: File "+inFile+" could not be found!");
      return;
    }
    
    String content=IOUtil.readFile(inFile);
    
    LBJPOSTagger tagger=new LBJPOSTagger();
    CharAnnotation[] tags=tagger.annotateString(content);
    
    PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile),"utf8"),true);
    for(int i=0;i<tags.length;i++){
    	writer.print(content.substring(tags[i].getOffset(),tags[i].getOffset()+tags[i].getLength()+1));
    	writer.print("[");
    	writer.print(tags[i].getType());
    	writer.print("] ");
    }
    writer.println();
    writer.close();
    
  }


}
