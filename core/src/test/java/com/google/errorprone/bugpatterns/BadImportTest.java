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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link BadImport}. */
@RunWith(JUnit4.class)
public final class BadImportTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(BadImport.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(BadImport.class, getClass());

  @Test
  public void positive_static_simpleCase() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.collect.ImmutableList.of;
            import com.google.common.collect.ImmutableList;

            class Test {
              // BUG: Diagnostic contains: ImmutableList.of()
              ImmutableList<?> list = of();
            }
            """)
        .doTest();
  }

  @Test
  public void positive_identifiers() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.errorprone.CompilationTestHelper.newInstance;
            import com.google.errorprone.CompilationTestHelper;
            import com.google.errorprone.bugpatterns.BugChecker;

            class Test {
              private final CompilationTestHelper compilationTestHelper =
                  // BUG: Diagnostic contains: CompilationTestHelper.newInstance
                  newInstance(BugChecker.class, getClass());
            }
            """)
        .doTest();
  }

  @Test
  public void msg() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.collect.ImmutableList.of;
            import com.google.common.collect.ImmutableList;

            class Test {
              // BUG: Diagnostic contains: qualified class: ImmutableList
              ImmutableList<?> list = of();
            }
            """)
        .doTest();
  }

  @Test
  public void positive_static_differentOverloadsInvoked() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
"""
import static com.google.common.collect.ImmutableList.of;
import com.google.common.collect.ImmutableList;

