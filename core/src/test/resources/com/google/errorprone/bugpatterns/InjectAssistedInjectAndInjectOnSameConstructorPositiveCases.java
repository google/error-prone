package com.google.errorprone.bugpatterns;

import com.google.inject.assistedinject.AssistedInject;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectAssistedInjectAndInjectOnSameConstructorPositiveCases {
  /**
   * Class has a constructor annotated with @javax.inject.Inject and @AssistedInject.
   */
  public class TestClass1 {
    //BUG: Suggestion includes "remove"
    @javax.inject.Inject
    //BUG: Suggestion includes "remove"
    @AssistedInject
    public TestClass1() {}
  }
  
  /**
   * Class has a constructor annotated with @com.google.inject.Inject and @AssistedInject.
   */
  public class TestClass2 {
    //BUG: Suggestion includes "remove"
    @com.google.inject.Inject
    //BUG: Suggestion includes "remove"
    @AssistedInject
    public TestClass2() {}
  }

}
