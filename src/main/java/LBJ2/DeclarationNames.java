package LBJ2;

import LBJ2.IR.AST;
import LBJ2.IR.Declaration;
import LBJ2.IR.DeclarationList;


/**
  *
  * @author Nick Rizzolo
 **/
public class DeclarationNames extends Pass
{
  /**
    * Instantiates a pass that runs on an entire {@link AST}.
    *
    * @param ast  The program to run this pass on.
   **/
  public DeclarationNames(AST ast) { super(ast); }


  /**
    * Runs this pass on all nodes of the indicated type.
    *
    * @param list The node to process.
   **/
  public void run(DeclarationList list) {
    for (DeclarationList.DeclarationListIterator I = list.listIterator();
         I.hasNext(); ) {
      Declaration d = I.nextItem();
      System.out.println(d.name);
    }
  }
}

