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

package com.google.errorprone.matchers;

import static org.junit.Assert.assertEquals;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;

public class DescendantOfAbstractTest extends CompilerBasedAbstractTest {

  final List<ScannerTest> tests = new ArrayList<ScannerTest>();

  @Before
  public void setUp() throws Exception {
    writeFile(
        "com/google/A.java",
        "package com.google;",
        "public class A { ",
        "  public int count() {",
        "    return 1;",
        "  }",
        "  public static int staticCount() {",
        "    return 2;",
        "  }",
        "}");
  }

  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  protected abstract class ScannerTest extends Scanner {
    public abstract void assertDone();
  }

  protected Scanner memberSelectMatches(final boolean shouldMatch, final DescendantOf toMatch) {
    ScannerTest test =
        new ScannerTest() {
          private boolean matched = false;

          @Override
          public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
            visitorState = visitorState.withPath(getCurrentPath());
            ExpressionTree methodSelect = node.getMethodSelect();
            if (toMatch.matches(methodSelect, visitorState)) {
              matched = true;
            }
            return super.visitMethodInvocation(node, visitorState);
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