class Test {
  // BUG: Diagnostic contains: ImmutableList.of(ImmutableList.of(1, 2, 3), ImmutableList.of())
  ImmutableList<?> list = of(of(1, 2, 3), of());
}
""")
        .doTest();
  }

  @Test
  public void positive_truth8AssertThatTrueFlag() {
    compilationTestHelper
        .setArgs("-XepOpt:BadImport:Truth8=true")
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth8.assertThat;
            import java.util.stream.IntStream;

            class Test {
              void x(IntStream s) {
                // BUG: Diagnostic contains: usually recommend
                assertThat(s).isEmpty();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_static_locallyDefinedMethod() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            """
            import static com.google.common.collect.ImmutableList.of;
            import com.google.common.collect.ImmutableList;

            class Test {
              class Blah {
                Blah() {
                  of(); // Left unchanged, because this is invoking Test.Blah.of.
                }

                void of() {}
              }

              ImmutableList<?> list = of();
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import static com.google.common.collect.ImmutableList.of;
            import com.google.common.collect.ImmutableList;

            class Test {
              class Blah {
                Blah() {
                  of(); // Left unchanged, because this is invoking Test.Blah.of.
                }

                void of() {}
              }

              ImmutableList<?> list = ImmutableList.of();
            }
            """)
        .doTest();
  }

  @Test
  public void negative_static_noStaticImport() {
    compilationTestHelper
        .addSourceLines(
            "in/Test.java",
            """
            class Test {
              void of() {}

              void foo() {
                of();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive_nested() {
    compilationTestHelper
        .addSourceLines(
            "BadImportPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link BadImport}.
 *
 * @author awturner@google.com (Andy Turner)
 */
class BadImportPositiveCases {
  public void variableDeclarations() {
    // Only the first match is reported; but all occurrences are fixed.
    // BUG: Diagnostic contains: ImmutableList.Builder
    Builder<String> qualified;
    Builder raw;
  }

  public void variableDeclarationsNestedGenerics() {
    Builder<Builder<String>> builder1;
    Builder<Builder> builder1Raw;
    ImmutableList.Builder<Builder<String>> builder2;
    ImmutableList.Builder<Builder> builder2Raw;
  }

  @Nullable Builder<@Nullable Builder<@Nullable String>>
      parameterizedWithTypeUseAnnotationMethod() {
    return null;
  }

  public void variableDeclarationsNestedGenericsAndTypeUseAnnotations() {

    @Nullable Builder<@Nullable String> parameterizedWithTypeUseAnnotation1;

    @Nullable Builder<@Nullable Builder<@Nullable String>> parameterizedWithTypeUseAnnotation2;
  }

  public void newClass() {
    new Builder<String>();
    new Builder<Builder<String>>();
  }

  Builder<String> returnGenericExplicit() {
    return new Builder<String>();
  }

  Builder<String> returnGenericDiamond() {
    return new Builder<>();
  }

  Builder returnRaw() {
    return new Builder();
  }

  void classLiteral() {
    System.out.println(Builder.class);
  }
}
""")
        .doTest();
  }

  @Test
  public void positive_nested_parentNotAlreadyImported() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList.Builder;

            class Test {
              // BUG: Diagnostic contains: ImmutableList.Builder<String> builder = null;
              Builder<String> builder = null;
            }
            """)
        .doTest();
  }

  @Test
  public void positive_nested_conflictingName() {
    compilationTestHelper
        .addSourceLines(
            "thing/A.java",
            """
            package thing;

            public class A {
              public static class B {
                public static class Builder {}
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            import thing.A.B.Builder;

            class Test {
              // BUG: Diagnostic contains: A.B.Builder builder;
              Builder builder;

              static class B {}
            }
            """)
        .doTest();
  }

  @Test
  public void positive_nested_conflictingNames_fullyQualified() {
    compilationTestHelper
        .addSourceLines(
            "thing/A.java",
            """
            package thing;

            public class A {
              public static class B {
                public static class Builder {}
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            import thing.A.B.Builder;

            class Test {
              // BUG: Diagnostic contains: thing.A.B.Builder builder
              Builder builder;

              static class A {}

              static class B {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative_nested() {
    compilationTestHelper
        .addSourceLines(
            "BadImportNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.common.collect.ImmutableList;

            /**
             * Tests for {@link BadImport}.
             *
             * @author awturner@google.com (Andy Turner)
             */
            public class BadImportNegativeCases {
              public void qualified() {
                ImmutableList.Builder<String> qualified;
                com.google.common.collect.ImmutableList.Builder<String> fullyQualified;
                ImmutableList.Builder raw;

                new ImmutableList.Builder<String>();
              }

              static class Nested {
                static class Builder {}

                void useNestedBuilder() {
                  new Builder();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_badImportIsTopLevelClass() {
    compilationTestHelper
        .addSourceLines(
            "thing/thang/Builder.java", // Avoid rewrapping onto single line.
            "package thing.thang;",
            "public class Builder {}")
        .addSourceLines(
            "Test.java", // Avoid rewrapping onto single line.
            "import thing.thang.Builder;",
            "class Test {",
            "  Builder builder;",
            "}")
        .doTest();
  }

  @Test
  public void nestedFixes() {
    refactoringTestHelper
        .addInputLines(
            "BadImportPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link BadImport}.
 *
 * @author awturner@google.com (Andy Turner)
 */
class BadImportPositiveCases {
  public void variableDeclarations() {
    // Only the first match is reported; but all occurrences are fixed.
    // BUG: Diagnostic contains: ImmutableList.Builder
    Builder<String> qualified;
    Builder raw;
  }

  public void variableDeclarationsNestedGenerics() {
    Builder<Builder<String>> builder1;
    Builder<Builder> builder1Raw;
    ImmutableList.Builder<Builder<String>> builder2;
    ImmutableList.Builder<Builder> builder2Raw;
  }

  @Nullable Builder<@Nullable Builder<@Nullable String>>
      parameterizedWithTypeUseAnnotationMethod() {
    return null;
  }

  public void variableDeclarationsNestedGenericsAndTypeUseAnnotations() {

    @Nullable Builder<@Nullable String> parameterizedWithTypeUseAnnotation1;

    @Nullable Builder<@Nullable Builder<@Nullable String>> parameterizedWithTypeUseAnnotation2;
  }

  public void newClass() {
    new Builder<String>();
    new Builder<Builder<String>>();
  }

  Builder<String> returnGenericExplicit() {
    return new Builder<String>();
  }

  Builder<String> returnGenericDiamond() {
    return new Builder<>();
  }

  Builder returnRaw() {
    return new Builder();
  }

  void classLiteral() {
    System.out.println(Builder.class);
  }
}
""")
        .addOutputLines(
            "BadImportPositiveCases_expected.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link BadImport}.
 *
 * @author awturner@google.com (Andy Turner)
 */
class BadImportPositiveCases {
  public void variableDeclarations() {
    ImmutableList.Builder<String> qualified;
    ImmutableList.Builder raw;
  }

  public void variableDeclarationsNestedGenerics() {
    ImmutableList.Builder<ImmutableList.Builder<String>> builder1;
    ImmutableList.Builder<ImmutableList.Builder> builder1Raw;
    ImmutableList.Builder<ImmutableList.Builder<String>> builder2;
    ImmutableList.Builder<ImmutableList.Builder> builder2Raw;
  }

  ImmutableList.@Nullable Builder<ImmutableList.@Nullable Builder<@Nullable String>>
      parameterizedWithTypeUseAnnotationMethod() {
    return null;
  }

  public void variableDeclarationsNestedGenericsAndTypeUseAnnotations() {

    ImmutableList.@Nullable Builder<@Nullable String> parameterizedWithTypeUseAnnotation1;

    ImmutableList.@Nullable Builder<ImmutableList.@Nullable Builder<@Nullable String>>
        parameterizedWithTypeUseAnnotation2;
  }

  public void newClass() {
    new ImmutableList.Builder<String>();
    new ImmutableList.Builder<ImmutableList.Builder<String>>();
  }

  ImmutableList.Builder<String> returnGenericExplicit() {
    return new ImmutableList.Builder<String>();
  }

  ImmutableList.Builder<String> returnGenericDiamond() {
    return new ImmutableList.Builder<>();
  }

  ImmutableList.Builder returnRaw() {
    return new ImmutableList.Builder();
  }

  void classLiteral() {
    System.out.println(ImmutableList.Builder.class);
  }
}
""")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void nestedTypeUseAnnotation() {
    refactoringTestHelper
        .addInputLines(
            "input/TypeUseAnnotation.java",
            """
            package test;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
            @interface TypeUseAnnotation {}
            """)
        .expectUnchanged()
        .addInputLines(
            "input/SomeClass.java",
            """
            package test;

            class SomeClass {
              static class Builder {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "input/Test.java",
            """
            package test;

            import java.util.List;
            import test.SomeClass.Builder;

            abstract class Test {
              @TypeUseAnnotation Builder builder;

              @TypeUseAnnotation
              abstract Builder method1();

              abstract @TypeUseAnnotation Builder method2();

              abstract void method3(@TypeUseAnnotation Builder builder);

              abstract void method4(List<@TypeUseAnnotation Builder> builder);
            }
            """)
        .addOutputLines(
            "output/Test.java",
            """
            package test;

            import java.util.List;

            abstract class Test {
              SomeClass.@TypeUseAnnotation Builder builder;

              abstract SomeClass.@TypeUseAnnotation Builder method1();

              abstract SomeClass.@TypeUseAnnotation Builder method2();

              abstract void method3(SomeClass.@TypeUseAnnotation Builder builder);

              abstract void method4(List<SomeClass.@TypeUseAnnotation Builder> builder);
            }
            """)
        .doTest();
  }

  @Test
  public void negative_truth8AssertThatFalseFlag() {
    compilationTestHelper
        .setArgs("-XepOpt:BadImport:Truth8=false")
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth8.assertThat;
            import java.util.stream.IntStream;

            class Test {
              void x(IntStream s) {
                assertThat(s).isEmpty();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_otherAssertThat() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.truth.Truth.assertThat;

            class Test {
              void x(Iterable<?> i) {
                assertThat(i).isEmpty();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suppressed_class() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.collect.ImmutableList.of;
            import com.google.common.collect.ImmutableList;

            @SuppressWarnings("BadImport")
            class Test {
              ImmutableList<?> list = of();
              ImmutableList<?> list2 = of();
            }
            """)
        .doTest();
  }

  @Test
  public void suppressed_field() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.collect.ImmutableList.of;
            import com.google.common.collect.ImmutableList;

            class Test {
              @SuppressWarnings("BadImport")
              ImmutableList<?> list = of();

              // BUG: Diagnostic contains: ImmutableList.of()
              ImmutableList<?> list2 = of();
            }
            """)
        .doTest();
  }

  @Test
  public void suppressed_method() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.collect.ImmutableList.of;
            import com.google.common.collect.ImmutableList;

            class Test {
              @SuppressWarnings("BadImport")
              void foo() {
                ImmutableList<?> list = of();
              }

              void bar() {
                // BUG: Diagnostic contains: ImmutableList.of()
                ImmutableList<?> list2 = of();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enumWithinSameCompilationUnitImported_noFinding() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            package pkg;

            import pkg.Test.Type;

            class Test {
              enum Type {
                A,
                B;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enumWithinDifferentCompilationUnitImported_finding() {
    refactoringTestHelper
        .addInputLines(
            "E.java",
            """
            package a;

            public enum E {
              INSTANCE;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            package pkg;

            import static a.E.INSTANCE;

            class Test {
              Object e = INSTANCE;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            package pkg;

            import static a.E.INSTANCE;
            import a.E;

            class Test {
              Object e = E.INSTANCE;
            }
            """)
        .doTest();
  }

  @Test
  public void enumValues() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            """
            import static java.util.concurrent.TimeUnit.values;

            import java.util.concurrent.TimeUnit;

            class Test {
              void foo() {
                // BUG: Diagnostic contains: TimeUnit.values()
                TimeUnit[] timeUnits = values();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void doesNotMatchProtos() {
    compilationTestHelper
        .addSourceLines(
            "ProtoOuterClass.java",
            """
            package pkg;

            import com.google.protobuf.MessageLite;

            public class ProtoOuterClass {
              public abstract static class Provider implements MessageLite {}
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            import pkg.ProtoOuterClass.Provider;

            class Test {
              public void test(Provider p) {}
            }
            """)
        .doTest();
  }

  @Test
  public void badEnclosingTypes() {
    refactoringTestHelper
        .setArgs("-XepOpt:BadImport:BadEnclosingTypes=org.immutables.value.Value")
        .addInputLines(
            "org/immutables/value/Value.java",
            """
            package org.immutables.value;

            public @interface Value {
              @interface Immutable {}
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import org.immutables.value.Value.Immutable;

            @Immutable
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            import org.immutables.value.Value;

            @Value.Immutable
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void badEnclosingTypes_doesNotMatchFullyQualifiedName() {
    compilationTestHelper
        .setArgs("-XepOpt:BadImport:BadEnclosingTypes=org.immutables.value.Value")
        .addSourceLines(
            "org/immutables/value/Value.java",
            """
            package org.immutables.value;

            public @interface Value {
              @interface Immutable {}
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            @org.immutables.value.Value.Immutable
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void badEnclosingTypes_staticMethod() {
    compilationTestHelper
        .setArgs("-XepOpt:BadImport:BadEnclosingTypes=com.google.common.collect.ImmutableList")
        .addSourceLines(
            "Test.java",
            """
            import static com.google.common.collect.ImmutableList.toImmutableList;
            import com.google.common.collect.ImmutableList;
            import java.util.stream.Collector;

            class Test {
              // BUG: Diagnostic contains: ImmutableList.toImmutableList()
              Collector<?, ?, ImmutableList<Object>> immutableList = toImmutableList();
            }
            """)
        .doTest();
  }
}
