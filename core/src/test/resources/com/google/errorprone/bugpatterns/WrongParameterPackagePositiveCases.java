package com.google.errorprone.bugpatterns;

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
public class WrongParameterPackagePositiveCases {

  public void testParameter(WrongParameterPackageNegativeCases.Integer x) {}

  public void testParameter(Integer x, Integer y) {}

  public void testParameter2(java.lang.Integer x, Integer y) {}

  public void testParameter3(Integer x, Integer y) {}

  /**
   * Test overrides
   */
  public static class Subclass extends WrongParameterPackagePositiveCases {

    //BUG: Suggestion includes "public void testParameter(com.google.errorprone.bugpatterns.WrongParameterPackageNegativeCases.Integer x) {}"
    public void testParameter(Integer x) {}

    //BUG: Suggestion includes "public void testParameter(com.google.errorprone.bugpatterns.WrongParameterPackagePositiveCases.Integer x, com.google.errorprone.bugpatterns.WrongParameterPackagePositiveCases.Integer y) {}"
    public void testParameter(WrongParameterPackageNegativeCases.Integer x, Integer y) {}

    //BUG: Suggestion includes "public void testParameter2(java.lang.Integer x, com.google.errorprone.bugpatterns.WrongParameterPackagePositiveCases.Integer y) {}"
    public void testParameter2(WrongParameterPackageNegativeCases.Integer x, java.lang.Integer y) {}

    //BUG: Suggestion includes "public void testParameter3(com.google.errorprone.bugpatterns.WrongParameterPackagePositiveCases.Integer x, com.google.errorprone.bugpatterns.WrongParameterPackagePositiveCases.Integer y) {}"
    public void testParameter3(java.lang.Integer x, java.lang.Integer y) {}

    /**
     * Ambiguous Integer class
     */
    public static class Integer {
    }
  }

  /**
   * Ambiguous Integer class
   */
  public static class Integer {
  }
}
