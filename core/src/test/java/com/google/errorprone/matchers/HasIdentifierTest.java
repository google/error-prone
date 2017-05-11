/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.Matchers.hasIdentifier;
import static org.junit.Assert.assertEquals;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cpovirk@google.com (Chris Povirk) */
@RunWith(JUnit4.class)
public class HasIdentifierTest extends CompilerBasedAbstractTest {
  final List<ScannerTest> tests = new ArrayList<ScannerTest>();

  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  @Test
  public void shouldMatchThis() {
    writeFile("A.java", "public class A {", "  A() { this(0); }", "  A(int foo) {}", "}");
    assertCompiles(
        methodHasIdentifierMatching(
            true,
            hasIdentifier(
                new Matcher<IdentifierTree>() {
                  @Override
                  public boolean matches(IdentifierTree tree, VisitorState state) {
                    return tree.getName().contentEquals("this");
                  }
                })));
  }

  @Test
  public void shouldMatchLocalVar() {
    writeFile(
        "A.java",
        "public class A {",
        "  A() {",
        "    int foo = 1;",
        "    int bar = foo;",
        "  }",
        "}");
    assertCompiles(
        methodHasIdentifierMatching(
            true,
            hasIdentifier(
                new Matcher<IdentifierTree>() {
                  @Override
                  public boolean matches(IdentifierTree tree, VisitorState state) {
                    return tree.getName().contentEquals("foo");
                  }
                })));
  }

  @Test
  public void shouldMatchParam() {
    writeFile("A.java", "public class A {", "  A(int foo) {", "    int bar = foo;", "  }", "}");
    assertCompiles(
        methodHasIdentifierMatching(
            true,
            hasIdentifier(
                new Matcher<IdentifierTree>() {
                  @Override
                  public boolean matches(IdentifierTree tree, VisitorState state) {
                    return tree.getName().contentEquals("foo");
                  }
                })));
  }

  @Test
  public void shouldNotMatchDeclaration() {
    writeFile("A.java", "public class A {", "  A() {", "    int foo = 1;", "  }", "}");
    assertCompiles(
        methodHasIdentifierMatching(
            false,
            hasIdentifier(
                new Matcher<IdentifierTree>() {
                  @Override
                  public boolean matches(IdentifierTree tree, VisitorState state) {
                    return tree.getName().contentEquals("foo");
                  }
                })));
  }

  /**
   * Tests that the matcher doesn't throw an exception when applied to a tree that doesn't contain
   * any identifiers or classes. Here, we apply the matcher to every LiteralTree.
   */
  @Test
  public void doesNotThrowWhenMatcherIsAppliedDirectlyToLiteral() {
    writeFile("A.java", "public class A {", "  A() {", "    int foo = 1;", "  }", "}");
    assertCompiles(
        literalHasIdentifierMatching(
            false,
            hasIdentifier(
                new Matcher<IdentifierTree>() {
                  @Override
                  public boolean matches(IdentifierTree tree, VisitorState state) {
                    return tree.getName().contentEquals("somethingElse");
                  }
                })));
  }

  private abstract static class ScannerTest extends Scanner {
    abstract void assertDone();
  }

  private Scanner methodHasIdentifierMatching(
      final boolean shouldMatch, final Matcher<Tree> toMatch) {
    ScannerTest test =
        new ScannerTest() {
          private boolean matched = false;

          @Override
          public Void visitMethod(MethodTree node, VisitorState visitorState) {
            visitorState = visitorState.withPath(getCurrentPath());
            if (toMatch.matches(node, visitorState)) {
              matched = true;
            }
            return super.visitMethod(node, visitorState);
          }

          @Override
          public void assertDone() {
            assertEquals(matched, shouldMatch);
          }
        };
    tests.add(test);
    return test;
  }

  private Scanner literalHasIdentifierMatching(
      final boolean shouldMatch, final Matcher<Tree> toMatch) {
    ScannerTest test =
        new ScannerTest() {
          private boolean matched = false;

          @Override
          public Void visitLiteral(LiteralTree node, VisitorState visitorState) {
            visitorState = visitorState.withPath(getCurrentPath());
            if (toMatch.matches(node, visitorState)) {
              matched = true;
            }
            return super.visitLiteral(node, visitorState);
          }

          @Override
          void assertDone() {
            assertEquals(matched, shouldMatch);
          }
        };
    tests.add(test);
    return test;
  }
}
