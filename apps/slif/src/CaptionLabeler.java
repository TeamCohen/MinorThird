import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.text.gui.*;
import edu.cmu.minorthird.text.learn.*;
import edu.cmu.minorthird.text.learn.experiments.*;
import edu.cmu.minorthird.text.mixup.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;
import edu.cmu.minorthird.classify.algorithms.trees.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.apache.log4j.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

/**
 * Label PNAS caption/figure pairs
 */

public class CaptionLabeler
{
  List figures = new ArrayList();
  Map figureMap = new HashMap(); // absolute caption path => figure
  List classNames = new ArrayList();
  String rootDirName = "";
  File saveTo;

  /** A single figure */
  private class Figure implements Visible
  {
    File captionFile,imageFile;
    String caption;
    Map labeling = new HashMap();
    boolean labeled = false;
    int index;
    public Figure(File c,File i) 
    {
      captionFile = c; imageFile = i;
      caption = loadFileContent(captionFile);      
    }
    public Viewer toGUI()
    {
      Viewer v = new FigViewer(CaptionLabeler.this);
      v.setContent(this);
      return v;
    }
    /** summarize a figure */
    private String id()
    {
      return captionFile.getAbsolutePath();
    }
    private String summary()
    { 
      String s = id();
      int lo = Math.max(0, s.length()-50);
      return s.substring( lo );
    }
  }

