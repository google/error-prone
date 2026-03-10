/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link AssertThrowsMultipleStatements}Test */
@RunWith(JUnit4.class)
public class AssertThrowsMinimizerTest {

  private final BugCheckerRefactoringTestHelper compilationHelper =
      BugCheckerRefactoringTestHelper.newInstance(AssertThrowsMinimizer.class, getClass())
          .addInputLines(
              "Foo.java",
              """
              interface Foo {
                static Builder builder() {
                  return null;
                }

                interface Builder {
                  Builder setBar(Bar bar);

                  Foo build();
                }
              }
              """)
          .expectUnchanged()
          .addInputLines(
              "Bar.java",
              """
              class Bar {}
              """)
          .expectUnchanged()
          .addInputLines(
              "Helper.java",
              """
              class Helper {
                void consume(int i) {}

                static int onlyUnchecked() {
                  return 0;
                }

                static int checked() throws java.io.IOException {
                  return 1;
                }

                static int otherChecked() throws java.sql.SQLException {
                  return 2;
                }

                static int bothChecked() throws java.io.IOException, java.sql.SQLException {
                  return 3;
                }
              }
              """)
          .expectUnchanged();

  @Test
  public void refactor() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                assertThrows(
                    IllegalStateException.class,
                    () -> {
                      Foo.builder().setBar(new Bar());
                    });
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                Foo.Builder builder = Foo.builder();
                Bar bar = new Bar();
                assertThrows(IllegalStateException.class, () -> builder.setBar(bar));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void variable() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                IllegalStateException ise =
                    assertThrows(
                        IllegalStateException.class,
                        () -> {
                          Foo.builder().setBar(new Bar());
                        });
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                Foo.Builder builder = Foo.builder();
                Bar bar = new Bar();
                IllegalStateException ise = assertThrows(IllegalStateException.class, () -> builder.setBar(bar));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void negative() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import com.google.common.collect.ImmutableList;

