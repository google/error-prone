/*
 * Copyright 2014 The Error Prone Authors.
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

/** {@link EqualsHashCode}Test */
@RunWith(JUnit4.class)
public class EqualsHashCodeTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(EqualsHashCode.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "EqualsHashCodeTestPositiveCases.java",
            """
            public class EqualsHashCodeTestPositiveCases {

              public static class EqualsOnly {
                // BUG: Diagnostic contains: Classes that override equals should also override hashCode
                public boolean equals(Object o) {
                  return false;
                }
              }

              public static class HashCodeOnly {
                // BUG: Diagnostic contains: Classes that override hashCode should also override equals
                public int hashCode() {
                  return 42;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void hashCodeOnlyDisabledByFlag() {
    compilationHelper
        .setArgs("-XepOpt:EqualsHashCode:CheckHashCodeWithoutEquals=false")
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int hashCode() {
                return 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "EqualsHashCodeTestNegativeCases.java",
            """
            public class EqualsHashCodeTestNegativeCases {

              public static class EqualsAndHashCode {
                public boolean equals(Object o) {
                  return false;
                }

                public int hashCode() {
                  return 42;
                }
              }

              public static class Neither {}
            }
            """)
        .doTest();
  }

  @Test
  public void superClassWithoutHashCode() {
    compilationHelper
        .addSourceLines("Super.java", "abstract class Super {}")
        .addSourceLines(
            "Test.java",
            """
            class Test extends Super {
              // BUG: Diagnostic contains:
              public boolean equals(Object o) {
                return false;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void superClassWithoutEquals() {
    compilationHelper
        .addSourceLines("Super.java", "abstract class Super {}")
        .addSourceLines(
            "Test.java",
            """
            class Test extends Super {
              // BUG: Diagnostic contains: Classes that override hashCode should also override equals
              public int hashCode() {
                return 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inherited() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            """
            class Super {
              // BUG: Diagnostic contains: Classes that override hashCode should also override equals
              public int hashCode() {
                return 42;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test extends Super {
              public boolean equals(Object o) {
                return false;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inheritedEquals() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            """
            abstract class Super {
              public abstract boolean equals(Object o);

              public abstract int hashCode();
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            abstract class Test extends Super {
              public int hashCode() {
                return 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void interfaceEquals() {
    compilationHelper
        .addSourceLines(
            "I.java",
            """
            interface I {
              boolean equals(Object o);
            }
            """)
        .doTest();
  }

  @Test
  public void abstractHashCode() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            """
            abstract class Super {
              public abstract boolean equals(Object o);

              public abstract int hashCode();
            }
            """)
        .doTest();
  }

  @Test
  public void abstractNoHashCode() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            """
            abstract class Super {
              // BUG: Diagnostic contains:
              public abstract boolean equals(Object o);
            }
            """)
        .doTest();
  }

  @Test
  public void suppressOnEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @SuppressWarnings("EqualsHashCode")
              public boolean equals(Object o) {
                return false;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nopEquals() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public boolean equals(Object o) {
                return super.equals(o);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nopEqualsWithNullable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.annotation.Nullable;

            class Test {
              public boolean equals(@Nullable Object o) {
                return super.equals(o);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void abstractNoEquals() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            """
            abstract class Super {
              // BUG: Diagnostic contains: Classes that override hashCode should also override equals
              public abstract int hashCode();
            }
            """)
        .doTest();
  }

  @Test
  public void suppressOnHashCode() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @SuppressWarnings("EqualsHashCode")
              public int hashCode() {
                return 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nopHashCode() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static class SuperHashCode {
                public int hashCode() {
                  return super.hashCode();
                }
              }

              static class IdentityHashCode {
                public int hashCode() {
                  return System.identityHashCode(this);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void hashCodeDelegatingToFieldIsFlagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private final String value = "";

              // BUG: Diagnostic contains: Classes that override hashCode should also override equals
              public int hashCode() {
                return value.hashCode();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void hashCodeAlwaysThrows() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int hashCode() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void autoValueAndAutoOneOf() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.auto.value.AutoOneOf;
            import com.google.auto.value.AutoValue;

            class Test {
              @AutoValue
              abstract static class MemoizedHashCode {
                abstract String value();

                public abstract int hashCode();
              }

              @AutoOneOf(MemoizedOneOf.Kind.class)
              abstract static class MemoizedOneOf {
                enum Kind {
                  STRING
                }

                abstract Kind getKind();

                abstract String string();

                public abstract int hashCode();
              }

              @AutoValue
              abstract static class ExplicitHashCode {
                abstract String value();

                // BUG: Diagnostic contains: Classes that override hashCode should also override equals
                public int hashCode() {
                  return 42;
                }
              }

              @AutoOneOf(OneOf.Kind.class)
              abstract static class OneOf {
                enum Kind {
                  STRING
                }

                abstract Kind getKind();

                abstract String string();

                // BUG: Diagnostic contains: Classes that override hashCode should also override equals
                public int hashCode() {
                  return 42;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordWithHashCodeOnly() {
    compilationHelper
        .addSourceLines(
            "R.java",
            """
            record R(int x) {
              @Override
              public int hashCode() {
                return x;
              }
            }
            """)
        .doTest();
  }
}
