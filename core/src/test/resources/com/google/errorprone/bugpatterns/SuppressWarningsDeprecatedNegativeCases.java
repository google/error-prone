package com.google.errorprone.bugpatterns;

/**
 * Negative cases for {@link SuppressWarningsDeprecated}.
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class SuppressWarningsDeprecatedNegativeCases {
  @SuppressWarnings({"deprecation"})
  public static void negativeCase1() {
  }
  
  @SuppressWarnings("deprecation")
  public static void negativeCase2() {
  }
  
  public static void negativeCase3() {
    @SuppressWarnings({"deprecation"})
    int a = 3;
  }
  
  public static void negativeCase4() {
    @SuppressWarnings("deprecation")
    int a = 3;
  }
  
  public static void negativeCase5() {
    @SuppressWarnings({"deprecation"})
    class Foo { }
    Foo a = null;
  }
  
  public static void negativeCase6() {
    @SuppressWarnings("deprecation")
    class Bar { }
    Bar b = null;
  }
}
