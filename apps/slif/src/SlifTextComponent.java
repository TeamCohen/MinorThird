import edu.cmu.minorthird.ui.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

import java.util.*;
import java.io.*;
import org.apache.log4j.*;


/**
 * 
 */

public class SlifTextComponent extends UIMain
{
    static final String VERSION_NUMBER = "1.0 (2006.10.18; 9.6.4.19)";

    MonotonicTextLabels annLabels = null;

    private ComponentListParams component = new ComponentListParams();
    private CommandLineUtil.SaveParams save = new CommandLineUtil.SaveParams();
    private CommandLineUtil.AnnotatorOutputParams output = new CommandLineUtil.AnnotatorOutputParams();

    // for gui
    public CommandLineUtil.SaveParams getSaveParameters() { return save; }
    public void setSaveParameters(CommandLineUtil.SaveParams p) { save=p; }
    public ComponentListParams getComponentParameters() { return component; } 
    public void setComponentParameters(ComponentListParams cp) { component=cp; } 

    static public class ComponentListParams extends BasicCommandLineProcessor 
    {
        private List nameList = new ArrayList();
        public void use(String s) { useCommaSepComponents(nameList,s); }
        public void usage() {
	    System.out.println("text component parameters:");
	    System.out.println(" -use TC:                 use comma-separated list of text components TC1,...");
	    System.out.println("                          where TCi is CellLine, Caption, or 'XonY'");
	    System.out.println("                            for X=CRF,DictHMM,SemiCRF and Y=Genia,Texas,Yapex");
        }
        // for GUI
        public String getComponentList() {
            return StringUtil.toString(nameList.toArray(),"","",",");
        }
        public void setComponentList(String s) {
            nameList.clear();
            useCommaSepComponents(nameList,s);
        }
        public String getComponentListHelp() {
            return "Comma-separated list of valid arguments for -use option. (See command-line help, available from [Parameters] button)";
        }
        private void useCommaSepComponents(List list,String s)
        {
            String[] arr = s.split(",");
            for (int i=0; i<arr.length; i++) list.add(arr[i]);
        }
    }

    public CommandLineProcessor getCLP()
    {
	return new JointCommandLineProcessor(new CommandLineProcessor[]{gui,base,save,output,component});
    }

    public void doMain()
    {
	annLabels = new NestedTextLabels(base.labels);
        ProgressCounter pc = new ProgressCounter("annotating documents","text component",component.nameList.size());
        for (Iterator i=component.nameList.iterator(); i.hasNext(); ) {
            String annName = (String)i.next();
            System.out.println("apply "+annName);
            InputStream s = null;
            try {
                s = getClass().getClassLoader().getResourceAsStream(annName+".eann");
                if (s==null) throw new IllegalArgumentException("can't find load '"+annName+".eann': stream==null");
                Annotator ann = (Annotator)IOUtil.loadSerialized(s);
                ann.annotate(annLabels);
            } catch (IOException ex) {
                throw new IllegalArgumentException("can't load annotator '"+annName+".eann': "+ex);
            }
            pc.progress();
        }
        pc.finished();

	// echo the annotated labels 
	if (base.showResult) {
	    new ViewerFrame("Annotated Documents",new SmartVanillaViewer(annLabels));
	}

        // save the result
	if (save.saveAs!=null) {
	    try {
		if ("minorthird".equals(output.format)) {
		    new TextLabelsLoader().saveTypesAsOps( annLabels, save.saveAs );
		} else if ("strings".equals(output.format)) {
		    new TextLabelsLoader().saveTypesAsStrings( annLabels, save.saveAs, true );					
                } else if ("xml".equals(output.format)) {
                    new TextLabelsLoader().saveDocsWithEmbeddedTypes( annLabels, save.saveAs );
		} else {
		    throw new IllegalArgumentException("illegal output format "+output.format+" allowed values are "
						       +StringUtil.toString(output.getAllowedOutputFormatValues()));
		}
	    } catch (IOException e) {
		throw new IllegalArgumentException("can't save to "+save.saveAs+": "+e);
	    }
	}
    }
    

    public Object getMainResult()
    {
        return annLabels;
    }
    public static void main(String args[])
    {
        System.out.println("SLIF Text Component Package Version "+VERSION_NUMBER);
	new SlifTextComponent().callMain(args);
    }

};
