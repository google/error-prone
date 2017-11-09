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

import static com.google.errorprone.matchers.Matchers.methodReturns;
import static org.junit.Assert.assertEquals;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.MethodTree;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cpovirk@google.com (Chris Povirk) */
@RunWith(JUnit4.class)
public class MethodReturnsTest extends CompilerBasedAbstractTest {
  final List<ScannerTest> tests = new ArrayList<ScannerTest>();

  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  @Test
  public void returnsString() {
    writeFile("A.java", "public class A {", "  static String foo() { return null; }", "}");
    assertCompiles(fooReturnsType(/* shouldMatch= */ true, "java.lang.String"));
  }

  @Test
  public void notReturnsString() {
    writeFile("A.java", "public class A {", "  static int foo() { return 0; }", "}");
    assertCompiles(fooReturnsType(/* shouldMatch= */ false, "java.lang.String"));
  }

  @Test
  public void returnsInt() {
    writeFile("A.java", "public class A {", "  static int foo() { return 0; }", "}");
    assertCompiles(fooReturnsType(/* shouldMatch= */ true, "int"));
  }

  private abstract static class ScannerTest extends Scanner {
    abstract void assertDone();
  }

  private Scanner fooReturnsType(final boolean shouldMatch, final String typeString) {
    ScannerTest test =
        new ScannerTest() {
          private boolean matched = false;

          @Override
          public Void visitMethod(MethodTree node, VisitorState visitorState) {
            if (methodReturns(Suppliers.typeFromString(typeString)).matches(node, visitorState)) {
              matched = true;
            }
            return super.visitMethod(node, visitorState);
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
