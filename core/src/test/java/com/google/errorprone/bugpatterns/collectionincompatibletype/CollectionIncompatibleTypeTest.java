/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugpatterns.collectionincompatibletype.CollectionIncompatibleType.FixType;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ScannerSupplier;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author alexeagle@google.com (Alex Eagle) */
@RunWith(JUnit4.class)
public class CollectionIncompatibleTypeTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(CollectionIncompatibleType.class, getClass());
  }

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypePositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeNegativeCases.java").doTest();
  }

  @Test
  public void testOutOfBounds() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeOutOfBounds.java").doTest();
  }

  @Test
  public void testClassCast() {
    compilationHelper.addSourceFile("CollectionIncompatibleTypeClassCast.java").doTest();
  }

  @Test
  public void testCastFixes() {
    CompilationTestHelper compilationHelperWithCastFix =
        CompilationTestHelper.newInstance(
            ScannerSupplier.fromScanner(
                new ErrorProneScanner(new CollectionIncompatibleType(FixType.CAST))),
            getClass());
    compilationHelperWithCastFix
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "public class Test {",
            "  public void doIt(Collection<String> c1, Collection<Integer> c2) {",
            "    // BUG: Diagnostic contains: c1.contains((Object) 1);",
            "    c1.contains(1);",
            "    // BUG: Diagnostic contains: c1.containsAll((Collection<?>) c2);",
            "    c1.containsAll(c2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testSuppressWarningsFix() {
    BugCheckerRefactoringTestHelper refactorTestHelper =
        BugCheckerRefactoringTestHelper.newInstance(
            new CollectionIncompatibleType(FixType.SUPPRESS_WARNINGS), getClass());
    refactorTestHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Collection;",
            "public class Test {",
            "  public void doIt(Collection<String> c1, Collection<Integer> c2) {",
            "    c1.contains(1);",
            "    c1.containsAll(c2);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Collection;",
            "public class Test {",
            "  @SuppressWarnings(\"CollectionIncompatibleType\")",
            // In this test environment, the fix doesn't include formatting
            "public void doIt(Collection<String> c1, Collection<Integer> c2) {",
            "    c1.contains(/* expected: String, actual: int */ 1);",
            "    c1.containsAll(/* expected: String, actual: Integer */ c2);",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  // This test is disabled because calling Types#asSuper in the check removes the upper bound on K.
  @Test
  @Ignore
  public void testBoundedTypeParameters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.HashMap;",
            "public class Test {",
            "  private static class MyHashMap<K extends Integer, V extends String>",
            "      extends HashMap<K, V> {}",
            "  public boolean boundedTypeParameters(MyHashMap<?, ?> myHashMap) {",
            "    // BUG: Diagnostic contains:",
            "    return myHashMap.containsKey(\"bad\");",
            "  }",
            "}")
        .doTest();
  }
}
