/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author mariasam@google.com (Maria Sam)
 * @author sulku@google.com (Marsela Sulku)
 */
@RunWith(JUnit4.class)
public class HidingFieldTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(HidingField.class, getClass());

  @Test
  public void hidingFieldPositiveCases() {
    compilationHelper
        .addSourceLines(
            "HidingFieldPositiveCases1.java",
"""
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class HidingFieldPositiveCases1 {

  /** base class */
  public static class ClassA {
    protected String varOne;
    public int varTwo;
    String varThree;
  }

  /** ClassB has a field with the same name as one in its parent. */
  public static class ClassB extends ClassA {
    // BUG: Diagnostic contains: ClassA
    private String varOne = "Test";
  }

  /** ClassC has a field with the same name as one in its grandparent. */
  public static class ClassC extends ClassB {
    // BUG: Diagnostic contains: ClassA
    public int varTwo;
  }

  /**
   * ClassD has multiple fields with the same name as those in its grandparent, as well as other
   * unrelated members.
   */
  public static class ClassD extends ClassB {
    // BUG: Diagnostic contains: ClassA
    protected int varThree;
    // BUG: Diagnostic contains: ClassA
    int varTwo;
    String randOne;
    String randTwo;
  }

  /** ClassE has same variable name as grandparent */
  public static class ClassE extends ClassC {
    // BUG: Diagnostic contains: ClassC
    public String varTwo;
  }

  public static class ClassF extends ClassA {
    @SuppressWarnings("HidingField") // no warning because it's suppressed
    public String varThree;
  }

  public static class ClassG extends ClassF {
    // BUG: Diagnostic contains: ClassF
    String varThree;
  }
}
""")
        .addSourceLines(
            "HidingFieldPositiveCases2.java",
"""
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class HidingFieldPositiveCases2 {

  /**
   * ClassA extends a class from a different file and ClassA has a member with the same name as its
   * parent
   */
  public class ClassA extends HidingFieldPositiveCases1.ClassB {
    // BUG: Diagnostic contains: hiding ClassA.varTwo
    private int varTwo;
  }

  /**
   * ClassB extends a class from a different file and ClassB has a member with the same name as its
   * grandparent
   */
  public class ClassB extends HidingFieldPositiveCases1.ClassB {
    // BUG: Diagnostic contains: hiding ClassA.varOne
    public int varOne = 2;
  }
}
""")
        .doTest();
  }

  @Test
  public void hidingFieldNegativeCases() {
    compilationHelper
        .addSourceLines(
            "HidingFieldNegativeCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class HidingFieldNegativeCases {
  // base class
  static class ClassA {
    public int varOne;
  }

  // subclass with member variables of different names
  static class ClassB extends ClassA {
    private String varTwo;
    private int varThree;
    public static int varFour;
    public int varFive;
  }

  // subclass with initialized member variable of different name
  static class ClassC extends ClassB {
    // publicly-visible static members in superclasses are pretty uncommon, and generally
    // referred to by qualification, so this 'override' is OK
    private String varFour = "Test";

    // The supertype's visibility is private, so this redeclaration is OK.
    private int varThree;

    // warning suppressed when overshadowing variable in parent
    @SuppressWarnings("HidingField")
    public int varFive;

    // warning suppressed when overshadowing variable in grandparent
    @SuppressWarnings("HidingField")
    public int varOne;
  }

  // subclass with member *methods* with the same name as superclass member variable -- this is ok
  static class ClassD extends ClassC {
    public void varThree() {}

    public void varTwo() {}
  }
}
""")
        .doTest();
  }
}
