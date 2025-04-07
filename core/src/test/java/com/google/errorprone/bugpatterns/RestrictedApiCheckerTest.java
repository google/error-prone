/*
 * Copyright 2016 The Error Prone Authors.
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
import com.sun.tools.javac.main.Main.Result;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit test for {@link RestrictedApiChecker} */
@RunWith(JUnit4.class)
public class RestrictedApiCheckerTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(RestrictedApiChecker.class, RestrictedApiCheckerTest.class)
          .addSourceLines(
              "Allowlist.java",
              """
              package com.google.errorprone.bugpatterns.testdata;

              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;

              @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
              public @interface Allowlist {}\
              """)
          .addSourceLines(
              "RestrictedApiMethods.java",
              """
              package com.google.errorprone.bugpatterns.testdata;

              import com.google.errorprone.annotations.RestrictedApi;
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Target;

              /** Example for {@link com.google.errorprone.bugpatterns.RestrictedApiCheckerTest}. */
              public class RestrictedApiMethods implements IFaceWithRestriction {

                public int normalMethod() {
                  return 0;
                }

                @RestrictedApi(
                    explanation = "lorem",
                    allowlistAnnotations = {Allowlist.class},
                    allowlistWithWarningAnnotations = {AllowlistWithWarning.class},
                    link = "")
                public RestrictedApiMethods() {}

                @RestrictedApi(
                    explanation = "lorem",
                    allowlistAnnotations = {Allowlist.class},
                    allowlistWithWarningAnnotations = {AllowlistWithWarning.class},
                    link = "")
                public RestrictedApiMethods(int restricted) {}

                @RestrictedApi(
                    explanation = "lorem",
                    allowlistAnnotations = {Allowlist.class},
                    allowlistWithWarningAnnotations = {AllowlistWithWarning.class},
                    link = "",
                    allowedOnPath = ".*testsuite/.*")
                public int restrictedMethod() {
                  return 1;
                }

                @RestrictedApi(
                    explanation = "lorem",
                    allowlistAnnotations = {Allowlist.class},
                    allowlistWithWarningAnnotations = {AllowlistWithWarning.class},
                    link = "")
                public static int restrictedStaticMethod() {
                  return 2;
                }

                @Override
                public void dontCallMe() {}

                public static class Subclass extends RestrictedApiMethods {
                  @Allowlist
                  public Subclass(int restricted) {
                    super(restricted);
                  }

                  @Override
                  public int restrictedMethod() {
                    return 42;
                  }
                }

                public static void accept(Runnable r) {}
              }

              interface IFaceWithRestriction {
                @RestrictedApi(explanation = "ipsum", link = "nothing")
                void dontCallMe();
              }

              @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
              @interface AllowlistWithWarning {}
              """)
          .matchAllDiagnostics();
  private final BugCheckerRefactoringTestHelper refactoringTest =
      BugCheckerRefactoringTestHelper.newInstance(
          RestrictedApiChecker.class, RestrictedApiCheckerTest.class);

  @Test
  public void normalCallAllowed() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              void foo(RestrictedApiMethods m) {
                m.normalMethod();
                m.accept(m::normalMethod);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void restrictedCallProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              void foo(RestrictedApiMethods m) {
                // BUG: Diagnostic contains: lorem
                m.restrictedMethod();
                // BUG: Diagnostic contains: lorem
                m.accept(m::restrictedMethod);
              }
            }
            """)
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void restrictedCallProhibited_inherited() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              void foo(RestrictedApiMethods.Subclass m) {
                // BUG: Diagnostic contains: lorem
                m.restrictedMethod();
                // BUG: Diagnostic contains: ipsum
                m.dontCallMe();
                // BUG: Diagnostic contains: lorem
                m.accept(m::restrictedMethod);
                // BUG: Diagnostic contains: ipsum
                m.accept(m::dontCallMe);
              }
            }
            """)
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void restrictedCallAllowedOnAllowlistedPath() {
    helper
        .addSourceLines(
            "testsuite/Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              void foo(RestrictedApiMethods m) {
                m.restrictedMethod();
                m.accept(m::restrictedMethod);
              }
            }
            """)
        .expectResult(Result.OK)
        .doTest();
  }

  @Test
  public void restrictedStaticCallProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              void foo() {
                // BUG: Diagnostic contains: lorem
                RestrictedApiMethods.restrictedStaticMethod();
                // BUG: Diagnostic contains: lorem
                RestrictedApiMethods.accept(RestrictedApiMethods::restrictedStaticMethod);
              }
            }
            """)
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void restrictedConstructorProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              void foo() {
                // BUG: Diagnostic contains: lorem
                new RestrictedApiMethods(0);
                // BUG: Diagnostic contains: lorem
                RestrictedApiMethods.accept(RestrictedApiMethods::new);
              }
            }
            """)
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void restrictedConstructorViaAnonymousClassProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              void foo() {
                // BUG: Diagnostic contains: lorem
                new RestrictedApiMethods() {};
              }
            }
            """)
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void restrictedConstructorViaAnonymousClassAllowed() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              @Allowlist
              void foo() {
                new RestrictedApiMethods() {};
              }
            }
            """)
        .expectResult(Result.OK)
        .doTest();
  }

  @Test
  public void restrictedCallAnonymousClassFromInterface() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              void foo() {
                new IFaceWithRestriction() {
                  @Override
                  public void dontCallMe() {}
                }
                // BUG: Diagnostic contains: ipsum
                .dontCallMe();
              }
            }
            """)
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void implicitRestrictedConstructorProhibited() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase extends RestrictedApiMethods {
              // BUG: Diagnostic contains: lorem
              public Testcase() {}
            }
            """)
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Ignore("Doesn't work yet")
  @Test
  public void implicitRestrictedConstructorProhibited_implicitConstructor() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            // BUG: Diagnostic contains: lorem
            class Testcase extends RestrictedApiMethods {}
            """)
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Test
  public void allowWithWarning() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              @AllowlistWithWarning
              void foo(RestrictedApiMethods m) {
                // BUG: Diagnostic contains: lorem
                m.restrictedMethod();
                // BUG: Diagnostic contains: lorem
                m.accept(m::restrictedMethod);
              }
            }
            """)
        .expectResult(Result.OK)
        .doTest();
  }

  @Test
  public void allowWithoutWarning() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            class Testcase {
              @Allowlist
              void foo(RestrictedApiMethods m) {
                m.restrictedMethod();
                m.accept(m::restrictedMethod);
              }
            }
            """)
        .expectResult(Result.OK)
        .doTest();
  }

  // Regression test for b/36160747
  @Test
  public void allowAllDefinitionsInFile() {
    helper
        .addSourceLines(
            "Testcase.java",
            """
            package separate.test;

            import com.google.errorprone.annotations.RestrictedApi;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            class Testcase {
              @Allowlist
              void caller() {
                restrictedMethod();
              }

              @RestrictedApi(
                  explanation = "test",
                  allowlistAnnotations = {Allowlist.class},
                  link = "foo")
              void restrictedMethod() {}

              @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
              @interface Allowlist {}
            }
            """)
        .doTest();
  }

  // https://github.com/google/error-prone/issues/2099
  @Test
  public void i2099() {
    helper
        .addSourceLines(
            "T.java",
            """
            package t;

            class T {
              static class Foo {
                class Loo {}
              }

              public void testFoo(Foo foo) {
                foo.new Loo() {};
              }
            }
            """)
        .expectResult(Result.OK)
        .doTest();
  }

  @Ignore("https://github.com/google/error-prone/issues/2152")
  @Test
  public void i2152() {
    helper
        .addSourceLines(
            "T.java",
            """
            class T extends S {
              void f() {
                this.new I("") {};
              }
            }

            abstract class S {
              public class I {
                public I(String name) {}
              }
            }
            """)
        .expectResult(Result.OK)
        .doTest();
  }

  @Test
  public void enumConstructor() {
    helper
        .addSourceLines(
            "T.java",
            """
            enum E {
              ONE(1, 2) {};

              E(int x, int y) {}
            }
            """)
        .expectResult(Result.OK)
        .doTest();
  }

  @Test
  public void restrictedApiOnRecordComponent() {
    helper
        .addSourceLines(
            "Allowlist.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
            @interface Allowlist {}
            """)
        .addSourceLines(
            "User.java",
            """
            import com.google.errorprone.annotations.RestrictedApi;

            public record User(
                String name,
                @RestrictedApi(
                        explanation = "test",
                        allowlistAnnotations = {Allowlist.class},
                        link = "foo")
                    String password) {}
            """)
        .addSourceLines(
            "Testcase.java",
            """
            class Testcase {
              void ctorAllowed() {
                new User("kak", "Hunter2");
              }

              @Allowlist
              void accessorAllowed(User user) {
                user.password();
              }

              void accessorRestricted(User user) {
                // BUG: Diagnostic contains: RestrictedApi
                user.password();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void restrictedApiOnRecordConstructor() {
    helper
        .addSourceLines(
            "Allowlist.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
            @interface Allowlist {}
            """)
        .addSourceLines(
            "User.java",
            """
            import com.google.errorprone.annotations.RestrictedApi;

            public record User(String name, String password) {

              @RestrictedApi(
                  explanation = "test",
                  allowlistAnnotations = {Allowlist.class},
                  link = "foo")
              public User {}
            }
            """)
        .addSourceLines(
            "Testcase.java",
            """
            class Testcase {
              void ctorRestricted() {
                // BUG: Diagnostic contains: RestrictedApi
                new User("kak", "Hunter2");
              }

              @Allowlist
              void ctorAllowed(User user) {
                new User("kak", "Hunter2");
              }

              void accessorAllowed(User user) {
                user.password();
              }
            }
            """)
        .doTest();
  }

  // NOTE: @RestrictedApi cannot be applied to an entire record declaration
}
