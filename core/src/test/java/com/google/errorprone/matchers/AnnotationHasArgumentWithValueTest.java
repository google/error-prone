/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.matchers.Matchers.stringLiteral;
import static org.junit.Assert.assertTrue;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.AnnotationTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author alexeagle@google.com (Alex Eagle) */
@RunWith(JUnit4.class)
public class AnnotationHasArgumentWithValueTest extends CompilerBasedAbstractTest {
  @Before
  public void setUp() {
    writeFile("Thing.java", "public @interface Thing {", "  String stuff();", "}");
  }

  @Test
  public void testMatches() {
    writeFile("A.java", "@Thing(stuff=\"y\")", "public class A {}");
    assertCompiles(
        annotationMatches(
            /* shouldMatch= */ true,
            new AnnotationHasArgumentWithValue("stuff", stringLiteral("y"))));
  }

  @Test
  public void testMatchesExtraParentheses() {
    writeFile("Thing2.java", "public @interface Thing2 {", "  String value();", "}");
    writeFile("A.java", "@Thing2((\"y\"))", "public class A {}");
    assertCompiles(
        annotationMatches(
            /* shouldMatch= */ true,
            new AnnotationHasArgumentWithValue("value", stringLiteral("y"))));
  }

  @Test
  public void notMatches() {
    writeFile("A.java", "@Thing(stuff=\"n\")", "public class A{}");
    assertCompiles(
        annotationMatches(
            /* shouldMatch= */ false,
            new AnnotationHasArgumentWithValue("stuff", stringLiteral("y"))));
    assertCompiles(
        annotationMatches(
            /* shouldMatch= */ false,
            new AnnotationHasArgumentWithValue("other", stringLiteral("n"))));
  }

  @Test
  public void arrayValuedElement() {
    writeFile("A.java", "@SuppressWarnings({\"unchecked\",\"fallthrough\"})", "public class A{}");
    assertCompiles(
        annotationMatches(
            /* shouldMatch= */ true,
            new AnnotationHasArgumentWithValue("value", stringLiteral("unchecked"))));
  }

  private Scanner annotationMatches(
      final boolean shouldMatch, final AnnotationHasArgumentWithValue toMatch) {
    return new Scanner() {
      @Override
      public Void visitAnnotation(AnnotationTree node, VisitorState visitorState) {
        assertTrue(node.toString(), !shouldMatch ^ toMatch.matches(node, visitorState));
        return super.visitAnnotation(node, visitorState);
      }
    };
  }
}
