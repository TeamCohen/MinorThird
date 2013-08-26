package LBJ2.util;

import java.util.Arrays;
import LBJ2.classify.Feature;


/**
  * This class implements an expandable array of features that should be
  * faster than java's <code>Vector</code>.
  *
  * @author Nick Rizzolo
 **/
public class FVector implements Cloneable, java.io.Serializable
{
  /** The default capacity of a vector upon first construction. */
  protected static final int defaultCapacity = 8;

  /** The elements of the vector. */
  protected Feature[] vector;
  /** The number of elements in the vector. */
  protected int size;


  /**
    * Constructs a new vector with capacity equal to {@link #defaultCapacity}.
   **/
  public FVector() { this(defaultCapacity); }

  /**
    * Constructs a new vector with the specified capacity.
    *
    * @param c  The initial capacity for the new vector.
   **/
  public FVector(int c) {
    vector = new Feature[Math.max(defaultCapacity, c)];
  }

  /**
    * Constructs a new vector using the specified array as a starting point.
    *
    * @param v  The initial array.
   **/
  public FVector(Feature[] v) {
    if (v.length == 0) vector = new Feature[defaultCapacity];
    else {
      vector = v;
      size = vector.length;
    }
  }

  /**
    * Constructs a copy of a vector starting with capacity equal to that
    * vector's size.
    *
    * @param v  The vector to copy.
   **/
  public FVector(FVector v) {
    int N = v.size();
    if (N == 0) vector = new Feature[defaultCapacity];
    else {
      vector = new Feature[N];
      size = N;
      System.arraycopy(v.vector, 0, vector, 0, N);
    }
  }


  /**
    * Throws an exception when the specified index is negative.
    *
    * @param i  The index.
    * @throws ArrayIndexOutOfBoundsException  When <code>i</code> &lt; 0.
   **/
  protected void boundsCheck(int i) {
    if (i < 0)
      throw
        new ArrayIndexOutOfBoundsException(
            "Attempted to access negative index of FVector.");
  }


  /**
    * Retrieves the value stored at the specified index of the vector, or
    * <code>null</code> if the vector isn't long enough.
    *
    * @param i  The index of the value to retrieve.
    * @return The retrieved value.
    * @throws ArrayIndexOutOfBoundsException  When <code>i</code> &lt; 0.
   **/
  public Feature get(int i) { return get(i, null); }

  /**
    * Retrieves the value stored at the specified index of the vector or
    * <code>d</code> if the vector isn't long enough.
    *
    * @param i  The index of the value to retrieve.
    * @param d  The default value.
    * @return The retrieved value.
    * @throws ArrayIndexOutOfBoundsException  When <code>i</code> &lt; 0.
   **/
  public Feature get(int i, Feature d) {
    boundsCheck(i);
    return i < size ? vector[i] : d;
  }


  /**
    * Sets the value at the specified index to the given value.
    *
    * @param i  The index of the value to set.
    * @param v  The new value at that index.
    * @return The value that used to be at index <code>i</code>.
    * @throws ArrayIndexOutOfBoundsException  When <code>i</code> &lt; 0.
   **/
  public Feature set(int i, Feature v) { return set(i, v, null); }

  /**
    * Sets the value at the specified index to the given value.  If the given
    * index is greater than the vector's current size, the vector will expand
    * to accomodate it.
    *
    * @param i  The index of the value to set.
    * @param v  The new value at that index.
    * @param d  The default value for other new indexes that might get
    *           created.
    * @return The value that used to be at index <code>i</code>.
    * @throws ArrayIndexOutOfBoundsException  When <code>i</code> &lt; 0.
   **/
  public Feature set(int i, Feature v, Feature d) {
    boundsCheck(i);
    expandFor(i, d);
    Feature result = vector[i];
    vector[i] = v;
    return result;
  }


  /**
    * Adds the specified value on to the end of the vector, expanding its
    * capacity as necessary.
    *
    * @param v  The new value to appear last in the vector.
   **/
  public void add(Feature v) {
    expandFor(size, null);
    vector[size - 1] = v;
  }


  /**
    * Adds all the values in the given vector to the end of this vector,
    * expanding its capacity as necessary.
    *
    * @param v  The new vector of values to appear at the end of this vector.
   **/
  public void addAll(FVector v) {
    expandFor(size + v.size - 1, null);
    System.arraycopy(v.vector, 0, vector, size - v.size, v.size);
  }


