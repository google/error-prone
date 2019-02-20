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
package com.google.errorprone.bugpatterns;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers.FIRST;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.Replacement;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.TryTree;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for the missing fail matcher. */
@RunWith(JUnit4.class)
public class MissingFailTest {

  private CompilationTestHelper compilationHelper;
  private BugCheckerRefactoringTestHelper refactoringHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(MissingFail.class, getClass());
    refactoringHelper = BugCheckerRefactoringTestHelper.newInstance(new MissingFail(), getClass());
  }

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("MissingFailPositiveCases.java").doTest();
  }

  @Test
  public void testPositiveCases2() {
    compilationHelper.addSourceFile("MissingFailPositiveCases2.java").doTest();
  }

  @Test
  public void testPositiveCases3() {
    compilationHelper.addSourceFile("MissingFailPositiveCases3.java").doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper.addSourceFile("MissingFailNegativeCases.java").doTest();
  }

  @Test
  public void testNegativeCases2() {
    compilationHelper.addSourceFile("MissingFailNegativeCases2.java").doTest();
  }

  @Test
  public void testFailImport() {
    TestScanner scanner = new TestScanner();
    CompilationTestHelper compilationHelper =
        CompilationTestHelper.newInstance(ScannerSupplier.fromScanner(scanner), getClass());

    compilationHelper
        .addSourceLines(
            "test/A.java",
            "package test;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "    } catch (IllegalArgumentException expected) {}",
            "  }",
            "}")
        .doTest();

    Description warning = Iterables.getOnlyElement(scanner.suggestedChanges);
    assertThat(warning.fixes).hasSize(2);
    // Fix that adds fail comes after assertThrows fix.
    Fix fix2 = warning.fixes.get(1);
    assertThat(fix2.getImportsToAdd()).containsExactly("import static org.junit.Assert.fail");
    assertThat(fix2.getImportsToRemove())
        .containsExactly(
            "import static junit.framework.TestCase.fail",
            "import static junit.framework.Assert.fail");
  }

  @Test
  public void testFailMessageMultiCatch() {
    TestScanner scanner = new TestScanner();
    CompilationTestHelper compilationHelper =
        CompilationTestHelper.newInstance(ScannerSupplier.fromScanner(scanner), getClass());

    compilationHelper
        .addSourceLines(
            "test/A.java",
            "package test;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "    } catch (IllegalArgumentException | IllegalStateException expected) {}",
            "  }",
            "}")
        .doTest();

    Description warning = Iterables.getOnlyElement(scanner.suggestedChanges);
    // Fix that adds fail comes after assertThrows fix.
    Fix fix2 = warning.fixes.get(1);
    assertThat(fix2.getReplacements(new NoopEndPosTable()))
        .containsExactly(Replacement.create(0, 0, "\nfail(\"Expected Exception\");"));
  }

  // verify that exceptions not named 'expected' are ignored
  @Test
  public void testToleratedException() {
    compilationHelper
        .addSourceLines(
            "test/A.java",
            "package test;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "    } catch (IllegalArgumentException | IllegalStateException tolerated) {}",
            "  }",
            "}")
        .doTest();
  }

  // verify that exceptions not named 'expected' are ignored
  @Test
  public void testToleratedExceptionWithAssert() {
    compilationHelper
        .addSourceLines(
            "test/A.java",
            "package test;",
            "import junit.framework.TestCase;",
            "public class A extends TestCase {",
            "  public void testMethod() {",
            "    try {",
            "      new String();",
            "    } catch (IllegalArgumentException | IllegalStateException tolerated) {",
            "      assertDummy();",
            "    }",
            "  }",
            "  static void assertDummy() {}",
            "}")
        .doTest();
  }

  @Test
  public void assertThrowsCatchBlock() {
    refactoringHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void f() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "      Files.readAllBytes(p);",
            "    } catch (IOException e) {",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "  @Test",
            "  public void g() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "    } catch (IOException e) {",
            "      assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void f() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    IOException e = assertThrows(IOException.class, () -> {",
            "      Files.readAllBytes(p);",
            "      Files.readAllBytes(p);",
            "    });",
            "    assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "  }",
            "  @Test",
            "  public void g() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    IOException e = assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "    assertThat(e).hasMessageThat().contains(\"NOSUCH\");",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest();
  }

  @Test
  public void assertThrowsEmptyCatch() {
    refactoringHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    try {",
            "      Files.readAllBytes(p);",
            "    } catch (IOException expected) {",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/ExceptionTest.java",
            "import static org.junit.Assert.assertThrows;",
            "import java.io.IOException;",
            "import java.nio.file.*;",
            "import org.junit.Test;",
            "class ExceptionTest {",
            "  @Test",
            "  public void test() throws Exception {",
            "    Path p = Paths.get(\"NOSUCH\");",
            "    assertThrows(IOException.class, () -> Files.readAllBytes(p));",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest();
  }

  @Test
  public void emptyTry() {
    refactoringHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import org.junit.Test;",
            "abstract class ExceptionTest {",
            "  abstract AutoCloseable c();",
            "  @Test",
            "  public void test() {",
            "    try (AutoCloseable c = c()) {",
            "    } catch (Exception expected) {",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void noEnclosingMethod() {
    refactoringHelper
        .addInputLines(
            "in/ExceptionTest.java",
            "import java.io.IOException;",
            "import org.junit.Test;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "abstract class ExceptionTest {",
            "  abstract void c();",
            "  {",
            "    try {",
            "      c();",
            "    } catch (Exception expected) {",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  private static class TestScanner extends Scanner {

    final List<Description> suggestedChanges = new ArrayList<>();

    @Override
    public Void visitTry(TryTree node, VisitorState visitorState) {
      suggestedChanges.add(
          new MissingFail().matchTry(node, visitorState.withPath(getCurrentPath())));
      return super.visitTry(node, visitorState);
    }
  }

  private static class NoopEndPosTable implements EndPosTable {

    @Override
    public int getEndPos(JCTree tree) {
      return 0;
    }

    @Override
    public void storeEnd(JCTree tree, int endpos) {}

    @Override
    public int replaceTree(JCTree oldtree, JCTree newtree) {
      return 0;
    }
  }
}
