/*
 * Copyright 2020 The Error Prone Authors.
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

/** Unit tests for {@link TypeToString}. */
@RunWith(JUnit4.class)
public class TypeToStringTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(TypeToString.class, getClass());

  @Test
  public void noMatch() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            """
            import com.google.errorprone.BugPattern;
            import com.google.errorprone.BugPattern.SeverityLevel;
            import com.google.errorprone.VisitorState;
            import com.google.errorprone.bugpatterns.BugChecker;
            import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
            import com.google.errorprone.matchers.Description;
            import com.sun.source.tree.ClassTree;
            import com.sun.tools.javac.code.Types;

            @BugPattern(name = "Example", summary = "", severity = SeverityLevel.ERROR)
            public class ExampleChecker extends BugChecker implements ClassTreeMatcher {
              @Override
              public Description matchClass(ClassTree t, VisitorState s) {
                return Description.NO_MATCH;
              }
            }
            """)
        .addModules("jdk.compiler/com.sun.tools.javac.code")
        .doTest();
  }

  @Test
  public void matchInABugChecker() {
    testHelper
        .addSourceLines(
            "ExampleChecker.java",
            """
            import com.google.errorprone.BugPattern;
            import com.google.errorprone.BugPattern.SeverityLevel;
            import com.google.errorprone.VisitorState;
            import com.google.errorprone.bugpatterns.BugChecker;
            import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
            import com.google.errorprone.fixes.SuggestedFix;
            import com.google.errorprone.matchers.Description;
            import com.google.errorprone.matchers.Matcher;
            import com.sun.source.tree.ClassTree;
            import com.sun.tools.javac.code.Type;
            import com.sun.tools.javac.code.Types;
            import com.sun.tools.javac.tree.JCTree;
            import com.sun.tools.javac.tree.JCTree.JCClassDecl;
            import com.sun.tools.javac.tree.TreeMaker;

            @BugPattern(name = "Example", summary = "", severity = SeverityLevel.ERROR)
            public class ExampleChecker extends BugChecker implements ClassTreeMatcher {
              @Override
              public Description matchClass(ClassTree tree, VisitorState state) {
                Type type = ((JCTree) tree).type;
                if (type.toString().contains("matcha")) {
                  return describeMatch(tree);
                }
                // BUG: Diagnostic contains: TypeToString
                if (type.toString().equals("match")) {
                  return describeMatch(tree);
                }
                if (new InnerClass().matchaMatcher(type)) {
                  return describeMatch(tree);
                }
                return Description.NO_MATCH;
              }

              class InnerClass {
                boolean matchaMatcher(Type type) {
                  // BUG: Diagnostic contains: TypeToString
                  return type.toString().equals("match");
                }
              }
            }
            """)
        .addModules(
            "jdk.compiler/com.sun.tools.javac.code",
            "jdk.compiler/com.sun.tools.javac.tree",
            "jdk.compiler/com.sun.tools.javac.util")
        .doTest();
  }

  @Test
  public void refactoringWithTypes() {
    BugCheckerRefactoringTestHelper.newInstance(TypeToString.class, getClass())
        .addInputLines(
            "Example.java",
            """
            import javax.lang.model.type.TypeMirror;
            import javax.lang.model.util.Types;

            class Example {
              void foo(TypeMirror type, Types types, TypeMirror other) {
                if (type.toString().equals(other.toString())) {}
              }
            }
            """)
        .addOutputLines(
            "Example.java",
            """
            import javax.lang.model.type.TypeMirror;
            import javax.lang.model.util.Types;

            class Example {
              void foo(TypeMirror type, Types types, TypeMirror other) {
                if (types.isSameType(type, other)) {}
              }
            }
            """)
        .addModules("jdk.compiler/com.sun.tools.javac.code")
        .doTest();
  }

  @Test
  public void refactoringWithStringLiteral() {
    BugCheckerRefactoringTestHelper.newInstance(TypeToString.class, getClass())
        .addInputLines(
            "Example.java",
            """
            import javax.lang.model.type.TypeMirror;
            import javax.lang.model.util.Elements;
            import javax.lang.model.util.Types;

            class Example {
              void foo(TypeMirror type, Types types, Elements elements) {
                if (type.toString().equals("java.lang.String")) {}
              }
            }
            """)
        .addOutputLines(
            "Example.java",
            """
            import java.util.Objects;
            import javax.lang.model.type.TypeMirror;
            import javax.lang.model.util.Elements;
            import javax.lang.model.util.Types;

            class Example {
              void foo(TypeMirror type, Types types, Elements elements) {
                if (Objects.equals(types.asElement(type), elements.getTypeElement("java.lang.String"))) {}
              }
            }
            """)
        .addModules("jdk.compiler/com.sun.tools.javac.code")
        .doTest();
  }

  @Test
  public void refactoringWithPrimitiveStringLiteral() {
    BugCheckerRefactoringTestHelper.newInstance(TypeToString.class, getClass())
        .addInputLines(
            "Example.java",
            """
            import javax.lang.model.type.TypeMirror;
            import javax.lang.model.util.Elements;
            import javax.lang.model.util.Types;

            class Example {
              void foo(TypeMirror type, Types types, Elements elements) {
                if (type.toString().equals("void")) {}
              }
            }
            """)
        .addOutputLines(
            "Example.java",
            """
            import javax.lang.model.type.TypeKind;
            import javax.lang.model.type.TypeMirror;
            import javax.lang.model.util.Elements;
            import javax.lang.model.util.Types;

            class Example {
              void foo(TypeMirror type, Types types, Elements elements) {
                if (type.getKind() == TypeKind.VOID) {}
              }
            }
            """)
        .addModules("jdk.compiler/com.sun.tools.javac.code")
        .doTest();
  }

  @Test
  public void refactoringWithProcessingEnvironment() {
    BugCheckerRefactoringTestHelper.newInstance(TypeToString.class, getClass())
        .addInputLines(
            "Example.java",
            """
            import javax.annotation.processing.ProcessingEnvironment;
            import javax.lang.model.type.TypeMirror;

            class Example {
              void foo(ProcessingEnvironment env, TypeMirror type) {
                if (type.toString().equals("java.lang.String")) {}
              }
            }
            """)
        .addOutputLines(
            "Example.java",
            """
            import java.util.Objects;
            import javax.annotation.processing.ProcessingEnvironment;
            import javax.lang.model.type.TypeMirror;

            class Example {
              void foo(ProcessingEnvironment env, TypeMirror type) {
                if (Objects.equals(
                    env.getTypeUtils().asElement(type),
                    env.getElementUtils().getTypeElement("java.lang.String"))) {}
              }
            }
            """)
        .addModules("jdk.compiler/com.sun.tools.javac.code")
        .doTest();
  }

  @Test
  public void unknownString() {
    BugCheckerRefactoringTestHelper.newInstance(TypeToString.class, getClass())
        .addInputLines(
            "Example.java",
            """
            import javax.annotation.processing.ProcessingEnvironment;
            import javax.lang.model.type.TypeMirror;

            class Example {
              boolean foo(ProcessingEnvironment env, TypeMirror type, String s) {
                return type.toString().equals(s);
              }
            }
            """)
        .expectUnchanged()
        .addModules("jdk.compiler/com.sun.tools.javac.code")
        .doTest();
  }

  @Test
  public void noMatch_autoValue() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.auto.value.AutoValue;
            import javax.lang.model.type.TypeMirror;

            @AutoValue
            abstract class Test {
              abstract TypeMirror type();
            }
            """)
        .addSourceLines(
            "AutoValue_Test.java",
            """
            import javax.annotation.processing.Generated;
            import javax.lang.model.type.TypeMirror;

            @Generated("com.google.auto.value.processor.AutoValueProcessor")
            abstract class AutoValue_Test extends Test {
              private final TypeMirror type;

              AutoValue_Test(TypeMirror type) {
                this.type = type;
              }

              @Override
              public boolean equals(Object o) {
                if (o == this) {
                  return true;
                }
                if (o instanceof Test) {
                  Test that = (Test) o;
                  return this.type.toString().equals(that.type().toString());
                }
                return false;
              }
            }
            """)
        .doTest();
  }
}
