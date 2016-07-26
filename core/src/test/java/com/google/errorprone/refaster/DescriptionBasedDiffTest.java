/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.apply.DescriptionBasedDiff;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@code DescriptionBasedDiff}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class DescriptionBasedDiffTest extends CompilerBasedTest {
  private JCCompilationUnit compilationUnit;

  private static final String[] lines = {
    "package foo.bar;",
    "import com.foo.Bar;",
    "",
    "class Foo {",
    "  public static void main(String[] args) {",
    "    System.out.println(\"foo\");",
    "  }",
    "}"
  };

  @Before
  public void setUp() {
    compile(lines);
    compilationUnit = Iterables.getOnlyElement(compilationUnits);
  }

  @Test
  public void noDiffs() {
    DescriptionBasedDiff diff = DescriptionBasedDiff.create(compilationUnit);
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines()).containsExactly((Object[]) lines).inOrder();
  }

  @Test
  public void oneDiff() {
    DescriptionBasedDiff diff = DescriptionBasedDiff.create(compilationUnit);
    diff.onDescribed(
        new Description(
            null, "message", SuggestedFix.replace(117, 120, "bar"), SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import com.foo.Bar;",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.out.println(\"bar\");",
            "  }",
            "}")
        .inOrder();
  }

  @Test
  public void prefixDiff() {
    DescriptionBasedDiff diff = DescriptionBasedDiff.create(compilationUnit);
    diff.onDescribed(
        new Description(
            null, "message", SuggestedFix.replace(120, 120, "bar"), SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import com.foo.Bar;",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.out.println(\"foobar\");",
            "  }",
            "}")
        .inOrder();
  }


  @Test
  public void twoDiffs() {
    DescriptionBasedDiff diff = DescriptionBasedDiff.create(compilationUnit);
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder().replace(104, 107, "longer").replace(117, 120, "bar").build(),
            SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import com.foo.Bar;",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.longer.println(\"bar\");",
            "  }",
            "}")
        .inOrder();
  }

  @Test
  public void addImport() {
    DescriptionBasedDiff diff = DescriptionBasedDiff.create(compilationUnit);
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder().addImport("com.google.foo.Bar").build(),
            SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import com.foo.Bar;",
            "import com.google.foo.Bar;",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.out.println(\"foo\");",
            "  }",
            "}")
        .inOrder();
  }

  @Test
  public void removeImport() {
    DescriptionBasedDiff diff = DescriptionBasedDiff.create(compilationUnit);
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder().removeImport("com.foo.Bar").build(),
            SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.out.println(\"foo\");",
            "  }",
            "}")
        .inOrder();
  }

  @Test
  public void twoDiffsWithImport() {
    DescriptionBasedDiff diff = DescriptionBasedDiff.create(compilationUnit);
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder()
                .replace(104, 107, "longer")
                .replace(117, 120, "bar")
                .addImport("com.google.foo.Bar")
                .build(),
            SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import com.foo.Bar;",
            "import com.google.foo.Bar;",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.longer.println(\"bar\");",
            "  }",
            "}")
        .inOrder();
  }
}
