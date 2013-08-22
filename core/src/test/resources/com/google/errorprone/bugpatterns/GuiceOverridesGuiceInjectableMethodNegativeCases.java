package com.google.errorprone.bugpatterns;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class GuiceOverridesGuiceInjectableMethodNegativeCases {

  /**
   * Class with a method foo() annotated with @com.google.inject.Inject.
   */
  public class TestClass1 {
    @com.google.inject.Inject
    public void foo() {}
  }
  
  /**
   * Class with a method foo() annotated with @javax.inject.Inject.
   */
  public class TestClass2 {
    @javax.inject.Inject
    public void foo() {}
  }
  
  /**
   * Class with a method foo() annotated with @javax.inject.Inject that overrides a method 
   * annotated with @com.google.inject.Inject.
   */
  public class TestClass3 extends TestClass1 {
    @javax.inject.Inject
    public void foo() {}
  }
  
  /**
   * Class with a method foo() annotated with @com.google.inject.Inject that overrides a method
   * annoted with @javax.inject.Inject.
   */
  public class TestClass4 extends TestClass2 {
    @com.google.inject.Inject
    public void foo() {}
  }
 
  /**
   * Class with a method foo() annotated with @javax.inject.Inject that overrides a method
   * annotated with @com.google.inject.Inject
   */
  public class TestClass5 extends TestClass1 {
    @javax.inject.Inject
    public void foo() {}
  }
  
  /**
   * Class with a method foo() that is not annotated with @Inject, but overrides a method that is
   * annotated with @com.google.inject.Inject. Warning is suppressed.
   */
  public class TestClass6 extends TestClass1 {
    @SuppressWarnings("GuiceOverridesGuiceInjectableMethod")
    @Override 
    public void foo() {}
  }
  
}
