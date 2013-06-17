package com.google.errorprone.bugpatterns;

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
public class WrongParameterPackageNegativeCases {

  public void testParameter(Integer x) {}

  public void testParameter(Integer x, Integer y) {}

  public void testParameter2(Integer x, Integer y) {}

  /**
   * Test overrides
   */
  public static class Subclass extends WrongParameterPackageNegativeCases {

    @Override
    public void testParameter(Integer x) {}

    @Override
    public void testParameter(Integer x, Integer y) {}

    public void testParameter(Boolean x, Integer y) {}

    public void testParameter(Boolean x) {}

    @Override
    public void testParameter2(WrongParameterPackageNegativeCases.Integer x, Integer y) {}

    @SuppressWarnings("ParameterPackage")
    public void testParameter(java.lang.Integer x) {}
  }

  /**
   * Ambiguous Integer class
   */
  public static class Integer {
  }
}
