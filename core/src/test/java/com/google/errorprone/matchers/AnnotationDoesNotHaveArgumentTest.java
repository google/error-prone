/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.AnnotationTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link AnnotationDoesNotHaveArgument}.
 *
 * @author mwacker@google.com (Mike Wacker)
 */
@RunWith(JUnit4.class)
public class AnnotationDoesNotHaveArgumentTest extends CompilerBasedAbstractTest {

  @Before
  public void createAnnotation() {
    writeFile(
        "Annotation.java",
        """
        public @interface Annotation {
          String value() default "";
        }
        """);
  }

  @Test
  public void matchesWhenArgumentIsNotPresent() {
    writeFile(
        "Class.java",
        """
        @Annotation
        public class Class {}
        """);
    assertCompiles(annotationMatches(true));
  }

  @Test
  public void matchesWhenArgumentIsNotPresent_otherArgumentPresent() {
    writeFile(
        "Annotation2.java",
        """
        public @interface Annotation2 {
          String value() default "";

          String otherValue() default "";
        }
        """);
    writeFile(
        "Class.java",
        """
        @Annotation2(otherValue = "literal")
        public class Class {}
        """);
    assertCompiles(annotationMatches(true));
  }

  @Test
  public void doesNotMatchWhenArgumentIsPresent_implicit() {
    writeFile(
        "Class.java",
        """
        @Annotation("literal")
        public class Class {}
        """);
    assertCompiles(annotationMatches(false));
  }

  @Test
  public void doesNotMatchWhenArgumentIsPresent_explicit() {
    writeFile(
        "Class.java",
        """
        @Annotation(value = "literal")
        public class Class {}
        """);
    assertCompiles(annotationMatches(false));
  }

  private Scanner annotationMatches(boolean shouldMatch) {
    AnnotationDoesNotHaveArgument toMatch = new AnnotationDoesNotHaveArgument("value");
    return new Scanner() {
      @Override
      public Void visitAnnotation(AnnotationTree node, VisitorState visitorState) {
        assertWithMessage(node.toString())
            .that(!shouldMatch ^ toMatch.matches(node, visitorState))
            .isTrue();
        return super.visitAnnotation(node, visitorState);
      }
    };
  }
}
