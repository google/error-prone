/*
 * Copyright 2013 The Error Prone Authors.
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
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@RunWith(JUnit4.class)
public class OverridesTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(Overrides.class, getClass());

  @Test
  public void positiveCase1() {
    compilationHelper
        .addSourceLines(
            "OverridesPositiveCase1.java",
            """
package com.google.errorprone.bugpatterns.testdata;

/**
 * This tests that the a bug is reported when a method override changes the type of a parameter from
 * varargs to array, or array to varargs. It also ensures that the implementation can handles cases
 * with multiple parameters, and whitespaces between the square brackets for array types.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase1 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);

    abstract void arrayMethod(int x, Object[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    // BUG: Diagnostic contains: abstract void arrayMethod(int x, Object[] newNames);
    abstract void arrayMethod(int x, Object... newNames);
  }

  abstract class Child2 extends Base {
    @Override
    // BUG: Diagnostic contains: abstract void varargsMethod(Object... xs);
    abstract void varargsMethod(Object[] xs);
  }

  abstract class Child3 extends Base {
    @Override
    // BUG: Diagnostic contains: abstract void varargsMethod(Object... xs);
    abstract void varargsMethod(Object[] xs);
  }

  abstract class Child4 extends Base {
    @Override
    // BUG: Diagnostic contains: abstract void varargsMethod(Object... xs);
    abstract void varargsMethod(Object[] xs);
  }

  abstract class Child5 extends Base {
    @Override
    // BUG: Diagnostic contains: Varargs
    abstract void varargsMethod(Object[ /**/] xs);
  }

  interface Interface {
    void varargsMethod(Object... xs);

    void arrayMethod(Object[] xs);
  }

  abstract class ImplementsInterface implements Interface {
    @Override
    // BUG: Diagnostic contains:
    public abstract void varargsMethod(Object[] xs);

    @Override
    // BUG: Diagnostic contains:
    public abstract void arrayMethod(Object... xs);
  }

  abstract class MyBase {
    abstract void f(Object... xs);

    abstract void g(Object[] xs);
  }

  interface MyInterface {
    void f(Object[] xs);

    void g(Object... xs);
  }

  abstract class ImplementsAndExtends extends MyBase implements MyInterface {
    // BUG: Diagnostic contains:
    public abstract void f(Object... xs);

    // BUG: Diagnostic contains:
    public abstract void g(Object[] xs);
  }

  abstract class ImplementsAndExtends2 extends MyBase implements MyInterface {
    // BUG: Diagnostic contains:
    public abstract void f(Object[] xs);

    // BUG: Diagnostic contains:
    public abstract void g(Object... xs);
  }
}
""")
        .doTest();
  }

  @Test
  public void positiveCase2() {
    compilationHelper
        .addSourceLines(
            "OverridesPositiveCase2.java",
            """
package com.google.errorprone.bugpatterns.testdata;

