package com.google.errorprone.bugpatterns;

import com.google.inject.assistedinject.AssistedInject;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectAssistedInjectAndInjectOnConstructorsPositiveCases {
  /**
   * Class has a constructor annotated with @javax.inject.Inject and another constructor annotated
   * with @AssistedInject.
   */
  public class TestClass1 {
    //BUG: Suggestion includes "remove"
    @javax.inject.Inject
    public TestClass1() {}

    //BUG: Suggestion includes "remove"
    @AssistedInject
    public TestClass1(int n) {}
  }
  
  /**
   * Class has a constructor annotated with @com.google.inject.Inject and another constructor
   * annotated with @AssistedInject.
   */
  public class TestClass2 {
    //BUG: Suggestion includes "remove"
    @com.google.inject.Inject
    public TestClass2() {}

    //BUG: Suggestion includes "remove"
    @AssistedInject
    public TestClass2(int n) {}
  }
  
  /**
   * Class has a constructor annotated with @com.google.inject.Inject, another constructor
   * annotated with @AssistedInject, and a third constructor with no annotation.
   */
  public class TestClass3 {
    //BUG: Suggestion includes "remove"
    @com.google.inject.Inject
    public TestClass3() {}

    //BUG: Suggestion includes "remove"
    @AssistedInject
    public TestClass3(int n) {}

    public TestClass3(String s) {}
  }
}
