/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.scanner;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.IdentifierTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Scanner}. */
@RunWith(JUnit4.class)
public class ScannerTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ShouldNotUseFoo.class, getClass());

  @Test
  public void notSuppressedByAnnotationOnType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.scanner.ScannerTest.Foo;",
            "class Test {",
            "  // BUG: Diagnostic contains: ShouldNotUseFoo",
            "  Foo foo;",
            "}")
        .doTest();
  }

  @Test
  public void notSuppressedByAnnotationOnParameterizedType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.scanner.ScannerTest.Foo;",
            "class Test {",
            "  // BUG: Diagnostic contains: ShouldNotUseFoo",
            "  Foo<String> foo;",
            "}")
        .doTest();
  }

  @Test
  public void suppressedByAnnotationOnUsage() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.scanner.ScannerTest.Foo;",
            "import com.google.errorprone.scanner.ScannerTest.OkToUseFoo;",
            "class Test {",
            "  @OkToUseFoo",
            "  Foo foo;",
            "}")
        .doTest();
  }

  @OkToUseFoo // Foo can use itself. But this shouldn't suppress errors on *usages* of Foo.
  public static final class Foo<T> {}

  public @interface OkToUseFoo {}

  @BugPattern(
      name = "ShouldNotUseFoo",
      summary = "Code should not use Foo.",
      category = JDK,
      severity = ERROR,
      suppressionAnnotations = OkToUseFoo.class)
  public static class ShouldNotUseFoo extends BugChecker implements IdentifierTreeMatcher {
    @Override
    public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
      return getSymbol(tree).getQualifiedName().contentEquals(Foo.class.getCanonicalName())
          ? describeMatch(tree)
          : NO_MATCH;
    }
  }
}
