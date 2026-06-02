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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link BoxingComparator} Test */
@RunWith(JUnit4.class)
public class BoxingComparatorTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(BoxingComparator.class, getClass());

  @Test
  public void comparing_primitiveInt() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                int getInt();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparing(Foo::getInt);
                Comparator<Foo> c2 = Comparator.comparing(x -> x.getInt());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                int getInt();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparingInt(Foo::getInt);
                Comparator<Foo> c2 = Comparator.comparingInt(x -> x.getInt());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void comparing_primitiveLong() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                long getLong();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparing(Foo::getLong);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                long getLong();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparingLong(Foo::getLong);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void comparing_primitiveDouble() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                double getDouble();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparing(Foo::getDouble);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                double getDouble();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparingDouble(Foo::getDouble);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void thenComparing_primitiveInt() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                int getInt();
              }

              void f(Comparator<Foo> base) {
                Comparator<Foo> c1 = base.thenComparing(Foo::getInt);
                Comparator<Foo> c2 = base.thenComparing(x -> x.getInt());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                int getInt();
              }

              void f(Comparator<Foo> base) {
                Comparator<Foo> c1 = base.thenComparingInt(Foo::getInt);
                Comparator<Foo> c2 = base.thenComparingInt(x -> x.getInt());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void comparing_widening() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                byte getByte();

                short getShort();

                char getChar();

                float getFloat();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparing(Foo::getByte);
                Comparator<Foo> c2 = Comparator.comparing(Foo::getShort);
                Comparator<Foo> c3 = Comparator.comparing(Foo::getChar);
                Comparator<Foo> c4 = Comparator.comparing(Foo::getFloat);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                byte getByte();

                short getShort();

                char getChar();

                float getFloat();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparingInt(Foo::getByte);
                Comparator<Foo> c2 = Comparator.comparingInt(Foo::getShort);
                Comparator<Foo> c3 = Comparator.comparingInt(Foo::getChar);
                Comparator<Foo> c4 = Comparator.comparingDouble(Foo::getFloat);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void comparing_blockLambda() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                int getInt();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparing(x -> x.getInt());
                Comparator<Foo> c2 =
                    Comparator.comparing(
                        x -> {
                          return x.getInt();
                        });
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                int getInt();
              }

              void f() {
                Comparator<Foo> c1 = Comparator.comparingInt(x -> x.getInt());
                Comparator<Foo> c2 =
                    Comparator.comparingInt(
                        x -> {
                          return x.getInt();
                        });
              }
            }
            """)
        .doTest();
  }

  @Test
  public void staticImportRefactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import static java.util.Comparator.comparing;
            import static java.util.Comparator.comparingInt;

            import java.util.Comparator;

            class Test {
              interface Foo {
                int getInt();
              }

              void f() {
                Comparator<Foo> c1 = comparing(Foo::getInt);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.util.Comparator.comparing;
            import static java.util.Comparator.comparingInt;

            import java.util.Comparator;

            class Test {
              interface Foo {
                int getInt();
              }

              void f() {
                Comparator<Foo> c1 = comparingInt(Foo::getInt);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCases() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.Comparator;

            class Test {
              interface Foo {
                String getString();

                Integer getBoxedInt();

                boolean getBoolean();

                long getLong();

                static int compare(Foo a, Foo b) {
                  return -1;
                }
              }

              void f(Comparator<Foo> base) {
                Comparator<Foo> c1 = Comparator.comparing(Foo::getString);
                Comparator<Foo> c2 = Comparator.comparing(Foo::getBoxedInt);
                Comparator<Foo> c3 = Comparator.comparing(Foo::getBoolean);
                Comparator<Foo> c4 = Comparator.<Foo, Long>comparing(Foo::getLong);
                Comparator<Foo> c5 = base.thenComparing(Foo::getString);
                Comparator<Foo> c6 = base.thenComparing(Foo::getBoxedInt);
                Comparator<Foo> c7 = base.thenComparing(Foo::compare);
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
