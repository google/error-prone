package com.google.errorprone.bugpatterns;

import com.google.inject.assistedinject.AssistedInject;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectAssistedInjectAndInjectOnConstructorsNegativeCases {
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
   * Class has a constructor with a @AssistedInject annotation as well as an injectable field
   */
  public class TestClass5 {
    @javax.inject.Inject
    private int n;

    @AssistedInject
    public TestClass5() {}
  }
}