  /** View and/or modify labels for a figure */
  static private class FigViewer extends ComponentViewer
  {
    final CaptionLabeler cl;
    public FigViewer(CaptionLabeler cl) { this.cl = cl; }
    public JComponent componentFor(Object o) 
    {
      final Figure f = (Figure)o;
      JPanel figPanel = new JPanel();
      figPanel.setBorder(new TitledBorder(f.summary()));
      ImageIcon icon = new ImageIcon(f.imageFile.getAbsolutePath(),f.summary());
      figPanel.add(new JLabel(icon));
      JTextArea ta = new JTextArea();
      ta.setColumns(30);
      ta.setLineWrap(true);
      ta.setText(f.caption);
      figPanel.add(ta);
      JPanel controlPanel = new JPanel();
      controlPanel.setLayout(new GridBagLayout());
      //controlPanel.add(new JLabel("Controls"));
      final Map cbMap = new HashMap();
      for (int i=0; i<cl.classNames.size(); i++) {
        final String cn = (String)cl.classNames.get(i);
        final boolean cv = f.labeling.get(cn)==null ? false : ((Boolean)f.labeling.get(cn)).booleanValue();
        System.out.println(f.id()+" class "+cn+" labeling: "+f.labeling.get(cn)+" => "+cv); 
        final JCheckBox cb = new JCheckBox(cn,cv);
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              f.labeling.put(cn, new Boolean(cb.isSelected()));
              System.out.println(cn+" => "+cb.isSelected());
            }
          });
        controlPanel.add(cb,col(i));
        cbMap.put(cn,cb);
      }
      controlPanel.add(new JButton(
                         new AbstractAction("Done") {
                           public void actionPerformed(ActionEvent ev) {
                             for (int i=0; i<cl.classNames.size(); i++) {
                               final String cn = (String)cl.classNames.get(i);
                               JCheckBox cb = (JCheckBox)cbMap.get(cn);
                               f.labeling.put(cn, new Boolean(cb.isSelected()));
                             }
                             cl.doneLabeling(f);
                             System.out.println(f.summary()+" is completely labeled");
                             sendSignal(Viewer.OBJECT_UPDATED,f);
                           }}),
                       col(cl.classNames.size()+1));
      JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      panel.add(controlPanel,row(0));
      panel.add(new JScrollPane(figPanel),row(1));
      return new JScrollPane(panel);
    }
  }

  public Viewer toGUI() 
  { 
    Viewer v = new MyViewer(); 
    v.setContent(this);
    return v;
  }

  static private class MyViewer extends SplitViewer
  {
    Viewer top,bottom;
    CaptionLabeler cl = null;
    Figure fig = null; // being viewed
    List prevFigs = new ArrayList();
    protected void initialize() 
    {
      super.initialize();
      splitPane.setResizeWeight(0.01);
      top = topView(); setSubViews(top,nextBottomView());
    }
    public boolean canReceive(Object obj) { return obj instanceof CaptionLabeler; }
    public void receiveContent(Object obj) { cl = (CaptionLabeler)obj; setSubViews(top, nextBottomView()); }
    public void clearContent() { /* do nothing */ }
    protected boolean canHandle(int sig,Object arg,ArrayList senders)
    {
      return sig==Viewer.OBJECT_UPDATED && arg instanceof Figure;
    }
    protected void handle(int sig,Object arg,ArrayList senders)
    {
      Figure f = (Figure)arg;
      System.out.println("signal: updated figure "+f.summary());
      setSubViews(top, nextBottomView());
    }
    private Viewer nextBottomView()
    {
      if (cl==null) return new VanillaViewer("figure goes here");      
      else {
        if (fig!=null) prevFigs.add(fig);
        fig = cl.getRandomUnlabeledFigure();
        if (fig==null) return new VanillaViewer("labeling is complete");
        else return fig.toGUI();
      }
    }
    private Viewer prevBottomView()
    {
      if (cl==null || prevFigs.size()==0) return new VanillaViewer("figure goes here");      
      else {
        fig = (Figure)prevFigs.remove(prevFigs.size()-1);
        return fig.toGUI();
      }
    }
    private Viewer topView()
    {
      Viewer v = new ComponentViewer() {
          public JComponent componentFor(Object o) {

            JPanel panel = new JPanel();
            int colNum = 0;
            panel.setLayout(new GridBagLayout());

            panel.add(
              new JButton(new AbstractAction("Next") {
                public void actionPerformed(ActionEvent ev) {
                  // pretend I got a 'done' signal
                  setSubViews(top, nextBottomView());                  
                }}),
              col(colNum++));

            panel.add(
              new JButton(new AbstractAction("Back") {
                public void actionPerformed(ActionEvent ev) {
                  // pretend I got a 'done' signal
                  setSubViews(top, prevBottomView());                  
                }}),
              col(colNum++));

            panel.add(
              new JButton(new AbstractAction("Save") {
                public void actionPerformed(ActionEvent ev) {
                  cl.saveLabels();
                }}),
              col(colNum++));

            final JTextField tf = new JTextField(10);
            panel.add(
              new JButton(new AbstractAction("New Label:") {
                public void actionPerformed(ActionEvent ev) {
                  cl.requireLabels(new String[]{tf.getText()});
                  setSubViews(top,fig.toGUI());
                }}),
              col(colNum++));
            panel.add(tf, col(colNum++));

            final JTextField tf2 = new JTextField(20);
            tf2.setText("-filename substr-");
            final JComboBox box = new JComboBox();
            panel.add(
              new JButton(new AbstractAction("Check Captions:") {
                  public void actionPerformed(ActionEvent ev) {
                    List figIndices = cl.getMatchingFigures( tf2.getText() );
                    box.removeAllItems();
                    for (Iterator i=figIndices.iterator(); i.hasNext(); ) {
                      box.addItem(i.next());
                    }
                  }
                }),
              col(colNum++));
            box.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ex) {
                  Integer figIndex = (Integer)box.getSelectedItem();
                  if (figIndex!=null) {
                    Figure newFig = cl.getFigure(figIndex.intValue());
                    prevFigs.add(newFig);
                    setSubViews(top,newFig.toGUI());
                  }
                }
              });
            panel.add(tf2,col(colNum++));
            panel.add(box,col(colNum++));
            return panel;
          }
        };
      v.setContent(this);
      return v;
    }
  }
  static private GridBagConstraints row(int i) { return gbc(0,i); }
  static private GridBagConstraints col(int i) { return gbc(i,0); }
  static private GridBagConstraints gbc(int x,int y)
  {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = gbc.weighty = 1.0;
    gbc.gridx = x; gbc.gridy = y;
    return gbc;
  }

  /** Shut down */
  public void finishLabeling()
  {
    saveLabels();
    System.out.println("You're done");
    System.exit(0);
  }

  /** Prepare for labeling for a class */
  public void requireLabels(String[] newClasses)
  {
    int oldLen = classNames.size();
    for (int j=0; j<newClasses.length; j++) {
      if (!classNames.contains(newClasses[j])) {
        classNames.add(newClasses[j]);
      }
    }
  }

  /** Mark f as completely labeled */
  private void doneLabeling(Figure f)
  {
    f.labeled = true;
  }

  /** Get all figures with ids containing a substring */
  public List getMatchingFigures(String substring)
  {
    List accum = new ArrayList();
    for (int i=0; i<figures.size(); i++) {
      Figure f = getFigure(i);
      if (f.id().indexOf(substring)>=0) {
        accum.add(new Integer(i));
      }
    }
    return accum;
  }

  /** Get a random unlabeled figure */
  public Figure getRandomUnlabeledFigure()
  {
    int[] unlabeled = new int[ figures.size() ];
    int numUnlabeled = 0;
    for (int i=0; i<figures.size(); i++) {
      if (!getFigure(i).labeled) {
        unlabeled[numUnlabeled++] = i;
      }
    }
    if (numUnlabeled>0) {
      int rx = new Random().nextInt( numUnlabeled );
      int i = unlabeled[rx];
      System.out.println("picked figure "+i+" of "+numUnlabeled+" unlabeled");
      System.out.println("labeled flag="+getFigure(i).labeled+" for figure "+getFigure(i).summary());
      return getFigure(i);
    } else {
      return null;
    }
  }

  /** Get the i-th figure */
  public Figure getFigure(int i) { return (Figure)figures.get(i); }

  public void saveToFile(File file) { saveTo = file; }

  public void setRootDirName(String r) { rootDirName=r; }

  public void loadLabels(File file)
  {
    System.out.println("loading from "+file);
    try {
      LineNumberReader in = new LineNumberReader(new FileReader(file));
      String line = null;
      int totLab = 0;
      Set classSet = new HashSet();
      while ((line = in.readLine())!=null) {
        String[] cols = line.split("\t");
        String id = cols[0];
        boolean found = false;
        Figure f = (Figure)figureMap.get(id);
        if (f!=null) {
          String cn = cols[1];
          classSet.add(cn);
          f.labeling.put(cn,new Boolean("true".equals(cols[2])));
          f.labeled = true;
          totLab++;
        } else {
          System.out.println("can't find figure for "+line);
        }
      }
      in.close();
      requireLabels((String[])classSet.toArray(new String[classSet.size()]));
      System.out.println(totLab+" labels, "+classNames.size()+" classes");
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void saveLabels() 
  {
    System.out.println("saving to "+saveTo);
    try {
      PrintStream out = new PrintStream(new FileOutputStream(saveTo));
      int totLabs = 0, totFigs = 0;
      for (int i=0; i<figures.size(); i++) {
        Figure f = getFigure(i);
        if (f.labeled) {
          totFigs++;
          for (int j=0; j<classNames.size(); j++) {
            String s = (String)classNames.get(j);
            Boolean b = (Boolean)f.labeling.get(s);
            if (b!=null) {
              out.println(f.id()+"\t"+s+"\t"+b);
              totLabs++;
            }
          }
        }
      }
      System.out.println("saved "+totLabs+" labels for "+classNames.size()+" classes");
      System.out.println(totFigs+"/"+figures.size()+" figures labeled");
      out.close();
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
      System.out.println("continuing...");
    }
  }

  public void saveToMinorthird(String stem) 
  {
    System.out.println("saving in minorthird format to "+stem);
    try {
      File dirFile = new File(stem);
      if (!dirFile.mkdir()) throw new IllegalArgumentException("can't create directory '"+stem+"'");
      PrintStream out = new PrintStream(new FileOutputStream(stem+".labels"));
      int totLabs = 0, totFigs = 0;
      for (int i=0; i<figures.size(); i++) {
        Figure f = getFigure(i);
        String mId = minorthirdId(f);
        PrintStream outF = new PrintStream(new FileOutputStream(new File(dirFile,mId)));
        outF.print(f.caption);
        outF.close();
        if (f.labeled) {
          totFigs++;
          for (int j=0; j<classNames.size(); j++) {
            String s = (String)classNames.get(j);
            Boolean b = (Boolean)f.labeling.get(s);
            if (Boolean.TRUE.equals(b)) {
              out.println("addToType "+mId+" 0 -1 "+s);
              totLabs++;
            }
          }
        }
      }
      for (int i=0; i<figures.size(); i++) {
        Figure f = getFigure(i);
        String mId = minorthirdId(f);
        out.println("closeAllTypes "+mId);
      }
      System.out.println("saved "+totLabs+" labels for "+classNames.size()+" classes");
      System.out.println(totFigs+"/"+figures.size()+" figures labeled");
      out.close();
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
      System.out.println("continuing...");
    }
  }
  public String minorthirdId(Figure f)
  {
    String clId = f.id();
    String rootPath = new File(rootDirName).getAbsolutePath();
    String p = (clId.startsWith(rootPath)) ? clId.substring(rootPath.length()) : clId;
    return p.replaceAll("[\\W]","_");
  }

  /** Load all figures from a slif database */
  public void loadFigures(File file) throws IOException
  {
    if (file.isDirectory()) {
      File[] subDirs = file.listFiles(new FileFilter() {
          public boolean accept(File f) { return f.isDirectory(); }
        });
      File[] figFiles = file.listFiles(new FilenameFilter() {
          public boolean accept(File f,String name) {
            return name.endsWith("-captionContent.txt");
          }
        });
      File[] imgFiles = file.listFiles(new FilenameFilter() {
          public boolean accept(File f,String name) {
            return name.endsWith("-image.jpg");
          }
        });
      if (imgFiles.length!=figFiles.length) 
        throw new IllegalArgumentException("unbalanced dir "+file);
      for (int i=0; i<figFiles.length; i++) {
        addFigure(new Figure(figFiles[i],imgFiles[i]));
      }
      for (int i=0; i<subDirs.length; i++) {      
        loadFigures(subDirs[i]);
      }
    }
  }

  // add a figure
  private void addFigure(Figure f)
  {
    f.index = figures.size();
    figures.add(f);
    figureMap.put(f.id(), f);
    //System.out.println("adding figure "+f.id());
  }

  // move to io.utils?
	static private String loadFileContent(File file)
  {
		try {
			LineNumberReader bReader = new LineNumberReader(new BufferedReader(new FileReader(file)));
			StringBuffer buf = new StringBuffer("");
			String line = null;
			while ((line = bReader.readLine()) != null) {
				buf.append(line);
				buf.append("\n");
			}
			bReader.close();
			return buf.toString();
		} catch (Exception e) {			
			e.printStackTrace();
			System.out.println("Error: "+e.toString());
			return null;
		}
	}

	/** Main program, see usage statement for usage.
	 */

	static public void main(String argv[]) throws IOException
	{
    if (argv.length==0) {
      System.out.println("usage: CaptionLabeler slif-db-root file-to-save-in");
      System.out.println("       slif-db-root is the root of a PNAS-spider-produced directory");
      System.out.println("       file-to-save-in may already exist");
      System.out.println("usage: CaptionLabeler -convert slif-db-root file-to-save-in minorthird-stem");
      System.out.println("       convert captionLabeler's format to standard minorthird format");
      System.out.println("       saved in a directory/.label file pair");
    }
    CaptionLabeler cl = new CaptionLabeler();
    if ("-convert".equals(argv[0])) {
      cl.setRootDirName(argv[1]);
      cl.loadFigures(new File(argv[1]));
      cl.loadLabels(new File(argv[2]));
      cl.saveToMinorthird(argv[3]);
    } else {
      cl.setRootDirName(argv[0]);
      cl.loadFigures(new File(argv[0]));
      System.out.println("loaded "+cl.figures.size()+" figs from "+argv[0]);
      String labelFileName = argv.length>1 ? argv[1] : "captionLabels.txt";
      File labelFile = new File(labelFileName);
      if (labelFile.exists()) cl.loadLabels(labelFile);
      else cl.requireLabels(new String[]{"FMI","1dGel","2dGel","DIC","tissue","clad","graph"});
      cl.saveToFile(labelFile); // mark output file for saving
      //Figure f = (Figure)cl.getRandomUnlabeledFigure();
      //new ViewerFrame("figure",f.toGUI());
      new ViewerFrame(argv[0], cl.toGUI());
    }
  }
}