  /**
    * Removes the element at the specified index of the vector.
    *
    * @param i  The index of the element to remove.
    * @return The removed element.
   **/
  public Feature remove(int i) {
    boundsCheck(i);
    if (i >= size)
      throw
        new ArrayIndexOutOfBoundsException(
            "LBJ: FVector: Can't remove element at index " + i
            + " as it is larger than the size (" + size + ")");
    Feature result = vector[i];
    for (int j = i + 1; j < size; ++j)
      vector[j - 1] = vector[j];
    vector[--size] = null;
    return result;
  }


  /** Returns the value of {@link #size}. */
  public int size() { return size; }


  /** Sorts this vector in increasing order. */
  public void sort() { Arrays.sort(vector, 0, size); }


  /**
    * After calling this method, the new size and capacity of this vector will
    * be equal to the number of non-<code>null</code> elements; all such
    * elements will be retained in the same relative order.
   **/
  public void consolidate() {
    int n = 0; while (n < size && vector[n] != null) ++n;
    int i = n; while (i < size && vector[i] == null) ++i;
    while (i < size) {
      vector[n++] = vector[i++];
      while (i < size && vector[i] == null) ++i;
    }

    if (n < vector.length) {
      size = n;
      Feature[] newVector = new Feature[size];
      System.arraycopy(vector, 0, newVector, 0, size);
      vector = newVector;
    }
  }


  /**
    * Makes sure the capacity and size of the vector can accomodate the
    * given index.  The capacity of the vector is simply doubled until it can
    * accomodate its size.
    *
    * @param index  The index where a new value will be stored.
    * @param d      The default value for other new indexes that might get
    *               created.
   **/
  protected void expandFor(int index, Feature d) {
    if (index < size) return;
    int oldSize = size, capacity = vector.length;
    size = index + 1;
    if (capacity >= size) return;
    while (capacity < size) capacity *= 2;
    Feature[] t = new Feature[capacity];
    System.arraycopy(vector, 0, t, 0, oldSize);
    if (d != null) Arrays.fill(t, oldSize, size, d);
    vector = t;
  }


  /**
    * Returns a new array of features containing the same data as this vector.
   **/
  public Feature[] toArray() {
    Feature[] result = new Feature[size];
    System.arraycopy(vector, 0, result, 0, size);
    return result;
  }


  /**
    * Two <code>FVector</code>s are considered equal if they contain
    * equivalent elements and have the same size.
   **/
  public boolean equals(Object o) {
    if (!(o instanceof FVector)) return false;
    FVector v = (FVector) o;
    return size == v.size && Arrays.equals(vector, v.vector);
  }


  /** A hash code based on the hash code of {@link #vector}. */
  public int hashCode() { return vector.hashCode(); }


  /**
    * Writes a binary representation of this vector to the given stream.
    *
    * @param out  The output stream.
   **/
  public void write(ExceptionlessOutputStream out) {
    out.writeInt(size);
    for (int i = 0; i < size; ++i) vector[i].write(out);
  }


  /**
    * Reads the binary representation of a vector from the specified stream,
    * overwriting the data in this object.
    *
    * @param in The input stream.
   **/
  public void read(ExceptionlessInputStream in) {
    size = in.readInt();
    if (size == 0) vector = new Feature[defaultCapacity];
    else {
      vector = new Feature[size];
      for (int i = 0; i < size; ++i) vector[i] = Feature.readFeature(in);
    }
  }


  /**
    * Returns a shallow clone of this vector; the vector itself is cloned, but
    * the element objects aren't.
   **/
  public Object clone() {
    FVector clone = null;

    try { clone = (FVector) super.clone(); }
    catch (Exception e) {
      System.err.println("Error cloning " + getClass().getName() + ":");
      e.printStackTrace();
      System.exit(1);
    }

    clone.vector = (Feature[]) vector.clone();
    return clone;
  }


  /** Returns a text representation of this vector. */
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("[");
    for (int i = 0; i < size; ++i) {
      result.append(vector[i]);
      if (i + 1 < size) result.append(", ");
    }
    result.append("]");
    return result.toString();
  }
}

