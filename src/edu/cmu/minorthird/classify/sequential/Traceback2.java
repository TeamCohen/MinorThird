package edu.cmu.minorthird.classify.sequential;
// Traceback objects

abstract class Traceback {
  int i, j;                     // absolute coordinates
}


// Traceback2 objects

public class Traceback2 extends Traceback {
  public Traceback2(int i, int j)
  { this.i = i; this.j = j; }
}
