/*
 * Copyright 2021 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StackTraceElementGetClass}. */
@RunWith(JUnit4.class)
public class StackTraceElementGetClassTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(StackTraceElementGetClass.class, getClass());

  @Test
  public void positive_getName_refactoredToGetClassName() {
    helper
        .addInputLines(
            "Test.java",
            "class Test{",
            " void f() {",
            "   try {",
            "     throw new Exception();",
            "   }",
            "   catch(Exception ex) {",
            "     ex.getStackTrace()[0].getClass().getName();",
            "   }",
            " }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test{",
            " void f() {",
            "   try {",
            "     throw new Exception();",
            "   }",
            "   catch(Exception ex) {",
            "     ex.getStackTrace()[0].getClassName();",
            "   }",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void positive_getSimpleName_refactoredToGetClassName() {
    helper
        .addInputLines(
            "Test.java",
            "class Test{",
            " void f() {",
            "   try {",
            "     throw new Exception();",
            "   }",
            "   catch(Exception ex) {",
            "     ex.getStackTrace()[0].getClass().getSimpleName();",
            "   }",
            " }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test{",
            " void f() {",
            "   try {",
            "     throw new Exception();",
            "   }",
            "   catch(Exception ex) {",
            "     ex.getStackTrace()[0].getClassName();",
            "   }",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void positive_getClass_refactoredToClass() {
    helper
        .addInputLines(
            "Test.java",
            "import java.lang.reflect.Field;",
            "class Test{",
            " void f() throws Exception {",
            "   try {",
            "     throw new Exception();",
            "   }",
            "   catch(Exception ex) {",
            "     Field f = "
                + " ex.getStackTrace()[0].getClass().getDeclaredField(\"declaringClass\");",
            "   }",
            " }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.lang.reflect.Field;",
            "class Test{",
            " void f() throws Exception {",
            "   try {",
            "     throw new Exception();",
            "   }",
            "   catch(Exception ex) {",
            "     Field f = StackTraceElement.class.getDeclaredField(\"declaringClass\");",
            "   }",
            " }",
            "}")
        .doTest();
  }

  @Test
  public void negative_unchanged() {
    helper
        .addInputLines(
            "Test.java",
            "class Test{",
            " void f() {",
            "   try {",
            "     throw new Exception();",
            "   }",
            "   catch(Exception ex) {",
            "     ex.getStackTrace()[0].getClassName();",
            "   }",
            " }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
