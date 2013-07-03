package com.google.errorprone.bugpatterns;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class GuiceOverridesGuiceInjectableMethodPositiveCases {

  /**
   * Class with a method foo() annotated with @com.google.inject.Inject.
   */
  public class TestClass1 {
    @com.google.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @Inject, but overrides a method that is
   * annotated with @com.google.inject.Inject
   */
  public class TestClass2 extends TestClass1 {
    @Override 
    //BUG: Suggestion includes "@Inject"
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @Inject, but overrides a method that in
   * turn is overrides a method that is annotated with @com.google.inject.Inject
   */
  public class TestClass3 extends TestClass2 {
    @Override 
    //BUG: Suggestion includes "@Inject"
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @javax.inject.Inject and overrides a
   * method that is annotated with @com.google.inject.Inject. This class does not contain an error,
   * but it is extended in the next test class.
   */
  public class TestClass4 extends TestClass1 {
    @Override
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @Inject and overrides a method that is is
   * annotated with @javax.inject.Inject. This super method in turn overrides a method that is
   * annoatated with @com.google.inject.Inject.
   */
  public class TestClass5 extends TestClass4 {
    @Override
    //BUG: Suggestion includes "@Inject"
    public void foo() {}
  }
}
