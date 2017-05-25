/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CompilerBasedAbstractTest;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Types;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test cases for {@link ASTHelpers#findSuperMethod(MethodSymbol, Types)} and {@link
 * ASTHelpers#findSuperMethod(MethodSymbol, Types)}.
 *
 * @author Łukasz Hanuszczak (hanuszczak@google.com)
 */
@RunWith(JUnit4.class)
public final class ASTHelpersFindSuperMethodsTest extends CompilerBasedAbstractTest {

  private FindSuperMethodsTestScanner scanner;

  @Before
  public void prepareScanClassHierarchy() {
    writeFile("Foo.java", "abstract class Foo {", "  public abstract void foo();", "}");
    writeFile(
        "Bar.java",
        "class Bar extends Foo {",
        "  @Override",
        "  public void foo() {",
        "    System.out.println(\"bar\");",
        "  }",
        "}");
    writeFile(
        "Baz.java",
        "class Baz extends Bar {",
        "  @Override",
        "  public void foo() {",
        "     System.out.println(\"baz\");",
        "  }",
        "}");
    writeFile(
        "Quux.java",
        "class Quux extends Baz {",
        "  public void foo(String string) {",
        "    System.out.println(\"I am not an override! \" + string);",
        "  }",
        "  public int bar(int x, int y) {",
        "    return x * y;",
        "  }",
        "}");
    writeFile(
        "Norf.java",
        "class Norf extends Quux {",
        "  @Override",
        "  public void foo() {",
        "    System.out.println(\"norf\");",
        "  }",
        "  @Override",
        "  public int bar(int x, int y) {",
        "    return super.bar(x, y) + 42;",
        "  }",
        "}");

    scanner = new FindSuperMethodsTestScanner();
    assertCompiles(scanner);
  }

  @Test
  public void findSuperMethods_findsSingleMethod() {
    MethodSymbol barOfNorf = scanner.getMethod("Norf", "bar");
    MethodSymbol barOfQuux = scanner.getMethod("Quux", "bar");
    assertThat(findSuperMethods(barOfNorf)).isEqualTo(ImmutableList.of(barOfQuux));
  }

  @Test
  public void findSuperMethods_findsAllMethodsInTheHierarchy() {
    MethodSymbol fooOfNorf = scanner.getMethod("Norf", "foo");
    MethodSymbol fooOfBaz = scanner.getMethod("Baz", "foo");
    MethodSymbol fooOfBar = scanner.getMethod("Bar", "foo");
    MethodSymbol fooOfQuux = scanner.getMethod("Foo", "foo");
    assertThat(findSuperMethods(fooOfNorf))
        .isEqualTo(ImmutableList.of(fooOfBaz, fooOfBar, fooOfQuux));
  }

  @Test
  public void findSuperMethod_findsNothingForAbstractMethod() {
    MethodSymbol fooOfFoo = scanner.getMethod("Foo", "foo");
    assertThat(findSuperMethod(fooOfFoo)).isEqualTo(Optional.empty());
  }

  @Test
  public void findSuperMethod_findsNothingForNewNonAbstractMethod() {
    MethodSymbol barOfQuux = scanner.getMethod("Quux", "bar");
    assertThat(findSuperMethod(barOfQuux)).isEqualTo(Optional.empty());
  }

  @Test
  public void findSuperMethod_findsAbstractSuperMethod() {
    MethodSymbol fooOfFoo = scanner.getMethod("Foo", "foo");
    MethodSymbol fooOfBar = scanner.getMethod("Bar", "foo");
    assertThat(findSuperMethod(fooOfBar)).isEqualTo(Optional.of(fooOfFoo));
  }

  @Test
  public void findSuperMethod_findsNormalSuperMethodForDirectSuperclass() {
    MethodSymbol fooOfBar = scanner.getMethod("Bar", "foo");
    MethodSymbol fooOfBaz = scanner.getMethod("Baz", "foo");
    assertThat(findSuperMethod(fooOfBaz)).isEqualTo(Optional.of(fooOfBar));

    MethodSymbol barOfQuux = scanner.getMethod("Quux", "bar");
    MethodSymbol barOfNorf = scanner.getMethod("Norf", "bar");
    assertThat(findSuperMethod(barOfNorf)).isEqualTo(Optional.of(barOfQuux));
  }

  @Test
  public void findSuperMethod_findsNormalSuperMethodForNonDirectSuperclass() {
    MethodSymbol fooOfBaz = scanner.getMethod("Baz", "foo");
    MethodSymbol fooOfNorf = scanner.getMethod("Norf", "foo");
    assertThat(findSuperMethod(fooOfNorf)).isEqualTo(Optional.of(fooOfBaz));
  }

  private ImmutableList<MethodSymbol> findSuperMethods(MethodSymbol method) {
    return ASTHelpers.findSuperMethods(method, getTypes()).stream().collect(toImmutableList());
  }

  private Optional<MethodSymbol> findSuperMethod(MethodSymbol method) {
    return ASTHelpers.findSuperMethod(method, getTypes());
  }

  private Types getTypes() {
    return scanner.getState().getTypes();
  }

  /**
   * A quite hacky class used to assert things in {@link ASTHelpersFindSuperMethodsTest}.
   *
   * <p>This does two things: it builds a mapping from class names and method names to method
   * symbols (for easy assertions) and keeps track of the last visited {@link VisitorState}.
   *
   * <p>We cannot do assertions in the {@link Scanner#scan(Tree, VisitorState)} like all the other
   * test cases do because we need data from all processed classes (and {@link Scanner#scan(Tree,
   * VisitorState)} is triggered for every single class). Therefore we need to make all assertions
   * in the test method itself but {@link ASTHelpers#findSuperMethods(MethodSymbol, Types)} requires
   * a {@link VisitorState} to be used. So we simply remember last state passed to the {@link
   * Scanner#scan(Tree, VisitorState)}.
   */
  private static class FindSuperMethodsTestScanner extends Scanner {

    // A `class name -> method name -> method symbol` structure mapping of given files.
    private final Table<String, String, MethodSymbol> methods;

    // Last state passed to the `Scanner#scan` method.
    private VisitorState state;

    public FindSuperMethodsTestScanner() {
      this.methods = HashBasedTable.create();
    }

    public MethodSymbol getMethod(String className, String methodName) {
      return methods.get(className, methodName);
    }

    public VisitorState getState() {
      return state;
    }

    @Override
    public Void scan(Tree tree, VisitorState state) {
      this.state = state;
      return super.scan(tree, state);
    }

    @Override
    public Void visitMethod(MethodTree methodTree, VisitorState state) {
      String classContext = ASTHelpers.getSymbol(methodTree).owner.getSimpleName().toString();
      String methodContext = methodTree.getName().toString();

      MethodSymbol method = ASTHelpers.getSymbol(methodTree);
      if (!methods.contains(classContext, methodContext)) {
        methods.put(classContext, methodContext, method);
      }

      return super.visitMethod(methodTree, state);
    }
  }
}
