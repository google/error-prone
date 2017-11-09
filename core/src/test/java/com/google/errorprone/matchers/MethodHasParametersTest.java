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
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.variableType;
import static org.junit.Assert.assertEquals;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.MethodTree;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class MethodHasParametersTest extends CompilerBasedAbstractTest {

  final List<ScannerTest> tests = new ArrayList<ScannerTest>();

  @Before
  public void setUp() {
    tests.clear();
    writeFile(
        "SampleAnnotation1.java", "package com.google;", "public @interface SampleAnnotation1 {}");
    writeFile(
        "SampleAnnotation2.java", "package com.google;", "public @interface SampleAnnotation2 {}");
  }

  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  @Test
  public void shouldMatchSingleParameter() {
    writeFile(
        "A.java", "package com.google;", "public class A {", "  public void A(int i) {}", "}");
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ true,
            new MethodHasParameters(AT_LEAST_ONE, variableType(isPrimitiveType()))));
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ true,
            new MethodHasParameters(ALL, variableType(isPrimitiveType()))));
  }

  @Test
  public void shouldNotMatchNoParameters() {
    writeFile("A.java", "package com.google;", "public class A {", "  public void A() {}", "}");
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ false,
            new MethodHasParameters(AT_LEAST_ONE, variableType(isPrimitiveType()))));
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ true,
            new MethodHasParameters(ALL, variableType(isPrimitiveType()))));
  }

  @Test
  public void shouldNotMatchNonmatchingParameter() {
    writeFile(
        "A.java", "package com.google;", "public class A {", "  public void A(Object obj) {}", "}");
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ false,
            new MethodHasParameters(AT_LEAST_ONE, variableType(isPrimitiveType()))));
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ false,
            new MethodHasParameters(ALL, variableType(isPrimitiveType()))));
  }

  @Test
  public void testMultipleParameters() {
    writeFile(
        "A.java",
        "package com.google;",
        "public class A {",
        "  public void A(int i, Object obj) {}",
        "}");
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ true,
            new MethodHasParameters(AT_LEAST_ONE, variableType(isPrimitiveType()))));
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ false,
            new MethodHasParameters(ALL, variableType(isPrimitiveType()))));
  }

  private abstract class ScannerTest extends Scanner {
    public abstract void assertDone();
  }

  private Scanner methodMatches(final boolean shouldMatch, final MethodHasParameters toMatch) {
    ScannerTest test =
        new ScannerTest() {
          private boolean matched = false;

          @Override
          public Void visitMethod(MethodTree node, VisitorState visitorState) {
            visitorState = visitorState.withPath(getCurrentPath());
            if (!isConstructor(node) && toMatch.matches(node, visitorState)) {
              matched = true;
            }
            return super.visitMethod(node, visitorState);
          }

          private boolean isConstructor(MethodTree node) {
            return node.getName().contentEquals("<init>");
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