/**
 * This tests the case where there is a chain of method overrides where the varargs constraint is
 * not met, and the root is a varargs parameter. TODO(cushon): The original implementation tried to
 * be clever and make this consistent, but didn't handle multiple interface inheritance.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase2 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);
  }

  abstract class SubOne extends Base {
    @Override
    // BUG: Diagnostic contains:
    abstract void varargsMethod(Object[] newNames);
  }

  abstract class SubTwo extends SubOne {
    @Override
    // BUG: Diagnostic contains:
    abstract void varargsMethod(Object... xs);
  }

  abstract class SubThree extends SubTwo {
    @Override
    // BUG: Diagnostic contains:
    abstract void varargsMethod(Object[] newNames);
  }
}\
""")
        .doTest();
  }

  @Test
  public void positiveCase3() {
    compilationHelper
        .addSourceLines(
            "OverridesPositiveCase3.java",
            """
package com.google.errorprone.bugpatterns.testdata;

/**
 * This tests the case where there is a chain of method overrides where the varargs constraint is
 * not met, and the root has an array parameter. TODO(cushon): The original implementation tried to
 * be clever and make this consistent, but didn't handle multiple interface inheritance.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase3 {
  abstract class Base {
    abstract void arrayMethod(Object[] xs);
  }

  abstract class SubOne extends Base {
    @Override
    // BUG: Diagnostic contains:
    abstract void arrayMethod(Object... newNames);
  }

  abstract class SubTwo extends SubOne {
    @Override
    // BUG: Diagnostic contains:
    abstract void arrayMethod(Object[] xs);
  }

  abstract class SubThree extends SubTwo {
    @Override
    // BUG: Diagnostic contains:
    abstract void arrayMethod(Object... newNames);
  }
}\
""")
        .doTest();
  }

  @Test
  public void positiveCase4() {
    compilationHelper
        .addSourceLines(
            "OverridesPositiveCase4.java",
            """
package com.google.errorprone.bugpatterns.testdata;

import java.util.Map;

/**
 * Test that the suggested fix is correct in the presence of whitespace, comments.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase4 {

  @interface Note {}

  abstract class Base {
    abstract void varargsMethod(@Note final Map<Object, Object>... xs);

    abstract void arrayMethod(@Note final Map<Object, Object>[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    // BUG: Diagnostic contains: (@Note final Map<Object, Object> /* asd */[] /* dsa */ xs);
    abstract void arrayMethod(@Note final Map<Object, Object> /* asd */... /* dsa */ xs);
  }

  abstract class Child2 extends Base {
    @Override
    // TODO(cushon): improve testing infrastructure so we can enforce that no fix is suggested.
    // BUG: Diagnostic contains: Varargs
    abstract void varargsMethod(@Note final Map<Object, Object> /*dsa*/[ /* [ */] /* dsa */ xs);
  }
}
""")
        .doTest();
  }

  @Test
  public void positiveCase5() {
    compilationHelper
        .addSourceLines(
            "OverridesPositiveCase5.java",
            """
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase5 {

  abstract class Base {
    abstract void varargsMethod(Object[] xs, Object... ys);

    abstract void arrayMethod(Object[] xs, Object[] ys);
  }

  abstract class Child1 extends Base {
    @Override
    // BUG: Diagnostic contains: Did you mean 'abstract void arrayMethod(Object[] xs, Object[] ys);'
    abstract void arrayMethod(Object[] xs, Object... ys);

    @Override
    // BUG: Diagnostic contains: Did you mean 'abstract void varargsMethod(Object[] xs, Object...
    // ys);'
    abstract void varargsMethod(Object[] xs, Object[] ys);

    void foo(Base base) {
      base.varargsMethod(null, new Object[] {}, new Object[] {}, new Object[] {}, new Object[] {});
    }
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase1() {
    compilationHelper
        .addSourceLines(
            "OverridesNegativeCase1.java",
            """
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesNegativeCase1 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);

    abstract void arrayMethod(Object[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    abstract void varargsMethod(final Object... newNames);
  }

  abstract class Child2 extends Base {
    @Override
    abstract void arrayMethod(Object[] xs);
  }

  static class StaticClass {
    static void staticVarargsMethod(Object... xs) {}

    static void staticArrayMethod(Object[] xs) {}
  }

  interface Interface {
    void varargsMethod(Object... xs);

    void arrayMethod(Object[] xs);
  }

  abstract class ImplementsInterface implements Interface {
    public abstract void varargsMethod(Object... xs);

    public abstract void arrayMethod(Object[] xs);
  }
}

// Varargs methods might end up overriding synthetic (e.g. bridge) methods, which will have already
// been lowered into a non-varargs form. Test that we don't report errors when a varargs method
// overrides a synthetic non-varargs method:

abstract class One {
  static class Builder {
    Builder varargsMethod(String... args) {
      return this;
    }
  }
}

class Two extends One {
  static class Builder extends One.Builder {
    @Override
    public Builder varargsMethod(String... args) {
      super.varargsMethod(args);
      return this;
    }
  }
}

class Three extends Two {
  static class Builder extends Two.Builder {
    @Override
    public Builder varargsMethod(String... args) {
      super.varargsMethod(args);
      return this;
    }
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase2() {
    compilationHelper
        .addSourceLines(
            "OverridesNegativeCase2.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author cushon@google.com (Liam Miller-Cushon)
             */
            public class OverridesNegativeCase2 {
              abstract class Base {
                abstract void varargsMethod(Object... xs);
              }

              abstract class SubOne extends Base {
                @Override
                abstract void varargsMethod(Object... newNames);
              }

              abstract class SubTwo extends SubOne {
                @Override
                abstract void varargsMethod(Object... xs);
              }

              abstract class SubThree extends SubTwo {
                @Override
                abstract void varargsMethod(Object... newNames);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase3() {
    compilationHelper
        .addSourceLines(
            "OverridesNegativeCase3.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author cushon@google.com (Liam Miller-Cushon)
             */
            public class OverridesNegativeCase3 {
              abstract class Base {
                abstract void arrayMethod(Object[] xs);
              }

              abstract class SubOne extends Base {
                @Override
                abstract void arrayMethod(Object[] xs);
              }

              abstract class SubTwo extends SubOne {
                @Override
                abstract void arrayMethod(Object[] xs);
              }

              abstract class SubThree extends SubTwo {
                @Override
                abstract void arrayMethod(Object[] xs);
              }
            }\
            """)
        .doTest();
  }
}
