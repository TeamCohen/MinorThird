package edu.cmu.minorthird.util.gui;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextArea;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;

/**
 * Some test cases for viewers.
 * 
 * @author William cohen
 *
 */

public class ViewerTest extends TestCase
{
	//private static Logger log = Logger.getLogger(ViewerTest.class);

	private Viewer lc,uc,van,indexViewer,lnameListViewer,familyViewer,mainViewer;
	private ParallelViewer par;
	private JList jlist;

	public ViewerTest(String name) { super(name); }
	public ViewerTest() { super("ViewerTest"); }
	public static Test suite() {  return new TestSuite(ViewerTest.class); }
	public static void main(String args[]) {  junit.textui.TestRunner.run(suite()); }

	public void testViewer()
	{
		Logger.getRootLogger().removeAllAppenders();
		org.apache.log4j.BasicConfigurator.configure();
		lc = new ComponentViewer() {
			static final long serialVersionUID=20071023L;
			public boolean canReceive(Object o) {	return o instanceof String;	}
			public JComponent componentFor(Object o) {
				String s = (String)o;
				System.out.println("converting "+s+" to lower case!");
				return new JTextArea(s.toLowerCase());
			}
		};
		uc = new ComponentViewer() {
			static final long serialVersionUID=20071023L;
			public boolean canReceive(Object o) {	return o instanceof String;	}
			public JComponent componentFor(Object o) {
				String s = (String)o;
				System.out.println("converting "+s+" to upper case!");
				return new JTextArea(s.toUpperCase());
			}
		};
		van = new VanillaViewer();
		par = new ParallelViewer();
		par.addSubView("Original",van);
		par.addSubView("LowerCase",lc);
		par.addSubView("UpperCase",uc);

		final Map<String,String[]> m = new HashMap<String,String[]>();
		m.put("Hurst", new String[]{"Matthew", "Wakako", "Hannah"});
		m.put("Cohen", new String[]{"William", "Susan", "Joshua", "Charlie"});
		m.put("Tomikoyo", new String[]{"Takashi", "Laura", "Makoto", "TBA"});

		indexViewer = new IndexedViewer() {
			static final long serialVersionUID=20071023L;
			public Object[] indexFor(Object o) {
				sendSignal(TEXT_MESSAGE,"displaying the "+o+" family");
				return (Object[]) m.get(o);
			}
		};
		lnameListViewer = new ComponentViewer() {
			static final long serialVersionUID=20071023L;
			public JComponent componentFor(Object o) {
				Object[] array = (Object[])o;
				jlist = new JList(array);
				monitorSelections(jlist);
				return jlist;
			}
		};
		lnameListViewer.setContent(m.keySet().toArray());
		familyViewer = new ZoomedViewer(indexViewer,par);
		((ZoomedViewer)familyViewer).setHorizontal();
		mainViewer = new MessageViewer(new ZoomedViewer(lnameListViewer,familyViewer));

		assertNotNull( mainViewer );
	}
}
