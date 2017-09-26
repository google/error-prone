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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.apply.DescriptionBasedDiff;
import com.google.errorprone.apply.ImportOrganizer;
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
    "import org.bar.Baz;",
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

  private DescriptionBasedDiff createDescriptionBasedDiff() {
    return DescriptionBasedDiff.create(compilationUnit, ImportOrganizer.STATIC_FIRST_ORGANIZER);
  }

  @Test
  public void noDiffs() {
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines()).containsExactly((Object[]) lines).inOrder();
  }

  @Test
  public void oneDiff() {
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    diff.onDescribed(
        new Description(
            null, "message", SuggestedFix.replace(137, 140, "bar"), SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import org.bar.Baz;",
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
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    diff.onDescribed(
        new Description(
            null, "message", SuggestedFix.replace(140, 140, "bar"), SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import org.bar.Baz;",
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
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder().replace(124, 127, "longer").replace(137, 140, "bar").build(),
            SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import org.bar.Baz;",
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
  public void overlappingDiffs_throws() {
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            diff.onDescribed(
                new Description(
                    null,
                    "message",
                    SuggestedFix.builder()
                        .replace(137, 140, "baz")
                        .replace(137, 140, "bar")
                        .build(),
                    SeverityLevel.SUGGESTION)));

    DescriptionBasedDiff diff2 = createDescriptionBasedDiff();
    diff2.onDescribed(
        new Description(
            null,
            "bah",
            SuggestedFix.builder().replace(137, 140, "baz").build(),
            SeverityLevel.SUGGESTION));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            diff2.onDescribed(
                new Description(
                    null,
                    "message",
                    SuggestedFix.builder().replace(137, 140, "bar").build(),
                    SeverityLevel.SUGGESTION)));

    DescriptionBasedDiff diff3 =
        DescriptionBasedDiff.createIgnoringOverlaps(
            compilationUnit, ImportOrganizer.STATIC_FIRST_ORGANIZER);
    diff3.onDescribed(
        new Description(
            null,
            "bah",
            SuggestedFix.builder().replace(137, 140, "baz").build(),
            SeverityLevel.SUGGESTION));
    // No throw, since it's lenient. Refactors to the first "baz" replacement and ignores this.
    diff3.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder().replace(137, 140, "bar").build(),
            SeverityLevel.SUGGESTION));
    diff3.applyDifferences(sourceFile);

    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import org.bar.Baz;",
            "import com.foo.Bar;",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.out.println(\"baz\");",
            "  }",
            "}")
        .inOrder();
  }

  @Test
  public void applyDifferences_addsImportAndSorts_whenAddingNewImport() {
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
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
            "import org.bar.Baz;",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.out.println(\"foo\");",
            "  }",
            "}")
        .inOrder();
  }

  @Test
  public void applyDifferences_preservesImportOrder_whenAddingExistingImport() {
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder().addImport("com.foo.Bar").build(),
            SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import org.bar.Baz;",
            "import com.foo.Bar;",
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
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder().removeImport("com.foo.Bar").removeImport("org.bar.Baz").build(),
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
  public void applyDifferences_preservesOrder_whenRemovingNonExistentImport() {
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder().removeImport("com.google.foo.Bar").build(),
            SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import org.bar.Baz;",
            "import com.foo.Bar;",
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
    DescriptionBasedDiff diff = createDescriptionBasedDiff();
    diff.onDescribed(
        new Description(
            null,
            "message",
            SuggestedFix.builder()
                .replace(124, 127, "longer")
                .replace(137, 140, "bar")
                .addImport("com.google.foo.Bar")
                .build(),
            SeverityLevel.SUGGESTION));
    diff.applyDifferences(sourceFile);
    assertThat(sourceFile.getLines())
        .containsExactly(
            "package foo.bar;",
            "import com.foo.Bar;",
            "import com.google.foo.Bar;",
            "import org.bar.Baz;",
            "",
            "class Foo {",
            "  public static void main(String[] args) {",
            "    System.longer.println(\"bar\");",
            "  }",
            "}")
        .inOrder();
  }
}
