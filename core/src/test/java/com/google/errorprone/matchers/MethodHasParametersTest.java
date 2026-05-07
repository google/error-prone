/*
 * Copyright 2013 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.ALL;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.predicates.TypePredicates.isPrimitive;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;

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

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class MethodHasParametersTest extends CompilerBasedAbstractTest {

  final List<ScannerTest> tests = new ArrayList<>();

  @Before
  public void setUp() {
    tests.clear();
    writeFile(
        "SampleAnnotation1.java",
        """
        package com.google;

        public @interface SampleAnnotation1 {}
        """);
    writeFile(
        "SampleAnnotation2.java",
        """
        package com.google;

        public @interface SampleAnnotation2 {}
        """);
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
        "A.java",
        """
        package com.google;

        public class A {
          public void A(int i) {}
        }
        """);
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ true,
            new MethodHasParameters(AT_LEAST_ONE, variableType(isPrimitive()))));
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ true, new MethodHasParameters(ALL, variableType(isPrimitive()))));
  }

  @Test
  public void shouldNotMatchNoParameters() {
    writeFile(
        "A.java",
        """
        package com.google;

        public class A {
          public void A() {}
        }
        """);
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ false,
            new MethodHasParameters(AT_LEAST_ONE, variableType(isPrimitive()))));
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ true, new MethodHasParameters(ALL, variableType(isPrimitive()))));
  }

  @Test
  public void shouldNotMatchNonmatchingParameter() {
    writeFile(
        "A.java",
        """
        package com.google;

        public class A {
          public void A(Object obj) {}
        }
        """);
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ false,
            new MethodHasParameters(AT_LEAST_ONE, variableType(isPrimitive()))));
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ false, new MethodHasParameters(ALL, variableType(isPrimitive()))));
  }

  @Test
  public void multipleParameters() {
    writeFile(
        "A.java",
        """
        package com.google;

        public class A {
          public void A(int i, Object obj) {}
        }
        """);
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ true,
            new MethodHasParameters(AT_LEAST_ONE, variableType(isPrimitive()))));
    assertCompiles(
        methodMatches(
            /* shouldMatch= */ false, new MethodHasParameters(ALL, variableType(isPrimitive()))));
  }

  private abstract static class ScannerTest extends Scanner {
    abstract void assertDone();
  }

  private Scanner methodMatches(boolean shouldMatch, MethodHasParameters toMatch) {
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
            return getSymbol(node).getKind() == CONSTRUCTOR;
          }

          @Override
          public void assertDone() {
            assertThat(shouldMatch).isEqualTo(matched);
          }
        };
    tests.add(test);
    return test;
  }
}
