/*
 * Copyright 2026 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link HashCodeInObjectsHash}. */
@RunWith(JUnit4.class)
public class HashCodeInObjectsHashTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(HashCodeInObjectsHash.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(HashCodeInObjectsHash.class, getClass());

  @Test
  public void instanceHashCode() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar.hashCode());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void objectsHashCode() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Objects.hashCode(bar));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedObjectsHashSingleArg() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Objects.hash(bar));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedObjectsHashMultipleArgs() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;
              Object baz;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Objects.hash(bar, baz));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;
              Object baz;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar, baz);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void primitiveWrapperHashCode() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              int a;
              long b;
              boolean c;

              @Override
              public int hashCode() {
                return Objects.hash(Integer.hashCode(a), Long.hashCode(b), Boolean.hashCode(c));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              int a;
              long b;
              boolean c;

              @Override
              public int hashCode() {
                return Objects.hash(a, b, c);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unqualifiedHashCode() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo, hashCode());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo, this);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void deeplyNested() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, (((bar.hashCode()))));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleArgObjectsHash() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hashCode(foo);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleArgObjectsHashWithHashCode() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo.hashCode());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hashCode(foo);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleArgObjectsHashWithNestedSingleArgObjectsHash() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(Objects.hash(foo));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hashCode(foo);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleArgObjectsHashWithNestedMultiArgObjectsHash() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(Objects.hash(foo, bar));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void singleArgObjectsHashStaticImport() {
    helper
        .addInputLines(
            "Test.java",
            """
            import static java.util.Objects.hash;

            class Test {
              Object foo;

              public int customHash() {
                return hash(foo);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              public int customHash() {
                return Objects.hashCode(foo);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void superHashCodeExempt() {
    compilationHelper
        .addSourceLines(
            "Base.java",
            """
            class Base {
              @Override
              public int hashCode() {
                return 42;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test extends Base {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo, super.hashCode());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void qualifiedSuperHashCodeExempt() {
    compilationHelper
        .addSourceLines(
            "Base.java",
            """
            class Base {
              @Override
              public int hashCode() {
                return 42;
              }
            }
            """)
        .addSourceLines(
            "Outer.java",
            """
            import java.util.Objects;

            class Outer extends Base {
              class Inner {
                Object foo;

                @Override
                public int hashCode() {
                  return Objects.hash(foo, Outer.super.hashCode());
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arraysHashCodeExempt() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.Objects;

            class Test {
              Object[] arr;
              Object[][] multiArr;
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Arrays.hashCode(arr), Arrays.deepHashCode(multiArr));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void identityHashCodeExempt() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, System.identityHashCode(bar));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void validObjectsHashUsage() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedEmptyObjectsHash() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Objects.hash());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo, 1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedObjectsHashCodeWithMultiArgObjectsHash() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object a;
              Object b;

              @Override
              public int hashCode() {
                return Objects.hash(Objects.hashCode(Objects.hash(a, b)));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object a;
              Object b;

              @Override
              public int hashCode() {
                return Objects.hash(a, b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedObjectsHashInsideArithmeticExpression() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Objects.hash(bar) + 1);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Objects.hashCode(bar) + 1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedObjectsHashWithInnerReplacement() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;
              Object baz;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Objects.hash(bar.hashCode(), baz));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object bar;
              Object baz;

              @Override
              public int hashCode() {
                return Objects.hash(foo, bar, baz);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void preservesArrayArgumentInVarargs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object[] foo;

              @Override
              public int hashCode() {
                return Objects.hash(foo);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedObjectsHashWithArrayArg() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Objects;

            class Test {
              Object foo;
              Object[] arr;

              @Override
              public int hashCode() {
                return Objects.hash(foo, Objects.hash(arr));
              }
            }
            """)
        .doTest();
  }
}
