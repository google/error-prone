package com.google.errorprone.bugpatterns;

import com.google.inject.assistedinject.AssistedInject;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectAssistedInjectAndInjectOnSameConstructorNegativeCases {
  /**
   * Class has a single constructor with no annotation.
   */
  public class TestClass1 {
    TestClass1() {}
  }

  /**
   * Class has a constructor with a @javax.inject.Inject annotation.
   */
  public class TestClass2 {
    @javax.inject.Inject
    public TestClass2() {}
  }

  /**
   * Class has a constructor with a @com.google.injectInject annotation.
   */
  public class TestClass3 {
    @com.google.inject.Inject
    public TestClass3() {}
  }

  /**
   * Class has a constructor annotated with @AssistedInject
   */
  public class TestClass4 {
    @AssistedInject
    public TestClass4() {}
  }

  /**
   * Class has one constructor with a @AssistedInject and one with @javax.inject.inject .
   */
  public class TestClass5 {
    @javax.inject.Inject
    public TestClass5(int n) {}

    @AssistedInject
    public TestClass5() {}
  }

  /**
   * Class has one constructor with a @AssistedInject and one with @javax.inject.inject .
   */
  public class TestClass6 {
    @com.google.inject.Inject
    public TestClass6(int n) {}

    @AssistedInject
    public TestClass6() {}
  }
}
