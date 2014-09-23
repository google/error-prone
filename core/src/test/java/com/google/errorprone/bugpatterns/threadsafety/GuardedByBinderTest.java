/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.base.Optional;
import com.google.errorprone.ErrorProneInMemoryFileManager;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

/** {@link GuardedByBinder}Test */
@RunWith(JUnit4.class)
public class GuardedByBinderTest {

  private ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

  @Test
  public void testInherited() throws Exception {
    assertEquals(
        "(SELECT (THIS) slock)",
        bind("Test", "slock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Super {",
                "  final Object slock = new Object();",
                "}",
                "class Test extends Super {",
                "}"
                )
            ));
  }

  @Test
  public void testFinal() throws Exception {
    assertEquals(
        "(SELECT (THIS) lock)",
        bind("Test", "lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "  final Object lock = new Object();",
                "}")
            ));
  }

  @Test
  public void testMethod() throws Exception {
    assertEquals(
        "(SELECT (SELECT (SELECT (SELECT (SELECT (SELECT "
            + "(THIS) s) f) g()) f) g()) lock)",
            bind("Test", "s.f.g().f.g().lock",
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
                    "}")
                ));
  }

  @Test
  public void testBadSuperAccess() throws Exception {
    bindFail("Test", "Super.this.lock",
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
  public void namedClass_this() throws Exception {
    assertEquals(
        "(CLASS_LITERAL threadsafety.Test)",
        bind("Test", "Test.class",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "}")
            ));
  }

  @Test
  public void namedClass_super() throws Exception {
    assertEquals(
        "(CLASS_LITERAL threadsafety.Super)",
        bind("Test", "Super.class",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Super {}",
                "class Test extends Super {}")
            ));
  }

  @Test
  public void namedClass_nonLiteral() throws Exception {
    bindFail("Test", "t.class",
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
  public void namedClass_none() throws Exception {
    bindFail("Test", "Super.class",
        fileManager.forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {}"));
  }

  @Test
  public void namedThis_none() throws Exception {
    bindFail("Test", "Segment.this",
        fileManager.forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "import javax.annotation.concurrent.GuardedBy;",
            "class Test {}"));
  }

  @Test
  public void outer_lock() throws Exception {
    assertEquals(
        "(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)",
        bind("Test", "Outer.this.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Outer {",
                "  final Object lock = new Object();",
                "  class Test {}",
                "}")
            ));
  }

  @Test
  public void outer_lock_simpleName() throws Exception {
    assertEquals(
        "(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)",
        bind("Test", "lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "import javax.annotation.concurrent.GuardedBy;",
                "class Outer {",
                "  final Object lock = new Object();",
                "  class Test {}",
                "}")
            ));
  }

  @Test
  public void otherClass() throws Exception {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Other) lock)",
        bind("Test", "Other.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Other {",
                "  static final Object lock = new Object();",
                "}",
                "class Test {",
                "}")
            )
        );
  }

  @Test
  public void simpleName() throws Exception {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Other) lock)",
        bind("Test", "Other.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Other {",
                "  static final Object lock = new Object();",
                "}",
                "class Test {",
                "  final Other Other = null;",
                "}")
            )
        );
  }

  @Test
  public void simpleNameClass() throws Exception {
    assertEquals(
        "(CLASS_LITERAL threadsafety.Other)",
        bind("Test", "Other.class",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Other {",
                "  static final Object lock = new Object();",
                "}",
                "class Test {",
                "  Other Other = null;",
                "}")
            )
        );
  }


  @Test
  public void simpleFieldName() throws Exception {
    assertEquals(
        "(SELECT (THIS) Other)",
        bind("Test", "Other",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Other {",
                "  static final Object lock = new Object();",
                "}",
                "class Test {",
                "  final Object Other = null;",
                "}")
            )
        );
  }

  @Test
  public void staticFieldGuard() throws Exception {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Test) lock)",
        bind("Test", "lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "  static final Object lock = new Object();",
                "}")
            )
        );
  }

  @Test
  public void staticMethodGuard() throws Exception {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Test) lock())",
        bind("Test", "lock()",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "  static Object lock() { return null; }",
                "}")
            )
        );
  }

  @Test
  public void staticOnStatic() throws Exception {
    assertEquals(
        "(SELECT (TYPE_LITERAL threadsafety.Test) lock)",
        bind("Test", "Test.lock",
            fileManager.forSourceLines(
                "threadsafety/Test.java",
                "package threadsafety;",
                "class Test {",
                "  static final Object lock = new Object();",
                "}"
            )));
  }

  @Test
  public void instanceOnStatic() throws Exception {
      bindFail("Test", "Test.lock",
          fileManager.forSourceLines(
              "threadsafety/Test.java",
              "package threadsafety;",
              "class Test {",
              "  final Object lock = new Object();",
              "}"
          ));
  }

  @Test
  public void instanceMethodOnStatic() throws Exception {
      bindFail("Test", "Test.lock()",
          fileManager.forSourceLines(
              "threadsafety/Test.java",
              "package threadsafety;",
              "class Test {",
              "  final Object lock() { return null; }",
              "}"
          ));
  }

  @Test
  public void explicitThisOuterClass() throws Exception {
      assertEquals(
          "(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)",
          bind("Inner", "this.lock",
              fileManager.forSourceLines(
                  "threadsafety/Test.java",
                  "package threadsafety;",
                  "class Outer {",
                  "  Object lock;",
                  "  class Inner {",
                  "    int x;",
                  "  }",
                  "}"
              )));
  }

  @Test
  public void implicitThisOuterClass() throws Exception {
      assertEquals(
          "(SELECT (SELECT (THIS) outer$threadsafety.Outer) lock)",
          bind("Inner", "lock",
              fileManager.forSourceLines(
                  "threadsafety/Test.java",
                  "package threadsafety;",
                  "class Outer {",
                  "  Object lock;",
                  "  class Inner {",
                  "    int x;",
                  "  }",
                  "}"
              )));
  }

  @Test
  public void implicitThisOuterClassMethod() throws Exception {
      assertEquals(
          "(SELECT (SELECT (SELECT (THIS) outer$threadsafety.Outer) endpoint()) lock())",
          bind("Inner", "endpoint().lock()",
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
                  "}"
              )));
  }

  //TODO(user): disallow non-final lock expressions
  @Ignore
  @Test
  public void nonFinalStatic() throws Exception {
    bindFail("Test", "Other.lock",
        fileManager.forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Other {",
            "  static Object lock = new Object();",
            "}",
            "class Test {",
            "  final Other Other = null;",
            "}")
        );
  }

  //TODO(user): disallow non-final lock expressions
  @Ignore
  @Test
  public void nonFinal() throws Exception {
    bindFail("Test", "lock",
        fileManager.forSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  Object lock = new Object();",
            "}")
        );
  }

  private void bindFail(String className, String exprString, JavaFileObject fileObject)
      throws IOException {
    try {
      bind(className, exprString, fileObject);
      fail("Expected binding to fail.");
    } catch (IllegalGuardedBy expected) {
    }
  }

  private String bind(String className, String exprString, JavaFileObject fileObject)
      throws IOException {
    JavaCompiler javaCompiler = JavacTool.create();
    JavacTaskImpl task = (JavacTaskImpl) javaCompiler.getTask(new PrintWriter(System.err, true),
        fileManager, null, Collections.<String>emptyList(), null, Arrays.asList(fileObject));
    Iterable<? extends CompilationUnitTree> compilationUnits = task.parse();
    task.analyze();
    for (CompilationUnitTree compilationUnit : compilationUnits) {
      FindClass finder = new FindClass();
      finder.visitTopLevel((JCTree.JCCompilationUnit) compilationUnit);
      for (JCTree.JCClassDecl classDecl : finder.decls) {
        if (classDecl.getSimpleName().contentEquals(className)) {
          Optional<GuardedByExpression> guardExpression = GuardedByBinder.bindString(
              exprString,
              GuardedBySymbolResolver.from(
                  ASTHelpers.getSymbol(classDecl),
                  compilationUnit,
                  task.getContext(),
                  null));
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

    private List<JCTree.JCClassDecl> decls = new ArrayList<JCTree.JCClassDecl>();

    @Override
    public void visitClassDef(JCTree.JCClassDecl classDecl) {
      decls.add(classDecl);
      super.visitClassDef(classDecl);
    }
  }
}
