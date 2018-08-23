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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.errorprone.ErrorProneInMemoryFileManager;
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

  private final ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

  @Test
  public void testInherited() {
    assertEquals(
        "(SELECT (THIS) slock)",
        bind(
            "Test",
            "slock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Super {",
                "  final Object slock = new Object();",
                "}",
                "class Test extends Super {",
                "}")));
  }

  @Test
  public void testFinal() {
    assertEquals(
        "(SELECT (THIS) lock)",
        bind(
            "Test",
            "lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "  final Object lock = new Object();",
                "}")));
  }

  @Test
  public void testMethod() {
    assertEquals(
        "(SELECT (SELECT (SELECT (SELECT (SELECT (SELECT " + "(THIS) s) f) g()) f) g()) lock)",
        bind(
            "Test",
            "s.f.g().f.g().lock",
            fileManager.forSourceLines(
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
                "}")));
  }

  @Test
  public void testBadSuperAccess() {
    bindFail(
        "Test",
        "Super.this.lock",
        fileManager.forSourceLines(
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
    assertEquals(
        "(CLASS_LITERAL threadsafety.Test)",
        bind(
            "Test",
            "Test.class",
            fileManager.forSourceLines(
                "threadsafety/Test.java", "package threadsafety;", "class Test {", "}")));
  }

  @Test
  public void namedClass_super() {
    assertEquals(
        "(CLASS_LITERAL threadsafety.Super)",
        bind(
            "Test",
            "Super.class",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Super {}",
                "class Test extends Super {}")));
  }

  @Test
  public void namedClass_nonLiteral() {
    bindFail(
        "Test",
        "t.class",
        fileManager.forSourceLines(
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
        fileManager.forSourceLines(
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
        fileManager.forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {}"));
  }

  @Test
  public void outer_lock() {
    assertEquals(
        "(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)",
        bind(
            "Test",
            "Outer.this.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Outer {",
                "  final Object lock = new Object();",
                "  class Test {}",
                "}")));
  }

  @Test
  public void outer_lock_simpleName() {
    assertEquals(
        "(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)",
        bind(
            "Test",
            "lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "import javax.annotation.concurrent.GuardedBy;",
                "class Outer {",
                "  final Object lock = new Object();",
                "  class Test {}",
                "}")));
  }

  @Test
  public void otherClass() {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Other) lock)",
        bind(
            "Test",
            "Other.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Other {",
                "  static final Object lock = new Object();",
                "}",
                "class Test {",
                "}")));
  }

  @Test
  public void simpleName() {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Other) lock)",
        bind(
            "Test",
            "Other.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Other {",
                "  static final Object lock = new Object();",
                "}",
                "class Test {",
                "  final Other Other = null;",
                "}")));
  }

  @Test
  public void simpleNameClass() {
    assertEquals(
        "(CLASS_LITERAL threadsafety.Other)",
        bind(
            "Test",
            "Other.class",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Other {",
                "  static final Object lock = new Object();",
                "}",
                "class Test {",
                "  Other Other = null;",
                "}")));
  }

  @Test
  public void simpleFieldName() {
    assertEquals(
        "(SELECT (THIS) Other)",
        bind(
            "Test",
            "Other",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Other {",
                "  static final Object lock = new Object();",
                "}",
                "class Test {",
                "  final Object Other = null;",
                "}")));
  }

  @Test
  public void staticFieldGuard() {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Test) lock)",
        bind(
            "Test",
            "lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "  static final Object lock = new Object();",
                "}")));
  }

  @Test
  public void staticMethodGuard() {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Test) lock())",
        bind(
            "Test",
            "lock()",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "  static Object lock() { return null; }",
                "}")));
  }

  @Test
  public void staticOnStatic() {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Test) lock)",
        bind(
            "Test",
            "Test.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "  static final Object lock = new Object();",
                "}")));
  }

  @Test
  public void instanceOnStatic() {
    bindFail(
        "Test",
        "Test.lock",
        fileManager.forSourceLines(
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
        fileManager.forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  final Object lock() { return null; }",
            "}"));
  }

  @Test
  public void explicitThisOuterClass() {
    assertEquals(
        "(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)",
        bind(
            "Inner",
            "this.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Outer {",
                "  Object lock;",
                "  class Inner {",
                "    int x;",
                "  }",
                "}")));
  }

  @Test
  public void implicitThisOuterClass() {
    assertEquals(
        "(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)",
        bind(
            "Inner",
            "lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Outer {",
                "  Object lock;",
                "  class Inner {",
                "    int x;",
                "  }",
                "}")));
  }

  @Test
  public void implicitThisOuterClassMethod() {
    assertEquals(
        "(SELECT (SELECT (SELECT (THIS) outer$threadsafety.Outer) endpoint()) lock())",
        bind(
            "Inner",
            "endpoint().lock()",
            fileManager.forSourceLines(
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
                "}")));
  }

  @Test
  public void explicitThisSameClass() {
    assertEquals(
        "(THIS)",
        bind(
            "Test",
            "Test.this",
            fileManager.forSourceLines(
                "threadsafety/Test.java", "package threadsafety;", "class Test {", "}")));
  }

  // regression test for issue 387
  @Test
  public void enclosingBlockScope() {
    assertEquals(
        "(SELECT (SELECT (THIS) outer$threadsafety.Test) mu)",
        bind(
            "",
            "mu",
            fileManager.forSourceLines(
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
                "}")));
  }

  // TODO(cushon): disallow non-final lock expressions
  @Ignore
  @Test
  public void nonFinalStatic() {
    bindFail(
        "Test",
        "Other.lock",
        fileManager.forSourceLines(
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
        fileManager.forSourceLines(
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
                fileManager,
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
                      ASTHelpers.getSymbol(classDecl), compilationUnit, task.getContext(), null));
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

    private final List<JCTree.JCClassDecl> decls = new ArrayList<JCTree.JCClassDecl>();

    @Override
    public void visitClassDef(JCTree.JCClassDecl classDecl) {
      decls.add(classDecl);
      super.visitClassDef(classDecl);
    }
  }
}
