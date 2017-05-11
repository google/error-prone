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

import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.ALL;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static org.junit.Assert.assertEquals;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.MethodVisibility.Visibility;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.ClassTree;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 *     <p>TODO(eaftan): Add test for correct matching of nodes.
 */
@RunWith(JUnit4.class)
public class ConstructorOfClassTest extends CompilerBasedAbstractTest {

  final List<ScannerTest> tests = new ArrayList<ScannerTest>();

  @Before
  public void setUp() {
    tests.clear();
  }

  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  @Test
  public void shouldMatchSingleConstructor() {
    writeFile("A.java", "package com.google;", "public class A {", "  private A() {}", "}");
    assertCompiles(
        classMatches(
            true, new ConstructorOfClass(AT_LEAST_ONE, methodHasVisibility(Visibility.PRIVATE))));
    assertCompiles(
        classMatches(true, new ConstructorOfClass(ALL, methodHasVisibility(Visibility.PRIVATE))));
  }

  @Test
  public void shouldNotMatchNoConstructors() {
    writeFile("A.java", "package com.google;", "public class A {", "}");
    assertCompiles(
        classMatches(
            false, new ConstructorOfClass(AT_LEAST_ONE, methodHasVisibility(Visibility.PRIVATE))));
    assertCompiles(
        classMatches(false, new ConstructorOfClass(ALL, methodHasVisibility(Visibility.PRIVATE))));
  }

  @Test
  public void shouldNotMatchNonmatchingConstructor() {
    writeFile("A.java", "package com.google;", "public class A {", "  public A() {}", "}");
    assertCompiles(
        classMatches(
            false, new ConstructorOfClass(AT_LEAST_ONE, methodHasVisibility(Visibility.PRIVATE))));
    assertCompiles(
        classMatches(false, new ConstructorOfClass(ALL, methodHasVisibility(Visibility.PRIVATE))));
  }

  @Test
  public void testMultipleConstructors() {
    writeFile(
        "A.java",
        "package com.google;",
        "public class A {",
        "  private A() {}",
        "  public A(int i) {}",
        "}");
    assertCompiles(
        classMatches(
            true, new ConstructorOfClass(AT_LEAST_ONE, methodHasVisibility(Visibility.PRIVATE))));
    assertCompiles(
        classMatches(false, new ConstructorOfClass(ALL, methodHasVisibility(Visibility.PRIVATE))));
  }

  private abstract class ScannerTest extends Scanner {
    public abstract void assertDone();
  }

  private Scanner classMatches(final boolean shouldMatch, final ConstructorOfClass toMatch) {
    ScannerTest test =
        new ScannerTest() {
          private boolean matched = false;

          @Override
          public Void visitClass(ClassTree node, VisitorState visitorState) {
            visitorState = visitorState.withPath(getCurrentPath());
            if (toMatch.matches(node, visitorState)) {
              matched = true;
            }
            return super.visitClass(node, visitorState);
          }

          @Override
          public void assertDone() {
            assertEquals(matched, shouldMatch);
          }
        };
    tests.add(test);
    return test;
  }
}
