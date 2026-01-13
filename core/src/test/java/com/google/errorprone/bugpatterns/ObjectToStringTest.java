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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.ClassTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class ObjectToStringTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ObjectToString.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "ObjectToStringPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author bhagwani@google.com (Sumit Bhagwani)
             */
            public class ObjectToStringPositiveCases {

              public static final class FinalObjectClassWithoutToString {}

              public static final class FinalGenericClassWithoutToString<T> {}

              void directToStringCalls() {
                FinalObjectClassWithoutToString finalObjectClassWithoutToString =
                    new FinalObjectClassWithoutToString();
                // BUG: Diagnostic contains: ObjectToString
                System.out.println(finalObjectClassWithoutToString.toString());
              }

              void genericClassShowsErasure() {
                FinalGenericClassWithoutToString<Object> finalGenericClassWithoutToString =
                    new FinalGenericClassWithoutToString<>();
                // BUG: Diagnostic contains: `FinalGenericClassWithoutToString@
                System.out.println(finalGenericClassWithoutToString.toString());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "ObjectToStringNegativeCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import org.joda.time.Duration;

/**
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class ObjectToStringNegativeCases {

  public static final class FinalObjectClassWithoutToString {}

  public static class NonFinalObjectClassWithoutToString {}

  public static final class FinalObjectClassWithToString {

    @Override
    public String toString() {
      return "hakuna";
    }
  }

  public static class NonFinalObjectClassWithToString {

    @Override
    public String toString() {
      return "matata";
    }
  }

  public void log(Object o) {
    System.out.println(o.toString());
  }

  void directToStringCalls() {
    NonFinalObjectClassWithoutToString nonFinalObjectClassWithoutToString =
        new NonFinalObjectClassWithoutToString();
    System.out.println(nonFinalObjectClassWithoutToString.toString());

    FinalObjectClassWithToString finalObjectClassWithToString = new FinalObjectClassWithToString();
    System.out.println(finalObjectClassWithToString.toString());

    NonFinalObjectClassWithToString nonFinalObjectClassWithToString =
        new NonFinalObjectClassWithToString();
    System.out.println(nonFinalObjectClassWithToString.toString());
  }

  void callsTologMethod() {
    FinalObjectClassWithoutToString finalObjectClassWithoutToString =
        new FinalObjectClassWithoutToString();
    log(finalObjectClassWithoutToString);

    NonFinalObjectClassWithoutToString nonFinalObjectClassWithoutToString =
        new NonFinalObjectClassWithoutToString();
    log(nonFinalObjectClassWithoutToString);

    FinalObjectClassWithToString finalObjectClassWithToString = new FinalObjectClassWithToString();
    log(finalObjectClassWithToString);

    NonFinalObjectClassWithToString nonFinalObjectClassWithToString =
        new NonFinalObjectClassWithToString();
    log(nonFinalObjectClassWithToString);
  }

  public void overridePresentInAbstractClassInHierarchy(Duration durationArg) {
    String unusedString = Duration.standardSeconds(86400).toString();
    System.out.println("test joda string " + Duration.standardSeconds(86400));

    unusedString = durationArg.toString();
    System.out.println("test joda string " + durationArg);
  }
}
""")
        .doTest();
  }

  /** A class that will be missing at compile-time for {@link #testIncompleteClasspath}. */
  public static class One {}

  /** A test class for {@link #testIncompleteClasspath}. */
  public abstract static class TestLib {

    /** Another test class for {@link #testIncompleteClasspath}. */
    public static final class Two extends One {
      @Override
      public String toString() {
        return "";
      }
    }

    public abstract Two f();
  }

  // A bugchecker that eagerly completes the missing symbol for testIncompleteClasspath below,
  // to avoid the CompletionFailure being reported later.
  /** A checker for {@link #testIncompleteClasspath}. */
  @BugPattern(summary = "", severity = ERROR)
  public static class CompletionChecker extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      var unused = state.getSymbolFromString(One.class.getName());
      return Description.NO_MATCH;
    }
  }

  // don't complain if we can't load the type hierarchy of a class that is toString()'d
  @Test
  public void incompleteClasspath() {
    CompilationTestHelper.newInstance(
            ScannerSupplier.fromBugCheckerClasses(ObjectToString.class, CompletionChecker.class),
            getClass())
        .addSourceLines(
            "Test.java",
            "import " + TestLib.class.getCanonicalName() + ";",
            "class Test {",
            "  String f(TestLib lib) {",
            "    return \"\" + lib.f();",
            "  }",
            "}")
        .withClasspath(ObjectToStringTest.class, TestLib.class, TestLib.Two.class)
        .doTest();
  }

  @Test
  public void qualifiedName() {
    compilationHelper
        .addSourceLines(
            "A.java",
            """
            class A {
              static final class B {}
            }
            """)
        .addSourceLines(
            "C.java",
            """
            class C {
              String test() {
                // BUG: Diagnostic contains: A.B
                return new A.B().toString();
              }
            }
            """)
        .doTest();
  }
}
