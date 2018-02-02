package edu.cmu.minorthird.classify;

import java.util.Iterator;

import junit.framework.TestCase;

/**
 * This class...
 * @author ksteppe
 */
public abstract class InstanceTestBase extends TestCase
{
  protected Instance instance;
  protected static final Feature hello = new Feature("token eq hello");
  protected static final Feature world = new Feature("token eq world");

  public InstanceTestBase(String name)
  { super(name); }

  public void testBinaryFeatures()
  {
    assertEquals(1d, instance.getWeight(hello), 0.01);
    assertEquals(1d, instance.getWeight(world), 0.01);

    Iterator<Feature> it = instance.binaryFeatureIterator();
    assertEquals(new Feature("token eq croutons"), it.next());
    assertEquals(hello, it.next());
  }

  public void testNumericFeatures()
  {
    assertEquals(1d, instance.getWeight(hello), 0.01);
    assertEquals(10d, instance.getWeight(world), 0.01);
    assertEquals(0.5, instance.getWeight(new Feature("token eq purple")), 0.01);
    assertEquals(Double.MAX_VALUE, instance.getWeight(new Feature("token eq max")), 0.01);
    assertEquals(new Double(Double.NaN), new Double(instance.getWeight(new Feature("token eq nan"))));
  }

  public void testMixedFeatures()
  {
    testBinaryFeatures();
    testNumericFeatures();

    Iterator<Feature> it = instance.featureIterator();
    assertEquals(new Feature("token eq croutons"), it.next());
    assertEquals(new Feature("token eq zzfencepost"), it.next());
    assertEquals(new Feature("token eq hello"), it.next());
    assertEquals(new Feature("token eq max"), it.next());
  }

}
