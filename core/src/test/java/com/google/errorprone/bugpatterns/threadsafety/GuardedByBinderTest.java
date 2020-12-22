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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.FileObjects.forSourceLines;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import com.google.errorprone.FileManagers;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link GuardedByBinder}Test */
@RunWith(JUnit4.class)
public class GuardedByBinderTest {

  @Test
  public void testInherited() {
    assertThat(
            bind(
                "Test",
                "slock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Super {",
                    "  final Object slock = new Object();",
                    "}",
                    "class Test extends Super {",
                    "}")))
        .isEqualTo("(SELECT (THIS) slock)");
  }

  @Test
  public void testFinal() {
    assertThat(
            bind(
                "Test",
                "lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Test {",
                    "  final Object lock = new Object();",
                    "}")))
        .isEqualTo("(SELECT (THIS) lock)");
  }

  @Test
  public void testMethod() {
    assertThat(
            bind(
                "Test",
                "s.f.g().f.g().lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "import javax.annotation.concurrent.GuardedBy;",
                    "class Super {",
                    "  final Super f = null;",
                    "  Super g() { return null; }",
                    "  final Object lock = new Object();",
                    "}",
                    "class Test {",
                    "  final Super s = null;",
                    "}")))
        .isEqualTo(
            "(SELECT (SELECT (SELECT (SELECT (SELECT (SELECT " + "(THIS) s) f) g()) f) g()) lock)");
  }

  @Test
  public void testBadSuperAccess() {
    bindFail(
        "Test",
        "Super.this.lock",
        forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Super {}",
            "class Test extends Super {",
            "  final Object lock = new Object();",
            "}"));
  }

  @Test
  public void namedClass_this() {
    assertThat(
            bind(
                "Test",
                "Test.class",
                forSourceLines(
                    "threadsafety/Test.java", "package threadsafety;", "class Test {", "}")))
        .isEqualTo("(CLASS_LITERAL threadsafety.Test)");
  }

  @Test
  public void namedClass_super() {
    assertThat(
            bind(
                "Test",
                "Super.class",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Super {}",
                    "class Test extends Super {}")))
        .isEqualTo("(CLASS_LITERAL threadsafety.Super)");
  }

  @Test
  public void namedClass_nonLiteral() {
    bindFail(
        "Test",
        "t.class",
        forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Super {}",
            "class Test extends Super {",
            "  Test t;",
            "}"));
  }

  @Test
  public void namedClass_none() {
    bindFail(
        "Test",
        "Super.class",
        forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {}"));
  }

  @Test
  public void namedThis_none() {
    bindFail(
        "Test",
        "Segment.this",
        forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {}"));
  }

  @Test
  public void outer_lock() {
    assertThat(
            bind(
                "Test",
                "Outer.this.lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Outer {",
                    "  final Object lock = new Object();",
                    "  class Test {}",
                    "}")))
        .isEqualTo("(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)");
  }

  @Test
  public void outer_lock_simpleName() {
    assertThat(
            bind(
                "Test",
                "lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "import javax.annotation.concurrent.GuardedBy;",
                    "class Outer {",
                    "  final Object lock = new Object();",
                    "  class Test {}",
                    "}")))
        .isEqualTo("(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)");
  }

  @Test
  public void otherClass() {
    assertThat(
            bind(
                "Test",
                "Other.lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Other {",
                    "  static final Object lock = new Object();",
                    "}",
                    "class Test {",
                    "}")))
        .isEqualTo("(SELECT (TYPE_LITERAL threadsafety.Other) lock)");
  }

  @Test
  public void simpleName() {
    assertThat(
            bind(
                "Test",
                "Other.lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Other {",
                    "  static final Object lock = new Object();",
                    "}",
                    "class Test {",
                    "  final Other Other = null;",
                    "}")))
        .isEqualTo("(SELECT (TYPE_LITERAL threadsafety.Other) lock)");
  }

  @Test
  public void simpleNameClass() {
    assertThat(
            bind(
                "Test",
                "Other.class",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Other {",
                    "  static final Object lock = new Object();",
                    "}",
                    "class Test {",
                    "  Other Other = null;",
                    "}")))
        .isEqualTo("(CLASS_LITERAL threadsafety.Other)");
  }

  @Test
  public void simpleFieldName() {
    assertThat(
            bind(
                "Test",
                "Other",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Other {",
                    "  static final Object lock = new Object();",
                    "}",
                    "class Test {",
                    "  final Object Other = null;",
                    "}")))
        .isEqualTo("(SELECT (THIS) Other)");
  }

  @Test
  public void staticFieldGuard() {
    assertThat(
            bind(
                "Test",
                "lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Test {",
                    "  static final Object lock = new Object();",
                    "}")))
        .isEqualTo("(SELECT (TYPE_LITERAL threadsafety.Test) lock)");
  }

  @Test
  public void staticMethodGuard() {
    assertThat(
            bind(
                "Test",
                "lock()",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Test {",
                    "  static Object lock() { return null; }",
                    "}")))
        .isEqualTo("(SELECT (TYPE_LITERAL threadsafety.Test) lock())");
  }

  @Test
  public void staticOnStatic() {
    assertThat(
            bind(
                "Test",
                "Test.lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Test {",
                    "  static final Object lock = new Object();",
                    "}")))
        .isEqualTo("(SELECT (TYPE_LITERAL threadsafety.Test) lock)");
  }

  @Test
  public void instanceOnStatic() {
    bindFail(
        "Test",
        "Test.lock",
        forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  final Object lock = new Object();",
            "}"));
  }

  @Test
  public void instanceMethodOnStatic() {
    bindFail(
        "Test",
        "Test.lock()",
        forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  final Object lock() { return null; }",
            "}"));
  }

  @Test
  public void explicitThisOuterClass() {
    assertThat(
            bind(
                "Inner",
                "this.lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Outer {",
                    "  Object lock;",
                    "  class Inner {",
                    "    int x;",
                    "  }",
                    "}")))
        .isEqualTo("(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)");
  }

  @Test
  public void implicitThisOuterClass() {
    assertThat(
            bind(
                "Inner",
                "lock",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Outer {",
                    "  Object lock;",
                    "  class Inner {",
                    "    int x;",
                    "  }",
                    "}")))
        .isEqualTo("(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)");
  }

  @Test
  public void implicitThisOuterClassMethod() {
    assertThat(
            bind(
                "Inner",
                "endpoint().lock()",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "class Outer {",
                    "  class Endpoint {",
                    "    Object lock() { return null; }",
                    "  }",
                    "  Endpoint endpoint() { return null; }",
                    "  class Inner {",
                    "    int x;",
                    "  }",
                    "}")))
        .isEqualTo("(SELECT (SELECT (SELECT (THIS) outer$threadsafety.Outer) endpoint()) lock())");
  }

  @Test
  public void explicitThisSameClass() {
    assertThat(
            bind(
                "Test",
                "Test.this",
                forSourceLines(
                    "threadsafety/Test.java", "package threadsafety;", "class Test {", "}")))
        .isEqualTo("(THIS)");
  }

  // regression test for issue 387
  @Test
  public void enclosingBlockScope() {
    assertThat(
            bind(
                "",
                "mu",
                forSourceLines(
                    "threadsafety/Test.java",
                    "package threadsafety;",
                    "import javax.annotation.concurrent.GuardedBy;",
                    "public class Test {",
                    "  public final Object mu = new Object();",
                    "  @GuardedBy(\"mu\") int x = 1;",
                    "  {",
                    "    new Object() {",
                    "      void f() {",
                    "        synchronized (mu) {",
                    "          x++;",
                    "        }",
                    "      }",
                    "    };",
                    "  }",
                    "}")))
        .isEqualTo("(SELECT (SELECT (THIS) outer$threadsafety.Test) mu)");
  }

  // TODO(cushon): disallow non-final lock expressions
  @Ignore
  @Test
  public void nonFinalStatic() {
    bindFail(
        "Test",
        "Other.lock",
        forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Other {",
            "  static Object lock = new Object();",
            "}",
            "class Test {",
            "  final Other Other = null;",
            "}"));
  }

  // TODO(cushon): disallow non-final lock expressions
  @Ignore
  @Test
  public void nonFinal() {
    bindFail(
        "Test",
        "lock",
        forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  Object lock = new Object();",
            "}"));
  }

  private void bindFail(String className, String exprString, JavaFileObject fileObject) {
    try {
      bind(className, exprString, fileObject);
      fail("Expected binding to fail.");
    } catch (IllegalGuardedBy expected) {
    }
  }

  private String bind(String className, String exprString, JavaFileObject fileObject) {
    JavaCompiler javaCompiler = JavacTool.create();
    JavacTaskImpl task =
        (JavacTaskImpl)
            javaCompiler.getTask(
                new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true),
                FileManagers.testFileManager(),
                null,
                Collections.<String>emptyList(),
                null,
                Arrays.asList(fileObject));
    Iterable<? extends CompilationUnitTree> compilationUnits = task.parse();
    task.analyze();
    for (CompilationUnitTree compilationUnit : compilationUnits) {
      FindClass finder = new FindClass();
      finder.visitTopLevel((JCTree.JCCompilationUnit) compilationUnit);
      for (JCTree.JCClassDecl classDecl : finder.decls) {
        if (classDecl.getSimpleName().contentEquals(className)) {
          Optional<GuardedByExpression> guardExpression =
              GuardedByBinder.bindString(
                  exprString,
                  GuardedBySymbolResolver.from(
                      ASTHelpers.getSymbol(classDecl),
                      compilationUnit,
                      task.getContext(),
                      null,
                      VisitorState.createForUtilityPurposes(task.getContext())),
                  GuardedByFlags.allOn());
          if (!guardExpression.isPresent()) {
            throw new IllegalGuardedBy(exprString);
          }
          return guardExpression.get().debugPrint();
        }
      }
    }
    throw new AssertionError("Couldn't find a class with the given name: " + className);
  }

  private static class FindClass extends TreeScanner {

    private final List<JCTree.JCClassDecl> decls = new ArrayList<>();

    @Override
    public void visitClassDef(JCTree.JCClassDecl classDecl) {
      decls.add(classDecl);
      super.visitClassDef(classDecl);
    }
  }
}
