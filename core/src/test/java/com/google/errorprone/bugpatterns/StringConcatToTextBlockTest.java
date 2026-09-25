/*
 * Copyright 2024 The Error Prone Authors.
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
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.TreeMaker;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StringConcatToTextBlockTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(StringConcatToTextBlock.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(StringConcatToTextBlock.class, getClass());

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String s = new Object() + "world\\n";
              String oneToken = "hello\\nworld";
              String comment =
                  "hello\\\\n"
                      // comment
                      + "world\\\\n";
              String noNewline = "hello" + "world\\n";
              String extra = "prefix" + s + "hello\\n" + "world\\n";
              String noTrailing = "hello\\n" + "world";
            }
            """)
        .doTest();
  }

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              String s = "hello\\n" + "world\\n";
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String s =
                  \"""
                  hello
                  world
                  \""";
            }
            """)
        .doTest();
  }

  @Test
  public void alreadyTextBlock() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.testing.compile.JavaFileObjects;
            import javax.tools.JavaFileObject;

            class Test {
              JavaFileObject javaFileObject =
                  JavaFileObjects.forSourceLines(
                      "foo.bar.NotAbstract",
                      \"""
                      package foo.bar;

                      import com.google.auto.value.AutoValue;

                      @AutoValue
                      public class NotAbstract {}
                      \""");
            }
            """)
        .doTest();
  }

  @Test
  public void noTrailing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              String s = "hello\\n" + "foo\\n" + "world";
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String s =
                  \"""
                  hello
                  foo
                  world\
                  \""";
            }
            """)
        .doTest();
  }

  @Test
  public void escape() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              String s = "\\n" + "\\nhello\\n" + "foo\\n\\n" + "world";
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String s =
                  \"""


                  hello
                  foo

                  world\
                  \""";
            }
            """)
        .doTest();
  }

  @Test
  public void guavaJoiner() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.base.Joiner;

            class Test {
              String s = Joiner.on('\\n').join("string", "literals");
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.base.Joiner;

            class Test {
              String s =
                  \"""
                  string
                  literals\
                  \""";
            }
            """)
        .doTest();
  }

  @Test
  public void stringJoiner() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            class Test {
              String s = new StringJoiner("\\n").add("string").add("literals").toString();
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            class Test {
              String s =
                  \"""
                  string
                  literals\
                  \""";
            }
            """)
        .doTest();
  }

  @Test
  public void stringJoin() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            class Test {
              String s = String.join("\\n", "string", "literals");
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            class Test {
              String s =
                  \"""
                  string
                  literals\
                  \""";
            }
            """)
        .doTest();
  }

  // b/396965922
  @Test
  public void trailingSpacesInMultilineString() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private static final String FOO =
                  "Lorem ipsum dolor sit amet, consectetur adipiscing elit: \\n"
                      + "- Lorem ipsum dolor sit amet, consectetur adipiscing elit?   \\n"
                      + "- Lorem ipsum dolor sit amet, consectetur adipiscing elit?\\n"
                      + "- Lorem ipsum dolor sit amet, consectetur adipiscing elit?\\n";
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private static final String FOO =
                  \"""
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit:\\s
                  - Lorem ipsum dolor sit amet, consectetur adipiscing elit?  \\s
                  - Lorem ipsum dolor sit amet, consectetur adipiscing elit?
                  - Lorem ipsum dolor sit amet, consectetur adipiscing elit?
                  \""";
            }
            """)
        .doTest();
  }

  @Test
  public void annotationString() {
    refactoringHelper
        .addInputLines(
            "Anno.java",
            """
            import java.lang.annotation.Documented;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
            @interface Anno {}
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            record Test(@SuppressWarnings("foo") @Anno int foo) {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void noDebug() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              String s = "hello\\n" + "world\\n";
            }
            """)
        .expectUnchanged()
        .setArgs("-g:none")
        .doTest();
  }

  // https://github.com/google/error-prone/issues/6103
  @Test
  public void recordOutputStyleLiteral() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void periodic(double encodersResetDelta) {
                recordOutput("Intake/EncoderResetDelta", encodersResetDelta);
              }

              static void recordOutput(String key, double value) {}
            }
            """)
        .doTest();
  }

  /**
   * Annotation processors (and similar AST rewriters) sometimes attach a generated string literal
   * to source positions that do not contain a string. {@code matchLiteral} must not assume that
   * re-lexing that range yields {@code STRINGLITERAL} tokens.
   *
   * <p>https://github.com/google/error-prone/issues/6103
   */
  @Test
  public void syntheticLiteralWithBorrowedSourcePositions() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @com.google.errorprone.bugpatterns.StringConcatToTextBlockTest.SyntheticStringLiteral
              void foo() {}
            }
            """)
        .setArgs("-processor", SyntheticStringLiteralProcessor.class.getName())
        .doTest();
  }

  /** Marks a method so {@link SyntheticStringLiteralProcessor} can inject a synthetic literal. */
  @Retention(RetentionPolicy.SOURCE)
  public @interface SyntheticStringLiteral {}

  /**
   * Injects {@code "generated";} into annotated methods, giving the literal the method's source
   * range (which is not a string literal).
   */
  @SupportedAnnotationTypes(
      "com.google.errorprone.bugpatterns.StringConcatToTextBlockTest.SyntheticStringLiteral")
  public static final class SyntheticStringLiteralProcessor extends AbstractProcessor {
    @Override
    public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      if (roundEnv.processingOver()) {
        return false;
      }
      Trees trees = Trees.instance(processingEnv);
      TreeMaker maker =
          TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());
      for (Element element : roundEnv.getElementsAnnotatedWith(SyntheticStringLiteral.class)) {
        JCMethodDecl method = (JCMethodDecl) trees.getTree(element);
        JCCompilationUnit unit = (JCCompilationUnit) trees.getPath(element).getCompilationUnit();
        int start = method.pos;
        int end = start + 1;
        JCLiteral literal = maker.at(start).Literal("generated");
        unit.endPositions.storeEnd(literal, end);
        JCExpressionStatement statement = maker.at(start).Exec(literal);
        unit.endPositions.storeEnd(statement, end);
        method.body.stats = method.body.stats.prepend(statement);
      }
      return false;
    }
  }
}
