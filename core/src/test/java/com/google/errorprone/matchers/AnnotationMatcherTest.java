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
import static com.google.errorprone.matchers.Matchers.isType;
import static org.junit.Assert.assertEquals;

import com.google.errorprone.VisitorState;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
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
public class AnnotationMatcherTest extends CompilerBasedAbstractTest {

  final List<ScannerTest> tests = new ArrayList<ScannerTest>();

  @Before
  public void setUp() {
    tests.clear();
    writeFile(
        "SampleAnnotation1.java", "package com.google;", "public @interface SampleAnnotation1 {}");
    writeFile(
        "SampleAnnotation2.java", "package com.google;", "public @interface SampleAnnotation2 {}");
    writeFile(
        "SampleNestedAnnotation.java",
        "package com.google;",
        "public class SampleNestedAnnotation {",
        "  public @interface Annotation {}",
        "}");
  }

  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  @Test
  public void shouldNotMatchNoAnnotations() {
    writeFile("A.java", "package com.google;", "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            false,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            false, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchSingleAnnotationOnClass() {
    writeFile("A.java", "package com.google;", "@SampleAnnotation1", "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchSingleFullyQualifiedAnnotationOnClass() {
    writeFile(
        "A.java", "package com.google.foo;", "@com.google.SampleAnnotation1", "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchSingleNestedAnnotationOnClass() {
    writeFile(
        "A.java", "package com.google;", "@SampleNestedAnnotation.Annotation", "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(
                AT_LEAST_ONE, isType("com.google.SampleNestedAnnotation.Annotation"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(
                ALL, isType("com.google.SampleNestedAnnotation.Annotation"))));
  }

  @Test
  public void shouldNotMatchNonmatchingSingleAnnotationOnClass() {
    writeFile("A.java", "package com.google;", "@SampleAnnotation1", "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            false,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.WrongAnnotation"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            false, new AnnotationMatcher<Tree>(ALL, isType("com.google.WrongAnnotation"))));
  }

  @Test
  public void shouldNotMatchNonmatchingSingleFullyQualifiedAnnotationOnClass() {
    writeFile(
        "A.java", "package com.google.foo;", "@com.google.SampleAnnotation1", "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            false,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.WrongAnnotation"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            false, new AnnotationMatcher<Tree>(ALL, isType("com.google.WrongAnnotation"))));
  }

  @Test
  public void shouldNotMatchNonmatchingNestedAnnotationOnClass() {
    writeFile(
        "A.java",
        "package com.google;",
        "@com.google.SampleNestedAnnotation.Annotation",
        "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            false,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.WrongAnnotation"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            false, new AnnotationMatcher<Tree>(ALL, isType("com.google.WrongAnnotation"))));
  }

  @Test
  public void shouldMatchAllAnnotationsOnClass() {
    writeFile(
        "A.java",
        "package com.google;",
        "@SampleAnnotation1 @SampleAnnotation2",
        "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(
                AT_LEAST_ONE,
                Matchers.<AnnotationTree>anyOf(
                    isType("com.google.SampleAnnotation1"),
                    isType("com.google.SampleAnnotation2")))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(
                ALL,
                Matchers.<AnnotationTree>anyOf(
                    isType("com.google.SampleAnnotation1"),
                    isType("com.google.SampleAnnotation2")))));
  }

  @Test
  public void matchOneAnnotationsOnClass() {
    writeFile(
        "A.java",
        "package com.google;",
        "@SampleAnnotation1 @SampleAnnotation2",
        "public class A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            false, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnInterface() {
    writeFile("A.java", "package com.google;", "@SampleAnnotation1", "public interface A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnEnum() {
    writeFile("A.java", "package com.google;", "@SampleAnnotation1", "public enum A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnField() {
    writeFile(
        "A.java",
        "package com.google;",
        "public class A {",
        "  @SampleAnnotation1",
        "  public int i;",
        "}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnMethod() {
    writeFile(
        "A.java",
        "package com.google;",
        "public class A {",
        "  @SampleAnnotation1",
        "  public void foo() {}",
        "}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnParameter() {
    writeFile(
        "A.java",
        "package com.google;",
        "public class A {",
        "  public void foo(@SampleAnnotation1 int i) {}",
        "}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnConstructor() {
    writeFile(
        "A.java",
        "package com.google;",
        "public class A {",
        "  @SampleAnnotation1",
        "  public A() {}",
        "}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnLocalVariable() {
    writeFile(
        "A.java",
        "package com.google;",
        "public class A {",
        "  public void foo() {",
        "    @SampleAnnotation1",
        "    int i = 0;",
        "  }",
        "}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnAnnotation() {
    writeFile("A.java", "package com.google;", "@SampleAnnotation1", "public @interface A {}");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  @Test
  public void shouldMatchAnnotationOnPackage() {
    writeFile("package-info.java", "@SampleAnnotation1", "package com.google;");
    assertCompiles(
        nodeWithAnnotationMatches(
            true,
            new AnnotationMatcher<Tree>(AT_LEAST_ONE, isType("com.google.SampleAnnotation1"))));
    assertCompiles(
        nodeWithAnnotationMatches(
            true, new AnnotationMatcher<Tree>(ALL, isType("com.google.SampleAnnotation1"))));
  }

  private abstract class ScannerTest extends Scanner {
    public abstract void assertDone();
  }

  private Scanner nodeWithAnnotationMatches(
      final boolean shouldMatch, final AnnotationMatcher<Tree> toMatch) {
    ScannerTest test =
        new ScannerTest() {
          private boolean matched = false;

          @Override
          public Void visitAnnotation(AnnotationTree node, VisitorState visitorState) {
            TreePath currPath = getCurrentPath().getParentPath();
            Tree parent = currPath.getLeaf();
            if (parent.getKind() == Kind.MODIFIERS) {
              currPath = currPath.getParentPath();
              parent = currPath.getLeaf();
            }
            visitorState = visitorState.withPath(currPath);
            if (toMatch.matches(parent, visitorState)) {
              matched = true;
            }

            visitorState = visitorState.withPath(getCurrentPath());
            return super.visitAnnotation(node, visitorState);
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