            class Test {
              void f() {
                ImmutableList.Builder<Integer> builder =
                    ImmutableList.<Integer>builder().add(1).add(Integer.valueOf(2));
                assertThrows(IllegalStateException.class, () -> builder.add(2));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeNull() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              void f() {
                List<Integer> list = new ArrayList<>();
                assertThrows(IllegalStateException.class, () -> list.add(null));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeField() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              List<Integer> list = new ArrayList<>();

              void f() {
                assertThrows(IllegalStateException.class, () -> list.add(42));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeFieldQualifiedWithThis() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.util.List;
            import java.util.ArrayList;

            class Test {
              List<Integer> list = new ArrayList<>();

              void f() {
                assertThrows(IllegalStateException.class, () -> this.list.add(42));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void twoAssertions() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                Bar bar = new Bar();
                assertThrows(IllegalStateException.class, () -> Foo.builder().setBar(bar));
                assertThrows(IllegalStateException.class, () -> Foo.builder().setBar(bar));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                Bar bar = new Bar();
                Foo.Builder builder = Foo.builder();
                assertThrows(IllegalStateException.class, () -> builder.setBar(bar));
                Foo.Builder builder2 = Foo.builder();
                assertThrows(IllegalStateException.class, () -> builder2.setBar(bar));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void rpcClientContextCreate_isNotHoisted() {
    compilationHelper
        .addInputLines(
            "RpcClientContext.java",
            """
            package com.google.net.rpc3.client;

            public class RpcClientContext {
              public static RpcClientContext create() {
                return null;
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            import com.google.net.rpc3.client.RpcClientContext;

            class Test {
              void consume(RpcClientContext r) {}

              void m() {
                assertThrows(IllegalArgumentException.class, () -> consume(RpcClientContext.create()));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void checkedException_argOnlyThrowsUnchecked_noHoist() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.io.IOException;

            class Test {
              void f(Helper helper) {
                assertThrows(IOException.class, () -> helper.consume(Helper.onlyUnchecked()));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void uncheckedException_argOnlyThrowsUnchecked_hoist() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f(Helper helper) {
                assertThrows(RuntimeException.class, () -> helper.consume(Helper.onlyUnchecked()));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f(Helper helper) {
                int i = Helper.onlyUnchecked();
                assertThrows(RuntimeException.class, () -> helper.consume(i));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void checkedException_argThrowsChecked_hoist() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.io.IOException;

            class Test {
              void f(Helper helper) throws Exception {
                assertThrows(IOException.class, () -> helper.consume(Helper.checked()));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.io.IOException;

            class Test {
              void f(Helper helper) throws Exception {
                int i = Helper.checked();
                assertThrows(IOException.class, () -> helper.consume(i));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void checkedException_argThrowsDifferentChecked_noHoist() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.io.IOException;

            class Test {
              void f(Helper helper) throws Exception {
                assertThrows(IOException.class, () -> helper.consume(Helper.otherChecked()));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void checkedException_argThrowsMultipleChecked_hoist() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.io.IOException;

            class Test {
              void f(Helper helper) throws Exception {
                assertThrows(IOException.class, () -> helper.consume(Helper.bothChecked()));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.io.IOException;

            class Test {
              void f(Helper helper) throws Exception {
                int i = Helper.bothChecked();
                assertThrows(IOException.class, () -> helper.consume(i));
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void throwsException() {
    compilationHelper
        .addInputLines(
            "Hoistable.java",
            """
            public class Hoistable {
              public static Hoistable create(Object t) throws Exception {
                return new Hoistable();
              }

              public static Object getThing() throws Exception {
                throw new Exception();
              }

              public static Object getOtherThing() throws Throwable {
                throw new Throwable();
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() {
                assertThrows(Exception.class, () -> Hoistable.create(Hoistable.getThing()));
              }

              void g() {
                assertThrows(Throwable.class, () -> Hoistable.create(Hoistable.getOtherThing()));
              }

              void h() throws Throwable {
                assertThrows(Throwable.class, () -> Hoistable.create(Hoistable.getOtherThing()));
              }

              void i() throws IllegalStateException {
                assertThrows(Throwable.class, () -> Hoistable.create(Hoistable.getOtherThing()));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;

            class Test {
              void f() throws Exception {
                Object t = Hoistable.getThing();
                assertThrows(Exception.class, () -> Hoistable.create(t));
              }

              void g() throws Throwable {
                Object t = Hoistable.getOtherThing();
                assertThrows(Throwable.class, () -> Hoistable.create(t));
              }

              void h() throws Throwable {
                Object t = Hoistable.getOtherThing();
                assertThrows(Throwable.class, () -> Hoistable.create(t));
              }

              void i() throws IllegalStateException, Throwable {
                Object t = Hoistable.getOtherThing();
                assertThrows(Throwable.class, () -> Hoistable.create(t));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void stringWrapper() {
    compilationHelper
        .addInputLines(
            "AbstractType.java",
            """
            package com.google.android.gms.tagmanager.internal.type;

            public abstract class AbstractType {}
            """)
        .expectUnchanged()
        .addInputLines(
            "StringWrapper.java",
            """
            package com.google.android.gms.tagmanager.internal.type;

            public class StringWrapper extends AbstractType {
              public StringWrapper(String value) {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import com.google.android.gms.tagmanager.internal.type.StringWrapper;

            abstract class Test {
              void f() {
                assertThrows(IllegalStateException.class, () -> doSomething(new StringWrapper("hello")));
                assertThrows(IllegalStateException.class, () -> doSomething(new StringWrapper(getString())));
              }

              abstract void doSomething(StringWrapper stringWrapper);

              abstract String getString();
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import com.google.android.gms.tagmanager.internal.type.StringWrapper;

            abstract class Test {
              void f() {
                assertThrows(IllegalStateException.class, () -> doSomething(new StringWrapper("hello")));
                StringWrapper stringWrapper = new StringWrapper(getString());
                assertThrows(IllegalStateException.class, () -> doSomething(stringWrapper));
              }

              abstract void doSomething(StringWrapper stringWrapper);

              abstract String getString();
            }
            """)
        .doTest();
  }

  @Test
  public void constValue() {
    compilationHelper
        .addInputLines(
            "Test.java",
            """
            import static org.junit.Assert.assertThrows;
            import java.util.List;
            import java.util.ArrayList;

            class Test {

              private static final String CONST = "hello";

              void f() {
                List<String> list = new ArrayList<>();
                assertThrows(IllegalStateException.class, () -> list.add(CONST));
                assertThrows(IllegalStateException.class, () -> list.add(CONST + "world"));
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
